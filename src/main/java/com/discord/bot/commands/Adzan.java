package com.discord.bot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.awt.Color;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

public class Adzan {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    public static SlashCommandData getCommandData() {
        return Commands.slash("adzan", "Retrieve the adzan schedule by city")
                .addOption(OptionType.STRING, "city", "Name of the city to retrieve the adzan schedule", true);
    }
    
    public static void execute(SlashCommandInteractionEvent interaction) {
        if (!interaction.isFromGuild()) {
            interaction.reply("You can only run this command inside the server.").setEphemeral(true).queue();
            return;
        }
        
        String city = interaction.getOption("city").getAsString();
        String country = "Indonesia";
        
        interaction.deferReply().queue();
        
        CompletableFuture.runAsync(() -> {
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                String url = "http://api.aladhan.com/v1/timingsByCity?city=" + 
                    URLEncoder.encode(city, StandardCharsets.UTF_8) + 
                    "&country=" + URLEncoder.encode(country, StandardCharsets.UTF_8) + 
                    "&method=2";
                
                HttpGet request = new HttpGet(url);
                
                try (ClassicHttpResponse response = client.execute(request)) {
                    String jsonResponse = EntityUtils.toString(response.getEntity());
                    JsonNode data = objectMapper.readTree(jsonResponse);
                    
                    JsonNode timings = data.get("data").get("timings");
                    JsonNode meta = data.get("data").get("meta");
                    
                    LocalDateTime now = LocalDateTime.now();
                    String formattedDate = now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"));
                    
                    String timezone = meta.get("timezone").asText();
                    String timeNow = LocalDateTime.now(ZoneId.of(timezone))
                        .format(DateTimeFormatter.ofPattern("HH:mm"));
                    
                    String capitalizedCity = city.substring(0, 1).toUpperCase() + city.substring(1);
                    
                    EmbedBuilder adzanEmbed = new EmbedBuilder()
                        .setColor(Color.RED)
                        .setTitle("Prayer Times | " + capitalizedCity)
                        .setDescription(formattedDate)
                        .addField("Subuh", timings.get("Fajr").asText(), false)
                        .addField("Dzuhur", timings.get("Dhuhr").asText(), false)
                        .addField("Ashar", timings.get("Asr").asText(), false)
                        .addField("Magrib", timings.get("Maghrib").asText(), false)
                        .addField("Isya", timings.get("Isha").asText(), false)
                        .setFooter("Source: Aladhan API | " + country + " • Today at " + timeNow);
                    
                    interaction.getHook().editOriginalEmbeds(adzanEmbed.build()).queue();
                }
            } catch (Exception error) {
                System.err.println("Adzan command error: " + error.getMessage());
                interaction.getHook().editOriginal(
                    "Failed to take the adzan schedule for " + city + ". Make sure the city name is correct."
                ).queue();
            }
        });
    }
}
