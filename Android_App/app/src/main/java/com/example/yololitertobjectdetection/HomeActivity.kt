package com.example.yololitertobjectdetection

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.yololitertobjectdetection.AboutActivity

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val historyButton = findViewById<ImageButton>(R.id.btnHistory)
        val countObjectsButton = findViewById<ImageButton>(R.id.btnCountObjects)
        val aboutButton = findViewById<Button>(R.id.btnAbout) // About Button
        val copyrightTextView = findViewById<TextView>(R.id.txtCopyright)

        historyButton.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }

        countObjectsButton.setOnClickListener {
            val intent = Intent(this, CountOptionsActivity::class.java)
            startActivity(intent)
        }

        aboutButton.setOnClickListener {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }

        copyrightTextView.text = "© 2025 Centre of Advanced Transport and Technology. All rights reserved."
    }
}
