#!/usr/bin/env bash
# Testes JUnit4 da lógica pura (sem device/emulador). Compila o app + tests
# contra android.jar e roda no JVM do host. Baixa junit/hamcrest/org.json
# (do Maven Central) na primeira execução.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

ANDROID_JAR="${ANDROID_JAR:-$ROOT/libs/android.jar}"
JUNIT="$ROOT/libs/junit-4.13.2.jar"
HAMCREST="$ROOT/libs/hamcrest-core-1.3.jar"
ORGJSON="$ROOT/libs/org.json-20240303.jar"
OUT="$ROOT/out/test"

if [ ! -f "$ANDROID_JAR" ]; then
    echo "ERRO: $ANDROID_JAR não existe. Rode 'bash scripts/build.sh' antes."
    exit 1
fi

fetch() {
    local url="$1" dest="$2"
    if [ ! -f "$dest" ]; then
        echo "* Baixando $(basename "$dest")…"
        curl -fsSL --max-time 120 -o "$dest" "$url"
    fi
}

fetch "https://repo1.maven.org/maven2/junit/junit/4.13.2/junit-4.13.2.jar" "$JUNIT"
fetch "https://repo1.maven.org/maven2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar" "$HAMCREST"
fetch "https://repo1.maven.org/maven2/org/json/json/20240303/json-20240303.jar" "$ORGJSON"

rm -rf "$OUT"
mkdir -p "$OUT/main-classes" "$OUT/test-classes"

echo "[1/3] javac app (release 8)"
find src -name '*.java' > "$OUT/app-sources.txt"
javac --release 8 -encoding UTF-8 \
    -classpath "$ANDROID_JAR:libs/zxing-core.jar" \
    -d "$OUT/main-classes" @"$OUT/app-sources.txt"

echo "[2/3] javac tests"
find tests -name '*.java' > "$OUT/test-sources.txt"
javac --release 8 -encoding UTF-8 \
    -classpath "$OUT/main-classes:$JUNIT:$HAMCREST:$ORGJSON:libs/zxing-core.jar:$ANDROID_JAR" \
    -d "$OUT/test-classes" @"$OUT/test-sources.txt"

echo "[3/3] JUnitCore"
java -cp "$OUT/test-classes:$OUT/main-classes:$JUNIT:$HAMCREST:$ORGJSON:libs/zxing-core.jar:$ANDROID_JAR" \
    org.junit.runner.JUnitCore \
    mrp.checkin.core.LogTruncateTest \
    mrp.checkin.core.TokenStoreTest \
    mrp.checkin.core.DevFixturesTest \
    mrp.checkin.ScanActivityTorchPolicyTest \
    mrp.checkin.TorchWiringSourceTest \
    mrp.checkin.BottomInsetTest \
    mrp.checkin.core.ScanPhaseTest \
    mrp.checkin.core.VerifyParserTest \
    mrp.checkin.net.EndpointResolverTest \
    mrp.checkin.net.ApiClientTest \
    mrp.checkin.net.ServerScannerTest \
    mrp.checkin.scan.DecodeThreadTest
echo "OK: todos os testes passaram"