package dev.blazelight.p4oc.core.log

import android.util.Log
import dev.blazelight.p4oc.BuildConfig

object AppLog {

    fun d(tag: String, msg: String) {
        if (BuildConfig.DEBUG) Log.d(tag, msg)
    }

    fun d(tag: String, msg: () -> String) {
        if (BuildConfig.DEBUG) Log.d(tag, msg())
    }

    fun v(tag: String, msg: String) {
        if (BuildConfig.DEBUG) Log.v(tag, msg)
    }

    fun v(tag: String, msg: () -> String) {
        if (BuildConfig.DEBUG) Log.v(tag, msg())
    }

    fun i(tag: String, msg: String) {
        if (BuildConfig.DEBUG) Log.i(tag, msg)
    }

    fun i(tag: String, msg: () -> String) {
        if (BuildConfig.DEBUG) Log.i(tag, msg())
    }

    fun w(tag: String, msg: String) {
        if (BuildConfig.DEBUG) Log.w(tag, msg)
    }

    fun w(tag: String, msg: String, throwable: Throwable?) {
        if (BuildConfig.DEBUG) Log.w(tag, msg, throwable)
    }

    fun e(tag: String, msg: String) {
        if (BuildConfig.DEBUG) Log.e(tag, msg)
    }

    fun e(tag: String, msg: String, throwable: Throwable?) {
        if (BuildConfig.DEBUG) Log.e(tag, msg, throwable)
    }
}
