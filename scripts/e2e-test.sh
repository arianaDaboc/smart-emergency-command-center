#!/usr/bin/env bash
# End-to-end test of the main emergency scenario against a running backend (http://localhost:8080).
# Requires: curl, jq. Run on a freshly seeded database.
set -euo pipefail
API=${API:-http://localhost:8080/api}
PW=password123
pass=0; fail=0
ok()   { echo "  ✔ $1"; pass=$((pass+1)); }
bad()  { echo "  ✘ $1"; fail=$((fail+1)); }
check(){ if [ "$2" == "$3" ]; then ok "$1"; else bad "$1 (expected '$3', got '$2')"; fi; }

login() { curl -s -X POST "$API/auth/login" -H 'Content-Type: application/json' -d "{\"username\":\"$1\",\"password\":\"$PW\"}" | jq -r .token; }
req() { # method token path [body] -> prints body, sets $CODE
  local m=$1 t=$2 p=$3 b=${4:-}
  local out; out=$(curl -s -w '\n%{http_code}' -X "$m" "$API$p" -H "Authorization: Bearer $t" -H 'Content-Type: application/json' ${b:+-d "$b"})
  CODE=$(echo "$out" | tail -1); BODY=$(echo "$out" | sed '$d')
}

echo "== Login"
P=$(login patient1); D=$(login dispatcher); C=$(login crew1); DOC=$(login doctor1); A=$(login admin)
[ "$P" != "null" ] && ok "patient login" || bad "patient login"
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$API/auth/login" -H 'Content-Type: application/json' -d '{"username":"patient1","password":"wrong"}')
check "wrong password -> 401" "$CODE" 401

echo "== Role-based access"
req GET "$P" /dispatcher/incidents; check "patient cannot call dispatcher API" "$CODE" 403
req GET "$C" /patient/medical-card; check "crew cannot read the patient's medical card endpoint" "$CODE" 403

echo "== Activate Emergency Request (SOS)"
req POST "$P" /patient/emergencies '{"type":"MEDICAL","note":"Chest pain","lat":46.7700,"lng":23.5900}'
check "SOS created" "$CODE" 201
INC=$(echo "$BODY" | jq .incident.id); AMB=$(echo "$BODY" | jq -r .ambulanceCode)
check "incident dispatched" "$(echo "$BODY" | jq -r .incident.status)" DISPATCHED
check "nearest ambulance assigned" "$AMB" "AMB-01"
check "priority = 45+2+6 = 53" "$(echo "$BODY" | jq .incident.priority)" 53
req POST "$P" /patient/emergencies '{"lat":46.77,"lng":23.59}'; check "second open SOS rejected" "$CODE" 409

echo "== Crew: View Assigned Emergency"
req GET "$C" /crew/assignment
check "crew sees incident" "$(echo "$BODY" | jq .assignment.incident.id)" "$INC"
check "crew sees blood type" "$(echo "$BODY" | jq -r .assignment.patient.bloodType)" "A+"
check "crew sees medical card but NOT history" "$(echo "$BODY" | jq '.assignment.patient | has("history")')" false
req POST "$C" /crew/assignment/status '{"status":"ON_SCENE"}'; check "skipping a step rejected" "$CODE" 409
req POST "$C" /crew/assignment/status '{"status":"EN_ROUTE"}'; check "EN_ROUTE" "$CODE" 200
req POST "$P" /patient/emergencies/$INC/cancel '{"reason":"x"}' ; echo "    (cancel while EN_ROUTE allowed: $CODE)"
# re-create scenario if cancel succeeded
if [ "$CODE" == 200 ]; then
  req POST "$P" /patient/emergencies '{"type":"MEDICAL","lat":46.7700,"lng":23.5900}'; INC=$(echo "$BODY" | jq .incident.id)
  req POST "$C" /crew/assignment/status '{"status":"EN_ROUTE"}'
fi
req POST "$C" /crew/assignment/status '{"status":"ON_SCENE"}'; check "ON_SCENE" "$CODE" 200
req POST "$P" /patient/emergencies/$INC/cancel '{"reason":"x"}'; check "cancel after ON_SCENE rejected" "$CODE" 409

echo "== Record Vital Signs"
req POST "$C" /crew/assignment/vitals '{"pulse":300,"spo2":95,"systolic":120,"diastolic":80,"temperature":36.6,"condition":"STABLE"}'
check "pulse 300 rejected" "$CODE" 400
req POST "$C" /crew/assignment/vitals '{"pulse":130,"spo2":89,"systolic":150,"diastolic":95,"temperature":37.2,"oxygenLpm":4,"iv":true,"medication":"Aspirin 300mg","condition":"SERIOUS"}'
check "vitals saved" "$CODE" 200
check "abnormal values flagged" "$(echo "$BODY" | jq -c '.assignment.vitals[0].abnormal')" '["pulse","spo2"]'

echo "== Transport + hand over"
HID=$(echo "$BODY" | jq .assignment.nearestHospitalId)
req POST "$C" /crew/assignment/status "{\"status\":\"TRANSPORTING\",\"hospitalId\":$HID}"; check "TRANSPORTING" "$CODE" 200
req GET "$DOC" /doctor/incoming; ASG=$(echo "$BODY" | jq '.[0].assignmentId')
check "doctor sees incoming patient" "$(echo "$BODY" | jq -r '.[0].patientName')" "Ion Popescu"
req PUT "$DOC" /doctor/patients/$ASG/case '{"diagnosis":"x","patientStatus":"ADMITTED"}'; check "case before hand-over rejected" "$CODE" 409
req POST "$D" /dispatcher/incidents/$INC/close '{}'; check "close during transport rejected" "$CODE" 409
req POST "$C" /crew/assignment/status '{"status":"AT_HOSPITAL"}'; check "AT_HOSPITAL" "$CODE" 200
req POST "$C" /crew/assignment/handover '{"note":"Stable during transport"}'; check "hand over" "$CODE" 200
check "ambulance available again" "$(echo "$BODY" | jq -r .ambulance.status)" AVAILABLE

echo "== Doctor"
req GET "$DOC" /doctor/patients/$ASG
check "doctor sees history" "$(echo "$BODY" | jq '.history | length')" 1
req PUT "$DOC" /doctor/patients/$ASG/case '{"diagnosis":"NSTEMI","treatment":"Coronary angiography","patientStatus":"ADMITTED"}'; check "diagnosis saved" "$CODE" 200
req POST "$DOC" /doctor/patients/$ASG/case/finalize '{"currentMedication":"Metformin, Enalapril, Clopidogrel"}'; check "finalized" "$CODE" 200
req GET "$P" /patient/medical-card
check "history updated" "$(echo "$BODY" | jq -r '.history[0].diagnosis')" NSTEMI
check "card updated" "$(echo "$BODY" | jq -r '.card.currentMedication')" "Metformin, Enalapril, Clopidogrel"

echo "== Dispatcher"
req GET "$D" /dispatcher/incidents/$INC; check "ready to close" "$(echo "$BODY" | jq .incident.readyToClose)" true
req POST "$D" /dispatcher/incidents/$INC/close '{"note":"done"}'; check "incident closed" "$(echo "$BODY" | jq -r .incident.status)" CLOSED
req POST "$D" /dispatcher/incidents '{"type":"FIRE","severity":"CRITICAL","affectedPeople":12,"description":"Warehouse fire","lat":46.78,"lng":23.61}'
check "phone incident created" "$CODE" 201
FI=$(echo "$BODY" | jq .incident.id)
check "fire priority 90 (+waiting)" "$(echo "$BODY" | jq .incident.priority)" 90
check "recommend 3 ambulances" "$(echo "$BODY" | jq '.recommendation.needs[] | select(.type=="AMBULANCE") | .candidates | length')" 3
IDS=$(echo "$BODY" | jq -c '[.recommendation.needs[].candidates[].id]')
req POST "$D" /dispatcher/incidents/$FI/assign "{\"resourceIds\":$IDS}"; check "recommendation accepted" "$(echo "$BODY" | jq -r .incident.status)" DISPATCHED
req POST "$D" /dispatcher/incidents/$FI/assign "{\"resourceIds\":$IDS}"; check "double assignment rejected" "$CODE" 409
req PATCH "$D" /dispatcher/resources/$(echo "$IDS" | jq '.[0]')/status '{"status":"OUT_OF_SERVICE"}'; check "busy resource cannot go out of service" "$CODE" 409

echo "== Contacts"
req POST "$P" /patient/contacts '{"name":"","phone":"12"}'; check "invalid contact rejected" "$CODE" 400

echo "== Admin"
req GET "$A" /admin/status; check "database OK" "$(echo "$BODY" | jq -r .databaseStatus)" OK
UID_P3=$(curl -s "$API/admin/users" -H "Authorization: Bearer $A" | jq '.[] | select(.username=="patient3") | .id')
req POST "$A" /admin/users/$UID_P3/enabled '{"enabled":false}'; check "user disabled" "$(echo "$BODY" | jq .enabled)" false
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$API/auth/login" -H 'Content-Type: application/json' -d '{"username":"patient3","password":"password123"}')
check "disabled user cannot log in" "$CODE" 403
req GET "$A" "/admin/audit?action=LOGIN_FAILED"; check "failed login logged" "$( [ "$(echo "$BODY" | jq .totalItems)" -ge 1 ] && echo yes)" yes
CODE=$(curl -s -o /tmp/audit.csv -w '%{http_code}' "$API/admin/audit/export" -H "Authorization: Bearer $A"); check "CSV export" "$CODE" 200

echo; echo "PASSED: $pass  FAILED: $fail"; [ $fail -eq 0 ]
