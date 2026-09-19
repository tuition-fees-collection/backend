# Full system check — 19 Sep 2026

Checked services, the seeded API walkthrough (including Youssef / Ahmed / Salma / Heba), MVP user-story API checks, both frontend portals being up, and the default Maven suite.

How to repeat this from the backend repo alone: [RUN_AND_TEST.md](RUN_AND_TEST.md).

**Result: pass.** Walkthrough + user-story scripts were each run **5 times** in a row after a fresh seed. All 10 script runs were green.

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

## API walkthrough (5/5)

`./scripts/walkthrough-cycle.sh` — **all checks passed** on every run.

| Actor | What was verified |
| --- | --- |
| `nadia` ADMIN | Institutions include Nile Valley and Maadi; daily report; outstanding collections; gateway checks; Karim listed; Laila listed under Nile. Invalid password rejected. |
| `karim` BANK_OFFICER | Mona `29805150101023` → Nour + Malak. Youssef `30007091301775` → Omar + Farah. Ahmed `29511020204536` → Karim Ahmed Tarek. Salma `29001301202283` → Lina. Heba parent `28809252506666` → Yassin at Maadi. Unknown NID `[]`. Short NID HTTP 400. Mock lookups: Youssef debit/account, Ahmed frozen/blocked, Salma dormant/expired. |
| `laila` INSTITUTION_OFFICER | Nile students including Youssef’s children; Heba parent search empty; Due / Partial / Paid; receipts `RCP-2026-WALK0001`; upload history. |
| `heba` INSTITUTION_OFFICER | Mona search empty. Yassin visible on Maadi collections. |
| `mona` PAYER | `GET /fee-searches/me` returns both children; receipt `RCP-2026-WALK0003`. |

## User-story API pass (5/5)

`./scripts/user-story-check.sh` — **all checks passed** on every run (idempotent unpaid cash order for Omar).

Covered: BO-US-01/02/03/04/09/11/12, INST-US-01/03/04/06/07/08, PAYER-US-02/06/07, ADMIN-US-01/02, SYS-US-02/03.

Live cash/card/source (`BO-US-05`–`08`, `PAYER-US-04`/`05`) and a fresh CSV upload (`LIVE_UPLOAD=1`) were not looped; they change balances. Opt-in flags are in [RUN_AND_TEST.md](RUN_AND_TEST.md).

## Maven tests

`POSTGRES_PORT=5433 ./mvnw test` (integration group excluded, as in `pom.xml`)

- **403 tests, 0 failures, 0 errors**. `BUILD SUCCESS`.
- `@Tag("integration")` bank-mock suites were not run.

## Seeded mock parents (new this pass)

| National ID | Guardian | Students |
| --- | --- | --- |
| `30007091301775` | Youssef Adel Nabil | Omar, Farah (Nile Valley) |
| `29511020204536` | Ahmed Tarek Mahmoud | Karim Ahmed Tarek |
| `29001301202283` | Salma Hosny Ibrahim | Lina Salma Hosny |
| `28809252506666` | Heba Mostafa Zaki | Yassin Mostafa Zaki (Maadi) |

## Not re-tested this pass

- Live cash / card / source / EPP charge from the UI (decline cards documented in RUN_AND_TEST)
- Fresh `LIVE_UPLOAD=1` CSV this run
- Payer UI (there is no payer portal)
- pgAdmin browsing
- Integration-tagged mock adapter tests

## Notes

- JWT lifetime is 15 minutes.
- `GET /api/v1/epp/plans` now loads instalments inside a read-only transaction (was HTTP 500 from a lazy collection).
- Walkthrough inputs stay in [PORTAL_WALKTHROUGH.md](PORTAL_WALKTHROUGH.md).
