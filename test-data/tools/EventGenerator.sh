#!/usr/bin/env bash

# This script generates a set of events for testing purposes. It takes one argument, the number of events to generate.
# If no argument is given, 100 events are generated.
# It generates events half of which are in the past, and half of which are in the future.
# The script expects that the database is freshly initiated with generated data from the create_local_users.sh script.

numberOfEvents=$1

DIVE_TYPE=("Avo" "Avo / luola" "Luola" "Hylky" "Yö" "Riutta")
EVENT_ID=5
############################################## Functions

function generateEvents() {
  local eventCount=$1
  local direction=$2
  local descriptionText=$3
  local -n EVENT_LIST=$4
  local -n PARTICIPANT_LIST=$5
  local DAYS_BETWEEN_EVENTS=$((3 + RANDOM % 5))

  for i in $(seq 1 "${DAYS_BETWEEN_EVENTS}" "${eventCount}"); do
    local start_date=$(date -d "${direction}${i} days" +"%Y-%m-%d")
    local time=$((8 + RANDOM % 6)):00:00
    local description="Sukellusta ${i} ${descriptionText}, kaikki mukaan"
    local event_duration=$((4 + RANDOM % 5))
    local max_depth=$((30 + RANDOM % 10))
    local max_duration=$((60 + RANDOM % 80))
    local max_participants=$((4 + RANDOM % 16))
    local published="true"
    local start_time="${start_date} ${time}"
    local title="Sukellus ${i} ${descriptionText}"
    local type=${DIVE_TYPE[$RANDOM % ${#DIVE_TYPE[@]}]}
    local organizer_id=$((1 + RANDOM % 4))
    # Add all users from 1 to 6 except organizer as participants to all events
    for j in $(seq 1 6); do
      if [ "${j}" -ne "${organizer_id}" ]; then
        PARTICIPANT_LIST+=("(${j}, ${EVENT_ID})")
      fi
    done

    EVENT_LIST+=("('${description}', ${event_duration}, ${max_depth}, ${max_duration}, ${max_participants}, ${published}, '${start_time}', '${title}', '${type}', ${organizer_id})")
    EVENT_ID=$((EVENT_ID + 1))
  done
}

function joinByChar() {
  local IFS="$1"
  shift
  echo "$*"
}

############################################## Main

if [ -z "${numberOfEvents}" ] || [[ ${numberOfEvents} -lt 2 ]]; then
  numberOfEvents=100
fi

pastEvents=$((numberOfEvents / 2))
futureEvents=$((numberOfEvents - pastEvents))
echo "The number of events to create is ${numberOfEvents}, ${pastEvents} in the past, ${futureEvents} in the future. Do you want to proceed? [y/n]"
read -r answer

if [ "${answer}" != "y" ]; then
  exit 0
fi

# Create the past events

EVENT_VALUE_ARRAY=()
EVENT_PARTICIPANT_ARRAY=()
generateEvents ${pastEvents} "-" "päivää sitten" EVENT_VALUE_ARRAY EVENT_PARTICIPANT_ARRAY
generateEvents ${futureEvents} "+" "päivän päästä" EVENT_VALUE_ARRAY EVENT_PARTICIPANT_ARRAY

EVENT_SQL_STRING="INSERT INTO events (description, event_duration, max_depth, max_duration, max_participants, published,\
start_time, title, type, organizer_id) VALUES "
printf -v EVENT_DATA_STRING '%s,' "${EVENT_VALUE_ARRAY[@]}"
# We have a trailing comma, remove it
INSERT_EVENT_DATA_STRING="${EVENT_SQL_STRING}${EVENT_DATA_STRING%,};"
../../create_local_users.sh "${INSERT_EVENT_DATA_STRING}"

PARTICIPANT_SQL_STRING="INSERT INTO event_participants (user_id, event_id) VALUES "
printf -v PARTICIPANT_DATA_STRING '%s,' "${EVENT_PARTICIPANT_ARRAY[@]}"
# We have a trailing comma, remove it
INSERT_PARTICIPANT_DATA_STRING="${PARTICIPANT_SQL_STRING}${PARTICIPANT_DATA_STRING%,};"
../../create_local_users.sh "${INSERT_PARTICIPANT_DATA_STRING}"

exit 0