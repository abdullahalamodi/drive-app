package com.example.googleauthapp

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.annotation.Nullable
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.api.client.http.AbstractInputStreamContent
import com.google.api.client.http.ByteArrayContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.concurrent.Executors


class DriveServiceHelper(private val mDriveService: Drive) {
    private val mExecutor: Executor = Executors.newSingleThreadExecutor()

    /**
     * Creates a text file in the user's My Drive folder and returns its file ID.
     */
    fun createFile(@Nullable folderId: String?): Task<String> {
        return Tasks.call(mExecutor,
            Callable {
                val root: List<String> = if (folderId == null) {
                    Collections.singletonList("root")
                } else {
                    Collections.singletonList(folderId)
                }
            val metadata = File()
                .setParents(root)
                .setMimeType("text/plain")
                .setName(MainActivity.DB_NAME)
            val googleFile = mDriveService.files().create(metadata).execute()
                ?: throw IOException("Null result when requesting file creation.")
            googleFile.id
        })
    }


    fun createSQlFile(
        // like "your_database_name.db"
        databaseName: String?,
        mediaContent:
        AbstractInputStreamContent,
        @Nullable folderId: String?
    ): Task<GoogleDriveFileHolder>? {
        return Tasks.call(mExecutor,
            Callable {
                val root: List<String> = if (folderId == null) {
                    Collections.singletonList("root")
                } else {
                    Collections.singletonList(folderId)
                }
                val metadata =
                    File()
                        .setParents(root)
                        .setName(databaseName)
                val googleFile =
                    mDriveService.files().create(metadata, mediaContent).execute()
                        ?: throw IOException("Null result when requesting file creation.")
                val googleDriveFileHolder = GoogleDriveFileHolder()
                googleDriveFileHolder.id = (googleFile.id)
                return@Callable googleDriveFileHolder
            }
        )
    }


    fun createFolder(
        folderName: String?,
        folderId: String?
    ): Task<String>? {
        return Tasks.call(mExecutor,
            Callable {
                val root: List<String> = if (folderId == null) {
                    Collections.singletonList("root")
                } else {
                    Collections.singletonList(folderId)
                }
                val metadata =
                    File()
                        .setParents(root)
                        .setMimeType("application/vnd.google-apps.folder")
                        .setName(folderName)
                val googleFile =
                    mDriveService.files().create(metadata).execute()
                        ?: throw IOException("Null result when requesting file creation.")
                return@Callable googleFile.id
            }
        )
    }


    fun getFolderId(folderName: String): Task<String?>? {
        return Tasks.call(mExecutor, Callable {
            // Retrive the metadata as a File object.
            val result: FileList = mDriveService.files().list()
                .setQ("mimeType = 'application/vnd.google-apps.folder' and name = '$folderName' ")
                .setSpaces("drive")
                .execute()
            if (result.files.size > 0) {
                return@Callable result.files[0].id
            }
            return@Callable null
        })
    }

    fun createTextFileIfNotExist(
        fileName: String,
        content: String?,
        @Nullable folderId: String?
    ): Task<GoogleDriveFileHolder?>? {
        return Tasks.call(
            mExecutor,
            Callable {
                val googleDriveFileHolder = GoogleDriveFileHolder()
                val result = mDriveService.files().list()
                    .setQ("mimeType = 'text/plain' and name = '$fileName' ")
                    .setSpaces("drive")
                    .execute()
                if (result.files.size > 0) {
                    googleDriveFileHolder.id = result.files[0].id
                    return@Callable googleDriveFileHolder
                } else {
                    val root: List<String> = if (folderId == null) {
                        Collections.singletonList("root")
                    } else {
                        Collections.singletonList(folderId)
                    }
                    val metadata =
                        File()
                            .setParents(root)
                            .setMimeType("text/plain")
                            .setName(fileName)
                    val contentStream =
                        ByteArrayContent.fromString("text/plain", content)
                    val googleFile =
                        mDriveService.files().create(metadata, contentStream).execute()
                            ?: throw IOException("Null result when requesting file creation.")
                    googleDriveFileHolder.id = googleFile.id
                    return@Callable googleDriveFileHolder
                }
            })
    }


//    fun downloadFile(
//        targetFile: java.io.File?,
//        fileId: String?
//    ): Task<String?>? {
//        return Tasks.call(
//            mExecutor,
//            Callable {
//                // Retrieve the metadata as a File object.
//                val outputStream: OutputStream = FileOutputStream(targetFile)
//                mDriveService.files()[fileId].executeMediaAndDownloadTo(outputStream)
//                return@Callable null
//            }
//        )
//    }


    fun queryFiles(folderId: String?): Task<List<GoogleDriveFileHolder?>?>? {
        return Tasks.call<List<GoogleDriveFileHolder?>?>(
            mExecutor,
            Callable {
                val googleDriveFileHolderList: MutableList<GoogleDriveFileHolder> =
                    ArrayList()
                var parent = "root"
                if (folderId != null) {
                    parent = folderId
                }
                val result =
                    mDriveService.files().list().setQ("'$parent' in parents")
                        .setFields("files(id, name,size,createdTime,modifiedTime,starred)")
                        .setSpaces("drive").execute()
                for (i in result.files.indices) {
                    val googleDriveFileHolder = GoogleDriveFileHolder()
                    googleDriveFileHolder.id = result.files[i].id
                    googleDriveFileHolder.name = result.files[i].name
                    if (result.files[i].getSize() != null) {
                        googleDriveFileHolder.size = result.files[i].getSize()
                    }
                    if (result.files[i].modifiedTime != null) {
                        googleDriveFileHolder.modifiedTime =
                            result.files[i].modifiedTime
                    }
                    if (result.files[i].createdTime != null) {
                        googleDriveFileHolder.createdTime =
                            result.files[i].createdTime
                    }
                    if (result.files[i].starred != null) {
                        googleDriveFileHolder.starred = result.files[i].starred
                    }
                    googleDriveFileHolderList.add(googleDriveFileHolder)
                }
                return@Callable googleDriveFileHolderList
            }
        )
    }


    /**
     * Opens the file identified by `fileId` and returns a [Pair] of its name and
     * contents.
     */
    fun readFile(fileId: String?): Task<Pair<String, String>> {
        return Tasks.call(mExecutor,
            Callable {
                // Retrieve the metadata as a File object.
                val metadata = mDriveService.files()[fileId].execute()
                val name = metadata.name
                mDriveService.files()[fileId].executeMediaAsInputStream().use { `is` ->
                    BufferedReader(InputStreamReader(`is`)).use { reader ->
                        val stringBuilder = StringBuilder()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            stringBuilder.append(line)
                        }
                        val contents = stringBuilder.toString()
                        return@Callable Pair(name, contents)
                    }
                }
            }
        )
    }

    /**
     * Updates the file identified by `fileId` with the given `name` and `content`.
     */
//    fun saveFile(
//        fileId: String?,
//        name: String?,
//        content: String?
//    ): Task<Void?> {
//        return Tasks.call(
//            mExecutor,
//            Callable {
//
//                // Create a File containing any metadata changes.
//                val metadata = File().setName(name)
//
//                // Convert content to an AbstractInputStreamContent instance.
//                val contentStream =
//                    ByteArrayContent.fromString("text/plain", content)
//
//                // Update the metadata and contents.
//                mDriveService.files().update(fileId, metadata, contentStream).execute()
//                null
//            }
//        )
//    }

    /**
     * Returns a [FileList] containing all the visible files in the user's My Drive.
     *
     *
     * The returned list will only contain files visible to this app, i.e. those which were
     * created by this app. To perform operations on files not created by the app, the project must
     * request Drive Full Scope in the [Google
 * Developer's Console](https://play.google.com/apps/publish) and be submitted to Google for verification.
     */
//    fun queryFiles(): Task<FileList> {
//        return Tasks.call(mExecutor,
//            Callable {
//                mDriveService.files().list()
//                    .setSpaces("root")
//                    .setFields("nextPageToken, files(id, name)")
//                    .setPageSize(10)
//                    .execute()
//            }
//        )
//    }

    /**
     * Returns an [Intent] for opening the Storage Access Framework file picker.
     */
    fun createFilePickerIntent(): Intent {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "text/plain"
        return intent
    }

    /**
     * Opens the file at the `uri` returned by a Storage Access Framework [Intent]
     * created by [.createFilePickerIntent] using the given `contentResolver`.
     */
    fun openFileUsingStorageAccessFramework(
        contentResolver: ContentResolver, uri: Uri?
    ): Task<Pair<String, String>> {
        return Tasks.call(mExecutor,
            Callable {

                // Retrieve the document's display name from its metadata.
                var name = ""
                contentResolver.query(uri!!, null, null, null, null).use { cursor ->
                    name = if (cursor != null && cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        cursor.getString(nameIndex)
                    } else {
                        throw IOException("Empty cursor returned for file.")
                    }
                }

                // Read the document's contents as a String.
                var content = ""
                contentResolver.openInputStream(uri).use { `is` ->
                    BufferedReader(InputStreamReader(`is`)).use { reader ->
                        val stringBuilder = StringBuilder()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            stringBuilder.append(line)
                        }
                        content = stringBuilder.toString()
                    }
                }
                return@Callable Pair(name, content)
            }
        )
    }

    companion object {
        var TYPE_AUDIO = "application/vnd.google-apps.audio"
        var TYPE_GOOGLE_DOCS = "application/vnd.google-apps.document"
        var TYPE_GOOGLE_DRAWING = "application/vnd.google-apps.drawing"
        var TYPE_GOOGLE_DRIVE_FILE = "application/vnd.google-apps.file"

        //        var TYPE_GOOGLE_DRIVE_FOLDER: String = DriveFolder.MIME_TYPE
        var TYPE_GOOGLE_FORMS = "application/vnd.google-apps.form"
        var TYPE_GOOGLE_FUSION_TABLES = "application/vnd.google-apps.fusiontable"
        var TYPE_GOOGLE_MY_MAPS = "application/vnd.google-apps.map"
        var TYPE_PHOTO = "application/vnd.google-apps.photo"
        var TYPE_GOOGLE_SLIDES = "application/vnd.google-apps.presentation"
        var TYPE_GOOGLE_APPS_SCRIPTS = "application/vnd.google-apps.script"
        var TYPE_GOOGLE_SITES = "application/vnd.google-apps.site"
        var TYPE_GOOGLE_SHEETS = "application/vnd.google-apps.spreadsheet"
        var TYPE_UNKNOWN = "application/vnd.google-apps.unknown"
        var TYPE_VIDEO = "application/vnd.google-apps.video"
        var TYPE_3_RD_PARTY_SHORTCUT = "application/vnd.google-apps.drive-sdk"
    }

}

