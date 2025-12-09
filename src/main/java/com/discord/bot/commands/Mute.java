package com.discord.bot.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import com.discord.bot.handlers.LogHandler;
import io.github.cdimascio.dotenv.Dotenv;

import java.awt.Color;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class Mute {

    private static final Dotenv dotenv = Dotenv.configure().load();

    public static SlashCommandData getCommandData() {
        return Commands.slash("mute", "Mute a user.")
                .addOption(OptionType.USER, "user", "The user to be muted.", true)
                .addOption(OptionType.STRING, "waktu", "Mute duration (e.g., 10m, 1h).", false)
                .addOption(OptionType.STRING, "alasan", "Reason for the mute.", false)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MODERATE_MEMBERS))
                .setGuildOnly(true);
    }

    public static void execute(SlashCommandInteractionEvent interaction) {
        if (!interaction.getMember().hasPermission(Permission.MODERATE_MEMBERS)) {
            interaction.reply("❌ You don't have permission to use this command!").setEphemeral(true).queue();
            return;
        }

        User user = interaction.getOption("user").getAsUser();
        String durationStr = interaction.getOption("waktu") != null ? interaction.getOption("waktu").getAsString() : null;
        String reason = interaction.getOption("alasan") != null ? interaction.getOption("alasan").getAsString() : "No reason provided.";

        String mutedRoleId = dotenv.get("MUTED_ROLE_ID");
        Role mutedRole = mutedRoleId != null ? interaction.getGuild().getRoleById(mutedRoleId) : null;

        if (mutedRole == null) {
            interaction.reply("❌ Muted role not found. Please configure MUTED_ROLE_ID in environment variables.").setEphemeral(true).queue();
            return;
        }

        Member member = interaction.getGuild().getMember(user);
        if (member == null) {
            interaction.reply("User not found in this server.").setEphemeral(true).queue();
            return;
        }

        if (!interaction.getGuild().getSelfMember().canInteract(member)) {
            interaction.reply("❌ I cannot mute this user due to role hierarchy.").setEphemeral(true).queue();
            return;
        }

        if (!interaction.getMember().canInteract(member)) {
            interaction.reply("❌ You cannot mute this user due to role hierarchy.").setEphemeral(true).queue();
            return;
        }

        if (member.getRoles().contains(mutedRole)) {
            interaction.reply("❌ This user is already muted.").setEphemeral(true).queue();
            return;
        }

        try {
            final Duration finalDuration = (durationStr != null) ? parseDuration(durationStr) : null;

            interaction.getGuild().addRoleToMember(member, mutedRole).reason(reason).queue(success -> {
                String responseMessage = "✅ **" + user.getAsTag() + "** has been muted.\n**Reason:** " + reason;
                if (finalDuration != null) {
                    responseMessage += "\n**Duration:** " + durationStr;

                    // Schedule unmute
                    scheduleUnmute(interaction.getGuild().getId(), user.getId(), mutedRole.getId(), finalDuration);
                }

                interaction.reply(responseMessage).queue();

                // Log the mute
                LogHandler.LogDetails logDetails = new LogHandler.LogDetails();
                logDetails.title = "User Muted";
                logDetails.description = "**User:** " + user.getAsTag() + " (" + user.getId() + ")\n" +
                    "**Moderator:** " + interaction.getUser().getAsTag() + "\n" +
                    "**Reason:** " + reason +
                    (finalDuration != null ? "\n**Duration:** " + durationStr : "");
                logDetails.color = new Color(0x808080);
                logDetails.userId = user.getId();

            }, error -> {
                System.err.println("Failed to mute user: " + error.getMessage());
                interaction.reply("❌ Failed to mute user.").setEphemeral(true).queue();
            });

        } catch (Exception error) {
            System.err.println("Mute command error: " + error.getMessage());
            interaction.reply("❌ Failed to mute user.").setEphemeral(true).queue();
        }
    }

    private static Duration parseDuration(String durationStr) {
        // Simple duration parser (10m, 1h, 2d, etc.)
        if (durationStr.endsWith("m")) {
            return Duration.of(Long.parseLong(durationStr.replace("m", "")), ChronoUnit.MINUTES);
        } else if (durationStr.endsWith("h")) {
            return Duration.of(Long.parseLong(durationStr.replace("h", "")), ChronoUnit.HOURS);
        } else if (durationStr.endsWith("d")) {
            return Duration.of(Long.parseLong(durationStr.replace("d", "")), ChronoUnit.DAYS);
        }
        return Duration.of(10, ChronoUnit.MINUTES); // Default 10 minutes
    }

    private static void scheduleUnmute(String guildId, String userId, String roleId, Duration duration) {
        // In a real implementation, you'd use a scheduler or database to track temporary mutes
        // This is a simplified version
        System.out.println("Scheduled unmute for user " + userId + " in " + duration.toString());
    }
}
