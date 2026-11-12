#!/bin/bash
# Helpers to record DOCoMETRe video tutorials on a virtual X display.
#
# Requirements : Xvfb, openbox, xdotool, ffmpeg, ImageMagick (import) and a
# DOCoMETRe release unzipped in $APP_DIR.
#
# A scenario script sources this file, starts DOCoMETRe (new_tutorial for the
# first one, continue_from <previous tutorial> for the next ones), calls
# start_recording, then uses move/click/type_text/key/say/pause, and finally
# calls end_tutorial and make_video.
# Subtitles written with "say" are stored with their time stamps and burnt
# into the final video by make_video.
#
# Tutorials follow each other : the state of DOCoMETRe at the end of a
# tutorial (workspace, preferences, remembered workspace) is saved in
# $STATE_DIR and restored at the beginning of the next one.

: "${DISPLAY_NUM:=:99}"
: "${SCREEN_W:=1600}"
: "${SCREEN_H:=900}"
: "${FPS:=25}"
: "${TYPE_DELAY:=70}"
: "${APP_RUN_DIR:=/opt/DOCoMETRe}"
: "${WORKSPACE:=/home/researcher/DOCoMETRe-Workspace}"
: "${STATE_DIR:=$HOME/.docometre-videos}"

export DISPLAY=$DISPLAY_NUM
export LANG=C.UTF-8 LC_ALL=C.UTF-8
# Under WSL (WSLg) or a Wayland session, make sure GTK uses the virtual X display
unset WAYLAND_DISPLAY
export GDK_BACKEND=x11

TITLE_FONT=/usr/share/fonts/opentype/inter/Inter-SemiBold.otf
TITLE_FONT2=/usr/share/fonts/opentype/inter/Inter-Regular.otf

# ---------------------------------------------------------------- checks
check_requirements() {
	local missing="" c
	for c in Xvfb openbox xdotool xdpyinfo ffmpeg import bc python3 java; do
		command -v $c >/dev/null 2>&1 || missing="$missing $c"
	done
	[ -f "$TITLE_FONT" ] || missing="$missing fonts-inter"
	if [ -n "$missing" ]; then
		echo "Missing :$missing (run setup-ubuntu.sh)" >&2
		exit 1
	fi
	if [ ! -f "$APP_DIR/DOCoMETRe" ]; then
		echo "DOCoMETRe release not found in '$APP_DIR'" >&2
		exit 1
	fi
}

# Stops everything and exits (called when the scenario can't go on)
abort() {
	echo "ERROR : $1" >&2
	[ -n "$WORK_DIR" ] && import -window root "$WORK_DIR/error.png" 2>/dev/null \
		&& echo "Screen at the time of the error : $WORK_DIR/error.png" >&2
	[ -n "$FFMPEG_PID" ] && kill -INT $FFMPEG_PID 2>/dev/null
	stop_docometre
	echo "DOCoMETRe log : $WORK_DIR/docometre.log" >&2
	exit 1
}

# ---------------------------------------------------------------- display
start_display() {
	check_requirements
	if ! xdpyinfo -display "$DISPLAY" >/dev/null 2>&1; then
		Xvfb "$DISPLAY" -screen 0 ${SCREEN_W}x${SCREEN_H}x24 >/dev/null 2>&1 &
		sleep 2
	fi
	xdpyinfo -display "$DISPLAY" >/dev/null 2>&1 || abort "virtual display $DISPLAY can't be started"
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
	abort "window '$pattern' not found after $timeout s"
}

snapshot() { import -window root "$WORK_DIR/$1.png"; }

# ---------------------------------------------------------------- application
# start_docometre [state] : runs a fresh copy of the release, after restoring
# the given saved state (see save_state) if any
start_docometre() {
	# The copy is run from a neutral folder : its path is displayed by the
	# application (console, Arduino libraries path...). Its content is
	# replaced, so that the folder itself can be owned by a non-root user.
	mkdir -p "$APP_RUN_DIR" 2>/dev/null || abort "can't create $APP_RUN_DIR (run setup-ubuntu.sh)"
	find "$APP_RUN_DIR" -mindepth 1 -delete
	rm -rf "$WORK_DIR/home"
	cp -r "$APP_DIR"/. "$APP_RUN_DIR"/
	# The launcher may lose its execute permission when the release is unzipped
	chmod +x "$APP_RUN_DIR/DOCoMETRe"
	rm -rf "$WORKSPACE"
	mkdir -p "$(dirname "$WORKSPACE")" 2>/dev/null \
		|| abort "can't create $(dirname "$WORKSPACE") (run setup-ubuntu.sh or set WORKSPACE=...)"
	[ -n "$1" ] && restore_state "$1"
	mkdir -p "$WORK_DIR/home"
	(cd "$APP_RUN_DIR" && HOME="$WORK_DIR/home" exec ./DOCoMETRe -nl en) > "$WORK_DIR/docometre.log" 2>&1 &
	DOCOMETRE_PID=$!
}

# Kills DOCoMETRe (on errors : its state is not saved)
stop_docometre() {
	[ -z "$DOCOMETRE_PID" ] && return
	kill $DOCOMETRE_PID 2>/dev/null
	local i
	for i in $(seq 1 20); do
		kill -0 $DOCOMETRE_PID 2>/dev/null || break
		sleep 0.5
	done
	kill -9 $DOCOMETRE_PID 2>/dev/null
	wait $DOCOMETRE_PID 2>/dev/null
	DOCOMETRE_PID=
}

# Quits DOCoMETRe as a user would do (File > Exit DOCoMETRe, filmed) : the
# workspace and the resources properties are saved, the lock file removed
MENU_FILE="18 31"; MENU_EXIT="90 386"
quit_docometre() {
	local i
	click $MENU_FILE
	pause 1
	click $MENU_EXIT
	for i in $(seq 1 120); do
		kill -0 $DOCOMETRE_PID 2>/dev/null || { wait $DOCOMETRE_PID 2>/dev/null; DOCOMETRE_PID=; return 0; }
		sleep 0.5
	done
	abort "DOCoMETRe didn't exit (a dialog may be open)"
}

# ---------------------------------------------------------------- tutorials sequence
# Name of the running tutorial : name of its script
TUTORIAL=$(basename "$0" .sh)

save_state() {
	mkdir -p "$STATE_DIR"
	tar -czf "$STATE_DIR/$1.tar.gz" -C / "${WORKSPACE#/}" "${APP_RUN_DIR#/}/workspaceData.properties" \
		|| abort "state of $1 can't be saved"
}

restore_state() {
	[ -f "$STATE_DIR/$1.tar.gz" ] \
		|| abort "state at the end of tutorial $1 not found in $STATE_DIR : record $1 first (or use record-all.sh)"
	tar -xzf "$STATE_DIR/$1.tar.gz" -C / || abort "state of $1 can't be restored"
}

# First tutorial : empty workspace, the Workspace Launcher is displayed
new_tutorial() {
	start_display
	start_docometre
	wait_window "Workspace Launcher" 60
	pause 2
}

# Next tutorials : DOCoMETRe is opened (before recording) on the workspace
# as it was at the end of the previous tutorial
continue_from() {
	start_display
	start_docometre "$1"
	wait_window "Workspace Launcher" 60
	pause 1
	key ctrl+a
	xdotool type --delay 5 "$WORKSPACE"
	key Return
	wait_window "DOCoMETRe [0-9]" 60
	pause 4
}

# End of the recording : DOCoMETRe is quit with File > Exit (filmed) and its
# state saved for the next tutorial
end_tutorial() {
	quit_docometre
	pause 0.5
	clear_subtitle
	stop_recording
	save_state "$TUTORIAL"
	mkdir -p "$OUT_DIR"
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
	local font=$TITLE_FONT font2=$TITLE_FONT2
	ffmpeg -loglevel error -y -f lavfi -i "color=c=0x1f3b57:s=${SCREEN_W}x${SCREEN_H}:d=4:r=$FPS" \
		-vf "drawtext=fontfile=$font:text='DOCoMETRe':fontcolor=0xffffff:fontsize=44:x=(w-tw)/2:y=h/2-120,\
drawtext=fontfile=$font:text='$title':fontcolor=0xffffff:fontsize=64:x=(w-tw)/2:y=h/2-40,\
drawtext=fontfile=$font2:text='$subtitle':fontcolor=0xc8d6e5:fontsize=34:x=(w-tw)/2:y=h/2+60,\
fade=t=in:st=0:d=0.5,fade=t=out:st=3.5:d=0.5" \
		-c:v libx264 -preset veryfast -crf 18 -pix_fmt yuv420p "$WORK_DIR/title.mp4"
	local duration=$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$RAW_VIDEO")
	local fade_out=$(echo "$duration - 0.6" | bc)
	ffmpeg -loglevel error -y -i "$RAW_VIDEO" -vf "ass=$ass,fade=t=in:st=0:d=0.5,fade=t=out:st=$fade_out:d=0.6" \
		-c:v libx264 -preset medium -crf 20 -pix_fmt yuv420p -r $FPS "$WORK_DIR/body.mp4"
	printf "file '%s'\nfile '%s'\n" "$WORK_DIR/title.mp4" "$WORK_DIR/body.mp4" > "$WORK_DIR/concat.txt"
	ffmpeg -loglevel error -y -f concat -safe 0 -i "$WORK_DIR/concat.txt" -c copy -movflags +faststart "$output"
}
