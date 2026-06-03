#!/usr/bin/env bash
set -e

BOOTSTRAP_SECRET="${1:-change-me-in-production}"

read -p "Email: " email
read -sp "Password: " password
echo
read -p "First name [Admin]: " fname
fname="${fname:-Admin}"
read -p "Last name [User]: " lname
lname="${lname:-User}"

echo "Creando admin $email ..."

curl -s -X POST http://localhost:8081/api/auth/register-admin \
    -H "Content-Type: application/json" \
    -H "X-Bootstrap-Secret: $BOOTSTRAP_SECRET" \
    -d "$(cat <<EOF
{
  "firstName": "$fname",
  "lastName": "$lname",
  "email": "$email",
  "password": "$password",
  "phone": ""
}
EOF
)" | python3 -m json.tool 2>/dev/null || python -m json.tool 2>/dev/null || cat
