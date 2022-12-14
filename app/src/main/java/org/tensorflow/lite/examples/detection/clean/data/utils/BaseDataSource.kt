package org.tensorflow.lite.examples.detection.clean.data.utils

import org.tensorflow.lite.examples.detection.clean.data.utils.GsonUtils.toObjectByGson
import retrofit2.HttpException
import retrofit2.Response
import java.net.UnknownHostException

abstract class BaseDataSource {
    protected suspend fun <T> getResult(call: suspend () -> Response<T>): Resource<T> {
        try {
            val response = call()

            if (response.isSuccessful)
                return Resource.Success(response.body())
            else {
                if (response.code() == 401)
                    return Resource.Failure.UnAuthorized(response.raw().message)
            }
            return Resource.Failure.Generic(
                response.errorBody()?.charStream()?.toObjectByGson(ErrorDto::class.java)?.getText()
            )
        } catch (e: Exception) {
            return when (e) {
                is UnknownHostException -> Resource.Failure.NetworkException(e.message)
                is HttpException -> {
                    if (e.code() == 401)
                        Resource.Failure.UnAuthorized(e.message)
                    else
                        error(e.message ?: e.toString())
                }
                else -> error(e.message ?: e.toString())
            }
        }
    }

    private fun <T> error(message: String): Resource<T> {
        return Resource.Failure.Generic("Network call has failed for a following reason: $message")
    }
}