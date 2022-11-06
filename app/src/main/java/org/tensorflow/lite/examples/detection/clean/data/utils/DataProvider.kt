package org.tensorflow.lite.examples.detection.clean.data.utils

import android.content.Context
import com.orhanobut.hawk.Hawk
import java.lang.Exception
import javax.inject.Inject

class DataProvider @Inject constructor(
    private val context: Context
) {
    private val anyLock = Any()
    fun saveData(key: String, data: Any?) {
        synchronized(anyLock) {
            Hawk.put(key, data)
        }
    }

    fun getData(key: String): Any? {
//        return try {
          return  Hawk.get(key, null)
//        }catch (e:Exception){
//            null
//        }
    }

    fun removeData(key: String) {
        synchronized(anyLock) {
            Hawk.delete(key)
        }
    }

    fun cleanAll(key: String) {
        synchronized(anyLock) {
            Hawk.deleteAll()
        }
    }

}