package com.example.miniperplexity

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)
    private val SYSTEM_INSTRUCTION = "Act as helpful assistant perform and Follow the instructions give by user strictly. Analyze if tool is required and Use the following tools or list of tools if needed: 1. googleSearch: Search content online via Google Search. If no tool required treat normal text response. If tools is triggered the combine result of user query along with result return from tools"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Firebase will be initialized in the Application class (MyApplication)
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
        val sourcesList = findViewById<RecyclerView>(R.id.sources_list)

        // Horizontal RecyclerView
        sourcesList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Setup paginated list
        val pagedList = findViewById<RecyclerView>(R.id.paged_list)
        val prevBtn = findViewById<Button>(R.id.prev_page_button)
        val nextBtn = findViewById<Button>(R.id.next_page_button)
        val pageIndicator = findViewById<TextView>(R.id.page_indicator)

        val adapter = PaginatedAdapter(pageSize = 10)
        pagedList.layoutManager = LinearLayoutManager(this)
        pagedList.adapter = adapter

        // Sample data: create 37 items to demonstrate pagination
        val sampleItems = List(37) { index -> "Item ${index + 1}" }
        adapter.setItems(sampleItems)
        pageIndicator.text = "Page ${adapter.getCurrentPage()} / ${adapter.getTotalPages()}"

        prevBtn.setOnClickListener {
            val moved = adapter.prevPage()
            if (moved) pageIndicator.text = "Page ${adapter.getCurrentPage()} / ${adapter.getTotalPages()}"
        }

        nextBtn.setOnClickListener {
            val moved = adapter.nextPage()
            if (moved) pageIndicator.text = "Page ${adapter.getCurrentPage()} / ${adapter.getTotalPages()}"
        }

        sendButton.setOnClickListener {
            val prompt = promptInput.text.toString().trim()
            if (prompt.isNotEmpty()) {
                // call firebase-ai sdk with system instruction and user prompt
                generateText(SYSTEM_INSTRUCTION, prompt, outputText, progress, promptInput, sourcesList)

                // Also demonstrate a POST to http://google.com/ with empty JSON body (off main thread)
                uiScope.launch {
                    try {
                        val resp = withContext(Dispatchers.IO) { APiServices.postEmptyJsonForGoogle() }
                        Log.d("MainActivity", "POST to google.com succeeded, length=${resp.length}")
                    } catch (e: Exception) {
                        Log.e("MainActivity", "POST to google.com failed", e)
                    }
                }
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
        systemInstruction: String,
        prompt: String,
        output: TextView,
        progress: ProgressBar,
        input: EditText,
        sourcesList: RecyclerView
    ) {

        // No API key required here because we're using Firebase AI SDK which is configured in Firebase setup

        progress.visibility = ProgressBar.VISIBLE
        uiScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { GeminiAiServices.generateContent(systemInstruction, prompt) }

                // Update UI: text and sources
                output.text = response.textResponse
                input.setText("")

                val adapter = SourceAdapter(response.links)
                sourcesList.adapter = adapter

            } catch (e: Exception) {
                output.text = "Error: ${e.message}"
                Log.e("MainActivity", "generateText error", e)
            } finally {
                progress.visibility = ProgressBar.GONE
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}