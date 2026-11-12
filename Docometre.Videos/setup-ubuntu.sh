#!/bin/bash
# Installs everything needed to record the DOCoMETRe video tutorials on
# Ubuntu 24.04 (native or under WSL2 on Windows).
#
# Usage : ./setup-ubuntu.sh [release folder (default ~/DocometreLinux)]
set -e

RELEASE_DIR=${1:-$HOME/DocometreLinux}
RELEASE_URL=https://github.com/fbuloup/docometre-release/releases/download/docometre/DocometreLinux.zip
# Versions of the Arduino tools (same conditions on every recording machine)
ARDUINO_CLI_VERSION=1.5.1
ARDUINO_CLI_SHA256=28a8e119c498a25607821c36cb2dc49e8463941b261a0d99091baa7bc692dd2b
AVR_VERSION=1.8.8
RENESAS_UNO_VERSION=1.6.0
ADS1X15_VERSION=0.6.2
ARDUINO_CLI=/usr/local/bin/arduino-cli

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

echo "== arduino-cli $ARDUINO_CLI_VERSION, Uno R3 and Uno R4 WiFi platforms, ADS1X15 library"
if ! "$ARDUINO_CLI" version 2>/dev/null | grep -q "Version: $ARDUINO_CLI_VERSION "; then
	tmp=$(mktemp -d)
	curl -fL -o "$tmp/arduino-cli.tar.gz" \
		"https://downloads.arduino.cc/arduino-cli/arduino-cli_${ARDUINO_CLI_VERSION}_Linux_64bit.tar.gz"
	echo "$ARDUINO_CLI_SHA256  $tmp/arduino-cli.tar.gz" | sha256sum -c --quiet
	tar -xzf "$tmp/arduino-cli.tar.gz" -C "$tmp" arduino-cli
	sudo install -m 755 "$tmp/arduino-cli" "$ARDUINO_CLI"
	rm -rf "$tmp"
fi
# Installed in ~/.arduino15 and ~/Arduino (about 1 GB)
"$ARDUINO_CLI" core update-index
"$ARDUINO_CLI" core install "arduino:avr@$AVR_VERSION" "arduino:renesas_uno@$RENESAS_UNO_VERSION"
"$ARDUINO_CLI" lib install "ADS1X15@$ADS1X15_VERSION"

echo
echo "Ready. Record a tutorial with, for example :"
echo "  $(dirname "$(realpath "$0")")/01-experiment-subjects-sessions.sh $RELEASE_DIR ~/videos"
