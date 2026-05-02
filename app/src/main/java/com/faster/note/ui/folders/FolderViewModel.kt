package com.faster.note.ui.folders

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faster.note.NoteApp
import com.faster.note.data.db.entity.FolderEntity
import com.faster.note.data.db.entity.TagEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FolderViewModel(application: Application) : AndroidViewModel(application) {
    private val folderRepo = (application as NoteApp).folderRepository
    private val tagRepo = (application as NoteApp).tagRepository

    val folders: StateFlow<List<FolderEntity>> = folderRepo.allFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags: StateFlow<List<TagEntity>> = tagRepo.allTags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createFolder(name: String, color: Int) = viewModelScope.launch {
        folderRepo.saveFolder(FolderEntity(name = name, color = color))
    }

    fun deleteFolder(folder: FolderEntity) = viewModelScope.launch {
        folderRepo.deleteFolder(folder)
    }

    fun createTag(name: String) = viewModelScope.launch {
        tagRepo.saveTag(TagEntity(name = name))
    }

    fun deleteTag(tag: TagEntity) = viewModelScope.launch {
        tagRepo.deleteTag(tag)
    }
}
