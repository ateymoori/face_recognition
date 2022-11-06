package org.tensorflow.lite.examples.detection.clean.domain.usecases

import org.tensorflow.lite.examples.detection.clean.data.models.MemberModel
import org.tensorflow.lite.examples.detection.clean.data.utils.Resource
import org.tensorflow.lite.examples.detection.clean.domain.repositories.MembersRepository
import javax.inject.Inject

class AddSyncMember @Inject constructor(
    private val membersRepository: MembersRepository
) : UseCase<MemberModel, Resource<MemberModel>>() {
    override suspend fun invoke(data: MemberModel?): Resource<MemberModel> {
        return membersRepository.addSyncRepository(data!!)
    }
}