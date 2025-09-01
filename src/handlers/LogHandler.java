package handlers;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import java.awt.Color;
import java.time.Instant;
import java.util.List;

public class LogHandler {

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
        }
    }

    public static void sendLog(JDA client, String channelId, LogDetails logDetails) {
        try {
            if (logDetails == null) {
                System.err.println("logDetails is invalid or empty!");
                return;
            }

            TextChannel logChannel = client.getTextChannelById(channelId);
            if (logChannel == null) {
                System.err.println("Log channel with ID " + channelId + " not found!");
                return;
            }

            String footerText = "";
            if (logDetails.userId != null && logDetails.messageId != null) {
                footerText = "User ID: " + logDetails.userId + " | Message ID: " + logDetails.messageId;
            } else if (logDetails.userId != null) {
                footerText = "User ID: " + logDetails.userId;
            } else if (logDetails.messageId != null) {
                footerText = "Message ID: " + logDetails.messageId;
            } else {
                footerText = "Unknown";
            }

            EmbedBuilder embed = new EmbedBuilder()
                .setColor(logDetails.color != null ? logDetails.color : new Color(0x00ffed))
                .setTitle(logDetails.title != null ? logDetails.title : "Log Notification")
                .setDescription(logDetails.description != null ? logDetails.description : "No description provided.")
                .setFooter(footerText)
                .setTimestamp(logDetails.timestamp != null ? logDetails.timestamp : Instant.now());

            if (logDetails.author != null) {
                embed.setAuthor(
                    logDetails.author.name != null ? logDetails.author.name : "Bot System",
                    null,
                    logDetails.author.iconUrl != null ? logDetails.author.iconUrl : client.getSelfUser().getAvatarUrl()
                );
            } else {
                embed.setAuthor("Bot System", null, client.getSelfUser().getAvatarUrl());
            }

            if (logDetails.fields != null) {
                for (MessageEmbed.Field field : logDetails.fields) {
                    embed.addField(field.getName(), field.getValue(), field.isInline());
                }
            }

            logChannel.sendMessageEmbeds(embed.build()).queue();

        } catch (Exception error) {
            System.err.println("Error sending log to channel " + channelId + ": " + error.getMessage());
        }
    }
}
