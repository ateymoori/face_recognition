package org.tensorflow.lite.examples.detection

import android.app.Application
import com.orhanobut.hawk.Hawk

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Hawk.init(this).build()
    }
}