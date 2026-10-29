package com.simla.android.sdk.tracker.storage

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.simla.android.sdk.tracker.model.TrackerEvent
import com.simla.android.sdk.tracker.serialization.SdkJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.security.MessageDigest

internal class SQLiteTrackerStorage(
    context: Context,
) : TrackerStorage, IncrementalTrackerStorage {
    private val databaseHelper = DatabaseHelper(context.applicationContext)

    @Volatile
    private var activeScopeHash: String? = null

    override fun activateScope(siteToken: String, userToken: String): Boolean {
        val newScopeHash = hashScope(siteToken, userToken)
        if (activeScopeHash == newScopeHash) {
            return false
        }
        activeScopeHash = newScopeHash
        return true
    }

    override fun getPendingEvents(): String? {
        val scopeHash = activeScopeHash ?: return null
        val events = mutableListOf<String>()
        databaseHelper.readableDatabase.query(
            TABLE_EVENTS,
            arrayOf(COLUMN_PAYLOAD),
            "$COLUMN_SCOPE_HASH = ?",
            arrayOf(scopeHash),
            null,
            null,
            "$COLUMN_POSITION ASC",
        ).use { cursor ->
            while (cursor.moveToNext()) {
                events += cursor.getString(0)
            }
        }
        return events.takeIf { it.isNotEmpty() }?.joinToString(
            prefix = "[",
            postfix = "]",
        )
    }

    override fun setPendingEvents(value: String?) {
        val scopeHash = activeScopeHash ?: return
        val database = databaseHelper.writableDatabase
        if (value.isNullOrBlank()) {
            database.delete(TABLE_EVENTS, "$COLUMN_SCOPE_HASH = ?", arrayOf(scopeHash))
            return
        }

        val events = SdkJson.parseToJsonElement(value).jsonArray
        database.inTransaction {
            val retainedIds = linkedSetOf<String>()
            events.forEachIndexed { position, element ->
                val eventId = element.jsonObject["id"]?.jsonPrimitive?.content
                    ?: return@forEachIndexed
                retainedIds += eventId
                insertWithOnConflict(
                    TABLE_EVENTS,
                    null,
                    ContentValues().apply {
                        put(COLUMN_SCOPE_HASH, scopeHash)
                        put(COLUMN_EVENT_ID, eventId)
                        put(COLUMN_POSITION, position)
                        put(COLUMN_PAYLOAD, element.toString())
                    },
                    SQLiteDatabase.CONFLICT_REPLACE,
                )
            }

            val removedIds = mutableListOf<String>()
            query(
                TABLE_EVENTS,
                arrayOf(COLUMN_EVENT_ID),
                "$COLUMN_SCOPE_HASH = ?",
                arrayOf(scopeHash),
                null,
                null,
                null,
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    cursor.getString(0).takeIf { it !in retainedIds }?.let(removedIds::add)
                }
            }
            removedIds.forEach { eventId ->
                delete(
                    TABLE_EVENTS,
                    "$COLUMN_SCOPE_HASH = ? AND $COLUMN_EVENT_ID = ?",
                    arrayOf(scopeHash, eventId),
                )
            }
        }
    }

    override fun updatePendingEvents(
        upsertedEvents: List<TrackerEvent>,
        removedEventIds: Set<String>,
    ) {
        val scopeHash = activeScopeHash ?: return
        if (upsertedEvents.isEmpty() && removedEventIds.isEmpty()) {
            return
        }

        databaseHelper.writableDatabase.inTransaction {
            removedEventIds.forEach { eventId ->
                delete(
                    TABLE_EVENTS,
                    "$COLUMN_SCOPE_HASH = ? AND $COLUMN_EVENT_ID = ?",
                    arrayOf(scopeHash, eventId),
                )
            }

            var nextPosition = rawQuery(
                "SELECT COALESCE(MAX($COLUMN_POSITION), -1) FROM $TABLE_EVENTS " +
                        "WHERE $COLUMN_SCOPE_HASH = ?",
                arrayOf(scopeHash),
            ).use { cursor ->
                cursor.moveToFirst()
                cursor.getLong(0) + 1
            }

            upsertedEvents.forEach { event ->
                val eventId = event.id ?: return@forEach
                val payload = SdkJson.encodeToString(event)
                val updatedRows = update(
                    TABLE_EVENTS,
                    ContentValues().apply { put(COLUMN_PAYLOAD, payload) },
                    "$COLUMN_SCOPE_HASH = ? AND $COLUMN_EVENT_ID = ?",
                    arrayOf(scopeHash, eventId),
                )
                if (updatedRows == 0) {
                    insertOrThrow(
                        TABLE_EVENTS,
                        null,
                        ContentValues().apply {
                            put(COLUMN_SCOPE_HASH, scopeHash)
                            put(COLUMN_EVENT_ID, eventId)
                            put(COLUMN_POSITION, nextPosition++)
                            put(COLUMN_PAYLOAD, payload)
                        },
                    )
                }
            }
        }
    }

    override fun getTimeSyncState(): String? {
        val scopeHash = activeScopeHash ?: return null
        return databaseHelper.readableDatabase.query(
            TABLE_STATE,
            arrayOf(COLUMN_TIME_SYNC_STATE),
            "$COLUMN_SCOPE_HASH = ?",
            arrayOf(scopeHash),
            null,
            null,
            null,
            "1",
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    override fun setTimeSyncState(value: String?) {
        val scopeHash = activeScopeHash ?: return
        val database = databaseHelper.writableDatabase
        if (value.isNullOrBlank()) {
            database.delete(TABLE_STATE, "$COLUMN_SCOPE_HASH = ?", arrayOf(scopeHash))
            return
        }
        database.insertWithOnConflict(
            TABLE_STATE,
            null,
            ContentValues().apply {
                put(COLUMN_SCOPE_HASH, scopeHash)
                put(COLUMN_TIME_SYNC_STATE, value)
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    override fun clear() {
        val scopeHash = activeScopeHash ?: return
        databaseHelper.writableDatabase.inTransaction {
            delete(TABLE_EVENTS, "$COLUMN_SCOPE_HASH = ?", arrayOf(scopeHash))
            delete(TABLE_STATE, "$COLUMN_SCOPE_HASH = ?", arrayOf(scopeHash))
        }
    }

    private class DatabaseHelper(context: Context) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
        override fun onCreate(database: SQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE $TABLE_EVENTS (
                    $COLUMN_SCOPE_HASH TEXT NOT NULL,
                    $COLUMN_EVENT_ID TEXT NOT NULL,
                    $COLUMN_POSITION INTEGER NOT NULL,
                    $COLUMN_PAYLOAD TEXT NOT NULL,
                    PRIMARY KEY ($COLUMN_SCOPE_HASH, $COLUMN_EVENT_ID)
                )
                """.trimIndent(),
            )
            database.execSQL(
                "CREATE INDEX tracker_events_scope_position " +
                        "ON $TABLE_EVENTS ($COLUMN_SCOPE_HASH, $COLUMN_POSITION)",
            )
            database.execSQL(
                """
                CREATE TABLE $TABLE_STATE (
                    $COLUMN_SCOPE_HASH TEXT PRIMARY KEY,
                    $COLUMN_TIME_SYNC_STATE TEXT NOT NULL
                )
                """.trimIndent(),
            )
        }

        override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }

    private companion object {
        private const val DATABASE_NAME = "crm_consultant_tracker.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_EVENTS = "tracker_events"
        private const val TABLE_STATE = "tracker_state"
        private const val COLUMN_SCOPE_HASH = "scope_hash"
        private const val COLUMN_EVENT_ID = "event_id"
        private const val COLUMN_POSITION = "position"
        private const val COLUMN_PAYLOAD = "payload"
        private const val COLUMN_TIME_SYNC_STATE = "time_sync_state"

        private fun hashScope(siteToken: String, userToken: String): String {
            val bytes = MessageDigest.getInstance("SHA-256")
                .digest("$siteToken\u0000$userToken".toByteArray(Charsets.UTF_8))
            return bytes.joinToString(separator = "") { byte ->
                (byte.toInt() and 0xff).toString(16).padStart(2, '0')
            }
        }
    }
}
