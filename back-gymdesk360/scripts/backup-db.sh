#!/usr/bin/env bash
# Respaldo diario de la base de GymDesk360 (PostgreSQL en Docker).
# pg_dump genera un volcado consistente sin detener la base. Guarda los últimos $KEEP.
#
# Uso:   ./scripts/backup-db.sh
# Cron:  0 3 * * * $HOME/docker/gymdesk360-demo/scripts/backup-db.sh >> $HOME/backups/gymdesk360-demo/backup.log 2>&1
set -euo pipefail

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEST="${BACKUP_DIR:-$HOME/backups/gymdesk360-demo}"
KEEP="${BACKUP_KEEP:-14}"
STAMP="$(date +%F_%H%M)"
FILE="$DEST/gymdesk360-db_$STAMP.dump"

umask 077
mkdir -p "$DEST"
cd "$APP_DIR"

# Formato custom (-Fc): comprimido y restaurable con pg_restore
docker compose exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc --no-owner' > "$FILE.tmp"

# Un volcado vacío o corrupto no debe reemplazar a los buenos
if [ ! -s "$FILE.tmp" ] || ! docker compose exec -T postgres pg_restore --list < "$FILE.tmp" > /dev/null; then
    rm -f "$FILE.tmp"
    echo "$(date "+%F %T") ERROR volcado inválido" >&2
    exit 1
fi
mv "$FILE.tmp" "$FILE"

# Borra los más antiguos y deja solo los últimos $KEEP
ls -1t "$DEST"/gymdesk360-db_*.dump | tail -n +$((KEEP + 1)) | xargs -r rm -f

echo "$(date "+%F %T") OK $FILE ($(du -h "$FILE" | cut -f1))"
