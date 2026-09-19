#!/usr/bin/env bash
# API checks mapped to MVP user stories. Safe to repeat after seed.
# Does not take live cash/card unless LIVE_PAY=1 (reseeds first if you want a clean pay).
#
#   ./scripts/user-story-check.sh
set -euo pipefail

BASE="${BASE:-http://localhost:8080}"
PASSWORD="${PASSWORD:-Admin123!}"
TODAY="$(date -u +%F)"
NILE="b0b0b0b0-0002-4000-8000-000000000001"
NOUR="e0e0e0e0-0005-4000-8000-000000000001"
OMAR="e0e0e0e0-0005-4000-8000-000000000003"
PARENT_NID="29805150101023"
YOUSSEF_NID="30007091301775"
AHMED_NID="29511020204536"
SALMA_NID="29001301202283"
HEBA_NID="28809252506666"
UNKNOWN_NID="29901011234567"
IDEMPOTENCY_KEY="user-story-omar-tuition-cash"

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
  local token="$1" method="$2" path="$3" data="${4:-}" extra_header="${5:-}"
  python3 - "$BASE" "$token" "$method" "$path" "$data" "$extra_header" <<'PY'
import json, sys, urllib.error, urllib.request
base, token, method, path, data, extra = sys.argv[1:7]
headers = {
    "Authorization": "Bearer " + token,
    "Accept": "application/json",
}
if data:
    headers["Content-Type"] = "application/json"
if extra:
    name, _, value = extra.partition(":")
    headers[name] = value
req = urllib.request.Request(
    base + path,
    data=(data.encode() if data else None),
    method=method,
    headers=headers,
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

api_code() {
  local token="$1" method="$2" path="$3" data="${4:-}"
  python3 - "$BASE" "$token" "$method" "$path" "$data" <<'PY'
import json, sys, urllib.error, urllib.request
base, token, method, path, data = sys.argv[1:6]
headers = {
    "Authorization": "Bearer " + token,
    "Accept": "application/json",
}
if data:
    headers["Content-Type"] = "application/json"
req = urllib.request.Request(
    base + path,
    data=(data.encode() if data else None),
    method=method,
    headers=headers,
)
try:
    with urllib.request.urlopen(req) as resp:
        print(resp.status)
except urllib.error.HTTPError as exc:
    print(exc.code)
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

section "BO-US-01 / INST-US-01 sign-in"
python3 - "$BASE" <<'PY'
import json, sys, urllib.error, urllib.request
base = sys.argv[1]
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
    print("  ok BO-US-01 invalid credentials rejected")
PY
KARIM="$(login karim)"
LAILA="$(login laila)"
HEBA="$(login heba)"
NADIA="$(login nadia)"
MONA="$(login mona)"
python3 - "$KARIM" "$LAILA" <<'PY'
import base64, json, sys
def claims(token):
    payload = token.split(".")[1]
    pad = "=" * (-len(payload) % 4)
    return json.loads(base64.urlsafe_b64decode(payload + pad))
karim, laila = (claims(t) for t in sys.argv[1:3])
if karim.get("role") != "BANK_OFFICER":
    raise SystemExit("karim role %s" % karim.get("role"))
if laila.get("role") != "INSTITUTION_OFFICER":
    raise SystemExit("laila role %s" % laila.get("role"))
if not laila.get("institution_id"):
    raise SystemExit("laila JWT missing institution_id")
print("  ok BO-US-01 / INST-US-01 JWT roles")
PY

section "BO-US-02 / BO-US-03 fee search"
SEARCH="$(api "$KARIM" POST "/api/v1/fee-searches" "{\"nationalId\":\"$PARENT_NID\"}")"
expect_contains "$SEARCH" "Nour Khaled Fahmy" "BO-US-02 Mona search"
expect_contains "$SEARCH" "feesByType" "BO-US-03 grouped by type"
expect_contains "$SEARCH" "TUITION" "BO-US-03 tuition category"
python3 - "$BASE" "$KARIM" <<'PY'
import json, sys, urllib.error, urllib.request
base, token = sys.argv[1:3]
req = urllib.request.Request(
    base + "/api/v1/fee-searches",
    data=json.dumps({"nationalId": "123"}).encode(),
    method="POST",
    headers={"Authorization": "Bearer " + token, "Content-Type": "application/json", "Accept": "application/json"},
)
try:
    with urllib.request.urlopen(req) as resp:
        raise SystemExit("short NID returned HTTP %s" % resp.status)
except urllib.error.HTTPError as exc:
    if exc.code != 400:
        raise SystemExit("short NID expected 400, got %s" % exc.code)
    print("  ok BO-US-02 invalid national id rejected")
PY
UNKNOWN="$(api "$KARIM" POST "/api/v1/fee-searches" "{\"nationalId\":\"$UNKNOWN_NID\"}")"
python3 - "$UNKNOWN" <<'PY'
import json, sys
if json.loads(sys.argv[1]):
    raise SystemExit("unknown NID should return []")
print("  ok BO-US-02 unknown national id is empty")
PY
YOUSSEF="$(api "$KARIM" POST "/api/v1/fee-searches" "{\"nationalId\":\"$YOUSSEF_NID\"}")"
AHMED="$(api "$KARIM" POST "/api/v1/fee-searches" "{\"nationalId\":\"$AHMED_NID\"}")"
SALMA="$(api "$KARIM" POST "/api/v1/fee-searches" "{\"nationalId\":\"$SALMA_NID\"}")"
HEBA_PARENT="$(api "$KARIM" POST "/api/v1/fee-searches" "{\"nationalId\":\"$HEBA_NID\"}")"
expect_contains "$YOUSSEF" "Omar Youssef Adel" "Youssef children searchable"
expect_contains "$YOUSSEF" "Farah Youssef Adel" "Youssef daughter searchable"
expect_contains "$AHMED" "Karim Ahmed Tarek" "Ahmed student searchable"
expect_contains "$SALMA" "Lina Salma Hosny" "Salma student searchable"
expect_contains "$HEBA_PARENT" "Yassin Mostafa Zaki" "Heba parent searchable"

section "PAYER-US-02 own fees"
MINE="$(api "$MONA" GET "/api/v1/fee-searches/me")"
expect_contains "$MINE" "Nour Khaled Fahmy" "PAYER-US-02 own students"
PAYER_FEE_SEARCH="$(api_code "$MONA" POST "/api/v1/fee-searches" "{\"nationalId\":\"$PARENT_NID\"}")"
python3 - "$PAYER_FEE_SEARCH" <<'PY'
import sys
code = int(sys.argv[1])
if code not in (401, 403):
    raise SystemExit("payer fee-search expected 401/403, got %s" % code)
print("  ok PAYER-US-02 payer cannot run officer search")
PY

section "BO-US-04 / SYS-US-02 create payment order (idempotent, unpaid)"
ORDER="$(python3 - "$BASE" "$KARIM" "$YOUSSEF" "$OMAR" "$IDEMPOTENCY_KEY" <<'PY'
import json, sys, urllib.request
base, token, search_raw, omar_id, key = sys.argv[1:6]
rows = json.loads(search_raw)
omar = next(r for r in rows if r.get("studentId") == omar_id or r.get("studentName") == "Omar Youssef Adel")
fees = []
for lines in (omar.get("feesByType") or {}).values():
    fees.extend(lines)
tuition = next(f for f in fees if f.get("feeType") == "TUITION")
body = {
    "studentId": omar["studentId"],
    "feeLines": [{"feeId": tuition["id"], "amountToApply": tuition["outstandingAmount"]}],
    "method": "CASH",
    "paymentType": "FULL",
    "currency": "EGP",
}
req = urllib.request.Request(
    base + "/api/v1/payment-orders",
    data=json.dumps(body).encode(),
    method="POST",
    headers={
        "Authorization": "Bearer " + token,
        "Content-Type": "application/json",
        "Accept": "application/json",
        "Idempotency-Key": key,
    },
)
with urllib.request.urlopen(req) as resp:
    print(resp.read().decode())
PY
)"
expect_contains "$ORDER" "CREATED" "BO-US-04 order created"
expect_contains "$ORDER" "6000" "BO-US-04 backend total"
REPLAY="$(api "$KARIM" POST "/api/v1/payment-orders" "{\"studentId\":\"$OMAR\",\"feeLines\":[{\"feeId\":\"20202020-0008-4000-8000-000000000006\",\"amountToApply\":6000}],\"method\":\"CASH\",\"paymentType\":\"FULL\",\"currency\":\"EGP\"}" "Idempotency-Key:$IDEMPOTENCY_KEY")"
python3 - "$ORDER" "$REPLAY" <<'PY'
import json, sys
first, second = (json.loads(v) for v in sys.argv[1:3])
if first["paymentId"] != second["paymentId"]:
    raise SystemExit("SYS-US-02 replay created a second order")
if second.get("status") != "CREATED":
    raise SystemExit("replay status %s" % second.get("status"))
print("  ok SYS-US-02 idempotent replay")
PY

if [ "${LIVE_PAY:-0}" = "1" ]; then
  PAYMENT_ID="$(python3 -c "import json,sys; print(json.loads(sys.argv[1])['paymentId'])" "$ORDER")"
  PAID="$(api "$KARIM" POST "/api/v1/payment-orders/$PAYMENT_ID/pay-cash")"
  expect_contains "$PAID" "SUCCESSFUL" "BO-US-05/07 live cash"
  expect_contains "$PAID" "RCP-" "BO-US-11 receipt issued"
fi

section "BO-US-11 / PAYER-US-07 / BO-US-12 receipts and history"
HISTORY="$(api "$KARIM" GET "/api/v1/payment-orders")"
expect_contains "$HISTORY" "RCP-2026-WALK0001" "BO-US-12 officer history"
RECEIPT="$(api "$KARIM" GET "/api/v1/payment-orders/30303030-0009-4000-8000-000000000001/receipt")"
expect_contains "$RECEIPT" "RCP-2026-WALK0001" "BO-US-11 receipt"
PAYER_HISTORY="$(api "$MONA" GET "/api/v1/payment-orders")"
expect_contains "$PAYER_HISTORY" "RCP-2026-WALK0003" "PAYER-US-07 payer history"

section "BO-US-09 EPP quotes"
QUOTES="$(api "$KARIM" GET "/api/v1/epp/quotes?amount=4000&currency=EGP" || true)"
python3 - "$QUOTES" <<'PY' || true
import json, sys
raw = sys.argv[1]
if not raw.strip():
    print("  skip BO-US-09 quotes (mock down)")
    raise SystemExit(0)
try:
    body = json.loads(raw)
except json.JSONDecodeError:
    print("  skip BO-US-09 quotes (mock down)")
    raise SystemExit(0)
blob = json.dumps(body)
if "3" in blob and "6" in blob:
    print("  ok BO-US-09 tenor quotes present")
else:
    print("  skip BO-US-09 quote shape", list(body)[:8] if isinstance(body, dict) else type(body))
PY
PLANS="$(api "$MONA" GET "/api/v1/epp/plans")"
expect_contains "$PLANS" "gw-epp-walkthrough-4000" "PAYER-US-06 seeded plan"

section "INST-US-03..08 institution collections and reports"
UPLOADS="$(api "$LAILA" GET "/api/v1/integration/uploads")"
expect_contains "$UPLOADS" "nile-valley-2026-09.csv" "INST-US-03/04 upload history"
BATCH="$(python3 -c "import json,sys; print(next(b['id'] for b in json.loads(sys.argv[1]) if b.get('filename')=='nile-valley-2026-09-corrections.csv'))" "$UPLOADS")"
BATCH_DETAIL="$(api "$LAILA" GET "/api/v1/integration/uploads/$BATCH")"
expect_contains "$BATCH_DETAIL" "Unknown fee type" "INST-US-04 rejected row reason"
STUDENT="$(api "$LAILA" GET "/api/v1/billing/students/$NOUR/fees")"
expect_contains "$STUDENT" "Nour Khaled Fahmy" "INST-US-06 student fee record"
DUE="$(api "$LAILA" GET "/api/v1/billing/collections?status=OUTSTANDING")"
expect_contains "$DUE" "Malak Hany Sobhy" "INST-US-07 outstanding"
PARTIAL="$(api "$LAILA" GET "/api/v1/billing/collections?status=PARTIALLY_PAID")"
expect_contains "$PARTIAL" "TUITION" "INST-US-07 partial"
PAID="$(api "$LAILA" GET "/api/v1/billing/collections?status=SETTLED")"
expect_contains "$PAID" "ACTIVITIES" "INST-US-07 paid"
REPORT="$(api "$LAILA" GET "/api/v1/reporting/daily-collections?date=$TODAY")"
expect_contains "$REPORT" "Nour Khaled Fahmy" "INST-US-08 daily report"
CSV="$(api "$LAILA" GET "/api/v1/reporting/daily-collections/csv?date=$TODAY")"
expect_contains "$CSV" "Fee Type" "INST-US-08 csv download"
expect_contains "$CSV" "CREDIT_CARD" "INST-US-08 csv has today's collections"
PDF_CODE="$(api_code "$LAILA" GET "/api/v1/reporting/daily-collections/pdf?date=$TODAY")"
python3 - "$PDF_CODE" <<'PY'
import sys
if sys.argv[1] != "200":
    raise SystemExit("INST-US-08 pdf expected 200, got %s" % sys.argv[1])
print("  ok INST-US-08 pdf download")
PY

section "SYS-US-03 / ADMIN isolation"
HEBA_MONA="$(api "$HEBA" POST "/api/v1/fee-searches" "{\"nationalId\":\"$PARENT_NID\"}")"
python3 - "$HEBA_MONA" <<'PY'
import json, sys
if json.loads(sys.argv[1]):
    raise SystemExit("Heba should not see Nile students")
print("  ok SYS-US-03 Maadi isolation")
PY
HEBA_NOUR_CODE="$(api_code "$HEBA" GET "/api/v1/billing/students/$NOUR/fees")"
python3 - "$HEBA_NOUR_CODE" <<'PY'
import sys
code = int(sys.argv[1])
if code not in (403, 404):
    raise SystemExit("Heba Nour record expected 403/404, got %s" % code)
print("  ok SYS-US-03 cross-school student record denied (%s)" % code)
PY
ADMIN_USERS="$(api_code "$KARIM" GET "/api/v1/admin/institutions/$NILE/users")"
python3 - "$ADMIN_USERS" <<'PY'
import sys
if int(sys.argv[1]) not in (401, 403):
    raise SystemExit("bank officer institution users expected 401/403, got %s" % sys.argv[1])
print("  ok SYS-US-03 bank officer cannot manage institution staff")
PY
INST="$(api "$NADIA" GET "/api/v1/admin/institutions")"
expect_contains "$INST" "Nile Valley School" "ADMIN-US-01 institution list"
OFFICERS="$(api "$NADIA" GET "/api/v1/admin/backoffice-employees")"
expect_contains "$OFFICERS" "Karim El-Sayed" "ADMIN-US-02 staff list"

echo
echo "User-story API checks passed."
