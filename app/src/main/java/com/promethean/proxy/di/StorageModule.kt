package com.promethean.proxy.di

import android.R
import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.promethean.proxy.ui.theme.style.ThemeMode
import com.promethean.proxy.ui.theme.style.ThemeStyle
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SettingsPrefs

val SECRET_USER_PREFS_NAME = "secret_user_settings"
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

    @Provides
    @Singleton
    @SettingsPrefs
    fun provideEncryptedSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            SECRET_USER_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}

@Singleton
class PreferenceRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @SettingsPrefs private val secretPrefs: SharedPreferences
) {
    private object Keys {
        val URL_KEY = stringPreferencesKey("ip")
        val PORT_KEY = intPreferencesKey("port")
        val WITH_AUTH = booleanPreferencesKey("withAuth")
        val THEME = stringPreferencesKey("theme")
        val THEME_MODE = stringPreferencesKey("themeMode")


        const val USERNAME_KEY = "username"
        const val PASSWORD_KEY = "password"
        const val TOKEN = "token"
        const val TOKEN_EXPIRY = "tokenExpiry"
    }

    suspend fun <T> getValue(key: Preferences.Key<T>, default: T): T {
        return dataStore.data.first()[key] ?: default
    }

    suspend fun <T> setValue(key: Preferences.Key<T>, value: T) {
        dataStore.edit { prefs -> prefs[key] = value }
    }

    suspend fun getUrl() = getValue(Keys.URL_KEY, "")
    suspend fun setUrl(value: String) = setValue(Keys.URL_KEY, value)

    suspend fun getPort() = getValue(Keys.PORT_KEY, 0)
    suspend fun setPort(value: Int) = setValue(Keys.PORT_KEY, value)

    suspend fun getWithAuth() = getValue(Keys.WITH_AUTH, true)
    suspend fun setWithAuth(value: Boolean) = setValue(Keys.WITH_AUTH, value)

    val themeFlow: Flow<ThemeStyle> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val name = preferences[Keys.THEME] ?: ThemeStyle.BLUE.name
            try { ThemeStyle.valueOf(name) } catch (e: Exception) { ThemeStyle.BLUE }
        }

    val themeModeFlow: Flow<ThemeMode> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val name = preferences[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
            try { ThemeMode.valueOf(name) } catch (e: Exception) { ThemeMode.SYSTEM }
        }

    suspend fun setTheme(style: ThemeStyle) { setValue(Keys.THEME, style.name) }

    suspend fun setThemeMode(mode: ThemeMode) { setValue(Keys.THEME_MODE, mode.name)   }

    // Secret Prefs
    fun getUsername() = secretPrefs.getString(Keys.USERNAME_KEY, "") ?: ""
    fun setUsername(value: String) = secretPrefs.edit().putString(Keys.USERNAME_KEY, value).apply()

    fun getPassword() = secretPrefs.getString(Keys.PASSWORD_KEY, "") ?: ""
    fun setPassword(value: String) = secretPrefs.edit().putString(Keys.PASSWORD_KEY, value).apply()

    fun getToken() = secretPrefs.getString(Keys.TOKEN, "") ?: ""
    fun setToken(value: String) = secretPrefs.edit().putString(Keys.TOKEN, value).apply()

    fun getTokenExpiry() = secretPrefs.getString(Keys.TOKEN_EXPIRY, "") ?: ""
    fun setTokenExpiry(value: String) = secretPrefs.edit().putString(Keys.TOKEN_EXPIRY, value).apply()
}
