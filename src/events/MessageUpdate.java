package events;

import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import handlers.LogHandler;
import io.github.cdimascio.dotenv.Dotenv;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

public class MessageUpdate extends ListenerAdapter {

    private static final Dotenv dotenv = Dotenv.configure().load();
    private static final List<String> ALLOWED_GUILD_IDS = Arrays.asList(
        dotenv.get("GUILD_ID") != null ? dotenv.get("GUILD_ID").split(",") : new String[0]
    );

    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        if (!event.isFromGuild() || !ALLOWED_GUILD_IDS.contains(event.getGuild().getId())) {
            return;
        }

        if (event.getAuthor().isBot()) {
            return;
        }

        // Check if content actually changed
        String oldContent = event.getMessage().getContentDisplay();
        if (oldContent == null || oldContent.isEmpty()) {
            return;
        }

        String logChannelId = dotenv.get("LOG_CHANNEL_ID");
        if (logChannelId == null) return;

        try {
            LogHandler.LogDetails logDetails = new LogHandler.LogDetails();
            logDetails.title = String.format("Message edited on https://discord.com/channels/%s/%s/%s",
                event.getGuild().getId(), event.getChannel().getId(), event.getMessageId());
            logDetails.description = String.format("**Before:**\n%s\n\n**After:**\n%s",
                "Content unavailable", // Old content not available in JDA MessageUpdateEvent
                event.getMessage().getContentDisplay());
            logDetails.color = new Color(0xFFCC00);
            logDetails.userId = event.getAuthor().getId();
            logDetails.messageId = event.getMessageId();

            if (logDetails.author == null) {
                logDetails.author = new LogHandler.LogDetails.Author(
                    event.getAuthor().getAsTag(),
                    event.getAuthor().getAvatarUrl()
                );
            }

            LogHandler.sendLog(event.getJDA(), logChannelId, logDetails);

        } catch (Exception error) {
            System.err.println("Error in messageUpdate event: " + error.getMessage());
        }
    }
}
