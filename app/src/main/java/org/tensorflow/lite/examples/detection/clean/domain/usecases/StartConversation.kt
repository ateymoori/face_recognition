package org.tensorflow.lite.examples.detection.clean.domain.usecases

import org.tensorflow.lite.examples.detection.clean.data.models.ConversationModel
import org.tensorflow.lite.examples.detection.clean.data.models.ConversationRequestModel
import org.tensorflow.lite.examples.detection.clean.data.models.MemberModel
import org.tensorflow.lite.examples.detection.clean.data.utils.Resource
import org.tensorflow.lite.examples.detection.clean.domain.repositories.ConversationsRepository
import org.tensorflow.lite.examples.detection.clean.domain.repositories.InMemoryMembersRepository
import org.tensorflow.lite.examples.detection.clean.domain.repositories.MembersRepository
import javax.inject.Inject

class StartConversation @Inject constructor(
    private val conversationsRepository: ConversationsRepository,
    private val inMemoryMembersRepository: InMemoryMembersRepository
) : UseCase<String, Resource<ConversationModel>>() {

    override suspend fun invoke(data: String?): Resource<ConversationModel> {
        val request = ConversationRequestModel(
            user_name = inMemoryMembersRepository.getMember.value?.user_name ?: "amir",
            question = data,
            request_type = "QUESTION"
        )
        return conversationsRepository.startConversation(request)
    }
}