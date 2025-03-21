package com.example.yololitertobjectdetection

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val historyButton = findViewById<ImageButton>(R.id.btnHistory)
        val countObjectsButton = findViewById<ImageButton>(R.id.btnCountObjects)

        historyButton.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }

        countObjectsButton.setOnClickListener {
            val intent = Intent(this, CountOptionsActivity::class.java)
            startActivity(intent)
        }
    }
}
