package org.example

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var adapter: NotesAdapter
    private lateinit var token: String
    private val viewModel: HomeViewModel by viewModels {
        val api = Retrofit.Builder()
            .baseUrl("https://simple-note.amirsalarsafaei.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
        val repo = Repository(api)
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(repo) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        token = getToken() ?: run {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        adapter = NotesAdapter(mutableListOf())
        binding.recyclerNotes.adapter = adapter

        viewModel.notes.observe(this) {
            it.onSuccess { list ->
                adapter.setNotes(list)
            }.onFailure {
                Toast.makeText(this, "error", Toast.LENGTH_SHORT).show()
            }
        }
        viewModel.loadNotes(token)

        binding.btnAddNote.setOnClickListener {
            startActivity(Intent(this, NoteCreationActivity::class.java))
        }

        binding.edtSearch.addTextChangedListener {
            val query = it.toString()
            adapter.filter(query)
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun getToken(): String? {
        val prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        return prefs.getString("JWT_TOKEN", null)
    }
}