package com.example.vitalize

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SignupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.signup)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val nameEt = findViewById<EditText>(R.id.nameEt)
        val emailEt = findViewById<EditText>(R.id.emailEt)
        val passwordEt = findViewById<EditText>(R.id.passwordEt)
        val confirmPasswordEt = findViewById<EditText>(R.id.confirmPasswordEt)
        val signupBtn = findViewById<Button>(R.id.signupBtn)
        val signinLink = findViewById<Button>(R.id.signinLink)

        // ✅ Save to SharedPreferences on signup
        signupBtn.setOnClickListener {
            val name = nameEt.text.toString().trim()
            val email = emailEt.text.toString().trim()
            val password = passwordEt.text.toString().trim()
            val confirmPassword = confirmPasswordEt.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val prefs = getSharedPreferences("VitalizePrefs", MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putString("name", name)
            editor.putString("email", email)
            editor.putString("password", password)
            editor.apply()

            Toast.makeText(this, "Signup successful! Please login.", Toast.LENGTH_SHORT).show()

            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Navigate to Login
        signinLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
