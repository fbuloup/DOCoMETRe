# DOCoMETRe video tutorials

Scripts that record DOCoMETRe video tutorials automatically: the application
runs on a virtual display (Xvfb), is driven with `xdotool`, recorded with
`ffmpeg`, and English subtitles are burnt into the video (a `.srt` file is
also produced, e.g. for YouTube or translations).

| Script | Use case |
| --- | --- |
| `01-experiment-subjects-sessions.sh` | Create an experiment, its subjects, their sessions and trials |
| `02-arduino-uno-configuration.sh` | DACQ configuration of an Arduino Uno R4 WiFi : general parameters, analog input, digital output, variable (parameter), real time chart |

Recorded videos and their subtitles are in the `output` folder.

## Requirements

Ubuntu 24.04, native or under WSL2 on Windows (see below). `setup-ubuntu.sh`
installs everything : `Xvfb`, `openbox`, `xdotool`, `ffmpeg` (with libass),
ImageMagick, `bc`, `python3`, the Inter font, Java 21, the
[Linux release of DOCoMETRe](https://github.com/fbuloup/docometre-release/releases/download/docometre/DocometreLinux.zip)
(unzipped in `~/DocometreLinux`) and creates, owned by the current user,
`/home/researcher` (folder of the workspace shown in the videos) and
`/opt/DOCoMETRe` (folder the application is run from).

```bash
./setup-ubuntu.sh
```

### Windows (WSL2)

The scripts run unchanged in WSL2 : the application is recorded on a virtual
display, so the Windows desktop stays usable during recording. The videos
show the Linux (GTK) look of DOCoMETRe.

1. In PowerShell (administrator) : `wsl --install -d Ubuntu-24.04`, then
   reboot and create the Linux user when Ubuntu starts.
2. In Ubuntu, clone the repository in the Linux file system (not in
   `/mnt/c/...` : slower, and scripts must keep Unix line endings) :
   `git clone https://github.com/fbuloup/DOCoMETRe.git ~/DOCoMETRe`
3. `~/DOCoMETRe/Docometre.Videos/setup-ubuntu.sh`
4. Record, writing the video directly in the Windows folder :
   `~/DOCoMETRe/Docometre.Videos/01-experiment-subjects-sessions.sh ~/DocometreLinux /mnt/c/Users/<user>/Videos`

## Usage

```bash
./01-experiment-subjects-sessions.sh /path/to/DocometreLinux /path/to/output
```

The release folder is copied before each recording (fresh preferences, no
remembered workspace). The workspace used in the video is
`/home/researcher/DOCoMETRe-Workspace` (override with `WORKSPACE=...`); it is
deleted before recording.

The release folder is run from `/opt/DOCoMETRe` (override with
`APP_RUN_DIR=...`), as its path is displayed by the application.

Script 2 needs no board : as DOCoMETRe checks that the `arduino-cli` tool and
the serial port exist, placeholders are created when they are missing
(`/usr/local/bin/arduino-cli` and `/dev/ttyACM0`, with `sudo` when the script
is not run as root).

If a window doesn't appear (e.g. the GUI changed), the script stops, saves a
screenshot (`error.png`) and gives the path of the DOCoMETRe log.

Mouse positions are given for a 1600x900 screen and the default layout of the
Design perspective: they must be checked if the GUI changes.
