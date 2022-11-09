package org.tensorflow.lite.examples.detection.clean.presentation.camera

import android.graphics.Bitmap
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.tensorflow.lite.examples.detection.clean.data.models.ConversationModel
import org.tensorflow.lite.examples.detection.clean.data.models.MemberModel
import org.tensorflow.lite.examples.detection.clean.data.utils.Resource
import org.tensorflow.lite.examples.detection.clean.data.utils.log
import org.tensorflow.lite.examples.detection.clean.data.utils.onError
import org.tensorflow.lite.examples.detection.clean.data.utils.onSuccess
import org.tensorflow.lite.examples.detection.clean.domain.repositories.InMemoryMembersRepository
import org.tensorflow.lite.examples.detection.clean.domain.usecases.AddSyncMember
import org.tensorflow.lite.examples.detection.clean.domain.usecases.StartConversation
import org.tensorflow.lite.examples.detection.clean.presentation.base.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val addSyncMember: AddSyncMember,
    private val inMemoryMembersRepository: InMemoryMembersRepository,
    private val startConversation: StartConversation
) : BaseViewModel(), LifecycleObserver {

    val syncState = MutableLiveData<Resource<MemberModel>>()
    val _member = MutableStateFlow<MemberModel?>(null)
    val _conversation = MutableStateFlow<ConversationModel?>(null)

    init {
        viewModelScope.launch {
            inMemoryMembersRepository.getMember
                .collect {
                    "${it.toString()}".log("debug_face inMemoryMembersRepository.getMember")
                    _member.value = it
                }
        }
    }

    fun syncUser(memberModel: MemberModel) {
        "${memberModel.toString()}".log("debug_face memberModel")
        viewModelScope.launch {
            syncState.postValue(Resource.Loading())
            syncState.postValue(addSyncMember.invoke(memberModel))
        }
    }

    fun faceDetected(memberName: String, faceBmp: Bitmap?) {
        "${memberName.toString()}".log("debug_face faceDetected")
        viewModelScope.launch {
            inMemoryMembersRepository.setMember(memberName , faceBmp)
        }
    }

    fun conversation(question: String?) {
        viewModelScope.launch {
            startConversation.invoke(question).onSuccess {
                _conversation.value = it
            }.onError {

            }
        }
    }
}