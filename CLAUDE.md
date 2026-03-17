# CLAUDE.md

This file is the source of truth for how Claude Code should work in this repository.

## Project Overview

KYC Matching System for Foo company. Reconciles customer records from two sources:
- `customers.json` — internal onboarding data
- `kyc_verified.json` — third-party KYC verification data

Goal: classify each customer pair as `confirmed_match`, `likely_match`, `needs_review`, or `no_match`, and flag elevated-risk records. Detailed requirements present in instructions.md file. 

## Architecture

See `architecture.md` for the full architecture guide. The project is a full-stack app:
- **Frontend**: React / Next.js (App Router)
- **Backend**: Java REST API (Spring Boot)

The two layers are fully decoupled, frontend communicates with backend over HTTP/REST only. No business logic in the frontend. Build the full stack only when required. Start simple and build logic in standalone classes and  and wait for my prompt to start building the APIs, frontend etc. 

## Key Conventions (from architecture.md)

- Backend: controllers are thin, all logic in services, DTOs never expose JPA entities
- Frontend: no business logic in pages, all API calls through `lib/api/`, hooks encapsulate single concerns
- Errors are first-class: named exception classes, global `@ControllerAdvice`, standard error envelope
- Config is environment-aware: values in `application.yml`, no hard-coded values in Java
- Tests: test the contract/outcome, not the implementation; unhappy paths are first-class test cases
- Commit messages follow Conventional Commits: `feat:`, `fix:`, `chore:`, `docs:`
- No magic numbers, no commented-out code, no new libraries without a comment explaining why