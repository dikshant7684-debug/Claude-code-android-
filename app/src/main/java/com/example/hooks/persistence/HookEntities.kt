package com.example.hooks.persistence

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "hook_rules")
data class HookRuleEntity(
  @PrimaryKey val id: String,
  val name: String,
  val description: String = "",
  val eventType: String,
  val filePattern: String = "*",
  val actionType: String,
  val actionConfigJson: String = "{}",
  val isEnabled: Boolean = true,
  val isApprovedByUser: Boolean = true,
  val executionCount: Int = 0,
  val lastExecutedAt: Long = 0L
)

@Entity(tableName = "hook_logs")
data class HookLogEntity(
  @PrimaryKey val id: String,
  val timestamp: Long,
  val ruleId: String,
  val ruleName: String,
  val eventType: String,
  val outputMessage: String,
  val durationMs: Long = 0L,
  val isSuccess: Boolean = true
)

@Dao
interface HookDao {
  @Query("SELECT * FROM hook_rules ORDER BY name ASC")
  fun getAllRulesFlow(): Flow<List<HookRuleEntity>>

  @Query("SELECT * FROM hook_rules ORDER BY name ASC")
  suspend fun getAllRules(): List<HookRuleEntity>

  @Query("SELECT * FROM hook_rules WHERE id = :id LIMIT 1")
  suspend fun getRuleById(id: String): HookRuleEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdateRule(rule: HookRuleEntity)

  @Query("DELETE FROM hook_rules WHERE id = :id")
  suspend fun deleteRule(id: String)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertLog(log: HookLogEntity)

  @Query("SELECT * FROM hook_logs ORDER BY timestamp DESC LIMIT 200")
  fun getRecentLogsFlow(): Flow<List<HookLogEntity>>
}
