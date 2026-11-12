# DOCoMETRe video tutorials

Scripts that record DOCoMETRe video tutorials automatically: the application
runs on a virtual display (Xvfb), is driven with `xdotool`, recorded with
`ffmpeg`, and English subtitles are burnt into the video (a `.srt` file is
also produced, e.g. for YouTube or translations).

| Script | Use case |
| --- | --- |
| `01-experiment-subjects-sessions.sh` | Create an experiment, its subjects, their sessions and trials |
| `02-arduino-uno-configuration.sh` | DACQ configuration of an Arduino Uno R4 WiFi : general parameters, analog input, digital output, variable (parameter), real time chart |
| `record-all.sh` | Records all the tutorials, in sequence |

Recorded videos and their subtitles are in the `output` folder.

## Requirements

Ubuntu 24.04, native or under WSL2 on Windows (see below). `setup-ubuntu.sh`
installs everything : `Xvfb`, `openbox`, `xdotool`, `ffmpeg` (with libass),
ImageMagick, `bc`, `python3`, the Inter font, Java 21, `arduino-cli` with the
Uno R3 and R4 WiFi platforms, the
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

The tutorials follow each other : tutorial 1 starts from an empty workspace,
and each next tutorial starts from the workspace as it was at the end of the
previous one (each tutorial ends by quitting DOCoMETRe with File > Exit
DOCoMETRe). Record them all, in order :

```bash
./record-all.sh /path/to/DocometreLinux /path/to/output
```

or one by one :

```bash
./01-experiment-subjects-sessions.sh /path/to/DocometreLinux /path/to/output
./02-arduino-uno-configuration.sh /path/to/DocometreLinux /path/to/output
```

The state of DOCoMETRe at the end of each tutorial (workspace and remembered
workspace) is saved in `~/.docometre-videos` (override with `STATE_DIR=...`).
A tutorial can thus be recorded again alone, without recording the previous
ones again : e.g. `./02-arduino-uno-configuration.sh ...` restores the state
saved at the end of tutorial 1. To record again from tutorial 2 to the last
one : `./record-all.sh /path/to/DocometreLinux /path/to/output 2`.
If a tutorial changes, the next ones must be recorded again.

The release folder is copied before each recording (no preferences except
those of the restored workspace). The workspace used in the videos is
`/home/researcher/DOCoMETRe-Workspace` (override with `WORKSPACE=...`).

The release folder is run from `/opt/DOCoMETRe` (override with
`APP_RUN_DIR=...`), as its path is displayed by the application.

Script 2 needs no board. `setup-ubuntu.sh` installs `arduino-cli` (fixed
version, checksum verified) in `/usr/local/bin`, the Uno R3 (`arduino:avr`)
and Uno R4 WiFi (`arduino:renesas_uno`) platforms and the `ADS1X15` library
(in `~/.arduino15` and `~/Arduino`, about 1 GB) : processes can be compiled
as with a real installation. As DOCoMETRe checks that the serial port exists,
a placeholder is created when it is missing (`/dev/ttyACM0`, with `sudo` when
the script is not run as root) : uploading to a board is not possible.

If a window doesn't appear (e.g. the GUI changed), the script stops, saves a
screenshot (`error.png`) and gives the path of the DOCoMETRe log.
A recording can be interrupted with Ctrl+C : DOCoMETRe and ffmpeg are stopped.
A DOCoMETRe left open on the virtual display (e.g. by a killed terminal) is
closed before the next recording.

Mouse positions are given for a 1600x900 screen and the default layout of the
Design perspective: they must be checked if the GUI changes.
