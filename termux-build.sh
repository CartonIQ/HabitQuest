#!/data/data/com.termux/files/usr/bin/bash
# ==============================================================================
# HabitQuest Premium - 1-Click Termux Build Script
# ==============================================================================
set -e

CYAN='\033[0;36m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${CYAN}======================================================${NC}"
echo -e "${CYAN}    🛡️  HabitQuest Premium — Termux Build Engine       ${NC}"
echo -e "${CYAN}======================================================${NC}"

# 1. Environment & Package Check
echo -e "\n${YELLOW}[1/5] Checking required Termux packages...${NC}"
MISSING_PKGS=""

command -v java >/dev/null 2>&1 || MISSING_PKGS="$MISSING_PKGS openjdk-17"
command -v aapt2 >/dev/null 2>&1 || MISSING_PKGS="$MISSING_PKGS aapt2"
command -v gradle >/dev/null 2>&1 || MISSING_PKGS="$MISSING_PKGS gradle"

if [ -n "$MISSING_PKGS" ]; then
    echo -e "${RED}Missing required packages:${MISSING_PKGS}${NC}"
    echo -e "Installing now via pkg..."
    pkg update -y
    pkg install -y $MISSING_PKGS
else
    echo -e "${GREEN}✓ Java, AAPT2, and Gradle packages are installed.${NC}"
fi

# 2. AAPT2 Architecture Fix (ARM / Termux)
echo -e "\n${YELLOW}[2/5] Configuring ARM AAPT2 override...${NC}"
TERMUX_AAPT2="/data/data/com.termux/files/usr/bin/aapt2"
if [ -f "$TERMUX_AAPT2" ]; then
    if ! grep -q "^android.aapt2FromMavenOverride=" gradle.properties; then
        echo "android.aapt2FromMavenOverride=$TERMUX_AAPT2" >> gradle.properties
        echo -e "${GREEN}✓ Added android.aapt2FromMavenOverride to gradle.properties${NC}"
    else
        echo -e "${GREEN}✓ AAPT2 override already configured in gradle.properties${NC}"
    fi
else
    echo -e "${YELLOW}! Termux aapt2 not found at standard path. If build fails, run: pkg install aapt2${NC}"
fi

# 3. Android SDK Location Detection
echo -e "\n${YELLOW}[3/5] Checking Android SDK location...${NC}"
SDK_DIR=""

if [ -n "$ANDROID_HOME" ] && [ -d "$ANDROID_HOME" ]; then
    SDK_DIR="$ANDROID_HOME"
elif [ -d "$PREFIX/share/android-sdk" ]; then
    SDK_DIR="$PREFIX/share/android-sdk"
elif [ -d "$HOME/android-sdk" ]; then
    SDK_DIR="$HOME/android-sdk"
elif [ -d "$HOME/Android/Sdk" ]; then
    SDK_DIR="$HOME/Android/Sdk"
elif [ -f "local.properties" ]; then
    SDK_DIR=$(grep "^sdk.dir=" local.properties | cut -d'=' -f2)
fi

if [ -n "$SDK_DIR" ] && [ -d "$SDK_DIR" ]; then
    echo "sdk.dir=$SDK_DIR" > local.properties
    echo -e "${GREEN}✓ SDK configured: $SDK_DIR${NC}"
else
    echo -e "${YELLOW}! Android SDK not detected in standard Termux paths.${NC}"
    echo -e "If you haven't installed commandline tools yet, you can set it in local.properties:"
    echo -e "  echo 'sdk.dir=/data/data/com.termux/files/home/android-sdk' > local.properties"
fi

# 4. Building Debug APK
echo -e "\n${YELLOW}[4/5] Building HabitQuest APK...${NC}"
if command -v gradle >/dev/null 2>&1; then
    gradle assembleDebug --stacktrace
else
    ./gradlew assembleDebug --stacktrace
fi

# 5. Output Verification
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    SIZE=$(du -h "$APK_PATH" | cut -f1)
    echo -e "\n${GREEN}======================================================${NC}"
    echo -e "${GREEN}  🎉 BUILD SUCCESSFUL!${NC}"
    echo -e "${GREEN}  APK Location: ${APK_PATH} (${SIZE})${NC}"
    echo -e "${GREEN}======================================================${NC}"
    echo -e "To install on your Android device right now, run:"
    echo -e "  ${CYAN}termux-open ${APK_PATH}${NC}"
    echo -e "Or copy to internal storage:"
    echo -e "  ${CYAN}cp ${APK_PATH} /sdcard/Download/HabitQuest-v1.2.0.apk${NC}\n"
else
    echo -e "\n${RED}Build completed, but APK not found at expected location.${NC}"
    exit 1
fi
