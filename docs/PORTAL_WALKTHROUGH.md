# Portal URLs and testing flow

Two frontend portals (Vite default `http://localhost:5173`). Backend is `http://localhost:8080`.

There is no payer portal. `mona` is API-only.

How to start the API, seed, and rerun checks: [RUN_AND_TEST.md](RUN_AND_TEST.md).

## Portal URLs

| Portal | URL | Who logs in |
| --- | --- | --- |
| Back office | http://localhost:5173/backoffice/login | `nadia` (admin), `karim` (bank officer) |
| Institution | http://localhost:5173/institution/login | `laila` (Nile Valley), `heba` (Maadi) |

Password for every seeded login: **`Admin123!`**

If the frontend is not on 5173, use that host/port instead.

## Related local services

| Component | Local address |
| --- | --- |
| Backend | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| pgAdmin | http://localhost:5051 |
| Bank mock | http://localhost:8000 |

Start `wit-mock-services` on `:8000` for customer lookup, card, and MOI.

## Seed first

Run once against the running Docker stack, from `tuitionnetwork`:

```sh
docker compose exec -T postgres psql -U tuition -d tuition < scripts/seed-dev-data.sql
```

Password for every seeded login: `Admin123!`.

| Username | Role | Portal |
| --- | --- | --- |
| `nadia` | ADMIN | `/backoffice/login` |
| `karim` | BANK_OFFICER | `/backoffice/login` |
| `laila` | INSTITUTION_OFFICER | `/institution/login` (Nile Valley School) |
| `heba` | INSTITUTION_OFFICER | `/institution/login` (Maadi Language School) |
| `mona` | PAYER | API only (`GET /api/v1/fee-searches/me`) |

## Shared search / lookup inputs

| Field | Value |
| --- | --- |
| Parent national ID | `29805150101023` |
| Parent name | Mona Samir Abdelrahman |
| Student | Nour Khaled Fahmy — `30103222103442` |
| Student | Malak Hany Sobhy — `31204010102041` |
| Youssef (debit / account, no EPP) | `30007091301775` — Omar / Farah at Nile Valley |
| Ahmed (frozen / blocked sources) | `29511020204536` — Karim Ahmed Tarek |
| Salma (dormant / expired sources) | `29001301202283` — Lina Salma Hosny |
| Heba parent (Maadi only) | `28809252506666` — Yassin Mostafa Zaki |
| No fees | `29901011234567` |
| Nile Valley School id | `b0b0b0b0-0002-4000-8000-000000000001` |
| Live CSV after seed | `tuitionnetwork/scripts/sample-fee-upload.csv` (period 2026-10) |

## Manual UI flow

### 1. Back office — admin

Open http://localhost:5173/backoffice/login

- User: `nadia` / `Admin123!`
- Check Overall Dashboard KPIs; institution filter includes Nile Valley.
- Create Institution and Create Officer should be visible.
- Find Fees should be hidden.

### 2. Back office — bank officer

Logout, then login `karim` / `Admin123!`

- Find Fees → search `29805150101023`
- Expect **Nour Khaled Fahmy** and **Malak Hany Sobhy** (Nile Valley School).
- Client Overview shows **Mona Samir Abdelrahman** if the bank mock is on `:8000`.
- Search `30007091301775` → **Omar Youssef Adel** and **Farah Youssef Adel**. Lookup is debit/account only.
- Search `29511020204536` → **Karim Ahmed Tarek**. Lookup is frozen/blocked.
- Search `29001301202283` → **Lina Salma Hosny**. Lookup is dormant/expired.
- Search `28809252506666` → **Yassin Mostafa Zaki** at Maadi.

### 3. Institution — Nile Valley

Open http://localhost:5173/institution/login

- User: `laila` / `Admin123!`
- Dashboard collected-today should be > 0.
- Customers search: `29805150101023` (same two students).
- Youssef NID returns Omar / Farah. Heba NID is empty.
- Collections: Due / Partial / Paid.
- Reports: student names present.
- View student receipt (do not pay).
- Submit dues: upload `tuitionnetwork/scripts/sample-fee-upload.csv`.

Sample CSV rows:

```csv
national_id,fee_type,amount,currency,collection_period,parent_national_id,parent_full_name
30103222103442,TUITION,9000.00,EGP,2026-10,29805150101023,Mona Samir Abdelrahman
30103222103442,ACTIVITIES,800.00,EGP,2026-10,29805150101023,Mona Samir Abdelrahman
31204010102041,TRANSPORTATION,1200.00,EGP,2026-10,29805150101023,Mona Samir Abdelrahman
31204010102041,TUITION,7500.00,EGP,2026-10,29805150101023,Mona Samir Abdelrahman
```

### 4. Institution — Maadi isolation

Logout, then login `heba` / `Admin123!`

- Customers search `29805150101023` → **empty** (other school).
- Customers search `28809252506666` → **Yassin Mostafa Zaki**.

## API check of the same cycle

From `tuitionnetwork`:

```sh
./scripts/walkthrough-cycle.sh
```

Optional live CSV upload:

```sh
LIVE_UPLOAD=1 ./scripts/walkthrough-cycle.sh
```

If the frontend is not on 5173:

```sh
FRONTEND=http://localhost:<port> ./scripts/walkthrough-cycle.sh
```

The script logs in as each seeded user and hits the matching API routes. Payer `mona` is checked on the API only.

User-story mapping (auth, search, grouped fees, unpaid order create, isolation, reports):

```sh
./scripts/user-story-check.sh
```
