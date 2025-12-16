package com.example.ainotemaster

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

class MainActivity : AppCompatActivity() {

    private lateinit var btnChoosePhoto: Button
    private lateinit var btnEnterText: Button
    private lateinit var btnHistory: Button
    private lateinit var btnChoosePdf: Button

    // ✅ 갤러리 런처
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                @Suppress("DEPRECATION")
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                runTextRecognition(bitmap)
            }
        }

    // ✅ 갤러리 권한 (Android 13+ / 이하)
    private fun galleryPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    // ✅ 권한 요청 런처 (갤러리만 처리)
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val allGranted = result.values.all { it }
            if (allGranted) {
                openGallery()
            } else {
                Toast.makeText(this, "권한을 허용해야 기능을 사용할 수 있어요.", Toast.LENGTH_SHORT).show()
            }
        }

    private fun hasAll(perms: Array<String>): Boolean =
        perms.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

    private fun checkAndOpenGallery() {
        val perms = galleryPermissions()
        if (hasAll(perms)) openGallery()
        else permissionLauncher.launch(perms)
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun extractPdfToTextAndGo(uri: Uri) {
        Thread {
            try {
                // 1) 텍스트 PDF면: PDFBox로 텍스트 추출
                val extracted = extractTextFromPdf(uri).trim()

                if (extracted.isNotBlank()) {
                    runOnUiThread { goToTextInput(extracted) }
                    return@Thread
                }

                // 2) 스캔 PDF면: PdfRenderer로 페이지를 Bitmap으로 → MLKit OCR
                val ocrText = ocrPdfByRendering(uri, maxPages = 5).trim()

                runOnUiThread {
                    if (ocrText.isBlank()) {
                        Toast.makeText(this, "PDF에서 텍스트를 추출하지 못했어요. (스캔 품질 확인)", Toast.LENGTH_LONG).show()
                    } else {
                        goToTextInput(ocrText)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "PDF 처리 실패: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    // ✅ (A) 텍스트 PDF 추출
    private fun extractTextFromPdf(uri: Uri): String {
        contentResolver.openInputStream(uri).use { input ->
            if (input == null) return ""
            val doc = PDDocument.load(input)
            doc.use {
                val stripper = PDFTextStripper()
                return stripper.getText(it) ?: ""
            }
        }
    }

    // ✅ (B) 스캔 PDF용: 페이지 렌더링 → OCR (최대 maxPages페이지만)
    private fun ocrPdfByRendering(uri: Uri, maxPages: Int): String {
        val pfd: ParcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r") ?: return ""
        PdfRenderer(pfd).use { renderer ->
            val pageCount = minOf(renderer.pageCount, maxPages)
            val sb = StringBuilder()

            // MLKit recognizer
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            for (i in 0 until pageCount) {
                renderer.openPage(i).use { page ->
                    val scale = 2 // 해상도(너무 크면 느려짐) 2~3 추천
                    val bitmap = Bitmap.createBitmap(
                        page.width * scale,
                        page.height * scale,
                        Bitmap.Config.ARGB_8888
                    )
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    val image = InputImage.fromBitmap(bitmap, 0)

                    // MLKit은 비동기라 여기서는 기다려야 함 → Task를 blocking으로 처리
                    val task = recognizer.process(image)
                    val visionText = com.google.android.gms.tasks.Tasks.await(task)
                    sb.appendLine(visionText.text)
                    sb.appendLine()
                }
            }
            return sb.toString()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // XML 연결

        btnChoosePhoto = findViewById(R.id.btnChoosePhoto)
        btnEnterText = findViewById(R.id.btnEnterText)
        btnHistory = findViewById(R.id.btnHistory)
        btnChoosePdf = findViewById(R.id.btnChoosePdf)
        btnChoosePdf.setOnClickListener { pdfLauncher.launch(arrayOf("application/pdf")) }

        // ✅ 텍스트 직접 입력
        btnEnterText.setOnClickListener {
            startActivity(Intent(this, TextInputActivity::class.java))
        }

        // ✅ 히스토리
        btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        // ✅ 갤러리에서 사진 불러오기
        btnChoosePhoto.setOnClickListener { checkAndOpenGallery() }

        btnChoosePdf.setOnClickListener {
            pdfLauncher.launch(arrayOf("application/pdf"))
        }

    }

    private fun runTextRecognition(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val rawText = visionText.text.trim()
                if (rawText.length < 20) {
                    Toast.makeText(this, "글자를 인식하지 못했어요. 더 크게/밝게 찍어주세요.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
                goToTextInput(rawText)

            }

    }

    private val pdfLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                // 앱 재실행 후에도 접근 가능하게 권한 유지(추천)
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                // PDF → 텍스트 추출(안 되면 OCR) → TextInputActivity로 넘김
                extractPdfToTextAndGo(uri)
            }
        }


    private fun goToTextInput(ocrText: String) {
        val intent = Intent(this, TextInputActivity::class.java)
        intent.putExtra("ocr_text", ocrText)
        startActivity(intent)
    }
}

object OpenAIClient {

    private const val PROXY_URL = "https://ai-note-worker.ai-note-worker.workers.dev"
    private const val APP_TOKEN = "ainote2025"

    data class AIResult(val summary: String, val questions: String, val answers: String)

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    fun createQuestions(text: String): AIResult {
        val bodyJson = JSONObject().put("text", text).toString()
        val body = bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType())

        val req = Request.Builder()
            .url(PROXY_URL)
            .header("Authorization", "Bearer ${APP_TOKEN.trim()}")
            .header("Content-Type", "application/json")
            .post(body)
            .build()

        val respStr = client.newCall(req).execute().use { resp ->
            val s = resp.body?.string().orEmpty()
            android.util.Log.e("PROXY", "HTTP ${resp.code} body=$s")
            if (!resp.isSuccessful) throw RuntimeException("Proxy error: HTTP ${resp.code}\n$s")
            s
        }

        val obj = JSONObject(respStr)
        val summary = obj.optString("summary", "")

        val qArr = obj.optJSONArray("questions")
        val aArr = obj.optJSONArray("answers")

        val questions = buildString {
            if (qArr != null) for (i in 0 until qArr.length()) append("${i + 1}. ${qArr.getString(i)}\n")
        }.trim()

        val answers = buildString {
            if (aArr != null) for (i in 0 until aArr.length()) append("${i + 1}. ${aArr.getString(i)}\n")
        }.trim()

        return AIResult(summary, questions, answers)
    }
}
