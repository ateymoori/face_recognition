package org.tensorflow.lite.examples.detection.clean.presentation.base

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

open class BaseViewModel( ) : ViewModel(),CoroutineScope {

    private val viewModelJob = SupervisorJob()

    override val coroutineContext: CoroutineContext = viewModelJob + Dispatchers.Main

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}