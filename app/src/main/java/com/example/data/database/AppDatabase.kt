package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import com.example.marketplace.persistence.ExtensionExecutionDao
import com.example.marketplace.persistence.ExtensionExecutionRecordEntity
import com.example.marketplace.persistence.InstalledExtensionDao
import com.example.marketplace.persistence.InstalledExtensionEntity
import com.example.marketplace.persistence.MarketplaceDao
import com.example.marketplace.persistence.MarketplaceEntity

import com.example.mcp.persistence.McpDao
import com.example.mcp.persistence.McpLogEntity
import com.example.mcp.persistence.McpServerEntity
import com.example.mcp.persistence.McpToolEntity
import com.example.hooks.persistence.HookDao
import com.example.hooks.persistence.HookLogEntity
import com.example.hooks.persistence.HookRuleEntity

/**
 * Main Room Database for Claude Code, managing persistent chat messages,
 * project memories, installed marketplace plugins, MCP server configurations,
 * and automated lifecycle hooks.
 */
@Database(
  entities = [
    ChatMessageEntity::class,
    ProjectMemoryEntity::class,
    MarketplaceEntity::class,
    InstalledExtensionEntity::class,
    ExtensionExecutionRecordEntity::class,
    McpServerEntity::class,
    McpToolEntity::class,
    McpLogEntity::class,
    HookRuleEntity::class,
    HookLogEntity::class
  ],
  version = 4,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

  abstract fun chatMessageDao(): ChatMessageDao
  abstract fun projectMemoryDao(): ProjectMemoryDao
  abstract fun marketplaceDao(): MarketplaceDao
  abstract fun installedExtensionDao(): InstalledExtensionDao
  abstract fun extensionExecutionDao(): ExtensionExecutionDao
  abstract fun mcpDao(): McpDao
  abstract fun hookDao(): HookDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getInstance(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "claude_code_chat.db"
        )
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
