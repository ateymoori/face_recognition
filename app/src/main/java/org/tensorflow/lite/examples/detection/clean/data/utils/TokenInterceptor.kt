package org.tensorflow.lite.examples.detection.clean.data.utils

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class TokenInterceptor @Inject constructor(
//    private val userLocalRepository: UserLocalRepository
) : Interceptor {

    var token: String? = ""

//    @Inject
//    lateinit var userCacheImpl: UserCacheImpl

    override fun intercept(chain: Interceptor.Chain): Response {

        var request = chain.request()

        if (request.header("No-Authentication") == null) {

//            token = userLocalRepository.getUser()?.access_token
            val finalToken = "Bearer $token"
            request = request.newBuilder()
                .addHeader("Authorization", finalToken)
                .build()
        }

        return chain.proceed(request)
    }

}