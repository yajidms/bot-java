package handlers;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;

import java.awt.Color;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class QuoteHandler {

    private static final Dotenv dotenv = Dotenv.configure().load();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static class Quote {
        public String quote;
        public String author;

        public Quote(String quote, String author) {
            this.quote = quote;
            this.author = author;
        }
    }

    public static Quote getQuoteOfTheDay() {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet("https://zenquotes.io/api/random");

            try (ClassicHttpResponse response = client.execute(request)) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                JsonNode jsonArray = objectMapper.readTree(jsonResponse);

                if (jsonArray.isArray() && jsonArray.size() > 0) {
                    JsonNode quoteData = jsonArray.get(0);
                    String quote = quoteData.get("q").asText();
                    String author = quoteData.get("a").asText();
                    return new Quote(quote, author);
                }
            }
        } catch (Exception error) {
            System.err.println("Error fetching quote: " + error.getMessage());
        }
        return new Quote(null, null);
    }

    public static void sendQOTD(JDA client) {
        try {
            String channelId = dotenv.get("QUOTE_CHANNEL_ID");
            if (channelId == null) {
                System.err.println("QUOTE_CHANNEL_ID not found in environment variables!");
                return;
            }

            TextChannel channel = client.getTextChannelById(channelId);
            if (channel == null) {
                System.err.println("Channel not found!");
                return;
            }

            Quote quoteData = getQuoteOfTheDay();
            if (quoteData.quote != null && quoteData.author != null) {
                EmbedBuilder embed = new EmbedBuilder()
                    .setColor(new Color(0x00ffed))
                    .setTitle("Quote of the Day")
                    .setDescription("\"" + quoteData.quote + "\"")
                    .setFooter("- " + quoteData.author)
                    .setTimestamp(Instant.now());

                channel.sendMessageEmbeds(embed.build()).queue();
                System.out.println("QOTD sent successfully");
            } else {
                System.err.println("Failed to get Quote of the Day");
            }
        } catch (Exception e) {
            System.err.println("Error sending QOTD: " + e.getMessage());
        }
    }

    public static void scheduleQOTD(JDA client) {
        try {
            String qotdTime = dotenv.get("QOTD_TIME");
            if (qotdTime == null) {
                System.err.println("QOTD_TIME not found in environment variables!");
                return;
            }

            String[] timeParts = qotdTime.split(":");
            int utcHour = Integer.parseInt(timeParts[0]);
            int utcMinute = Integer.parseInt(timeParts[1]);

            // Calculate initial delay and period (24 hours)
            long initialDelay = calculateInitialDelay(utcHour, utcMinute);
            long period = 24 * 60 * 60; // 24 hours in seconds

            scheduler.scheduleAtFixedRate(() -> sendQOTD(client), initialDelay, period, TimeUnit.SECONDS);

            System.out.println("QOTD scheduled for " + String.format("%02d:%02d", utcHour, utcMinute) + " UTC daily");

        } catch (Exception e) {
            System.err.println("Error scheduling QOTD: " + e.getMessage());
        }
    }

    private static long calculateInitialDelay(int targetHour, int targetMinute) {
        long currentTime = System.currentTimeMillis() / 1000; // Current time in seconds
        long currentDaySeconds = currentTime % (24 * 60 * 60); // Seconds since start of day
        long targetSeconds = targetHour * 60 * 60 + targetMinute * 60; // Target time in seconds since start of day

        if (targetSeconds > currentDaySeconds) {
            return targetSeconds - currentDaySeconds; // Today
        } else {
            return (24 * 60 * 60) - currentDaySeconds + targetSeconds; // Tomorrow
        }
    }
}
