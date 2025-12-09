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

public class Untimeout {

    public static SlashCommandData getCommandData() {
        return Commands.slash("untimeout", "Remove timeout from a user.")
                .addOption(OptionType.USER, "user", "The user to remove timeout from.", true)
                .addOption(OptionType.STRING, "reason", "Reason for removing timeout.", false)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MODERATE_MEMBERS))
                .setGuildOnly(true);
    }

    public static void execute(SlashCommandInteractionEvent interaction) {
        if (!interaction.getMember().hasPermission(Permission.MODERATE_MEMBERS)) {
            interaction.reply("❌ You don't have permission to use this command!").setEphemeral(true).queue();
            return;
        }

        User user = interaction.getOption("user").getAsUser();
        String reason = interaction.getOption("reason") != null ? interaction.getOption("reason").getAsString() : "No reason provided.";

        Member member = interaction.getGuild().getMember(user);
        if (member == null) {
            interaction.reply("User not found in this server.").setEphemeral(true).queue();
            return;
        }

        if (!member.isTimedOut()) {
            interaction.reply("❌ This user is not timed out.").setEphemeral(true).queue();
            return;
        }

        try {
            member.removeTimeout().reason(reason).queue(success -> {
                interaction.reply("✅ **" + user.getAsTag() + "** timeout has been removed.\n**Reason:** " + reason).queue();

                LogHandler.LogDetails logDetails = new LogHandler.LogDetails();
                logDetails.title = "User Timeout Removed";
                logDetails.description = "**User:** " + user.getAsTag() + " (" + user.getId() + ")\n" +
                    "**Moderator:** " + interaction.getUser().getAsTag() + "\n" +
                    "**Reason:** " + reason;
                logDetails.color = Color.GREEN;
                logDetails.userId = user.getId();

            }, error -> {
                System.err.println("Failed to remove timeout: " + error.getMessage());
                interaction.reply("❌ Failed to remove timeout.").setEphemeral(true).queue();
            });

        } catch (Exception error) {
            System.err.println("Untimeout command error: " + error.getMessage());
            interaction.reply("❌ Failed to remove timeout.").setEphemeral(true).queue();
        }
    }
}
