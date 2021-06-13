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
- Simple text editor to open edit and save text files
  - Cursor and selection information
  - Undo actions
  - Commands
    - `/my text`: find next occurrence of _my text_ from the current cursor position
    - `d/my text/`: delete all occurrences of _my text_
    - `s/my text/new text/`: substitute all occurrences of _my text_ with _new text_
    - `N s/my text/new text/`: substitute first N occurrence of _my text_ with _new text_
      (write the number instead of N)
- Lock access to the private storage using a quick tile
  - Auto lock after 15 minutes
  - Password for locking access to the files
- Saving text snippets from selected text as "Snippet" files
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
