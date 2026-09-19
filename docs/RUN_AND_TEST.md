# Run and test the backend

This is for someone who already has this repository. There is no frontend in
this repo. Portals, if you run them separately, talk to `http://localhost:8080`.

## 1. Prerequisites

- Docker Desktop (or Docker Engine with Compose 2.23.1+)
- Optional: a bank mock on port `8000` (`wit-mock-services`) for customer
  lookup, card, source, EPP, and MOI
- Optional: the frontend on port `5173` if you want to click through the UI

## 2. Start the API

From `tuitionnetwork`:

```sh
export JWT_SECRET_BASE64='MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE='
export NATIONAL_ID_HMAC_KEY_BASE64='YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXpBQkNERUY='
export NATIONAL_ID_ENCRYPTION_KEY_BASE64='MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY='

docker compose up -d --build
docker compose logs -f app
```

Wait until the log says `Started TuitionnetworkApplication`.

| Service | URL |
| --- | --- |
| API | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui.html |
| pgAdmin | http://localhost:5051 |
| Postgres (host) | localhost:5433 |
| Bank mock | http://localhost:8000 |

Do not reuse those crypto values outside local development.

## 3. Seed walkthrough data

The seed wipes previous demo rows, then inserts the accounts below.

```sh
docker compose exec -T postgres psql -U tuition -d tuition < scripts/seed-dev-data.sql
```

Password for every seeded login: **`Admin123!`**

| Username | Role | Where |
| --- | --- | --- |
| `nadia` | ADMIN | Back office |
| `karim` | BANK_OFFICER | Back office |
| `laila` | INSTITUTION_OFFICER | Nile Valley School |
| `heba` | INSTITUTION_OFFICER | Maadi Language School |
| `mona` | PAYER | API only (`GET /api/v1/fee-searches/me`) |

Login:

```sh
curl -sS -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"karim","password":"Admin123!"}'
```

Use `Authorization: Bearer <accessToken>` on every other `/api/v1/**` call.
Swagger is open; authorize there with the same token.

## 4. Seeded national IDs (mock-aligned)

Happy path (Mona already has receipts today):

| Who | National ID | What Find Fees returns |
| --- | --- | --- |
| Mona Samir Abdelrahman | `29805150101023` | Nour Khaled Fahmy, Malak Hany Sobhy (Nile Valley) |
| Nour (student) | `30103222103442` | — |
| Malak (student) | `31204010102041` | — |

Other mock customers, now linked to students:

| Who | National ID | Students | Bank mock source |
| --- | --- | --- | --- |
| Youssef Adel Nabil | `30007091301775` | Omar Youssef Adel `31005202112358`, Farah Youssef Adel `31011142124688` | Current account + **debit** card (no EPP) |
| Ahmed Tarek Mahmoud | `29511020204536` | Karim Ahmed Tarek `31108120213419` | Frozen account, blocked Visa, working Mastercard |
| Salma Hosny Ibrahim | `29001301202283` | Lina Salma Hosny `31201181224685` | Dormant account, expired card |
| Heba Mostafa Zaki | `28809252506666` | Yassin Mostafa Zaki `30903032512350` at **Maadi** | No accounts; card returns HTTP 502 |

Unknown 14-digit ID used by the script (no fees): `29901011234567`.

## 5. Automated checks (do this first)

From `tuitionnetwork`:

```sh
./scripts/walkthrough-cycle.sh
./scripts/user-story-check.sh
```

`walkthrough-cycle.sh` is read-only. It logs in as each seeded user and checks:

- Admin institutions, daily report, outstanding collections, officers
- Invalid password is rejected
- Karim fee search for Mona, Youssef, Ahmed, Salma, Heba
- Unknown national ID returns `[]`
- Short national ID returns HTTP 400
- Customer lookup for Youssef / Ahmed / Salma (skipped if the mock is down)
- Laila sees Nile students, not Maadi’s Yassin
- Heba does not see Mona’s Nile children, and does see Yassin
- Mona can list her own students and receipt `RCP-2026-WALK0003`

Run both scripts several times; they should stay green without reseeding unless
you took a live payment or uploaded a CSV.

```sh
for i in 1 2 3 4 5; do
  echo "===== check $i ====="
  ./scripts/walkthrough-cycle.sh || exit 1
  ./scripts/user-story-check.sh || exit 1
done
```

Optional live CSV (period 2026-10; do not loop this without reseeding):

```sh
LIVE_UPLOAD=1 ./scripts/walkthrough-cycle.sh
```

Maven (default suite, integration-tagged mock tests excluded by `pom.xml`):

```sh
POSTGRES_PORT=5433 ./mvnw test
```

## 6. User-story API pass (same seed)

`./scripts/user-story-check.sh` covers the stories below against the running
API. It creates one unpaid cash order for Omar’s Nile tuition with a fixed
idempotency key, so repeating it replays instead of double-charging.

Stories in `Tuition_Fees_Collection_Network_MVP_User_Stories`. Map them to the
running API like this:

| Story | What to call / expect |
| --- | --- |
| BO-US-01 / INST-US-01 | Login `karim` / `laila`; wrong password → 401 |
| BO-US-02 | `POST /api/v1/fee-searches` with Mona’s NID; `[]` for `29901011234567`; 400 for `123` |
| BO-US-03 | Same search is grouped by student and `feesByType` |
| PAYER-US-02 | Login `mona`, `GET /api/v1/fee-searches/me` |
| BO-US-04 | `POST /api/v1/payment-orders` with `Idempotency-Key` (creates a CREATED order) |
| BO-US-05 / BO-US-06 | `POST .../pay-cash` or `.../pay` / `.../pay-source` |
| INST-US-02 | Template is `scripts/dues-upload-template.csv` (same columns as the sample upload) |
| INST-US-03 / INST-US-04 | `GET /api/v1/integration/uploads` as `laila` (history + rejected-row reasons) |
| INST-US-05 | Re-upload the same file after `LIVE_UPLOAD=1` (duplicate checksum) |
| INST-US-06 / INST-US-07 | Student fee record + collections Due/Partial/Paid |
| INST-US-08 | `GET /api/v1/reporting/daily-collections` plus `/csv` and `/pdf` |
| ADMIN-US-01 / ADMIN-US-02 | `nadia`: institutions and back-office employees |

Do **not** leave unpaid CREATED orders around if you want the walkthrough loop
to stay identical; reseed after live pays.

## 7. Manual UI (if the frontend is running)

See [PORTAL_WALKTHROUGH.md](PORTAL_WALKTHROUGH.md). Short version:

1. `nadia` — dashboard, create institution/officer, no Find Fees
2. `karim` — Find Fees Mona, then Youssef / Ahmed / Salma / Heba
3. `laila` — dashboard, customers, collections, receipts, submit dues
4. `heba` — Mona search empty; Heba NID shows Yassin

After a successful pay, Student Fee Accounts should show **Paid** without a
fresh national-ID search (frontend caches a refreshed search).

## 8. Mock failure cards (need `:8000`)

Expiry **12/2030**, CVV **123** unless you are testing expiry.

| Card | Result |
| --- | --- |
| `4000000000000002` | Declined — insufficient funds |
| `4000000000009995` | Declined — do not honour |
| `4000000000000101` | Declined — incorrect CVV |
| `4000000000000069` | Declined — expired card |
| `4000000000000119` | HTTP 502 issuer unavailable |
| `4000000000003220` | 3-D Secure, OTP `123456` |

Youssef can pay from `acc_youssef_current` or his debit card; EPP should be
refused. Ahmed’s frozen account and blocked Visa should fail. Salma has nothing
usable. Heba’s on-file card returns 502.

## 9. If something fails

| Symptom | Check |
| --- | --- |
| App 401 on `/` | Expected without a token |
| Login 401 | Reseed; password is `Admin123!` |
| Youssef search empty | Reseed; he is not in older seed files |
| Lookup empty / 500 | Start the mock on `:8000` |
| Walkthrough wants `RCP-2026-WALK0001` | Reseed; live payments do not replace those refs |
| Maven cannot reach Postgres | `POSTGRES_PORT=5433` and Compose Postgres is healthy |
