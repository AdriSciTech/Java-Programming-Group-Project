# Finance Tracker - Personal Budget Management System

A comprehensive JavaFX-based financial management application with Supabase backend for tracking income, expenses, budgets, bills, investments, and analyzing spending patterns.

## Features

- **Monthly Income Tracking**: Record and manage multiple income sources
- **Expense Management**: Categorize and track all expenses
- **Budget Planning**: Set spending limits with real-time alerts
- **Bills & Subscriptions**: Track recurring payments and get reminders
- **Investment Tracking**: Monitor portfolio performance
- **Analytics & Reports**: Visualize spending patterns with charts
- **Transfer Management**: Track money movement between accounts
- **Custom Categories**: Create personalized income/expense categories

## Quick Start

1. Configure `src/main/resources/application.properties` with your Supabase credentials
2. Run the database schema: `database_schema.sql` in Supabase SQL Editor
3. Build: `mvn clean compile`
4. Run: `mvn javafx:run`

## Project Structure

```
src/main/java/com/financetracker/
├── model/              # Data models
├── controller/         # JavaFX controllers
├── service/           # Business logic & database operations
├── util/              # Utility classes
└── MainApp.java       # Application entry point
```

## Technologies

- JavaFX 17
- Supabase (PostgreSQL)
- Maven
- SLF4J + Logback
