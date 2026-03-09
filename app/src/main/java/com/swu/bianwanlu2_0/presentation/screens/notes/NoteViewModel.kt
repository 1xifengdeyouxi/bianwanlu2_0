package com.swu.bianwanlu2_0.presentation.screens.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swu.bianwanlu2_0.data.local.entity.Note
import com.swu.bianwanlu2_0.data.repository.NoteRepository
import com.swu.bianwanlu2_0.utils.GUEST_USER_ID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val repository: NoteRepository
) : ViewModel() {

    val notes: StateFlow<List<Note>> = repository
        .getAllNotes(GUEST_USER_ID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val noteCount: StateFlow<Int> = repository
        .countNotes(GUEST_USER_ID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun addNote(title: String, content: String) {
        if (title.isBlank() && content.isBlank()) return
        viewModelScope.launch {
            repository.insert(
                Note(
                    title = title.trim(),
                    content = content.trim(),
                    userId = GUEST_USER_ID
                )
            )
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.delete(note)
        }
    }
}
