package com.app.replaybuffer.utils

import android.util.Log

/**
 * Centralized logging utility for the replay buffer module
 */
object Logger {

    fun debug(tag: String, message: String) {
        Log.d(tag, message)
    }

    fun info(tag: String, message: String) {
        Log.i(tag, message)
    }

    fun warn(tag: String, message: String) {
        Log.w(tag, message)
    }

    fun warn(tag: String, message: String, throwable: Throwable) {
        Log.w(tag, message, throwable)
    }

    fun error(tag: String, message: String) {
        Log.e(tag, message)
    }

    fun error(tag: String, message: String, throwable: Throwable) {
        Log.e(tag, message, throwable)
    }

    fun verbose(tag: String, message: String) {
        Log.v(tag, message)
    }
}
