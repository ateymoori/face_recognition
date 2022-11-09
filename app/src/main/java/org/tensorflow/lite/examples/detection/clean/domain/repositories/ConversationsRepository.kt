package org.tensorflow.lite.examples.detection.clean.domain.repositories

import org.tensorflow.lite.examples.detection.clean.data.models.ConversationModel
import org.tensorflow.lite.examples.detection.clean.data.models.ConversationRequestModel
import org.tensorflow.lite.examples.detection.clean.data.models.MemberModel
import org.tensorflow.lite.examples.detection.clean.data.utils.Resource

interface ConversationsRepository {
    suspend fun startConversation(conversationRequestModel: ConversationRequestModel): Resource<ConversationModel>
}