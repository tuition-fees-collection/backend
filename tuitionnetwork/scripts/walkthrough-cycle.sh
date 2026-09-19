#!/usr/bin/env bash
# Read-only walkthrough of every seeded portal against a running API.
# Does not take live cash or upload a CSV unless you opt in.
#
#   docker compose exec -T postgres psql -U tuition -d tuition < scripts/seed-dev-data.sql
#   ./scripts/walkthrough-cycle.sh
#
# Optional:
#   LIVE_UPLOAD=1 ./scripts/walkthrough-cycle.sh   # school CSV upload (2026-10)
#   BASE=http://localhost:8080 FRONTEND=http://localhost:5173
set -euo pipefail

BASE="${BASE:-http://localhost:8080}"
FRONTEND="${FRONTEND:-http://localhost:5173}"
PASSWORD="${PASSWORD:-Admin123!}"
TODAY="$(date -u +%F)"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
NILE="b0b0b0b0-0002-4000-8000-000000000001"
NOUR="e0e0e0e0-0005-4000-8000-000000000001"
PARENT_NID="29805150101023"

login() {
  local user="$1"
  python3 - "$BASE" "$user" "$PASSWORD" <<'PY'
import json, sys, urllib.request
base, user, password = sys.argv[1:4]
req = urllib.request.Request(
    base + "/api/v1/auth/login",
    data=json.dumps({"username": user, "password": password}).encode(),
    headers={"Content-Type": "application/json"},
    method="POST",
)
with urllib.request.urlopen(req) as resp:
    body = json.load(resp)
token = body.get("accessToken") or body.get("access_token")
if not token:
    raise SystemExit("login for %s did not return a token: %s" % (user, body))
print(token)
PY
}

api() {
  local token="$1" method="$2" path="$3" data="${4:-}"
  python3 - "$BASE" "$token" "$method" "$path" "$data" <<'PY'
import json, sys, urllib.error, urllib.request
base, token, method, path, data = sys.argv[1:6]
req = urllib.request.Request(
    base + path,
    data=(data.encode() if data else None),
    method=method,
    headers={
        "Authorization": "Bearer " + token,
        "Accept": "application/json",
        **({"Content-Type": "application/json"} if data else {}),
    },
)
try:
    with urllib.request.urlopen(req) as resp:
        raw = resp.read().decode()
        status = resp.status
except urllib.error.HTTPError as exc:
    raw = exc.read().decode()
    print(raw)
    raise SystemExit("%s %s -> HTTP %s" % (method, path, exc.code))
print(raw)
if status >= 400:
    raise SystemExit("%s %s -> HTTP %s" % (method, path, status))
PY
}

expect_contains() {
  local haystack="$1" needle="$2" label="$3"
  python3 - "$haystack" "$needle" "$label" <<'PY'
import sys
haystack, needle, label = sys.argv[1:4]
if needle not in haystack:
    raise SystemExit("missing %s in %s" % (needle, label))
print("  ok", label)
PY
}

section() {
  printf "\n== %s ==\n" "$1"
}

echo "API       $BASE"
echo "Frontend  $FRONTEND"
echo "Date      $TODAY"

section "1. Admin Nadia — Overall Dashboard, institutions, officers"
echo "UI  $FRONTEND/backoffice/login   user=nadia"
echo "    then $FRONTEND/backoffice/overall-dashboard"
echo "         $FRONTEND/backoffice/create-institution"
echo "         $FRONTEND/backoffice/create-officer"
NADIA="$(login nadia)"
INSTITUTIONS="$(api "$NADIA" GET "/api/v1/admin/institutions")"
expect_contains "$INSTITUTIONS" "Nile Valley School" "admin institution list"
expect_contains "$INSTITUTIONS" "Maadi Language School" "second school present"
REPORT="$(api "$NADIA" GET "/api/v1/reporting/daily-collections?date=$TODAY")"
expect_contains "$REPORT" "paymentCount" "admin network daily report"
COLLECTIONS="$(api "$NADIA" GET "/api/v1/billing/collections?status=OUTSTANDING")"
expect_contains "$COLLECTIONS" "$NOUR" "admin outstanding collections"
CHECKS="$(api "$NADIA" GET "/api/v1/admin/gateway-checks")"
expect_contains "$CHECKS" "matched" "gateway checks"
EMPLOYEES="$(api "$NADIA" GET "/api/v1/admin/backoffice-employees")"
expect_contains "$EMPLOYEES" "Karim El-Sayed" "bank officer listed"
USERS="$(api "$NADIA" GET "/api/v1/admin/institutions/$NILE/users")"
expect_contains "$USERS" "Laila Mostafa" "nile officer listed"

section "2. Bank officer Karim — Find Fees wizard"
echo "UI  $FRONTEND/backoffice/login   user=karim"
echo "    Find Fees -> search $PARENT_NID"
echo "    expect student names Nour Khaled Fahmy and Malak Hany Sobhy"
echo "    then Fee Management -> Client Overview"
KARIM="$(login karim)"
SEARCH="$(api "$KARIM" POST "/api/v1/fee-searches" "{\"nationalId\":\"$PARENT_NID\"}")"
expect_contains "$SEARCH" "Nour Khaled Fahmy" "search student name"
expect_contains "$SEARCH" "Malak Hany Sobhy" "second child in search"
expect_contains "$SEARCH" "Nile Valley School" "search institution name"
LOOKUP="$(api "$KARIM" GET "/api/v1/payment-orders/customer-lookup?nationalId=$PARENT_NID" || true)"
# Lookup hits the bank mock; still pass if the mock is down as long as search worked.
python3 - "$LOOKUP" <<'PY' || true
import json, sys
raw = sys.argv[1]
if not raw.strip():
    print("  skip customer lookup (empty)")
    raise SystemExit(0)
try:
    body = json.loads(raw)
except json.JSONDecodeError:
    print("  skip customer lookup (mock not running)")
    raise SystemExit(0)
name = body.get("customerName") or body.get("full_name_en") or ""
if "Mona" in name:
    print("  ok customer lookup", name)
else:
    print("  skip customer lookup shape", list(body)[:6])
PY
HISTORY="$(api "$KARIM" GET "/api/v1/payment-orders")"
expect_contains "$HISTORY" "RCP-2026-WALK0001" "officer payment history"
DASH="$(api "$KARIM" GET "/api/v1/reporting/daily-collections?date=$TODAY")"
expect_contains "$DASH" "totalCollected" "officer dashboard report"

section "3. School officer Laila — Nile Valley School"
echo "UI  $FRONTEND/institution/login   user=laila"
echo "    Dashboard, Customers search $PARENT_NID, Collections, Reports, student receipt view, Submit dues"
LAILA="$(login laila)"
SCHOOL_SEARCH="$(api "$LAILA" POST "/api/v1/fee-searches" "{\"nationalId\":\"$PARENT_NID\"}")"
expect_contains "$SCHOOL_SEARCH" "Nour Khaled Fahmy" "school search names"
STUDENT="$(api "$LAILA" GET "/api/v1/billing/students/$NOUR/fees")"
expect_contains "$STUDENT" "Nour Khaled Fahmy" "student fee record name"
expect_contains "$STUDENT" "Nile Valley School" "student fee record school"
DUE="$(api "$LAILA" GET "/api/v1/billing/collections?status=OUTSTANDING")"
expect_contains "$DUE" "Malak Hany Sobhy" "outstanding collections names"
PARTIAL="$(api "$LAILA" GET "/api/v1/billing/collections?status=PARTIALLY_PAID")"
expect_contains "$PARTIAL" "TUITION" "partial collections"
PAID="$(api "$LAILA" GET "/api/v1/billing/collections?status=SETTLED")"
expect_contains "$PAID" "ACTIVITIES" "settled collections"
SCHOOL_REPORT="$(api "$LAILA" GET "/api/v1/reporting/daily-collections?date=$TODAY")"
expect_contains "$SCHOOL_REPORT" "Nour Khaled Fahmy" "school report item names"
RECEIPTS="$(api "$LAILA" GET "/api/v1/billing/students/$NOUR/receipts")"
expect_contains "$RECEIPTS" "RCP-2026-WALK0001" "school student receipts"
expect_contains "$RECEIPTS" "Nour Khaled Fahmy" "school receipt student name"
UPLOADS="$(api "$LAILA" GET "/api/v1/integration/uploads")"
expect_contains "$UPLOADS" "nile-valley-2026-09.csv" "upload history"

if [ "${LIVE_UPLOAD:-0}" = "1" ]; then
  echo "  uploading scripts/sample-fee-upload.csv"
  curl -sfS -X POST "$BASE/api/v1/integration/uploads" \
    -H "Authorization: Bearer $LAILA" \
    -F "file=@$SCRIPT_DIR/sample-fee-upload.csv;type=text/csv" \
    | python3 -c "import json,sys; d=json.load(sys.stdin); print('  upload', d.get('status'), 'accepted', d.get('acceptedCount') or d.get('rowCount'))"
fi

section "4. School officer Heba — Maadi isolation"
echo "UI  $FRONTEND/institution/login   user=heba"
echo "    Customers search $PARENT_NID should return no Nile Valley fees"
HEBA="$(login heba)"
HEBA_SEARCH="$(api "$HEBA" POST "/api/v1/fee-searches" "{\"nationalId\":\"$PARENT_NID\"}")"
python3 - "$HEBA_SEARCH" <<'PY'
import json, sys
rows = json.loads(sys.argv[1])
if rows:
    raise SystemExit("Maadi officer should not see Nile Valley fees, got %s" % rows)
print("  ok maadi search is empty")
PY

section "5. Payer Mona — own fees (API only; no payer portal in this frontend)"
MONA="$(login mona)"
MINE="$(api "$MONA" GET "/api/v1/fee-searches/me")"
expect_contains "$MINE" "Nour Khaled Fahmy" "payer own student names"
expect_contains "$MINE" "Malak Hany Sobhy" "payer second child"
PAYER_HISTORY="$(api "$MONA" GET "/api/v1/payment-orders")"
expect_contains "$PAYER_HISTORY" "RCP-2026-WALK0003" "payer receipts"

section "Frontend cycle (manual, after API checks pass)"
cat <<EOF
  Back office
    1. $FRONTEND/backoffice/login          nadia / Admin123!
       Overall Dashboard KPIs populated; institution filter includes Nile Valley.
       Create Institution / Create Officer visible. Find Fees hidden.
    2. Logout. Login karim / Admin123!
       Find Fees: $PARENT_NID -> Nour Khaled Fahmy, Malak Hany Sobhy.
       Client Overview: name Mona Samir Abdelrahman if the mock is on :8000.
    Institution
    3. $FRONTEND/institution/login         laila / Admin123!
       Dashboard collected-today > 0. Customers search same NID.
       Collections Due/Partial/Paid. Reports names. View student receipt (not pay).
       Submit dues: scripts/sample-fee-upload.csv
    4. Logout. Login heba / Admin123!
       Customers search same NID -> empty (other school).
  Mock
    Start wit-mock-services on :8000 for lookup / card / MOI.
EOF

echo
echo "Walkthrough API checks passed."
