#!/usr/bin/env bash

DB_NAME="oxdb"
CONTAINER_NAME="dev_${DB_NAME}"
VOLUME_NAME="${DB_NAME}_volume"

docker rm -f ${CONTAINER_NAME}
docker volume rm ${VOLUME_NAME}

docker run -d -p 5432:5432 -v "${VOLUME_NAME}:/var/lib/postgresql/data/" \
  -e "POSTGRES_HOST_AUTH_METHOD=trust" \
  --name "${CONTAINER_NAME}" postgres:15

docker exec -it "${CONTAINER_NAME}" bash -c "\
    while ! psql --host=localhost --port=5432 --username postgres <<< exit 2>> /dev/null; do\
        echo \"Waiting for postgres to start\";\
        sleep 0.5;\
    done;\
    echo \"Re-creating DB...\";\
    createdb ${DB_NAME} -U postgres && echo \"${DB_NAME} created!\""
