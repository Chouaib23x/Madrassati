package dz.madrassati.data.repo

import android.content.SharedPreferences
import android.util.Base64
import dz.madrassati.domain.repository.AuthRepository
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val prefs: SharedPreferences
) : AuthRepository {

    override suspend fun hasAccount(): Boolean =
        prefs.contains(KEY_USERNAME)

    override suspend fun createAccount(username: String, password: String): Boolean =
        withContext(Dispatchers.IO) {
            if (prefs.contains(KEY_USERNAME)) return@withContext false
            val salt = randomSalt()
            prefs.edit()
                .putString(KEY_USERNAME, username.trim())
                .putString(KEY_SALT, encode(salt))
                .putString(KEY_HASH, hash(password, salt))
                .apply()
            true
        }

    override suspend fun login(username: String, password: String): Boolean =
        withContext(Dispatchers.IO) {
            val storedUsername = prefs.getString(KEY_USERNAME, null) ?: return@withContext false
            val salt = prefs.getString(KEY_SALT, null)?.let { decode(it) } ?: return@withContext false
            val storedHash = prefs.getString(KEY_HASH, null) ?: return@withContext false
            storedUsername == username.trim() && storedHash == hash(password, salt)
        }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Boolean =
        withContext(Dispatchers.IO) {
            if (!verifyCurrent(currentPassword)) return@withContext false
            val salt = randomSalt()
            prefs.edit()
                .putString(KEY_SALT, encode(salt))
                .putString(KEY_HASH, hash(newPassword, salt))
                .apply()
            true
        }

    override suspend fun resetPassword(newPassword: String) =
        withContext(Dispatchers.IO) {
            val salt = randomSalt()
            prefs.edit()
                .putString(KEY_SALT, encode(salt))
                .putString(KEY_HASH, hash(newPassword, salt))
                .apply()
        }

    override fun rememberedUsername(): String =
        prefs.getString(KEY_REMEMBERED_USERNAME, "").orEmpty()

    override suspend fun setRememberedUsername(username: String?) =
        withContext(Dispatchers.IO) {
            if (username.isNullOrBlank()) {
                prefs.edit().remove(KEY_REMEMBERED_USERNAME).apply()
            } else {
                prefs.edit().putString(KEY_REMEMBERED_USERNAME, username.trim()).apply()
            }
        }

    private suspend fun verifyCurrent(currentPassword: String): Boolean {
        val salt = prefs.getString(KEY_SALT, null)?.let { decode(it) } ?: return false
        val storedHash = prefs.getString(KEY_HASH, null) ?: return false
        return storedHash == hash(currentPassword, salt)
    }

    override fun hasPin(): Boolean = prefs.contains(KEY_PIN)

    override fun ensureDefaultPin() {
        if (!prefs.contains(KEY_PIN)) {
            prefs.edit().putString(KEY_PIN, DEFAULT_PIN).apply()
        }
    }

    override suspend fun setPin(pin: String) {
        prefs.edit().putString(KEY_PIN, pin).apply()
    }

    override suspend fun verifyPin(pin: String): Boolean =
        prefs.getString(KEY_PIN, null) == pin

    private fun randomSalt(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

    private fun hash(password: String, salt: ByteArray): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return encode(factory.generateSecret(spec).encoded)
    }

    private fun encode(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun decode(value: String): ByteArray =
        Base64.decode(value, Base64.NO_WRAP)

    companion object {
        private const val KEY_USERNAME = "auth_username"
        private const val KEY_SALT = "auth_salt"
        private const val KEY_HASH = "auth_hash"
        private const val KEY_PIN = "auth_pin"
        private const val KEY_REMEMBERED_USERNAME = "auth_remembered_username"
        private const val ITERATIONS = 65536
        private const val KEY_BITS = 256
        const val DEFAULT_PIN = "1234"
    }
}
