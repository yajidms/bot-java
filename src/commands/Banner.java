package commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.awt.Color;
import java.time.Instant;

public class Banner {

    public static SlashCommandData getCommandData() {
        return Commands.slash("banner", "View user profile banner")
                .addOption(OptionType.USER, "user", "Select a user", false);
    }

    public static void execute(SlashCommandInteractionEvent interaction) {
        User targetUser = interaction.getOption("user") != null ?
            interaction.getOption("user").getAsUser() : interaction.getUser();

        try {
            targetUser.retrieveProfile().queue(profile -> {
                String bannerUrl = profile.getBannerUrl();
                String displayName = targetUser.getGlobalName() != null ?
                    targetUser.getGlobalName() : targetUser.getName();

                if (bannerUrl == null) {
                    interaction.reply("❌ " + displayName + " doesn't have a banner.").setEphemeral(true).queue();
                    return;
                }

                bannerUrl += "?size=512";

                EmbedBuilder embed = new EmbedBuilder()
                    .setColor(new Color(0x00ffed))
                    .setTitle("Banner - " + displayName)
                    .setImage(bannerUrl)
                    .setFooter("Banner")
                    .setTimestamp(Instant.now());

                interaction.replyEmbeds(embed.build()).queue();
            }, error -> {
                interaction.reply("❌ Failed to retrieve user profile.").setEphemeral(true).queue();
            });

        } catch (Exception error) {
            System.err.println("Banner command error: " + error.getMessage());
            interaction.reply("❌ Failed to display banner").setEphemeral(true).queue();
        }
    }
}
