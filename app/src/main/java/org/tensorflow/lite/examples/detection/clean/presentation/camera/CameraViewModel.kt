package org.tensorflow.lite.examples.detection.clean.presentation.camera

import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.tensorflow.lite.examples.detection.clean.data.models.MemberModel
import org.tensorflow.lite.examples.detection.clean.data.utils.Resource
import org.tensorflow.lite.examples.detection.clean.domain.usecases.AddSyncMember
import org.tensorflow.lite.examples.detection.clean.presentation.base.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val addSyncMember: AddSyncMember
) : BaseViewModel(), LifecycleObserver {

    val syncState = MutableLiveData<Resource<MemberModel>>()

    fun syncUser(memberModel: MemberModel) {
        viewModelScope.launch {
            syncState.postValue(Resource.Loading())
            syncState.postValue(addSyncMember.invoke(memberModel))
        }
    }

}