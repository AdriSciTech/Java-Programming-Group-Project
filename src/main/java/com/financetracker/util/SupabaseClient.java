package com.financetracker.util;

import okhttp3.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
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
    
    private SupabaseClient() {
        ConfigManager config = ConfigManager.getInstance();
        this.supabaseUrl = config.getSupabaseUrl();
        this.supabaseKey = config.getSupabaseKey();
        this.dbUrl = config.getDbUrl();
        this.dbUsername = config.getDbUsername();
        this.dbPassword = config.getDbPassword();
        
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        
        this.gson = new Gson();
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
     * Sign up a new user
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
                logger.info("User signed up successfully: {}", email);
                return jsonResponse;
            } else {
                logger.error("Sign up failed: {}", responseBody);
                throw new IOException("Sign up failed: " + responseBody);
            }
        }
    }
    
    /**
     * Sign in an existing user
     */
    public JsonObject signIn(String email, String password) throws IOException {
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
     * Sign out the current user
     */
    public void signOut() {
        currentUserToken = null;
        currentUserId = null;
        logger.info("User signed out");
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
     * Check if a user is currently signed in
     */
    public boolean isUserSignedIn() {
        return currentUserToken != null && currentUserId != null;
    }
    
    /**
     * Get a database connection
     */
    public Connection getConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
            Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
            logger.debug("Database connection established");
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
}
