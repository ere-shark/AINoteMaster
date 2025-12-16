package com.example.ainotemaster

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ainotemaster.OpenAIClient
import com.example.ainotemaster.R
import com.example.ainotemaster.ResultActivity

class TextInputActivity : AppCompatActivity() {

    private lateinit var etNote: EditText
    private lateinit var btnAnalyze: Button
    private lateinit var btnCancel: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_input)

        etNote = findViewById(R.id.etNote)
        btnAnalyze = findViewById(R.id.btnAnalyze)
        btnCancel = findViewById(R.id.btnCancel)

        btnCancel.setOnClickListener {
            finish()
        }

        // 혹시 OCR에서 텍스트를 넘겨받았다면 setText
        val ocrText = intent.getStringExtra("ocr_text")
        if (!ocrText.isNullOrEmpty()) {
            etNote.setText(ocrText)
        }

        btnAnalyze.setOnClickListener {
            val text = etNote.text.toString()
            if (text.isBlank()) {
                Toast.makeText(this, "텍스트를 입력해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            callAI(text)
        }
    }

    private fun callAI(input: String) {
        Thread {
            try {
                val result = OpenAIClient.createQuestions(input)
                runOnUiThread {
                    val intent = Intent(this, ResultActivity::class.java).apply {
                        putExtra("summary", result.summary)
                        putExtra("questions", result.questions)
                        putExtra("answers", result.answers)
                    }
                    startActivity(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "분석 실패: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
}
