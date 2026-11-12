#!/bin/bash
# Video tutorial 2 : DACQ configuration of an Arduino Uno (R4 WiFi).
#
# Usage : 02-arduino-uno-configuration.sh <DOCoMETRe release folder> <output folder>
#
# No board is needed. As DOCoMETRe checks that the arduino-cli tool and the
# serial port exist, placeholders are created when they are missing
# (/usr/local/bin/arduino-cli and /dev/ttyACM0, root rights required) : run
# this script on a machine dedicated to the recordings.

APP_DIR=$(realpath "$1")
OUT_DIR=$(realpath "${2:-.}")
WORK_DIR=${WORK_DIR:-$(mktemp -d)}
WORKSPACE=${WORKSPACE:-/home/researcher/DOCoMETRe-Workspace}
ARDUINO_CLI=${ARDUINO_CLI:-/usr/local/bin/arduino-cli}
SERIAL_PORT=${SERIAL_PORT:-/dev/ttyACM0}
source "$(dirname "$0")/lib.sh"

# Placeholders for the arduino-cli tool and the serial port of the board
if [ ! -e "$ARDUINO_CLI" ]; then
	mkdir -p "$(dirname "$ARDUINO_CLI")"
	printf '#!/bin/sh\necho "arduino-cli placeholder (video recording)"\n' > "$ARDUINO_CLI"
	chmod +x "$ARDUINO_CLI"
fi
[ -e "$SERIAL_PORT" ] || ln -s /dev/null "$SERIAL_PORT"

# Positions in the main window (1600x900 screen)
TB_EXPERIMENT="90 59"; TB_DACQ="117 59"
W_DESC="845 415"; W_FINISH="1038 543"
# DACQ configuration wizard
D_DESC="880 385"; D_SYSTEM="880 461"; D_ARDUINO="720 521"
# Editor tabs (editor maximized)
TAB_GENERAL="67 854"; TAB_CHARTS="120 854"; TAB_VARIABLES="177 854"
TAB_ANIN="256 854"; TAB_DIGIO="474 854"
# General tab
G_CLI_CHECK="67 323"; G_CLI_PATH="800 353"; G_REVISION="900 392"; G_R4="285 452"
G_PORT="800 431"; G_FREQUENCY="257 502"; G_DEFAULT="1537 125"

rm -rf "$WORKSPACE"; mkdir -p "$(dirname "$WORKSPACE")"
start_display
start_docometre
wait_window "Workspace Launcher" 60
pause 2
type_text "$WORKSPACE"
key Return
wait_window "DOCoMETRe [0-9]" 60
pause 4
click 132 91
pause 1
start_recording

say "In this tutorial you will describe an Arduino Uno board in a DACQ (data acquisition) configuration." 5
say "First, create an experiment (see tutorial 1)." 1
move $TB_EXPERIMENT; pause 0.5; xdotool click 1
wait_window "New Resource" 10; pause 0.8
type_text "MotorLearning"
click $W_FINISH
pause 1.5

# ------------------------------------------------------------ wizard
say "Select the experiment and click the 'DACQ Configuration' button." 2
click 70 122
move $TB_DACQ; pause 1.5; xdotool click 1
wait_window "New Resource" 10; pause 1
type_text "ArduinoUno"
click $D_DESC
type_text "Arduino Uno R4 WiFi - force sensor on A1"
say "Choose the system: ADwin or Arduino UNO." 1.5
click $D_SYSTEM
pause 1
click $D_ARDUINO
pause 1
say "As default configuration of the experiment, it will be pre-selected when processes are created." 3.5
click $W_FINISH
pause 2
say "The configuration appears in the Experiments view with its system. Double click to open its editor." 3
double_click 120 145
pause 3
say "Double click on the editor tab to maximize it." 1.5
double_click 510 91
pause 2

# ------------------------------------------------------------ General tab
say "The General tab describes how DOCoMETRe compiles the processes and reaches the board." 4
move 700 214; pause 1
say "Arduino libraries are provided with DOCoMETRe. Check 'Use command line tool' to compile and upload with arduino-cli..." 3
click $G_CLI_CHECK
pause 0.5
say "...and give its path (Browse... opens a file dialog)." 1
click $G_CLI_PATH
type_text "$ARDUINO_CLI"
key Tab
pause 1
say "Choose the revision of your board: R3 or R4 WiFi." 1
click $G_REVISION
pause 1
click $G_R4
pause 1.5
say "The serial port of the board: COM3 on Windows, /dev/ttyACM0 on Linux... Browse... lists the available ports." 2
click $G_PORT
replace_text "$SERIAL_PORT"
key Tab
pause 2
say "The global frequency is the frequency of the real time loop. Click on its value to change it." 3
click $G_FREQUENCY
pause 1
type_text "1000"
pause 1
key Return
pause 1.5
say "Each module of the board has its own tab: analog inputs, analog outputs and digital inputs/outputs." 4
move 120 652; pause 1; move 400 854; pause 2.5

# ------------------------------------------------------------ Analog inputs
say "Analog inputs tab: first choose the analog reference. AR_DEFAULT (5 V) is the default one of the R4 WiFi." 2
click $TAB_ANIN
pause 1.5
click 860 360
pause 1
click 240 478
pause 2
say "Then check the channels you use. Here a force sensor is wired on pin A1." 2
click 61 680
pause 1
say "T (Transfer): values are sent to the computer, to be displayed during acquisition." 1.5
click 371 680
pause 1.5
say "Rec. (Record): transferred values are saved in a samples file for each trial." 1.5
click 511 680
pause 1.5
say "Give the channel a meaningful name: it is used in processes, charts and data files names." 2
click 160 680
pause 0.5
replace_text "Force"
key Return
pause 1.5
say "S.F. is the sample frequency of the channel, a sub-multiple of the global frequency: 500 Hz here." 2
click 255 772
pause 0.5
click 287 772
pause 1.2
click 246 490
key Return
pause 2.5

# ------------------------------------------------------------ Digital inputs/outputs
say "Digital inputs/outputs tab: pin 13 (the LED of the board) is used as a digital output." 2
click $TAB_DIGIO
pause 1.5
click 61 528
pause 0.5
click 600 528
pause 0.5
click 1433 528
pause 1
click 610 574
key Return
pause 2.5

# ------------------------------------------------------------ Variables
say "Variables tab: variables are used by the processes (counters, states, computed values...)." 2.5
click $TAB_VARIABLES
pause 1
click 1532 195
pause 1
say "Add a variable, give it a name and a type." 1
click 95 284
pause 0.5
replace_text "targetForce"
key Return
pause 0.5
click 455 284
pause 0.5
click 496 284
pause 1
click 455 400
key Return
pause 1
say "Checked as Parameter, its value will be read for each trial in the parameters file of the session." 2
click 581 284
pause 3

# ------------------------------------------------------------ Charts
say "Charts tab: the charts displayed in real time during acquisition." 2
click $TAB_CHARTS
pause 1
click 1423 167
wait_window "New Chart Dialog" 10; pause 1.5
say "Several types are available: oscilloscope, XY, gauge, tank, image... Here an oscilloscope chart." 2
click 1038 520
pause 1.5
say "Add the curves to display: only transferred channels and variables are listed." 1.5
click 1532 520
wait_window "Add Curve" 10; pause 1.5
click 600 351
pause 0.8
click 1038 520
pause 1.5
say "Set the properties of the chart: a 5 seconds time width, with automatic scaling." 1
click 520 226
replace_text "5"
key Return
pause 0.5
click 372 250
pause 2.5

# ------------------------------------------------------------ save
say "Save the configuration with Ctrl+S." 1.5
key ctrl+s
pause 1.5
click $TAB_GENERAL
pause 1
say "This button saves these settings in the preferences: they will initialize your next Arduino configurations." 2
move $G_DEFAULT
pause 4
double_click 150 91
pause 2
click 120 145
say "The DACQ configuration is ready. Next tutorial: create a process that uses these channels and variables." 5
clear_subtitle
pause 1

stop_recording
stop_docometre
mkdir -p "$OUT_DIR"
make_video "$OUT_DIR/02-Arduino-Uno-Configuration.mp4" "Arduino Uno configuration" "Board, modules, channels, variables and charts"
echo "Video : $OUT_DIR/02-Arduino-Uno-Configuration.mp4"
