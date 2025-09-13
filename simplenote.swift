import Foundation
import FoundationNetworking

struct TokenPair: Decodable {
    let access: String
    let refresh: String
}

struct AccessOnly: Decodable {
    let access: String
}

struct MessageResponse: Decodable {
    let detail: String?
}

struct RegisterResponse: Decodable {
    let username: String
    let email: String?
    let firstName: String?
    let lastName: String?
}

struct UserInfo: Decodable {
    let id: Int?
    let username: String
    let email: String?
    let firstName: String?
    let lastName: String?
}

struct Note: Decodable {
    let id: Int
    let title: String
    let description: String
    let createdAt: String?
    let updatedAt: String?
    let creatorName: String?
    let creatorUsername: String?
}

struct PagedNotes: Decodable {
    let count: Int
    let next: String?
    let previous: String?
    let results: [Note]
}

enum ApiError: Error, CustomStringConvertible {
    case network(String)
    case decoding(String)
    case server(Int, String)

    var description: String {
        switch self {
        case .network(let msg): return "Network error: \(msg)"
        case .decoding(let msg): return "Decoding error: \(msg)"
        case .server(let code, let body): return "server(\(code), \"\(body)\")"
        }
    }
}

extension URLSession {
    func syncRequest(with req: URLRequest) throws -> (Data, URLResponse) {
        var result: Result<(Data, URLResponse), Error>?
        let sem = DispatchSemaphore(value: 0)
        let task = dataTask(with: req) { data, response, error in
            if let error = error {
                result = .failure(error)
            } else if let response = response, let data = data {
                result = .success((data, response))
            } else {
                result = .failure(NSError(domain: "ApiClient", code: -1, userInfo: [NSLocalizedDescriptionKey: "No data/response"]))
            }
            sem.signal()
        }
        task.resume()
        sem.wait()
        switch result {
        case .some(.success(let pair)):
            return pair
        case .some(.failure(let err)):
            throw err
        case .none:
            throw ApiError.network("no response")
        }
    }
}

class ApiClient {
    let baseURL: String
    private let decoder: JSONDecoder

    init(baseURL: String = "http://127.0.0.1:8000/api") {
        self.baseURL = baseURL
        self.decoder = JSONDecoder()
        self.decoder.keyDecodingStrategy = .convertFromSnakeCase
    }

    private func buildURL(path: String, query: [String: String]?) -> URL? {
        guard var comps = URLComponents(string: baseURL + path) else { return nil }
        if let q = query, !q.isEmpty {
            comps.queryItems = q.map { URLQueryItem(name: $0.key, value: $0.value) }
        }
        return comps.url
    }

    func performRequest<T: Decodable>(
        path: String,
        method: String = "GET",
        query: [String: String]? = nil,
        body: [String: Any]? = nil,
        token: String? = nil
    ) throws -> T {
        guard let url = buildURL(path: path, query: query) else {
            throw ApiError.network("Invalid URL for path \(path)")
        }

        var req = URLRequest(url: url)
        req.httpMethod = method
        if let body = body {
            req.httpBody = try JSONSerialization.data(withJSONObject: body, options: [])
            req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }
        if let token = token {
            req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        let (data, response) = try URLSession.shared.syncRequest(with: req)
        guard let http = response as? HTTPURLResponse else {
            throw ApiError.network("Invalid HTTP response")
        }

        let bodyString = String(data: data, encoding: .utf8) ?? ""

        if 200 ..< 300 ~= http.statusCode {
            if data.count == 0 {
                throw ApiError.decoding("Empty response body for type \(T.self)")
            }
            do {
                return try decoder.decode(T.self, from: data)
            } catch {
                throw ApiError.decoding("Failed to decode \(T.self): \(error.localizedDescription) — body: \(bodyString)")
            }
        } else {
            throw ApiError.server(http.statusCode, bodyString)
        }
    }

    func performRequestNoDecode(
        path: String,
        method: String = "DELETE",
        query: [String: String]? = nil,
        body: [String: Any]? = nil,
        token: String? = nil
    ) throws -> HTTPURLResponse {
        guard let url = buildURL(path: path, query: query) else {
            throw ApiError.network("Invalid URL for path \(path)")
        }

        var req = URLRequest(url: url)
        req.httpMethod = method
        if let body = body {
            req.httpBody = try JSONSerialization.data(withJSONObject: body, options: [])
            req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }
        if let token = token {
            req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        let (data, response) = try URLSession.shared.syncRequest(with: req)
        guard let http = response as? HTTPURLResponse else {
            throw ApiError.network("Invalid HTTP response")
        }

        if 200 ..< 300 ~= http.statusCode || http.statusCode == 204 {
            return http
        } else {
            let bodyString = String(data: data, encoding: .utf8) ?? ""
            throw ApiError.server(http.statusCode, bodyString)
        }
    }
}

class AuthManager {
    private let api: ApiClient

    init(apiClient: ApiClient = ApiClient()) {
        self.api = apiClient
    }

    func login(username: String, password: String) throws -> TokenPair {
        let body: [String: Any] = [
            "username": username,
            "password": password
        ]
        return try api.performRequest(path: "/auth/token/", method: "POST", body: body, token: nil)
    }

    func register(firstName: String, lastName: String, username: String, email: String, password: String) throws -> RegisterResponse {
        let body: [String: Any] = [
            "username": username,
            "password": password,
            "email": email,
            "first_name": firstName,
            "last_name": lastName
        ]
        return try api.performRequest(path: "/auth/register/", method: "POST", body: body, token: nil)
    }

    func refreshToken(_ refresh: String) throws -> String {
        let body: [String: Any] = ["refresh": refresh]
        let accessOnly: AccessOnly = try api.performRequest(path: "/auth/token/refresh/", method: "POST", body: body, token: nil)
        return accessOnly.access
    }

    func getUserInfo(accessToken: String) throws -> UserInfo {
        return try api.performRequest(path: "/auth/userinfo/", method: "GET", token: accessToken)
    }

    func changePassword(accessToken: String, oldPassword: String, newPassword: String) throws -> MessageResponse {
        let body: [String: Any] = [
            "old_password": oldPassword,
            "new_password": newPassword
        ]
        return try api.performRequest(path: "/auth/change-password/", method: "POST", body: body, token: accessToken)
    }
}

class NoteRepository {
    private let api: ApiClient

    init(apiClient: ApiClient = ApiClient()) {
        self.api = apiClient
    }

    func fetchNotesPaged(token: String, page: Int = 1, pageSize: Int? = nil) throws -> PagedNotes {
        var query: [String: String] = ["page": "\(page)"]
        if let ps = pageSize { query["page_size"] = "\(ps)" }
        return try api.performRequest(path: "/notes/", method: "GET", query: query.isEmpty ? nil : query, token: token)
    }

    func searchNotes(token: String, queryString: String, page: Int = 1, pageSize: Int? = nil) throws -> PagedNotes {
        var query: [String: String] = ["title": queryString, "page": "\(page)"]
        if let ps = pageSize { query["page_size"] = "\(ps)" }
        return try api.performRequest(path: "/notes/filter", method: "GET", query: query, token: token)
    }

    func fetchNoteById(token: String, id: Int) throws -> Note {
        return try api.performRequest(path: "/notes/\(id)/", method: "GET", token: token)
    }

    func createNote(token: String, title: String, description: String) throws -> Note {
        let body: [String: Any] = ["title": title, "description": description]
        return try api.performRequest(path: "/notes/", method: "POST", body: body, token: token)
    }

    func updateNote(token: String, id: Int, title: String, description: String) throws -> Note {
        let body: [String: Any] = ["title": title, "description": description]
        return try api.performRequest(path: "/notes/\(id)/", method: "PUT", body: body, token: token)
    }

    func patchNote(token: String, id: Int, title: String?, description: String?) throws -> Note {
        var body: [String: Any] = [:]
        if let t = title { body["title"] = t }
        if let d = description { body["description"] = d }
        return try api.performRequest(path: "/notes/\(id)/", method: "PATCH", body: body, token: token)
    }

    func deleteNote(token: String, id: Int) throws -> Bool {
        _ = try api.performRequestNoDecode(path: "/notes/\(id)/", method: "DELETE", token: token)
        return true
    }

    func bulkCreate(token: String, items: [[String: Any]]) throws -> [Note] {
        return try api.performRequest(path: "/notes/bulk", method: "POST", body: ["items": items], token: token)
    }
}

func printServerErrorBodyIfAvailable(_ error: Error) {
    if let api = error as? ApiError {
        print("Error: \(api.description)")
    } else {
        print("Error: \(error.localizedDescription)")
    }
}

func main() {
    let apiClient = ApiClient()
    let authManager = AuthManager(apiClient: apiClient)
    let repo = NoteRepository(apiClient: apiClient)

    var accessToken: String? = nil
    var refreshToken: String? = nil

    print("""
    Available commands:
    register              - Register a new account
    login                 - Login with username and password
    list [page]           - Show notes list (paginated)
    search <query> [page] - Search notes by title
    get <id>              - Show note by ID
    create <title> <desc> - Add a new note
    update <id> <title> <desc> - Update note
    patch <id> [title] [desc] - Patch note (partial update)
    delete <id>           - Delete note
    settings              - View profile / change password / logout
    exit                  - Exit program
    """)

    mainLoop: while true {
        print("> ", terminator: "")
        guard let raw = readLine() else { break }
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty { continue }
        if trimmed == "exit" { break }

        let parts = trimmed.split(separator: " ", maxSplits: 3, omittingEmptySubsequences: true).map(String.init)
        let command = parts.first ?? ""

        do {
            switch command {
            case "register":
                print("First name: ", terminator: ""); let first = readLine() ?? ""
                print("Last name: ", terminator: ""); let last = readLine() ?? ""
                print("Username: ", terminator: ""); let username = readLine() ?? ""
                print("Email: ", terminator: ""); let email = readLine() ?? ""
                print("Password: ", terminator: ""); let password = readLine() ?? ""
                print("Retype password: ", terminator: ""); let retype = readLine() ?? ""
                if password != retype {
                    print("Passwords do not match")
                    continue
                }
                let resp = try authManager.register(firstName: first, lastName: last, username: username, email: email, password: password)
                print("Registered: \(resp.username)")

            case "login":
                print("Username: ", terminator: ""); let username = readLine() ?? ""
                print("Password: ", terminator: ""); let password = readLine() ?? ""
                let tokens = try authManager.login(username: username, password: password)
                accessToken = tokens.access
                refreshToken = tokens.refresh
                print("Logged in successfully")

            case "list":
                guard let token = accessToken else { print("Please login first"); continue }
                let pageArg = parts.count > 1 ? Int(parts[1]) ?? 1 : 1
                let resp = try repo.fetchNotesPaged(token: token, page: pageArg)
                if resp.results.isEmpty {
                    print("No notes found.")
                } else {
                    resp.results.forEach { n in
                        print("[\(n.id)] \(n.title)")
                    }
                    print("Page \(pageArg) / total \(resp.count)")
                }

            case "search":
                guard let token = accessToken else { print("Please login first"); continue }
                if parts.count < 2 {
                    print("Usage: search <query> [page]")
                    continue
                }
                let queryString = parts[1]
                let pageArg = parts.count > 2 ? Int(parts[2]) ?? 1 : 1
                let resp = try repo.searchNotes(token: token, queryString: queryString, page: pageArg)
                if resp.results.isEmpty {
                    print("No results.")
                } else {
                    resp.results.forEach { n in print("[\(n.id)] \(n.title)") }
                    print("Page \(pageArg) / total \(resp.count)")
                }

            case "get":
                guard let token = accessToken else { print("Please login first"); continue }
                guard parts.count >= 2, let id = Int(parts[1]) else {
                    print("Usage: get <id>"); continue
                }
                let n = try repo.fetchNoteById(token: token, id: id)
                print("Note: [\(n.id)] \(n.title) / \(n.description)")

            case "create":
                guard let token = accessToken else { print("Please login first"); continue }
                if parts.count < 3 {
                    print("Usage: create <title> <desc>"); continue
                }
                let title = parts[1]
                let desc = parts[2]
                let note = try repo.createNote(token: token, title: title, description: desc)
                print("Created note: [\(note.id)] \(note.title)")

            case "update":
                guard let token = accessToken else { print("Please login first"); continue }
                if parts.count < 4 {
                    print("Usage: update <id> <title> <desc>"); continue
                }
                guard let id = Int(parts[1]) else { print("Invalid id"); continue }
                let title = parts[2]
                let desc = parts[3]
                let updated = try repo.updateNote(token: token, id: id, title: title, description: desc)
                print("Updated: [\(updated.id)] \(updated.title)")

            case "patch":
                guard let token = accessToken else { print("Please login first"); continue }
                let rawParts = trimmed.split(separator: " ").map(String.init)
                guard rawParts.count >= 2, let id = Int(rawParts[1]) else { print("Usage: patch <id> [title] [desc]"); continue }
                let title: String? = (rawParts.count >= 3) ? rawParts[2] : nil
                let desc: String? = (rawParts.count >= 4) ? rawParts[3] : nil
                let patched = try repo.patchNote(token: token, id: id, title: title, description: desc)
                print("Patched: [\(patched.id)] \(patched.title)")

            case "delete":
                guard let token = accessToken else { print("Please login first"); continue }
                guard parts.count >= 2, let id = Int(parts[1]) else { print("Usage: delete <id>"); continue }
                _ = try repo.deleteNote(token: token, id: id)
                print("Deleted.")

            case "settings":
                guard let token = accessToken else { print("Please login first"); continue }
                print("""
                Settings:
                profile          - Show profile info
                change-password  - Change your password
                logout           - Logout
                back             - Return
                """)
                settingsLoop: while true {
                    print("settings> ", terminator: "")
                    guard let s = readLine()?.trimmingCharacters(in: .whitespacesAndNewlines), !s.isEmpty else { continue }
                    if s == "back" { break settingsLoop }
                    switch s {
                    case "profile":
                        do {
                            let info = try authManager.getUserInfo(accessToken: token)
                            print("Profile:")
                            print(" id: \(info.id.map(String.init) ?? "n/a")")
                            print(" username: \(info.username)")
                            print(" email: \(info.email ?? "n/a")")
                            print(" first name: \(info.firstName ?? "n/a")")
                            print(" last name: \(info.lastName ?? "n/a")")
                        } catch {
                            printServerErrorBodyIfAvailable(error)
                        }
                    case "change-password":
                        print("Current password: ", terminator: ""); let old = readLine() ?? ""
                        print("New password: ", terminator: ""); let newp = readLine() ?? ""
                        print("Retype new password: ", terminator: ""); let re = readLine() ?? ""
                        if newp != re {
                            print("New passwords do not match")
                            continue
                        }
                        do {
                            let msg = try authManager.changePassword(accessToken: token, oldPassword: old, newPassword: newp)
                            print("Password changed: \(msg.detail ?? "OK")")
                        } catch {
                            printServerErrorBodyIfAvailable(error)
                        }
                    case "logout":
                        accessToken = nil
                        refreshToken = nil
                        print("Logged out")
                        break settingsLoop
                    default:
                        print("Unknown settings command")
                    }
                }

            default:
                print("Unknown command: \(command)")
            }
        } catch let apiErr as ApiError {
            print("Error: \(apiErr.description)")
            if case let ApiError.server(code, _) = apiErr, code == 401 {
                if let refresh = refreshToken {
                    do {
                        let newAccess = try authManager.refreshToken(refresh)
                        accessToken = newAccess
                        print("Access token refreshed. Try your command again.")
                    } catch {
                        print("Refresh failed: \(error)")
                        accessToken = nil
                        refreshToken = nil
                    }
                } else {
                    print("No refresh token available; please login again.")
                }
            }
        } catch {
            print("Error: \(error.localizedDescription)")
            if let refresh = refreshToken {
                do {
                    let newAccess = try authManager.refreshToken(refresh)
                    accessToken = newAccess
                    print("Access token refreshed. Try your command again.")
                } catch {
                    print("Refresh failed: \(error)")
                    accessToken = nil
                    refreshToken = nil
                }
            }
        }
    }

    print("Exiting...")
}

main()
