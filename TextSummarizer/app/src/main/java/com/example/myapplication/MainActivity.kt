package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)
    private val httpClient = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val promptInput = findViewById<EditText>(R.id.prompt_input)
        val sendButton = findViewById<Button>(R.id.send_button)
        val outputText = findViewById<TextView>(R.id.output_text)
        val progress = findViewById<ProgressBar>(R.id.progress)

        sendButton.setOnClickListener {
            val prompt = promptInput.text.toString().trim()
            if (prompt.isNotEmpty()) {
                generateText(prompt, outputText, progress, promptInput)
            }
        }

        promptInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendButton.performClick()
                true
            } else false
        }
    }

    private fun generateText(
        prompt: String,
        output: TextView,
        progress: ProgressBar,
        input: EditText
    ) {
        val apiKey = "ADD-GEMINI-KEY"
        if (apiKey.isBlank()) {
            output.text =
                "GENERATIVE_API_KEY is not set. Put it in res/values/strings.xml as generative_api_key"
            return
        }

        progress.visibility = ProgressBar.VISIBLE
        uiScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { callGenerativeApi(prompt, apiKey) }
                output.text = result
                input.setText("")
            } catch (e: Exception) {
                output.text = "Error: ${e.message}"
            } finally {
                progress.visibility = ProgressBar.GONE
            }
        }
    }

    private fun callGenerativeApi(prompt: String, apiKey: String): String {
        val url =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

        val json = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        httpClient.newCall(request).execute().use { resp ->
            val respBody = resp.body?.string() ?: throw Exception("Empty response")
            if (!resp.isSuccessful) throw Exception("HTTP ${resp.code}: $respBody")

            val jsonResp = JSONObject(respBody)

            // try to extract from `candidates` or `output` or nested structure
            if (jsonResp.has("candidates")) {
                val candidates = jsonResp.getJSONArray("candidates")
                if (candidates.length() > 0) {
                    val first = candidates.getJSONObject(0)
                    Log.d("MainActivity", first.toString())
                    if (first.has("content")) {
                        val content = first.getJSONObject("content")
                        val parts = content.getJSONArray("parts")
                        Log.d("MainActivity", parts.toString())
                        return parts.getJSONObject(0).optString("text", "No Response")
                    }
                }
            }



            return jsonResp.toString()


        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        httpClient.dispatcher.executorService.shutdown()
    }
}