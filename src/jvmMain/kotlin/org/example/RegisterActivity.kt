package org.example

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: RegisterViewModel by viewModels {
        val api = Retrofit.Builder()
            .baseUrl("https://simple-note.amirsalarsafaei.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
        val repo = Repository(api)
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RegisterViewModel(repo) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener {
            viewModel.firstName.value = binding.edtFirstName.text.toString()
            viewModel.lastName.value = binding.edtLastName.text.toString()
            viewModel.username.value = binding.edtUsername.text.toString()
            viewModel.email.value = binding.edtEmail.text.toString()
            viewModel.password.value = binding.edtPassword.text.toString()
            viewModel.retypePassword.value = binding.edtRetypePassword.text.toString()
            viewModel.register()
        }

        viewModel.registerResult.observe(this) {
            it.onSuccess {
                Toast.makeText(this, "register", Toast.LENGTH_SHORT).show()
                finish() // بازگشت به صفحه ورود
            }.onFailure { e ->
                Toast.makeText(this, e.message ?: "error", Toast.LENGTH_SHORT).show()
            }
        }

        binding.txtBackToLogin.setOnClickListener {
            finish()
        }
    }
}