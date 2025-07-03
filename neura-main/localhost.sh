#!/bin/zsh

if command -v docker-compose >/dev/null 2>&1; then
  docker-compose up -d
elif docker compose version >/dev/null 2>&1; then
  docker compose up -d
else
  echo "❌ Neither 'docker-compose' nor 'docker compose' found. Please install Docker Compose."
  exit 1
fi

echo "⏳ Waiting for PostgreSQL to become available..."
until docker exec neura-postgresql pg_isready -U postgres >/dev/null 2>&1; do
  sleep 1
done

docker exec neura-postgresql psql -U postgres -d neura -c "CREATE SCHEMA IF NOT EXISTS neura;"
docker exec neura-postgresql psql -U postgres -d neura -c "ALTER SCHEMA neura OWNER TO postgres;"
docker exec neura-postgresql psql -U postgres -d neura -c "GRANT ALL ON SCHEMA neura TO postgres;"

./gradlew bootRun --args='--spring.profiles.active=local'