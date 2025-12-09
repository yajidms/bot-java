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

public class Unmute {

    private static final Dotenv dotenv = Dotenv.configure().load();

    public static SlashCommandData getCommandData() {
        return Commands.slash("unmute", "Unmute a user.")
                .addOption(OptionType.USER, "user", "The user to unmute.", true)
                .addOption(OptionType.STRING, "reason", "Reason for unmuting.", false)
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

        if (!member.getRoles().contains(mutedRole)) {
            interaction.reply("❌ This user is not muted.").setEphemeral(true).queue();
            return;
        }

        try {
            interaction.getGuild().removeRoleFromMember(member, mutedRole).reason(reason).queue(success -> {
                interaction.reply("✅ **" + user.getAsTag() + "** has been unmuted.\n**Reason:** " + reason).queue();

                LogHandler.LogDetails logDetails = new LogHandler.LogDetails();
                logDetails.title = "User Unmuted";
                logDetails.description = "**User:** " + user.getAsTag() + " (" + user.getId() + ")\n" +
                    "**Moderator:** " + interaction.getUser().getAsTag() + "\n" +
                    "**Reason:** " + reason;
                logDetails.color = Color.GREEN;
                logDetails.userId = user.getId();

            }, error -> {
                System.err.println("Failed to unmute user: " + error.getMessage());
                interaction.reply("❌ Failed to unmute user.").setEphemeral(true).queue();
            });

        } catch (Exception error) {
            System.err.println("Unmute command error: " + error.getMessage());
            interaction.reply("❌ Failed to unmute user.").setEphemeral(true).queue();
        }
    }
}
