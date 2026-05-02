package com.faster.note.ui.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faster.note.NoteApp
import com.faster.note.data.db.entity.NoteEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NoteListViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as NoteApp).noteRepository

    private val _filter by lazy { MutableStateFlow<NoteFilter>(NoteFilter.All) }
    val notes: StateFlow<List<NoteEntity>> = _filter.flatMapLatest { filter ->
        when (filter) {
            is NoteFilter.All -> repo.allNotes
            is NoteFilter.Folder -> repo.getNotesByFolder(filter.folderId)
            is NoteFilter.Tag -> repo.getNotesByTag(filter.tagId)
            is NoteFilter.Favorite -> repo.getFavoriteNotes()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(filter: NoteFilter) { _filter.value = filter }

    fun deleteNote(id: Long) = viewModelScope.launch { repo.deleteNote(id) }

    fun toggleFavorite(note: NoteEntity) {
        viewModelScope.launch { repo.updateNote(note.copy(isFavorite = !note.isFavorite)) }
    }
}

sealed class NoteFilter {
    data object All : NoteFilter()
    data class Folder(val folderId: Long) : NoteFilter()
    data class Tag(val tagId: Long) : NoteFilter()
    data object Favorite : NoteFilter()
}
