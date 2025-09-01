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
import java.util.concurrent.CompletableFuture;

public class YtdlHandler {

    private static final long FILE_SIZE_LIMIT = 10 * 1024 * 1024; // 10MB
    private static final ObjectMapper objectMapper = new ObjectMapper();

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

    public static void handleYtDownload(MessageReceivedEvent event) {
        String prefix = "f.yt";
        String content = event.getMessage().getContentDisplay();

        if (!content.startsWith(prefix)) return;

        String[] args = content.substring(prefix.length()).trim().split("\\s+");

        if (args.length == 0 || !args[0].matches("https?://\\S+")) {
            event.getChannel().sendMessage("Enter a valid YouTube URL!\nExample: `f.yt https://youtube.com/...`").queue();
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                event.getMessage().delete().queue(null, throwable -> {});

                String ytUrl = args[0];
                String apiUrl = "https://api.ryzendesu.vip/api/downloader/ytmp4?url=" +
                    URLEncoder.encode(ytUrl, StandardCharsets.UTF_8) + "&quality=480";

                try (CloseableHttpClient client = HttpClients.createDefault()) {
                    HttpGet request = new HttpGet(apiUrl);
                    request.setHeader("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36");

                    try (ClassicHttpResponse response = client.execute(request)) {
                        String jsonResponse = EntityUtils.toString(response.getEntity());
                        JsonNode data = objectMapper.readTree(jsonResponse);

                        String title = data.has("title") ? data.get("title").asText() : "-";
                        String author = data.has("author") ? data.get("author").asText() : "-";
                        String description = data.has("description") ? data.get("description").asText() : "-";
                        String videoUrl = data.get("url").asText();
                        String thumbnail = data.has("thumbnail") ? data.get("thumbnail").asText() : null;

                        String text = String.format("**from :** <@%s>\n\n**YouTube**\n**Title:** %s\n**Author:** %s\n**Description:** %s",
                            event.getAuthor().getId(), title, author, description);

                        long fileSize = getFileSize(videoUrl);
                        boolean canSendAttachment = fileSize <= FILE_SIZE_LIMIT;

                        if (!canSendAttachment) {
                            // File too large, send download link + thumbnail
                            if (thumbnail != null) {
                                try (InputStream thumbStream = new HttpGet(thumbnail).getEntity().getContent()) {
                                    FileUpload thumbAttachment = FileUpload.fromData(thumbStream, "thumbnail.jpg");
                                    event.getChannel().sendMessage(text + "\n\n[Download Video](" + videoUrl + ")")
                                        .addFiles(thumbAttachment)
                                        .queue();
                                }
                            } else {
                                event.getChannel().sendMessage(text + "\n\n[Download Video](" + videoUrl + ")").queue();
                            }
                        } else {
                            try (InputStream videoStream = new HttpGet(videoUrl).getEntity().getContent()) {
                                FileUpload videoAttachment = FileUpload.fromData(videoStream, "youtube.mp4");
                                event.getChannel().sendMessage(text)
                                    .addFiles(videoAttachment)
                                    .queue();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("YouTube download error: " + e.getMessage());
                event.getChannel().sendMessage("An error occurred: " + e.getMessage()).queue();
            }
        });
    }
}
