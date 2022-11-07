package org.tensorflow.lite.examples.detection

import android.app.Application
import com.orhanobut.hawk.Hawk
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {
    companion object{
        lateinit var instance : Application
    }
    override fun onCreate() {
        super.onCreate()
        instance = this
        Hawk.init(this).build()
    }
}