package com.fnc314.tools.envdown

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.pin
import platform.windows.GetEnvironmentVariableW

@OptIn(ExperimentalForeignApi::class)
public actual fun getEnv(key: String): String? {
    // Get required buffer size
    val size = GetEnvironmentVariableW(key, null, 0U)
    if (size == 0U) return null

    // Create buffer and pin it
    val buffer = UShortArray(size.toInt())
    val pinned = buffer.pin()
    val bufferPtr = pinned.addressOf(0)
    val result = GetEnvironmentVariableW(key, bufferPtr, size)
    pinned.unpin()
    if (result == 0U) return null

    // Convert UShortArray to String
    var endIdx = 0
    while (endIdx < buffer.size && buffer[endIdx] != 0.toUShort()) {
        endIdx++
    }
    val sb = StringBuilder(endIdx)
    for (i in 0 until endIdx) {
        sb.append(buffer[i].toInt().toChar())
    }
    return sb.toString()
}
