<#
.SYNOPSIS
  Verifies a deployment of the online-exam microservices (Windows PowerShell 5.1+ / PowerShell 7).

.EXAMPLE
  # OpenShift: discovers Routes, checks rollouts/pods/health, then runs the API flow
  .\scripts\verify-deployment.ps1 -OpenShift -Namespace online-exam -AdminEmail admin@x -AdminPassword '...'

.EXAMPLE
  # Any environment with explicit URLs
  .\scripts\verify-deployment.ps1 -AuthUrl http://localhost:8080 -OrgUrl http://localhost:8082 `
      -QuestionUrl http://localhost:8084 -ExamUrl http://localhost:8086 -AdminEmail admin@x -AdminPassword '...'

.NOTES
  The API flow creates an organisation, users, a class, a subject and two questions with a unique
  suffix, takes an exam as a student, checks the score (50 %) and deletes what it created.
#>
param(
    [switch]$OpenShift,
    [string]$Namespace = "",
    [string]$AuthUrl = $env:AUTH_URL,
    [string]$OrgUrl = $env:ORG_URL,
    [string]$QuestionUrl = $env:QUESTION_URL,
    [string]$ExamUrl = $env:EXAM_URL,
    [string]$AdminEmail = $env:ADMIN_EMAIL,
    [string]$AdminPassword = $env:ADMIN_PASSWORD,
    [switch]$Insecure
)

$ErrorActionPreference = "Stop"
$script:Pass = 0
$script:Fail = 0
$services = @("auth-service", "org-service", "question-service", "exam-service")

[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
if ($Insecure) {
    if ($PSVersionTable.PSVersion.Major -ge 6) {
        $PSDefaultParameterValues["Invoke-WebRequest:SkipCertificateCheck"] = $true
    } else {
        Add-Type @"
using System.Net; using System.Security.Cryptography.X509Certificates;
public class TrustAll : ICertificatePolicy {
  public bool CheckValidationResult(ServicePoint s, X509Certificate c, WebRequest r, int p) { return true; }
}
"@
        [Net.ServicePointManager]::CertificatePolicy = New-Object TrustAll
    }
}

function Section($t) { Write-Host ""; Write-Host "== $t ==" -ForegroundColor White }
function Ok($m) { $script:Pass++; Write-Host "  PASS  $m" -ForegroundColor Green }
function Ko($m) { $script:Fail++; Write-Host "  FAIL  $m" -ForegroundColor Red }
function Check($name, $actual, $expected) {
    if ("$actual" -eq "$expected") { Ok $name } else { Ko "$name (expected $expected, got $actual)" }
}

# Returns @{ Status = <int>; Json = <object or $null> }
function Call([string]$Method, [string]$Url, [string]$Token = "", $Body = $null) {
    $headers = @{ Accept = "application/json" }
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    $params = @{ Method = $Method; Uri = $Url; Headers = $headers; UseBasicParsing = $true; TimeoutSec = 30 }
    if ($null -ne $Body) {
        $params["ContentType"] = "application/json"
        $params["Body"] = [Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 10 -Compress))
    }
    try {
        $r = Invoke-WebRequest @params
        $json = $null
        if ($r.Content) { $json = $r.Content | ConvertFrom-Json }
        return @{ Status = [int]$r.StatusCode; Json = $json; Raw = $r.Content }
    } catch {
        $resp = $_.Exception.Response
        if ($null -eq $resp) { return @{ Status = 0; Json = $null; Raw = $_.Exception.Message } }
        return @{ Status = [int]$resp.StatusCode; Json = $null; Raw = "" }
    }
}

# ---------------------------------------------------------------------------------------------
if ($OpenShift) {
    if (-not (Get-Command oc -ErrorAction SilentlyContinue)) { throw "oc CLI not found in PATH" }
    $nsArgs = @()
    if ($Namespace) { $nsArgs = @("-n", $Namespace) } else { $Namespace = (oc project -q) }

    Section "Cluster access"
    $who = oc whoami 2>$null
    if ($LASTEXITCODE -ne 0) { throw "Not logged in - run: oc login --token=... --server=..." }
    Ok "logged in as $who on $(oc whoami --show-server), project $Namespace"

    Section "Builds"
    foreach ($s in $services) {
        $phase = oc get builds -l "buildconfig=$s" --sort-by=.metadata.creationTimestamp -o "jsonpath={.items[-1:].status.phase}" @nsArgs 2>$null
        if ($phase) { Check "$s latest build" $phase "Complete" } else { Write-Host "  info  ${s}: no in-cluster build (external image)" }
    }

    Section "Rollouts and pods"
    foreach ($s in $services) {
        oc rollout status "deploy/$s" --timeout=240s @nsArgs *> $null
        if ($LASTEXITCODE -eq 0) { Ok "$s rolled out" } else { Ko "$s rollout not complete" }
        $want = oc get deploy $s -o "jsonpath={.spec.replicas}" @nsArgs 2>$null
        $ready = oc get deploy $s -o "jsonpath={.status.readyReplicas}" @nsArgs 2>$null
        if (-not $ready) { $ready = 0 }
        Check "$s ready replicas" $ready $want
        $restarts = oc get pods -l "app.kubernetes.io/name=$s" -o "jsonpath={.items[*].status.containerStatuses[0].restartCount}" @nsArgs 2>$null
        $max = 0; foreach ($n in "$restarts".Split(" ", [StringSplitOptions]::RemoveEmptyEntries)) { if ([int]$n -gt $max) { $max = [int]$n } }
        Check "$s container restarts" $max 0
    }

    Section "Actuator health (API server service proxy)"
    foreach ($s in $services) {
        $h = oc get --raw "/api/v1/namespaces/$Namespace/services/http:${s}:management/proxy/actuator/health" 2>$null
        if ($LASTEXITCODE -eq 0 -and $h) { Check "$s /actuator/health" (($h | ConvertFrom-Json).status) "UP" }
        else { Write-Host "  info  ${s}: service proxy not permitted; readiness status above is authoritative" }
    }

    Section "Routes"
    $urls = @{}
    foreach ($s in $services) {
        $routeHost = oc get route $s -o "jsonpath={.spec.host}" @nsArgs 2>$null
        if ($routeHost) { Ok "$s route https://$routeHost"; $urls[$s] = "https://$routeHost" } else { Ko "$s has no route" }
    }
    if (-not $AuthUrl) { $AuthUrl = $urls["auth-service"] }
    if (-not $OrgUrl) { $OrgUrl = $urls["org-service"] }
    if (-not $QuestionUrl) { $QuestionUrl = $urls["question-service"] }
    if (-not $ExamUrl) { $ExamUrl = $urls["exam-service"] }
}

foreach ($v in @("AuthUrl", "OrgUrl", "QuestionUrl", "ExamUrl", "AdminEmail", "AdminPassword")) {
    if (-not (Get-Variable $v -ValueOnly)) { throw "-$v is required (or the matching environment variable)" }
}

# ---------------------------------------------------------------------------------------------
Section "Security baseline"
Check "no token -> 401" (Call GET "$AuthUrl/api/users").Status 401
Check "invalid token -> 401" (Call GET "$AuthUrl/api/users" "not.a.jwt").Status 401
Check "bad credentials -> 401" (Call POST "$AuthUrl/api/auth/login" "" @{ email = "nobody@example.com"; password = "wrong-password" }).Status 401

Section "Business flow"
$run = "v" + [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$studentPw = "Student#$run"
$adminPw = "OrgAdmin#$run"

$r = Call POST "$AuthUrl/api/auth/login" "" @{ email = $AdminEmail; password = $AdminPassword }
Check "super admin login" $r.Status 200
if ($r.Status -ne 200) { Write-Host "Cannot continue without a super admin token" -ForegroundColor Red; exit 1 }
$sa = $r.Json.accessToken

$r = Call POST "$OrgUrl/api/orgs" $sa @{ name = "Verify $run"; email = "org-$run@verify.local" }
Check "create organisation" $r.Status 201; $org = $r.Json.id

$r = Call POST "$AuthUrl/api/users" $sa @{ orgId = $org; name = "Verify Admin"; email = "admin-$run@verify.local"; password = $adminPw; role = "ADMIN" }
Check "create org admin" $r.Status 201; $adminId = $r.Json.id
$r = Call POST "$AuthUrl/api/users" $sa @{ orgId = $org; name = "Verify Student"; email = "student-$run@verify.local"; password = $studentPw; role = "USER" }
Check "create student" $r.Status 201; $studentId = $r.Json.id

$r = Call POST "$AuthUrl/api/auth/login" "" @{ email = "admin-$run@verify.local"; password = $adminPw }
Check "org admin login" $r.Status 200; $ta = $r.Json.accessToken
$r = Call POST "$AuthUrl/api/auth/login" "" @{ email = "student-$run@verify.local"; password = $studentPw }
Check "student login" $r.Status 200; $ts = $r.Json.accessToken

$r = Call POST "$OrgUrl/api/classes" $ta @{ orgId = $org; name = "Class $run" }
Check "create class" $r.Status 201; $class = $r.Json.id
$r = Call POST "$OrgUrl/api/subjects" $ta @{ classId = $class; name = "Science $run" }
Check "create subject" $r.Status 201; $subject = $r.Json.id
Check "student cannot create class -> 403" (Call POST "$OrgUrl/api/classes" $ts @{ orgId = $org; name = "Nope" }).Status 403

function New-Question($text, $right, $wrong) {
    Call POST "$QuestionUrl/api/questions" $ta @{
        classId = $class; subjectId = $subject; text = $text
        options = @(@{ text = $right; correct = $true }, @{ text = $wrong; correct = $false })
    }
}
$q1 = New-Question "Which is the biggest planet?" "Jupiter" "Mars"
Check "create question 1" $q1.Status 201
$q2 = New-Question "How many wheels does a scooter have?" "2" "3"
Check "create question 2" $q2.Status 201

$r = Call GET "$QuestionUrl/api/question-papers?subjectId=$subject&limit=10" $ts
Check "student gets question paper" $r.Status 200
Check "paper has 2 questions" $r.Json.totalAvailable 2
if ($r.Raw -match '"correct"') { Ko "paper leaks correct answers" } else { Ok "paper hides correct answers" }
Check "student cannot read question bank -> 403" (Call GET "$QuestionUrl/api/questions?subjectId=$subject" $ts).Status 403

# Question 1 answered correctly, question 2 wrongly -> 50 %.
$submission = @{
    subjectId = $subject
    answers = @(
        @{ questionId = $q1.Json.id; selectedOptionId = ($q1.Json.options | Where-Object { $_.correct })[0].id },
        @{ questionId = $q2.Json.id; selectedOptionId = ($q2.Json.options | Where-Object { -not $_.correct })[0].id }
    )
}
$r = Call POST "$ExamUrl/api/exams/submissions" $ts $submission
Check "submit exam (exam -> question service call)" $r.Status 201
$session = $r.Json.sessionId
Check "correct answers" $r.Json.correctAnswers 1
Check "score percent" ([double]$r.Json.scorePercent) 50

Check "student reads own result" (Call GET "$ExamUrl/api/exams/sessions/$session" $ts).Status 200
Check "org admin reads result" (Call GET "$ExamUrl/api/exams/sessions/$session" $ta).Status 200
Check "student history lists the attempt" (Call GET "$ExamUrl/api/exams/sessions" $ts).Json.page.totalElements 1

Section "Cleanup"
Check "delete question 1" (Call DELETE "$QuestionUrl/api/questions/$($q1.Json.id)" $ta).Status 204
Check "delete question 2" (Call DELETE "$QuestionUrl/api/questions/$($q2.Json.id)" $ta).Status 204
Check "delete subject" (Call DELETE "$OrgUrl/api/subjects/$subject" $ta).Status 204
Check "delete class" (Call DELETE "$OrgUrl/api/classes/$class" $ta).Status 204
Check "deactivate student" (Call DELETE "$AuthUrl/api/users/$studentId" $sa).Status 204
Check "deactivate org admin" (Call DELETE "$AuthUrl/api/users/$adminId" $sa).Status 204
Check "deactivate organisation" (Call DELETE "$OrgUrl/api/orgs/$org" $sa).Status 204

Section "Result"
Write-Host "  passed: $($script:Pass)   failed: $($script:Fail)"
if ($script:Fail -eq 0) { Write-Host "  DEPLOYMENT VERIFIED" -ForegroundColor Green; exit 0 }
Write-Host "  VERIFICATION FAILED" -ForegroundColor Red
exit 1
