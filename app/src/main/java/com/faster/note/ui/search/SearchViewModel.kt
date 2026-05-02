package com.faster.note.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faster.note.NoteApp
import com.faster.note.data.db.entity.NoteEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as NoteApp).noteRepository

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<NoteEntity>>(emptyList())
    val results: StateFlow<List<NoteEntity>> = _results.asStateFlow()

    private var searchJob: Job? = null

    fun updateQuery(q: String) {
        _query.value = q
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            if (q.isBlank()) {
                _results.value = emptyList()
                return@launch
            }
            repo.allNotes.first().let { all ->
                _results.value = all.filter {
                    it.title.contains(q, ignoreCase = true) ||
                    it.content.contains(q, ignoreCase = true)
                }
            }
        }
    }
}
