package com.discord.bot.handlers;

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
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Handler for Quote of the Day functionality.
 */
public class QuoteHandler {

    private static final Dotenv dotenv = Dotenv.configure().load();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * Quote data class
     */
    public record Quote(String quote, String author) {}

    /**
     * Fetches Quote of the Day from ZenQuotes API
     */
    public static Quote getQuoteOfTheDay() {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            var request = new HttpGet("https://zenquotes.io/api/random");

            try (ClassicHttpResponse response = client.execute(request)) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                JsonNode jsonArray = objectMapper.readTree(jsonResponse);

                if (jsonArray.isArray() && !jsonArray.isEmpty()) {
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

    /**
     * Sends QOTD to Discord channel with embed format
     */
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
            if (quoteData.quote() != null && quoteData.author() != null) {
                var embed = new EmbedBuilder()
                        .setColor(new Color(0x00ffed))
                        .setTitle("Quote of the Day")
                        .setDescription("\"" + quoteData.quote() + "\"")
                        .setFooter("- " + quoteData.author())
                        .setTimestamp(Instant.now())
                        .build();

                channel.sendMessageEmbeds(embed).queue();
                System.out.println("QOTD sent successfully");
            } else {
                System.err.println("Failed to get Quote of the Day");
            }
        } catch (Exception e) {
            System.err.println("Error sending QOTD: " + e.getMessage());
        }
    }

    /**
     * Schedules QOTD based on time from .env (format HH:mm UTC time)
     */
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

            // Calculate initial delay until next scheduled time
            LocalTime scheduledTime = LocalTime.of(utcHour, utcMinute);
            LocalTime nowUtc = LocalTime.now(ZoneOffset.UTC);

            long initialDelay;
            if (nowUtc.isBefore(scheduledTime)) {
                initialDelay = ChronoUnit.MINUTES.between(nowUtc, scheduledTime);
            } else {
                // Schedule for tomorrow
                initialDelay = ChronoUnit.MINUTES.between(nowUtc, LocalTime.MAX) +
                        ChronoUnit.MINUTES.between(LocalTime.MIN, scheduledTime) + 1;
            }

            // Schedule daily execution
            scheduler.scheduleAtFixedRate(
                    () -> sendQOTD(client),
                    initialDelay,
                    24 * 60, // 24 hours in minutes
                    TimeUnit.MINUTES
            );

            String formattedHour = String.format("%02d", utcHour);
            String formattedMinute = String.format("%02d", utcMinute);

            System.out.println("QOTD scheduled daily at " + formattedHour + ":" + formattedMinute + " UTC.");

        } catch (Exception e) {
            System.err.println("Error scheduling QOTD: " + e.getMessage());
        }
    }

    /**
     * Gets QOTD for user (via command), returns embed
     */
    public static EmbedBuilder getQOTDForUser() {
        Quote quoteData = getQuoteOfTheDay();

        if (quoteData.quote() == null || quoteData.author() == null) {
            return new EmbedBuilder()
                    .setDescription("Failed to get Quote of the Day. Please try again later!")
                    .setColor(Color.RED);
        }

        return new EmbedBuilder()
                .setColor(new Color(0x00ffed))
                .setTitle("Quote of the Day")
                .setDescription("\"" + quoteData.quote() + "\"")
                .setFooter("- " + quoteData.author())
                .setTimestamp(Instant.now());
    }

    /**
     * Shuts down the scheduler gracefully
     */
    public static void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}

