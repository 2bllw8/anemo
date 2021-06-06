# Copyright (c) 2021 2bllw8
# SPDX-License-Identifier: GPL-3.0-only
load("@rules_android//android:rules.bzl", "android_binary")

android_binary(
    name = "app",
    manifest = "AppManifest.xml",
    custom_package = "eu.bbllw8.anemo",
    shrink_resources = 1,
    proguard_generate_mapping = True,
    proguard_specs = [
        "proguard-rules.pro",
    ],
    visibility = [
        "//visibility:public",
    ],
    deps = [
        "//java/eu/bbllw8/anemo/documents",
        "//java/eu/bbllw8/anemo/editor",
        "//java/eu/bbllw8/anemo/password",
        "//java/eu/bbllw8/anemo/receiver",
        "//java/eu/bbllw8/anemo/snippet",
    ],
)

android_binary(
    name = "app_debug",
    manifest = "AppManifest.xml",
    custom_package = "eu.bbllw8.anemo",
    visibility = [
        "//visibility:public",
    ],
    deps = [
        "//java/eu/bbllw8/anemo/documents",
        "//java/eu/bbllw8/anemo/editor",
        "//java/eu/bbllw8/anemo/password",
        "//java/eu/bbllw8/anemo/receiver",
        "//java/eu/bbllw8/anemo/snippet",
    ],
)
