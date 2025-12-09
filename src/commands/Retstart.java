package commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import handlers.LogHandler;
import io.github.cdimascio.dotenv.Dotenv;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

public class Retstart {

    private static final Dotenv dotenv = Dotenv.configure().load();
    private static final List<String> DEVELOPER_IDS = Arrays.asList(
        dotenv.get("DEV_ID") != null ? dotenv.get("DEV_ID").split(",") : new String[0]
    );

    public static SlashCommandData getCommandData() {
        return Commands.slash("restart", "[Developer Only] Restart bot system")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .setGuildOnly(true);
    }

    public static void execute(SlashCommandInteractionEvent interaction) {
        if (!DEVELOPER_IDS.contains(interaction.getUser().getId())) {
            interaction.reply("❌ You don't have permission to use this command!").setEphemeral(true).queue();
            return;
        }

        try {
            interaction.reply("🔄 **System Rebooting**\nBot akan restart dalam 3 detik...").setEphemeral(true).queue();

            System.out.println("[SYSTEM] Initiated restart by: " + interaction.getUser().getAsTag());
            System.out.println("=== SHUTTING DOWN ===");

            // Send log to dev channel
            String devLogChannelId = dotenv.get("DEV_LOG_CHANNEL_ID");
            if (devLogChannelId != null) {
                LogHandler.LogDetails logDetails = new LogHandler.LogDetails();
                logDetails.title = "[SYSTEM] Bot Restart";
                logDetails.description = "Bot restart initiated by developer\n" +
                    "**User:** " + interaction.getUser().getAsTag() + "\n" +
                    "**Time:** " + java.time.Instant.now().toString();
                logDetails.color = new Color(0xFF6600);
                logDetails.author = new LogHandler.LogDetails.Author(
                    "[SYSTEM] " + interaction.getUser().getAsTag(),
                    interaction.getUser().getAvatarUrl()
                );

                LogHandler.sendLog(interaction.getJDA(), devLogChannelId, logDetails);
            }

            // Schedule shutdown after 3 seconds
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    System.out.println("=== BOT RESTARTING ===");
                    System.exit(0);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();

        } catch (Exception error) {
            System.err.println("Restart command error: " + error.getMessage());
            interaction.reply("❌ Failed to restart bot").setEphemeral(true).queue();
        }
    }
}
