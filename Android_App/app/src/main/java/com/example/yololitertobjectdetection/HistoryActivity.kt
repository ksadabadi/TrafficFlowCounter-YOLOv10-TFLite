package com.example.yololitertobjectdetection

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class HistoryActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var fileList: List<File>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        listView = findViewById(R.id.listViewFiles)

        loadFiles()
    }

    private fun loadFiles() {
        val dir = getExternalFilesDir(null)
        if (dir != null) {
            fileList = dir.listFiles()?.toList() ?: emptyList()
            val fileNames = fileList.map { it.name }

            if (fileNames.isEmpty()) {
                Toast.makeText(this, "No saved files found", Toast.LENGTH_SHORT).show()
            }

            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, fileNames)
            listView.adapter = adapter

            listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                val selectedFile = fileList[position]
                val intent = Intent(this, FileViewerActivity::class.java)
                intent.putExtra("FILE_PATH", selectedFile.absolutePath)
                startActivity(intent)
            }
        }
    }
}
