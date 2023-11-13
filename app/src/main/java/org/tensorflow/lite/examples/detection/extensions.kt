package org.tensorflow.lite.examples.detection

import android.util.Log

fun String.log(tag: String? = "debug"): String {
    if (BuildConfig.DEBUG) {
        Log.d(tag, this)
    }
    return this
}