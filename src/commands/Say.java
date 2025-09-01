package commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.utils.FileUpload;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class Say {

    private static final Dotenv dotenv = Dotenv.configure().load();
    private static final List<String> DEVELOPER_IDS = Arrays.asList(
        dotenv.get("DEV_ID") != null ? dotenv.get("DEV_ID").split(",") : new String[0]
    );

    public static SlashCommandData getCommandData() {
        return Commands.slash("say", "Send a message or reply to a specific message")
                .addOption(OptionType.STRING, "message", "Content of the message to send", true)
                .addOption(OptionType.STRING, "reply_to", "ID of the message to reply to (optional)", false)
                .addOption(OptionType.ATTACHMENT, "file", "File to attach (optional)", false);
    }

    public static void execute(SlashCommandInteractionEvent interaction) {
        if (!interaction.getMember().hasPermission(Permission.ADMINISTRATOR) &&
            !DEVELOPER_IDS.contains(interaction.getUser().getId())) {
            interaction.reply("❌ You don't have permission to use this command!").setEphemeral(true).queue();
            return;
        }

        try {
            String messageContent = interaction.getOption("message").getAsString();
            String replyTargetId = interaction.getOption("reply_to") != null ?
                interaction.getOption("reply_to").getAsString() : null;
            Message.Attachment attachment = interaction.getOption("file") != null ?
                interaction.getOption("file").getAsAttachment() : null;

            interaction.deferReply(true).queue();

            if (replyTargetId != null) {
                // Reply to specific message
                interaction.getChannel().retrieveMessageById(replyTargetId).queue(targetMessage -> {
                    if (attachment != null) {
                        try (InputStream inputStream = new URL(attachment.getUrl()).openStream()) {
                            FileUpload fileUpload = FileUpload.fromData(inputStream, attachment.getFileName());
                            targetMessage.reply(messageContent).addFiles(fileUpload).queue(
                                success -> interaction.getHook().editOriginal("✅ Message sent with attachment!").queue(),
                                error -> interaction.getHook().editOriginal("❌ Failed to send message with attachment.").queue()
                            );
                        } catch (IOException e) {
                            interaction.getHook().editOriginal("❌ Failed to download attachment.").queue();
                        }
                    } else {
                        targetMessage.reply(messageContent).queue(
                            success -> interaction.getHook().editOriginal("✅ Reply sent!").queue(),
                            error -> interaction.getHook().editOriginal("❌ Failed to send reply.").queue()
                        );
                    }
                }, error -> interaction.getHook().editOriginal("❌ Target message not found.").queue());
            } else {
                // Send new message
                if (attachment != null) {
                    try (InputStream inputStream = new URL(attachment.getUrl()).openStream()) {
                        FileUpload fileUpload = FileUpload.fromData(inputStream, attachment.getFileName());
                        interaction.getChannel().sendMessage(messageContent).addFiles(fileUpload).queue(
                            success -> interaction.getHook().editOriginal("✅ Message sent with attachment!").queue(),
                            error -> interaction.getHook().editOriginal("❌ Failed to send message with attachment.").queue()
                        );
                    } catch (IOException e) {
                        interaction.getHook().editOriginal("❌ Failed to download attachment.").queue();
                    }
                } else {
                    interaction.getChannel().sendMessage(messageContent).queue(
                        success -> interaction.getHook().editOriginal("✅ Message sent!").queue(),
                        error -> interaction.getHook().editOriginal("❌ Failed to send message.").queue()
                    );
                }
            }

        } catch (Exception error) {
            System.err.println("Say command error: " + error.getMessage());
            if (interaction.isAcknowledged()) {
                interaction.getHook().editOriginal("❌ Failed to send message.").queue();
            } else {
                interaction.reply("❌ Failed to send message.").setEphemeral(true).queue();
            }
        }
    }
}
