package com.numina.data.repositories

import com.numina.data.tables.SystemSettings
import com.numina.domain.SettingType
import com.numina.domain.SystemSetting
import kotlinx.datetime.toKotlinInstant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

interface SystemSettingsRepository {
    suspend fun createOrUpdateSetting(key: String, value: String, type: SettingType, description: String?): SystemSetting
    suspend fun getSetting(key: String): SystemSetting?
    suspend fun getAllSettings(): List<SystemSetting>
    suspend fun deleteSetting(key: String): Boolean
}

class SystemSettingsRepositoryImpl : SystemSettingsRepository {
    override suspend fun createOrUpdateSetting(key: String, value: String, type: SettingType, description: String?): SystemSetting = transaction {
        val existing = SystemSettings.select { SystemSettings.key eq key }.singleOrNull()
        val now = kotlinx.datetime.Clock.System.now().toJavaInstant()

        if (existing != null) {
            SystemSettings.update({ SystemSettings.key eq key }) {
                it[SystemSettings.value] = value
                it[SystemSettings.type] = type.name
                it[SystemSettings.description] = description
                it[updatedAt] = now
            }
        } else {
            SystemSettings.insert {
                it[SystemSettings.key] = key
                it[SystemSettings.value] = value
                it[SystemSettings.type] = type.name
                it[SystemSettings.description] = description
                it[updatedAt] = now
            }
        }

        getSetting(key)!!
    }

    override suspend fun getSetting(key: String): SystemSetting? = transaction {
        SystemSettings.select { SystemSettings.key eq key }
            .map { rowToSystemSetting(it) }
            .singleOrNull()
    }

    override suspend fun getAllSettings(): List<SystemSetting> = transaction {
        SystemSettings.selectAll().map { rowToSystemSetting(it) }
    }

    override suspend fun deleteSetting(key: String): Boolean = transaction {
        SystemSettings.deleteWhere { SystemSettings.key eq key } > 0
    }

    private fun rowToSystemSetting(row: ResultRow): SystemSetting {
        return SystemSetting(
            key = row[SystemSettings.key],
            value = row[SystemSettings.value],
            type = SettingType.valueOf(row[SystemSettings.type]),
            description = row[SystemSettings.description],
            updatedAt = row[SystemSettings.updatedAt].toKotlinInstant()
        )
    }
}
