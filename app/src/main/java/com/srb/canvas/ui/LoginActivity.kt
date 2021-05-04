package com.srb.canvas.ui

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.srb.canvas.R
import com.srb.canvas.databinding.ActivityLoginBinding
import com.srb.canvas.utils.snackBarMsg


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
            TextUtils.isEmpty(password) || email.length < 6-> Snackbar.make(binding.loginLayout,"Password is invalid", Snackbar.LENGTH_LONG).show()

            else -> {
                val mAuth: FirebaseAuth = FirebaseAuth.getInstance()

                mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            saveUserInfo(email,password)
//
                            val intent = Intent(this,MainActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)

                            finish()
                        } else {
                            val message = task.exception.toString()
                            Snackbar.make(binding.loginLayout, "email or password is incorrect", Snackbar.LENGTH_SHORT)
                                .show()
                            //  Toast.makeText(this,"Error : $message",Toast.LENGTH_LONG).show()
                            mAuth.signOut()
                            // progressDialog.dismiss()
                        }
                    }
            }
        }
    }

    private fun saveUserInfo(email: String,pwd : String) {

        val map = HashMap<String,String>()
        map["email"] = email
        map["pwd"] = pwd

        val db = FirebaseFirestore.getInstance()
        db.collection("data").document("credentials")
            .set(map).addOnCompleteListener { task ->
                if(task.isSuccessful){
                    Toast.makeText(this,"Welcome", Toast.LENGTH_SHORT).show()
                }else{
                    Toast.makeText(this,"Error occured", Toast.LENGTH_SHORT).show()
                }

            }
    }


}