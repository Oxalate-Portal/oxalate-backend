#!/usr/bin/env bash

# This script generates a random set of users and past events for statistics purposes.
# The script expects that the database is freshly initiated with generated data from the create_local_users.sh script.
USER_ID_START=6

DIVE_TYPE=("Avo" "Avo / luola" "Luola" "Hylky" "Yö" "Riutta")
KIN_TYPE=("Isä" "Äiti" "Puoliso" "Sisarus" "Lapsi" "Ystävä")
EVENT_ID=5
############################################## Functions
source ./commonFunctions.sh

function generateUsers() {
  # Create 60-140 users
  local userCount=$((60 + RANDOM % 80))
  local USER_ID_END=$((USER_ID_START + userCount))
  for i in $(seq "${USER_ID_START}" "${USER_ID_END}"); do
    local uuidString=$(uuidgen)
    local email_address="${uuidString}@a.tld"
    local first_name=$(echo "${uuidString}" | cut -d '-' -f1)
    local last_name=$(echo "${uuidString}" | cut -d '-' -f5)
    local password=$(htpasswd -bnBC 10 "" "${first_name}" | tr -d ':\n')
    local phone_number=$(generatePhoneNumber)
    local next_of_kin=${KIN_TYPE[$RANDOM % ${#KIN_TYPE[@]}]}
    next_of_kin="${next_of_kin} ================ $(generatePhoneNumber)"
    echo "Email: ${email_address} ${first_name} ${last_name} ${phone_number} ${password} ${next_of_kin}"
    USERS_INSERT_ARRAY+=("('${email_address}', '${first_name}', '${last_name}', '${phone_number}', '${password}', '${next_of_kin}', false, 'ACTIVE')")
  done
}

############################################## Main

# Create the users
USERS_INSERT_ARRAY=()

generateUsers

USER_SQL_STRING="INSERT INTO users (username, first_name, last_name, phone_number, password, next_of_kin, privacy, status) VALUES "
printf -v USER_DATA_STRING '%s,' "${USERS_INSERT_ARRAY[@]}"
INSERT_USER_SQL_STRING="${USER_SQL_STRING}${USER_DATA_STRING%,};"

echo "==========================================="
echo "${INSERT_USER_SQL_STRING}"
echo "==========================================="

../../create_local_users.sh "${INSERT_USER_SQL_STRING}"