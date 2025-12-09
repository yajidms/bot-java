package com.discord.bot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.awt.Color;
import java.time.Instant;

public class Ping {

    public static SlashCommandData getCommandData() {
        return Commands.slash("ping", "Check bot and API latency")
                .setGuildOnly(false); // Allows DM usage
    }

    public static void execute(SlashCommandInteractionEvent interaction) {
        try {
            interaction.deferReply().queue();

            long startTime = System.currentTimeMillis();

            interaction.getHook().retrieveOriginal().queue(reply -> {
                long botLatency = reply.getTimeCreated().toEpochSecond() * 1000 - interaction.getTimeCreated().toEpochSecond() * 1000;
                long apiLatency = interaction.getJDA().getGatewayPing();

                EmbedBuilder pingEmbed = new EmbedBuilder()
                    .setColor(Color.RED)
                    .setTitle("🏓 Pong!")
                    .addField("Bot Latency", "`" + Math.abs(botLatency) + "ms`", true)
                    .addField("API Latency", "`" + apiLatency + "ms`", true)
                    .setTimestamp(Instant.now());

                interaction.getHook().editOriginalEmbeds(pingEmbed.build()).queue();
            }, error -> {
                System.err.println("Ping command error: " + error.getMessage());

                String errorMessage = "❌ Failed to check latency";
                interaction.getHook().editOriginal(errorMessage).queue();
            });

        } catch (Exception error) {
            System.err.println("Ping command error: " + error.getMessage());

            String errorMessage = "❌ Failed to check latency";
            if (interaction.isAcknowledged()) {
                interaction.getHook().editOriginal(errorMessage).queue();
            } else {
                interaction.reply(errorMessage).setEphemeral(true).queue();
            }
        }
    }
}
