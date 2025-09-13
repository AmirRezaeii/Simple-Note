package org.example

class HomeViewModel(private val repo: Repository) : ViewModel() {
    private val _notes = MutableLiveData<Result<List<Note>>>()
    val notes: LiveData<Result<List<Note>>> = _notes

    fun loadNotes(token: String, page: Int = 1) {
        repo.getNotes(token, page).observeForever {
            _notes.value = it
        }
    }
}