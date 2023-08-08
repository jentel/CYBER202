package com.jentel.authdemo.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import com.google.firebase.auth.FirebaseAuth
import com.jentel.authdemo.R
import com.jentel.authdemo.databinding.ActivityLoginBinding
import com.jentel.authdemo.model.BiometricPromptUtils
import com.jentel.authdemo.model.SampleAppUser
import com.jentel.authdemo.util.CIPHERTEXT_WRAPPER
import com.jentel.authdemo.util.CryptographyManager
import com.jentel.authdemo.util.SHARED_PREFS_FILENAME

class LoginActivity : AppCompatActivity() {

    private val TAG = LoginActivity::class.simpleName
    private lateinit var binding: ActivityLoginBinding

    private lateinit var biometricPrompt: BiometricPrompt
    private val cryptographyManager = CryptographyManager()
    private val ciphertextWrapper
        get() = cryptographyManager.getCiphertextWrapperFromSharedPrefs(
            applicationContext,
            SHARED_PREFS_FILENAME,
            Context.MODE_PRIVATE,
            CIPHERTEXT_WRAPPER
        )

    private lateinit var  auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val canAuthenticate = BiometricManager.from(applicationContext).canAuthenticate()
        if(canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            binding.useBiometrics.visibility = View.VISIBLE
            binding.useBiometrics.setOnClickListener {
                if(ciphertextWrapper != null) {
                    showBiometricPromptForDecryption()
                } else {
                    startActivity(Intent(this, EnableBiometricLoginActivity::class.java))
                }
            }
        } else {
            binding.useBiometrics.visibility = View.INVISIBLE
        }

        auth= FirebaseAuth.getInstance()

        if(ciphertextWrapper == null) {
            // TODO setupForLoginWithPassword()
        }
    }

    /**
     * Biometrics Section
     */
    private fun showBiometricPromptForDecryption() {
        ciphertextWrapper?.let { textWrapper ->
            val secretKeyName = getString(R.string.secret_key_name)
            val cipher = cryptographyManager.getInitializedCipherForDecryption(
                secretKeyName, textWrapper.initializationVector
            )

            biometricPrompt = BiometricPromptUtils.createBiometricPrompt(
                this,
                ::decryptServerTokenFromStorage
            )
            val promptInfo = BiometricPromptUtils.createPromptInfo(this)
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }
    }

    private fun decryptServerTokenFromStorage(authResult: BiometricPrompt.AuthenticationResult) {
        ciphertextWrapper?.let { textWrapper ->
            authResult.cryptoObject?.cipher?.let {
                val plaintext = cryptographyManager.decryptData(textWrapper.ciphertext, it)
                SampleAppUser.fakeToken = plaintext

                // Now that you have the token, you can query server for everything else
                // the only reason we call this fakeToken is because we didn't really get it from
                // the server. In your case, you will have gotten it from the server the first time
                // and therefore, it's a real token.

//                auth.signInWithCustomToken(plaintext).addOnCompleteListener {
//                    task ->
//                    if(task.isSuccessful){
                        val intent= Intent(this,MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
//                }.addOnFailureListener { exception ->
//                    Toast.makeText(applicationContext,exception.localizedMessage, Toast.LENGTH_LONG).show()
//                }

                updateApp(getString(R.string.already_signedin))
            }
        //}
    }

    /**
     * Username + Password section
     */

    fun login(view: View){
        val email= binding.editTextEmailAddress.text.toString()
        val password = binding.editTextPassword.text.toString()
        auth.signInWithEmailAndPassword(email,password).addOnCompleteListener { task ->
            if(task.isSuccessful){
                val intent= Intent(this,MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(applicationContext,exception.localizedMessage, Toast.LENGTH_LONG).show()
        }
    }

    fun goToRegister(view: View){
        val intent= Intent(this,RegisterActivity::class.java)
        startActivity(intent)
    }

    private fun updateApp(successMsg: String) {
        Toast.makeText(applicationContext, successMsg, Toast.LENGTH_LONG)
    }
}