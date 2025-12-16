package com.example.ainotemaster

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.ainotemaster.db.AppDatabase
import com.example.ainotemaster.db.NoteEntity
import kotlin.concurrent.thread

class ResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val tvSummary = findViewById<TextView>(R.id.tvSummary)
        val tvQuestions = findViewById<TextView>(R.id.tvQuestions)
        val tvAnswers = findViewById<TextView>(R.id.tvAnswers)

        val btnShowAnswer = findViewById<Button>(R.id.btnShowAnswer)
        val btnSave = findViewById<Button>(R.id.btnSave)

        val btnGoMain = findViewById<Button>(R.id.btnGoMain)

// ✅ 메인 화면으로 돌아가기
        btnGoMain.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }



        val summary = intent.getStringExtra("summary") ?: ""
        val questionsRaw = intent.getStringExtra("questions") ?: ""
        val answersRaw = intent.getStringExtra("answers") ?: ""

        tvSummary.text = summary

        // 줄 단위로 분리
        val questionList = questionsRaw.split("\n").filter { it.isNotBlank() }
        val answerList = answersRaw.split("\n").filter { it.isNotBlank() }

        val (qEasy, qNormal, qHard) = splitByDifficulty(questionList)
        val (aEasy, aNormal, aHard) = splitByDifficulty(answerList)

        tvQuestions.text =
            formatByDifficulty("쉬움", qEasy) + "\n" +
                    formatByDifficulty("보통", qNormal) + "\n" +
                    formatByDifficulty("어려움", qHard)

        tvAnswers.text =
            formatByDifficulty("쉬움", aEasy) + "\n" +
                    formatByDifficulty("보통", aNormal) + "\n" +
                    formatByDifficulty("어려움", aHard)

        // ✅ 정답 보기 토글
        btnShowAnswer.setOnClickListener {
            val willShow = tvAnswers.visibility != View.VISIBLE
            tvAnswers.visibility = if (willShow) View.VISIBLE else View.GONE
            btnShowAnswer.text = if (willShow) "정답 숨기기" else "정답 보기"
        }

        // ✅ 저장하기 → 제목 입력 팝업 → DB 저장
        btnSave.setOnClickListener {
            showSaveDialog(
                summary = summary,
                questions = tvQuestions.text.toString(),
                answers = tvAnswers.text.toString()
            )
        }
    }

    // ---------------- 저장 관련 ----------------

    private fun showSaveDialog(
        summary: String,
        questions: String,
        answers: String
    ) {
        val input = EditText(this).apply {
            hint = "예: 국어 수행평가 정리"
            setSingleLine(true)
        }

        AlertDialog.Builder(this)
            .setTitle("제목 입력")
            .setView(input)
            .setNegativeButton("취소", null)
            .setPositiveButton("저장") { _, _ ->
                val title = input.text.toString().trim()
                    .ifEmpty { "제목 없음" }

                saveNote(title, summary, questions, answers)
            }
            .show()
    }

    private fun saveNote(
        title: String,
        summary: String,
        questions: String,
        answers: String
    ) {
        thread {
            try {
                val db = AppDatabase.getInstance(this)
                val note = NoteEntity(
                    title = title,
                    summary = summary,
                    questions = questions,
                    answers = answers
                )
                db.noteDao().insert(note)

                runOnUiThread {
                    Toast.makeText(this, "저장 완료!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "저장 실패: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // ---------------- 기존 유틸 함수 ----------------

    private fun formatByDifficulty(title: String, items: List<String>): String {
        val sb = StringBuilder()
        sb.append("[").append(title).append("]\n")

        items.forEachIndexed { index, item ->
            sb.append("${index + 1}. ${item}\n")
        }
        return sb.toString()
    }

    private fun splitByDifficulty(
        list: List<String>
    ): Triple<List<String>, List<String>, List<String>> {

        val easy = mutableListOf<String>()
        val normal = mutableListOf<String>()
        val hard = mutableListOf<String>()

        list.forEach { raw ->
            // "1. ", "1) " 제거
            val line = raw.replace(Regex("^\\s*\\d+[.)]\\s*"), "").trim()

            when {
                line.startsWith("[쉬움]") ->
                    easy.add(line.removePrefix("[쉬움]").trim())
                line.startsWith("[보통]") ->
                    normal.add(line.removePrefix("[보통]").trim())
                line.startsWith("[어려움]") ->
                    hard.add(line.removePrefix("[어려움]").trim())
            }
        }

        return Triple(easy, normal, hard)
    }
}
