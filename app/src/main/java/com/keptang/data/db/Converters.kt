package com.keptang.data.db

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromCaptureStatus(value: CaptureStatus): String = value.name

    @TypeConverter
    fun toCaptureStatus(value: String): CaptureStatus = CaptureStatus.valueOf(value)

    @TypeConverter
    fun fromReviewStatus(value: ReviewStatus): String = value.name

    @TypeConverter
    fun toReviewStatus(value: String): ReviewStatus = ReviewStatus.valueOf(value)
}
