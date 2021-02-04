package com.example.googleauthapp.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.googleauthapp.Student
import java.util.*

@Dao
interface StudentDao {

    @Query("SELECT * FROM student")
   fun getStudents(): LiveData<List<Student>>

    @Query("SELECT * FROM student WHERE id=(:id)")
    fun getStudent(id: String): LiveData<Student?>

    @Update
    fun updateStudent(student: Student)

    @Insert
    fun addStudent(student: Student)
}