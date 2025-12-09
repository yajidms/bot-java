package handlers;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Handler for downloading media from Instagram, Facebook, and TikTok.
 * Matches the functionality of downloaderHandler.js exactly.
 */
public class DownloaderHandler {

    private static final long FILE_SIZE_LIMIT = 100 * 1024 * 1024; // 100MB
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36";

    // Platform configurations matching JS PLATFORM_CONFIG
    private record PlatformConfig(String endpoint, String fileName, String dataPath, String prefix) {}

    private static final Map<String, PlatformConfig> PLATFORM_CONFIG = Map.of(
        "ig", new PlatformConfig("igdl", "instagram.mp4", "data[0]", "f.ig"),
        "fb", new PlatformConfig("fbdl", "facebook.mp4", "data[0]", "f.fb"),
        "tt", new PlatformConfig("ttdl", "tiktok.mp4", "data[0]", "f.tt")
    );

    // Usage tutorials matching JS USAGE_TUTORIAL
    private static final Map<String, String> USAGE_TUTORIAL = Map.of(
        "ig", "Masukkan URL Reel Instagram yang valid!\nContoh: `f.ig https://instagram.com/...`",
        "fb", "Masukkan URL Video Facebook yang valid!\nContoh: `f.fb https://facebook.com/...`",
        "tt", "Masukkan URL Video TikTok yang valid!\nContoh: `f.tt https://tiktok.com/...`"
    );

    /**
     * Validates URL format - matches JS validateUrl
     */
    private static boolean validateUrl(String url) {
        return url != null && url.matches("^https?://\\S+");
    }

    /**
     * Gets file size from URL via HEAD request - matches JS getFileSize
     */
    private static long getFileSize(String url) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            var request = new HttpHead(url);
            request.setHeader("User-Agent", USER_AGENT);
            // Follow redirects
            try (ClassicHttpResponse response = client.execute(request)) {
                var contentLengthHeader = response.getFirstHeader("Content-Length");
                if (contentLengthHeader != null) {
                    return Long.parseLong(contentLengthHeader.getValue());
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to get file size: " + e.getMessage());
        }
        return Long.MAX_VALUE;
    }

    /**
     * Extracts media URL from API response - matches JS extractMediaData
     */
    private static String extractMediaUrl(JsonNode response, String dataPath) {
        try {
            // Parse dataPath like "data[0]"
            String[] pathParts = dataPath.split("\\[|\\]");
            String mainKey = pathParts[0];
            int arrayIndex = Integer.parseInt(pathParts[1]);

            JsonNode dataNode = response.get(mainKey);
            if (dataNode != null && dataNode.isArray() && dataNode.size() > arrayIndex) {
                JsonNode mediaNode = dataNode.get(arrayIndex);
                if (mediaNode.has("url")) {
                    return mediaNode.get("url").asText();
                }
            }
        } catch (Exception e) {
            System.err.println("Error extracting media URL: " + e.getMessage());
        }
        return null;
    }

    /**
     * Downloads media bytes from URL
     */
    private static byte[] downloadMedia(String url) throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            var request = new HttpGet(url);
            request.setHeader("User-Agent", USER_AGENT);

            try (ClassicHttpResponse response = client.execute(request)) {
                return response.getEntity().getContent().readAllBytes();
            }
        }
    }

    /**
     * Core handler for media downloads - matches JS handleMediaDownload exactly
     */
    private static void handleMediaDownload(MessageReceivedEvent event, String platform) {
        var config = PLATFORM_CONFIG.get(platform);
        String content = event.getMessage().getContentDisplay();
        String[] args = content.substring(config.prefix().length()).trim().split("\\s+");

        // Get URL and message content
        String url = args.length > 0 ? args[0] : "";
        StringBuilder messageContent = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) messageContent.append(" ");
            messageContent.append(args[i]);
        }

        // Show tutorial if URL is invalid - matches JS behavior
        if (!validateUrl(url)) {
            event.getChannel().sendMessage(USAGE_TUTORIAL.get(platform)).queue();
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                // Delete original message - matches JS behavior
                event.getMessage().delete().queue(null, throwable -> {});

                // Call API
                String apiUrl = "https://api.ryzendesu.vip/api/downloader/" + config.endpoint() +
                        "?url=" + URLEncoder.encode(url, StandardCharsets.UTF_8);

                try (CloseableHttpClient client = HttpClients.createDefault()) {
                    var request = new HttpGet(apiUrl);
                    request.setHeader("User-Agent", USER_AGENT);

                    try (ClassicHttpResponse response = client.execute(request)) {
                        String jsonResponse = EntityUtils.toString(response.getEntity());
                        JsonNode jsonNode = objectMapper.readTree(jsonResponse);

                        String mediaUrl = extractMediaUrl(jsonNode, config.dataPath());
                        if (mediaUrl == null) {
                            throw new RuntimeException("Media tidak ditemukan");
                        }

                        long fileSize = getFileSize(mediaUrl);
                        String modifiedUrl = mediaUrl.replace("dl=1", "dl=0");

                        // Build message content - matches JS format exactly
                        String userMention = "<@" + event.getAuthor().getId() + ">";
                        StringBuilder finalContent = new StringBuilder();

                        if (!messageContent.toString().isEmpty()) {
                            finalContent.append(messageContent.toString().trim()).append("\n");
                        }
                        finalContent.append(userMention);

                        if (fileSize > FILE_SIZE_LIMIT) {
                            // File too large - send as hidden link (matching JS [᲼](url) format)
                            finalContent.append("\n[᲼](").append(modifiedUrl).append(")");
                            event.getChannel().sendMessage(finalContent.toString())
                                    .setAllowedMentions(java.util.Collections.emptyList())
                                    .queue();
                        } else {
                            // Download and send as attachment
                            byte[] mediaBytes = downloadMedia(mediaUrl);
                            try (InputStream inputStream = new ByteArrayInputStream(mediaBytes)) {
                                var attachment = FileUpload.fromData(inputStream, config.fileName());
                                event.getChannel().sendMessage(finalContent.toString())
                                        .addFiles(attachment)
                                        .setAllowedMentions(java.util.Collections.emptyList())
                                        .queue();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[" + platform.toUpperCase() + "_ERROR] " + e.getMessage());
                e.printStackTrace();

                // Send error message and delete after 5 seconds - matches JS behavior
                event.getChannel().sendMessage("❌ Gagal mengunduh " + platform.toUpperCase() + " video!")
                        .queue(errorMessage -> {
                            errorMessage.delete().queueAfter(5, TimeUnit.SECONDS, null, t -> {});
                        });
            }
        });
    }

    /**
     * Handle Instagram download - matches JS handleIg
     */
    public static void handleIg(MessageReceivedEvent event) {
        handleMediaDownload(event, "ig");
    }

    /**
     * Handle Facebook download - matches JS handleFb
     */
    public static void handleFb(MessageReceivedEvent event) {
        handleMediaDownload(event, "fb");
    }

    /**
     * Handle TikTok download - matches JS handleTt
     */
    public static void handleTt(MessageReceivedEvent event) {
        handleMediaDownload(event, "tt");
    }
}

