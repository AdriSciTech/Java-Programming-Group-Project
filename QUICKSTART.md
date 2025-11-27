# Quick Start Guide - Finance Tracker

## Prerequisites Check

Before starting, ensure you have:
- [ ] Java JDK 11+ installed
- [ ] Maven 3.6+ installed
- [ ] Supabase account created
- [ ] IDE installed (IntelliJ IDEA recommended)

## Step-by-Step Setup

### 1. Verify Java Installation
```bash
java -version
# Should show version 11 or higher
```

### 2. Verify Maven Installation
```bash
mvn -version
# Should show Maven 3.6 or higher
```

### 3. Set Up Supabase Database

#### Create Supabase Project
1. Go to https://supabase.com
2. Sign up for a free account
3. Click "New Project"
4. Fill in project details:
   - Name: finance-tracker
   - Database Password: (create a strong password)
   - Region: (choose closest to you)
5. Wait for project to be created (2-3 minutes)

#### Run Database Schema
1. In Supabase Dashboard, go to SQL Editor
2. Click "New Query"
3. Copy entire content of `database_schema.sql`
4. Paste into SQL Editor
5. Click "Run" or press Ctrl+Enter
6. Verify all tables created (should see success message)

#### Get Your Credentials
1. Go to Settings > API
2. Copy:
   - Project URL (e.g., https://xxxxx.supabase.co)
   - Anon/Public Key
3. Go to Settings > Database
4. Copy:
   - Database Host
   - Database Port (usually 5432)
   - Database Name (usually postgres)
   - Your Database Password

### 4. Configure Application

1. Open `src/main/resources/application.properties`
2. Replace placeholders with your Supabase credentials:

```properties
# Example configuration
supabase.url=https://abcdefgh.supabase.co
supabase.key=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
supabase.jwt.secret=your-jwt-secret

db.url=jdbc:postgresql://db.abcdefgh.supabase.co:5432/postgres
db.username=postgres
db.password=your-strong-password-here
```

### 5. Build the Project

```bash
cd FinanceTracker
mvn clean install
```

Expected output:
```
[INFO] BUILD SUCCESS
[INFO] Total time: XX.XXX s
```

### 6. Run the Application

Option A - Using Maven:
```bash
mvn javafx:run
```

Option B - Using IDE:
1. Open project in IntelliJ IDEA
2. Wait for Maven to import dependencies
3. Find `MainApp.java` in src/main/java/com/financetracker/
4. Right-click and select "Run MainApp.main()"

## First Time Login

### Create Test User in Supabase

Since signup is not yet implemented, create a test user manually:

1. Go to Supabase Dashboard > Authentication > Users
2. Click "Add User"
3. Enter:
   - Email: test@example.com
   - Password: Test123!
   - Email Confirm: YES
4. Click "Create User"

5. Go to SQL Editor and run:
```sql
INSERT INTO users (user_id, email, password_hash, full_name, is_active)
VALUES (
  (SELECT id FROM auth.users WHERE email = 'test@example.com'),
  'test@example.com',
  '$2a$10$...',  -- This will be overwritten when you login
  'Test User',
  true
);
```

### Login
1. Launch the application
2. Enter credentials:
   - Email: test@example.com
   - Password: Test123!
3. Click "Login"

## Troubleshooting

### Application Won't Start
- Check Java version: `java -version`
- Check if port 5432 is accessible
- Verify Supabase credentials in application.properties

### Database Connection Failed
- Verify Supabase project is active
- Check database URL and credentials
- Ensure your IP is not blocked by Supabase

### Login Failed
- Verify user exists in Supabase Authentication
- Check user is active in users table
- Verify password is correct

### Maven Build Failed
- Run `mvn clean` first
- Check internet connection (Maven downloads dependencies)
- Delete `.m2/repository` folder and try again

### JavaFX Not Found
- Ensure you're using Java 11+ (JavaFX is separate in Java 11+)
- Maven should download JavaFX automatically
- Check pom.xml has JavaFX dependencies

## Verify Installation

Run these commands to verify everything is working:

```bash
# 1. Check if project compiles
mvn clean compile

# 2. Check if tests pass
mvn test

# 3. Run the application
mvn javafx:run
```

## Next Steps

Once the application is running:

1. **Explore the Dashboard**
   - View summary cards
   - Navigate through menu items

2. **Add Test Data**
   - Go to SQL Editor in Supabase
   - Insert sample income/expenses
   - Refresh dashboard

3. **Customize**
   - Modify CSS in `src/main/resources/css/style.css`
   - Add new features in respective packages

## Getting Help

- Check `README.md` for detailed documentation
- Review code comments in source files
- Check Supabase documentation: https://supabase.com/docs
- Review JavaFX documentation: https://openjfx.io/

## Common Development Tasks

### Add a New Model
1. Create class in `com.financetracker.model`
2. Add corresponding service in `com.financetracker.service`
3. Create controller in `com.financetracker.controller`
4. Design FXML in `src/main/resources/fxml`

### Add a New View
1. Create FXML file in `resources/fxml/`
2. Create corresponding controller
3. Add navigation in MainApp or existing controller
4. Style with CSS

### Debug Application
1. Add breakpoints in IDE
2. Run in Debug mode
3. Check logs in `logs/finance-tracker.log`
4. Use Supabase Dashboard to verify database queries

## Performance Tips

- Use connection pooling for database
- Implement caching for frequently accessed data
- Lazy load data in tables
- Optimize SQL queries

## Security Reminders

- Never commit `application.properties` with real credentials
- Use environment variables for production
- Keep Supabase keys secret
- Regularly update dependencies

---

Happy coding! 🚀
