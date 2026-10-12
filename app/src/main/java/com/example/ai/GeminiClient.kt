package com.example.ai

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiModels {
    const val FLASH = "gemini-3.5-flash"
    const val PRO = "gemini-3.1-pro-preview"
    const val FLASH_LITE = "gemini-3.1-flash-lite-preview"

    val AVAILABLE_MODELS = listOf(FLASH, PRO, FLASH_LITE)

    fun getDisplayName(model: String): String = when (model) {
        FLASH -> "Gemini 3.5 Flash (Balanced & Fast)"
        PRO -> "Gemini 3.1 Pro (Deep Reasoning)"
        FLASH_LITE -> "Gemini 3.1 Flash Lite (Ultra Fast)"
        else -> model
    }
}

data class ExtractedNoteData(
    val category: String,
    val title: String,
    val content: String,
    val tags: List<String>,
    val colorHex: String,
    val structuredFields: Map<String, String>
)

class GeminiClient(private val context: android.content.Context? = null) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun getEffectiveApiKey(overrideKey: String? = null): String {
        if (!overrideKey.isNullOrBlank()) return overrideKey.trim()
        if (context != null) {
            val key = ApiKeyManager.getActiveApiKey(context)
            if (key.isNotBlank()) return key
        }
        val buildKey = BuildConfig.GEMINI_API_KEY
        if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") {
            return buildKey
        }
        return ""
    }

    fun isApiKeyAvailable(): Boolean {
        return getEffectiveApiKey().isNotBlank()
    }

    suspend fun testConnection(testKey: String? = null): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey(testKey)
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("No API key provided."))
        }
        try {
            val root = JSONObject()
            val contentsArr = JSONArray()
            val turn = JSONObject()
            turn.put("role", "user")
            val parts = JSONArray()
            val part = JSONObject()
            part.put("text", "Respond with 'Connection successful' only.")
            parts.put(part)
            turn.put("parts", parts)
            contentsArr.put(turn)
            root.put("contents", contentsArr)

            val url = "https://generativelanguage.googleapis.com/v1beta/models/${GeminiModels.FLASH}:generateContent?key=$apiKey"
            val requestBody = root.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder().url(url).post(requestBody).build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMsg = try {
                        JSONObject(body).optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                    } catch (_: Exception) {
                        "HTTP ${response.code}"
                    }
                    return@withContext Result.failure(Exception(errorMsg))
                }
                Result.success("Connection verified successfully with Gemini!")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateChatResponse(
        prompt: String,
        model: String = GeminiModels.FLASH,
        imageBitmap: Bitmap? = null,
        history: List<Pair<String, String>> = emptyList() // List of (role, content)
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("GEMINI_API_KEY is not configured. Add your key in Settings or AI Studio Secrets.")
            )
        }

        try {
            val root = JSONObject()
            val contentsArr = JSONArray()

            // Append limited conversation history
            for ((role, text) in history.takeLast(6)) {
                val turnObj = JSONObject()
                turnObj.put("role", if (role == "model") "model" else "user")
                val parts = JSONArray()
                val partObj = JSONObject()
                partObj.put("text", text)
                parts.put(partObj)
                turnObj.put("parts", parts)
                contentsArr.put(turnObj)
            }

            // Current message
            val currentTurn = JSONObject()
            currentTurn.put("role", "user")
            val currentParts = JSONArray()

            val textPart = JSONObject()
            textPart.put("text", prompt)
            currentParts.put(textPart)

            if (imageBitmap != null) {
                val inlineData = JSONObject()
                inlineData.put("mimeType", "image/jpeg")
                inlineData.put("data", bitmapToBase64(imageBitmap))
                val imagePart = JSONObject()
                imagePart.put("inlineData", inlineData)
                currentParts.put(imagePart)
            }
            currentTurn.put("parts", currentParts)
            contentsArr.put(currentTurn)

            root.put("contents", contentsArr)

            // System instructions
            val systemInstruction = JSONObject()
            val sysParts = JSONArray()
            val sysTextPart = JSONObject()
            sysTextPart.put(
                "text",
                "You are an intelligent workspace and notes assistant in StickyDesk AI. " +
                        "Provide helpful, concise, and structured answers. You can format responses with markdown headings, bullet points, and code blocks."
            )
            sysParts.put(sysTextPart)
            systemInstruction.put("parts", sysParts)
            root.put("systemInstruction", systemInstruction)

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val requestBody = root.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMsg = try {
                        val errObj = JSONObject(body).optJSONObject("error")
                        errObj?.optString("message") ?: "API Error HTTP ${response.code}"
                    } catch (_: Exception) {
                        "API Error HTTP ${response.code}"
                    }
                    return@withContext Result.failure(Exception(errorMsg))
                }

                val resJson = JSONObject(body)
                val candidates = resJson.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val contentObj = firstCandidate?.optJSONObject("content")
                val parts = contentObj?.optJSONArray("parts")
                val responseText = parts?.optJSONObject(0)?.optString("text")

                if (!responseText.isNullOrBlank()) {
                    Result.success(responseText)
                } else {
                    Result.failure(Exception("Empty response received from Gemini."))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Extracts structured fields for a note from conversation text or user notes.
     */
    suspend fun extractStructuredNote(
        rawText: String,
        preferredCategory: String = "Auto-detect",
        model: String = GeminiModels.FLASH
    ): Result<ExtractedNoteData> = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank()) {
            // Local fallback extraction if API key is missing
            return@withContext Result.success(fallbackLocalExtraction(rawText, preferredCategory))
        }

        val prompt = """
You are a note-organizing AI. Given the following user text or conversation snippet, extract and organize it into a structured note.

Categories:
1. "Travel": destination, location/place, budget, short description, travel dates if known, useful details.
2. "Projects": project name, current state, what needs to be done, milestones, next actions.
3. "Tech": title, description, source URL or source name if available, special notes.
4. "Clinical": title, concise summary, important mechanisms, diagnostic points, management, source/reference when available. (Educational study notes only, do not invent clinical facts).
5. "Other": universal title, summary, general content, tags.

Target category: ${if (preferredCategory == "Auto-detect") "Choose the best matching category from Travel, Projects, Tech, Clinical, Other" else preferredCategory}.

Extract place names, budgets, and specific details ONLY when mentioned in the text. Do NOT invent missing details.

Output STRICTLY valid JSON with the following structure:
{
  "category": "Travel" | "Projects" | "Tech" | "Clinical" | "Other",
  "title": "Clear concise note title",
  "content": "Formatted note content with bullet points",
  "tags": ["tag1", "tag2"],
  "colorHex": "#FFF9C4" (Yellow for Projects/Other) or "#E1F5FE" (Blue for Tech) or "#FCE4EC" (Pink for Clinical) or "#E8F5E9" (Mint for Travel) or "#FFE0B2" (Orange),
  "fields": {
    "key1": "value1",
    "key2": "value2"
  }
}

Input text:
$rawText
""".trimIndent()

        try {
            val root = JSONObject()
            val contentsArr = JSONArray()
            val turn = JSONObject()
            turn.put("role", "user")
            val parts = JSONArray()
            val textPart = JSONObject()
            textPart.put("text", prompt)
            parts.put(textPart)
            turn.put("parts", parts)
            contentsArr.put(turn)
            root.put("contents", contentsArr)

            // Generation config with JSON response type
            val genConfig = JSONObject()
            genConfig.put("responseMimeType", "application/json")
            root.put("generationConfig", genConfig)

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val requestBody = root.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.success(fallbackLocalExtraction(rawText, preferredCategory))
                }

                val resJson = JSONObject(body)
                val candidates = resJson.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val contentObj = firstCandidate?.optJSONObject("content")
                val textParts = contentObj?.optJSONArray("parts")
                val jsonString = textParts?.optJSONObject(0)?.optString("text") ?: ""

                val parsed = JSONObject(jsonString)
                val category = parsed.optString("category", if (preferredCategory != "Auto-detect") preferredCategory else "Other")
                val title = parsed.optString("title", "Untitled Note")
                val content = parsed.optString("content", rawText)
                val tagsArr = parsed.optJSONArray("tags")
                val tags = mutableListOf<String>()
                if (tagsArr != null) {
                    for (i in 0 until tagsArr.length()) {
                        tags.add(tagsArr.getString(i))
                    }
                }
                val colorHex = parsed.optString("colorHex", getColorForCategory(category))
                val fieldsObj = parsed.optJSONObject("fields")
                val fieldsMap = mutableMapOf<String, String>()
                if (fieldsObj != null) {
                    val keys = fieldsObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        fieldsMap[k] = fieldsObj.optString(k, "")
                    }
                }

                Result.success(
                    ExtractedNoteData(
                        category = category,
                        title = title,
                        content = content,
                        tags = tags,
                        colorHex = colorHex,
                        structuredFields = fieldsMap
                    )
                )
            }
        } catch (_: Exception) {
            Result.success(fallbackLocalExtraction(rawText, preferredCategory))
        }
    }

    private fun fallbackLocalExtraction(text: String, preferredCategory: String): ExtractedNoteData {
        val lines = text.trim().lines().filter { it.isNotBlank() }
        val title = lines.firstOrNull()?.take(40)?.replace(Regex("^#+\\s*"), "") ?: "Quick Note"
        val content = if (lines.size > 1) lines.drop(1).joinToString("\n") else text

        val category = if (preferredCategory != "Auto-detect") preferredCategory else {
            val lower = text.lowercase()
            when {
                lower.contains("flight") || lower.contains("hotel") || lower.contains("trip") || lower.contains("destination") -> "Travel"
                lower.contains("roadmap") || lower.contains("sprint") || lower.contains("feature") || lower.contains("milestone") -> "Projects"
                lower.contains("code") || lower.contains("api") || lower.contains("database") || lower.contains("bug") || lower.contains("http") -> "Tech"
                lower.contains("patient") || lower.contains("diagnosis") || lower.contains("symptom") || lower.contains("treatment") -> "Clinical"
                else -> "Other"
            }
        }

        val tags = listOf(category.lowercase(), "extracted")
        val colorHex = getColorForCategory(category)
        val fields = mutableMapOf<String, String>()

        when (category) {
            "Travel" -> {
                fields["destination"] = title
                fields["shortDescription"] = content
            }
            "Projects" -> {
                fields["projectName"] = title
                fields["whatNeedsToBeDone"] = content
            }
            "Tech" -> {
                fields["title"] = title
                fields["description"] = content
            }
            "Clinical" -> {
                fields["title"] = title
                fields["conciseSummary"] = content
            }
            else -> {
                fields["summary"] = content
            }
        }

        return ExtractedNoteData(
            category = category,
            title = title,
            content = content,
            tags = tags,
            colorHex = colorHex,
            structuredFields = fields
        )
    }

    private fun getColorForCategory(category: String): String = when (category) {
        "Projects" -> "#FFF9C4" // Pastel Yellow
        "Travel" -> "#E8F5E9"   // Pastel Mint
        "Tech" -> "#E1F5FE"     // Pastel Blue
        "Clinical" -> "#FCE4EC" // Pastel Pink
        else -> "#FFE0B2"       // Pastel Orange
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        val byteArray = stream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
