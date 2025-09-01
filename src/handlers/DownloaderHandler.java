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

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class DownloaderHandler {

    private static final long FILE_SIZE_LIMIT = 100 * 1024 * 1024; // 100MB
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Map<String, PlatformConfig> PLATFORM_CONFIG = new HashMap<>();

    static {
        PLATFORM_CONFIG.put("ig", new PlatformConfig("igdl", "instagram.mp4", "data[0]", "f.ig"));
        PLATFORM_CONFIG.put("fb", new PlatformConfig("fbdl", "facebook.mp4", "data[0]", "f.fb"));
        PLATFORM_CONFIG.put("tt", new PlatformConfig("ttdl", "tiktok.mp4", "data[0]", "f.tt"));
    }

    private static final Map<String, String> USAGE_TUTORIAL = new HashMap<>();

    static {
        USAGE_TUTORIAL.put("ig", "Enter a valid Instagram Reel URL!\nExample: `f.ig https://instagram.com/...`");
        USAGE_TUTORIAL.put("fb", "Enter a valid Facebook Video URL!\nExample: `f.fb https://facebook.com/...`");
        USAGE_TUTORIAL.put("tt", "Enter a valid TikTok Video URL!\nExample: `f.tt https://tiktok.com/...`");
    }

    private static class PlatformConfig {
        String endpoint;
        String fileName;
        String dataPath;
        String prefix;

        PlatformConfig(String endpoint, String fileName, String dataPath, String prefix) {
            this.endpoint = endpoint;
            this.fileName = fileName;
            this.dataPath = dataPath;
            this.prefix = prefix;
        }
    }

    private static boolean validateUrl(String url) {
        return url != null && url.matches("^https?://\\S+");
    }

    private static long getFileSize(String url) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpHead request = new HttpHead(url);
            request.setHeader("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36");

            try (ClassicHttpResponse response = client.execute(request)) {
                String contentLength = response.getFirstHeader("Content-Length") != null ?
                    response.getFirstHeader("Content-Length").getValue() : null;
                return contentLength != null ? Long.parseLong(contentLength) : Long.MAX_VALUE;
            }
        } catch (Exception e) {
            return Long.MAX_VALUE;
        }
    }

    private static String extractMediaUrl(JsonNode response, String dataPath) {
        try {
            String[] pathParts = dataPath.split("\\[|\\]");
            String mainKey = pathParts[0];
            int arrayIndex = Integer.parseInt(pathParts[1]);

            return response.get("data").get(mainKey).get(arrayIndex).get("url").asText();
        } catch (Exception e) {
            return null;
        }
    }

    private static void handleMediaDownload(MessageReceivedEvent event, String platform) {
        PlatformConfig config = PLATFORM_CONFIG.get(platform);
        String content = event.getMessage().getContentDisplay();
        String[] args = content.substring(config.prefix.length()).trim().split("\\s+");

        if (args.length == 0 || !validateUrl(args[0])) {
            event.getChannel().sendMessage(USAGE_TUTORIAL.get(platform)).queue();
            return;
        }

        String url = args[0];
        StringBuilder messageContent = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            messageContent.append(args[i]).append(" ");
        }

        CompletableFuture.runAsync(() -> {
            try {
                event.getMessage().delete().queue(null, throwable -> {});

                String apiUrl = "https://api.ryzendesu.vip/api/downloader/" + config.endpoint +
                    "?url=" + URLEncoder.encode(url, StandardCharsets.UTF_8);

                try (CloseableHttpClient client = HttpClients.createDefault()) {
                    HttpGet request = new HttpGet(apiUrl);
                    request.setHeader("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36");

                    try (ClassicHttpResponse response = client.execute(request)) {
                        String jsonResponse = EntityUtils.toString(response.getEntity());
                        JsonNode jsonNode = objectMapper.readTree(jsonResponse);

                        String mediaUrl = extractMediaUrl(jsonNode, config.dataPath);
                        if (mediaUrl == null) {
                            throw new RuntimeException("Media tidak ditemukan");
                        }

                        long fileSize = getFileSize(mediaUrl);
                        String modifiedUrl = mediaUrl.replace("dl=1", "dl=0");

                        String userMention = "<@" + event.getAuthor().getId() + ">";
                        StringBuilder finalContent = new StringBuilder();

                        if (messageContent.length() > 0) {
                            finalContent.append(messageContent.toString().trim()).append("\n");
                        }
                        finalContent.append(userMention);

                        if (fileSize > FILE_SIZE_LIMIT) {
                            finalContent.append("\n[᲼](").append(modifiedUrl).append(")");
                            event.getChannel().sendMessage(finalContent.toString()).queue();
                        } else {
                            try (InputStream inputStream = new HttpGet(mediaUrl).getEntity().getContent()) {
                                FileUpload attachment = FileUpload.fromData(inputStream, config.fileName);
                                event.getChannel().sendMessage(finalContent.toString())
                                    .addFiles(attachment)
                                    .queue();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[" + platform.toUpperCase() + "_ERROR] " + e.getMessage());
                event.getChannel().sendMessage("❌ Failed to download " + platform.toUpperCase() + " video!")
                    .queue(message -> {
                        CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS)
                            .execute(() -> message.delete().queue(null, throwable -> {}));
                    });
            }
        });
    }

    public static void handleIg(MessageReceivedEvent event) {
        handleMediaDownload(event, "ig");
    }

    public static void handleFb(MessageReceivedEvent event) {
        handleMediaDownload(event, "fb");
    }

    public static void handleTt(MessageReceivedEvent event) {
        handleMediaDownload(event, "tt");
    }
}
