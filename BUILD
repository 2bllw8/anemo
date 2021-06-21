# Copyright (c) 2021 2bllw8
# SPDX-License-Identifier: GPL-3.0-only
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
        "//java/eu/bbllw8/anemo/editor/main",
        "//java/eu/bbllw8/anemo/documents/password",
        "//java/eu/bbllw8/anemo/documents/provider",
        "//java/eu/bbllw8/anemo/documents/receiver",
        "//java/eu/bbllw8/anemo/documents/snippet",
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
        "//java/eu/bbllw8/anemo/editor/main",
        "//java/eu/bbllw8/anemo/documents/password",
        "//java/eu/bbllw8/anemo/documents/provider",
        "//java/eu/bbllw8/anemo/documents/receiver",
        "//java/eu/bbllw8/anemo/documents/snippet",
    ],
)
