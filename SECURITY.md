# Security Policy

## Supported Versions

Use this section to tell people about which versions of your project are
currently being supported with security updates.

| Version | Supported          |
|---------|--------------------|
| 2.7.x   | :white_check_mark: |
| < 2.7.0 | :x:                |

## Threat model

Anemo stores files in its private data directory, which is
[protected and encrypted by the OS](https://source.android.com/docs/security/features/encryption/file-based),
assuming that you're running a [CDD](https://source.android.com/docs/compatibility/cdd)-compliant
Android distribution.
Highly privileged processes running on your device could access the private directory where the
files are stored (such processes include core OS services, `adb root`'ed shells and third party
processes running with root capabilities).

It is possible to setup a password that can be used to enable and disable access to the private
storage itself, the password is stored locally in a private directory protected by the same
mechanisms as the rest of the data, hashed using the SHA-256 algorithm, as provided by the OS.

## Reporting a Vulnerability

Please report vulnerabilities either
on [GitHub issues](https://github.com/2bllw8/anemo/issues/new?assignees=&labels=bug&template=bug_report.yml&title=%5BBug%5D+)
or by emailing the main maintainer (find the email address in git commits).

Any vulnerability that involves the breakage of one of the security models defined by the Android
CDD will not be addressed, as it is outside of the scope of this project.
(E.g., files being accessible from an adb shell using adbd with root privileges).
Anemo is designed to  operate under the assumption that the OS itself is able to fulfill the
promises made in its APIs and  its specifications.
