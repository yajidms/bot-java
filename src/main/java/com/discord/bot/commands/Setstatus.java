package com.discord.bot.commands;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import com.discord.bot.handlers.LogHandler;
import io.github.cdimascio.dotenv.Dotenv;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Setstatus {

    private static final Dotenv dotenv = Dotenv.configure().load();
    private static final List<String> DEVELOPER_IDS = Arrays.asList(
        dotenv.get("DEV_ID") != null ? dotenv.get("DEV_ID").split(",") : new String[0]
    );
    private static final Map<String, Object> activeTimeouts = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public static SlashCommandData getCommandData() {
        return Commands.slash("set", "[Developer Only] Manage bot settings")
                .addSubcommands(
                    new SubcommandData("status", "[Developer Only] Set temporary or permanent bot status")
                        .addOptions(
                            new OptionData(OptionType.STRING, "type", "The type of status to set", true)
                                .addChoice("Online", "online")
                                .addChoice("Idle", "idle")
                                .addChoice("Do Not Disturb", "dnd")
                                .addChoice("Invisible", "invisible"),
                            new OptionData(OptionType.STRING, "message", "Custom status text to display", true),
                            new OptionData(OptionType.STRING, "duration", "How long should this status last?", true)
                                .addChoice("Today", "today")
                                .addChoice("4 hours", "4h")
                                .addChoice("1 hour", "1h")
                                .addChoice("30 minutes", "30m")
                                .addChoice("Permanent (Don't Clear)", "dont_clear")
                        ),
                    new SubcommandData("activity", "[Developer Only] Set temporary or permanent bot activity")
                        .addOptions(
                            new OptionData(OptionType.STRING, "type", "The type of activity to set", true)
                                .addChoice("Playing", "PLAYING")
                                .addChoice("Listening", "LISTENING")
                                .addChoice("Watching", "WATCHING")
                                .addChoice("Streaming", "STREAMING"),
                            new OptionData(OptionType.STRING, "message", "Activity text to display", true),
                            new OptionData(OptionType.STRING, "duration", "How long should this activity last?", true)
                                .addChoice("Today", "today")
                                .addChoice("4 hours", "4h")
                                .addChoice("1 hour", "1h")
                                .addChoice("30 minutes", "30m")
                                .addChoice("Permanent (Don't Clear)", "dont_clear")
                        )
                )
                .setGuildOnly(true);
    }

    public static void execute(SlashCommandInteractionEvent interaction) {
        if (!DEVELOPER_IDS.contains(interaction.getUser().getId())) {
            interaction.reply("❌ You don't have permission to use this command!").setEphemeral(true).queue();
            return;
        }

        String subcommand = interaction.getSubcommandName();

        try {
            if ("status".equals(subcommand)) {
                handleStatusCommand(interaction);
            } else if ("activity".equals(subcommand)) {
                handleActivityCommand(interaction);
            }
        } catch (Exception error) {
            System.err.println("Setstatus command error: " + error.getMessage());
            interaction.reply("❌ Failed to set status/activity").setEphemeral(true).queue();
        }
    }

    private static void handleStatusCommand(SlashCommandInteractionEvent interaction) {
        String statusType = interaction.getOption("type").getAsString();
        String message = interaction.getOption("message").getAsString();
        String duration = interaction.getOption("duration").getAsString();

        OnlineStatus status = switch (statusType) {
            case "online" -> OnlineStatus.ONLINE;
            case "idle" -> OnlineStatus.IDLE;
            case "dnd" -> OnlineStatus.DO_NOT_DISTURB;
            case "invisible" -> OnlineStatus.INVISIBLE;
            default -> OnlineStatus.ONLINE;
        };

        // Set bot status (simplified - JDA handles this differently)
        interaction.getJDA().getPresence().setStatus(status);

        String responseMessage = String.format("✅ Bot status set to **%s** with message: \"%s\"",
            statusType.toUpperCase(), message);

        if (!"dont_clear".equals(duration)) {
            long delayMillis = parseDuration(duration);
            scheduler.schedule(() -> {
                interaction.getJDA().getPresence().setStatus(OnlineStatus.ONLINE);
                System.out.println("Status reset to default after timeout");
            }, delayMillis, TimeUnit.MILLISECONDS);

            responseMessage += String.format("\n⏰ Will reset after: **%s**", duration);
        }

        interaction.reply(responseMessage).setEphemeral(true).queue();

        // Log the change
        logStatusChange(interaction, "Status", statusType + " - " + message, duration);
    }

    private static void handleActivityCommand(SlashCommandInteractionEvent interaction) {
        String activityType = interaction.getOption("type").getAsString();
        String message = interaction.getOption("message").getAsString();
        String duration = interaction.getOption("duration").getAsString();

        Activity.ActivityType type = switch (activityType) {
            case "PLAYING" -> Activity.ActivityType.PLAYING;
            case "LISTENING" -> Activity.ActivityType.LISTENING;
            case "WATCHING" -> Activity.ActivityType.WATCHING;
            case "STREAMING" -> Activity.ActivityType.STREAMING;
            default -> Activity.ActivityType.PLAYING;
        };

        Activity activity = Activity.of(type, message);
        interaction.getJDA().getPresence().setActivity(activity);

        String responseMessage = String.format("✅ Bot activity set to **%s %s**",
            activityType.toLowerCase(), message);

        if (!"dont_clear".equals(duration)) {
            long delayMillis = parseDuration(duration);
            scheduler.schedule(() -> {
                interaction.getJDA().getPresence().setActivity(null);
                System.out.println("Activity reset to default after timeout");
            }, delayMillis, TimeUnit.MILLISECONDS);

            responseMessage += String.format("\n⏰ Will reset after: **%s**", duration);
        }

        interaction.reply(responseMessage).setEphemeral(true).queue();

        // Log the change
        logStatusChange(interaction, "Activity", activityType + " " + message, duration);
    }

    private static long parseDuration(String duration) {
        return switch (duration) {
            case "30m" -> TimeUnit.MINUTES.toMillis(30);
            case "1h" -> TimeUnit.HOURS.toMillis(1);
            case "4h" -> TimeUnit.HOURS.toMillis(4);
            case "today" -> TimeUnit.HOURS.toMillis(24);
            default -> TimeUnit.HOURS.toMillis(1);
        };
    }

    private static void logStatusChange(SlashCommandInteractionEvent interaction, String changeType, String details, String duration) {
        String devLogChannelId = dotenv.get("DEV_LOG_CHANNEL_ID");
        if (devLogChannelId != null) {
            LogHandler.LogDetails logDetails = new LogHandler.LogDetails();
            logDetails.title = "[SYSTEM] Bot " + changeType + " Changed";
            logDetails.description = String.format("**%s:** %s\n**Duration:** %s\n**Changed by:** %s",
                changeType, details, duration, interaction.getUser().getAsTag());
            logDetails.color = new Color(0x00FF00);
            logDetails.author = new LogHandler.LogDetails.Author(
                "[SYSTEM] " + interaction.getUser().getAsTag(),
                interaction.getUser().getAvatarUrl()
            );

            LogHandler.sendLog(interaction.getJDA(), devLogChannelId, logDetails);
        }
    }
}
