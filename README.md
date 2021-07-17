# Anemo

[![Anemo CI](https://github.com/2bllw8/anemo/actions/workflows/main.yml/badge.svg)](https://github.com/2bllw8/anemo/actions/workflows/main.yml)
[![Latest release](https://img.shields.io/github/v/release/2bllw8/anemo?color=red&label=download)](https://github.com/2bllw8/anemo/releases/latest)

Anemo is a file utility application that provides private local storage, but instead of being a
stand-alone file manager user interface, it hooks into various components of Android making
it feel like a native part of the operative system.
It also includes a simple text editor for editing text files on the device.
Moreover it provides ways for the user to export contents from other apps and save them as files.

## Features

- Private file storage available in the default Files application (the _DocumentsProviderUI_)
- Text editor
  - Open, edit and create text files
  - Cursor and selection information
  - Undo actions
  - Automatically close brackets and quotes: `'`, `"`, `(`, `[`, `{`
  - Commands
    - `/my text`: find next occurrence of _my text_ from the current cursor position
    - `d/my text`: delete all occurrences of _my text_
    - `N d/my text`: delete first N occurrences of _my text_ (write the number instead of N)
    - `s/my text/new text`: substitute all occurrences of _my text_ with _new text_
    - `N s/my text/new text`: substitute first N occurrences of _my text_ with _new text_
      (write the number instead of N)
    - `set/key/value`: set config option `key` to `value`
        - `commands` [`on` | `off`] : Change commands field visibility
        - `pair` [`on` | `off`] : Enable or disable auto–close brackets and quotes
        - `size` [`large` | `medium` | `small`] : Change text size
        - `style` [`mono` | `sans` | `serif`] : Change text style
  - Open selected text from other apps as a new text file and save it on the device
- Lock access to the private storage using a quick tile
  - Auto lock after 15 minutes
  - Password for locking access to the files
- Import from the share Android functionality
  - Audio
  - Images
  - PDF files
  - Videos

### Planned features

- Improvements and more features to the text editor

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
