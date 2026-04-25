#!/bin/bash
# Capture 15 slides via headless Chrome at 1920x1080
set -e
cd "$(dirname "$0")"
BASE="http://127.0.0.1:8765/sports-ai-pitch-deck.html"
CHROME="/c/Program Files/Google/Chrome/Application/chrome.exe"
PROFILE="$PWD/chrome-profile"
rm -rf slides chrome-profile
mkdir -p slides

for i in $(seq 1 15); do
  N=$(printf "%02d" $i)
  echo "Capturing slide $N..."
  "$CHROME" --headless=new --disable-gpu --hide-scrollbars \
    --window-size=1920,1080 --user-data-dir="$PROFILE" \
    --virtual-time-budget=3000 \
    --screenshot="$PWD/slides/slide-$N.png" \
    "$BASE#$i" 2>&1 | tail -1
done

ls -la slides/
