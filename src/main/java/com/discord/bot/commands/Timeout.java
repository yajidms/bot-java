package com.discord.bot.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import com.discord.bot.handlers.LogHandler;

import java.awt.Color;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class Timeout {

    public static SlashCommandData getCommandData() {
        return Commands.slash("timeout", "Times out a user.")
                .addOption(OptionType.USER, "user", "The user to timeout.", true)
                .addOption(OptionType.STRING, "duration", "Timeout duration (e.g., 10m, 1h).", true)
                .addOption(OptionType.STRING, "reason", "Timeout reason.", false)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MODERATE_MEMBERS))
                .setGuildOnly(true);
    }

    public static void execute(SlashCommandInteractionEvent interaction) {
        if (!interaction.getMember().hasPermission(Permission.MODERATE_MEMBERS)) {
            interaction.reply("❌ You don't have permission to use this command!").setEphemeral(true).queue();
            return;
        }

        User user = interaction.getOption("user").getAsUser();
        String durationStr = interaction.getOption("duration").getAsString();
        String reason = interaction.getOption("reason") != null ? interaction.getOption("reason").getAsString() : "No reason provided.";

        Member member = interaction.getGuild().getMember(user);
        if (member == null) {
            interaction.reply("User not found in this server.").setEphemeral(true).queue();
            return;
        }

        if (!interaction.getGuild().getSelfMember().canInteract(member)) {
            interaction.reply("❌ I cannot timeout this user due to role hierarchy.").setEphemeral(true).queue();
            return;
        }

        if (!interaction.getMember().canInteract(member)) {
            interaction.reply("❌ You cannot timeout this user due to role hierarchy.").setEphemeral(true).queue();
            return;
        }

        try {
            Duration duration = parseDuration(durationStr);
            if (duration.toDays() > 28) {
                interaction.reply("❌ Timeout duration cannot exceed 28 days.").setEphemeral(true).queue();
                return;
            }

            member.timeoutFor(duration).reason(reason).queue(success -> {
                interaction.reply("✅ **" + user.getAsTag() + "** has been timed out for " + durationStr + ".\n**Reason:** " + reason).queue();

                LogHandler.LogDetails logDetails = new LogHandler.LogDetails();
                logDetails.title = "User Timed Out";
                logDetails.description = "**User:** " + user.getAsTag() + " (" + user.getId() + ")\n" +
                    "**Moderator:** " + interaction.getUser().getAsTag() + "\n" +
                    "**Duration:** " + durationStr + "\n" +
                    "**Reason:** " + reason;
                logDetails.color = new Color(0xFF6B00);
                logDetails.userId = user.getId();

            }, error -> {
                System.err.println("Failed to timeout user: " + error.getMessage());
                interaction.reply("❌ Failed to timeout user.").setEphemeral(true).queue();
            });

        } catch (Exception error) {
            System.err.println("Timeout command error: " + error.getMessage());
            interaction.reply("❌ Invalid duration format. Use format like 10m, 1h, 2d.").setEphemeral(true).queue();
        }
    }

    private static Duration parseDuration(String durationStr) {
        if (durationStr.endsWith("s")) {
            return Duration.of(Long.parseLong(durationStr.replace("s", "")), ChronoUnit.SECONDS);
        } else if (durationStr.endsWith("m")) {
            return Duration.of(Long.parseLong(durationStr.replace("m", "")), ChronoUnit.MINUTES);
        } else if (durationStr.endsWith("h")) {
            return Duration.of(Long.parseLong(durationStr.replace("h", "")), ChronoUnit.HOURS);
        } else if (durationStr.endsWith("d")) {
            return Duration.of(Long.parseLong(durationStr.replace("d", "")), ChronoUnit.DAYS);
        }
        throw new IllegalArgumentException("Invalid duration format");
    }
}
