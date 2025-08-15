# Kit Registry: C Package Manager Design Document

## Project Overview

### Vision
Create a modern package registry for C libraries that brings npm/cargo-style convenience to C development. The registry will be built using Apion (our Scala.js framework) with RDB (our Scala database project) as the backend, serving as both a practical tool for the C community and a showcase for both frameworks' capabilities.

### Core Problems Solved
- **Package Discovery**: Central registry for finding C libraries
- **Dependency Management**: Automated fetching and version resolution
- **Cross-Platform Support**: Works identically on desktop and embedded targets
- **Source-Based Distribution**: Avoids ABI compatibility issues by building from source

### Key Features
- **Git-Based Storage**: Packages stay on GitHub/GitLab, registry stores metadata only
- **User Accounts**: npm-style user registration and authentication
- **CLI Tool**: `kit` command for publishing and consuming packages
- **Web Interface**: Browse packages, view documentation, user profiles
- **Version Management**: Semantic versioning with dependency resolution

## Architecture Overview

### System Components

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Kit CLI       │    │  Kit Registry   │    │  Git Providers  │
│  (Scala Native) │◄──►│  (Apion+RDB)    │◄──►│  (GitHub/etc)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
        │                       │                       │
        │                       │                       │
        ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ Local Project   │    │   RDB Database  │    │ Source Code     │
│   (kit.toml)    │    │ (registry.sql)  │    │   Repos         │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### Technology Stack
- **Registry Server**: Apion (Scala.js + Node.js)
- **Database**: RDB (your Scala database project) - in-memory with SQL file persistence for development
- **CLI Tool**: Scala Native (for fast startup and easy distribution)
- **Frontend**: Server-rendered HTML + Progressive Enhancement
- **Hosting**: Your existing server infrastructure

## Data Models

### Core Entities

```scala
// User Management
case class User(
  id: String,                    // UUID
  email: String,
  username: String,              // Unique, URL-safe
  passwordHash: String,          // bcrypt
  apiKey: String,               // For CLI authentication
  emailVerified: Boolean = false,
  isActive: Boolean = true,
  createdAt: Instant,
  updatedAt: Instant
) derives JsonEncoder, JsonDecoder

// Package Metadata
case class Package(
  name: String,                  // Primary key, URL-safe
  displayName: Option[String],   // Pretty name for display
  owner: String,                 // User.username
  description: String,
  gitUrl: String,               // Source repository
  homepage: Option[String],
  documentation: Option[String],
  license: String,              // SPDX identifier
  keywords: List[String],       // For search
  categories: List[String],     // Organized grouping
  platforms: List[String],      // linux, windows, embedded, etc
  cStandard: Option[String],    // c99, c11, c17, c23
  isVerified: Boolean = false,  // Registry admin approval
  downloads: Long = 0,
  weeklyDownloads: Long = 0,
  isDeprecated: Boolean = false,
  deprecationMessage: Option[String],
  createdAt: Instant,
  updatedAt: Instant
) derives JsonEncoder, JsonDecoder

// Version Information
case class PackageVersion(
  packageName: String,           // Foreign key
  version: String,              // Semantic version
  gitTag: String,               // Exact git reference
  gitCommit: String,            // Commit SHA for verification
  description: Option[String],   // Version-specific notes
  dependencies: Map[String, String], // name -> version constraint
  devDependencies: Map[String, String],
  files: List[String],          // Main files (for header-only libs)
  sourceFiles: List[String],    // Source files to compile
  includeFiles: List[String],   // Additional includes
  buildInstructions: Option[String], // Custom build notes
  examples: List[String],       // Example file paths
  benchmarks: List[String],     // Benchmark file paths
  compatibility: PackageCompatibility,
  isPrerelease: Boolean,
  isYanked: Boolean = false,    // Hidden from resolution
  yankReason: Option[String],
  publishedAt: Instant,
  publishedBy: String           // User.username
) derives JsonEncoder, JsonDecoder

case class PackageCompatibility(
  cStandards: List[String],     // c99, c11, etc
  compilers: List[String],      // gcc, clang, msvc
  platforms: List[String],      // linux-x64, arm-cortex-m4, etc
  tested: Boolean = false       // Has CI test results
) derives JsonEncoder, JsonDecoder

// Download Statistics
case class DownloadStats(
  packageName: String,
  version: String,
  date: LocalDate,
  downloads: Long
) derives JsonEncoder, JsonDecoder

// User Package Ownership
case class PackageOwnership(
  packageName: String,
  username: String,
  role: OwnershipRole,          // Owner, Maintainer, Contributor
  grantedAt: Instant,
  grantedBy: String
) derives JsonEncoder, JsonDecoder

enum OwnershipRole derives JsonEncoder, JsonDecoder:
  case Owner, Maintainer, Contributor
```

## API Design

### Authentication
- **JWT tokens** for API access
- **API keys** for CLI authentication
- **Session cookies** for web interface

### REST API Endpoints

```scala
// Package Discovery
GET  /api/v1/packages                    // List/search packages
GET  /api/v1/packages/:name              // Get package metadata
GET  /api/v1/packages/:name/versions     // List package versions
GET  /api/v1/packages/:name/:version     // Get specific version
GET  /api/v1/packages/:name/stats        // Download statistics
GET  /api/v1/packages/:name/dependents   // Who depends on this package

// Package Management (Authenticated)
POST   /api/v1/packages                  // Publish new package
PUT    /api/v1/packages/:name            // Update package metadata
DELETE /api/v1/packages/:name            // Delete package (admin only)
POST   /api/v1/packages/:name/versions   // Publish new version
PATCH  /api/v1/packages/:name/:version   // Yank/unyank version
DELETE /api/v1/packages/:name/:version   // Delete version

// User Management
POST /api/v1/auth/register               // Create account
POST /api/v1/auth/login                  // Login (returns JWT)
POST /api/v1/auth/logout                 // Logout
GET  /api/v1/auth/profile                // Get current user
PUT  /api/v1/auth/profile                // Update profile
POST /api/v1/auth/reset-password         // Password reset
POST /api/v1/auth/verify-email           // Email verification

// User Profiles
GET  /api/v1/users/:username             // Public profile
GET  /api/v1/users/:username/packages    // User's packages

// Search and Discovery
GET  /api/v1/search                      // Full-text search
GET  /api/v1/categories                  // List categories
GET  /api/v1/categories/:category        // Packages in category
GET  /api/v1/trending                    // Trending packages
GET  /api/v1/new                        // Recently published

// Registry Metadata
GET  /api/v1/registry/stats              // Registry statistics
GET  /api/v1/registry/status             // Health check
```

### API Request/Response Examples

```json
// GET /api/v1/packages/dynamic_string
{
  "name": "dynamic_string",
  "displayName": "Dynamic String",
  "owner": "yourusername",
  "description": "Modern string library with reference counting",
  "gitUrl": "https://github.com/yourusername/dynamic_string.h",
  "homepage": "https://yourusername.github.io/dynamic_string.h",
  "license": "MIT OR Unlicense",
  "keywords": ["string", "unicode", "memory-safe"],
  "categories": ["text-processing", "data-structures"],
  "platforms": ["linux", "windows", "embedded"],
  "cStandard": "c11",
  "downloads": 1250,
  "weeklyDownloads": 85,
  "versions": ["0.1.0", "0.1.1", "0.2.0"],
  "latestVersion": "0.2.0",
  "createdAt": "2025-01-15T10:00:00Z",
  "updatedAt": "2025-01-20T14:30:00Z"
}

// POST /api/v1/packages/:name/versions
{
  "version": "0.2.0",
  "gitTag": "v0.2.0",
  "gitCommit": "abc123def456...",
  "description": "Added Unicode support and performance improvements",
  "dependencies": {
    "unity": "^2.5.0"
  },
  "files": ["dynamic_string.h"],
  "examples": ["examples/basic_usage.c", "examples/unicode_demo.c"],
  "compatibility": {
    "cStandards": ["c99", "c11", "c17"],
    "compilers": ["gcc", "clang"],
    "platforms": ["linux-x64", "windows-x64", "arm-cortex-m4"],
    "tested": true
  }
}
```

## CLI Design

### Kit Configuration

```toml
# ~/.kit/config.toml
[registry]
url = "https://kit-registry.example.com"
api_key = "kit_1234567890abcdef"

[user]
username = "yourusername"
email = "you@example.com"

[build]
default_c_standard = "c11"
default_compiler = "gcc"
target_dir = "build"
```

### Project Configuration

```toml
# kit.toml (in project root)
[package]
name = "dynamic_string"
version = "0.2.0"
description = "Modern string library with reference counting"
authors = ["Your Name <you@example.com>"]
license = "MIT OR Unlicense"
repository = "https://github.com/yourusername/dynamic_string.h"
homepage = "https://yourusername.github.io/dynamic_string.h"
documentation = "https://yourusername.github.io/dynamic_string.h/docs"
keywords = ["string", "unicode", "memory-safe"]
categories = ["text-processing"]

[package.metadata]
c_standard = "c11"
single_header = true
thread_safe = true

[dependencies]
# No dependencies for this library

[dev-dependencies]
unity = "^2.5.0"

[build]
# For single-header libraries
files = ["dynamic_string.h"]

# For multi-file libraries
# sources = ["src/*.c"]
# headers = ["include/*.h"]
# include_dirs = ["include"]

[build.embedded]
# Embedded-specific configuration
exclude_files = ["examples/*"]
defines = ["EMBEDDED_BUILD=1"]

[platforms]
desktop = ["linux", "macos", "windows"]
embedded = ["arm-cortex-m", "riscv", "esp32"]

[examples]
basic = { file = "examples/basic_usage.c", description = "Basic string operations" }
unicode = { file = "examples/unicode_demo.c", description = "Unicode handling" }

[tests]
runner = "unity"
files = ["test/*.c"]
```

### CLI Commands

```bash
# Package Management
kit new my_project                    # Create new project with kit.toml
kit init                             # Initialize kit.toml in existing project
kit add unity@2.5.0                  # Add dependency
kit remove unity                     # Remove dependency
kit install                          # Install all dependencies
kit update                           # Update dependencies
kit clean                            # Clean dependency cache

# Information
kit list                             # List installed dependencies
kit show unity                       # Show package information
kit search "string library"          # Search packages
kit deps                             # Show dependency tree

# Publishing
kit login                            # Login to registry
kit logout                           # Logout
kit publish                          # Publish current package
kit yank 0.1.0                      # Yank a version
kit unyank 0.1.0                    # Unyank a version

# User Management
kit whoami                           # Show current user
kit profile                          # Show user profile
kit packages                         # Show user's packages

# Project Building (optional convenience)
kit build                           # Build project using CMake/Make
kit test                            # Run tests
kit check                           # Validate kit.toml and dependencies
```

## Web Interface Design

### Page Structure

```
├── / (Homepage)
│   ├── Featured packages
│   ├── Search bar
│   ├── Recent activity
│   └── Getting started guide
│
├── /packages (Package listing)
│   ├── Search and filters
│   ├── Category navigation
│   └── Pagination
│
├── /packages/:name (Package detail)
│   ├── README/documentation
│   ├── Version history
│   ├── Dependencies
│   ├── Download statistics
│   └── Installation instructions
│
├── /users/:username (User profile)
│   ├── Published packages
│   ├── Profile information
│   └── Activity feed
│
├── /categories (Category listing)
├── /trending (Trending packages)
├── /new (Recently published)
│
└── /docs (Documentation)
    ├── Getting started
    ├── CLI reference
    ├── Package guidelines
    └── API documentation
```

### Key UI Components

```scala
// Server-rendered pages with Apion
def packageDetailPage(packageName: String): Handler = request =>
  for {
    pkg <- packageService.getPackage(packageName)
    versions <- packageService.getVersions(packageName)
    stats <- statsService.getDownloadStats(packageName)
    readme <- githubService.getReadme(pkg.gitUrl)
  } yield {
    val html = views.packageDetail(pkg, versions, stats, readme)
    html.asText.withHeader("Content-Type", "text/html")
  }

// Search with progressive enhancement
def searchPage(query: Option[String], category: Option[String]): Handler = request =>
  for {
    results <- searchService.search(query, category, page = 1)
    categories <- categoryService.listCategories()
  } yield {
    val html = views.searchResults(results, categories, query, category)
    html.asText.withHeader("Content-Type", "text/html")
  }
```

## Database Schema

### RDB Tables

```sql
-- Users
CREATE TABLE users (
  id UUID AUTO PRIMARY KEY,
  email TEXT NOT NULL,
  username TEXT NOT NULL,
  password_hash TEXT NOT NULL,
  api_key TEXT NOT NULL,
  email_verified BOOLEAN,
  is_active BOOLEAN,
  profile_data JSON,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

-- Packages
CREATE TABLE packages (
  name TEXT PRIMARY KEY,
  display_name TEXT,
  owner_id UUID NOT NULL, -- Foreign key to users(id)
  description TEXT NOT NULL,
  git_url TEXT NOT NULL,
  homepage TEXT,
  documentation TEXT,
  license TEXT,
  metadata JSON, -- Contains keywords, categories, platforms, etc.
  downloads BIGINT,
  weekly_downloads BIGINT,
  is_deprecated BOOLEAN,
  deprecation_message TEXT,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

-- Package Versions
CREATE TABLE package_versions (
  package_name TEXT NOT NULL, -- Foreign key to packages(name)
  version TEXT NOT NULL,
  git_tag TEXT NOT NULL,
  git_commit TEXT NOT NULL,
  description TEXT,
  version_metadata JSON, -- Contains dependencies, files, compatibility, etc.
  is_prerelease BOOLEAN,
  is_yanked BOOLEAN,
  yank_reason TEXT,
  published_at TIMESTAMP,
  published_by UUID, -- Foreign key to users(id)
  PRIMARY KEY (package_name, version)
);

-- Download Statistics
CREATE TABLE download_stats (
  package_name TEXT NOT NULL, -- Foreign key to packages(name)
  version TEXT,
  date TEXT NOT NULL, -- ISO date string
  downloads BIGINT,
  PRIMARY KEY (package_name, version, date)
);

-- Package Ownership
CREATE TABLE package_ownerships (
  package_name TEXT NOT NULL, -- Foreign key to packages(name)
  user_id UUID NOT NULL, -- Foreign key to users(id)
  role TEXT NOT NULL, -- 'owner', 'maintainer', 'contributor'
  granted_at TIMESTAMP,
  granted_by UUID, -- Foreign key to users(id)
  PRIMARY KEY (package_name, user_id)
);
```

## Development Workflow

### Database Management
- **Schema**: Defined in `registry-data.sql` 
- **Development**: In-memory RDB database loads from SQL file on startup
- **Schema Changes**: Update SQL file and restart server
- **Data Persistence**: Export/import via SQL commands during development
- **Version Control**: Entire database state is in git

### Adding Test Data
```sql
-- Add to registry-data.sql
INSERT INTO users VALUES 
  ('550e8400-e29b-41d4-a716-446655440000', 'you@example.com', 'yourusername', 
   '$2b$12$...', 'kit_abc123...', true, true, '{}', 
   '2025-01-01T00:00:00Z', '2025-01-01T00:00:00Z');

INSERT INTO packages VALUES 
  ('unity', 'Unity Test Framework', '550e8400-e29b-41d4-a716-446655440000',
   'Unit testing framework for C', 'https://github.com/ThrowTheSwitch/Unity',
   'https://www.throwtheswitch.org/unity', null, 'MIT',
   '{"keywords": ["testing", "unit-test"], "categories": ["testing"], "platforms": ["all"]}',
   0, 0, false, null, '2025-01-01T00:00:00Z', '2025-01-01T00:00:00Z');

INSERT INTO package_versions VALUES
  ('unity', '2.5.2', 'v2.5.2', 'abc123def456...',
   'Latest stable release',
   '{"dependencies": {}, "files": ["src/unity.c", "src/unity.h"], "compatibility": {"cStandards": ["c99", "c11"]}}',
   false, false, null, '2025-01-01T00:00:00Z', '550e8400-e29b-41d4-a716-446655440000');
```

### Server Initialization
```scala
// In your Apion server
object RegistryDB {
  implicit val db: DB = new MemoryDB
  
  def initialize(): Unit = {
    val initSQL = Source.fromResource("registry-data.sql").mkString
    executeSQL(initSQL)
    logger.info("Registry database initialized from registry-data.sql")
  }
}
```

## Implementation Plan

### Phase 1: Core Registry (Week 1-2)
- [ ] Basic Apion server setup with RDB integration
- [ ] Load registry-data.sql on startup
- [ ] User registration/authentication with RDB
- [ ] Package CRUD API with JSON metadata storage
- [ ] Basic CLI for publishing

### Phase 2: Package Management (Week 3-4)
- [ ] Version management with dependency resolution
- [ ] CLI install/update commands with kit.lock generation
- [ ] Search functionality using RDB's text search
- [ ] Download statistics tracking

### Phase 3: Web Interface (Week 5-6)
- [ ] Package browsing pages
- [ ] User profiles
- [ ] Search interface
- [ ] Documentation integration

### Phase 4: Advanced Features (Week 7-8)
- [ ] Email notifications
- [ ] Webhook integrations
- [ ] Package verification
- [ ] API rate limiting
- [ ] Admin interface

### Phase 5: Production Ready (Week 9-10)
- [ ] Monitoring and logging
- [ ] Backup and recovery (SQL export/import)
- [ ] Performance optimization using RDB features
- [ ] Security audit
- [ ] Launch preparation

## Technical Considerations

### Security
- **Input Validation**: Strict validation on all user inputs
- **Rate Limiting**: Prevent abuse of API endpoints
- **CSRF Protection**: For web interface forms
- **SQL Injection**: RDB's parameterized queries prevent SQL injection
- **Package Verification**: Validate Git URLs and tags
- **API Key Security**: Secure generation and storage

### Performance
- **Database Indexing**: Leverage RDB's search capabilities
- **Caching**: In-memory nature of RDB provides natural caching
- **CDN**: Static assets via CloudFlare
- **Pagination**: Large result sets
- **Search**: Use RDB's built-in text search features

### Scalability
- **Horizontal Scaling**: Stateless Apion servers
- **Database**: RDB in-memory with periodic SQL snapshots
- **Monitoring**: Prometheus + Grafana

### Reliability
- **Backup Strategy**: Automated SQL exports
- **Health Checks**: Monitor all components
- **Error Handling**: Graceful degradation
- **Logging**: Structured logging with correlation IDs

## Deployment Strategy

### Server Requirements
- **Memory**: 2GB minimum (4GB recommended) - Less than PostgreSQL setup
- **Storage**: 10GB SSD - Much less needed without external database
- **CPU**: 1 core minimum (2 cores recommended) - Lighter weight
- **Bandwidth**: 1TB/month (scales with usage)

### Docker Setup

```dockerfile
# Dockerfile
FROM node:18-alpine

WORKDIR /app
COPY target/scala-3.6.2/apion-fastopt/main.js .
COPY registry-data.sql .
COPY node_modules/ ./node_modules/

EXPOSE 3000
CMD ["node", "main.js"]
```

```yaml
# docker-compose.yml
version: '3.8'
services:
  registry:
    build: .
    ports:
      - "3000:3000"
    environment:
      - JWT_SECRET=your-secret-key
      - API_BASE_URL=https://kit-registry.example.com
    volumes:
      - ./registry-data.sql:/app/registry-data.sql:ro
      - ./backups:/app/backups
```

### Monitoring

```scala
// Health check endpoint
.get("/health", _ => 
  for {
    dbStatus <- Try(executeSQL("SELECT 1")).isSuccess
    timestamp = Instant.now()
  } yield {
    val status = if (dbStatus) "healthy" else "unhealthy"
    Map("status" -> status, "timestamp" -> timestamp, "database" -> "rdb").asJson
  }
)
```

## Launch Strategy

### Pre-Launch
1. **Seed Content**: Publish your own libraries (dynamic_string, dynamic_array)
2. **Documentation**: Complete CLI and API docs
3. **Testing**: Comprehensive testing with real-world usage
4. **Performance**: Load testing and optimization

### Launch
1. **Announcement**: Blog post, Reddit, Hacker News
2. **Community**: Reach out to C library authors
3. **Showcase**: Demonstrate with popular libraries (stb, sokol)
4. **Feedback**: Gather and iterate on user feedback

### Post-Launch
1. **Feature Requests**: Prioritize based on usage
2. **Ecosystem Growth**: Encourage library authors to publish
3. **Tooling Integration**: CMake, Makefile generators
4. **Platform Expansion**: Windows, embedded toolchain support

## Success Metrics

### Technical Metrics
- **API Response Time**: < 200ms p95
- **Uptime**: > 99.9%
- **Database Performance**: < 50ms query time (RDB in-memory advantage)
- **Package Resolution**: < 5s for complex dependency trees

### Business Metrics
- **Registered Users**: Target 1,000 in first month
- **Published Packages**: Target 100 in first month  
- **Downloads**: Target 10,000 in first month
- **Search Queries**: Track usage patterns

### Community Metrics
- **GitHub Stars**: Track kit CLI popularity
- **Community Contributions**: Package submissions
- **Documentation Views**: Usage of guides and docs
- **Support Requests**: Track and respond to issues

## Future Enhancements

### Short Term (3-6 months)
- **Private Packages**: Enterprise features
- **CI Integration**: GitHub Actions, GitLab CI
- **Package Analytics**: Detailed usage statistics
- **Dependency Security**: Vulnerability scanning

### Medium Term (6-12 months)
- **Mirror Support**: Regional mirrors for better performance
- **Package Signing**: Cryptographic verification
- **Build Service**: Cloud-based compilation
- **IDE Integration**: VS Code, CLion plugins

### Long Term (1+ years)
- **Package Policies**: Automated quality checks
- **Sponsored Packages**: Monetization for maintainers
- **Enterprise Features**: SSO, team management
- **Cross-Language**: Support for C++ header-only libraries
- **RDB Evolution**: When RDB adds file-based persistence, migrate seamlessly

---

*This design document serves as the complete blueprint for implementing the Kit Registry using RDB as the backend database. The choice of RDB provides both a lightweight solution for the registry and real-world testing for the database project.* 
