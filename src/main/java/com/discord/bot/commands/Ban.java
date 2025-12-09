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
import java.util.concurrent.TimeUnit;

public class Ban {

    public static SlashCommandData getCommandData() {
        return Commands.slash("ban", "Ban a user.")
                .addOption(OptionType.USER, "user", "The user to be banned.", true)
                .addOption(OptionType.STRING, "waktu", "Ban duration (e.g., 7d, 1h).", false)
                .addOption(OptionType.STRING, "alasan", "Reason for the ban.", false)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS))
                .setGuildOnly(true);
    }

    public static void execute(SlashCommandInteractionEvent interaction) {
        if (!interaction.getMember().hasPermission(Permission.BAN_MEMBERS)) {
            interaction.reply("❌ You don't have permission to use this command!").setEphemeral(true).queue();
            return;
        }

        User user = interaction.getOption("user").getAsUser();
        String duration = interaction.getOption("waktu") != null ? interaction.getOption("waktu").getAsString() : null;
        String reason = interaction.getOption("alasan") != null ? interaction.getOption("alasan").getAsString() : "No reason provided.";
        Member member = interaction.getGuild().getMember(user);

        if (member == null) {
            interaction.reply("User not found in this server.").setEphemeral(true).queue();
            return;
        }

        if (!interaction.getGuild().getSelfMember().canInteract(member)) {
            interaction.reply("❌ I cannot ban this user due to role hierarchy.").setEphemeral(true).queue();
            return;
        }

        if (!interaction.getMember().canInteract(member)) {
            interaction.reply("❌ You cannot ban this user due to role hierarchy.").setEphemeral(true).queue();
            return;
        }

        try {
            // Send DM before ban
            user.openPrivateChannel().queue(privateChannel -> {
                privateChannel.sendMessage("You have been banned from **" + interaction.getGuild().getName() + "**.\n" +
                    "**Reason:** " + reason +
                    (duration != null ? "\n**Duration:** " + duration : "")).queue(
                    success -> proceedWithBan(interaction, member, user, reason, duration),
                    error -> proceedWithBan(interaction, member, user, reason, duration)
                );
            }, error -> proceedWithBan(interaction, member, user, reason, duration));

        } catch (Exception error) {
            System.err.println("Ban command error: " + error.getMessage());
            interaction.reply("❌ Failed to ban user.").setEphemeral(true).queue();
        }
    }

    private static void proceedWithBan(SlashCommandInteractionEvent interaction, Member member, User user, String reason, String duration) {
        interaction.getGuild().ban(user, 0, TimeUnit.SECONDS)
            .reason(reason)
            .queue(success -> {
                String responseMessage = "✅ **" + user.getAsTag() + "** has been banned.\n" +
                    "**Reason:** " + reason +
                    (duration != null ? "\n**Duration:** " + duration : "");

                interaction.reply(responseMessage).queue();

                // Log the ban
                LogHandler.LogDetails logDetails = new LogHandler.LogDetails();
                logDetails.title = "User Banned";
                logDetails.description = "**User:** " + user.getAsTag() + " (" + user.getId() + ")\n" +
                    "**Moderator:** " + interaction.getUser().getAsTag() + "\n" +
                    "**Reason:** " + reason +
                    (duration != null ? "\n**Duration:** " + duration : "");
                logDetails.color = Color.RED;
                logDetails.userId = user.getId();

                // Note: Would need LOG_CHANNEL_ID from environment
                // logHandler.sendLog(interaction.getJDA(), LOG_CHANNEL_ID, logDetails);

            }, error -> {
                System.err.println("Failed to ban user: " + error.getMessage());
                interaction.reply("❌ Failed to ban user.").setEphemeral(true).queue();
            });
    }
}
