package tw.zipe.mcp.googledrive

import com.google.gson.Gson
import java.io.File
import java.io.FileWriter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.AfterEach

/**
 * @author Gary
 * @created 2025/3/28
 */
class GoogleDriveFileOperationsTest {
    private lateinit var driveOperations: GoogleDriveFileOperations
    private lateinit var tempTestFile: File
    private val gson = Gson()

    // 用於存儲測試過程中創建的資源 ID，便於清理
    private val testFileIds = mutableListOf<String>()
    private val testDirectoryIds = mutableListOf<String>()

    @BeforeEach
    fun setup() {
        SSLUtil.disableSSLVerification()
        // 初始化 Google Drive 操作類
        driveOperations = GoogleDriveFileOperations()

        // 創建臨時測試文件
        tempTestFile = createTempTestFile()
    }

    @AfterEach
    fun cleanup() {
        // 清理所有創建的測試文件
        testFileIds.forEach {
            try {
                driveOperations.deleteFile(it)
            } catch (ex: Exception) {
                println("清理文件失敗: ${ex.message}")
            }
        }

        // 清理所有創建的目錄
        testDirectoryIds.forEach {
            try {
                driveOperations.deleteDirectory(it)
            } catch (ex: Exception) {
                println("清理目錄失敗: ${ex.message}")
            }
        }

        testFileIds.clear()
        testDirectoryIds.clear()
    }

    private fun createTempTestFile(): File {
        // 創建一個臨時測試文件
        return File.createTempFile("test_file_ops", ".txt").apply {
            FileWriter(this).use { writer ->
                writer.write("This is a test file for Google Drive file operations")
            }
            deleteOnExit() // 確保測試完成後刪除臨時文件
        }
    }

    @Test
    fun `test create and delete file`() {
        // 測試創建新文件
        val fileName = "test_file_${System.currentTimeMillis()}.txt"
        val content = "This is a test file content"
        val createResultJson = driveOperations.createNewFile(fileName, content)
        val createResult = gson.fromJson(createResultJson, Map::class.java)

        // 驗證返回的信息包含成功創建文件的訊息
        assertTrue(
            createResult["success"] as Boolean,
            "File creation should be successful"
        )

        // 從返回的消息中提取文件ID
        val fileId = createResult["fileId"] as String
        testFileIds.add(fileId)  // 添加到清理列表

        assertNotNull(fileId, "Should be able to extract file ID from result")

        // 測試讀取文件內容
        val readResultJson = driveOperations.readFile(fileId)
        val readResult = gson.fromJson(readResultJson, Map::class.java)
        assertTrue(readResult["success"] as Boolean, "Reading file content should be successful")
        assertEquals(content, readResult["content"], "File content should match what was written")

        // 測試刪除文件
        val deleteResultJson = driveOperations.deleteFile(fileId)
        val deleteResult = gson.fromJson(deleteResultJson, Map::class.java)
        assertTrue(deleteResult["success"] as Boolean, "File deletion should be successful")
        testFileIds.remove(fileId)  // 從清理列表移除

        // 確認文件已刪除 - 嘗試再次讀取應該失敗
        val readAgainResultJson = driveOperations.readFile(fileId)
        val readAgainResult = gson.fromJson(readAgainResultJson, Map::class.java)
        assertFalse(readAgainResult["success"] as Boolean, "Reading deleted file should fail")
    }

    @Test
    fun `test upload and update file`() {
        // 上傳測試文件
        val fileName = "upload_test_${System.currentTimeMillis()}.txt"
        val uploadResultJson = driveOperations.uploadFile(
            tempTestFile.absolutePath,
            fileName,
            "text/plain"
        )
        val uploadResult = gson.fromJson(uploadResultJson, Map::class.java)
        assertTrue(uploadResult["success"] as Boolean, "File upload should be successful")

        val fileId = uploadResult["fileId"] as String
        testFileIds.add(fileId)  // 添加到清理列表

        // 讀取上傳的文件內容
        val readResultJson = driveOperations.readFile(fileId)
        val readResult = gson.fromJson(readResultJson, Map::class.java)
        assertTrue(readResult["success"] as Boolean, "Reading uploaded file should be successful")
        val originalContent = readResult["content"] as String

        // 創建一個新的臨時文件，用於更新
        val updatedFile = File.createTempFile("updated_file", ".txt").apply {
            FileWriter(this).use { writer ->
                writer.write("This is updated content for testing file update")
            }
            deleteOnExit()
        }

        // 更新文件內容
        val updateResultJson = driveOperations.updateFile(fileId, updatedFile.absolutePath, "text/plain")
        val updateResult = gson.fromJson(updateResultJson, Map::class.java)
        assertTrue(updateResult["success"] as Boolean, "File update should be successful")

        // 驗證文件內容已更新
        val readUpdatedResultJson = driveOperations.readFile(fileId)
        val readUpdatedResult = gson.fromJson(readUpdatedResultJson, Map::class.java)
        assertTrue(readUpdatedResult["success"] as Boolean, "Reading updated file should be successful")
        val updatedContent = readUpdatedResult["content"] as String

        assertNotNull(updatedContent, "Updated content should not be null")
        assertTrue(updatedContent != originalContent, "Updated content should differ from original content")
    }

    @Test
    fun `test file listing and searching`() {
        // 創建一個具有獨特名稱的文件，以便於搜尋
        val uniqueFileName = "unique_search_test_${System.currentTimeMillis()}.txt"
        val createResultJson = driveOperations.createNewFile(uniqueFileName, "Content for searching test")
        val createResult = gson.fromJson(createResultJson, Map::class.java)
        assertTrue(createResult["success"] as Boolean, "File creation should be successful")

        val fileId = createResult["fileId"] as String
        testFileIds.add(fileId)  // 添加到清理列表

        // 測試列出所有文件
        val listResultJson = driveOperations.listFiles()
        val listResult = gson.fromJson(listResultJson, Map::class.java)
        assertTrue(listResult["success"] as Boolean, "Listing files should be successful")

        val files = listResult["files"] as List<*>
        assertTrue(files.isNotEmpty(), "Files list should not be empty")

        // 確認我們創建的文件在列表中
        var foundInList = false
        for (file in files) {
            val fileMap = file as Map<*, *>
            if (fileMap["id"] == fileId) {
                foundInList = true
                break
            }
        }
        assertTrue(foundInList, "Created file should appear in the file list")

        // 測試通過名稱搜索文件
        val searchResultJson = driveOperations.searchFiles(uniqueFileName.substring(0, 10))
        val searchResult = gson.fromJson(searchResultJson, Map::class.java)
        assertTrue(searchResult["success"] as Boolean, "File search should be successful")

        val searchFiles = searchResult["files"] as List<*>
        assertTrue(searchFiles.isNotEmpty(), "Search results should not be empty")

        // 測試通過精確名稱獲取文件ID
        val getIdResultJson = driveOperations.getFileIdByName(uniqueFileName)
        val getIdResult = gson.fromJson(getIdResultJson, Map::class.java)
        assertTrue(getIdResult["success"] as Boolean, "Getting file ID by name should be successful")
        assertEquals(fileId, getIdResult["fileId"], "Retrieved file ID should match created file ID")
    }

    @Test
    fun `test file rename`() {
        // 創建測試文件
        val originalName = "original_name_${System.currentTimeMillis()}.txt"
        val createResultJson = driveOperations.createNewFile(originalName, "Content for renaming test")
        val createResult = gson.fromJson(createResultJson, Map::class.java)
        assertTrue(createResult["success"] as Boolean, "File creation should be successful")

        val fileId = createResult["fileId"] as String
        testFileIds.add(fileId)  // 添加到清理列表

        // 重命名文件
        val newName = "renamed_file_${System.currentTimeMillis()}.txt"
        val renameResultJson = driveOperations.renameFile(fileId, newName)
        val renameResult = gson.fromJson(renameResultJson, Map::class.java)
        assertTrue(renameResult["success"] as Boolean, "Renaming file should be successful")

        // 驗證文件已被重命名
        val getIdResultJson = driveOperations.getFileIdByName(newName)
        val getIdResult = gson.fromJson(getIdResultJson, Map::class.java)
        assertTrue(getIdResult["success"] as Boolean, "Getting file ID by new name should be successful")
        assertEquals(fileId, getIdResult["fileId"], "File ID after renaming should be unchanged")

        // 確認舊名稱不再存在
        val getOldIdResultJson = driveOperations.getFileIdByName(originalName)
        val getOldIdResult = gson.fromJson(getOldIdResultJson, Map::class.java)
        assertFalse(getOldIdResult["success"] as Boolean, "Old filename should no longer exist")
    }

    @Test
    fun `test directory operations`() {
        // 創建測試目錄
        val dirName = "test_dir_${System.currentTimeMillis()}"
        val createDirResultJson = driveOperations.createDirectory(dirName)
        val createDirResult = gson.fromJson(createDirResultJson, Map::class.java)
        assertTrue(createDirResult["success"] as Boolean, "Directory creation should be successful")

        val directoryId = createDirResult["directoryId"] as String
        testDirectoryIds.add(directoryId)  // 添加到清理列表

        // 測試在目錄中創建子目錄
        val subDirName = "test_subdir_${System.currentTimeMillis()}"
        val createSubDirResultJson = driveOperations.createDirectoryInParent(subDirName, directoryId)
        val createSubDirResult = gson.fromJson(createSubDirResultJson, Map::class.java)
        assertTrue(createSubDirResult["success"] as Boolean, "Sub-directory creation should be successful")

        val subDirId = createSubDirResult["directoryId"] as String
        testDirectoryIds.add(subDirId)  // 添加到清理列表

        // 驗證子目錄存在於父目錄中
        val listDirResultJson = driveOperations.listDirectoryContents(directoryId)
        val listDirResult = gson.fromJson(listDirResultJson, Map::class.java)
        assertTrue(listDirResult["success"] as Boolean, "Listing directory contents should be successful")

        val dirContents = listDirResult["contents"] as List<*>
        assertTrue(dirContents.isNotEmpty(), "Parent directory should not be empty")

        var foundSubDir = false
        for (content in dirContents) {
            val contentMap = content as Map<*, *>
            if (contentMap["id"] == subDirId && contentMap["name"] == subDirName) {
                foundSubDir = true
                break
            }
        }

        assertTrue(foundSubDir, "Sub-directory should be found in parent directory contents")

        // 測試重命名目錄
        val newDirName = "renamed_dir_${System.currentTimeMillis()}"
        val renameDirResultJson = driveOperations.renameDirectory(directoryId, newDirName)
        val renameDirResult = gson.fromJson(renameDirResultJson, Map::class.java)
        assertTrue(renameDirResult["success"] as Boolean, "Directory renaming should be successful")

        // 刪除子目錄
        val deleteSubDirResultJson = driveOperations.deleteDirectory(subDirId)
        val deleteSubDirResult = gson.fromJson(deleteSubDirResultJson, Map::class.java)
        assertTrue(deleteSubDirResult["success"] as Boolean, "Sub-directory deletion should be successful")
        testDirectoryIds.remove(subDirId)  // 從清理列表移除

        // 驗證子目錄被成功刪除
        val listAfterDeleteJson = driveOperations.listDirectoryContents(directoryId)
        val listAfterDelete = gson.fromJson(listAfterDeleteJson, Map::class.java)
        val contentsAfterDelete = listAfterDelete["contents"] as List<*>

        foundSubDir = false
        for (content in contentsAfterDelete) {
            val contentMap = content as Map<*, *>
            if (contentMap["id"] == subDirId) {
                foundSubDir = true
                break
            }
        }

        assertFalse(foundSubDir, "Sub-directory should no longer exist after deletion")
    }

    @Test
    fun `test directory and file operations integration`() {
        // 創建測試目錄
        val dirName = "test_dir_files_${System.currentTimeMillis()}"
        val createDirResultJson = driveOperations.createDirectory(dirName)
        val createDirResult = gson.fromJson(createDirResultJson, Map::class.java)
        assertTrue(createDirResult["success"] as Boolean, "Directory creation should be successful")

        val directoryId = createDirResult["directoryId"] as String
        testDirectoryIds.add(directoryId)  // 添加到清理列表

        // 創建測試文件
        val fileName = "test_file_in_dir_${System.currentTimeMillis()}.txt"
        val createFileResultJson = driveOperations.createNewFile(fileName, "Test content for file in directory")
        val createFileResult = gson.fromJson(createFileResultJson, Map::class.java)
        assertTrue(createFileResult["success"] as Boolean, "File creation should be successful")

        val fileId = createFileResult["fileId"] as String
        testFileIds.add(fileId)  // 添加到清理列表

        // 將文件移動到目錄中
        val moveResultJson = driveOperations.moveFile(fileId, directoryId)
        val moveResult = gson.fromJson(moveResultJson, Map::class.java)
        assertTrue(moveResult["success"] as Boolean, "Moving file to directory should be successful")

        // 列出目錄內容並確認文件存在
        val listDirResultJson = driveOperations.listDirectoryContents(directoryId)
        val listDirResult = gson.fromJson(listDirResultJson, Map::class.java)
        assertTrue(listDirResult["success"] as Boolean, "Listing directory contents should be successful")

        val dirContents = listDirResult["contents"] as List<*>
        assertEquals(1, dirContents.size, "Directory should contain exactly one file")

        val fileInDir = dirContents[0] as Map<*, *>
        assertEquals(fileId, fileInDir["id"], "File in directory should have correct ID")
        assertEquals(fileName, fileInDir["name"], "File in directory should have correct name")

        // 重命名目錄中的文件
        val newFileName = "renamed_file_${System.currentTimeMillis()}.txt"
        val renameResultJson = driveOperations.renameFile(fileId, newFileName)
        val renameResult = gson.fromJson(renameResultJson, Map::class.java)
        assertTrue(renameResult["success"] as Boolean, "Renaming file should be successful")

        // 確認文件已重命名
        val listAfterRenameJson = driveOperations.listDirectoryContents(directoryId)
        val listAfterRename = gson.fromJson(listAfterRenameJson, Map::class.java)
        val contentsAfterRename = listAfterRename["contents"] as List<*>

        val renamedFile = contentsAfterRename[0] as Map<*, *>
        assertEquals(newFileName, renamedFile["name"], "File should have new name after rename")
    }

    @Test
    fun `test root directory listing`() {
        // 測試列出根目錄內容
        val listRootResultJson = driveOperations.listDirectoryContents()
        val listRootResult = gson.fromJson(listRootResultJson, Map::class.java)
        assertTrue(listRootResult["success"] as Boolean, "Listing root directory contents should be successful")

        val rootContents = listRootResult["contents"] as List<*>
        assertNotNull(rootContents, "Root directory contents should not be null")

        // 確認返回的目錄ID是 "root"
        assertEquals("root", listRootResult["directoryId"], "Directory ID should be 'root'")
    }
}

// SSLUtil 類用於禁用測試中的 SSL 驗證
class SSLUtil {
    companion object {
        fun disableSSLVerification() {
            try {
                // 創建一個信任所有證書的信任管理器
                val trustAllCerts = arrayOf(object : javax.net.ssl.X509TrustManager {
                    override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
                    override fun checkClientTrusted(
                        certs: Array<java.security.cert.X509Certificate>,
                        authType: String
                    ) {
                    }

                    override fun checkServerTrusted(
                        certs: Array<java.security.cert.X509Certificate>,
                        authType: String
                    ) {
                    }
                })

                // 安裝自定義的 SSLSocketFactory
                val sc = javax.net.ssl.SSLContext.getInstance("SSL")
                sc.init(null, trustAllCerts, java.security.SecureRandom())
                javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)

                // 安裝自定義的 HostnameVerifier
                javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
