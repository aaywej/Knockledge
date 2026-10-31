package group.two.knockledge.di

import android.app.Application
import dagger.Module
import dagger.Provides
import group.two.knockledge.audio.AndroidAudioPlayer
import group.two.knockledge.audio.AudioPlayer
import javax.inject.Singleton

/** Provides audio playback hardware binding. */
@Module
object AudioModule {

    /** Provides the platform MediaPlayer audio player implementation. */
    @Provides
    @Singleton
    fun provideAudioPlayer(app: Application): AudioPlayer = AndroidAudioPlayer(app)
}
