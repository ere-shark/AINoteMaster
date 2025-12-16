package com.example.ainotemaster

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.ainotemaster.db.AppDatabase
import kotlin.concurrent.thread

class HistoryActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout
    private lateinit var btnBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        container = findViewById(R.id.containerHistory)
        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener { finish() }

        loadNotes()
    }

    override fun onResume() {
        super.onResume()
        // ✅ 상세에서 삭제 후 돌아오면 여기서 다시 로드되어 목록이 즉시 갱신됨
        loadNotes()
    }

    private fun loadNotes() {
        thread {
            val noteList = AppDatabase.getInstance(this)
                .noteDao()
                .getAllOrderByDateDesc()   // ✅ DAO에 있는 함수로 변경!

            runOnUiThread {
                container.removeAllViews()

                if (noteList.isEmpty()) {
                    val tvEmpty = TextView(this).apply {
                        text = "저장된 기록이 없습니다."
                        textSize = 18f
                        setPadding(0, 40, 0, 0)
                    }
                    container.addView(tvEmpty)
                    return@runOnUiThread
                }

                noteList.forEach { note ->
                    val tv = TextView(this).apply {
                        text = "📌 ${note.title}"
                        textSize = 16f
                        setPadding(0, 16, 0, 16)
                        setOnClickListener {
                            val i = Intent(this@HistoryActivity, NoteDetailActivity::class.java)
                            i.putExtra("note_id", note.id) // Long 그대로
                            startActivity(i)
                        }
                    }
                    container.addView(tv)
                }
            }
        }
    }
}
