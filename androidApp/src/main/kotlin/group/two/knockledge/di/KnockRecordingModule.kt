package group.two.knockledge.di

import dagger.Binds
import dagger.Module
import group.two.knockledge.core.FileLocalAudioRingBuffer
import group.two.knockledge.core.LocalAudioRingBuffer
import group.two.knockledge.sensor.AndroidKnockGestureDetector
import group.two.knockledge.sensor.KnockGestureDetector
import javax.inject.Singleton

/** Binds the recording collaborators to their Android implementations, one of each per process. */
@Module
abstract class KnockRecordingModule {

    /** The single detector the service listens through. */
    @Binds
    @Singleton
    abstract fun bindGestureDetector(impl: AndroidKnockGestureDetector): KnockGestureDetector

    /** The single buffer the service writes and Rewind snapshots. */
    @Binds
    @Singleton
    abstract fun bindAudioRingBuffer(impl: FileLocalAudioRingBuffer): LocalAudioRingBuffer
}
