function joinByChar() {
  local IFS="$1"
  shift
  echo "$*"
}

function generatePhoneNumber() {
  local phone_number=$((( RANDOM % 1000 ) + ( RANDOM % 1000 )*1000 + ( RANDOM % 1000 )*1000000 + ( RANDOM % 1000 )*1000000000))
  echo "${phone_number}"
}