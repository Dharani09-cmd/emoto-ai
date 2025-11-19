package com.example.emoto

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface OpenAIService {

    @Headers("Content-Type: application/json")
    @POST("/chat")
    suspend fun getResponse(@Body request: UserMessageRequest): AIReplyResponse
}

data class UserMessageRequest(val message: String)
data class AIReplyResponse(val reply: String)
