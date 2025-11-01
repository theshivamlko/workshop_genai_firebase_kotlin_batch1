package com.example.miniperplexity

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerationConfig
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.TextPart
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig


object GeminiAiServices {
    private const val TAG = "GeminiAiServices"
    private const val MODEL_NAME = "gemini-2.5-flash"

    suspend fun generateContent(
        systemInstruction: String,
        userQuery: String
    ): SearchResponse {
        try {



            val model = Firebase.ai(backend = GenerativeBackend.googleAI())
                .generativeModel(
                    MODEL_NAME,
                    systemInstruction = content { text(systemInstruction) },
                    generationConfig = generationConfig {
                        responseModalities = listOf(ResponseModality.TEXT)
                    },
                    tools = listOf(Tool.googleSearch())

                )


            val userPrompt = content { text(userQuery) }




            val response = model.generateContent(userPrompt)

            val functionCalls = response.functionCalls.toList()
            Log.d(TAG, "generateContent text: ${response.text}")
            Log.d(TAG, "generateContent functionCalls: ${functionCalls.map { it.name }}")

            if (functionCalls.isNotEmpty()) {
                return SearchResponse("No Tool Found")
            } else {
                val groundingMetadata = response.candidates.firstOrNull()?.groundingMetadata
                val links = mutableListOf<WebLink>()
                if (groundingMetadata != null) {
                    val groundingChunks = groundingMetadata.groundingChunks
                    for (chunk in groundingChunks) {
                        val title = chunk.web?.title
                        val url = chunk.web?.uri
                        if (!url.isNullOrBlank()) {
                            links.add(WebLink(title ?: "No Title", url))
                        }
                    }
                }

                return SearchResponse(response.text ?: "", links)
            }

        } catch (e: Exception) {
            Log.e(TAG, "generateContent error", e)
            throw e
        }
    }
}

