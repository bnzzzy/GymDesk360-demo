#!/usr/bin/env bash
# Despliega la DEMO de GymDesk360 en el NUC desde tu PC (Git Bash / Linux / macOS).
#
#   ./scripts/deploy.sh back     # solo backend
#   ./scripts/deploy.sh front    # solo frontend
#   ./scripts/deploy.sh          # ambos
#
# Copia el código por SSH (no necesita que el NUC tenga acceso a GitHub),
# reconstruye la imagen y espera a que el servicio responda.
# Nunca envía ni pisa el .env del servidor.
set -euo pipefail

HOST="${DEPLOY_HOST:-nuc}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGET="${1:-all}"

EXCLUDES=(--exclude=./.env --exclude=./target --exclude=./node_modules --exclude=./dist
          --exclude=./.angular --exclude=./.idea --exclude=./.vscode --exclude=./.git)

log() { printf '\033[1;33m==>\033[0m %s\n' "$*"; }
fail() { printf '\033[1;31mERROR:\033[0m %s\n' "$*" >&2; exit 1; }

# Avisa si hay cambios sin commitear (se despliega lo que hay en disco)
if [ -n "$(git -C "$ROOT" status --porcelain 2>/dev/null)" ]; then
    log "Ojo: hay cambios sin commitear; se despliega el código tal como está en disco."
fi

# deploy <carpeta local> <carpeta remota> <servicio> <url de salud> <código esperado>
deploy() {
    local src="$1" dest="$2" service="$3" health="$4" expected="$5"
    log "Desplegando $service en $HOST:$dest"

    # Se borra src/ (y public/ en el front) en el destino para no dejar archivos eliminados
    tar "${EXCLUDES[@]}" -czf - -C "$ROOT/$src" . 2>/dev/null | ssh "$HOST" "
        set -e
        mkdir -p $dest && cd $dest
        rm -rf src public
        tar -xzf - 2>/dev/null
        chmod +x scripts/*.sh 2>/dev/null || true
        [ -f compose.yaml ] || { echo 'Falta compose.yaml' >&2; exit 1; }
        docker compose up -d --build $service 2>&1 | grep -E 'ERROR|error:|npm error|Started|Running' || true
    " || fail "falló el despliegue de $service"

    log "Esperando a que $service responda..."
    for _ in $(seq 1 45); do
        code="$(ssh "$HOST" "curl -s -o /dev/null -w '%{http_code}' $health" || true)"
        if [ "$code" = "$expected" ]; then
            log "$service OK ($health -> $code)"
            return 0
        fi
        sleep 2
    done
    ssh "$HOST" "cd $dest && docker compose logs --tail 30 $service" || true
    fail "$service no respondió $expected en $health (último: ${code:-sin respuesta})"
}

case "$TARGET" in
    back | all) deploy back-gymdesk360 '~/docker/gymdesk360-demo' backend http://127.0.0.1:8084/api/auth/me 401 ;;&
    front | all) deploy gymdesk360-front '~/docker/gymdesk360-demo-front' frontend http://127.0.0.1:8085/ 200 ;;
    back) ;;
    *) fail "uso: $0 [back|front|all]" ;;
esac

ssh "$HOST" 'docker image prune -f >/dev/null' || true
log "Listo."
