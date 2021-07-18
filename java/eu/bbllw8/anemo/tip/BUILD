# Copyright (c) 2021 2bllw8
# SPDX-License-Identifier: GPL-3.0-only
android_library(
    name = "tip",
    srcs = glob([
        "**/*.java",
    ]),
    manifest = "AndroidManifest.xml",
    resource_files = glob([
        "res/**",
    ]),
    visibility = [
        "//java/eu/bbllw8/anemo/documents/receiver:__pkg__",
        "//java/eu/bbllw8/anemo/documents/snippet:__pkg__",
        "//java/eu/bbllw8/anemo/editor/main:__pkg__",
    ],
    deps = [
        "@maven//:androidx_annotation_annotation",
    ],
)
