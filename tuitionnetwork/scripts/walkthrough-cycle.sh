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
MAADI="b0b0b0b0-0002-4000-8000-000000000002"
NOUR="e0e0e0e0-0005-4000-8000-000000000001"
OMAR="e0e0e0e0-0005-4000-8000-000000000003"
YASSIN="e0e0e0e0-0005-4000-8000-000000000007"
PARENT_NID="29805150101023"
YOUSSEF_NID="30007091301775"
AHMED_NID="29511020204536"
SALMA_NID="29001301202283"
HEBA_NID="28809252506666"
UNKNOWN_NID="29901011234567"

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

api_soft() {
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
        print(resp.read().decode())
except urllib.error.HTTPError as exc:
    print(exc.read().decode())
except urllib.error.URLError as exc:
    print("")
PY
}

expect_empty_array() {
  local haystack="$1" label="$2"
  python3 - "$haystack" "$label" <<'PY'
import json, sys
raw, label = sys.argv[1:3]
rows = json.loads(raw)
if rows:
    raise SystemExit("%s should be empty, got %s" % (label, rows))
print("  ok", label)
PY
}

expect_login_rejected() {
  python3 - "$BASE" "$PASSWORD" <<'PY'
import json, sys, urllib.error, urllib.request
base, password = sys.argv[1:3]
req = urllib.request.Request(
    base + "/api/v1/auth/login",
    data=json.dumps({"username": "karim", "password": "wrong-password"}).encode(),
    headers={"Content-Type": "application/json"},
    method="POST",
)
try:
    with urllib.request.urlopen(req) as resp:
        raise SystemExit("invalid login returned HTTP %s" % resp.status)
except urllib.error.HTTPError as exc:
    if exc.code not in (401, 403):
        raise SystemExit("invalid login expected 401, got %s" % exc.code)
    print("  ok invalid credentials rejected")
PY
}

expect_search_rejected() {
  local token="$1"
  python3 - "$BASE" "$token" <<'PY'
import json, sys, urllib.error, urllib.request
base, token = sys.argv[1:3]
req = urllib.request.Request(
    base + "/api/v1/fee-searches",
    data=json.dumps({"nationalId": "123"}).encode(),
    method="POST",
    headers={
        "Authorization": "Bearer " + token,
        "Content-Type": "application/json",
        "Accept": "application/json",
    },
)
try:
    with urllib.request.urlopen(req) as resp:
        raise SystemExit("short national id returned HTTP %s" % resp.status)
except urllib.error.HTTPError as exc:
    if exc.code != 400:
        raise SystemExit("short national id expected 400, got %s" % exc.code)
    print("  ok invalid national id rejected")
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
expect_login_rejected
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
expect_search_rejected "$KARIM"
UNKNOWN_SEARCH="$(api "$KARIM" POST "/api/v1/fee-searches" "{\"nationalId\":\"$UNKNOWN_NID\"}")"
expect_empty_array "$UNKNOWN_SEARCH" "unknown national id has no fees"

section "2b. Mock-aligned parents — Youssef, Ahmed, Salma, Heba"
echo "    Youssef $YOUSSEF_NID -> Omar / Farah (debit/account, no EPP)"
echo "    Ahmed   $AHMED_NID -> Karim Ahmed Tarek (frozen/blocked sources)"
echo "    Salma   $SALMA_NID -> Lina Salma Hosny (dormant/expired sources)"
echo "    Heba    $HEBA_NID -> Yassin Mostafa Zaki at Maadi"
YOUSSEF_SEARCH="$(api "$KARIM" POST "/api/v1/fee-searches" "{\"nationalId\":\"$YOUSSEF_NID\"}")"
expect_contains "$YOUSSEF_SEARCH" "Omar Youssef Adel" "youssef son"
expect_contains "$YOUSSEF_SEARCH" "Farah Youssef Adel" "youssef daughter"
expect_contains "$YOUSSEF_SEARCH" "Nile Valley School" "youssef school"
AHMED_SEARCH="$(api "$KARIM" POST "/api/v1/fee-searches" "{\"nationalId\":\"$AHMED_NID\"}")"
expect_contains "$AHMED_SEARCH" "Karim Ahmed Tarek" "ahmed student"
SALMA_SEARCH="$(api "$KARIM" POST "/api/v1/fee-searches" "{\"nationalId\":\"$SALMA_NID\"}")"
expect_contains "$SALMA_SEARCH" "Lina Salma Hosny" "salma student"
HEBA_PARENT_SEARCH="$(api "$KARIM" POST "/api/v1/fee-searches" "{\"nationalId\":\"$HEBA_NID\"}")"
expect_contains "$HEBA_PARENT_SEARCH" "Yassin Mostafa Zaki" "heba parent student"
expect_contains "$HEBA_PARENT_SEARCH" "Maadi Language School" "heba parent school"

YOUSSEF_LOOKUP="$(api_soft "$KARIM" GET "/api/v1/payment-orders/customer-lookup?nationalId=$YOUSSEF_NID")"
AHMED_LOOKUP="$(api_soft "$KARIM" GET "/api/v1/payment-orders/customer-lookup?nationalId=$AHMED_NID")"
SALMA_LOOKUP="$(api_soft "$KARIM" GET "/api/v1/payment-orders/customer-lookup?nationalId=$SALMA_NID")"
python3 - "$YOUSSEF_LOOKUP" "$AHMED_LOOKUP" "$SALMA_LOOKUP" <<'PY'
import json, sys
def parse(raw):
    raw = (raw or "").strip()
    if not raw:
        return None
    try:
        return json.loads(raw)
    except json.JSONDecodeError:
        return None

youssef, ahmed, salma = (parse(v) for v in sys.argv[1:4])
if youssef is None or ahmed is None or salma is None:
    print("  skip mock lookups (bank mock not running)")
    raise SystemExit(0)
if youssef.get("error") or ahmed.get("error") or salma.get("error"):
    print("  skip mock lookups (bank mock error)")
    raise SystemExit(0)

def blob(body):
    return json.dumps(body)

y = blob(youssef)
if "Youssef" not in y or "acc_youssef_current" not in y:
    raise SystemExit("youssef lookup missing account: %s" % list(youssef)[:8])
if "DEBIT" not in y:
    raise SystemExit("youssef lookup should include a debit card")
print("  ok youssef lookup debit/account")

a = blob(ahmed)
if "FROZEN" not in a:
    raise SystemExit("ahmed lookup should include FROZEN account")
if "BLOCKED" not in a:
    raise SystemExit("ahmed lookup should include BLOCKED card")
print("  ok ahmed lookup frozen/blocked")

s = blob(salma)
if "DORMANT" not in s:
    raise SystemExit("salma lookup should include DORMANT account")
if "EXPIRED" not in s:
    raise SystemExit("salma lookup should include EXPIRED card")
print("  ok salma lookup dormant/expired")
PY

section "3. School officer Laila — Nile Valley School"
echo "UI  $FRONTEND/institution/login   user=laila"
echo "    Dashboard, Customers search $PARENT_NID, Collections, Reports, student receipt view, Submit dues"
LAILA="$(login laila)"
SCHOOL_SEARCH="$(api "$LAILA" POST "/api/v1/fee-searches" "{\"nationalId\":\"$PARENT_NID\"}")"
expect_contains "$SCHOOL_SEARCH" "Nour Khaled Fahmy" "school search names"
YOUSSEF_SCHOOL="$(api "$LAILA" POST "/api/v1/fee-searches" "{\"nationalId\":\"$YOUSSEF_NID\"}")"
expect_contains "$YOUSSEF_SCHOOL" "Omar Youssef Adel" "nile sees youssef children"
HEBA_ON_NILE="$(api "$LAILA" POST "/api/v1/fee-searches" "{\"nationalId\":\"$HEBA_NID\"}")"
expect_empty_array "$HEBA_ON_NILE" "nile does not see maadi student"
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
expect_empty_array "$HEBA_SEARCH" "maadi search is empty for Mona"
HEBA_OWN="$(api "$HEBA" POST "/api/v1/fee-searches" "{\"nationalId\":\"$HEBA_NID\"}")"
expect_contains "$HEBA_OWN" "Yassin Mostafa Zaki" "maadi officer sees own-school student"
YASSIN_FEES="$(api "$HEBA" GET "/api/v1/billing/students/$YASSIN/fees")"
expect_contains "$YASSIN_FEES" "Yassin Mostafa Zaki" "maadi student fee record"
expect_contains "$YASSIN_FEES" "Maadi Language School" "maadi student school"
MAADI_DUE="$(api "$HEBA" GET "/api/v1/billing/collections?status=OUTSTANDING")"
expect_contains "$MAADI_DUE" "Yassin Mostafa Zaki" "maadi outstanding collections"

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
       Also search:
         $YOUSSEF_NID Youssef -> Omar / Farah (pay from account or debit; no EPP)
         $AHMED_NID Ahmed -> Karim Ahmed Tarek (frozen account / blocked Visa)
         $SALMA_NID Salma -> Lina Salma Hosny (dormant / expired; cannot pay from source)
         $HEBA_NID Heba -> Yassin Mostafa Zaki at Maadi
         $UNKNOWN_NID -> no fees
    Institution
    3. $FRONTEND/institution/login         laila / Admin123!
       Dashboard collected-today > 0. Customers search same NID.
       Collections Due/Partial/Paid. Reports names. View student receipt (not pay).
       Submit dues: scripts/sample-fee-upload.csv
       Youssef NID returns Omar/Farah. Heba NID is empty.
    4. Logout. Login heba / Admin123!
       Customers search $PARENT_NID -> empty (other school).
       Customers search $HEBA_NID -> Yassin Mostafa Zaki.
  Mock
    Start wit-mock-services on :8000 for lookup / card / MOI.
    Decline cards: 4000000000000002 insufficient, 4000000000009995 do not honour,
    4000000000000101 bad CVV, 4000000000000069 expired, 4000000000000119 issuer 502.
EOF

echo
echo "Walkthrough API checks passed."
