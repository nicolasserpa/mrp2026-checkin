#!/usr/bin/env bash
# Build CLI do APK DEV (modo offline) sem Gradle — espelha build.sh.
# Gera o package mrp.checkin.dev (lado a lado com o release mrp.checkin),
# label "MRP Dev", mesmo versionCode do release, versionName com sufixo -dev.
# Não apaga os artefatos do release em out/ (só limpa os intermediários dev).
# Pré-requisitos: os mesmos de build.sh (+ keystore p/ assinar via sign.sh).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

ANDROID_JAR="${ANDROID_JAR:-$ROOT/libs/android.jar}"
AAPT2="${AAPT2:-aapt2}"
AAPT="${AAPT:-aapt}"
R8_JAR="${R8_JAR:-$ROOT/libs/r8.jar}"
D8_CMD="${D8:-d8}"
OUT="${OUT:-$ROOT/out}"

# Limpa só os intermediários/finais dev; o release em out/ é preservado.
rm -rf "$OUT/dev-classes" "$OUT/dev-dex"
rm -f "$OUT/dev-compiled.zip" "$OUT/dev-classes.jar" \
    "$OUT/app-dev-unsigned.apk" "$OUT/AndroidManifest.dev.xml" \
    "$OUT/mrp-checkin-dev.apk" "$OUT/buildinfo-dev.txt"
mkdir -p "$OUT/dev-classes" "$OUT/dev-dex"

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

echo "[1/6] manifest dev temporário"
# Package .dev (instalável lado a lado) + label "MRP Dev" + versionName -dev.
# Activities com nome totalmente qualificado para continuarem apontando
# para as classes mrp.checkin.* (o "." seguiria o package .dev, inexistente).
sed -e 's/package="mrp\.checkin"/package="mrp.checkin.dev"/' \
    -e 's/android:versionName="\([^"]*\)"/android:versionName="\1-dev"/' \
    -e 's/android:label="@string\/app_name"/android:label="MRP Dev"/' \
    -e 's/android:name="\./android:name="mrp.checkin./g' \
    AndroidManifest.xml > "$OUT/AndroidManifest.dev.xml"
grep -o 'package="[^"]*"' "$OUT/AndroidManifest.dev.xml" | head -1
grep -o 'android:versionName="[^"]*"' "$OUT/AndroidManifest.dev.xml" | head -1
grep -o 'android:versionCode="[^"]*"' "$OUT/AndroidManifest.dev.xml" | head -1

echo "[2/6] aapt2 compile"
"$AAPT2" compile --dir res -o "$OUT/dev-compiled.zip"

echo "[3/6] aapt2 link (manifest dev)"
"$AAPT2" link -o "$OUT/app-dev-unsigned.apk" \
    -I "$ANDROID_JAR" \
    --manifest "$OUT/AndroidManifest.dev.xml" \
    --auto-add-overlay \
    --min-sdk-version 26 \
    --target-sdk-version 36 \
    "$OUT/dev-compiled.zip"

echo "[4/6] javac (release 8)"
find src -name '*.java' > "$OUT/dev-sources.txt"
javac --release 8 -encoding UTF-8 \
    -classpath "$ANDROID_JAR:libs/zxing-core.jar" \
    -d "$OUT/dev-classes" @"$OUT/dev-sources.txt"

echo "[5/6] D8 --dex (classes + zxing-core)"
jar cf "$OUT/dev-classes.jar" -C "$OUT/dev-classes" .
if [ -f "$R8_JAR" ]; then
    java -cp "$R8_JAR" com.android.tools.r8.D8 \
        --min-api 26 --lib "$ANDROID_JAR" --output "$OUT/dev-dex" \
        "$OUT/dev-classes.jar" libs/zxing-core.jar
else
    "$D8_CMD" --min-api 26 --lib "$ANDROID_JAR" --output "$OUT/dev-dex" \
        "$OUT/dev-classes.jar" libs/zxing-core.jar
fi

echo "[6/6] aapt add classes.dex"
cp "$OUT/app-dev-unsigned.apk" "$OUT/app-dev-unsigned-dex.apk"
(
    cd "$OUT/dev-dex"
    "$AAPT" add "$OUT/app-dev-unsigned-dex.apk" classes.dex
)
mv "$OUT/app-dev-unsigned-dex.apk" "$OUT/app-dev-unsigned.apk"

BUILDINFO="$OUT/buildinfo-dev.txt"
{
    echo "mrp2026-checkin buildinfo DEV (build-dev.sh)"
    echo "data:        $(date -u +%Y-%m-%dT%H:%M:%SZ)"
    echo "versao:      $(grep -o 'android:versionName="[^"]*"' "$OUT/AndroidManifest.dev.xml" | head -1)"
    echo "code:        $(grep -o 'android:versionCode="[^"]*"' "$OUT/AndroidManifest.dev.xml" | head -1)"
    echo "package:     $(grep -o 'package="[^"]*"' "$OUT/AndroidManifest.dev.xml" | head -1)"
    echo "sha256 classes.dex:  $(sha256sum "$OUT/dev-dex/classes.dex" | cut -d' ' -f1)"
    echo "sha256 app-dev-unsigned: $(sha256sum "$OUT/app-dev-unsigned.apk" | cut -d' ' -f1)"
} | tee "$BUILDINFO"

echo
echo "Assinando com o mesmo keystore do release…"
bash "$ROOT/scripts/sign.sh" "$OUT/app-dev-unsigned.apk" "$OUT/mrp-checkin-dev.apk"

echo
echo "Artefato dev: $OUT/mrp-checkin-dev.apk"
echo "Buildinfo:    $BUILDINFO"
echo "Instala lado a lado com o release (package mrp.checkin.dev, label MRP Dev)."
