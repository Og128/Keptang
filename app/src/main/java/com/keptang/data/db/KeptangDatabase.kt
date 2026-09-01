package com.keptang.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [CaptureEntity::class, ExpenseEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class KeptangDatabase : RoomDatabase() {

    abstract fun captureDao(): CaptureDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var instance: KeptangDatabase? = null

        fun getInstance(context: Context): KeptangDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    KeptangDatabase::class.java,
                    "keptang.db"
                ).build().also { instance = it }
            }
    }
}
