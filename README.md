# Rippo

Rippo is a cross-platform GitHub repository explorer built with Flutter and Spring Boot.

Users can connect their GitHub account, browse repositories, explore project files, and view repository details through a clean and simple interface.

## Features

* GitHub OAuth Login
* Fetch User Repositories
* Repository Explorer
* File & Folder Navigation
* Repository Details View
* Spring Boot Backend Integration
* Flutter Cross-Platform Frontend

## Tech Stack

### Frontend

* Flutter
* Dart

### Backend

* Spring Boot
* Spring Security
* OAuth2 Client

### APIs

* GitHub REST API

## Project Structure

```text
frontend/
├── lib/
├── android/
└── pubspec.yaml

backend/
├── src/
├── pom.xml
└── mvnw
```

## Current Status

Rippo is currently focused on GitHub repository exploration and codebase navigation.

Upcoming features include:

* README Viewer
* Commit History
* File Content Viewer
* AI-Powered Repository Analysis
* MCP-Based Tool Architecture

## Getting Started

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

### Frontend

```bash
cd frontend
flutter pub get
flutter run
```

## License

This project is for learning and experimentation purposes.
