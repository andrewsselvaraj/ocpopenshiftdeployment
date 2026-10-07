# Deployment Guide – Online Exam Microservices

Audience: developers and DevOps / platform engineers who build, ship and operate the four services
(`auth-service`, `org-service`, `question-service`, `exam-service`) on Docker, Kubernetes and OpenShift.

| Section | What it covers |
|---|---|
| [1. Prerequisites](#1-prerequisites) | Tools and access |
| [2. Configuration reference](#2-configuration-reference) | Environment variables, secrets, ports |
| [3. Build and test (Maven)](#3-build-and-test-maven) | Local build, tests, running without containers |
| [4. Docker](#4-docker) | Images and the docker-compose stack |
| [5. Kubernetes](#5-kubernetes) | Plain Kubernetes deployment with Ingress |
| [6. OpenShift (OCP)](#6-openshift-ocp) | Project, database, builds, Routes, monitoring |
| [7. Verifying the deployment](#7-verifying-the-deployment) | Automated verifier and manual checks |
| [8. CI/CD pipeline](#8-cicd-pipeline-github-actions) | GitHub Actions → GHCR → OpenShift |
| [9. Day-2 operations](#9-day-2-operations) | Rollout, rollback, scaling, logs, DB migrations |
| [10. Troubleshooting](#10-troubleshooting) | Symptoms and fixes |
| [11. Production readiness checklist](#11-production-readiness-checklist) | Gaps to close before go-live |

---

## 1. Prerequisites

| Tool | Version | Needed for |
|---|---|---|
| JDK | 17 (the locally installed Microsoft OpenJDK 17 is fine) | building and running |
| Maven | none to install – use `./mvnw` (`mvnw.cmd` on Windows); it downloads Maven 3.9.16 | building |
| Docker or Podman | Docker 24+ / Podman 4+ | section 4, local images |
| `kubectl` | 1.28+ | section 5 |
| `oc` | 4.12+ (matching the cluster) | section 6 – download from the OCP console: **?** → *Command line tools* |
| Cluster access | project `admin` (OCP) / namespace admin (k8s) | sections 5–6 |
| Legacy dump | `m2itssql_complete.sql` | loading existing data |

> **The dump contains personal data and plain-text passwords.** It is listed in `.gitignore`; never commit it,
> copy it to shared drives, or bake it into an image. Load it straight into the database as shown below.

## 2. Configuration reference

All services read the same variables (Spring relaxed binding: any property can also be set as an env var).

| Variable | Default | Secret? | Purpose |
|---|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `3306` / `m2its` | no | MySQL location |
| `DB_USER` / `DB_PASSWORD` | `m2its` / – | **yes** | MySQL credentials |
| `DB_ROOT_PASSWORD` | – | **yes** | only for the bundled MySQL StatefulSet |
| `DB_USE_SSL` | `false` | no | `true` for managed databases that require TLS |
| `DB_POOL_SIZE` | `10` | no | Hikari pool per pod – keep `replicas × services × pool` below MySQL `max_connections` |
| `JWT_SECRET` | – (required) | **yes** | HS256 key shared by all services, ≥ 32 chars. `openssl rand -base64 48` |
| `JWT_ISSUER` | `m2i-online-exam` | no | `iss` claim, validated by every service |
| `JWT_TTL` | `1h` | no | access-token lifetime |
| `BOOTSTRAP_ADMIN_EMAIL` / `BOOTSTRAP_ADMIN_PASSWORD` | empty | **yes** | auth-service creates this super admin on start if it does not exist |
| `QUESTION_SERVICE_URL` | `http://question-service:8080` | no | used by exam-service |
| `LOGGING_STRUCTURED_FORMAT_CONSOLE` | unset (plain text) | no | `ecs` = JSON logs (set in the cluster ConfigMap) |
| `API_DOCS_ENABLED` | `true` | no | Swagger UI / OpenAPI; set `false` in production if not wanted |
| `SPRING_PROFILES_ACTIVE` | none | no | `local` = in-memory H2, no external dependencies (dev/tests only) |

Ports: **8080** = API (exposed by Route/Ingress), **8081** = actuator (`/actuator/health/{liveness,readiness}`,
`/actuator/prometheus`) – never exposed outside the cluster.

## 3. Build and test (Maven)

```bash
./mvnw verify            # compiles all modules, runs 14 integration tests (H2, real JWTs, full security chain)
./mvnw -pl exam-service -am package -DskipTests   # build one service (+ the common module it needs)
```

Run all four services on one machine without Docker or MySQL (each gets its own in-memory database):

```bash
export SPRING_PROFILES_ACTIVE=local
BOOTSTRAP_ADMIN_EMAIL=root@local.test BOOTSTRAP_ADMIN_PASSWORD='Root#Pass1' SERVER_PORT=18080 java -jar auth-service/target/auth-service.jar &
SERVER_PORT=18082 java -jar org-service/target/org-service.jar &
SERVER_PORT=18084 java -jar question-service/target/question-service.jar &
SERVER_PORT=18086 QUESTION_SERVICE_URL=http://localhost:18084 java -jar exam-service/target/exam-service.jar &

AUTH_URL=http://localhost:18080 ORG_URL=http://localhost:18082 QUESTION_URL=http://localhost:18084 \
EXAM_URL=http://localhost:18086 ADMIN_EMAIL=root@local.test ADMIN_PASSWORD='Root#Pass1' \
  ./scripts/verify-deployment.sh
```

## 4. Docker

### 4.1 Image

One `Dockerfile` builds any service; choose it with `SERVICE`:

```bash
docker build --build-arg SERVICE=auth-service -t m2i/auth-service:1.0.0 .
```

- Build stage `ubi9/openjdk-17` runs `./mvnw package`; runtime stage `ubi9/openjdk-17-runtime` (~JRE only).
- The Spring Boot jar is extracted into layers (dependencies → loader → application) so a code change
  re-pushes only a few hundred KB.
- Runs as non-root UID 185 and works with OpenShift's random UID. `JAVA_TOOL_OPTIONS` sizes the heap to
  75 % of the container memory limit and exits on OOM so Kubernetes restarts the pod.
- Podman users: replace `docker` with `podman` – no other changes.

### 4.2 Full stack with docker compose

```bash
cp .env.example .env          # then edit every value
# m2itssql_complete.sql must be in the repository root - it is imported on the first start
docker compose up --build -d
docker compose ps             # wait until all services are "healthy"
```

Endpoints: auth `:8080`, org `:8082`, question `:8084`, exam `:8086` (Swagger: `/swagger-ui.html`).
Verify with the script (section 7) using those URLs and the `BOOTSTRAP_ADMIN_*` credentials from `.env`.
Reset the database: `docker compose down -v` (deletes the volume, the dump is re-imported on next start).

### 4.3 Push to a registry

```bash
for s in auth-service org-service question-service exam-service; do
  docker build --build-arg SERVICE=$s -t ghcr.io/<owner>/online-exam/$s:1.0.0 .
  docker push ghcr.io/<owner>/online-exam/$s:1.0.0
done
```

## 5. Kubernetes

Manifests use Kustomize (`kubectl apply -k`):

```
k8s/base                    Deployments (2 replicas, probes, limits, hardened securityContext), Services,
                            HPA (2-5 pods @ 70 % CPU), PDB, ConfigMap exam-config, NetworkPolicy
k8s/database/base           MySQL 8 StatefulSet + 5 Gi PVC (dev/test - see checklist for production)
k8s/database/kubernetes     + fsGroup for plain Kubernetes volumes
k8s/overlays/kubernetes     GHCR images, NGINX Ingress (one host, routed by path), ingress NetworkPolicy
```

```bash
kubectl create namespace online-exam
kubectl config set-context --current --namespace=online-exam

# 1. Secrets (never stored in git)
kubectl create secret generic exam-secrets \
  --from-literal=DB_USER=m2its \
  --from-literal=DB_PASSWORD="$(openssl rand -base64 24)" \
  --from-literal=DB_ROOT_PASSWORD="$(openssl rand -base64 24)" \
  --from-literal=JWT_SECRET="$(openssl rand -base64 48)" \
  --from-literal=BOOTSTRAP_ADMIN_EMAIL=platform.admin@yourdomain \
  --from-literal=BOOTSTRAP_ADMIN_PASSWORD='<strong password>'

# 2. Database, then load the legacy data BEFORE the services start (see 6.3 for why)
kubectl apply -k k8s/database/kubernetes
kubectl rollout status statefulset/mysql --timeout=5m
kubectl cp m2itssql_complete.sql mysql-0:/tmp/m2its.sql
kubectl exec mysql-0 -- bash -c 'mysql --default-character-set=utf8mb4 -h 127.0.0.1 -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" m2its < /tmp/m2its.sql && rm /tmp/m2its.sql'

# 3. Services (edit the image tags / Ingress host in k8s/overlays/kubernetes first)
kubectl apply -k k8s/overlays/kubernetes
kubectl get pods -w
```

If the GHCR packages are private, add a pull secret and attach it to the default service account:
`kubectl create secret docker-registry ghcr --docker-server=ghcr.io --docker-username=<user> --docker-password=<PAT>`
then `kubectl patch serviceaccount default -p '{"imagePullSecrets":[{"name":"ghcr"}]}'`.

The API is then available at `http://exam.example.com/api/...` (change the host in `ingress.yaml`; add TLS with cert-manager).

## 6. OpenShift (OCP)

The OpenShift overlay builds images **inside the cluster** (Docker-strategy BuildConfigs → ImageStreams) and
redeploys automatically when a new image lands (image-change triggers). Everything runs under the default
`restricted-v2` SCC – no extra privileges are requested.

### 6.1 Log in and create the project

```bash
oc login --token=<token> --server=https://api.<cluster>:6443     # copy from console: user menu -> Copy login command
oc new-project online-exam --display-name="Online Exam"
```

### 6.2 Secrets

```bash
oc create secret generic exam-secrets \
  --from-literal=DB_USER=m2its \
  --from-literal=DB_PASSWORD="$(openssl rand -base64 24)" \
  --from-literal=DB_ROOT_PASSWORD="$(openssl rand -base64 24)" \
  --from-literal=JWT_SECRET="$(openssl rand -base64 48)" \
  --from-literal=BOOTSTRAP_ADMIN_EMAIL=platform.admin@yourdomain \
  --from-literal=BOOTSTRAP_ADMIN_PASSWORD='<strong password>'
```

Windows PowerShell without `openssl`: generate a value with
`[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }) -as [byte[]])`.

### 6.3 Database and legacy data

```bash
oc apply -k k8s/database/base
oc rollout status statefulset/mysql --timeout=5m

oc cp m2itssql_complete.sql mysql-0:/tmp/m2its.sql
oc exec mysql-0 -- bash -c 'mysql --default-character-set=utf8mb4 -h 127.0.0.1 -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" m2its < /tmp/m2its.sql && rm /tmp/m2its.sql'
oc exec mysql-0 -- bash -c 'mysql -h 127.0.0.1 -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" m2its -e "select count(*) from question_info_v_two"'   # expect 1023
```

**Order matters:** import the dump *before* the services start the first time. The dump uses plain
`CREATE TABLE`, so it fails if Flyway has already created empty tables. (Use `oc cp` rather than piping the
file through PowerShell, which would re-encode the Tamil text in the dump.) To start without legacy data,
skip the import – Flyway creates empty tables and the bootstrap admin can log in.

On first start each service's Flyway baselines the existing schema and applies its own migrations:
widening password columns for BCrypt, adding a primary key to `contact_m2i`, and adding lookup indexes.

### 6.4 Deploy and build

```bash
# Optional dry run - validates Routes/BuildConfigs/ImageStreams against the cluster API
oc apply -k k8s/overlays/openshift --dry-run=server

oc apply -k k8s/overlays/openshift

# Build all four images from your working copy (no Git access needed from the cluster) ...
for s in auth-service org-service question-service exam-service; do
  oc start-build $s --from-dir=. --follow
done
# ... or from GitHub once the code is pushed (BuildConfigs point at main):
#   oc start-build auth-service --follow
```

Pods show `ImagePullBackOff` until their first build finishes; the image-change trigger then rolls them out.
Builds need outbound access to Maven Central and `registry.access.redhat.com`. In a disconnected cluster,
mirror the UBI images and point Maven at an internal repository (add a `settings.xml` with a `<mirror>` and pass
`-s settings.xml` to the `mvnw` call in the Dockerfile), or build the images in CI and import them (section 8).

```bash
oc get pods                       # 2 pods per service, READY 1/1
oc get routes                     # https://<service>-online-exam.apps.<cluster>
```

### 6.5 What gets created

| Kind | Objects |
|---|---|
| BuildConfig / ImageStream | one per service |
| Deployment | one per service, 2 replicas, rolling update with `maxUnavailable: 0` |
| Service | ports `http` 8080 and `management` 8081 |
| Route | edge TLS, HTTP → HTTPS redirect, API port only |
| HorizontalPodAutoscaler / PodDisruptionBudget | 2–5 replicas at 70 % CPU / `minAvailable: 1` |
| NetworkPolicy | same-namespace traffic, router → 8080, cluster monitoring → 8081 |
| ServiceMonitor | Prometheus scraping of `/actuator/prometheus` |
| ConfigMap `exam-config` | non-secret settings (section 2) |

### 6.6 Monitoring

User-workload monitoring must be enabled once per cluster (cluster-admin):

```bash
oc -n openshift-monitoring patch configmap cluster-monitoring-config --type merge \
  -p '{"data":{"config.yaml":"enableUserWorkload: true\n"}}'
```

(Create the ConfigMap first if it does not exist.) Then **Observe → Metrics** in the console, for example:
`sum by (application) (rate(http_server_requests_seconds_count{namespace="online-exam"}[5m]))`,
`exam_submissions_total`, `exam_score_percent_count`, `resilience4j_circuitbreaker_state`, `hikaricp_connections_active`.

Logs are JSON (ECS) and include `requestId`, so one request can be followed across services in the cluster
logging stack.

## 7. Verifying the deployment

### 7.1 Automated verifier

`scripts/verify-deployment.sh` (bash + curl) and `scripts/verify-deployment.ps1` (Windows PowerShell 5.1 / 7)
perform the same checks.

**Platform checks** (`--openshift` / `-OpenShift`): logged-in user and project, latest build per service is
`Complete`, rollouts finished, ready replicas = desired, zero container restarts, MySQL ready, actuator
`/health` = `UP` (via the API-server service proxy), a Route exists per service.

**Functional checks** through the public Routes (31 checks):

1. Security baseline – no token → 401, forged token → 401, bad password → 401.
2. Super admin logs in → creates an organisation → an org admin and a student.
3. Org admin creates a class and subject; the student is refused (403).
4. Org admin creates two questions; the student gets a question paper that **does not contain** correct
   answers and cannot read the question bank (403).
5. The student submits one right and one wrong answer → exam-service fetches answer keys from
   question-service (service-to-service JWT) → score must be **50 %**.
6. Student and org admin can read the result; the attempt appears in the student's history.
7. Everything created is deleted / deactivated again.

```bash
# bash (Linux, macOS, Git Bash, CI)
ADMIN_EMAIL=platform.admin@yourdomain ADMIN_PASSWORD='<password>' \
  ./scripts/verify-deployment.sh --openshift -n online-exam
```

```powershell
# Windows PowerShell
.\scripts\verify-deployment.ps1 -OpenShift -Namespace online-exam `
    -AdminEmail platform.admin@yourdomain -AdminPassword '<password>'
```

Add `INSECURE=true` / `-Insecure` when the cluster uses self-signed router certificates.
For Kubernetes / compose, pass the URLs instead of `--openshift` (`AUTH_URL`, `ORG_URL`, `QUESTION_URL`, `EXAM_URL`
or `-AuthUrl ...`). Exit code 0 = verified, 1 = at least one check failed – usable as a pipeline gate.

Expected tail of the output:

```
== Result ==
  passed: 31   failed: 0
  DEPLOYMENT VERIFIED
```

### 7.2 Manual checks

```bash
oc get pods -l app.kubernetes.io/part-of=online-exam -o wide
oc describe deploy/exam-service | grep -A3 Conditions
oc logs deploy/auth-service --tail=50          # "Started AuthServiceApplication", Flyway "Successfully applied"
oc get --raw /api/v1/namespaces/online-exam/services/http:auth-service:management/proxy/actuator/health

AUTH=https://$(oc get route auth-service -o jsonpath='{.spec.host}')
curl -s -X POST $AUTH/api/auth/login -H 'Content-Type: application/json' \
     -d '{"email":"platform.admin@yourdomain","password":"<password>"}'

oc port-forward svc/question-service 8080:8080     # then http://localhost:8080/swagger-ui.html
oc exec mysql-0 -- bash -c 'mysql -h127.0.0.1 -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" m2its -e "select * from flyway_history_auth"'
```

## 8. CI/CD pipeline (GitHub Actions)

`.github/workflows/ci.yml`:

| Job | Trigger | Steps |
|---|---|---|
| `build-test` | every push / PR | `./mvnw verify`, publishes test reports |
| `images` | push to `main` or a `v*` tag | per service: build, **Trivy scan (fails on fixable CRITICAL CVEs)**, push to `ghcr.io/<owner>/online-exam/<service>` tagged with the commit SHA (+ `latest` on main, version on tags) |
| `deploy-openshift` | push to `main`, when `OPENSHIFT_NAMESPACE` is configured | `oc apply -k k8s/overlays/openshift`, `oc tag` the commit's GHCR images into the ImageStreams (triggers rollouts), then runs `verify-deployment.sh --openshift` |

One-time setup:

```bash
# Service account the pipeline deploys with
oc create serviceaccount github-deployer -n online-exam
oc policy add-role-to-user edit -z github-deployer -n online-exam
oc create token github-deployer -n online-exam --duration=8760h     # -> OPENSHIFT_TOKEN

# If the GHCR packages are private, let OpenShift import/pull them
oc create secret docker-registry ghcr --docker-server=ghcr.io --docker-username=<user> --docker-password=<PAT read:packages> -n online-exam
oc secrets link default ghcr --for=pull -n online-exam
```

GitHub repository → Settings → *Environments* → `openshift`:

| Name | Type | Value |
|---|---|---|
| `OPENSHIFT_SERVER` | secret | `https://api.<cluster>:6443` |
| `OPENSHIFT_TOKEN` | secret | token from above |
| `VERIFY_ADMIN_EMAIL` / `VERIFY_ADMIN_PASSWORD` | secret | a super admin used by the verifier |
| `OPENSHIFT_NAMESPACE` | variable | `online-exam` |

Add required reviewers on the environment for a manual approval gate before production.
Alternative inside the cluster: OpenShift Pipelines (Tekton) with the `maven`, `buildah` and `openshift-client`
ClusterTasks follows the same three stages.

## 9. Day-2 operations

| Task | Command |
|---|---|
| Deploy a new version | push to `main` (CI) or `oc start-build <service> --from-dir=. --follow` |
| Watch a rollout | `oc rollout status deploy/<service>` |
| Roll back | `oc rollout undo deploy/<service>` (`oc rollout history deploy/<service>` lists revisions) |
| Pin a specific image | `oc tag <service>@sha256:<digest> <service>:latest` |
| Scale manually | `oc scale deploy/<service> --replicas=4` (the HPA overrides within 2–5; edit the HPA to change bounds) |
| Change configuration | `oc edit configmap exam-config` then `oc rollout restart deploy/<service>` |
| Rotate `JWT_SECRET` | update the secret, restart all four deployments together; users must log in again |
| Logs | `oc logs -f deploy/<service>`; across pods: `oc logs -l app.kubernetes.io/name=<service> --prefix` |
| DB backup (dev MySQL) | `oc exec mysql-0 -- bash -c 'mysqldump -h127.0.0.1 -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" --single-transaction m2its' > backup.sql` |

**Schema changes:** add `V<n>__description.sql` under the owning service's `src/main/resources/db/migration`.
Flyway applies it on the next rollout (each service holds a lock on its own history table, so concurrent pods are
safe). Keep migrations backwards compatible with the previous version (expand → deploy → contract) because
old and new pods overlap during a rolling update.

## 10. Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| Pod `ImagePullBackOff` right after `oc apply` | image not built yet | run the builds (6.4); the trigger redeploys |
| Build fails downloading dependencies | no egress to Maven Central / Red Hat registry | Maven mirror, proxy env vars on the BuildConfig, or build via CI |
| `CrashLoopBackOff`, log: `app.security.jwt.secret ... must be at least 32 characters` | `JWT_SECRET` missing/short | fix `exam-secrets`, `oc rollout restart` |
| Log: `Communications link failure` | DB not reachable / wrong `DB_HOST` | `oc get pods -l app.kubernetes.io/name=mysql`, check `exam-config` |
| Log: `Validate failed: Migrations have failed validation` | a released migration file was edited | never edit applied migrations; add a new version |
| Dump import: `Table 'admin_info' already exists` | services started before the import | for a fresh environment: delete the MySQL PVC and repeat 6.3 before 6.4 |
| Readiness stays false | DB health failing | `oc get --raw .../proxy/actuator/health` shows the failing component |
| 401 on every call | token from another environment or expired | log in again; check `JWT_ISSUER`/`JWT_SECRET` are identical for all services |
| 403 on another organisation's data | tenant isolation working as designed | use a super admin or a user of that organisation |
| 409 when creating a question | legacy FK: class/subject id does not exist | create/select the class and subject through org-service first |
| Exam submit → 503 "Question service is unavailable" | question-service down; circuit breaker open | check question-service pods; the breaker closes ~20 s after recovery |
| Route returns 503 "Application is not available" | no ready pods or NetworkPolicy blocks the router | `oc get endpoints <service>`; check `networkpolicy-openshift.yaml` labels exist on the router namespace |

## 11. Production readiness checklist

Already in place: non-root hardened containers, probes, resource limits, HPA, PDB, rolling updates without
downtime, NetworkPolicies, TLS at the edge, secrets out of git, BCrypt with automatic migration of legacy
passwords, tenant isolation, server-side scoring, retries + circuit breaker, metrics, JSON logs, request IDs,
versioned schema migrations, image scanning, automated post-deployment verification.

Close before go-live:

- [ ] **Database:** replace the single MySQL pod with a managed/HA MySQL (or an operator) with automated backups
      and point-in-time recovery; set `DB_USE_SSL=true`.
- [ ] **Token signing:** move from the shared HS256 secret to asymmetric keys (RS256 + JWKS) or Red Hat build of
      Keycloak, so only the identity service can mint tokens.
- [ ] **Login protection:** rate limiting / lockout on `POST /api/auth/login` (Route annotation
      `haproxy.router.openshift.io/rate-limit-connections` or an API gateway).
- [ ] **Legacy passwords:** accounts that never log in keep plain-text passwords. Force a reset for them after a
      grace period (`select pk_user_id from user_info where user_password not like '$2%'`).
- [ ] **Data quality:** four e-mails appear twice in `user_info` (`aravind@m2i.com`, `balachandar.saas@gmail.com`,
      `naveen@shss.com`, `siva@gmail.com`); login uses the most recently updated row. De-duplicate, then add a unique index.
- [ ] **Unmapped legacy tables:** `question_info` (v1), `answer_info_v_two_bk`, `main_table`, `dynamic_table`,
      `test_table_one`, `role_master` (empty) are not used by any service – archive or drop them.
- [ ] **Images:** deploy immutable tags/digests (CI already tags by commit SHA); keep `latest` for dev only.
- [ ] **Swagger:** set `API_DOCS_ENABLED=false` if the API contract must not be public.
- [ ] **Capacity:** load-test exam submission peaks and size `DB_POOL_SIZE`, replicas and HPA bounds accordingly.
