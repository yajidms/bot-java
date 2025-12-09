package commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import handlers.LogHandler;

import java.awt.Color;

public class Kick {

    public static SlashCommandData getCommandData() {
        return Commands.slash("kick", "Kicks a user from the server.")
                .addOption(OptionType.USER, "user", "The user to be kicked.", true)
                .addOption(OptionType.STRING, "alasan", "Reason for the kick.", false)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.KICK_MEMBERS))
                .setGuildOnly(true);
    }

    public static void execute(SlashCommandInteractionEvent interaction) {
        if (!interaction.getMember().hasPermission(Permission.KICK_MEMBERS)) {
            interaction.reply("❌ You don't have permission to use this command!").setEphemeral(true).queue();
            return;
        }

        User user = interaction.getOption("user").getAsUser();
        String reason = interaction.getOption("alasan") != null ? interaction.getOption("alasan").getAsString() : "No reason provided.";
        Member member = interaction.getGuild().getMember(user);

        if (member == null) {
            interaction.reply("User not found in this server.").setEphemeral(true).queue();
            return;
        }

        if (!interaction.getGuild().getSelfMember().canInteract(member)) {
            interaction.reply("❌ I cannot kick this user due to role hierarchy.").setEphemeral(true).queue();
            return;
        }

        if (!interaction.getMember().canInteract(member)) {
            interaction.reply("❌ You cannot kick this user due to role hierarchy.").setEphemeral(true).queue();
            return;
        }

        try {
            member.kick().reason(reason).queue(success -> {
                interaction.reply("✅ **" + user.getAsTag() + "** has been kicked.\n**Reason:** " + reason).queue();

                LogHandler.LogDetails logDetails = new LogHandler.LogDetails();
                logDetails.title = "User Kicked";
                logDetails.description = "**User:** " + user.getAsTag() + " (" + user.getId() + ")\n" +
                    "**Moderator:** " + interaction.getUser().getAsTag() + "\n" +
                    "**Reason:** " + reason;
                logDetails.color = new Color(0xFFA500);
                logDetails.userId = user.getId();

            }, error -> {
                System.err.println("Failed to kick user: " + error.getMessage());
                interaction.reply("❌ Failed to kick user.").setEphemeral(true).queue();
            });

        } catch (Exception error) {
            System.err.println("Kick command error: " + error.getMessage());
            interaction.reply("❌ Failed to kick user.").setEphemeral(true).queue();
        }
    }
}
