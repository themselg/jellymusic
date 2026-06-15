package dev.themselg.jellymusic.di

import dev.themselg.jellymusic.player.PlayerController
import dev.themselg.jellymusic.player.PlayerControllerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerModule {

    @Binds
    @Singleton
    abstract fun bindPlayerController(impl: PlayerControllerImpl): PlayerController
}
