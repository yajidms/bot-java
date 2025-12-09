package com.discord.bot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class Downloader {

    public static SlashCommandData getCommandData() {
        return Commands.slash("downloader", "Video downloader from various platforms")
                .addOption(OptionType.STRING, "url", "URL of the media to download", true)
                .addOption(OptionType.STRING, "message", "Additional message", false)
                .setGuildOnly(true);
    }

    public static void execute(SlashCommandInteractionEvent interaction) {
        interaction.deferReply().queue();

        String url = interaction.getOption("url").getAsString();
        String message = interaction.getOption("message") != null ?
            interaction.getOption("message").getAsString() : "";

        try {
            // Detect platform and provide appropriate response
            if (url.contains("instagram.com") || url.contains("instagr.am")) {
                interaction.getHook().editOriginal("Instagram downloader: " + url +
                    (message.isEmpty() ? "" : "\nMessage: " + message)).queue();
            } else if (url.contains("tiktok.com")) {
                interaction.getHook().editOriginal("TikTok downloader: " + url +
                    (message.isEmpty() ? "" : "\nMessage: " + message)).queue();
            } else if (url.contains("facebook.com") || url.contains("fb.watch")) {
                interaction.getHook().editOriginal("Facebook downloader: " + url +
                    (message.isEmpty() ? "" : "\nMessage: " + message)).queue();
            } else if (url.contains("twitter.com") || url.contains("x.com")) {
                interaction.getHook().editOriginal("Twitter/X downloader: " + url +
                    (message.isEmpty() ? "" : "\nMessage: " + message)).queue();
            } else {
                interaction.getHook().editOriginal("Unsupported platform. Please use a URL from Instagram, TikTok, Facebook, or Twitter/X.").queue();
            }
        } catch (Exception error) {
            System.err.println("Downloader error: " + error.getMessage());
            interaction.getHook().editOriginal("An error occurred while processing the download request").queue();
        }
    }
}
