package com.example.yololitertobjectdetection

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class CountOptionsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_count_options)

        val btnLeftToRight = findViewById<ImageButton>(R.id.btnLeftToRight)
        val btnRightToLeft = findViewById<ImageButton>(R.id.btnRightToLeft)
        val btnTopToBottom = findViewById<ImageButton>(R.id.btnTopToBottom)
        val btnBottomToTop = findViewById<ImageButton>(R.id.btnBottomToTop)

        // Handle button clicks and pass data to CameraFragment
        btnLeftToRight.setOnClickListener { openCameraFragment("LEFT_TO_RIGHT") }
        btnRightToLeft.setOnClickListener { openCameraFragment("RIGHT_TO_LEFT") }
        btnTopToBottom.setOnClickListener { openCameraFragment("TOP_TO_BOTTOM") }
        btnBottomToTop.setOnClickListener { openCameraFragment("BOTTOM_TO_TOP") }
    }

    // Method to replace the current fragment with CameraFragment
    private fun openCameraFragment(direction: String) {
        // Create a new CameraFragment instance
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("DIRECTION", direction)

        // Start the CameraActivity
        startActivity(intent)
    }
}
