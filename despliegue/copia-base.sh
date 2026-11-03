#!/usr/bin/env bash
# Copia de la base con 14 días de historial. Se programa con cron; ver backend/docs/despliegue.md.
# Una copia que solo está en este servidor no sirve si se pierde el servidor: define NEXO_COPIAS_DESTINO.
set -euo pipefail
cd "$(dirname "$0")"

destino=$(sed -n 's/^NEXO_COPIAS_DESTINO=//p' .env)
archivo="copias/nexo-$(date +%F).sql.gz"

mkdir -p copias
# Primero a un temporal: un fallo a la mitad no deja una copia incompleta que parezca buena
docker compose exec -T base pg_dump -U nexo --clean --if-exists nexo | gzip > "$archivo.tmp"
gzip --test "$archivo.tmp"
mv "$archivo.tmp" "$archivo"
find copias -name 'nexo-*.sql.gz' -mtime +14 -delete

if [ -n "$destino" ]; then
  rclone copy "$archivo" "$destino"
fi
