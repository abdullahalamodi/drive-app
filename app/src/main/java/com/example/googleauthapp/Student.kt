package com.example.googleauthapp
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class Student(
    @PrimaryKey
    val id: String = "1",
    var name: String = "",
    var number: Int = 0,
    var passed: Boolean = true
)