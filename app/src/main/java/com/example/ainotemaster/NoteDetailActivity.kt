package com.example.ainotemaster

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.ainotemaster.db.AppDatabase
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class NoteDetailActivity : AppCompatActivity() {

    private val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_detail)

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }


        val noteId = intent.getLongExtra("note_id", -1L)
        if (noteId <= 0) {
            finish()
            return
        }

        val tvTitle = findViewById<TextView>(R.id.tvDetailTitle)
        val tvMeta = findViewById<TextView>(R.id.tvDetailMeta)
        val tvSummary = findViewById<TextView>(R.id.tvDetailSummary)
        val tvQuestions = findViewById<TextView>(R.id.tvDetailQuestions)
        val tvAnswers = findViewById<TextView>(R.id.tvDetailAnswers)

        val btnDelete = findViewById<Button>(R.id.btnDelete)

        thread {
            val dao = AppDatabase.getInstance(this).noteDao()
            val note = dao.getById(noteId)

            runOnUiThread {
                if (note == null) {
                    finish()
                    return@runOnUiThread
                }

                tvTitle.text = note.title
                tvMeta.text = "저장일: ${df.format(Date(note.createdAt))}"
                tvSummary.text = note.summary
                tvQuestions.text = note.questions
                tvAnswers.text = note.answers

                btnDelete.setOnClickListener {
                    AlertDialog.Builder(this)
                        .setTitle("삭제할까요?")
                        .setMessage("이 기록을 삭제하면 되돌릴 수 없어요.")
                        .setNegativeButton("취소", null)
                        .setPositiveButton("삭제") { _, _ ->
                            thread {
                                dao.deleteById(noteId)
                                runOnUiThread { finish() }
                            }
                        }
                        .show()
                }
            }
        }
    }
}
