echo "Leser secrets fra disk til environment"

if test -f "/secrets/database/config/jdbc_url"; then
  export DATASOURCE_URL=$(cat /secrets/database/config/jdbc_url)
  echo "Eksporterer variabel DATASOURCE_URL"
fi