package org.tensorflow.lite.examples.detection.clean.data.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.tensorflow.lite.examples.detection.clean.data.api.RestApi
import org.tensorflow.lite.examples.detection.clean.data.repositories.MembersRepositoryImpl
import org.tensorflow.lite.examples.detection.clean.domain.repositories.MembersRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideMembers(api: RestApi, @ApplicationContext appContext: Context): MembersRepository {
        return MembersRepositoryImpl(api, appContext)
    }
}