#!/bin/sh
set -eu

psql \
  -v ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" \
  --set admin_username="${ADMIN_USERNAME:-admin}" \
  --set admin_password="${ADMIN_PASSWORD:-123456}" \
  --set admin_nickname="${ADMIN_NICKNAME:-管理员}" \
  --set admin_real_name="${ADMIN_REAL_NAME:-管理员}" \
  --set admin_mobile="${ADMIN_MOBILE:-13600000000}" <<'SQL'
INSERT INTO app_user (
    username,
    password_hash,
    role_type,
    nickname,
    real_name,
    mobile,
    enabled
) VALUES (
    :'admin_username',
    :'admin_password',
    'admin',
    :'admin_nickname',
    :'admin_real_name',
    :'admin_mobile',
    TRUE
)
ON CONFLICT (username) DO NOTHING;
SQL
