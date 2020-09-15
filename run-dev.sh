#!/usr/bin/env bash

readonly container_name='db-pinger-database'

readonly db_type="$1"

function print_db_usage() {
  cat <<-EOF
Use these commands to simulate a database outage:
    docker stop '$container_name'
    docker start '$container_name'

EOF
}

function on_exit() {
  docker container stop "$container_name"
  docker container rm "$container_name"
}

trap on_exit 2 3 6 9 15

case "$db_type" in
  pg|postgres*)
    docker run -d --name "$container_name" -e POSTGRES_HOST_AUTH_METHOD=trust -e POSTGRES_USER="$USER" -p 5432:5432 postgres
    print_db_usage
    clj -A:run:databases ping --jdbc-uri "jdbc:postgresql://localhost:5432/postgres?username=postgres&password=password"
    ;;
  *)
    echo "I don't know what to do about '${db_type}' databases"
    exit 1
    ;;
esac
