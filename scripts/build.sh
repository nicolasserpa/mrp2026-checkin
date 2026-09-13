#!/usr/bin/env bash
# Build CLI do APK sem Gradle (Termux/Android/Linux).
# Pré-requisitos:
#   pkg install openjdk-21 aapt2 aapt apksigner
# D8/R8: usa libs/r8.jar (baixado) quando presente; senão usa 'd8' do Termux
# (nv. 3.3.20 tem bug com classes do javac 21 — por isso o preferido é o r8.jar).
# android.jar não vem no Termux: baixado de Sable/android-platforms em libs/.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

ANDROID_JAR="${ANDROID_JAR:-$ROOT/libs/android.jar}"
AAPT2="${AAPT2:-aapt2}"
AAPT="${AAPT:-aapt}"
R8_JAR="${R8_JAR:-$ROOT/libs/r8.jar}"
D8_CMD="${D8:-d8}"
OUT="${OUT:-$ROOT/out}"

rm -rf "$OUT"
mkdir -p "$OUT/classes" "$OUT/dex"

if [ ! -f "$ANDROID_JAR" ]; then
    echo "* Baixando android.jar (Sable/android-platforms)…"
    curl -fsSL --max-time 300 -o "$ANDROID_JAR" \
        "https://raw.githubusercontent.com/Sable/android-platforms/master/android-30/android.jar"
fi

if [ ! -f "$R8_JAR" ]; then
    echo "* Baixando r8.jar 8.3.37 (build-tools da Google)…"
    curl -fsSL --max-time 300 -o "$R8_JAR" \
        "https://dl.google.com/dl/android/maven2/com/android/tools/r8/8.3.37/r8-8.3.37.jar"
fi

echo "[1/5] aapt2 compile"
"$AAPT2" compile --dir res -o "$OUT/compiled.zip"

echo "[2/5] aapt2 link (versão vinda do AndroidManifest.xml)"
"$AAPT2" link -o "$OUT/app-unsigned.apk" \
    -I "$ANDROID_JAR" \
    --manifest AndroidManifest.xml \
    --auto-add-overlay \
    --min-sdk-version 26 \
    --target-sdk-version 33 \
    "$OUT/compiled.zip"

echo "[3/5] javac (release 8)"
find src -name '*.java' > "$OUT/sources.txt"
javac --release 8 -encoding UTF-8 \
    -classpath "$ANDROID_JAR:libs/zxing-core.jar" \
    -d "$OUT/classes" @"$OUT/sources.txt"

echo "[4/5] D8 --dex (classes + zxing-core)"
jar cf "$OUT/classes.jar" -C "$OUT/classes" .
if [ -f "$R8_JAR" ]; then
    java -cp "$R8_JAR" com.android.tools.r8.D8 \
        --min-api 26 --lib "$ANDROID_JAR" --output "$OUT/dex" \
        "$OUT/classes.jar" libs/zxing-core.jar
else
    "$D8_CMD" --min-api 26 --lib "$ANDROID_JAR" --output "$OUT/dex" \
        "$OUT/classes.jar" libs/zxing-core.jar
fi

echo "[5/5] aapt add classes.dex"
cp "$OUT/app-unsigned.apk" "$OUT/app-unsigned-dex.apk"
(
    cd "$OUT/dex"
    "$AAPT" add "$OUT/app-unsigned-dex.apk" classes.dex
)
mv "$OUT/app-unsigned-dex.apk" "$OUT/app-unsigned.apk"

echo
echo "Confira os endereços:"
ls -la "$ANDROID_JAR" "$R8_JAR" "$OUT/app-unsigned.apk" "$OUT/dex/classes.dex"
echo
echo "Assine com:  bash scripts/sign.sh"
echo "Artefato:    $OUT/app-unsigned.apk"