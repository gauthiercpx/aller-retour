package io.github.gauthiercpx.roundtrip.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataStoreModule {
    @Binds
    abstract fun bindSettingsStore(impl: DataStoreSettingsStore): SettingsStore

    @Binds
    abstract fun bindDeparturesCacheStore(impl: DataStoreDeparturesCacheStore): DeparturesCacheStore

    companion object {
        // Exactly one DataStore instance per file in the process, hence the singleton.
        @Provides
        @Singleton
        fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("round_trip") }
    }
}
