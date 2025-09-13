package org.example

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://simple-note.amirsalarsafaei.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(ApiService::class.java)
        val repo = Repository(api)
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LoginViewModel(repo) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            viewModel.email.value = binding.edtEmail.text.toString()
            viewModel.password.value = binding.edtPassword.text.toString()
            viewModel.login()
        }

        viewModel.loginResult.observe(this) { result ->
            result.onSuccess { token ->
                Toast.makeText(this, "Login Success", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }
            result.onFailure { e ->
                Toast.makeText(this, e.message ?: "Login failed", Toast.LENGTH_SHORT).show()
            }
        }

        binding.txtGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}