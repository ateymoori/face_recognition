package org.tensorflow.lite.examples.detection.clean.data.repositories

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.tensorflow.lite.examples.detection.clean.data.api.RestApi
import org.tensorflow.lite.examples.detection.clean.data.models.MemberModel
import org.tensorflow.lite.examples.detection.clean.data.utils.onSuccess
import org.tensorflow.lite.examples.detection.clean.domain.repositories.InMemoryMembersRepository
import org.tensorflow.lite.examples.detection.clean.domain.repositories.MembersRepository
import javax.inject.Inject

class InMemoryMembersRepositoryImpl @Inject constructor(
    private val membersRepository: MembersRepository
) : InMemoryMembersRepository {

    private val _member = MutableStateFlow<MemberModel?>(null)

    override suspend fun setMember(memberName: String) {
        if(_member.value?.user_name != memberName){
            //sync with server
            membersRepository.getMemberByName(memberName).onSuccess {
                _member.value = it
            }
        }
    }

    override val getMember: StateFlow<MemberModel?>
        get() = _member
}