package com.promethean.proxy.di

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private const val USER_PREFS_NAME = "user_settings"

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = USER_PREFS_NAME
)

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }
}


@Singleton
class PreferenceRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {

        val URL_KEY = stringPreferencesKey("ip")
        val PORT_KEY = intPreferencesKey("port")
        val USERNAME_KEY = stringPreferencesKey("username")
        val PASSWORD_KEY = stringPreferencesKey("password")
        val TOKEN = stringPreferencesKey("token")
        val TOKEN_EXPIRY = stringPreferencesKey("tokenExpiry")
    }

    suspend fun <T> getValue(key: Preferences.Key<T>, default: T): T {
        return dataStore.data.first()[key] ?: default
    }

    suspend fun <T> setValue(key: Preferences.Key<T>, value: T) {
        dataStore.edit { prefs -> prefs[key] = value }
    }

    public suspend fun getUrl() = getValue(Keys.URL_KEY, "127.0.0.1")
    public suspend fun setUrl(value: String) = setValue(Keys.URL_KEY, value)

    public suspend fun getPort() = getValue(Keys.PORT_KEY, 8080)
    public suspend fun setPort(value: Int) = setValue(Keys.PORT_KEY, value)

    public suspend fun getUsername() = getValue(Keys.USERNAME_KEY, "")
    public  suspend fun setUsername(value: String) = setValue(Keys.USERNAME_KEY, value)

    public suspend fun getPassword() = getValue(Keys.PASSWORD_KEY, "")
    public suspend fun setPassword(value: String) = setValue(Keys.PASSWORD_KEY, value)

    public  suspend fun getToken() = getValue(Keys.TOKEN, "")
    public suspend fun setToken(value: String) = setValue(Keys.TOKEN, value)

    public suspend fun getTokenExpiry() = getValue(Keys.TOKEN_EXPIRY, "")
    public suspend fun setTokenExpiry(value: String) = setValue(Keys.TOKEN_EXPIRY, value)
}

