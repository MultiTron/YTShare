# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

YTShare is a multi-platform app for sharing YouTube video links. This repo contains three independent sub-projects:

- **`backend/`** — Spring Boot 4 (Java 21) REST API with PostgreSQL
- **`frontend/`** — Angular 20+ SPA with Firebase Auth
- **`YTShare.Android/`** — Kotlin Android app (legacy Views + Jetpack Compose migration in progress)

## Build & Run Commands

### Backend (from `backend/`)
```sh
./mvnw spring-boot:run          # starts on :8080, auto-starts Postgres via docker-compose
./mvnw clean package             # build JAR (target/*.jar)
./mvnw clean package -DskipTests # build without tests
./mvnw test                      # run all tests
docker compose up -d             # Postgres only (port 5432)
```

### Frontend (from `frontend/`)
```sh
npm install        # first-time setup
npm start          # dev server on http://localhost:4200
npm run build      # production build to dist/
npm test           # Karma tests
npm run lint       # ESLint
```

## Architecture

### Authentication Flow
Firebase is the single auth provider across the entire stack. The frontend uses the Firebase JS SDK directly (no AngularFire) to handle login/register and obtain ID tokens. An HTTP interceptor (`auth.interceptor.ts`) attaches Bearer tokens to all requests to the backend API. On the backend, `FirebaseTokenFilter` verifies tokens via the Firebase Admin SDK and sets a `FirebaseAuthenticationToken` in the Spring Security context. Use `SecurityUtils` to get the current user's Firebase UID/email in service code.

Public endpoints are under `/api/public/**`; everything else requires authentication.

### Backend Structure
Each domain (user, video, chat, message, friends, device, user/preferences) follows the same pattern:
- **Entity** extends `BaseEntity` (UUID PK, `createdAt`/`updatedAt` audit fields)
- **Repository** (Spring Data JPA)
- **Service** (business logic)
- **Controller** (REST endpoints)
- **Mapper** (MapStruct, Spring component model) with input/output DTOs
- **Exception** classes per domain, handled by `GlobalExceptionHandler`

Database migrations are managed by **Liquibase** (`src/main/resources/db/changelog/`). Schema validation is set to `validate` — always create a changelog for schema changes rather than relying on Hibernate auto-DDL.

### Frontend Structure
Angular standalone components (no NgModules). All components use **OnPush** change detection and **SCSS** styles (configured as defaults in `angular.json`). Auth state is managed via Angular **signals** in `AuthService`. Routes are lazy-loaded.

### Environment Config
- **Backend**: configured via env vars (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `FIREBASE_CREDENTIALS_JSON`). Spring Boot's docker-compose integration auto-starts Postgres in dev.
- **Frontend**: `src/environments/environment.ts` (dev) and `environment.prod.ts` (prod) — uses `fileReplacements` in the build. Backend API URL is `environment.apiUrl`.

### CORS
Backend allows `http://localhost:4200` only (see `SecurityConfig.corsConfigurationSource()`). Update this when deploying or adding origins.

## Key Conventions

- Backend uses **Lombok** throughout (`@Getter`, `@Setter`, `@SuperBuilder`, `@RequiredArgsConstructor`). Annotation processor order matters: Lombok must run before MapStruct (configured in `pom.xml`).
- MapStruct mappers use `componentModel = "spring"` (set globally via compiler arg `-Amapstruct.defaultComponentModel=spring`).
- Backend fields are declared `final` where possible; controller/service methods use `final` parameters.
- Frontend services use `inject()` function style rather than constructor injection.
