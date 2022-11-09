package org.tensorflow.lite.examples.detection.clean.data.models

data class ConversationRequestModel(
    val user_name: String?,
    val question: String?,
    val request_type: String? = "QUESTION"
)
