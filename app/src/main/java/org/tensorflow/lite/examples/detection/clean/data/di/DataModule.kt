package org.tensorflow.lite.examples.detection.clean.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.tensorflow.lite.examples.detection.clean.data.api.RestApi
import org.tensorflow.lite.examples.detection.clean.data.repositories.ConversationsRepositoryImpl
import org.tensorflow.lite.examples.detection.clean.data.repositories.InMemoryMembersRepositoryImpl
import org.tensorflow.lite.examples.detection.clean.data.repositories.MembersRepositoryImpl
import org.tensorflow.lite.examples.detection.clean.domain.repositories.ConversationsRepository
import org.tensorflow.lite.examples.detection.clean.domain.repositories.InMemoryMembersRepository
import org.tensorflow.lite.examples.detection.clean.domain.repositories.MembersRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideMembers(api: RestApi): MembersRepository {
        return MembersRepositoryImpl(api)
    }

    @Provides
    @Singleton
    fun provideMemoryMembers(membersRepository: MembersRepository): InMemoryMembersRepository =
        InMemoryMembersRepositoryImpl(membersRepository)

    @Provides
    @Singleton
    fun provideConversations(api: RestApi): ConversationsRepository =
        ConversationsRepositoryImpl(api)

}