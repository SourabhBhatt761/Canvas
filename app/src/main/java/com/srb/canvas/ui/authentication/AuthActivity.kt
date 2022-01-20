package com.srb.canvas.ui.authentication

import android.content.Intent
import android.os.Bundle
import  androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.srb.canvas.R
import com.srb.canvas.databinding.ActivityAuthBinding
import com.srb.canvas.ui.MainActivity

class AuthActivity : AppCompatActivity() {

    private lateinit var _binding : ActivityAuthBinding
    private val binding  get() = _binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityAuthBinding.inflate(layoutInflater)
        setTheme(R.style.AppTheme)

        setContentView(binding.root)

        if (FirebaseAuth.getInstance().currentUser != null) {

            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)

            finish()
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

    }

}