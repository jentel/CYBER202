package com.jentel.authdemo.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jentel.authdemo.databinding.ActivityEnableBiometricLoginBinding
import com.jentel.authdemo.model.BiometricPromptUtils
import com.jentel.authdemo.model.SampleAppUser
import com.jentel.authdemo.util.CIPHERTEXT_WRAPPER
import com.jentel.authdemo.util.CryptographyManager
import com.jentel.authdemo.util.SHARED_PREFS_FILENAME


class EnableBiometricLoginActivity : AppCompatActivity() {

    private val TAG = EnableBiometricLoginActivity::class.simpleName
    private lateinit var cryptographyManager: CryptographyManager

    private var customToken: String? = null

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityEnableBiometricLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.cancel.setOnClickListener { finish() }
        binding.authorize.setOnClickListener { (showBiometricPromptForEncryption()) }

       auth = Firebase.auth
    }

    private fun showBiometricPromptForEncryption() {
        val canAuthenticate = BiometricManager.from(applicationContext).canAuthenticate()
        if(canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            val secretKeyName = "biometric_sample_encryption_key"
            cryptographyManager = CryptographyManager()
            val cipher = cryptographyManager.getInitializedCipherForEncryption(secretKeyName)
            val biometricPrompt = BiometricPromptUtils.createBiometricPrompt(this, ::encryptAndStoreServerToken)
            val promptInfo = BiometricPromptUtils.createPromptInfo(this)
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }
    }

    private fun encryptAndStoreServerToken(authResult: BiometricPrompt.AuthenticationResult) {
        authResult.cryptoObject?.cipher?.apply {
            SampleAppUser.fakeToken?.let { token ->
                Log.d(TAG, "Token from the server is $token")

                val encryptedServerTokenWrapper = cryptographyManager.encryptData(token, this)
                cryptographyManager.persistCiphertextWrapperToSharedPrefs(
                    encryptedServerTokenWrapper,
                    applicationContext,
                    SHARED_PREFS_FILENAME,
                    Context.MODE_PRIVATE,
                    CIPHERTEXT_WRAPPER
                )
            }
        }
        val intent= Intent(applicationContext,MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}