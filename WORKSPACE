# Copyright (c) 2021 2bllw8
# SPDX-License-Identifier: GPL-3.0-only
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

# Android
ANDROID_API_LEVEL = 30

ANDROID_BUILD_TOOLS = "30.0.3"

# Jvm External
RULES_JVM_EXTERNAL_TAG = "4.1"

RULES_JVM_EXTERNAL_SHA = "f36441aa876c4f6427bfb2d1f2d723b48e9d930b62662bf723ddfb8fc80f0140"

# Maven
ANNOTATION_VERSION = "1.1.0"

android_sdk_repository(
    name = "androidsdk",
    api_level = ANDROID_API_LEVEL,
    build_tools_version = ANDROID_BUILD_TOOLS,
)

http_archive(
    name = "rules_jvm_external",
    sha256 = RULES_JVM_EXTERNAL_SHA,
    strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/%s.zip" % RULES_JVM_EXTERNAL_TAG,
)

load("@rules_jvm_external//:defs.bzl", "maven_install")

maven_install(
    artifacts = [
        "androidx.annotation:annotation:%s" % ANNOTATION_VERSION,
    ],
    fetch_sources = True,
    repositories = [
        "https://maven.google.com",
    ],
)
