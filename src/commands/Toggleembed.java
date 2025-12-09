package commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import handlers.LogHandler;
import io.github.cdimascio.dotenv.Dotenv;

import java.awt.Color;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * Command to toggle embed detection system.
 */
public class Toggleembed {

    private static final Dotenv dotenv = Dotenv.configure().load();
    private static final List<String> DEVELOPER_IDS;
    private static boolean embedDetectionStatus = true;

    static {
        String devIdEnv = dotenv.get("DEV_ID");
        if (devIdEnv != null && !devIdEnv.isEmpty()) {
            DEVELOPER_IDS = Arrays.asList(devIdEnv.split(","))
                    .stream()
                    .map(String::trim)
                    .toList();
        } else {
            DEVELOPER_IDS = List.of();
        }
    }

    public static CommandData getCommandData() {
        return Commands.slash("toggleembed", "[Developer Only] Toggle embed detection system")
                .addOptions(
                        new OptionData(OptionType.STRING, "opsi", "Embed detection system status", true)
                                .addChoice("On", "on")
                                .addChoice("Off", "off")
                )
                .setGuildOnly(true);
    }

    public static void execute(SlashCommandInteractionEvent interaction) {
        // Check if user is a developer
        if (!DEVELOPER_IDS.contains(interaction.getUser().getId())) {
            interaction.reply("🚫 **Developer Access Required**\nThis command is for the development team only!")
                    .setEphemeral(true).queue();
            return;
        }

        try {
            String opsi = interaction.getOption("opsi").getAsString();
            boolean currentStatus = getEmbedDetectionStatus();

            // Check if status is already set to requested value
            if ((opsi.equals("on") && currentStatus) || (opsi.equals("off") && !currentStatus)) {
                interaction.reply("⚠️ Embed detection system is already **" +
                        (currentStatus ? "active" : "inactive") + "**")
                        .setEphemeral(true).queue();
                return;
            }

            // Update status
            embedDetectionStatus = opsi.equals("on");
            boolean newStatus = getEmbedDetectionStatus();

            String statusText = newStatus ? "**ACTIVE** 🟢" : "**INACTIVE** 🔴";
            interaction.reply("✅ **Embed Detection System**\nStatus: " + statusText)
                    .setEphemeral(true).queue();

            // Logging
            System.out.println("[SYSTEM] Embed detection set to: " + newStatus +
                    " by " + interaction.getUser().getAsTag());

            // Send log to dev channel
            String devLogChannelId = dotenv.get("DEV_LOG_CHANNEL_ID");
            if (devLogChannelId != null && !devLogChannelId.isEmpty()) {
                LogHandler.LogDetails logDetails = new LogHandler.LogDetails();
                logDetails.title = "EMBED DETECTION UPDATE";
                logDetails.description = String.format("""
                        **New Status**: %s
                        **Changed By**: <@%s>
                        **Environment**: `%s`""",
                        newStatus ? "ACTIVE" : "INACTIVE",
                        interaction.getUser().getId(),
                        System.getProperty("java.version"));
                logDetails.color = newStatus ? new Color(0x00FF00) : new Color(0xFF0000);
                logDetails.author = new LogHandler.LogDetails.Author(
                        "[SYSTEM] " + interaction.getUser().getAsTag(),
                        interaction.getUser().getAvatarUrl()
                );
                logDetails.fields = List.of(
                        new net.dv8tion.jda.api.entities.MessageEmbed.Field("Command", "`/toggleembed`", true),
                        new net.dv8tion.jda.api.entities.MessageEmbed.Field("Time",
                                "<t:" + Instant.now().getEpochSecond() + ":R>", true)
                );
                logDetails.timestamp = Instant.now();

                LogHandler.sendLog(interaction.getJDA(), devLogChannelId, logDetails);
            }

        } catch (Exception error) {
            System.err.println("[SYSTEM ERROR] Failed to update embed detection: " + error.getMessage());
            error.printStackTrace();
            interaction.reply("❌ **System Update Failed**\nAn internal error occurred!")
                    .setEphemeral(true).queue();
        }
    }

    public static boolean getEmbedDetectionStatus() {
        return embedDetectionStatus;
    }

    public static void setEmbedDetectionStatus(boolean status) {
        embedDetectionStatus = status;
    }
}

