#!/usr/bin/env bash
# Copia los respaldos de la base desde el NUC a este PC (copia fuera del servidor).
# Solo descarga los archivos que aún no existen localmente.
#
#   ./scripts/pull-backups.sh                 # destino por defecto: /e/Respaldos/GymDesk360-demo-db
#   BACKUP_LOCAL=/otra/carpeta ./scripts/pull-backups.sh
set -euo pipefail

HOST="${DEPLOY_HOST:-nuc}"
REMOTE="backups/gymdesk360-demo"
LOCAL="${BACKUP_LOCAL:-/e/Respaldos/GymDesk360-demo-db}"

mkdir -p "$LOCAL"
new=0
for f in $(ssh "$HOST" "ls -1 $REMOTE/*.dump 2>/dev/null | xargs -n1 basename"); do
    if [ ! -f "$LOCAL/$f" ]; then
        scp -q "$HOST:$REMOTE/$f" "$LOCAL/$f"
        echo "descargado $f"
        new=$((new + 1))
    fi
done
echo "Listo: $new respaldo(s) nuevo(s) en $LOCAL ($(ls -1 "$LOCAL"/*.dump 2>/dev/null | wc -l) en total)"
