package com.agent.voice.net

import com.agent.voice.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class ChatRequest(val conversationId: String, val message: String)

@Serializable
private data class ChatResponse(val reply: String)

class BackendClient {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun chat(conversationId: String, message: String): String {
        val response: ChatResponse = client.post("${BuildConfig.BACKEND_URL}/api/chat") {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${BuildConfig.DEVICE_TOKEN}")
            }
            contentType(ContentType.Application.Json)
            setBody(ChatRequest(conversationId, message))
        }.body()
        return response.reply
    }
}
