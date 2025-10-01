package com.example.vitalize

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationSet
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_screen)

        // ✅ Make splash fullscreen (hide status + navigation bar)
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

        // ✅ Hide action bar (title bar)
        supportActionBar?.hide()

        val logo = findViewById<ImageView>(R.id.logoImage)

        // Load animations
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val scaleUp = AnimationUtils.loadAnimation(this, R.anim.scale_up)

        // Combine both animations
        val animationSet = AnimationSet(true).apply {
            addAnimation(fadeIn)
            addAnimation(scaleUp)
        }

        // Start animation on logo
        logo.startAnimation(animationSet)

        // Delay 2.5s then move to SignupActivity (or LoginActivity)
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, SignupActivity::class.java))
            finish()
        }, 3000)
    }
}
