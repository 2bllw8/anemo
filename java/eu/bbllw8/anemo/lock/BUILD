# Copyright (c) 2021 2bllw8
# SPDX-License-Identifier: GPL-3.0-only
load("@rules_android//android:rules.bzl", "android_library")

android_library(
    name = "lock",
    manifest = "AndroidManifest.xml",
    srcs = glob([
        "**/*.java",
    ]),
    resource_files = glob([
        "res/**"
    ]),
    visibility = [
        "//java/eu/bbllw8/anemo/documents:__pkg__",
    ],
    deps = [
        "@maven//:androidx_annotation_annotation",
    ],
)
