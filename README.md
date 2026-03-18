# Alihsan Java Backend (Prime Integration BFF)

Spring Boot backend that sits between mobile frontend and Prime/Frappe.

## Why this structure

- Keeps credentials on backend only.
- Uses idempotent payment-intent flow.
- Final business workflow should be triggered through Prime method APIs, not random direct writes.

## Current APIs

- `POST /api/healthcare/appointments`
  - Creates payment intent + initiates payment.
- `POST /api/payment/webhook`
  - Confirms payment and triggers Prime workflow creation.
- `GET /api/payment/verify/{referenceId}`
  - Checks transaction status from Waafi (`HPP_GETTRANINFO`).
- `GET /api/healthcare/appointments?patientId=...`
  - Returns patient appointments.
- `GET /api/healthcare/lab-reports?patientId=...`
  - Returns lab reports.
- `GET /health`

## Required Prime method

This backend expects a whitelisted method in Prime:

- `prime.mobile_api.create_que_from_mobile`
- `prime.mobile_api.get_lab_reports_for_mobile`

Expected response shape:

```json
{
  "message": {
    "que": "QUE-0001",
    "invoice": "ACC-SINV-0001"
  }
}
```

## Environment Variables

- `PORT`
- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- `PRIME_BASE_URL`
- `PRIME_API_KEY`
- `PRIME_API_SECRET`
- `WAAFI_URL`
- `WAAFI_MERCHANT_UID`
- `WAAFI_API_USER_ID`
- `WAAFI_API_KEY`
- `WAAFI_CHANNEL_NAME`
- `HORMUUD_SMS_USERNAME` (optional for SMS notifications)
- `HORMUUD_SMS_PASSWORD` (optional for SMS notifications)
- `HORMUUD_SMS_SENDER_ID` (optional for SMS notifications)

## Run

```bash
mvn spring-boot:run
```

## Docker

1. Copy env template:

```bash
cp .env.example .env
```

2. Update `.env` values (Prime + Waafi credentials).

3. Build and start:

```bash
docker compose up --build -d
```

4. Logs:

```bash
docker compose logs -f app
```

5. Stop:

```bash
docker compose down
```

## Notes

- `PaymentProviderService` now sends real Waafi purchase + status-check requests.
- Webhook should be your source of truth for payment approval.
- Avoid in-memory flow for production payment state.

## CI/CD to Kubernetes (GitOps)

Workflow file: `.github/workflows/build-deploy.yml`

Flow:

1. Push to `main` in this repo.
2. GitHub Actions builds Docker image and pushes to GHCR:
   - `ghcr.io/<owner>/alihsan-java-backend:<short-sha>`
   - `ghcr.io/<owner>/alihsan-java-backend:latest`
3. Workflow updates image tag in:
   - `argocd/erpnext-gitops/system/alihsan-java-backend/deployment.yaml`
4. ArgoCD detects commit and syncs to cluster.

Required repository secrets (in this repo):

- `ARGOCD_REPO_TOKEN`: PAT with write access to `AyoubDahir/argocd`.
