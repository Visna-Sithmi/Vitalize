package com.example.vitalize

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.Spanned
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.pow

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        // Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val prefs = getSharedPreferences("VitalizePrefs", Context.MODE_PRIVATE)

        // 🔹 UI components
        val tvName = findViewById<TextView>(R.id.tvName)
        val tvEmail = findViewById<TextView>(R.id.tvEmail)
        val tvPassword = findViewById<TextView>(R.id.tvPassword)
        val btnBackHome = findViewById<Button>(R.id.btnBackHome)
        val btnChangePassword = findViewById<Button>(R.id.btnChangePassword)
        val etGender = findViewById<EditText>(R.id.etGender)
        val etAge = findViewById<EditText>(R.id.etAge)
        val etHeight = findViewById<EditText>(R.id.etHeight)
        val etWeight = findViewById<EditText>(R.id.etWeight)
        val btnCalculateBMI = findViewById<Button>(R.id.btnCalculateBMI)

        // Load user data
        tvName.text = prefs.getString("name", "Not available")
        tvEmail.text = prefs.getString("email", "Not available")
        tvPassword.text = prefs.getString("password", "Not available")

        etGender.setText(prefs.getString("gender", ""))
        etAge.setText(prefs.getString("age", ""))
        etHeight.setText(prefs.getString("height", ""))
        etWeight.setText(prefs.getString("weight", ""))

        // 🔹 Input filters + validation

        // Gender (only letters)
        val onlyLetters = object : InputFilter {
            override fun filter(
                source: CharSequence?, start: Int, end: Int,
                dest: Spanned?, dstart: Int, dend: Int
            ): CharSequence? {
                if (source != null && !source.matches(Regex("[a-zA-Z ]*"))) {
                    Toast.makeText(this@ProfileActivity, "Invalid: only letters allowed in Gender", Toast.LENGTH_SHORT).show()
                    return ""
                }
                return null
            }
        }
        etGender.filters = arrayOf(onlyLetters)

        // Age (numbers only, max 100)
        val ageFilter = object : InputFilter {
            override fun filter(
                source: CharSequence?, start: Int, end: Int,
                dest: Spanned?, dstart: Int, dend: Int
            ): CharSequence? {
                if (source != null && !source.matches(Regex("[0-9]*"))) {
                    Toast.makeText(this@ProfileActivity, "Invalid: only numbers allowed in Age", Toast.LENGTH_SHORT).show()
                    return ""
                }
                val newText = (dest?.substring(0, dstart) ?: "") +
                        source?.subSequence(start, end) +
                        (dest?.substring(dend, dest.length) ?: "")
                if (newText.isNotEmpty() && newText.toIntOrNull() != null && newText.toInt() > 100) {
                    Toast.makeText(this@ProfileActivity, "Age cannot exceed 100", Toast.LENGTH_SHORT).show()
                    return ""
                }
                return null
            }
        }
        etAge.filters = arrayOf(ageFilter)

        // Height (50–250 cm)
        val heightFilter = object : InputFilter {
            override fun filter(
                source: CharSequence?, start: Int, end: Int,
                dest: Spanned?, dstart: Int, dend: Int
            ): CharSequence? {
                if (source != null && !source.matches(Regex("[0-9]*"))) {
                    Toast.makeText(this@ProfileActivity, "Invalid: only numbers allowed in Height", Toast.LENGTH_SHORT).show()
                    return ""
                }
                val newText = (dest?.substring(0, dstart) ?: "") +
                        source?.subSequence(start, end) +
                        (dest?.substring(dend, dest.length) ?: "")
                val value = newText.toIntOrNull()
                if (value != null && (value < 50 || value > 250)) {
                    Toast.makeText(this@ProfileActivity, "Height must be between 50–250 cm", Toast.LENGTH_SHORT).show()
                    return ""
                }
                return null
            }
        }
        etHeight.filters = arrayOf(heightFilter)

        // Weight (10–300 kg)
        val weightFilter = object : InputFilter {
            override fun filter(
                source: CharSequence?, start: Int, end: Int,
                dest: Spanned?, dstart: Int, dend: Int
            ): CharSequence? {
                if (source != null && !source.matches(Regex("[0-9]*"))) {
                    Toast.makeText(this@ProfileActivity, "Invalid: only numbers allowed in Weight", Toast.LENGTH_SHORT).show()
                    return ""
                }
                val newText = (dest?.substring(0, dstart) ?: "") +
                        source?.subSequence(start, end) +
                        (dest?.substring(dend, dest.length) ?: "")
                val value = newText.toIntOrNull()
                if (value != null && (value < 10 || value > 300)) {
                    Toast.makeText(this@ProfileActivity, "Weight must be between 10–300 kg", Toast.LENGTH_SHORT).show()
                    return ""
                }
                return null
            }
        }
        etWeight.filters = arrayOf(weightFilter)

        // 🔹 Change password popup with validation
        btnChangePassword.setOnClickListener {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null)
            val etNewPassword = dialogView.findViewById<EditText>(R.id.etNewPassword)
            val etConfirmPassword = dialogView.findViewById<EditText>(R.id.etConfirmPassword)

            val dialog = AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setView(dialogView)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create()

            dialog.setOnShowListener {
                val saveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                saveBtn.setOnClickListener {
                    val newPass = etNewPassword.text.toString().trim()
                    val confirmPass = etConfirmPassword.text.toString().trim()

                    if (newPass.isEmpty() || confirmPass.isEmpty()) {
                        Toast.makeText(this, "Please fill both fields", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    if (newPass != confirmPass) {
                        Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    val regex = Regex("^(?=.*[0-9])(?=.*[A-Z])(?=.*[!@#\$%^&*()_+=-]).{8,}\$")
                    if (!regex.matches(newPass)) {
                        Toast.makeText(this,
                            "Password must be 8+ chars, 1 capital, 1 number, 1 special char",
                            Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }
                    prefs.edit().putString("password", newPass).apply()
                    tvPassword.text = newPass
                    Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
            dialog.show()
        }

        // 🔹 BMI calculation button
        btnCalculateBMI.setOnClickListener {
            val gender = etGender.text.toString().trim()
            val ageStr = etAge.text.toString().trim()
            val heightStr = etHeight.text.toString().trim()
            val weightStr = etWeight.text.toString().trim()

            if (gender.isEmpty() || ageStr.isEmpty() || heightStr.isEmpty() || weightStr.isEmpty()) {
                Toast.makeText(this, "Please fill all fields first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val age = ageStr.toIntOrNull()
            val height = heightStr.toFloatOrNull()
            val weight = weightStr.toFloatOrNull()

            if (age == null || age <= 0 || age > 100) {
                Toast.makeText(this, "Invalid age", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (height == null || height < 50 || height > 250) {
                Toast.makeText(this, "Invalid height", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (weight == null || weight < 10 || weight > 300) {
                Toast.makeText(this, "Invalid weight", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.edit()
                .putString("gender", gender)
                .putString("age", ageStr)
                .putString("height", heightStr)
                .putString("weight", weightStr)
                .apply()

            val bmi = weight / (height / 100).pow(2)
            val message = when {
                bmi < 18.5 -> "Your BMI is %.1f\nYou are underweight.".format(bmi)
                bmi in 18.5..24.9 -> "Your BMI is %.1f\nYou are in the normal range.".format(bmi)
                bmi in 25.0..29.9 -> "Your BMI is %.1f\nYou are overweight.".format(bmi)
                else -> "Your BMI is %.1f\nYou are obese.".format(bmi)
            }

            AlertDialog.Builder(this)
                .setTitle("BMI Result")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
        }

        // 🔹 Back
        btnBackHome.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }
}
