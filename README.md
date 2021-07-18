# Anemo

[![Anemo CI](https://github.com/2bllw8/anemo/actions/workflows/main.yml/badge.svg)](https://github.com/2bllw8/anemo/actions/workflows/main.yml)
[![Latest release](https://img.shields.io/github/v/release/2bllw8/anemo?color=red&label=download)](https://github.com/2bllw8/anemo/releases/latest)

Anemo is an utility application made of two components: a private local storage, and a text editor.
Instead of being a stand-alone file manager user interface, it hooks into various components of
Android making it feel like a native part of the operative system.
Moreover it provides ways for the user to export contents from other apps and save them as files.
The text editor can read and edit and create text files from the device or other apps.

## Features

- Private file storage
  - Create folders and organize files freely
  - All files in the private storage won't appear in the other apps
  - Access in the system Files application (the _DocumentsProviderUI_)
  - Lock access to the private storage
    - Quick tile
    - Auto lock after 15 minutes
    - Password for locking access to the files
  - Import content using the share Android functionality
    - Audio
    - Images
    - PDF files
    - Videos
- Text editor
  - Open, edit and create text files
  - Cursor and selection information
  - Undo actions
  - Automatically close brackets and quotes: `' '`, `" "`, `( )`, `[ ]`, `{ }`
  - Commands
    - `/my text`: find next occurrence of _my text_ from the current cursor position
    - `d/my text`: delete all occurrences of _my text_
    - `N d/my text`: delete first N occurrences of _my text_ (write the number instead of N)
    - `s/my text/new text`: substitute all occurrences of _my text_ with _new text_
    - `N s/my text/new text`: substitute first N occurrences of _my text_ with _new text_
      (write the number instead of N)
    - `set/key/value`: set config option _key_ to _value_
        - `commands` [`on` | `off`] : Change commands field visibility
        - `pair` [`on` | `off`] : Enable or disable auto–close brackets and quotes
        - `size` [`large` | `medium` | `small`] : Change text size
        - `style` [`mono` | `sans` | `serif`] : Change text style
  - Open selected text from other apps as a new text file and save it on the device
  - Keyboard shortcuts
    - `ctrl` + `N`: Create a new file
    - `ctrl` + `O`: Open a file
    - `ctrl` + `Q`: Quit
    - `ctrl` + `S`: Save
    - `ctrl` + `Z`: Undo
    - `ctrl` + `+`: Increase text size
    - `ctrl` + `-`: Decrease text size
    - `ctrl` + `/`: Show (or hide) command field

## Download

Download the apk from [latest](https://github.com/2bllw8/anemo/releases/latest) release tag.

## Build

With Bazel:
- `bazel build //:app`
- `bazel build //:app_debug`

With AOSP / LineageOS:
- `mka anemo`

## Get help

Open an issue [on github](https://github.com/2bllw8/anemo/issues/)

## License

Anemo is licensed under the [GNU General Public License v3 (GPL-3)](http://www.gnu.org/copyleft/gpl.html).
