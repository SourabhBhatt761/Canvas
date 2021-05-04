package com.srb.canvas.ui

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.srb.canvas.R
import com.srb.canvas.databinding.ActivityLoginBinding


class LoginActivity : AppCompatActivity() {

    private var _binding: ActivityLoginBinding? = null
    private val binding get() = _binding!!


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = DataBindingUtil.setContentView(this, R.layout.activity_login)


        binding.logInSignUpButton.setOnClickListener {
            startActivity(Intent(this,SignUpActivity::class.java))
        }

        binding.logInLogInButton.setOnClickListener {
            logInUser()
        }

    }

    private fun logInUser() {
        val email = binding.logInEmail.text.toString()
        val password = binding.logInPwdEt.text.toString()

        when{
            TextUtils.isEmpty(email)-> Snackbar.make(binding.loginLayout,"Email is required", Snackbar.LENGTH_LONG).show()
            TextUtils.isEmpty(password)-> Snackbar.make(binding.loginLayout,"Password is required", Snackbar.LENGTH_LONG).show()

            else -> {
                val mAuth: FirebaseAuth = FirebaseAuth.getInstance()

                mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
//                            saveUserInfo(email)
//
                            val intent = Intent(this,MainActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)

                            finish()
                        } else {
                            val message = task.exception.toString()
                            Snackbar.make(binding.loginLayout, "Error : $message", Snackbar.LENGTH_SHORT)
                                .show()
                            //  Toast.makeText(this,"Error : $message",Toast.LENGTH_LONG).show()
                            mAuth.signOut()
                            // progressDialog.dismiss()
                        }
                    }
            }
        }
    }


}