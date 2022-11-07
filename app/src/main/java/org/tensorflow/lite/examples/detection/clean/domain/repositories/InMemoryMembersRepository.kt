package org.tensorflow.lite.examples.detection.clean.domain.repositories

import kotlinx.coroutines.flow.StateFlow
import org.tensorflow.lite.examples.detection.clean.data.models.MemberModel
import org.tensorflow.lite.examples.detection.clean.data.utils.Resource

interface InMemoryMembersRepository {
    suspend fun setMember(memberName: String)
    val getMember: StateFlow<MemberModel?>
}