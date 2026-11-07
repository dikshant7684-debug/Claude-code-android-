package com.example.mcp.persistence

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "mcp_servers")
data class McpServerEntity(
  @PrimaryKey val id: String,
  val name: String,
  val url: String,
  val transportType: String,
  val authType: String,
  val encryptedAuthToken: String = "",
  val customHeadersJson: String = "{}",
  val envJson: String = "{}",
  val timeoutSeconds: Int = 30,
  val description: String = "",
  val isEnabled: Boolean = true,
  val isApprovedByUser: Boolean = false,
  val approvedPermissionsJson: String = "[\"execute_tools\"]",
  val lastConnectedAt: Long = 0L
)

@Entity(tableName = "mcp_tools")
data class McpToolEntity(
  @PrimaryKey val id: String,
  val serverId: String,
  val serverName: String,
  val name: String,
  val description: String,
  val inputSchemaJson: String = "{}",
  val requiredPermissionsJson: String = "[\"execute_tools\"]",
  val isEnabled: Boolean = true,
  val executionCount: Int = 0
)

@Entity(tableName = "mcp_logs")
data class McpLogEntity(
  @PrimaryKey val id: String,
  val timestamp: Long,
  val serverId: String,
  val serverName: String,
  val toolName: String?,
  val agentId: String?,
  val eventType: String,
  val message: String,
  val durationMs: Long = 0L,
  val isError: Boolean = false
)

@Dao
interface McpDao {
  @Query("SELECT * FROM mcp_servers ORDER BY name ASC")
  fun getAllServersFlow(): Flow<List<McpServerEntity>>

  @Query("SELECT * FROM mcp_servers ORDER BY name ASC")
  suspend fun getAllServers(): List<McpServerEntity>

  @Query("SELECT * FROM mcp_servers WHERE id = :id LIMIT 1")
  suspend fun getServerById(id: String): McpServerEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdateServer(server: McpServerEntity)

  @Query("DELETE FROM mcp_servers WHERE id = :id")
  suspend fun deleteServer(id: String)

  @Query("SELECT * FROM mcp_tools WHERE serverId = :serverId")
  suspend fun getToolsForServer(serverId: String): List<McpToolEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTools(tools: List<McpToolEntity>)

  @Query("DELETE FROM mcp_tools WHERE serverId = :serverId")
  suspend fun deleteToolsForServer(serverId: String)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertLog(log: McpLogEntity)

  @Query("SELECT * FROM mcp_logs ORDER BY timestamp DESC LIMIT 200")
  fun getRecentLogsFlow(): Flow<List<McpLogEntity>>
}
