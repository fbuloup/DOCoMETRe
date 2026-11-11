#!/bin/bash
# Helpers to record DOCoMETRe video tutorials on a virtual X display.
#
# Requirements : Xvfb, openbox, xdotool, ffmpeg, ImageMagick (import) and a
# DOCoMETRe release unzipped in $APP_DIR.
#
# A scenario script sources this file, calls start_session, then uses
# move/click/type_text/key/say/pause, and finally calls end_session.
# Subtitles written with "say" are stored with their time stamps and burnt
# into the final video by make_video.

: "${DISPLAY_NUM:=:99}"
: "${SCREEN_W:=1600}"
: "${SCREEN_H:=900}"
: "${FPS:=25}"
: "${TYPE_DELAY:=70}"

export DISPLAY=$DISPLAY_NUM
export LANG=C.UTF-8 LC_ALL=C.UTF-8

# ---------------------------------------------------------------- display
start_display() {
	if ! xdpyinfo -display "$DISPLAY" >/dev/null 2>&1; then
		Xvfb "$DISPLAY" -screen 0 ${SCREEN_W}x${SCREEN_H}x24 >/dev/null 2>&1 &
		sleep 2
	fi
	pgrep -x openbox >/dev/null || { openbox >/dev/null 2>&1 & sleep 1; }
}

# ---------------------------------------------------------------- recording
start_recording() {
	RAW_VIDEO="$WORK_DIR/raw.mp4"
	SUBS_LOG="$WORK_DIR/subs.log"
	: > "$SUBS_LOG"
	ffmpeg -loglevel error -y -f x11grab -draw_mouse 1 -framerate $FPS \
		-video_size ${SCREEN_W}x${SCREEN_H} -i "$DISPLAY" \
		-c:v libx264 -preset veryfast -crf 18 -pix_fmt yuv420p "$RAW_VIDEO" &
	FFMPEG_PID=$!
	sleep 0.5
	T0=$(date +%s.%N)
}

stop_recording() {
	echo "$(now) CLEAR" >> "$SUBS_LOG"
	kill -INT $FFMPEG_PID
	wait $FFMPEG_PID 2>/dev/null
}

now() { echo "$(date +%s.%N) - $T0" | bc; }

# ---------------------------------------------------------------- subtitles
# say "text" [seconds] : display text as subtitle (until next say/clear) and
# wait the given number of seconds (default : reading time of the text)
say() {
	local text="$1" wait="$2"
	echo "$(now) $text" >> "$SUBS_LOG"
	if [ -z "$wait" ]; then
		local words=$(echo "$text" | wc -w)
		wait=$(echo "scale=1; 1.2 + $words * 0.33" | bc)
	fi
	sleep "$wait"
}
clear_subtitle() { echo "$(now) CLEAR" >> "$SUBS_LOG"; }

# ---------------------------------------------------------------- mouse/keyboard
# Smooth mouse move to x y
move() {
	local x=$1 y=$2 steps=${3:-20}
	eval $(xdotool getmouselocation --shell)
	local i
	for i in $(seq 1 $steps); do
		# ease in/out
		local t=$(echo "scale=4; $i/$steps" | bc)
		local e=$(echo "scale=4; $t*$t*(3-2*$t)" | bc)
		xdotool mousemove $(echo "$X + ($x - $X)*$e" | bc | cut -d. -f1) \
		                  $(echo "$Y + ($y - $Y)*$e" | bc | cut -d. -f1)
		sleep 0.015
	done
	xdotool mousemove $x $y
}
click() { move $1 $2; sleep 0.25; xdotool click ${3:-1}; sleep 0.4; }
double_click() { move $1 $2; sleep 0.25; xdotool click --repeat 2 --delay 80 1; sleep 0.4; }
right_click() { click $1 $2 3; }
type_text() { xdotool type --delay $TYPE_DELAY "$1"; sleep 0.3; }
key() { xdotool key "$@"; sleep 0.3; }
pause() { sleep "$1"; }
# Select all text of focused field then type new text
replace_text() { key ctrl+a; type_text "$1"; }

# Wait for a window whose title matches regexp (timeout in seconds)
wait_window() {
	local pattern="$1" timeout=${2:-30} i
	for i in $(seq 1 $((timeout * 4))); do
		xdotool search --onlyvisible --name "$pattern" >/dev/null 2>&1 && return 0
		sleep 0.25
	done
	echo "Window '$pattern' not found" >&2
	return 1
}

snapshot() { import -window root "$WORK_DIR/$1.png"; }

# ---------------------------------------------------------------- application
start_docometre() {
	# Fresh copy of the release : no remembered workspace, no preferences
	rm -rf "$WORK_DIR/app" "$WORK_DIR/home"
	cp -r "$APP_DIR" "$WORK_DIR/app"
	mkdir -p "$WORK_DIR/home"
	(cd "$WORK_DIR/app" && HOME="$WORK_DIR/home" ./DOCoMETRe -nl en > "$WORK_DIR/docometre.log" 2>&1 &)
}

stop_docometre() {
	pkill -f "$WORK_DIR/app" 2>/dev/null
	sleep 2
}

# ---------------------------------------------------------------- final video
# Builds subtitles file (ASS) from subs.log and burns it into the raw video,
# with a title card at the beginning.
make_video() {
	local output="$1" title="$2" subtitle="$3"
	local ass="$WORK_DIR/subs.ass"
	python3 - "$SUBS_LOG" "$ass" "$SCREEN_W" "$SCREEN_H" "${output%.mp4}.srt" <<'EOF'
import sys
log, ass, w, h, srt = sys.argv[1], sys.argv[2], int(sys.argv[3]), int(sys.argv[4]), sys.argv[5]
def ts(t):
    t = max(0.0, t)
    hh = int(t // 3600); mm = int(t % 3600 // 60); ss = t % 60
    return "%d:%02d:%05.2f" % (hh, mm, ss)
entries = []
for line in open(log, encoding="utf-8"):
    line = line.rstrip("\n")
    if not line: continue
    t, text = line.split(" ", 1)
    entries.append((float(t), text))
out = open(ass, "w", encoding="utf-8")
out.write("""[Script Info]
ScriptType: v4.00+
PlayResX: %d
PlayResY: %d
WrapStyle: 0

[V4+ Styles]
Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
Style: Default,Inter,30,&H00FFFFFF,&H00FFFFFF,&HB4201A14,&H00000000,0,0,0,0,100,100,0,0,3,14,0,2,160,160,36,1

[Events]
Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
""" % (w, h))
for i, (t, text) in enumerate(entries):
    if text == "CLEAR": continue
    end = entries[i + 1][0] if i + 1 < len(entries) else t + 4
    out.write("Dialogue: 0,%s,%s,Default,,0,0,0,,%s\n" % (ts(t), ts(end - 0.05), text.replace("\\n", "\\N")))
out.close()
# Same subtitles as a .srt file, shifted by the duration of the title card
def srt_ts(t):
    ms = int(round(t * 1000))
    return "%02d:%02d:%02d,%03d" % (ms // 3600000, ms // 60000 % 60, ms // 1000 % 60, ms % 1000)
out = open(srt, "w", encoding="utf-8")
n = 0
for i, (t, text) in enumerate(entries):
    if text == "CLEAR": continue
    end = entries[i + 1][0] if i + 1 < len(entries) else t + 4
    n += 1
    out.write("%d\n%s --> %s\n%s\n\n" % (n, srt_ts(t + 4), srt_ts(end - 0.05 + 4), text.replace("\\n", "\n")))
out.close()
EOF
	# Title card (4 s)
	local font=/usr/share/fonts/opentype/inter/Inter-SemiBold.otf
	local font2=/usr/share/fonts/opentype/inter/Inter-Regular.otf
	ffmpeg -loglevel error -y -f lavfi -i "color=c=0x1f3b57:s=${SCREEN_W}x${SCREEN_H}:d=4:r=$FPS" \
		-vf "drawtext=fontfile=$font:text='DOCoMETRe':fontcolor=0xffffff:fontsize=44:x=(w-tw)/2:y=h/2-120,\
drawtext=fontfile=$font:text='$title':fontcolor=0xffffff:fontsize=64:x=(w-tw)/2:y=h/2-40,\
drawtext=fontfile=$font2:text='$subtitle':fontcolor=0xc8d6e5:fontsize=34:x=(w-tw)/2:y=h/2+60,\
fade=t=in:st=0:d=0.5,fade=t=out:st=3.5:d=0.5" \
		-c:v libx264 -preset veryfast -crf 18 -pix_fmt yuv420p "$WORK_DIR/title.mp4"
	ffmpeg -loglevel error -y -i "$RAW_VIDEO" -vf "ass=$ass,fade=t=in:st=0:d=0.5" \
		-c:v libx264 -preset medium -crf 20 -pix_fmt yuv420p -r $FPS "$WORK_DIR/body.mp4"
	printf "file '%s'\nfile '%s'\n" "$WORK_DIR/title.mp4" "$WORK_DIR/body.mp4" > "$WORK_DIR/concat.txt"
	ffmpeg -loglevel error -y -f concat -safe 0 -i "$WORK_DIR/concat.txt" -c copy -movflags +faststart "$output"
}
