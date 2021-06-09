# Anemo

[![Bazel Android](https://github.com/2bllw8/anemo/actions/workflows/bazel.yml/badge.svg)](https://github.com/2bllw8/anemo/actions/workflows/bazel.yml)

Anemo is a file utility application that provides private local storage, but instead of being a
stand-alone file manager user interface, it hooks into various components of Android making
it feel like a native part of the operative system.
It also includes a simple text editor for editing text files on the device.
Moreover it provides ways for the user to export contents from other apps and save them files.

## Features

- Private file storage available in the default Files application (the _DocumentsProviderUI_)
- Simple text editor to open edit and save text files
  - Cursor and selection information
  - Undo actions
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
- Replace snippets UI with new editor interface

## Get

### Download

Download the apk from [latest](https://github.com/2bllw8/anemo/releases/latest) release tag.

### Build

With Bazel:
- `bazel build //:app`
- `bazel build //:app_debug`

With AOSP / LineageOS:
- `mka anemo`
