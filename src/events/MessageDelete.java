package events;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import handlers.LogHandler;
import io.github.cdimascio.dotenv.Dotenv;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

public class MessageDelete extends ListenerAdapter {

    private static final Dotenv dotenv = Dotenv.configure().load();
    private static final List<String> ALLOWED_GUILD_IDS = Arrays.asList(
        dotenv.get("GUILD_ID") != null ? dotenv.get("GUILD_ID").split(",") : new String[0]
    );

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        // Batasi hanya untuk server tertentu
        if (!event.isFromGuild() || !ALLOWED_GUILD_IDS.contains(event.getGuild().getId())) {
            return;
        }

        // Get cached message if available
        Message deletedMessage = event.getChannel().getHistory().getMessageById(event.getMessageId());
        if (deletedMessage == null || deletedMessage.getAuthor().isBot()) {
            return;
        }

        String logChannelId = dotenv.get("LOG_CHANNEL_ID");
        if (logChannelId == null) return;

        try {
            LogHandler.LogDetails logDetails = new LogHandler.LogDetails();
            logDetails.title = "Message Deleted";
            logDetails.description = String.format(
                "**Author:** %s (%s)\n**Channel:** <#%s>\n**Content:** %s",
                deletedMessage.getAuthor().getAsTag(),
                deletedMessage.getAuthor().getId(),
                event.getChannel().getId(),
                deletedMessage.getContentDisplay().isEmpty() ? "*No text content*" : deletedMessage.getContentDisplay()
            );
            logDetails.color = Color.RED;
            logDetails.userId = deletedMessage.getAuthor().getId();
            logDetails.messageId = event.getMessageId();

            // Handle attachments
            if (!deletedMessage.getAttachments().isEmpty()) {
                StringBuilder attachments = new StringBuilder();
                int count = 0;
                for (Message.Attachment attachment : deletedMessage.getAttachments()) {
                    if (count < 5) {
                        attachments.append("[").append(attachment.getFileName()).append("](").append(attachment.getUrl()).append(")\n");
                        count++;
                    }
                }

                if (attachments.length() > 0) {
                    logDetails.description += "\n\n**Attachments:**\n" + attachments.toString();
                }

                if (deletedMessage.getAttachments().size() > 5) {
                    logDetails.description += "\n*And " + (deletedMessage.getAttachments().size() - 5) + " more attachments...*";
                }
            }

            LogHandler.sendLog(event.getJDA(), logChannelId, logDetails);

        } catch (Exception error) {
            System.err.println("Error in messageDelete event: " + error.getMessage());
        }
    }
}
