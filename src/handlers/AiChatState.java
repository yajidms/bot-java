package handlers;

import io.github.cdimascio.dotenv.Dotenv;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AiChatState {

    private static final Dotenv dotenv = Dotenv.configure().load();
    private static final String[] GEMINI_KEYS = {dotenv.get("GEMINI_API_KEY")};
    private static int currentGeminiKeyIndex = 0;

    // Store chat states for users
    private static final Map<String, ChatSession> chatSessions = new ConcurrentHashMap<>();

    private static final String GEMINI_MODEL_NAME = "Gemini 2.5 Flash";
    private static final String GEMINI_INTERNAL_ID = "gemini-2.5-flash";

    public static class ChatSession {
        private String userId;
        private String channelId;
        private long lastActivity;
        private StringBuilder conversationHistory;

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

        // Getters and setters
        public String getUserId() { return userId; }
        public String getChannelId() { return channelId; }
        public long getLastActivity() { return lastActivity; }
    }

    public static String getActiveGeminiKey() {
        if (GEMINI_KEYS.length == 0 || GEMINI_KEYS[0] == null) {
            throw new RuntimeException("No valid GEMINI_API_KEY found in the .env file");
        }
        return GEMINI_KEYS[currentGeminiKeyIndex];
    }

    public static void switchGeminiKey() {
        if (GEMINI_KEYS.length > 1) {
            currentGeminiKeyIndex = (currentGeminiKeyIndex + 1) % GEMINI_KEYS.length;
            System.out.println("[AI Key] Switching to Gemini Key Index: " + currentGeminiKeyIndex);
        } else {
            System.out.println("[AI Key] Only 1 Gemini API key available, cannot switch.");
        }
    }

    public static ChatSession getOrCreateChatSession(String userId, String channelId) {
        String sessionKey = userId + "_" + channelId;
        return chatSessions.computeIfAbsent(sessionKey, k -> new ChatSession(userId, channelId));
    }

    public static void removeChatSession(String userId, String channelId) {
        String sessionKey = userId + "_" + channelId;
        chatSessions.remove(sessionKey);
    }

    public static void cleanupExpiredSessions(long maxIdleTime) {
        chatSessions.entrySet().removeIf(entry -> entry.getValue().isExpired(maxIdleTime));
    }

    public static int getActiveSessions() {
        return chatSessions.size();
    }

    public static String getGeminiModelName() {
        return GEMINI_MODEL_NAME;
    }

    public static String getGeminiInternalId() {
        return GEMINI_INTERNAL_ID;
    }
}
