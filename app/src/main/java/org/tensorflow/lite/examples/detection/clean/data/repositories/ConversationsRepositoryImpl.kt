package org.tensorflow.lite.examples.detection.clean.data.repositories

import org.tensorflow.lite.examples.detection.clean.data.api.RestApi
import org.tensorflow.lite.examples.detection.clean.data.models.ConversationModel
import org.tensorflow.lite.examples.detection.clean.data.models.ConversationRequestModel
import org.tensorflow.lite.examples.detection.clean.data.utils.BaseDataSource
import org.tensorflow.lite.examples.detection.clean.data.utils.Resource
import org.tensorflow.lite.examples.detection.clean.data.utils.onError
import org.tensorflow.lite.examples.detection.clean.data.utils.onSuccess
import org.tensorflow.lite.examples.detection.clean.domain.repositories.ConversationsRepository
import javax.inject.Inject

class ConversationsRepositoryImpl @Inject constructor(
    private val restApi: RestApi,
) : ConversationsRepository, BaseDataSource() {

    override suspend fun startConversation(conversationRequestModel: ConversationRequestModel): Resource<ConversationModel> {
        getResult {
            restApi.startConversation(
                conversationRequestModel
            )
        }.onSuccess {
            return Resource.Success(it)
        }.onError {
            return Resource.Failure.Generic(it)
        }
        return Resource.Failure.Generic(null)
    }
}



