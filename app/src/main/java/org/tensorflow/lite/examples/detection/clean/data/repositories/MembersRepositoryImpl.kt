package org.tensorflow.lite.examples.detection.clean.data.repositories

import android.content.Context
import android.graphics.Bitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.tensorflow.lite.examples.detection.clean.data.api.RestApi
import org.tensorflow.lite.examples.detection.clean.data.models.MemberModel
import org.tensorflow.lite.examples.detection.clean.data.utils.BaseDataSource
import org.tensorflow.lite.examples.detection.clean.data.utils.Resource
import org.tensorflow.lite.examples.detection.clean.data.utils.onError
import org.tensorflow.lite.examples.detection.clean.data.utils.onSuccess
import org.tensorflow.lite.examples.detection.clean.domain.repositories.MembersRepository
import org.tensorflow.lite.examples.detection.log
import java.io.*
import javax.inject.Inject

class MembersRepositoryImpl @Inject constructor(
    private val restApi: RestApi, @ApplicationContext private val appContext: Context
) : MembersRepository, BaseDataSource() {

    override suspend fun addSyncRepository(member: MemberModel): Resource<MemberModel> {
        getResult {
            restApi.addMember(
                face = bitmapToMultipart(member.face!!),
                user_name = member.user_name,
                last_mood = member.last_mood ?: "",
                uuid = member.uuid ?: ""
            )
        }.onSuccess {
            return Resource.Success(it)
        }.onError {
            return Resource.Failure.Generic(it)
        }
        return Resource.Failure.Generic(null)
    }


    private fun bitmapToMultipart(imageBitmap: Bitmap): MultipartBody.Part {
        val bos = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100 /*ignored for PNG*/, bos)
        val bitmapdata = bos.toByteArray()
        val name: RequestBody = bitmapdata.toRequestBody("image/*".toMediaTypeOrNull(), 0, bitmapdata.size)
        return MultipartBody.Part.createFormData("face", "face.jpg", name)
    }

}



