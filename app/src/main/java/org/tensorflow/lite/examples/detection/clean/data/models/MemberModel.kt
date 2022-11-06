package org.tensorflow.lite.examples.detection.clean.data.models

import android.graphics.Bitmap

data class MemberModel(
    var id: Int,
    var face: Bitmap?,
    var user_name: String,
    var last_mood: String? = null,
    var last_conversation: String? = null,
    var uuid: String? = null
)
