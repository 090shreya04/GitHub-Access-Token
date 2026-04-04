# GitHub Access Report Service

A Spring Boot service that connects to GitHub and generates a structured access report showing **which users have access to which repositories** within a given organization.

---

## 🚀 How to Run the Project

### Prerequisites
- Java 17+
- Maven 3.8+
- A GitHub Personal Access Token (PAT)

### 1. Clone the Repository
```bash
git clone https://github.com/090shreya04/GitHub-Access-Token.git
cd GitHub-Access-Token
```

### 2. Set Your GitHub Token (Environment Variable)
**Linux / macOS:**
```bash
export GITHUB_TOKEN=<your_token>
```

**Windows PowerShell:**
```powershell
$env:GITHUB_TOKEN="<your_token>"
```

**Windows CMD:**
```cmd
set GITHUB_TOKEN=<your_token>
```

### 3. Run the Application
```bash
mvn spring-boot:run
```

The server starts at: `http://localhost:8082`

---

## 🔐 How Authentication is Configured

- The app uses a **GitHub Personal Access Token (PAT)** passed via the `Authorization: Bearer <token>` header.
- The token is read from the `GITHUB_TOKEN` **environment variable** — it is **never hardcoded** in any file.
- The token value must never be committed to Git. The `.gitignore` blocks `application-local.properties` and `.env` files.

### Required Token Scopes
| Scope | Purpose |
|-------|---------|
| `repo` | Access private repositories |
| `read:org` | Read organization members and teams |
| `read:user` | Read user profile data |

> **Note**: Without `read:org` and admin access, the service gracefully falls back to **contributors** (public data) instead of collaborators.

---

## 📡 How to Call the API Endpoint

**Method:** `GET`  
**URL:** `/api/reports/access`  
**Query Parameter:** `org` — The GitHub organization name

### Example Request
```bash
curl "http://localhost:8082/api/reports/access?org=spring-projects"
```

### Example Response
```json
{
  "organization": "spring-projects",
  "totalUsers": 2,
  "totalRepositories": 3,
  "generatedAt": "2026-04-05T10:00:00",
  "userAccessMappings": [
    {
      "user": {
        "login": "alice",
        "id": 12345
      },
      "repositories": [
        {
          "id": 1,
          "name": "spring-framework",
          "fullName": "spring-projects/spring-framework",
          "permission": "write",
          "accessType": "direct",
          "teamName": null
        },
        {
          "id": 2,
          "name": "spring-boot",
          "fullName": "spring-projects/spring-boot",
          "permission": "admin",
          "accessType": "team",
          "teamName": "core-team"
        }
      ]
    }
  ]
}
```

### Error Responses

| Scenario | HTTP Status | Example Message |
|----------|-------------|-----------------|
| Organization not found | `404 Not Found` | `"Organization not found: badorgname"` |
| Invalid/expired token | `401 Unauthorized` | `"Invalid or expired GitHub token"` |
| Rate limit exceeded | `429 Too Many Requests` | `"GitHub API rate limit exceeded"` |
| Insufficient permissions | `502 Bad Gateway` | `"Access forbidden. Token may not have required permissions."` |

---

## ⚙️ Architecture & Design Decisions

### Overall Flow
```
Client Request → AccessReportController
                        ↓
                  ReportService
                        ↓
                  GitHubService (aggregation logic)
                        ↓
              GitHubApiClient (WebClient calls)
                        ↓
                  GitHub REST API
```

### Key Design Decisions

**1. Reactive Stack (WebFlux + WebClient)**  
Used Spring WebFlux's `WebClient` instead of `RestTemplate` for **non-blocking, concurrent API calls**. This allows fetching collaborators for multiple repos simultaneously, which is critical for orgs with 100+ repos.

**2. Concurrency Limit (flatMap with 5)**  
GitHub enforces secondary rate limits on concurrent requests. The `flatMap(..., 5)` operator limits how many repo fetches run in parallel, balancing speed vs. safety.

**3. Graceful Fallback: Collaborators → Contributors**  
The collaborators API requires admin access. For tokens without admin permissions, the service automatically falls back to fetching **contributors** (public data), so the endpoint always returns meaningful data.

**4. Team-based Access**  
In addition to direct collaborators, the service fetches **repository teams** and their members. This captures access that was granted via team membership, which is common in large organizations.

**5. Direct vs. Team Deduplication**  
If a user has both direct and team-based access to the same repo, the direct access takes priority and is shown only once.

**6. Access Levels**  
Permission levels (`admin`, `write`, `read`) are extracted from the GitHub API's collaborator response when available. Contributors default to `read`.

---

## 🧪 Running Tests

```bash
mvn test
```

Tests included:
- `GitHubServiceTest` — aggregation logic, fallback, empty org
- `ReportServiceTest` — metadata fields (totalUsers, totalRepositories, generatedAt)
- `AccessReportControllerTest` — REST endpoint responses

---

## 📁 Project Structure
```
src/main/java/com/yourname/githubreport/
├── GitHubReportApplication.java
├── client/
│   └── GitHubApiClient.java          # All GitHub API calls
├── config/
│   └── GitHubConfig.java             # WebClient configuration
├── controller/
│   ├── AccessReportController.java   # REST endpoint
│   └── GlobalExceptionHandler.java   # Centralized error handling
├── exception/
│   ├── GitHubApiException.java
│   ├── OrganizationNotFoundException.java
│   └── RateLimitException.java
├── model/
│   ├── AccessReport.java             # Final response model
│   ├── Repository.java               # Repo info
│   ├── RepositoryAccess.java         # Repo + permission + access type
│   ├── Team.java                     # GitHub team model
│   ├── User.java                     # User info + permissions map
│   └── UserAccessMapping.java        # User → list of repo accesses
└── service/
    ├── GitHubService.java            # Core aggregation logic
    └── ReportService.java            # Report assembly + metadata
```
