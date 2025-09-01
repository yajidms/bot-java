package commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import handlers.LogHandler;

import java.awt.Color;

public class Unban {

    public static SlashCommandData getCommandData() {
        return Commands.slash("unban", "Unbans a specific user.")
                .addOption(OptionType.STRING, "user_id", "The user ID to unban.", true)
                .setDefaultPermissions(Permission.ADMINISTRATOR.getRawValue())
                .setGuildOnly(true);
    }

    public static void execute(SlashCommandInteractionEvent interaction) {
        if (!interaction.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            interaction.reply("❌ You don't have permission to use this command!").setEphemeral(true).queue();
            return;
        }

        String userId = interaction.getOption("user_id").getAsString();

        try {
            interaction.getGuild().retrieveBanList().queue(banList -> {
                boolean isBanned = banList.stream().anyMatch(ban -> ban.getUser().getId().equals(userId));

                if (!isBanned) {
                    interaction.reply("User with ID **" + userId + "** was not found in the ban list.").setEphemeral(true).queue();
                    return;
                }

                interaction.getGuild().unban(userId).queue(success -> {
                    interaction.reply("User with ID **" + userId + "** has been successfully unbanned.").queue();

                    LogHandler.LogDetails logDetails = new LogHandler.LogDetails();
                    logDetails.title = "User Unbanned";
                    logDetails.description = "**User ID:** " + userId + "\n" +
                        "**Moderator:** " + interaction.getUser().getAsTag();
                    logDetails.color = Color.GREEN;
                    logDetails.userId = userId;

                }, error -> {
                    System.err.println("Failed to unban user: " + error.getMessage());
                    interaction.reply("❌ Failed to unban user.").setEphemeral(true).queue();
                });
            }, error -> {
                interaction.reply("❌ Failed to retrieve ban list.").setEphemeral(true).queue();
            });

        } catch (Exception error) {
            System.err.println("Unban command error: " + error.getMessage());
            interaction.reply("❌ Failed to unban user.").setEphemeral(true).queue();
        }
    }
}
