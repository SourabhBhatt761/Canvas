package com.srb.canvas.ui.authentication

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.Navigation
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.srb.canvas.R
import com.srb.canvas.databinding.FragmentSignUpBinding
import com.srb.canvas.ui.MainActivity


class SignUpFragment : Fragment() {

    private lateinit var _binding : FragmentSignUpBinding
    private val binding get() = _binding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentSignUpBinding.inflate(layoutInflater)

        binding.signUpSignUpButton.setOnClickListener {
            createAccount()
        }

        binding.signUpLogInButton.setOnClickListener {
            Navigation.findNavController(it).popBackStack()
        }

        binding.signUpBackButton.setOnClickListener {
            Navigation.findNavController(it).popBackStack()
        }


        return binding.root
    }

    private fun createAccount() {
        val email = binding.signUpEmail.text.toString()
        val password = binding.signUpPwdEt.text.toString()

        when{
            TextUtils.isEmpty(email)-> Snackbar.make(binding.root,"Email is required", Snackbar.LENGTH_LONG).show()
            TextUtils.isEmpty(password) || email.length < 6-> Snackbar.make(binding.root,"Password is invalid", Snackbar.LENGTH_LONG).show()

            else -> {
                val mAuth: FirebaseAuth = FirebaseAuth.getInstance()

                mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            saveUserInfo(email,password)
//
                            val intent = Intent(requireContext(), MainActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)

                            requireActivity().finish()
                        } else {
                            val message = task.exception.toString()
                            Snackbar.make(binding.root, "email or password is incorrect", Snackbar.LENGTH_SHORT)
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
//                    Toast.makeText(requireActivity(),"Welcome", Toast.LENGTH_SHORT).show()
                    Log.i("uni","sign up successfull")
                }else{
//                    Toast.makeText(requireActivity(),"Error occurred", Toast.LENGTH_SHORT).show()
                    Log.i("uni", task.exception.toString())
                }

            }
    }


}