package com.example.googleauthapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.googleauthapp.database.StudentRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.gson.Gson
import java.io.*
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var mDriveServiceHelper: DriveServiceHelper
    lateinit var signInButton: SignInButton
    lateinit var uploadButton: Button
    lateinit var namView: TextView
    private var myAccount: GoogleSignInAccount? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        signInButton = findViewById(R.id.sign_in_button)
        uploadButton = findViewById(R.id.upload_btn)
        namView = findViewById(R.id.name_tv)
//        addStudent()
        getStudents()
        checkIfDatabaseFile()
        signInButton.setOnClickListener {
            signIn()
        }

        uploadButton.setOnClickListener {
//            createFile()
//            createFolder()
            getFolderId()
//            getFile()

//        namView.text = getDatabasePath(DB_NAME).absolutePath
        }

    }

    override fun onStart() {
        super.onStart()
        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        myAccount = GoogleSignIn.getLastSignedInAccount(this)
        updateUI(myAccount)
    }

    private fun addStudent() {
        StudentRepository.get().addStudent(
            Student("2")
        )
    }

    private fun getStudents() {
        StudentRepository.get().getStudents().observe(
            this,
            androidx.lifecycle.Observer {
                namView.text = it.size.toString()
            }
        )
    }

    private fun checkIfDatabaseFile() {
        StudentRepository.get().closeDb()
        val database = getDatabasePath(DB_NAME)
        Toast.makeText(
            this, database.isFile.toString(),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun updateUI(account: GoogleSignInAccount?) {
        if (account != null) {
            namView.text = account.email
            mDriveServiceHelper = DriveServiceHelper(getGoogleDrive())
        } else {
            namView.text = "register"
        }
    }

    //sign-in
//    ------------------------------------------------

    //    private fun checkForGooglePermissions() {
//        if (!GoogleSignIn.hasPermissions(
//                GoogleSignIn.getLastSignedInAccount(applicationContext),
//                ACCESS_DRIVE_SCOPE,
//                SCOPE_EMAIL
//            )
//        ) {
//            GoogleSignIn.requestPermissions(
//                this@MainActivity,
//                RC_AUTHORIZE_DRIVE,
//                GoogleSignIn.getLastSignedInAccount(applicationContext),
//                ACCESS_DRIVE_SCOPE,
//                SCOPE_EMAIL
//            )
//        } else {
//            Toast.makeText(
//                this,
//                "Permission to access Drive and Email has been granted",
//                Toast.LENGTH_SHORT
//            ).show()
//            driveSetUp()
//        }
//    }
    private fun getGoogleDrive(): Drive {
        val credential =
            GoogleAccountCredential.usingOAuth2(
                applicationContext, Collections.singleton(Scopes.DRIVE_FILE)
            )
        credential.selectedAccount = myAccount?.account
        return Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory(),
            credential
        )
            .setApplicationName("googleAuthApp")
            .build()
    }

    private fun signIn() {
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(
                Scope(Scopes.DRIVE_FILE)
//                Scope(Scopes.DRIVE_APPFOLDER)
            )
            .requestEmail()
            .build()

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);


        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            myAccount =
                completedTask.getResult(ApiException::class.java)

            // Signed in successfully, show authenticated UI.
            updateUI(myAccount)
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(TAG, "signInResult:failed code=" + e.statusCode)
            updateUI(null)
        }
    }


    //google drive
//    ------------------------------------------------

    private fun createFile(folderId: String) {
        // you can provide  folder id in case you want to save this file inside some folder.
        // if folder id is null, it will save file to the root
        mDriveServiceHelper.createFile(folderId)
            .addOnSuccessListener { googleDriveFileHolder ->
                val gson = Gson()
                Log.d(
                    TAG,
                    "onSuccess: " + gson.toJson(googleDriveFileHolder)
                )
                Toast.makeText(this, "success", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.d(
                    TAG,
                    "onFailure: " + e.message
                )
            }

    }

    private fun createSQLFile(folderId: String) {
        StudentRepository.get().closeDb()
        val database = getDatabasePath(DB_NAME)
//        val databasePath = database.absolutePath
//        val filePath = File(databasePath)
        val mediaContent = FileContent("application/sqlite", database)
        mDriveServiceHelper.createSQlFile(
            databaseName = DB_NAME,
            mediaContent = mediaContent,
            folderId = folderId
        )
            ?.addOnSuccessListener { fileId ->
                Toast.makeText(this, "success : $fileId", Toast.LENGTH_SHORT).show()
                getFileId(folderId)
            }
            ?.addOnFailureListener { e ->
                Log.d(
                    TAG,
                    "onFailure: " + e.message
                )
            }

    }

    private fun createFolder() {
        // you can provide  folder id in case you want to save this file inside some folder.
        // if folder id is null, it will save file to the root
        mDriveServiceHelper.createFolder(FOLDER_NAME, null)
            ?.addOnSuccessListener { folderId ->
                namView.text = folderId
//                createSQLFile(folderId)
                getFileId(folderId)
            }
            ?.addOnFailureListener { e ->
                Log.d(
                    TAG,
                    "onFailure: " + e.message
                )
            }

    }


    private fun getFileId(folderId: String) {
        mDriveServiceHelper.queryFiles(folderId)
            ?.addOnSuccessListener { fileId ->
                namView.text = fileId
                downloadFile(fileId!!)
            }
            ?.addOnFailureListener { e ->
                Log.d(
                    TAG,
                    "onFailure: " + e.message
                )
            }
    }

    private fun downloadFile(fileId: String) {
        val targetFile = File("backup")
        mDriveServiceHelper.downloadFile(
            targetFile = targetFile,
            fileId = fileId
        )
            ?.addOnSuccessListener {
                restoreDb(targetFile.inputStream())
            }
            ?.addOnFailureListener { e ->
                Log.d(
                    TAG,
                    "onFailure: " + e.message
                )
            }
    }

    private fun getFolderId() {
        mDriveServiceHelper.getFolderId(FOLDER_NAME)
            ?.addOnSuccessListener { folderId ->
                if (folderId == null) {
                    createFolder()
                } else {
                    namView.text = folderId
//                    createSQLFile(folderId)
                    getFileId(folderId)
                }
            }
            ?.addOnFailureListener { e ->
                Log.d(
                    TAG,
                    "onFailure: " + e.message
                )
            }
    }

    private fun restoreDb(inputStreamNewDB: InputStream?) {
        StudentRepository.get().closeDb()
        val oldDB: File = getDatabasePath(DB_NAME)
        if (inputStreamNewDB != null) {
            try {
                DriveServiceHelper.copyFile(
                    (inputStreamNewDB as FileInputStream?)!!,
                    FileOutputStream(oldDB)
                )
                Toast.makeText(this, "success : restore", Toast.LENGTH_SHORT).show()
                //Take the user to home screen and there we will validate if the database file was actually restored correctly.
            } catch (e: IOException) {
                Log.d(TAG, "ex for is of restore: $e")
                e.printStackTrace()
            }
        } else {
            Log.d(TAG, "Restore - file does not exists")
        }
    }


//    private fun downloadFile(fileId: String) {
//        mDriveServiceHelper.downloadFile(
//            File(
//                applicationContext.filesDir,
//                "filename.txt"
//            ), fileId
//        )
//            ?.addOnSuccessListener {
//                Log.d(
//                    TAG,
//                    "onSuccess: file downloaded"
//                )
//            }
//            ?.addOnFailureListener {
//
//            }
//    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            val task =
                GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    companion object {
        const val RC_SIGN_IN = 207
        const val TAG = "MainActivityLog"
        const val FOLDER_NAME = "OABackup"
        const val DB_NAME = "student_database"
    }
}