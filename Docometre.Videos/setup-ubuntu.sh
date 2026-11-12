#!/bin/bash
# Installs everything needed to record the DOCoMETRe video tutorials on
# Ubuntu 24.04 (native or under WSL2 on Windows).
#
# Usage : ./setup-ubuntu.sh [release folder (default ~/DocometreLinux)]
set -e

RELEASE_DIR=${1:-$HOME/DocometreLinux}
RELEASE_URL=https://github.com/fbuloup/docometre-release/releases/download/docometre/DocometreLinux.zip

echo "== System packages"
sudo apt-get update
sudo apt-get install -y xvfb openbox xdotool x11-utils ffmpeg imagemagick bc \
	python3 fonts-inter openjdk-21-jre libgtk-3-0t64 unzip curl

echo "== DOCoMETRe Linux release -> $RELEASE_DIR"
tmp=$(mktemp -d)
curl -fL -o "$tmp/DocometreLinux.zip" "$RELEASE_URL"
rm -rf "$RELEASE_DIR"
mkdir -p "$RELEASE_DIR"
unzip -q "$tmp/DocometreLinux.zip" -d "$RELEASE_DIR"
rm -rf "$tmp"
# The zip may contain a single top folder
if [ ! -f "$RELEASE_DIR/DOCoMETRe" ] && [ $(ls "$RELEASE_DIR" | wc -l) -eq 1 ]; then
	sub="$RELEASE_DIR/$(ls "$RELEASE_DIR")"
	mv "$sub"/* "$RELEASE_DIR"/ && rmdir "$sub"
fi
chmod +x "$RELEASE_DIR/DOCoMETRe"

echo "== Folders of the workspace shown in the videos and of the running application"
for d in /home/researcher /opt/DOCoMETRe; do
	if [ ! -w "$d" ]; then
		sudo mkdir -p "$d"
		sudo chown "$(id -u):$(id -g)" "$d"
	fi
done

echo "== arduino-cli placeholder (tutorial 2 checks that the tool exists)"
if [ ! -e /usr/local/bin/arduino-cli ]; then
	printf '#!/bin/sh\necho "arduino-cli placeholder (video recording)"\n' | sudo tee /usr/local/bin/arduino-cli >/dev/null
	sudo chmod +x /usr/local/bin/arduino-cli
fi

echo
echo "Ready. Record a tutorial with, for example :"
echo "  $(dirname "$(realpath "$0")")/01-experiment-subjects-sessions.sh $RELEASE_DIR ~/videos"
