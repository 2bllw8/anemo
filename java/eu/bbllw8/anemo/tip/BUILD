# Copyright (c) 2021 2bllw8
# SPDX-License-Identifier: GPL-3.0-only
android_library(
    name = "tip",
    manifest = "AndroidManifest.xml",
    srcs = glob([
        "**/*.java",
    ]),
    resource_files = glob([
        "res/**",
    ]),
    visibility = [
        "//java/eu/bbllw8/anemo/editor/main:__pkg__",
        "//java/eu/bbllw8/anemo/documents/receiver:__pkg__",
        "//java/eu/bbllw8/anemo/documents/snippet:__pkg__",
    ],
    deps = [
        "@maven//:androidx_annotation_annotation",
    ],
)
