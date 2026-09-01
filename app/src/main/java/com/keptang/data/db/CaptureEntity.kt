package com.keptang.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "captures")
data class CaptureEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "captured_at_epoch_millis") val capturedAtEpochMillis: Long,
    @ColumnInfo(name = "time_zone_id") val timeZoneId: String,
    @ColumnInfo(name = "audio_file_path") val audioFilePath: String,
    @ColumnInfo(name = "duration_millis") val durationMillis: Long,
    @ColumnInfo(name = "raw_transcript") val rawTranscript: String?,
    @ColumnInfo(name = "status") val status: CaptureStatus,
    @ColumnInfo(name = "error_message") val errorMessage: String?,
    @ColumnInfo(name = "created_at_epoch_millis") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis") val updatedAtEpochMillis: Long
)
