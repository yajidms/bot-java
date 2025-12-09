package handlers;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Handler for sending logs to Discord channels.
 */
public class LogHandler {

    /**
     * Log details class
     */
    public static class LogDetails {
        public Color color;
        public Author author;
        public String title;
        public String description;
        public List<MessageEmbed.Field> fields;
        public String userId;
        public String messageId;
        public Instant timestamp;

        public static class Author {
            public String name;
            public String iconUrl;

            public Author(String name, String iconUrl) {
                this.name = name;
                this.iconUrl = iconUrl;
            }
        }

        public LogDetails() {
            this.color = new Color(0x00ffed);
            this.timestamp = Instant.now();
            this.fields = new ArrayList<>();
        }
    }

    /**
     * Sends log to specified channel with embed format
     */
    public static void sendLog(JDA client, String channelId, LogDetails logDetails) {
        try {
            if (logDetails == null) {
                System.err.println("logDetails is invalid or empty!");
                return;
            }

            if (channelId == null || channelId.isEmpty()) {
                System.err.println("Log channel ID is null or empty!");
                return;
            }

            TextChannel logChannel = client.getTextChannelById(channelId);
            if (logChannel == null) {
                System.err.println("Log channel with ID " + channelId + " not found!");
                return;
            }

            // Build footer text
            String footerText;
            if (logDetails.userId != null && logDetails.messageId != null) {
                footerText = "User ID: " + logDetails.userId + " | Message ID: " + logDetails.messageId;
            } else if (logDetails.userId != null) {
                footerText = "User ID: " + logDetails.userId;
            } else if (logDetails.messageId != null) {
                footerText = "Message ID: " + logDetails.messageId;
            } else {
                footerText = "Unknown";
            }

            // Build embed
            var embed = new EmbedBuilder()
                    .setColor(logDetails.color != null ? logDetails.color : new Color(0x00ffed))
                    .setTitle(logDetails.title != null ? logDetails.title : "Log Notification")
                    .setDescription(logDetails.description != null ? logDetails.description : "No description provided.")
                    .setFooter(footerText)
                    .setTimestamp(logDetails.timestamp != null ? logDetails.timestamp : Instant.now());

            // Set author
            if (logDetails.author != null) {
                embed.setAuthor(
                        logDetails.author.name != null ? logDetails.author.name : "Bot System",
                        null,
                        logDetails.author.iconUrl != null ? logDetails.author.iconUrl : client.getSelfUser().getAvatarUrl()
                );
            } else {
                embed.setAuthor("Bot System", null, client.getSelfUser().getAvatarUrl());
            }

            // Add fields
            if (logDetails.fields != null) {
                for (MessageEmbed.Field field : logDetails.fields) {
                    if (field != null) {
                        embed.addField(field.getName(), field.getValue(), field.isInline());
                    }
                }
            }

            logChannel.sendMessageEmbeds(embed.build()).queue();

        } catch (Exception error) {
            System.err.println("Error sending log to channel " + channelId + ": " + error.getMessage());
            error.printStackTrace();
        }
    }

    /**
     * Creates a simple log for common use cases
     */
    public static void sendSimpleLog(JDA client, String channelId, String title, String description, String userId) {
        LogDetails details = new LogDetails();
        details.title = title;
        details.description = description;
        details.userId = userId;
        sendLog(client, channelId, details);
    }

    /**
     * Creates an error log with red color
     */
    public static void sendErrorLog(JDA client, String channelId, String title, String errorMessage, String userId) {
        LogDetails details = new LogDetails();
        details.title = title;
        details.description = errorMessage;
        details.userId = userId;
        details.color = Color.RED;
        sendLog(client, channelId, details);
    }
}

