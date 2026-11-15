#!/usr/bin/env bash
# Restaura un respaldo de GymDesk360 sobre la base actual (REEMPLAZA todos los datos).
#
# Uso:  ./scripts/restore-db.sh ~/backups/gymdesk360/gymdesk360-db_2026-11-15_0300.dump
set -euo pipefail

FILE="${1:-}"
if [ -z "$FILE" ] || [ ! -f "$FILE" ]; then
    echo "Uso: $0 <archivo.dump>" >&2
    echo "Respaldos disponibles:" >&2
    ls -1t "$HOME/backups/gymdesk360-demo/"*.dump 2>/dev/null | head -10 >&2
    exit 1
fi

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$APP_DIR"

echo "Se reemplazarán TODOS los datos actuales con: $FILE"
read -r -p "Escribe RESTAURAR para continuar: " CONFIRM
[ "$CONFIRM" = "RESTAURAR" ] || { echo "Cancelado."; exit 1; }

# Respaldo de seguridad del estado actual antes de pisarlo
BACKUP_KEEP=1000 "$APP_DIR/scripts/backup-db.sh"

# Detener la API para que nadie escriba mientras se restaura
docker compose stop backend
trap 'docker compose start backend >/dev/null' EXIT

docker compose exec -T postgres sh -c 'pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists --no-owner --single-transaction' < "$FILE"

echo "Restauración completada. Iniciando backend..."
