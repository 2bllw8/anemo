# Copyright (c) 2021 2bllw8
# SPDX-License-Identifier: GPL-3.0-only
android_library(
    name = "editor",
    manifest = "AndroidManifest.xml",
    srcs = glob([
        "**/*.java",
    ]),
    resource_files = glob([
        "res/**",
    ]),
    visibility = [
        "//:__pkg__"
    ],
    deps = [
        "//java/eu/bbllw8/anemo/shell",
        "//java/eu/bbllw8/anemo/task",
        "//java/eu/bbllw8/anemo/tip",
        "@maven//:androidx_annotation_annotation",
    ],
)
