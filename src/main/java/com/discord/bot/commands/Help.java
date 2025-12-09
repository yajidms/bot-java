package com.discord.bot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.awt.Color;
import java.time.Instant;

public class Help {

    public static SlashCommandData getCommandData() {
        return Commands.slash("help", "Displays a list of all commands.");
    }

    public static void execute(SlashCommandInteractionEvent interaction) {
        try {
            String commandsList = String.join("\n",
                "**/ping**: Check bot and API latency",
                "**/help**: Displays a list of all commands",
                "**/info**: Shows bot information",
                "**/avatar**: Display user avatar",
                "**/banner**: Display user banner",
                "**/userinfo**: Display user information",
                "**/serverinfo**: Display server information",
                "**/list-roles**: Display server roles",
                "**/adzan [city]**: Get prayer times",
                "**/aichat**: Start AI chat session",
                "**/endchat**: End AI chat session",
                "**/downloader [url]**: Download media from social platforms",
                "**/ban [user]**: Ban a user (Admin only)",
                "**/kick [user]**: Kick a user (Admin only)",
                "**/mute [user]**: Mute a user (Admin only)",
                "**/unmute [user]**: Unmute a user (Admin only)",
                "**/timeout [user]**: Timeout a user (Admin only)",
                "**/untimeout [user]**: Remove timeout (Admin only)",
                "**/unban [user_id]**: Unban a user (Admin only)",
                "**/clean [amount]**: Delete messages (Admin only)",
                "**/say [message]**: Send message (Admin only)",
                "**/restart**: Restart bot (Developer only)",
                "**/set**: Manage bot settings (Developer only)"
            );

            EmbedBuilder embed = new EmbedBuilder()
                .setColor(new Color(0x00ffed))
                .setTitle("Command List")
                .setDescription("Here is a list of available commands:\n\n" + commandsList)
                .setFooter("Use these commands to interact with the bot")
                .setTimestamp(Instant.now());

            interaction.replyEmbeds(embed.build()).setEphemeral(true).queue();

        } catch (Exception error) {
            System.err.println("Help command error: " + error.getMessage());
            interaction.reply("❌ Failed to display help").setEphemeral(true).queue();
        }
    }
}
