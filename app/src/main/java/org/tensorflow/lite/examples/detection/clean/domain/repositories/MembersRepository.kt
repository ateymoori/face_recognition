package org.tensorflow.lite.examples.detection.clean.domain.repositories

import org.tensorflow.lite.examples.detection.clean.data.models.MemberModel
import org.tensorflow.lite.examples.detection.clean.data.utils.Resource

interface MembersRepository {
    suspend fun addSyncRepository(member:MemberModel): Resource<MemberModel>
}