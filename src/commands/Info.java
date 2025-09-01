package commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.ActionRow;

import java.awt.Color;
import java.time.Instant;

public class Info {

    public static SlashCommandData getCommandData() {
        return Commands.slash("info", "Information about this bot.");
    }

    public static void execute(SlashCommandInteractionEvent interaction) {
        try {
            String botAvatar = interaction.getJDA().getSelfUser().getAvatarUrl();
            if (botAvatar == null) {
                botAvatar = interaction.getJDA().getSelfUser().getDefaultAvatarUrl();
            }

            EmbedBuilder infoEmbed = new EmbedBuilder()
                .setColor(new Color(0x00ffed))
                .setTitle("... Bot Information")
                .setDescription(
                    "**... Bot** is a versatile assistant designed to maintain A Server. This bot can detect links from Reddit only then transform them into automatic embeds (but this discontinue for instagram and twitter now, replaced with downloader commands instead). Other features include **AI assistance** for information retrieval without going through a browser."
                )
                .setThumbnail(botAvatar)
                .setFooter("Made with ❤️ by ...")
                .setTimestamp(Instant.now());

            Button platformButton = Button.link("https://www...", "platform-web");

            interaction.replyEmbeds(infoEmbed.build())
                .addActionRow(platformButton)
                .queue();

        } catch (Exception error) {
            System.err.println("Info command error: " + error.getMessage());
            interaction.reply("❌ Failed to display bot information").setEphemeral(true).queue();
        }
    }
}
