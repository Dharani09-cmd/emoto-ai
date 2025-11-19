package com.example.emoto

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

data class ChatMessage(val message: String, val isUser: Boolean)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val _chatMessages = MutableLiveData<MutableList<ChatMessage>>(mutableListOf())
    val chatMessages: LiveData<MutableList<ChatMessage>> = _chatMessages

    // Room DAO
    private val moodDao = MoodDatabase.getDatabase(application).moodDao()
    private val _moodList = MutableLiveData<List<Mood>>()
    val moodList: LiveData<List<Mood>> = _moodList

    // Retrofit
    private val aiService: OpenAIService

    init {
        val retrofit = Retrofit.Builder()
            baseUrl("https://emoto-backend.onrender.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        aiService = retrofit.create(OpenAIService::class.java)

        loadMoods()
    }

    fun sendMessage(message: String) {
        _chatMessages.value?.add(ChatMessage(message, true))
        _chatMessages.postValue(_chatMessages.value)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = aiService.getResponse(
                    OpenAIRequest(
                        model = "gpt-3.5-turbo-instruct",
                        prompt = message,
                        max_tokens = 100
                    )
                )
                val aiReply = response.choices[0].text.trim()

                _chatMessages.postValue(
                    _chatMessages.value?.apply {
                        add(ChatMessage(aiReply, false))
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addMood(emoji: String, description: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val mood = Mood(emoji = emoji, description = description)
            moodDao.insert(mood)
            _moodList.postValue(moodDao.getAllMoods())
        }
    }

    fun loadMoods() {
        viewModelScope.launch(Dispatchers.IO) {
            _moodList.postValue(moodDao.getAllMoods())
        }
    }
}

// Retrofit API Interface
interface OpenAIService {

    @Headers(
        "Content-Type: application/json",
        "Authorization: Bearer YOUR_OPENAI_KEY_HERE"
    )
    @POST("v1/completions")
    suspend fun getResponse(@Body request: OpenAIRequest): OpenAIResponse
}

// API Models
data class OpenAIRequest(
    val model: String,
    val prompt: String,
    val max_tokens: Int
)

data class OpenAIResponse(val choices: List<Choice>)
data class Choice(val text: String)
