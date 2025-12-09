package handlers;

import io.github.cdimascio.dotenv.Dotenv;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * State management for AI chat sessions.
 */
public class AiChatState {

    private static final Dotenv dotenv = Dotenv.configure().load();
    private static final String GEMINI_API_KEY = dotenv.get("GEMINI_API_KEY");

    // Store active AI chats by channel ID
    private static final Map<String, ChatData> activeAIChats = new ConcurrentHashMap<>();

    // Model configuration
    public static final String GEMINI_MODEL_NAME = "Gemini 2.5 Pro";
    public static final String GEMINI_INTERNAL_ID = "gemini-2.5-pro";

    /**
     * Chat data for active sessions
     */
    public static class ChatData {
        private final String channelId;
        private final String userId;
        private final String modelName;
        private final StringBuilder conversationHistory;
        private long lastActivity;

        public ChatData(String channelId, String userId, String modelName) {
            this.channelId = channelId;
            this.userId = userId;
            this.modelName = modelName;
            this.conversationHistory = new StringBuilder();
            this.lastActivity = System.currentTimeMillis();
        }

        public void addMessage(String role, String content) {
            conversationHistory.append(String.format("[%s]: %s\n", role, content));
            updateLastActivity();
        }

        public void updateLastActivity() {
            this.lastActivity = System.currentTimeMillis();
        }

        public boolean isExpired(long maxIdleTimeMs) {
            return System.currentTimeMillis() - lastActivity > maxIdleTimeMs;
        }

        // Getters
        public String getChannelId() { return channelId; }
        public String getUserId() { return userId; }
        public String getModelName() { return modelName; }
        public String getConversationHistory() { return conversationHistory.toString(); }
        public long getLastActivity() { return lastActivity; }
    }

    /**
     * Legacy ChatSession class for backward compatibility
     */
    public static class ChatSession {
        private final String userId;
        private final String channelId;
        private final StringBuilder conversationHistory;
        private long lastActivity;

        public ChatSession(String userId, String channelId) {
            this.userId = userId;
            this.channelId = channelId;
            this.lastActivity = System.currentTimeMillis();
            this.conversationHistory = new StringBuilder();
        }

        public void addMessage(String role, String content) {
            conversationHistory.append(String.format("[%s]: %s\n", role, content));
            updateLastActivity();
        }

        public void updateLastActivity() {
            this.lastActivity = System.currentTimeMillis();
        }

        public boolean isExpired(long maxIdleTime) {
            return System.currentTimeMillis() - lastActivity > maxIdleTime;
        }

        public String getConversationHistory() {
            return conversationHistory.toString();
        }

        public String getUserId() { return userId; }
        public String getChannelId() { return channelId; }
        public long getLastActivity() { return lastActivity; }
    }

    // Store chat sessions by userId_channelId key
    private static final Map<String, ChatSession> chatSessions = new ConcurrentHashMap<>();

    /**
     * Gets the active Gemini API key
     */
    public static String getActiveGeminiKey() {
        if (GEMINI_API_KEY == null || GEMINI_API_KEY.isEmpty()) {
            throw new RuntimeException("No valid GEMINI_API_KEY found in the .env file");
        }
        return GEMINI_API_KEY;
    }

    /**
     * Checks if there's an active AI chat in a channel
     */
    public static boolean hasActiveChat(String channelId) {
        return activeAIChats.containsKey(channelId);
    }

    /**
     * Gets the active chat data for a channel
     */
    public static ChatData getActiveChat(String channelId) {
        return activeAIChats.get(channelId);
    }

    /**
     * Starts an AI chat session in a channel
     */
    public static void startChat(String channelId, String userId, String modelName) {
        activeAIChats.put(channelId, new ChatData(channelId, userId, modelName));
    }

    /**
     * Ends an AI chat session in a channel
     */
    public static void endChat(String channelId) {
        activeAIChats.remove(channelId);
    }

    /**
     * Gets or creates a chat session for a user in a channel
     */
    public static ChatSession getOrCreateChatSession(String userId, String channelId) {
        String sessionKey = userId + "_" + channelId;
        return chatSessions.computeIfAbsent(sessionKey, k -> new ChatSession(userId, channelId));
    }

    /**
     * Removes a chat session
     */
    public static void removeChatSession(String userId, String channelId) {
        String sessionKey = userId + "_" + channelId;
        chatSessions.remove(sessionKey);
    }

    /**
     * Cleans up expired sessions
     */
    public static void cleanupExpiredSessions(long maxIdleTimeMs) {
        // Cleanup active chats
        activeAIChats.entrySet().removeIf(entry -> entry.getValue().isExpired(maxIdleTimeMs));
        // Cleanup chat sessions
        chatSessions.entrySet().removeIf(entry -> entry.getValue().isExpired(maxIdleTimeMs));
    }

    /**
     * Gets the number of active sessions
     */
    public static int getActiveSessions() {
        return activeAIChats.size();
    }

    /**
     * Gets the Gemini model display name
     */
    public static String getGeminiModelName() {
        return GEMINI_MODEL_NAME;
    }

    /**
     * Gets the Gemini internal model ID
     */
    public static String getGeminiInternalId() {
        return GEMINI_INTERNAL_ID;
    }
}

