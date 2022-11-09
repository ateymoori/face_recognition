package org.tensorflow.lite.examples.detection.clean.data.models

data class ConversationModel(
    val id: Int?,
    val member: String?,
    val question: String?,
    val first_answer: String?,
    val second_answer: String?,
    val third_answer: String?,
    val request_type: String?,
    val is_successful: Boolean? = true,
    val created_at: String?,
)
