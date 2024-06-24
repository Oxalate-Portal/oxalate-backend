#!/usr/bin/env bash

TEST_SQL="$1"

DB_NAME="oxdb"
CONTAINER_NAME="dev_${DB_NAME}"

echo "Called with: ${TEST_SQL}"

if [ -z "${TEST_SQL}" ]; then
  TEST_SQL=$(cat test-data/test.sql)
fi

docker exec -it "${CONTAINER_NAME}" bash -c "echo \"${TEST_SQL}\" | psql --host=localhost --port=5432 --username oxalate ${DB_NAME}"
