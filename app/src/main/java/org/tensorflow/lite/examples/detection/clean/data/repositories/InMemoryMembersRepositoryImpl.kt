package org.tensorflow.lite.examples.detection.clean.data.repositories

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.tensorflow.lite.examples.detection.clean.data.models.MemberModel
import org.tensorflow.lite.examples.detection.clean.domain.repositories.InMemoryMembersRepository
import org.tensorflow.lite.examples.detection.clean.domain.repositories.MembersRepository
import javax.inject.Inject

class InMemoryMembersRepositoryImpl @Inject constructor(
    private val membersRepository: MembersRepository
) : InMemoryMembersRepository {

    private val _member = MutableStateFlow<MemberModel?>(null)

    override suspend fun setMember(memberName: String, faceBmp: Bitmap?) {
        _member.emit(
            MemberModel(
                id = 0, face = faceBmp, user_name = memberName, last_mood = null, last_conversation = null, uuid = null

            )
        )
//        "${memberName.toString()}".log("debug_face setMember")
//        if(_member.value?.user_name != memberName){
//            "${memberName.toString()}".log("debug_face .user_name != memberName")
//            //sync with server
//             membersRepository.getMemberByName(memberName).onSuccess {
//                _member.value = it
//                 "${it.toString()}".log("debug_face onSuccess")
//           }.onError {
//                 "${it.toString()}".log("debug_face onError")
//             }
//        }
    }

    override val getMember: StateFlow<MemberModel?>
        get() = _member
}