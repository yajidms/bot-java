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

public class TwitterHandler {
    
    private static final long FILE_SIZE_LIMIT = 100 * 1024 * 1024; // 100MB
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
    
    public static void handleX(MessageReceivedEvent event) {
        String content = event.getMessage().getContentDisplay();
        String[] args = content.trim().split("\\s+");
        
        if (args.length == 0 || !args[0].startsWith("f.x")) return;
        
        if (args.length < 2 || !args[1].matches("^https?://\\S+")) {
            event.getChannel().sendMessage("Enter a valid Twitter/X Video URL!\nExample: `f.x https://x.com/...`").queue();
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                String messageContent = "";
                if (args.length > 2) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 2; i < args.length; i++) {
                        sb.append(args[i]).append(" ");
                    }
                    messageContent = sb.toString().trim();
                }
                
                String apiUrl = "https://api.ryzendesu.vip/api/downloader/twitter?url=" + 
                    URLEncoder.encode(args[1], StandardCharsets.UTF_8);
                
                event.getMessage().delete().queue(null, throwable -> {});
                
                try (CloseableHttpClient client = HttpClients.createDefault()) {
                    HttpGet request = new HttpGet(apiUrl);
                    request.setHeader("User-Agent", 
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36");
                    
                    try (ClassicHttpResponse response = client.execute(request)) {
                        String jsonResponse = EntityUtils.toString(response.getEntity());
                        JsonNode data = objectMapper.readTree(jsonResponse);
                        
                        if (!data.has("media") || data.get("media").size() == 0) {
                            throw new RuntimeException("No media found");
                        }
                        
                        JsonNode mediaData = data.get("media").get(0);
                        String videoUrl = mediaData.get("url").asText();
                        
                        long fileSize = getFileSize(videoUrl);
                        
                        if (fileSize > FILE_SIZE_LIMIT) {
                            // File too large, send URL instead
                            String modifiedUrl = videoUrl.replace("dl=1", "dl=0");
                            String finalMessage = messageContent.isEmpty() ? "᲼" : messageContent;
                            event.getChannel().sendMessage("[" + finalMessage + "](" + modifiedUrl + ")").queue();
                        } else {
                            // Send as attachment
                            try (InputStream videoStream = new HttpGet(videoUrl).getEntity().getContent()) {
                                FileUpload attachment = FileUpload.fromData(videoStream, "x.mp4");
                                event.getChannel().sendMessage(messageContent)
                                    .addFiles(attachment)
                                    .queue();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Twitter download error: " + e.getMessage());
                event.getChannel().sendMessage("Failed to download the video").queue();
            }
        });
    }
}
