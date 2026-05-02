package com.faster.note.util

import android.content.Context
import com.faster.note.data.db.entity.NoteEntity
import org.json.JSONObject
import java.io.*
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry

class BackupManager(private val context: Context) {

    private val backupDir = File(context.getExternalFilesDir(null), "NoteApp/Backups")

    init { backupDir.mkdirs() }

    fun exportBackup(notes: List<NoteEntity>): File {
        val timestamp = System.currentTimeMillis()
        val zipFile = File(backupDir, "note_backup_$timestamp.zip")
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            val meta = JSONObject().apply {
                put("version", 1)
                put("exportedAt", timestamp)
                put("noteCount", notes.size)
            }
            zos.putNextEntry(ZipEntry("notes.json"))
            zos.write(meta.toString(2).toByteArray())
            zos.closeEntry()

            notes.forEach { note ->
                val fileName = "notes/${note.id}.html"
                zos.putNextEntry(ZipEntry(fileName))
                val content = """
                    <html><head><title>${note.title}</title></head>
                    <body>${note.content}</body></html>
                """.trimIndent()
                zos.write(content.toByteArray())
                zos.closeEntry()
            }
        }
        return zipFile
    }

    fun getBackupFiles(): List<File> {
        return backupDir.listFiles { f -> f.extension == "zip" }?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }
}
