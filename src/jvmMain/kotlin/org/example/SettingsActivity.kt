package org.example

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: SettingsViewModel by viewModels {
        val api = Retrofit.Builder()
            .baseUrl("https://simple-note.amirsalarsafaei.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
        val repo = Repository(api)
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(repo) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.loadUser()

        viewModel.userLiveData.observe(this) { user ->
            if (user != null) {
                binding.tvUserName.text = "${user.firstName} ${user.lastName}"
                binding.tvUserEmail.text = user.email
            }
        }

        binding.btnChangePassword.setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmDialog()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun showLogoutConfirmDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out from the application?")
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Yes") { _, _ ->
                clearUserData()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .show()
    }

    private fun clearUserData() {
        val prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}