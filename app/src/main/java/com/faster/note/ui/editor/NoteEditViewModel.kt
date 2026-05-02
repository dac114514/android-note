package com.faster.note.ui.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faster.note.NoteApp
import com.faster.note.data.db.entity.NoteEntity
import com.faster.note.data.db.entity.TagEntity
import com.faster.note.ui.editor.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NoteEditViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as NoteApp).noteRepository
    private val tagRepo = (application as NoteApp).tagRepository

    private var _noteId: Long? = null
    val noteId: Long? get() = _noteId

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _htmlContent = MutableStateFlow("")
    val htmlContent: StateFlow<String> = _htmlContent.asStateFlow()

    private val _folderId = MutableStateFlow<Long?>(null)
    val folderId: StateFlow<Long?> = _folderId.asStateFlow()

    private val _noteColor = MutableStateFlow<Int?>(null)
    val noteColor: StateFlow<Int?> = _noteColor.asStateFlow()

    val tags: StateFlow<List<TagEntity>> = tagRepo.allTags.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val selectedTagIds: StateFlow<Set<Long>> = _noteId?.let { id ->
        repo.getTagIdsForNote(id).map { it.toSet() }.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet()
        )
    } ?: MutableStateFlow(emptySet())

    private var saveJob: Job? = null

    fun loadNote(id: Long) {
        _noteId = id
        viewModelScope.launch {
            val note = repo.getNoteById(id) ?: return@launch
            _title.value = note.title
            _htmlContent.value = note.content
            _folderId.value = note.folderId
            _noteColor.value = note.color
        }
    }

    fun updateTitle(t: String) { _title.value = t; scheduleSave() }
    fun updateContent(c: String) { _htmlContent.value = c; scheduleSave() }
    fun updateFolder(id: Long?) { _folderId.value = id; scheduleSave() }
    fun updateColor(c: Int?) { _noteColor.value = c; scheduleSave() }

    fun toggleTag(tagId: Long) {
        val current = selectedTagIds.value.toMutableSet()
        if (tagId in current) current.remove(tagId) else current.add(tagId)
        (selectedTagIds as MutableStateFlow).value = current
        scheduleSave()
    }

    fun createTag(name: String) = viewModelScope.launch {
        tagRepo.saveTag(TagEntity(name = name))
    }

    fun saveNow() = viewModelScope.launch { performSave() }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(500)
            performSave()
        }
    }

    private suspend fun performSave() {
        val id = _noteId
        val note = NoteEntity(
            id = id ?: 0,
            title = _title.value,
            content = _htmlContent.value,
            folderId = _folderId.value,
            color = _noteColor.value,
            updatedAt = System.currentTimeMillis()
        )
        val savedId = if (id != null) {
            repo.updateNote(note)
            id
        } else {
            val newId = repo.saveNote(note)
            _noteId = newId
            newId
        }
        if (selectedTagIds.value.isNotEmpty()) {
            repo.setTagsForNote(savedId, selectedTagIds.value.toList())
        }
    }
}
