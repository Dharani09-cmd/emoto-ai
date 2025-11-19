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

    // Room DB
    private val moodDao = MoodDatabase.getDatabase(application).moodDao()
    private val _moodList = MutableLiveData<List<Mood>>()
    val moodList: LiveData<List<Mood>> = _moodList

    // Retrofit service using Render backend
    private val aiService: BackendService

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://emoto-backend.onrender.com/")   // IMPORTANT
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        aiService = retrofit.create(BackendService::class.java)

        loadMoods()
    }

    fun sendMessage(message: String) {
        _chatMessages.value?.add(ChatMessage(message, true))
        _chatMessages.postValue(_chatMessages.value)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = aiService.sendToAI(ChatRequest(message))
                val reply = response.reply.trim()

                _chatMessages.postValue(
                    _chatMessages.value?.apply {
                        add(ChatMessage(reply, false))
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

// Retrofit interface for your Render backend
interface BackendService {
    @Headers("Content-Type: application/json")
    @POST("chat")   // Render endpoint
    suspend fun sendToAI(@Body request: ChatRequest): ChatResponse
}

// Request sent to your node backend
data class ChatRequest(
    val message: String
)

// Response returned by your node backend
data class ChatResponse(
    val reply: String
)
