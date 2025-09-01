package commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.Color;
import java.time.Instant;

public class Avatar {

    public static SlashCommandData getCommandData() {
        return Commands.slash("avatar", "View user profile avatar")
                .addOption(OptionType.USER, "user", "Select a user", false)
                .addOptions(new OptionData(OptionType.STRING, "type", "Choose avatar type", false)
                        .addChoice("Global", "global")
                        .addChoice("Server", "server"));
    }

    public static void execute(SlashCommandInteractionEvent interaction) {
        try {
            User targetUser = interaction.getOption("user") != null ?
                interaction.getOption("user").getAsUser() : interaction.getUser();
            Member targetMember = interaction.getOption("user") != null ?
                interaction.getOption("user").getAsMember() : interaction.getMember();
            String avatarType = interaction.getOption("type") != null ?
                interaction.getOption("type").getAsString() : "global";

            String avatarUrl;
            boolean isAnimated = false;
            String displayName = targetUser.getGlobalName() != null ?
                targetUser.getGlobalName() : targetUser.getName();

            if ("server".equals(avatarType) && targetMember != null && targetMember.getAvatar() != null) {
                avatarUrl = targetMember.getAvatarUrl() + "?size=512";
                isAnimated = targetMember.getAvatar().startsWith("a_");
            } else {
                if (targetUser.getAvatar() != null) {
                    avatarUrl = targetUser.getAvatarUrl() + "?size=512";
                    isAnimated = targetUser.getAvatar().startsWith("a_");
                } else {
                    // Default avatar
                    long userId = Long.parseLong(targetUser.getId());
                    int defaultAvatarNumber = (int) (userId % 5);
                    avatarUrl = "https://cdn.discordapp.com/embed/avatars/" + defaultAvatarNumber + ".png";
                }
            }

            String title = ("server".equals(avatarType) ? "Server" : "Global") + " Avatar - " + displayName;

            EmbedBuilder embed = new EmbedBuilder()
                .setColor(new Color(0x00ffed))
                .setTitle(title)
                .setImage(avatarUrl)
                .setFooter("Avatar" + (isAnimated ? " (Animated)" : ""))
                .setTimestamp(Instant.now());

            Button downloadButton = Button.link(avatarUrl, "Download Avatar");

            interaction.replyEmbeds(embed.build())
                .addActionRow(downloadButton)
                .queue();

        } catch (Exception error) {
            System.err.println("Avatar command error: " + error.getMessage());
            interaction.reply("❌ Failed to display avatar").setEphemeral(true).queue();
        }
    }
}
