# Copyright (c) 2021 2bllw8
# SPDX-License-Identifier: GPL-3.0-only
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

# Android
ANDROID_API_LEVEL = 30
ANDROID_BUILD_TOOLS = "30.0.3"
# Rules
RULES_JVM_EXTERNAL_TAG = "4.0"
RULES_JVM_EXTERNAL_SHA = "31701ad93dbfe544d597dbe62c9a1fdd76d81d8a9150c2bf1ecf928ecdf97169"
# Maven
ANNOTATION_VERSION = "1.1.0"
RECYLERVIEW_VERSION = "1.2.1"

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
        "androidx.recyclerview:recyclerview:%s" % RECYLERVIEW_VERSION,
    ],
    fetch_sources = True,
    repositories = [
        "https://maven.google.com",
    ],
)
