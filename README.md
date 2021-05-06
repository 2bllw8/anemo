# Anemo - Private local storage

[![Bazel Android](https://github.com/2bllw8/anemo/actions/workflows/bazel.yml/badge.svg)](https://github.com/2bllw8/anemo/actions/workflows/bazel.yml)

Anemo is a private local storage application that instead of providing a stand-alone UI it hooks
into various components of the Android operative system making it feel as native as possible.
Moreover it provides ways for the user to save content from other apps as files.

## Features

- [x] File storage available in the default Files application (the DocumentsProviderUI)
- [x] Lock access to the storage using a quick tile
- [x] Saving text snippets from selected text as "Snippet" files
- [x] Saving audio, images and videos to the Anemo storage from the share Android functionality

### To do

Some of the planned features

- [ ] Support saving pdf files from the share Android functionality too
- [ ] Make a nice UI for importing snippets and files
- [ ] Password for unlocking access to the files
