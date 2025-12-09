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
 * Handler for downloading YouTube videos.
 * Matches the functionality of ytdlHandler.js exactly.
 */
public class YtdlHandler {

    // 100MB limit matching JS FILE_SIZE_LIMIT
    private static final long FILE_SIZE_LIMIT = 100 * 1024 * 1024;
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
     * Handle YouTube download - matches JS handleYtDownload exactly
     */
    public static void handleYtDownload(MessageReceivedEvent event) {
        String prefix = "f.yt";
        String content = event.getMessage().getContentDisplay();

        if (!content.startsWith(prefix)) return;

        String[] args = content.substring(prefix.length()).trim().split("\\s+");

        // Validate URL - matches JS urlPattern.test(args[0])
        if (args.length == 0 || args[0].isEmpty() || !args[0].matches("https?://\\S+")) {
            event.getChannel().sendMessage("Enter a valid YouTube URL!\nExample: `f.yt https://youtube.com/...`")
                    .queue();
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                // Delete original message - matches JS behavior
                event.getMessage().delete().queue(null, throwable -> {});

                String ytUrl = args[0];
                String apiUrl = "https://api.ryzendesu.vip/api/downloader/ytmp4?url=" +
                        URLEncoder.encode(ytUrl, StandardCharsets.UTF_8) + "&quality=480";

                try (CloseableHttpClient client = HttpClients.createDefault()) {
                    var request = new HttpGet(apiUrl);
                    request.setHeader("User-Agent", USER_AGENT);

                    try (ClassicHttpResponse response = client.execute(request)) {
                        String jsonResponse = EntityUtils.toString(response.getEntity());
                        JsonNode data = objectMapper.readTree(jsonResponse);

                        // Extract video info - matches JS data structure
                        String title = data.has("title") ? data.get("title").asText() : "-";
                        String author = data.has("author") ? data.get("author").asText() : "-";
                        String description = data.has("description") ? data.get("description").asText() : "-";
                        String videoUrl = data.has("url") ? data.get("url").asText() : null;
                        String thumbnail = data.has("thumbnail") ? data.get("thumbnail").asText() : null;

                        if (videoUrl == null) {
                            throw new RuntimeException("Video URL not found in API response");
                        }

                        // Build message text - matches JS format exactly
                        String text = String.format("""
                                **from :** <@%s>

                                **YouTube**
                                **Title:** %s
                                **Author:** %s
                                **Description:** %s""",
                                event.getAuthor().getId(), title, author, description);

                        // Check file size and decide how to send
                        long fileSize = getFileSize(videoUrl);
                        boolean canSendAttachment = fileSize <= FILE_SIZE_LIMIT;

                        if (!canSendAttachment) {
                            // File too large or unknown size - send download link + thumbnail
                            // Matches JS behavior exactly
                            if (thumbnail != null && !thumbnail.isEmpty()) {
                                try {
                                    byte[] thumbBytes = downloadData(thumbnail);
                                    try (InputStream thumbStream = new ByteArrayInputStream(thumbBytes)) {
                                        var thumbAttachment = FileUpload.fromData(thumbStream, "thumbnail.jpg");
                                        event.getChannel().sendMessage(text + "\n\n[Download Video](" + videoUrl + ")")
                                                .addFiles(thumbAttachment)
                                                .setAllowedMentions(Collections.emptyList())
                                                .queue();
                                    }
                                } catch (Exception thumbError) {
                                    // Fallback without thumbnail
                                    event.getChannel().sendMessage(text + "\n\n[Download Video](" + videoUrl + ")")
                                            .setAllowedMentions(Collections.emptyList())
                                            .queue();
                                }
                            } else {
                                event.getChannel().sendMessage(text + "\n\n[Download Video](" + videoUrl + ")")
                                        .setAllowedMentions(Collections.emptyList())
                                        .queue();
                            }
                        } else {
                            // Download and send video as attachment
                            byte[] videoBytes = downloadData(videoUrl);
                            try (InputStream videoStream = new ByteArrayInputStream(videoBytes)) {
                                var videoAttachment = FileUpload.fromData(videoStream, "youtube.mp4");
                                event.getChannel().sendMessage(text)
                                        .addFiles(videoAttachment)
                                        .setAllowedMentions(Collections.emptyList())
                                        .queue();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("YouTube download error: " + e.getMessage());
                e.printStackTrace();
                event.getChannel().sendMessage("An error occurred: " + e.getMessage()).queue();
            }
        });
    }
}

