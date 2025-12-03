package com.financetracker.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import okhttp3.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

/**
 * Supabase Client Manager for handling authentication and database connections
 */
public class SupabaseClient {
    private static final Logger logger = LoggerFactory.getLogger(SupabaseClient.class);
    private static SupabaseClient instance;

    private final String supabaseUrl;
    private final String supabaseKey;
    private final String dbUrl;
    private final String dbUsername;
    private final String dbPassword;

    private OkHttpClient httpClient;
    private Gson gson;
    private String currentUserToken;
    private String currentUserId;
    private String currentUserEmail;

    private HikariDataSource dataSource;

    private SupabaseClient() {
        ConfigManager config = ConfigManager.getInstance();
        this.supabaseUrl = config.getSupabaseUrl();
        this.supabaseKey = config.getSupabaseKey();
        this.dbUrl = config.getDbUrl();
        this.dbUsername = config.getDbUsername();
        this.dbPassword = config.getDbPassword();
        
        // Validate configuration
        if (supabaseUrl == null || supabaseUrl.contains("your-project") || supabaseUrl.isEmpty()) {
            logger.error("⚠️  SUPABASE CONFIGURATION ERROR ⚠️");
            logger.error("The application.properties file contains placeholder values!");
            logger.error("Please update src/main/resources/application.properties with your actual Supabase credentials:");
            logger.error("  - supabase.url=https://YOUR-PROJECT-ID.supabase.co");
            logger.error("  - supabase.key=YOUR-ANON-KEY");
            logger.error("  - db.url=jdbc:postgresql://db.YOUR-PROJECT-ID.supabase.co:5432/postgres");
            logger.error("  - db.password=YOUR-DATABASE-PASSWORD");
            logger.error("");
            logger.error("Get your credentials from: https://app.supabase.com → Your Project → Settings → API");
        }

        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        this.gson = new Gson();

        // Initialize HikariCP
        initDataSource();

        logger.info("SupabaseClient initialized");
    }

    private void initDataSource() {
        if (dbUrl != null && !dbUrl.isEmpty()) {
            try {
                HikariConfig hikariConfig = new HikariConfig();
                hikariConfig.setJdbcUrl(dbUrl);
                hikariConfig.setUsername(dbUsername);
                hikariConfig.setPassword(dbPassword);
                hikariConfig.setDriverClassName("org.postgresql.Driver");
                
                // Optimization settings
                hikariConfig.setMaximumPoolSize(10);
                hikariConfig.setMinimumIdle(2);
                hikariConfig.setIdleTimeout(300000); // 5 minutes
                hikariConfig.setConnectionTimeout(20000); // 20 seconds
                hikariConfig.setLeakDetectionThreshold(60000); // 1 minute
                
                this.dataSource = new HikariDataSource(hikariConfig);
                logger.info("Database connection pool initialized");
            } catch (Exception e) {
                logger.error("Failed to initialize connection pool", e);
            }
        }
    }

    public static SupabaseClient getInstance() {
        if (instance == null) {
            synchronized (SupabaseClient.class) {
                if (instance == null) {
                    instance = new SupabaseClient();
                }
            }
        }
        return instance;
    }

    /**
     * Sign up a new user (original method with full name)
     */
    public JsonObject signUp(String email, String password, String fullName) throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("email", email);
        requestBody.addProperty("password", password);

        JsonObject metadata = new JsonObject();
        metadata.addProperty("full_name", fullName);
        requestBody.add("data", metadata);

        String url = supabaseUrl + "/auth/v1/signup";

        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

            if (response.isSuccessful()) {
                // Store token if returned
                if (jsonResponse.has("access_token")) {
                    currentUserToken = jsonResponse.get("access_token").getAsString();
                }
                if (jsonResponse.has("user")) {
                    JsonObject user = jsonResponse.getAsJsonObject("user");
                    if (user.has("id")) {
                        currentUserId = user.get("id").getAsString();
                    }
                    if (user.has("email")) {
                        currentUserEmail = user.get("email").getAsString();
                    }
                }
                logger.info("User signed up successfully: {}", email);
                return jsonResponse;
            } else {
                logger.error("Sign up failed: {}", responseBody);
                throw new IOException("Sign up failed: " + responseBody);
            }
        }
    }

    /**
     * Sign up a new user (simplified - returns boolean for LoginController compatibility)
     */
    public boolean signUp(String email, String password) throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("email", email);
        requestBody.addProperty("password", password);

        String url = supabaseUrl + "/auth/v1/signup";

        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

            if (response.isSuccessful()) {
                // Store token if returned
                if (jsonResponse.has("access_token")) {
                    currentUserToken = jsonResponse.get("access_token").getAsString();
                }
                if (jsonResponse.has("user")) {
                    JsonObject user = jsonResponse.getAsJsonObject("user");
                    if (user.has("id")) {
                        currentUserId = user.get("id").getAsString();
                    }
                    if (user.has("email")) {
                        currentUserEmail = user.get("email").getAsString();
                    }
                }
                logger.info("User signed up successfully: {}", email);
                return true;
            } else {
                logger.error("Sign up failed: {}", responseBody);
                // Parse error message
                if (jsonResponse.has("error_description")) {
                    throw new IOException(jsonResponse.get("error_description").getAsString());
                } else if (jsonResponse.has("msg")) {
                    throw new IOException(jsonResponse.get("msg").getAsString());
                } else if (jsonResponse.has("error")) {
                    throw new IOException(jsonResponse.get("error").getAsString());
                }
                throw new IOException("Sign up failed: " + responseBody);
            }
        }
    }

    /**
     * Sign in an existing user (original method returning JsonObject)
     */
    public JsonObject signInAndGetResponse(String email, String password) throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("email", email);
        requestBody.addProperty("password", password);

        String url = supabaseUrl + "/auth/v1/token?grant_type=password";

        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

            if (response.isSuccessful()) {
                // Store the access token and user ID
                if (jsonResponse.has("access_token")) {
                    currentUserToken = jsonResponse.get("access_token").getAsString();
                }
                if (jsonResponse.has("user")) {
                    JsonObject user = jsonResponse.getAsJsonObject("user");
                    if (user.has("id")) {
                        currentUserId = user.get("id").getAsString();
                    }
                    if (user.has("email")) {
                        currentUserEmail = user.get("email").getAsString();
                    }
                }
                logger.info("User signed in successfully: {}", email);
                return jsonResponse;
            } else {
                logger.error("Sign in failed: {}", responseBody);
                throw new IOException("Sign in failed: " + responseBody);
            }
        }
    }

    /**
     * Sign in an existing user (returns boolean for LoginController compatibility)
     */
    public boolean signIn(String email, String password) throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("email", email);
        requestBody.addProperty("password", password);

        String url = supabaseUrl + "/auth/v1/token?grant_type=password";

        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

            if (response.isSuccessful()) {
                // Store the access token and user ID
                if (jsonResponse.has("access_token")) {
                    currentUserToken = jsonResponse.get("access_token").getAsString();
                }
                if (jsonResponse.has("user")) {
                    JsonObject user = jsonResponse.getAsJsonObject("user");
                    if (user.has("id")) {
                        currentUserId = user.get("id").getAsString();
                    }
                    if (user.has("email")) {
                        currentUserEmail = user.get("email").getAsString();
                    }
                }
                logger.info("User signed in successfully: {}", email);
                return true;
            } else {
                logger.error("Sign in failed: {}", responseBody);
                // Parse error message
                if (jsonResponse.has("error_description")) {
                    throw new IOException(jsonResponse.get("error_description").getAsString());
                } else if (jsonResponse.has("msg")) {
                    throw new IOException(jsonResponse.get("msg").getAsString());
                } else if (jsonResponse.has("error")) {
                    throw new IOException(jsonResponse.get("error").getAsString());
                }
                throw new IOException("Invalid login credentials");
            }
        }
    }

    /**
     * Sign out the current user
     */
    public void signOut() {
        currentUserToken = null;
        currentUserId = null;
        currentUserEmail = null;
        logger.info("User signed out");
    }

    /**
     * Request password reset email
     */
    public boolean resetPassword(String email) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("email", email);

        String url = supabaseUrl + "/auth/v1/recover";

        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                logger.info("Password reset email sent to: {}", email);
                return true;
            } else {
                String responseBody = response.body().string();
                logger.error("Password reset failed: {}", responseBody);
                return false;
            }
        } catch (IOException e) {
            logger.error("Password reset request failed", e);
            return false;
        }
    }

    /**
     * Get the current user's access token
     */
    public String getCurrentUserToken() {
        return currentUserToken;
    }

    /**
     * Get the current user's ID
     */
    public String getCurrentUserId() {
        return currentUserId;
    }

    /**
     * Get the current user's email
     */
    public String getCurrentUserEmail() {
        return currentUserEmail;
    }

    /**
     * Check if a user is currently signed in
     */
    public boolean isUserSignedIn() {
        return currentUserToken != null && currentUserId != null;
    }

    /**
     * Check if authenticated (alias for isUserSignedIn)
     */
    public boolean isAuthenticated() {
        return isUserSignedIn();
    }

    /**
     * Get a database connection
     */
    public Connection getConnection() throws SQLException {
        if (dataSource != null) {
            return dataSource.getConnection();
        }
        
        // Fallback for initial connection test or if pool failed
        try {
            Class.forName("org.postgresql.Driver");
            Connection conn = java.sql.DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
            logger.debug("Direct database connection established (fallback)");
            return conn;
        } catch (ClassNotFoundException e) {
            logger.error("PostgreSQL JDBC Driver not found", e);
            throw new SQLException("PostgreSQL JDBC Driver not found", e);
        }
    }

    /**
     * Execute a REST API call to Supabase
     */
    public JsonObject executeRestCall(String endpoint, String method, JsonObject body) throws IOException {
        String url = supabaseUrl + "/rest/v1/" + endpoint;

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
                .addHeader("Content-Type", "application/json");

        if (currentUserToken != null) {
            requestBuilder.addHeader("Authorization", "Bearer " + currentUserToken);
        }

        RequestBody requestBody = null;
        if (body != null) {
            requestBody = RequestBody.create(
                    body.toString(),
                    MediaType.parse("application/json")
            );
        }

        switch (method.toUpperCase()) {
            case "GET":
                requestBuilder.get();
                break;
            case "POST":
                requestBuilder.post(requestBody);
                break;
            case "PUT":
                requestBuilder.put(requestBody);
                break;
            case "PATCH":
                requestBuilder.patch(requestBody);
                break;
            case "DELETE":
                requestBuilder.delete();
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }

        Request request = requestBuilder.build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();

            if (response.isSuccessful()) {
                return gson.fromJson(responseBody, JsonObject.class);
            } else {
                logger.error("REST call failed: {}", responseBody);
                throw new IOException("REST call failed: " + responseBody);
            }
        }
    }

    /**
     * Check if configuration is valid
     */
    public boolean isConfigured() {
        return supabaseUrl != null && !supabaseUrl.isEmpty() &&
                dbUrl != null && !dbUrl.isEmpty();
    }
    
    /**
     * Shutdown connection pool
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Connection pool closed");
        }
    }
}