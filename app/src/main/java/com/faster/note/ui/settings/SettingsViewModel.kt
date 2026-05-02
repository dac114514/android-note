package com.faster.note.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faster.note.NoteApp
import com.faster.note.util.BackupManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as NoteApp).noteRepository
    val backupManager = BackupManager(application)

    private val _backupFiles = MutableStateFlow<List<File>>(emptyList())
    val backupFiles: StateFlow<List<File>> = _backupFiles.asStateFlow()

    private val _exporting = MutableStateFlow(false)
    val exporting: StateFlow<Boolean> = _exporting.asStateFlow()

    fun refreshBackupFiles() {
        _backupFiles.value = backupManager.getBackupFiles()
    }

    fun exportAllNotes() {
        viewModelScope.launch {
            _exporting.value = true
            repo.allNotes.first().let { notes ->
                backupManager.exportBackup(notes)
            }
            refreshBackupFiles()
            _exporting.value = false
        }
    }
}
