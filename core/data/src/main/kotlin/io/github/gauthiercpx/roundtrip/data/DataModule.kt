package io.github.gauthiercpx.roundtrip.data

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
