#!/bin/bash
# Video tutorial 1 : create an experiment, its subjects and their sessions.
#
# Usage : 01-experiment-subjects-sessions.sh <DOCoMETRe release folder> <output folder>

APP_DIR=$(realpath "$1")
OUT_DIR=$(realpath "${2:-.}")
WORK_DIR=${WORK_DIR:-$(mktemp -d)}
source "$(dirname "$0")/lib.sh"

# Positions in the main window (1600x900 screen)
TB_EXPERIMENT="90 59"; TB_SUBJECT="171 59"; TB_SESSION="198 59"
row() { echo "$((70 + 18 * $1)) $((122 + 23 * $2))"; }   # tree item : depth, row
arrow() { echo "$((16 + 18 * $1)) $((122 + 23 * $2))"; } # expand/collapse arrow
# Small wizards (experiment, subject)
W_NAME="845 310"; W_DESC="845 415"; W_FINISH="1038 543"
# Session wizard
S_NAME="868 179"; S_DESC="868 300"; S_PREFIX_CHECK="410 437"; S_PREFIX="868 437"
S_TRIALS="840 547"; S_NEXT="915 608"; S_FINISH="1148 608"

new_tutorial
start_recording

say "In this tutorial you will create an experiment, add subjects (participants) and organize their sessions and trials." 6
say "When DOCoMETRe starts, choose a workspace: the folder where all your experiments are stored." 3
type_text "$WORKSPACE"
pause 1
key Return
wait_window "DOCoMETRe [0-9]" 60
pause 4
say "Close the Welcome page to reach the Design perspective." 2.5
click 132 91
pause 1.5
say "The Experiments view (left) shows the experiments of the workspace as a tree." 3
move 200 380; pause 1.5
say "The Description view shows the description of the selected item, and the Console view displays messages." 3
move 200 790; pause 1.2; move 1000 780; pause 2

# ------------------------------------------------------------ experiment
say "Step 1 - Create an experiment with the 'Experiment' button of the tool bar." 2.5
move $TB_EXPERIMENT; pause 1.5; xdotool click 1
wait_window "New Resource" 10; pause 1
say "Give it a name and a short description." 1
type_text "MotorLearning"
click $W_DESC
type_text "Reaching task with visual feedback - pilot study"
pause 1
say "Click Finish: the experiment appears in the Experiments view." 2
click $W_FINISH
pause 2
click $(row 0 0)
say "Its description is displayed in the Description view." 3

# ------------------------------------------------------------ subjects
say "Step 2 - Select the experiment and click the 'Subject' button to add a participant." 2.5
move $TB_SUBJECT; pause 1.5; xdotool click 1
wait_window "New Resource" 10; pause 1
type_text "S01"
click $W_DESC
type_text "Participant 1 - right-handed, 24 years old"
pause 1
click $W_FINISH
pause 1.5
say "Do the same for each participant of the study." 1
click $(row 0 0)
move $TB_SUBJECT; pause 0.5; xdotool click 1
wait_window "New Resource" 10; pause 0.8
type_text "S02"
click $W_DESC
type_text "Participant 2 - left-handed, 31 years old"
click $W_FINISH
pause 2
say "Subjects are folders of the experiment. The percentage of trials already done will be displayed next to their name." 4

# ------------------------------------------------------------ sessions
say "Step 3 - Select a subject and click the 'Session' button. A session is a set of trials run in sequence." 2
click $(row 1 1)
move $TB_SESSION; pause 1.5; xdotool click 1
wait_window "New Resource" 10; pause 1
type_text "Familiarization"
click $S_DESC
type_text "Short session to get used to the task"
pause 0.5
say "Choose the number of trials of the session." 1
click $S_TRIALS
replace_text "10"
key Tab
pause 1
say "Data files names are built from the channel name, plus the session name and the trial number as suffixes..." 3.5
move 600 513; pause 1
say "...and, optionally, a prefix: here the subject code, so that files keep unique names outside DOCoMETRe." 2
click $S_PREFIX_CHECK
click $S_PREFIX
replace_text "S01"
key Tab
move 600 513
pause 3
say "Click Next to organize the session." 1.5
click $S_NEXT
pause 1.5
say "This page associates a data acquisition process with each trial. The experiment has no process yet: this will be done in another tutorial." 6
say "Click Finish to create the session and its trials." 1.5
click $S_FINISH
pause 2
# The subject is expanded and the new session selected
click $(arrow 2 2)
pause 1
say "The session contains 10 trials. [S.Man.] and [V.Man.] mean that each trial is started and validated manually." 5
move $(row 3 3); pause 1; move $(row 3 8); pause 2
say "Their icon will show whether they have been done or not." 2.5
click $(arrow 2 2)
pause 1

# Second session, created directly from the first page
say "Add a second session to S01 : 'Learning', with 40 trials." 1.5
click $(row 1 1)
move $TB_SESSION; pause 0.5; xdotool click 1
wait_window "New Resource" 10; pause 0.8
type_text "Learning"
click $S_DESC
type_text "Learning session with perturbed visual feedback"
click $S_TRIALS
replace_text "40"
key Tab
click $S_PREFIX_CHECK
click $S_PREFIX
replace_text "S01"
key Tab
pause 1.5
say "When processes are not needed yet, Finish can be clicked from the first page." 2
click $S_FINISH
pause 2
click $(row 2 3)
say "The selected session and its description: everything can be edited later (Edit session in the context menu)." 3
right_click $(row 2 3)
pause 1
for i in 1 2 3 4 5 6; do key Down; pause 0.15; done
pause 2.5
key Escape
pause 1

# ------------------------------------------------------------ end
say "Your experimental protocol is ready: subjects, sessions and trials. Next step: describe the data acquisition system and its processes." 6
say "Quit DOCoMETRe with File > Exit DOCoMETRe: the workspace is saved and will be opened again next time." 2
end_tutorial
make_video "$OUT_DIR/01-Experiment-Subjects-Sessions.mp4" "Create an experiment" "Subjects, sessions and trials"
echo "Video : $OUT_DIR/01-Experiment-Subjects-Sessions.mp4"
