#!/usr/bin/env bash
# Assina o APK gerado por build.sh com o keystore de release.
# Credenciais: env vars KEYSTORE_PATH/KEYSTORE_PASS/KEY_ALIAS ou o arquivo
# $HOME/.keys/mrp-release.env criado por scripts/keystore-create.sh.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

ENV_FILE="${KEYSTORE_ENV_FILE:-$HOME/.keys/mrp-release.env}"
if [ -f "$ENV_FILE" ]; then
    # gerar o arquivo com: bash scripts/keystore-create.sh
    # shellcheck source=/dev/null
    source "$ENV_FILE"
fi

: "${KEYSTORE_PATH:?Defina KEYSTORE_PATH ou rode scripts/keystore-create.sh}"
: "${KEYSTORE_PASS:?Defina KEYSTORE_PASS ou rode scripts/keystore-create.sh}"
: "${KEY_ALIAS:?Defina KEY_ALIAS ou rode scripts/keystore-create.sh}"

INPUT="${1:-$ROOT/out/app-unsigned.apk}"
OUTPUT="${2:-$ROOT/out/mrp-checkin-release.apk}"

apksigner sign \
    --ks "$KEYSTORE_PATH" \
    --ks-key-alias "$KEY_ALIAS" \
    --ks-pass "pass:$KEYSTORE_PASS" \
    --key-pass "pass:$KEYSTORE_PASS" \
    --min-sdk-version 26 \
    --out "$OUTPUT" \
    "$INPUT"

apksigner verify --print-certs "$OUTPUT"

DOWNLOAD_DIR="$HOME/storage/downloads"
DOWNLOAD_NAME="$(basename "$OUTPUT")"
if [ -d "$DOWNLOAD_DIR" ]; then
    cp -f "$OUTPUT" "$DOWNLOAD_DIR/$DOWNLOAD_NAME"
    SUM="$(md5sum "$DOWNLOAD_DIR/$DOWNLOAD_NAME" | cut -d' ' -f1)"
    echo
    echo "Copiado para: $DOWNLOAD_DIR/$DOWNLOAD_NAME"
    echo "MD5: $SUM"
else
    echo
    echo "AVISO: $DOWNLOAD_DIR nao existe. Rode 'termux-setup-storage' uma vez e copie o APK manualmente."
fi

echo
echo "Assinado e verificado: $OUTPUT"
echo "Instalar: termux-open ~/storage/downloads/$DOWNLOAD_NAME"