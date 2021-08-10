# Copyright (c) 2021 2bllw8
# SPDX-License-Identifier: GPL-3.0-only
android_library(
    name = "help",
    srcs = glob([
        "**/*.java",
    ]),
    manifest = "AndroidManifest.xml",
    resource_files = glob([
        "res/**",
    ]),
    visibility = [
        "//:__pkg__",
    ],
    deps = [
        "//java/eu/bbllw8/anemo/editor/markdown",
        "//java/eu/bbllw8/anemo/shell",
        "//java/eu/bbllw8/anemo/task",
        "@maven//:androidx_annotation_annotation",
    ],
)
