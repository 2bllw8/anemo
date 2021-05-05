# Copyright (c) 2021 2bllw8
# SPDX-License-Identifier: GPL-3.0-only
load("@rules_android//android:rules.bzl", "android_library")

android_library(
    name = "home",
    manifest = "AndroidManifest.xml",
    srcs = glob([
        "**/*.java",
    ]),
    visibility = [
        "//java/eu/bbllw8/anemo/documents:__pkg__",
        "//java/eu/bbllw8/anemo/receiver:__pkg__",
        "//java/eu/bbllw8/anemo/snippet:__pkg__",
    ],
    deps = [
        "@maven//:androidx_annotation_annotation",
    ],
)
