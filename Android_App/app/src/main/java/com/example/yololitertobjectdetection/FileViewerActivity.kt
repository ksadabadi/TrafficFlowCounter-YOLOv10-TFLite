package com.example.yololitertobjectdetection

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class FileViewerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_viewer)

        val textView = findViewById<TextView>(R.id.textViewFileContent)
        val filePath = intent.getStringExtra("FILE_PATH")

        if (filePath != null) {
            val file = File(filePath)
            if (file.exists()) {
                textView.text = file.readText()
            } else {
                textView.text = "Error: File not found!"
            }
        }
    }
}
