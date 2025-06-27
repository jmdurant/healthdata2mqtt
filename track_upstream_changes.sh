#!/bin/bash

# Health Data MQTT - Upstream Change Tracker
# Monitors key data processing files from RobertWojtowicz/export2garmin for updates

echo "🔍 Tracking Upstream Changes for healthdata2mqtt"
echo "================================================="
echo "Checking for updates to data processing algorithms..."
echo ""

# Key files that contain data processing algorithms
ALGORITHM_FILES=(
    "miscale/Xiaomi_Scale_Body_Metrics.py"
    "miscale/body_scales.py" 
    "miscale/miscale_ble.py"
    "omron/omblepy.py"
    "omron/sharedDriver.py"
    "omron/deviceSpecific/hem-6232t.py"
    "omron/deviceSpecific/hem-7150t.py"
    "omron/deviceSpecific/hem-7155t.py"
    "omron/deviceSpecific/hem-7322t.py"
    "omron/deviceSpecific/hem-7342t.py"
    "omron/deviceSpecific/hem-7361t.py"
    "omron/deviceSpecific/hem-7530t.py"
    "omron/deviceSpecific/hem-7600t.py"
)

# Fetch latest upstream changes
echo "📡 Fetching upstream changes..."
git fetch upstream

# Check if there are any changes
UPSTREAM_COMMITS=$(git rev-list HEAD..upstream/master --count)
if [ "$UPSTREAM_COMMITS" -eq 0 ]; then
    echo "✅ No upstream changes detected - you're up to date!"
    echo ""
    exit 0
fi

echo "⚠️  Found $UPSTREAM_COMMITS new commits upstream"
echo ""

# Show new commits
echo "📋 New Commits in Upstream:"
echo "----------------------------"
git log --oneline HEAD..upstream/master | head -10
echo ""

# Check for changes to algorithm files
echo "🔬 Checking Algorithm Files for Changes:"
echo "----------------------------------------"

CHANGED_FILES=()
for file in "${ALGORITHM_FILES[@]}"; do
    if git diff HEAD upstream/master --name-only | grep -q "^$file$"; then
        CHANGED_FILES+=("$file")
        echo "⚠️  CHANGED: $file"
    else
        echo "✅ No change: $file"
    fi
done

echo ""

if [ ${#CHANGED_FILES[@]} -eq 0 ]; then
    echo "✅ No algorithm files changed - only configuration/export logic updated"
    echo ""
    exit 0
fi

echo "🚨 ALGORITHM FILES NEED REVIEW:"
echo "==============================="
for file in "${CHANGED_FILES[@]}"; do
    echo ""
    echo "📄 Changes in: $file"
    echo "-------------------"
    git diff HEAD upstream/master -- "$file" | head -20
    echo "... (showing first 20 lines, use 'git diff HEAD upstream/master -- $file' for full diff)"
    echo ""
done

echo "🔧 RECOMMENDED ACTIONS:"
echo "======================"
echo "1. Review changes in algorithm files above"
echo "2. Test changes in a branch: git checkout -b upstream-sync"
echo "3. Apply algorithm updates: git checkout upstream/master -- [filename]"
echo "4. Update Android Kotlin equivalents in android/app/src/main/java/com/healthdata/mqtt/data/"
echo "5. Test all three platforms: standalone Python, Docker, Android"
echo ""
echo "📍 To apply a specific file: git checkout upstream/master -- miscale/Xiaomi_Scale_Body_Metrics.py"
echo "📍 To see full diff: git diff HEAD upstream/master -- [filename]"
echo ""

# Check for new files
echo "🆕 New Files in Upstream:"
echo "-------------------------"
NEW_FILES=$(git diff HEAD upstream/master --name-only --diff-filter=A)
if [ -n "$NEW_FILES" ]; then
    echo "$NEW_FILES"
else
    echo "No new files"
fi
echo ""

echo "Run this script regularly to stay in sync with upstream improvements!"