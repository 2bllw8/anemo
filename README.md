# Anemo

[![Anemo CI](https://github.com/2bllw8/anemo/actions/workflows/main.yml/badge.svg)](https://github.com/2bllw8/anemo/actions/workflows/main.yml)
[![Latest release](https://img.shields.io/github/v/release/2bllw8/anemo?color=red&label=download)](https://github.com/2bllw8/anemo/releases/latest)

Anemo is a private local storage utility application for android.
Instead of being a stand-alone file manager user interface, it hooks into various components of
Android making it feel like a native part of the operative system.
Moreover it provides ways for the user to export contents from other apps and save them as files.

## Features

- Create folders and organize files freely
- All files in the private storage won't appear in the other apps
- Access in the system Files application (the _DocumentsProviderUI_)
    - An optional shortcut for devices that do not expose the system Files app is offered
- Lock access to the private storage
  - Quick tile
  - Auto lock after 15 minutes
  - Password for locking access to the files
- Import content using the share Android functionality

## Threat model

Anemo stores files in its private data directory (`/data/data/exe.bbllw8.anemo`) and can be
configured to require a password before you can view these files. It does not encrypt the files,
instead relying on your device's [native storage encryption](https://source.android.com/docs/security/features/encryption/file-based)
to keep files safe from someone who can access your internal storage chip. You can check whether
your device is encrypted under Settings &rarr; Security &rarr; More/Other. This encryption is
only effective if you have an authentication method on your lock screen such as a PIN, password,
fingerprint, etc.

Besides hardware attacks, only a few system processes can access the private data directory
unless you granted something like root access to an app or process. In that case, anyone
who can get root in some way (such as with `adb shell su`) can also access the files.

The password that you configure is hashed using SHA-256 (in [LockStore.java](https://github.com/2bllw8/anemo/blob/main/app/src/main/java/exe/bbllw8/anemo/lock/LockStore.java#L184)).

## Download

Get the apk from [latest](https://github.com/2bllw8/anemo/releases/latest) release tag or
from F-Droid.

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="60">](https://f-droid.org/packages/exe.bbllw8.anemo/)

## Build

With Gradle:
- `./gradlew :app:assembleRelease`
- `./gradlew :app:assembleDebug`

With AOSP / LineageOS:
- `mka anemo`

## Get help

Open an discussion [on github](https://github.com/2bllw8/anemo/discussions/new?category=Q-A)

## Contributions

**Every contributions, including ideas, feature requests, bug reports and Pull Requests are welcome!**

- **Solve bug(s)**, add **feature(s)** or **translate** Anemo to your native language by making a **[pull request](https://help.github.com/articles/about-pull-requests/)**
- You have an idea for improvement or a new feature? Open a **[feature request](https://github.com/2bllw8/anemo/issues/new?assignees=&labels=enhancement&template=feature_request.yml&title=[Feature+request]+)**
- You faced a bug, let us know by opening a **[bug report](https://github.com/2bllw8/anemo/issues/new?assignees=&labels=bug&template=bug_report.yml&title=%5BBug%5D+)**

## License

Anemo is licensed under the [GNU General Public License v3 (GPL-3)](http://www.gnu.org/copyleft/gpl.html).
