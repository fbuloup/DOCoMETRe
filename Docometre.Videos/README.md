# DOCoMETRe video tutorials

Scripts that record DOCoMETRe video tutorials automatically: the application
runs on a virtual display (Xvfb), is driven with `xdotool`, recorded with
`ffmpeg`, and English subtitles are burnt into the video (a `.srt` file is
also produced, e.g. for YouTube or translations).

| Script | Use case |
| --- | --- |
| `01-experiment-subjects-sessions.sh` | Create an experiment, its subjects, their sessions and trials |

## Requirements (Linux)

`Xvfb`, `openbox`, `xdotool`, `ffmpeg` (with libass), ImageMagick, `bc`,
`python3`, the Inter font, Java 21 and the
[Linux release of DOCoMETRe](https://github.com/fbuloup/docometre-release/releases/download/docometre/DocometreLinux.zip)
unzipped in a folder.

## Usage

```bash
./01-experiment-subjects-sessions.sh /path/to/DocometreLinux /path/to/output
```

The release folder is copied before each recording (fresh preferences, no
remembered workspace). The workspace used in the video is
`/home/researcher/DOCoMETRe-Workspace` (override with `WORKSPACE=...`); it is
deleted before recording.

Mouse positions are given for a 1600x900 screen and the default layout of the
Design perspective: they must be checked if the GUI changes.
