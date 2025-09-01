package commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.awt.Color;
import java.util.stream.Collectors;

public class ListRoles {

    public static SlashCommandData getCommandData() {
        return Commands.slash("list-roles", "Displays a list of all roles on this server")
                .setGuildOnly(true);
    }

    public static void execute(SlashCommandInteractionEvent interaction) {
        try {
            String roleList = interaction.getGuild().getRoles().stream()
                .filter(role -> !role.getName().equals("@everyone") &&
                               !role.isManaged() &&
                               !role.getId().equals(interaction.getGuild().getId()))
                .sorted((a, b) -> Integer.compare(b.getPosition(), a.getPosition()))
                .map(role -> "<@&" + role.getId() + ">")
                .collect(Collectors.joining(", "));

            if (roleList.isEmpty()) {
                roleList = "Tidak ada role selain @everyone";
            }

            long roleCount = interaction.getGuild().getRoles().stream()
                .filter(role -> !role.getName().equals("@everyone") &&
                               !role.isManaged() &&
                               !role.getId().equals(interaction.getGuild().getId()))
                .count();

            EmbedBuilder rolesEmbed = new EmbedBuilder()
                .setColor(Color.RED)
                .setTitle("Daftar Role di " + interaction.getGuild().getName())
                .setDescription(roleList)
                .setFooter("Total: " + roleCount + " roles");

            interaction.replyEmbeds(rolesEmbed.build()).queue();

        } catch (Exception error) {
            System.err.println("List-roles command error: " + error.getMessage());
            interaction.reply("❌ Failed to display roles").setEphemeral(true).queue();
        }
    }
}
