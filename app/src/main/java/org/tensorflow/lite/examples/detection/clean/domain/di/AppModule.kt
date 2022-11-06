package org.tensorflow.lite.examples.detection.clean.domain.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.tensorflow.lite.examples.detection.clean.data.api.RestApi
import javax.inject.Singleton

//@Module
//@InstallIn(SingletonComponent::class)
//object AppModule {
//
//    @Provides
//    @Singleton
//    fun provideAskRepository(
//        restApi: RestApi
//    ): LoginRepository {
//        return AskQuestionRepositoryImpl(restApi)
//    }
//
//}