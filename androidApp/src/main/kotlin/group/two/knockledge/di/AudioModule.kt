package group.two.knockledge.di

import dagger.Binds
import dagger.Module
import group.two.knockledge.audio.AndroidAudioPlayer
import group.two.knockledge.audio.AudioPlayer
import javax.inject.Singleton

/** Provides audio playback hardware binding. */
@Module
abstract class AudioModule {

    /** Binds the platform MediaPlayer audio player implementation. */
    @Binds
    @Singleton
    abstract fun bindAudioPlayer(impl: AndroidAudioPlayer): AudioPlayer
}
