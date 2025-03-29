package tw.zipe.mcp.googledrive

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.FileContent
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.gson.Gson
import io.quarkiverse.mcp.server.Tool
import io.quarkiverse.mcp.server.ToolArg
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import org.apache.commons.io.IOUtils

/**
 * @author Gary
 * @created 2025/3/28
 */
class GoogleDriveFileOperations {
    private companion object {
        private const val APPLICATION_NAME = "Google Drive File Operations"
        private val JSON_FACTORY: JsonFactory = GsonFactory.getDefaultInstance()
        private const val TOKENS_DIRECTORY_PATH = "tokens"
        private val SCOPES = listOf(DriveScopes.DRIVE_FILE)
        private const val FOLDER_MIME_TYPE = "application/vnd.google-apps.folder"
        private val gson = Gson()
    }

    private val driveService: Drive

    init {
        val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
        driveService = Drive.Builder(
            httpTransport,
            JSON_FACTORY,
            getCredentials(httpTransport)
        )
            .setApplicationName(APPLICATION_NAME)
            .build()
    }

    // Shared method: Generate success response JSON
    private fun createSuccessResponse(data: Map<String, Any?>): String {
        val responseMap = mutableMapOf<String, Any?>("success" to true)
        responseMap.putAll(data)
        return gson.toJson(responseMap)
    }

    // Shared method: Generate error response JSON
    private fun createErrorResponse(error: String, data: Map<String, Any?> = emptyMap()): String {
        val responseMap = mutableMapOf<String, Any?>("success" to false, "error" to error)
        responseMap.putAll(data)
        return gson.toJson(responseMap)
    }

    private fun getCredentials(httpTransport: HttpTransport): Credential {
        // Load client secrets
        val jsonFile = System.getenv("CREDENTIALS_FILE_PATH")
        require(!jsonFile.isNullOrEmpty()) { "CREDENTIALS_FILE_PATH environment variable is not set." }

        val credentialFile = java.io.File(jsonFile)
        val inputStream = credentialFile.inputStream()
        val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(inputStream))

        // Build authorization flow
        val flow = GoogleAuthorizationCodeFlow.Builder(
            httpTransport,
            JSON_FACTORY,
            clientSecrets,
            SCOPES
        )
            .setDataStoreFactory(FileDataStoreFactory(java.io.File(TOKENS_DIRECTORY_PATH)))
            .setAccessType("offline")
            .build()

        val receiver = LocalServerReceiver.Builder().setPort(8888).build()
        return AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
    }

    // Shared method: Check if item is a directory
    private fun isDirectory(fileId: String): Boolean {
        val file = driveService.files()[fileId].execute()
        return file.mimeType == FOLDER_MIME_TYPE
    }

    // Shared method: Get file information
    private fun getFileInfo(fileId: String, fields: String = "id,name,mimeType"): File {
        return driveService.files().get(fileId).setFields(fields).execute()
    }

    // Shared method: Generic rename functionality
    private fun rename(fileId: String, newName: String): File {
        val fileMetadata = File().setName(newName)
        return driveService.files().update(fileId, fileMetadata).execute()
    }

    // Create new file
    @Tool(description = "Create a new file in Google Drive")
    fun createNewFile(
        @ToolArg(description = "File name") fileName: String,
        @ToolArg(description = "File content, can be empty") content: String = "",
        @ToolArg(description = "File type, such as: text/plain") mimeType: String = "text/plain"
    ): String {
        // Set file metadata
        val fileMetadata = File().setName(fileName)

        // Create temporary file and write content
        val tempFile = java.io.File.createTempFile("temp_", "_file")
        tempFile.writeText(content)
        val mediaContent = FileContent(mimeType, tempFile)

        return try {
            // Upload file to Google Drive
            val uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id, name")
                .execute()

            createSuccessResponse(
                mapOf(
                    "fileId" to uploadedFile.id,
                    "fileName" to uploadedFile.name
                )
            )
        } catch (e: Exception) {
            createErrorResponse(e.message ?: "Failed to create file")
        } finally {
            // Delete temporary file
            tempFile.delete()
        }
    }

    // Upload file
    @Tool(description = "Upload a file to Google Drive")
    fun uploadFile(
        @ToolArg(description = "Local file path") filePath: String,
        @ToolArg(description = "File name in Google Drive") fileName: String,
        @ToolArg(description = "File type, such as: text/plain") fileType: String
    ): String {
        return try {
            val fileMetadata = File().setName(fileName)
            val file = java.io.File(filePath)
            val mediaContent = FileContent(fileType, file)

            val uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id, name")
                .execute()

            createSuccessResponse(
                mapOf(
                    "fileId" to uploadedFile.id,
                    "fileName" to uploadedFile.name
                )
            )
        } catch (e: Exception) {
            createErrorResponse(e.message ?: "Failed to upload file")
        }
    }

    // Delete file
    @Tool(description = "Delete a file from Google Drive")
    fun deleteFile(@ToolArg(description = "Google Drive file ID") fileId: String): String {
        return try {
            driveService.files().delete(fileId).execute()
            createSuccessResponse(
                mapOf(
                    "message" to "File successfully deleted",
                    "fileId" to fileId
                )
            )
        } catch (e: Exception) {
            createErrorResponse(e.message ?: "Failed to delete file", mapOf("fileId" to fileId))
        }
    }

    // Read file content
    @Tool(description = "Read file content from Google Drive")
    fun readFile(@ToolArg(description = "Google Drive file ID") fileId: String): String {
        return try {
            val fileInfo = getFileInfo(fileId, "id,name,mimeType")
            val inputStream = driveService.files()[fileId].executeMediaAsInputStream()
            // Convert file content to string
            val content = if (fileInfo.mimeType.contains("text/") || fileInfo.mimeType.contains("application/json")) {
                IOUtils.toString(inputStream, StandardCharsets.UTF_8)
            } else {
                // Binary files return Base64 encoding
                "[Binary file - Please use download function]"
            }

            createSuccessResponse(
                mapOf(
                    "fileId" to fileId,
                    "fileName" to fileInfo.name,
                    "mimeType" to fileInfo.mimeType,
                    "content" to content
                )
            )
        } catch (e: Exception) {
            createErrorResponse(e.message ?: "Failed to read file", mapOf("fileId" to fileId))
        }
    }

    // Update file content
    @Tool(description = "Update file content in Google Drive")
    fun updateFile(
        @ToolArg(description = "Google Drive file ID") fileId: String,
        @ToolArg(description = "Local file path to upload") filePath: String,
        @ToolArg(description = "File type, such as: text/plain") fileType: String
    ): String {
        return try {
            val fileMetadata = File()
            val file = java.io.File(filePath)
            val mediaContent = FileContent(fileType, file)

            val updatedFile = driveService.files().update(fileId, fileMetadata, mediaContent)
                .setFields("id,name")
                .execute()

            createSuccessResponse(
                mapOf(
                    "message" to "File content updated successfully",
                    "fileId" to updatedFile.id,
                    "fileName" to updatedFile.name
                )
            )
        } catch (e: Exception) {
            createErrorResponse(e.message ?: "Failed to update file", mapOf("fileId" to fileId))
        }
    }

    // List all files
    @Tool(description = "List all files in Google Drive")
    fun listFiles(): String {
        return try {
            val result = driveService.files().list()
                .setPageSize(10)
                .setQ("trashed = false")  // 添加此條件排除垃圾桶檔案
                .setFields("nextPageToken, files(id, name, mimeType)")
                .execute()


            val files = result.files.map { file ->
                mapOf(
                    "id" to file.id,
                    "name" to file.name,
                    "type" to if (file.mimeType == FOLDER_MIME_TYPE) "Directory" else "File"
                )
            }

            createSuccessResponse(
                mapOf(
                    "files" to files,
                    "totalCount" to files.size
                )
            )
        } catch (e: Exception) {
            createErrorResponse(e.message ?: "Failed to list files")
        }
    }

    // Get file ID by file name
    @Tool(description = "Get file ID by file name in Google Drive")
    fun getFileIdByName(@ToolArg(description = "File name in Google Drive") fileName: String): String {
        return try {
            val result = driveService.files().list()
                .setQ("name = '$fileName' and trashed = false")
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()

            val files = result.files
            if (!files.isNullOrEmpty()) {
                createSuccessResponse(
                    mapOf(
                        "fileId" to files[0].id,
                        "fileName" to files[0].name
                    )
                )
            } else {
                createErrorResponse("Could not find file named '$fileName'")
            }
        } catch (e: Exception) {
            createErrorResponse(e.message ?: "Failed to get file ID")
        }
    }

    // Advanced search function, supports fuzzy matching
    @Tool(description = "Search files in Google Drive and return file ID list")
    fun searchFiles(@ToolArg(description = "Search keyword") query: String): String {
        return try {
            val result = driveService.files().list()
                .setQ("name contains '$query' and trashed = false")
                .setSpaces("drive")
                .setFields("files(id, name, mimeType)")
                .execute()

            val files = result.files.map {
                mapOf(
                    "id" to it.id,
                    "name" to it.name,
                    "type" to if (it.mimeType == FOLDER_MIME_TYPE) "Directory" else "File"
                )
            }

            createSuccessResponse(
                mapOf(
                    "files" to files,
                    "totalCount" to files.size,
                    "query" to query
                )
            )
        } catch (e: Exception) {
            createErrorResponse(e.message ?: "Failed to search files", mapOf("query" to query))
        }
    }

    // Rename file
    @Tool(description = "Change file name in Google Drive")
    fun renameFile(
        @ToolArg(description = "Google Drive file ID") fileId: String,
        @ToolArg(description = "New name") newName: String
    ): String {
        return try {
            val renamedFile = rename(fileId, newName)
            createSuccessResponse(
                mapOf(
                    "message" to "File renamed successfully",
                    "fileId" to fileId,
                    "newName" to newName,
                    "oldName" to renamedFile.name
                )
            )
        } catch (e: Exception) {
            createErrorResponse(e.message ?: "Failed to rename", mapOf("fileId" to fileId))
        }
    }

    // Create directory in root
    @Tool(description = "Create directory in Google Drive root")
    fun createDirectory(
        @ToolArg(description = "Directory name") directoryName: String
    ): String {
        return try {
            val fileMetadata = File().setName(directoryName).setMimeType(FOLDER_MIME_TYPE)
            val createdDir = driveService.files().create(fileMetadata)
                .setFields("id, name")
                .execute()

            createSuccessResponse(
                mapOf(
                    "message" to "Directory created successfully",
                    "directoryId" to createdDir.id,
                    "directoryName" to createdDir.name
                )
            )
        } catch (e: Exception) {
            createErrorResponse(e.message ?: "Failed to create directory")
        }
    }

    // Create directory in specified parent directory
    @Tool(description = "Create directory in a specified parent directory in Google Drive")
    fun createDirectoryInParent(
        @ToolArg(description = "Directory name") directoryName: String,
        @ToolArg(description = "Parent directory ID") parentId: String
    ): String {
        return try {
            val fileMetadata = File().setName(directoryName).setMimeType(FOLDER_MIME_TYPE)
            fileMetadata.parents = listOf(parentId)

            val createdDir = driveService.files().create(fileMetadata)
                .setFields("id, name")
                .execute()

            createSuccessResponse(
                mapOf(
                    "message" to "Subdirectory created successfully",
                    "directoryId" to createdDir.id,
                    "directoryName" to createdDir.name,
                    "parentId" to parentId
                )
            )
        } catch (e: Exception) {
            createErrorResponse(e.message ?: "Failed to create subdirectory", mapOf("parentId" to parentId))
        }
    }

    // Delete directory
    @Tool(description = "Delete a directory from Google Drive")
    fun deleteDirectory(
        @ToolArg(description = "Google Drive directory ID") dirId: String
    ): String {
        return try {
            if (!isDirectory(dirId)) {
                return createErrorResponse(
                    "ID $dirId is not a directory, cannot execute directory deletion",
                    mapOf("directoryId" to dirId)
                )
            }

            driveService.files().delete(dirId).execute()
            createSuccessResponse(
                mapOf(
                    "message" to "Directory deleted successfully",
                    "directoryId" to dirId
                )
            )
        } catch (e: Exception) {
            createErrorResponse(e.message ?: "Failed to delete directory", mapOf("directoryId" to dirId))
        }
    }

    // Rename directory
    @Tool(description = "Rename a directory in Google Drive")
    fun renameDirectory(
        @ToolArg(description = "Google Drive directory ID") dirId: String,
        @ToolArg(description = "New directory name") newName: String
    ): String {
        return try {
            if (!isDirectory(dirId)) {
                return createErrorResponse(
                    "ID $dirId is not a directory, cannot execute directory rename operation",
                    mapOf("directoryId" to dirId)
                )
            }

            val renamedDir = rename(dirId, newName)
            createSuccessResponse(
                mapOf(
                    "message" to "Directory renamed successfully",
                    "directoryId" to dirId,
                    "newName" to newName,
                    "oldName" to renamedDir.name
                )
            )
        } catch (e: Exception) {
            createErrorResponse(e.message ?: "Failed to rename", mapOf("directoryId" to dirId))
        }
    }

    // List files and subdirectories in a directory
    @Tool(description = "List files and subdirectories in a Google Drive directory")
    fun listDirectoryContents(
        @ToolArg(description = "Google Drive directory ID, empty value means root directory") dirId: String? = null
    ): String {
        return try {
            val query = if (dirId.isNullOrEmpty()) {
                "'root' in parents and trashed = false"
            } else {
                "'$dirId' in parents and trashed = false"
            }

            val result = driveService.files().list()
                .setQ(query)
                .setFields("files(id, name, mimeType)")
                .execute()

            val contents = result.files.map {
                val type = if (it.mimeType == FOLDER_MIME_TYPE) "Directory" else "File"
                mapOf(
                    "id" to it.id,
                    "name" to it.name,
                    "type" to type
                )
            }

            createSuccessResponse(
                mapOf(
                    "directoryId" to (dirId ?: "root"),
                    "contents" to contents,
                    "totalCount" to contents.size
                )
            )
        } catch (e: Exception) {
            createErrorResponse(
                e.message ?: "Failed to list directory contents",
                mapOf("directoryId" to (dirId ?: "root"))
            )
        }
    }

    // Move file to specified directory
    @Tool(description = "Move a file to a specified directory")
    fun moveFile(
        @ToolArg(description = "Google Drive file ID") fileId: String,
        @ToolArg(description = "Target directory ID") targetDirId: String
    ): String {
        return try {
            // First get the current parent directory of the file
            val file = getFileInfo(fileId, "id,name,parents")

            // Remove from all parent directories and add to new directory
            val previousParents = file.parents.joinToString(",")
            val updatedFile = driveService.files().update(fileId, null)
                .setAddParents(targetDirId)
                .setRemoveParents(previousParents)
                .setFields("id, parents, name")
                .execute()

            createSuccessResponse(
                mapOf(
                    "message" to "File moved successfully",
                    "fileId" to fileId,
                    "fileName" to updatedFile.name,
                    "targetDirectoryId" to targetDirId,
                    "previousParents" to previousParents
                )
            )
        } catch (e: Exception) {
            createErrorResponse(
                e.message ?: "Failed to move file", mapOf(
                    "fileId" to fileId,
                    "targetDirectoryId" to targetDirId
                )
            )
        }
    }
}
