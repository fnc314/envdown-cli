package com.fnc314.tools.envdown

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv

@OptIn(ExperimentalForeignApi::class)
public actual fun getEnv(key: String): String? = getenv(key)?.toKString()
