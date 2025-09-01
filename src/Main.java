import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import handlers.CommandLoader;
import handlers.EventLoader;
import handlers.DownloaderHandler;
import handlers.YtdlHandler;
import handlers.TwitterHandler;
import handlers.AiHandler;
import handlers.QuoteHandler;
import events.InteractionCreate;
import events.Ready;
import events.MessageCreate;
import events.MessageDelete;
import events.MessageUpdate;

import java.util.HashMap;
import java.util.Map;

public class Main extends ListenerAdapter {
    private static JDA jda;
    public static Map<String, Object> commands = new HashMap<>();

    public static void main(String[] args) {
        try {
            Dotenv dotenv = Dotenv.configure().load();
            String token = dotenv.get("DISCORD_TOKEN");

            if (token == null || token.isEmpty()) {
                System.err.println("ERROR: DISCORD_TOKEN not found in environment variables!");
                return;
            }

            jda = JDABuilder.createDefault(token)
                    .enableIntents(
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_MEMBERS
                    )
                    .addEventListeners(
                        new Main(),
                        new InteractionCreate(),
                        new Ready(),
                        new MessageCreate(),
                        new MessageDelete(),
                        new MessageUpdate()
                    )
                    .build();

            // Load events and commands
            EventLoader.loadEvents(jda);
            CommandLoader.loadCommands(jda);

            // Initialize quote scheduler
            QuoteHandler.scheduleQOTD(jda);

            jda.awaitReady();
            System.out.println("Bot siap! Login sebagai " + jda.getSelfUser().getAsTag());

        } catch (Exception e) {
            System.err.println("Error starting bot: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        String content = event.getMessage().getContentDisplay();

        // Handle prefix commands
        if (content.startsWith("f.x")) {
            TwitterHandler.handleX(event);
        } else if (content.startsWith("f.ig")) {
            DownloaderHandler.handleIg(event);
        } else if (content.startsWith("f.fb")) {
            DownloaderHandler.handleFb(event);
        } else if (content.startsWith("f.tt")) {
            DownloaderHandler.handleTt(event);
        } else if (content.startsWith("f.yt")) {
            YtdlHandler.handleYtDownload(event);
        } else if (content.startsWith("f.")) {
            // Handle AI commands
            AiHandler.handleAiChat(event);
        }
    }
}
