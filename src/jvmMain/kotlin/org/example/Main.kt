package org.example

import kotlinx.coroutines.runBlocking
import java.util.Scanner

fun main() = runBlocking {
    val scanner = Scanner(System.`in`)
    val authManager = AuthManager()
    val repo = NoteRepository()

    var accessToken: String? = null
    var refreshToken: String? = null

    println(
        """
        ✨ Available commands:
        register              - Register a new account
        login                 - Login with username and password
        list [page]           - Show notes list (paginated)
        search <query> [page] - Search notes by title
        get <id>              - Show note by ID
        create <title> <desc> - Add a new note
        update <id> <title> <desc> - Update note
        delete <id>           - Delete note
        settings              - View profile / change password / logout
        exit                  - Exit program
        """.trimIndent()
    )

    loop@ while (true) {
        print("> ")
        val input = scanner.nextLine().trim()
        if (input == "exit") break

        val parts = input.split(" ", limit = 4)
        val command = parts.getOrNull(0)

        try {
            when (command) {
                "register" -> {
                    print("First name: "); val firstName = scanner.nextLine().trim()
                    print("Last name: "); val lastName = scanner.nextLine().trim()
                    print("Username: "); val username = scanner.nextLine().trim()
                    print("Email: "); val email = scanner.nextLine().trim()
                    print("Password: "); val password = scanner.nextLine().trim()
                    print("Retype password: "); val retype = scanner.nextLine().trim()

                    val result = authManager.register(firstName, lastName, username, email, password, retype)
                    result.onSuccess { println("✅ Registered successfully.") }
                        .onFailure { e -> println("❌ Register failed: ${e.message}") }
                }

                "login" -> {
                    print("Username: "); val username = scanner.nextLine().trim()
                    print("Password: "); val password = scanner.nextLine().trim()

                    val result = authManager.login(username, password)
                    result.onSuccess { tokens ->
                        accessToken = "Bearer ${tokens.access}"
                        refreshToken = tokens.refresh
                        println("✅ Logged in successfully")
                    }.onFailure { e -> println("❌ Login failed: ${e.message}") }
                }

                "list" -> {
                    if (accessToken == null) { println("❌ Please login first"); continue }
                    val page = parts.getOrNull(1)?.toIntOrNull() ?: 1
                    val response = repo.fetchNotesPaged(accessToken!!, page)
                    if (response.results.isEmpty()) {
                        println("No notes found.")
                    } else {
                        response.results.forEach { println("• [${it.id}] ${it.title}") }
                        println("Page $page / total ${response.count}")
                    }
                }

                "search" -> {
                    if (accessToken == null) { println("❌ Please login first"); continue }
                    val query = parts.getOrNull(1)
                    if (query == null) {
                        println("❌ Usage: search <query> [page]")
                        continue
                    }
                    val page = parts.getOrNull(2)?.toIntOrNull() ?: 1
                    val response = repo.searchNotes(accessToken!!, query, page)
                    if (response.results.isEmpty()) {
                        println("No results.")
                    } else {
                        response.results.forEach { println("🔎 [${it.id}] ${it.title}") }
                        println("Page $page / total ${response.count}")
                    }
                }

                "get" -> {
                    if (accessToken == null) { println("❌ Please login first"); continue }
                    val id = parts.getOrNull(1)?.toIntOrNull()
                    if (id == null) {
                        println("❌ Usage: get <id>")
                        continue
                    }
                    val note = repo.fetchNoteById(accessToken!!, id)
                    println("✅ Note: ${note.title} / ${note.content}")
                }

                "create" -> {
                    if (accessToken == null) { println("❌ Please login first"); continue }
                    val title = parts.getOrNull(1)
                    val desc = parts.getOrNull(2)
                    if (title == null || desc == null) {
                        println("❌ Usage: create <title> <desc>")
                        continue
                    }
                    val note = repo.createNote(accessToken!!, title, desc)
                    println("✅ Created note: [${note.id}] ${note.title}")
                }

                "update" -> {
                    if (accessToken == null) { println("❌ Please login first"); continue }
                    val id = parts.getOrNull(1)?.toIntOrNull()
                    val title = parts.getOrNull(2)
                    val desc = parts.getOrNull(3)
                    if (id == null || title == null || desc == null) {
                        println("❌ Usage: update <id> <title> <desc>")
                        continue
                    }
                    val note = repo.updateNote(accessToken!!, id, title, desc)
                    println("✅ Updated: ${note.title}")
                }

                "delete" -> {
                    if (accessToken == null) { println("❌ Please login first"); continue }
                    val id = parts.getOrNull(1)?.toIntOrNull()
                    if (id == null) {
                        println("❌ Usage: delete <id>")
                        continue
                    }
                    val success = repo.deleteNote(accessToken!!, id)
                    println(if (success) "✅ Deleted." else "❌ Failed to delete.")
                }

                "settings" -> {
                    if (accessToken == null) { println("❌ Please login first"); continue }
                    println(
                        """
                        ⚙️ Settings:
                        profile          - Show profile info
                        change-password  - Change your password
                        logout           - Logout
                        back             - Return
                        """.trimIndent()
                    )

                    while (true) {
                        print("settings> ")
                        val sInput = scanner.nextLine().trim()
                        if (sInput == "back") break

                        when (sInput) {
                            "profile" -> {
                                println("👤 Profile (dummy for now): username only visible in UI normally.")
                            }

                            "change-password" -> {
                                print("Current password: "); val current = scanner.nextLine().trim()
                                print("New password: "); val newPass = scanner.nextLine().trim()
                                print("Retype new password: "); val retype = scanner.nextLine().trim()

                                if (newPass != retype) {
                                    println("❌ New passwords do not match")
                                } else {
                                    // TODO: hook up real API if available
                                    println("✅ Password change simulated (API call needed).")
                                }
                            }

                            "logout" -> {
                                accessToken = null
                                refreshToken = null
                                println("👋 Logged out")
                                break
                            }

                            else -> println("❌ Unknown settings command")
                        }
                    }
                }

                else -> println("❌ Unknown command: $command")
            }
        } catch (e: Exception) {
            println("❌ Error: ${e.message}")

            // try refresh token on error (like 401 Unauthorized)
            if (refreshToken != null) {
                val refreshed = authManager.refreshToken(refreshToken!!)
                refreshed.onSuccess { newAccess ->
                    accessToken = "Bearer $newAccess"
                    println("🔄 Access token refreshed. Try your command again.")
                }.onFailure { err ->
                    println("❌ Refresh failed: ${err.message}")
                    accessToken = null
                    refreshToken = null
                }
            }
        }
    }

    println("👋 Exiting...")
}
