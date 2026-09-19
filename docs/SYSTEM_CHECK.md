# Full system check — 19 Sep 2026

Checked services, the seeded API walkthrough, both frontend portals, and the default Maven suite.

**Result: pass.** The documented cycle works end to end. Live cash/card pay and a fresh CSV upload were not re-run this pass.

## Services

| Component | Address | Status |
| --- | --- | --- |
| Backend (Docker `app`) | http://localhost:8080 | Up. `GET /` is 401 without a token (expected). |
| Swagger UI | http://localhost:8080/swagger-ui.html | 302 → UI |
| OpenAPI JSON | http://localhost:8080/v3/api-docs | 200 |
| Frontend | http://localhost:5173 | 200 |
| Bank mock | http://localhost:8000 | 200 |
| PostgreSQL | localhost:5433 | Healthy |
| pgAdmin | http://localhost:5051 | 302 |

Flyway history: V1–V7, V9–V11 all successful. There is no V8.

## API walkthrough

`./scripts/walkthrough-cycle.sh` — **all checks passed**.

| Actor | What was verified |
| --- | --- |
| `nadia` ADMIN | Institutions include Nile Valley and Maadi; daily report; outstanding collections; gateway checks; Karim listed; Laila listed under Nile. |
| `karim` BANK_OFFICER | Fee search `29805150101023` returns Nour Khaled Fahmy and Malak Hany Sobhy at Nile Valley. Mock lookup returns Mona Samir Abdelrahman. Payment history includes `RCP-2026-WALK0001`. |
| `laila` INSTITUTION_OFFICER | Same students; fee record; Due / Partial / Paid collections; daily report names; student receipts `RCP-2026-WALK0001`; upload history. |
| `heba` INSTITUTION_OFFICER | Same national ID search is empty (Maadi isolation). |
| `mona` PAYER | `GET /fee-searches/me` returns both children; receipt `RCP-2026-WALK0003`. |

## Portal UI

Password for seeded logins: `Admin123!`

### Back office — `nadia`

http://localhost:5173/backoffice/login → Overall Dashboard

- Sidebar: Overall Dashboard, Create Institution, Create Officer. Find Fees hidden.
- KPIs: 3 institutions, **13,500 EGP** collected today, 3 payments, **34,000 EGP** outstanding.
- Institution filter includes Nile Valley School, Maadi Language School, Closed Nile University.
- Create Institution and Create Officer forms load.
- Earlier admin POST created officer **`sara`** (`BANK_OFFICER`, branch `zamalek`, ACTIVE).

### Back office — `karim`

- Lands on Find Fees (Create Institution / Create Officer hidden).
- Search `29805150101023` → Nour Khaled Fahmy (EGP 23,300) and Malak Hany Sobhy (EGP 20,700), Nile Valley School.
- Client Overview: **Mona Samir Abdelrahman**, paid this month 13,500 EGP, receipts WALK0001–0003.

### Institution — `laila`

http://localhost:5173/institution/login

- Dashboard collected today **13,500 EGP** / 3 payments.
- Customers search same NID → both Nile Valley students.
- Collections Due / Partial / Paid populated (Due includes Malak; Partial is Nour tuition 2026-09 remaining 10,000; Paid has Activities 4,000 and Other 1,500).
- Student record for Nour; **View receipts** shows WALK0001 / 0002 / 0003 (view only, no pay).
- Submit dues page loads (CSV template / upload). Not re-uploaded this pass. 2026-10 dues are already present from an earlier upload.

### Institution — `heba`

- Dashboard collected today **0 EGP**, outstanding 0 (no Nile Valley data).
- API fee search for the same NID is empty.

## Maven tests

`POSTGRES_PORT=5433 ./mvnw test` (integration group excluded, as in `pom.xml`)

- **410 tests, 0 failures, 0 errors** across 61 suites.
- `@Tag("integration")` bank-mock suites were not run. Older surefire files from 16 Sep still show those as failing against a different mock shape; they are stale and excluded.

## Not re-tested this pass

- Live cash / card / source / EPP charge from the UI
- Fresh `LIVE_UPLOAD=1` CSV this run
- Payer UI (there is no payer portal)
- pgAdmin browsing
- Integration-tagged mock adapter tests

## Notes

- JWT lifetime is 15 minutes; institution session expired once during the UI pass and needed a re-login.
- Create Officer UI also collects national ID, email, and mobile. The backend create-employee API stores name, branch, role, username, and password only.
- Walkthrough inputs stay in [PORTAL_WALKTHROUGH.md](PORTAL_WALKTHROUGH.md).
