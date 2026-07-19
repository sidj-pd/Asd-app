#!/bin/bash
# =============================================================
# Picto Play — Release Keystore Generator
# Run this ONCE locally to create your signing keystore.
# Keep the generated file and passwords SAFE — if lost,
# you cannot update the app on the Play Store.
# =============================================================

set -e

OUTPUT_DIR="app/keystore"
KEYSTORE_FILE="$OUTPUT_DIR/release.keystore"

if [ -f "$KEYSTORE_FILE" ]; then
  echo "⚠️  Keystore already exists at $KEYSTORE_FILE"
  echo "    Delete it manually if you want to regenerate."
  exit 1
fi

mkdir -p "$OUTPUT_DIR"

echo ""
echo "============================================"
echo "  Picto Play — Release Keystore Generator"
echo "============================================"
echo ""
echo "You will be asked to set passwords and provide"
echo "your identity details. Write them down and store"
echo "them securely — you CANNOT publish updates without them."
echo ""

read -rp "Keystore password (min 6 chars): " KS_PASS
read -rp "Key alias (e.g. pictoplay):       " KEY_ALIAS
read -rp "Key password (min 6 chars):       " KEY_PASS
read -rp "Your full name (or org name):     " DNAME_CN
read -rp "Organisation / team name:         " DNAME_O
read -rp "Country code (e.g. AU, US, GB):   " DNAME_C

echo ""
echo "Generating keystore..."

keytool -genkeypair \
  -v \
  -keystore "$KEYSTORE_FILE" \
  -alias "$KEY_ALIAS" \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass "$KS_PASS" \
  -keypass "$KEY_PASS" \
  -dname "CN=$DNAME_CN, O=$DNAME_O, C=$DNAME_C"

echo ""
echo "✅ Keystore created at: $KEYSTORE_FILE"
echo ""
echo "============================================"
echo "  Next: Add the following GitHub Secrets"
echo "  Go to: GitHub Repo → Settings → Secrets"
echo "============================================"
echo ""
echo "1. KEYSTORE_BASE64"
echo "   Run this and paste the output as the secret value:"
echo ""
echo "   base64 -w 0 $KEYSTORE_FILE"
echo ""
echo "2. KEYSTORE_PASSWORD  →  $KS_PASS"
echo "3. KEY_ALIAS          →  $KEY_ALIAS"
echo "4. KEY_PASSWORD       →  $KEY_PASS"
echo ""
echo "⚠️  Add $OUTPUT_DIR/ to your .gitignore — never commit the keystore!"
echo ""

# Append to .gitignore if not already there
if ! grep -q "keystore/" .gitignore 2>/dev/null; then
  echo "app/keystore/" >> .gitignore
  echo "✅ Added app/keystore/ to .gitignore"
fi
