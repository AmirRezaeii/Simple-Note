package org.example.ui

class LoginViewModel(private val repository: Repository) : ViewModel() {
    val email = MutableLiveData<String>()
    val password = MutableLiveData<String>()
    val loginResult = MutableLiveData<Result<String>>()

    fun login() {
        val e = email.value ?: ""
        val p = password.value ?: ""
        if (e.isEmpty() || p.isEmpty()) {
            loginResult.value = Result.failure(Throwable("Please fill all fields"))
            return
        }
        repository.login(e, p).observeForever {
            loginResult.value = it
        }
    }
}