package com.example.data.database

import com.example.data.model.UiChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository abstracting Room Database data access for conversational chat messages.
 */
class ChatRepository(
  private val chatMessageDao: ChatMessageDao
) {

  val messagesFlow: Flow<List<UiChatMessage>> = chatMessageDao.getAllMessagesFlow()
    .map { entities ->
      entities
        .filter { it.content.isNotBlank() || it.role == "USER" }
        .distinctBy { it.id }
        .map { it.toUiModel() }
    }

  suspend fun purgeEmptyMessages() = withContext(Dispatchers.IO) {
    try {
      chatMessageDao.purgeEmptyMessages()
    } catch (_: Exception) {}
  }

  suspend fun getAllMessages(): List<UiChatMessage> = withContext(Dispatchers.IO) {
    chatMessageDao.getAllMessages()
      .filter { it.content.isNotBlank() || it.role == "USER" }
      .distinctBy { it.id }
      .map { it.toUiModel() }
  }

  suspend fun saveMessage(message: UiChatMessage) = withContext(Dispatchers.IO) {
    // Never persist empty assistant messages
    if (message.role == com.example.data.model.MessageRole.ASSISTANT && message.content.isBlank()) {
      return@withContext
    }
    chatMessageDao.insertMessage(ChatMessageEntity.fromUiModel(message))
  }

  suspend fun saveMessages(messages: List<UiChatMessage>) = withContext(Dispatchers.IO) {
    val valid = messages.filter { !(it.role == com.example.data.model.MessageRole.ASSISTANT && it.content.isBlank()) }
    if (valid.isNotEmpty()) {
      chatMessageDao.insertMessages(valid.distinctBy { it.id }.map { ChatMessageEntity.fromUiModel(it) })
    }
  }

  suspend fun deleteMessage(id: String) = withContext(Dispatchers.IO) {
    chatMessageDao.deleteMessageById(id)
  }

  suspend fun clearAllMessages() = withContext(Dispatchers.IO) {
    chatMessageDao.clearAllMessages()
  }
}
