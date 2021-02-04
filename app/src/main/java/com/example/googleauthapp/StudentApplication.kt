package com.example.googleauthapp
import android.app.Application
import com.example.googleauthapp.database.StudentRepository

class StudentApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        StudentRepository.initialize(applicationContext); //applicationContext for long object life
    }
}