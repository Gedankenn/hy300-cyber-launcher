#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
export JAVA_HOME=/usr/lib/jvm/java-27-openjdk
export PATH="$JAVA_HOME/bin:$PATH"
BT="$HOME/Android/Sdk/build-tools/36.0.0"
AJ="$HOME/Android/Sdk/platforms/android-36/android.jar"

rm -rf build gen
mkdir -p build/dex gen

"$BT/aapt2" compile --dir res -o build/res.zip
"$BT/aapt2" link -o build/base.apk -I "$AJ" --manifest AndroidManifest.xml \
  -A assets \
  -R build/res.zip --java gen --min-sdk-version 21 --target-sdk-version 33 \
  --version-code 3 --version-name 2.0

javac -nowarn -source 8 -target 8 -bootclasspath "$AJ" -cp "$AJ" \
  -d build/classes $(find src gen -name "*.java")

"$BT/d8" --lib "$AJ" --min-api 21 --output build/dex $(find build/classes -name "*.class")

cp build/base.apk build/u.apk
(cd build && zip -q -j u.apk dex/classes.dex)
"$BT/zipalign" -f 4 build/u.apk build/a.apk
"$BT/apksigner" sign --ks "$HOME/.config/.android/debug.keystore" --ks-pass pass:android --key-pass pass:android \
  --out launcher.apk build/a.apk 2>/dev/null

echo "built: $(stat -c%s launcher.apk) bytes"
