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
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/**
 * Handler for downloading Twitter/X videos.
 * Matches the functionality of twitterHandler.js exactly.
 */
public class TwitterHandler {

    private static final long FILE_SIZE_LIMIT = 100 * 1024 * 1024; // 100MB
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36";

    /**
     * Gets file size from URL via HEAD request - matches JS getFileSize
     */
    private static long getFileSize(String url) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            var request = new HttpHead(url);
            request.setHeader("User-Agent", USER_AGENT);

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
     * Downloads data from URL and returns as bytes
     */
    private static byte[] downloadData(String url) throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            var request = new HttpGet(url);
            request.setHeader("User-Agent", USER_AGENT);

            try (ClassicHttpResponse response = client.execute(request)) {
                return response.getEntity().getContent().readAllBytes();
            }
        }
    }

    /**
     * Handle Twitter/X download - matches JS handleX exactly
     */
    public static void handleX(MessageReceivedEvent event) {
        String content = event.getMessage().getContentDisplay();
        String[] args = content.trim().split("\\s+");

        if (args.length == 0 || !args[0].startsWith("f.x")) return;

        String command = args[0].replace("f.", "");

        if ("x".equals(command)) {
            // Validate URL - matches JS urlPattern.test(args[1])
            if (args.length < 2 || !args[1].matches("^https?://\\S+")) {
                event.getChannel().sendMessage("Enter a valid Twitter/X Video URL!\nExample: `f.x https://x.com/...`")
                        .queue();
                return;
            }

            CompletableFuture.runAsync(() -> {
                try {
                    // Get message content (everything after URL) - matches JS messageContent
                    StringBuilder messageContent = new StringBuilder();
                    for (int i = 2; i < args.length; i++) {
                        if (i > 2) messageContent.append(" ");
                        messageContent.append(args[i]);
                    }

                    String apiUrl = "https://api.ryzendesu.vip/api/downloader/twitter?url=" +
                            URLEncoder.encode(args[1], StandardCharsets.UTF_8);

                    // Delete original message - matches JS msg.delete()
                    event.getMessage().delete().queue(null, throwable -> {});

                    try (CloseableHttpClient client = HttpClients.createDefault()) {
                        var request = new HttpGet(apiUrl);
                        request.setHeader("User-Agent", USER_AGENT);

                        try (ClassicHttpResponse response = client.execute(request)) {
                            String jsonResponse = EntityUtils.toString(response.getEntity());
                            JsonNode data = objectMapper.readTree(jsonResponse);

                            // Check if media exists - matches JS response.data.media[0]
                            if (!data.has("media") || !data.get("media").isArray() || data.get("media").isEmpty()) {
                                throw new RuntimeException("No media found");
                            }

                            JsonNode mediaData = data.get("media").get(0);
                            String videoUrl = mediaData.has("url") ? mediaData.get("url").asText() : null;

                            if (videoUrl == null) {
                                throw new RuntimeException("Video URL not found");
                            }

                            System.out.println("Twitter media data: " + mediaData.toString());

                            long fileSize = getFileSize(videoUrl);

                            if (fileSize > FILE_SIZE_LIMIT) {
                                // File too large - send URL instead
                                // Matches JS behavior: msg.channel.send(`[${messageContent || "᲼"}](${modifiedUrl})`)
                                String modifiedUrl = videoUrl.replace("dl=1", "dl=0");
                                String finalMessage = messageContent.toString().isEmpty() ? "᲼" : messageContent.toString();
                                event.getChannel().sendMessage("[" + finalMessage + "](" + modifiedUrl + ")").queue();
                            } else {
                                // Send as attachment - matches JS behavior
                                byte[] videoBytes = downloadData(videoUrl);
                                try (InputStream videoStream = new ByteArrayInputStream(videoBytes)) {
                                    var attachment = FileUpload.fromData(videoStream, "x.mp4");
                                    String msg = messageContent.toString() + " <@" + event.getAuthor().getId() + ">";
                                    event.getChannel().sendMessage(msg)
                                            .addFiles(attachment)
                                            .setAllowedMentions(Collections.emptyList())
                                            .queue();
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Twitter download error: " + e.getMessage());
                    e.printStackTrace();

                    // Check for specific error codes - matches JS error handling
                    if (e.getMessage() != null && e.getMessage().contains("40005")) {
                        // Entity request too large - send URL instead
                        try {
                            String apiUrl = "https://api.ryzendesu.vip/api/downloader/twitter?url=" +
                                    URLEncoder.encode(args[1], StandardCharsets.UTF_8);

                            try (CloseableHttpClient client = HttpClients.createDefault()) {
                                var request = new HttpGet(apiUrl);
                                request.setHeader("User-Agent", USER_AGENT);

                                try (ClassicHttpResponse response = client.execute(request)) {
                                    String jsonResponse = EntityUtils.toString(response.getEntity());
                                    JsonNode data = objectMapper.readTree(jsonResponse);
                                    JsonNode mediaData = data.get("media").get(0);
                                    String videoUrl = mediaData.get("url").asText();
                                    String modifiedUrl = videoUrl.replace("dl=1", "dl=0");

                                    StringBuilder messageContent = new StringBuilder();
                                    for (int i = 2; i < args.length; i++) {
                                        if (i > 2) messageContent.append(" ");
                                        messageContent.append(args[i]);
                                    }

                                    String finalMessage = messageContent.toString().isEmpty() ? "_" : messageContent.toString();
                                    event.getChannel().sendMessage("[" + finalMessage + "](" + modifiedUrl + ")").queue();
                                }
                            }
                        } catch (Exception fallbackError) {
                            event.getChannel().sendMessage("Failed to download the video").queue();
                        }
                    } else {
                        event.getChannel().sendMessage("Failed to download the video").queue();
                    }
                }
            });
        }
    }
}

