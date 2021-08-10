# Copyright (c) 2021 2bllw8
# SPDX-License-Identifier: GPL-3.0-only
android_binary(
    name = "app",
    custom_package = "eu.bbllw8.anemo",
    manifest = "AppManifest.xml",
    proguard_generate_mapping = True,
    proguard_specs = [
        "proguard-rules.pro",
    ],
    shrink_resources = 1,
    visibility = [
        "//visibility:public",
    ],
    deps = [
        "//java/eu/bbllw8/anemo/documents/password",
        "//java/eu/bbllw8/anemo/documents/provider",
        "//java/eu/bbllw8/anemo/documents/receiver",
        "//java/eu/bbllw8/anemo/editor/help",
        "//java/eu/bbllw8/anemo/editor/main",
    ],
)

android_binary(
    name = "app_debug",
    custom_package = "eu.bbllw8.anemo",
    manifest = "AppManifest.xml",
    visibility = [
        "//visibility:public",
    ],
    deps = [
        "//java/eu/bbllw8/anemo/documents/password",
        "//java/eu/bbllw8/anemo/documents/provider",
        "//java/eu/bbllw8/anemo/documents/receiver",
        "//java/eu/bbllw8/anemo/editor/help",
        "//java/eu/bbllw8/anemo/editor/main",
    ],
)
