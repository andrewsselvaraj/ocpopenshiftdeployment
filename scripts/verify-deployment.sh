#!/usr/bin/env bash
# Verifies a deployment of the online-exam microservices.
#
#   OpenShift (discovers Routes, checks rollouts/pods/health, then runs the API flow):
#     ADMIN_EMAIL=... ADMIN_PASSWORD=... ./scripts/verify-deployment.sh --openshift [-n online-exam]
#
#   Any environment (docker compose, Ingress, port-forward, local JVMs):
#     AUTH_URL=http://localhost:8080 ORG_URL=http://localhost:8082 \
#     QUESTION_URL=http://localhost:8084 EXAM_URL=http://localhost:8086 \
#     ADMIN_EMAIL=... ADMIN_PASSWORD=... ./scripts/verify-deployment.sh
#
# ADMIN_EMAIL / ADMIN_PASSWORD: a super admin (e.g. the BOOTSTRAP_ADMIN_* account).
# INSECURE=true skips TLS verification (self-signed cluster certificates).
# The API flow creates an organisation, users, a class, a subject and questions with a unique
# suffix, takes an exam as a student, checks the score, and deletes what it created.
# Requires only bash and curl (plus oc for --openshift).

set -uo pipefail

OPENSHIFT=false
NAMESPACE=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --openshift) OPENSHIFT=true ;;
    -n|--namespace) NAMESPACE="$2"; shift ;;
    -h|--help) sed -n '2,20p' "$0"; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; exit 2 ;;
  esac
  shift
done

SERVICES=(auth-service org-service question-service exam-service)
PASS=0
FAIL=0
CURL_OPTS=(-sS --max-time 30)
[[ "${INSECURE:-false}" == "true" ]] && CURL_OPTS+=(-k)
TMP=$(mktemp)
trap 'rm -f "$TMP"' EXIT

green() { printf '\033[32m%s\033[0m\n' "$*"; }
red() { printf '\033[31m%s\033[0m\n' "$*"; }
section() { printf '\n\033[1m== %s ==\033[0m\n' "$*"; }
ok() { PASS=$((PASS + 1)); green "  PASS  $*"; }
ko() { FAIL=$((FAIL + 1)); red "  FAIL  $*"; }
check() { if [[ "$2" == "$3" ]]; then ok "$1"; else ko "$1 (expected $3, got $2)"; fi; }

# call METHOD URL [TOKEN] [JSON] -> sets STATUS and BODY
call() {
  local args=("${CURL_OPTS[@]}" -X "$1" -o "$TMP" -w '%{http_code}' -H 'Accept: application/json')
  [[ -n "${3:-}" ]] && args+=(-H "Authorization: Bearer $3")
  [[ -n "${4:-}" ]] && args+=(-H 'Content-Type: application/json' --data "$4")
  STATUS=$(curl "${args[@]}" "$2" 2>/dev/null) || STATUS="000"
  BODY=$(cat "$TMP" 2>/dev/null || true)
}
# Minimal JSON helpers for flat string/number fields (no jq dependency).
jstr() { printf '%s' "$1" | grep -o "\"$2\":\"[^\"]*\"" | head -1 | cut -d'"' -f4; }
jnum() { printf '%s' "$1" | grep -o "\"$2\":[0-9.]*" | head -1 | cut -d: -f2; }
ids() { printf '%s' "$1" | grep -o '"id":"[^"]*"' | cut -d'"' -f4; }

# ---------------------------------------------------------------------------------------------
if $OPENSHIFT; then
  command -v oc >/dev/null || { red "oc CLI not found"; exit 2; }
  OC=(oc)
  [[ -n "$NAMESPACE" ]] && OC+=(-n "$NAMESPACE")
  NS=${NAMESPACE:-$(oc project -q)}

  section "Cluster access"
  if oc whoami >/dev/null 2>&1; then ok "logged in as $(oc whoami) on $(oc whoami --show-server), project $NS"
  else red "Not logged in - run: oc login --token=... --server=..."; exit 2; fi

  section "Builds and images"
  for s in "${SERVICES[@]}"; do
    phase=$("${OC[@]}" get builds -l "buildconfig=$s" --sort-by=.metadata.creationTimestamp \
      -o jsonpath='{.items[-1:].status.phase}' 2>/dev/null)
    if [[ -n "$phase" ]]; then check "$s latest build" "$phase" "Complete"; else echo "  info  $s: no in-cluster build (external image)"; fi
  done

  section "Rollouts and pods"
  for s in "${SERVICES[@]}"; do
    if "${OC[@]}" rollout status "deploy/$s" --timeout=240s >/dev/null 2>&1; then ok "$s rolled out"
    else ko "$s rollout not complete"; fi
    want=$("${OC[@]}" get deploy "$s" -o jsonpath='{.spec.replicas}' 2>/dev/null)
    ready=$("${OC[@]}" get deploy "$s" -o jsonpath='{.status.readyReplicas}' 2>/dev/null)
    check "$s ready replicas" "${ready:-0}" "${want:-?}"
    restarts=$("${OC[@]}" get pods -l "app.kubernetes.io/name=$s" \
      -o jsonpath='{range .items[*]}{.status.containerStatuses[0].restartCount}{"\n"}{end}' 2>/dev/null | sort -n | tail -1)
    check "$s container restarts" "${restarts:-0}" "0"
  done
  db_ready=$("${OC[@]}" get statefulset mysql -o jsonpath='{.status.readyReplicas}' 2>/dev/null)
  [[ -n "$db_ready" ]] && check "mysql ready replicas" "$db_ready" "1"

  section "Actuator health (through the API server service proxy)"
  for s in "${SERVICES[@]}"; do
    h=$(oc get --raw "/api/v1/namespaces/$NS/services/http:$s:management/proxy/actuator/health" 2>/dev/null)
    if [[ -n "$h" ]]; then check "$s /actuator/health" "$(jstr "$h" status)" "UP"
    else echo "  info  $s: service proxy not permitted, readiness probe status above is authoritative"; fi
  done

  section "Routes"
  for s in "${SERVICES[@]}"; do
    host=$("${OC[@]}" get route "$s" -o jsonpath='{.spec.host}' 2>/dev/null)
    if [[ -n "$host" ]]; then ok "$s route https://$host"; else ko "$s has no route"; fi
    var="$(echo "${s%-service}" | tr '[:lower:]' '[:upper:]')_URL"
    [[ -z "${!var:-}" && -n "$host" ]] && printf -v "$var" '%s' "https://$host"
  done
fi

: "${AUTH_URL:?AUTH_URL not set}" "${ORG_URL:?ORG_URL not set}"
: "${QUESTION_URL:?QUESTION_URL not set}" "${EXAM_URL:?EXAM_URL not set}"
: "${ADMIN_EMAIL:?ADMIN_EMAIL not set}" "${ADMIN_PASSWORD:?ADMIN_PASSWORD not set}"

# ---------------------------------------------------------------------------------------------
section "Security baseline"
call GET "$AUTH_URL/api/users";                  check "no token -> 401" "$STATUS" "401"
call GET "$AUTH_URL/api/users" "not.a.jwt";      check "invalid token -> 401" "$STATUS" "401"
call POST "$AUTH_URL/api/auth/login" "" '{"email":"nobody@example.com","password":"wrong-password"}'
check "bad credentials -> 401" "$STATUS" "401"

section "Business flow"
RUN="v$(date +%s)"
STUDENT_PW="Student#$RUN"
ADMINU_PW="OrgAdmin#$RUN"

call POST "$AUTH_URL/api/auth/login" "" "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}"
check "super admin login" "$STATUS" "200"
SA=$(jstr "$BODY" accessToken)
[[ -z "$SA" ]] && { red "Cannot continue without a super admin token"; exit 1; }

call POST "$ORG_URL/api/orgs" "$SA" "{\"name\":\"Verify $RUN\",\"email\":\"org-$RUN@verify.local\"}"
check "create organisation" "$STATUS" "201"; ORG=$(jstr "$BODY" id)

call POST "$AUTH_URL/api/users" "$SA" "{\"orgId\":\"$ORG\",\"name\":\"Verify Admin\",\"email\":\"admin-$RUN@verify.local\",\"password\":\"$ADMINU_PW\",\"role\":\"ADMIN\"}"
check "create org admin" "$STATUS" "201"; ADMIN_ID=$(jstr "$BODY" id)
call POST "$AUTH_URL/api/users" "$SA" "{\"orgId\":\"$ORG\",\"name\":\"Verify Student\",\"email\":\"student-$RUN@verify.local\",\"password\":\"$STUDENT_PW\",\"role\":\"USER\"}"
check "create student" "$STATUS" "201"; STUDENT_ID=$(jstr "$BODY" id)

call POST "$AUTH_URL/api/auth/login" "" "{\"email\":\"admin-$RUN@verify.local\",\"password\":\"$ADMINU_PW\"}"
check "org admin login" "$STATUS" "200"; TA=$(jstr "$BODY" accessToken)
call POST "$AUTH_URL/api/auth/login" "" "{\"email\":\"student-$RUN@verify.local\",\"password\":\"$STUDENT_PW\"}"
check "student login" "$STATUS" "200"; TS=$(jstr "$BODY" accessToken)

call POST "$ORG_URL/api/classes" "$TA" "{\"orgId\":\"$ORG\",\"name\":\"Class $RUN\"}"
check "create class" "$STATUS" "201"; CLASS=$(jstr "$BODY" id)
call POST "$ORG_URL/api/subjects" "$TA" "{\"classId\":\"$CLASS\",\"name\":\"Science $RUN\"}"
check "create subject" "$STATUS" "201"; SUBJECT=$(jstr "$BODY" id)
call POST "$ORG_URL/api/classes" "$TS" "{\"orgId\":\"$ORG\",\"name\":\"Nope\"}"
check "student cannot create class -> 403" "$STATUS" "403"

question() {
  call POST "$QUESTION_URL/api/questions" "$TA" "{\"classId\":\"$CLASS\",\"subjectId\":\"$SUBJECT\",\"text\":\"$1\",\"options\":[{\"text\":\"$2\",\"correct\":true},{\"text\":\"$3\",\"correct\":false}]}"
}
question "Which is the biggest planet?" "Jupiter" "Mars"
check "create question 1" "$STATUS" "201"; mapfile -t Q1 < <(ids "$BODY")   # [question, correct, wrong]
question "How many wheels does a scooter have?" "2" "3"
check "create question 2" "$STATUS" "201"; mapfile -t Q2 < <(ids "$BODY")

call GET "$QUESTION_URL/api/question-papers?subjectId=$SUBJECT&limit=10" "$TS"
check "student gets question paper" "$STATUS" "200"
check "paper has 2 questions" "$(jnum "$BODY" totalAvailable)" "2"
if grep -q '"correct"' <<<"$BODY"; then ko "paper leaks correct answers"; else ok "paper hides correct answers"; fi
call GET "$QUESTION_URL/api/questions?subjectId=$SUBJECT" "$TS"
check "student cannot read question bank -> 403" "$STATUS" "403"

# Answer question 1 correctly and question 2 wrongly -> 50 %.
call POST "$EXAM_URL/api/exams/submissions" "$TS" "{\"subjectId\":\"$SUBJECT\",\"answers\":[{\"questionId\":\"${Q1[0]}\",\"selectedOptionId\":\"${Q1[1]}\"},{\"questionId\":\"${Q2[0]}\",\"selectedOptionId\":\"${Q2[2]}\"}]}"
check "submit exam (exam -> question service call)" "$STATUS" "201"
SESSION=$(jstr "$BODY" sessionId)
check "correct answers" "$(jnum "$BODY" correctAnswers)" "1"
check "score percent" "$(jnum "$BODY" scorePercent)" "50.0"

call GET "$EXAM_URL/api/exams/sessions/$SESSION" "$TS";  check "student reads own result" "$STATUS" "200"
call GET "$EXAM_URL/api/exams/sessions/$SESSION" "$TA";  check "org admin reads result" "$STATUS" "200"
call GET "$EXAM_URL/api/exams/sessions" "$TS"
check "student history lists the attempt" "$(jnum "$BODY" totalElements)" "1"

section "Cleanup"
call DELETE "$QUESTION_URL/api/questions/${Q1[0]}" "$TA"; check "delete question 1" "$STATUS" "204"
call DELETE "$QUESTION_URL/api/questions/${Q2[0]}" "$TA"; check "delete question 2" "$STATUS" "204"
call DELETE "$ORG_URL/api/subjects/$SUBJECT" "$TA";       check "delete subject" "$STATUS" "204"
call DELETE "$ORG_URL/api/classes/$CLASS" "$TA";          check "delete class" "$STATUS" "204"
call DELETE "$AUTH_URL/api/users/$STUDENT_ID" "$SA";      check "deactivate student" "$STATUS" "204"
call DELETE "$AUTH_URL/api/users/$ADMIN_ID" "$SA";        check "deactivate org admin" "$STATUS" "204"
call DELETE "$ORG_URL/api/orgs/$ORG" "$SA";               check "deactivate organisation" "$STATUS" "204"

section "Result"
echo "  passed: $PASS   failed: $FAIL"
if [[ $FAIL -eq 0 ]]; then green "  DEPLOYMENT VERIFIED"; exit 0; else red "  VERIFICATION FAILED"; exit 1; fi
