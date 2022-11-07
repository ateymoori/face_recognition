package org.tensorflow.lite.examples.detection.clean.presentation.camera

import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.tensorflow.lite.examples.detection.clean.data.models.MemberModel
import org.tensorflow.lite.examples.detection.clean.data.utils.Resource
import org.tensorflow.lite.examples.detection.clean.data.utils.log
import org.tensorflow.lite.examples.detection.clean.domain.repositories.InMemoryMembersRepository
import org.tensorflow.lite.examples.detection.clean.domain.usecases.AddSyncMember
import org.tensorflow.lite.examples.detection.clean.presentation.base.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val addSyncMember: AddSyncMember,
    private val inMemoryMembersRepository: InMemoryMembersRepository
) : BaseViewModel(), LifecycleObserver {

    val syncState = MutableLiveData<Resource<MemberModel>>()
    val _member = MutableStateFlow<MemberModel?>(null)


    fun syncUser(memberModel: MemberModel) {
        viewModelScope.launch {
            syncState.postValue(Resource.Loading())
            syncState.postValue(addSyncMember.invoke(memberModel))
        }
    }

    fun faceDetected(memberName: String) {
        "faceDetected".log(memberName)
        viewModelScope.launch {
            inMemoryMembersRepository.setMember(memberName)
        }
    }

    init {
        viewModelScope.launch {
            inMemoryMembersRepository.getMember
                .collect {
                    _member.value = it
                }
        }
    }

}