package com.example.googleauthapp.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.googleauthapp.Student

@Database(entities = [Student::class ], version=1,exportSchema = false)
abstract class StudentDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao;
}