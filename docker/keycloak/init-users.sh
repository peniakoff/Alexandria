#!/usr/bin/env sh
set -eu

KC_BASE_URL="${KC_BASE_URL:-http://keycloak:8080}"
KC_ADMIN_USER="${KC_ADMIN_USER:-admin}"
KC_ADMIN_PASSWORD="${KC_ADMIN_PASSWORD:-admin}"
REALM="${REALM:-alexandria}"

create_user() {
  username="$1"
  password="$2"
  first_name="$3"
  last_name="$4"

  echo "Ensuring user $username exists..."

  # Create user if missing
  existing_id="$(curl -fsS -H "Authorization: Bearer $ADMIN_TOKEN" \
    "$KC_BASE_URL/admin/realms/$REALM/users?username=$(printf %s "$username" | jq -sRr @uri)" \
    | jq -r '.[0].id // empty')"

  if [ -z "$existing_id" ]; then
    curl -fsS -X POST "$KC_BASE_URL/admin/realms/$REALM/users" \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H "Content-Type: application/json" \
      -d "{
        \"username\": \"$username\",
        \"email\": \"$username\",
        \"firstName\": \"$first_name\",
        \"lastName\": \"$last_name\",
        \"enabled\": true,
        \"emailVerified\": true,
        \"requiredActions\": []
      }" >/dev/null
    existing_id="$(curl -fsS -H "Authorization: Bearer $ADMIN_TOKEN" \
      "$KC_BASE_URL/admin/realms/$REALM/users?username=$(printf %s "$username" | jq -sRr @uri)" \
      | jq -r '.[0].id')"
  fi

  # Set password (non-temporary)
  curl -fsS -X PUT "$KC_BASE_URL/admin/realms/$REALM/users/$existing_id/reset-password" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
      \"type\": \"password\",
      \"temporary\": false,
      \"value\": \"$password\"
    }" >/dev/null
}

echo "Waiting for Keycloak..."
for i in $(seq 1 60); do
  if curl -fsS "$KC_BASE_URL/realms/master" >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

echo "Obtaining admin token..."
ADMIN_TOKEN="$(curl -fsS -X POST "$KC_BASE_URL/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "grant_type=password" \
  --data-urlencode "client_id=admin-cli" \
  --data-urlencode "username=$KC_ADMIN_USER" \
  --data-urlencode "password=$KC_ADMIN_PASSWORD" \
  | jq -r .access_token)"

if [ -z "$ADMIN_TOKEN" ] || [ "$ADMIN_TOKEN" = "null" ]; then
  echo "Failed to get admin token" >&2
  exit 1
fi

create_user "admin@alexandria.local" "Admin123!" "Admin" "Alexandria"
create_user "user@alexandria.local" "User1234!" "Jan" "Kowalski"

echo "Keycloak init done."

