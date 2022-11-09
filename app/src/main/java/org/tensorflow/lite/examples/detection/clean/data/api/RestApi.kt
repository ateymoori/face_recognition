package org.tensorflow.lite.examples.detection.clean.data.api

import android.graphics.Bitmap
import okhttp3.MultipartBody
import org.tensorflow.lite.examples.detection.clean.data.models.ConversationModel
import org.tensorflow.lite.examples.detection.clean.data.models.ConversationRequestModel
import org.tensorflow.lite.examples.detection.clean.data.models.MemberModel
import retrofit2.Response
import retrofit2.http.*

interface RestApi {

    companion object {
        const val API_VERSION = "v1"
    }

    @Multipart
    @POST("members/add")
    suspend fun addMember(
//        @Body member: MemberModel ,
        @Part face: MultipartBody.Part?,
        @Part("user_name") user_name: String,
        @Part("last_mood") last_mood: String,
        @Part("uuid") uuid: String
    ): Response<MemberModel>


    @POST("conversation")
    suspend fun startConversation(
        @Body conversation: ConversationRequestModel
    ): Response<ConversationModel>

    @GET("members/getByName")
    suspend fun getMemberByName(@Query("name") name: String?): Response<MemberModel>

}