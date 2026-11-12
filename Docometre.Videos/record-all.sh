#!/bin/bash
# Records the tutorials in sequence : each one starts from the workspace as it
# was at the end of the previous one.
#
# Usage : record-all.sh <DOCoMETRe release folder> <output folder> [first]
#
# [first] : number of the first tutorial to record (e.g. 2), the previous ones
# having already been recorded (their final state is saved in
# ~/.docometre-videos).

DIR=$(dirname "$(realpath "$0")")
FIRST=${3:-1}

for script in "$DIR"/[0-9][0-9]-*.sh; do
	name=$(basename "$script")
	[ $((10#${name:0:2})) -lt "$FIRST" ] && continue
	echo "== $name"
	"$script" "$1" "$2" || { echo "Stopped : $name failed" >&2; exit 1; }
done
