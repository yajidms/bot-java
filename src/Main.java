import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
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

/**
 * Main entry point for the Discord bot.
 * Matches the structure and functionality of index.js exactly.
 */
public class Main extends ListenerAdapter {
    private static JDA jda;
    public static Map<String, Object> commands = new HashMap<>();

    public static void main(String[] args) {
        try {
            // Load .env file - matches JS: require("dotenv").config()
            Dotenv dotenv = Dotenv.configure().load();
            String token = dotenv.get("DISCORD_TOKEN");

            if (token == null || token.isEmpty()) {
                System.err.println("ERROR: DISCORD_TOKEN not found in environment variables!");
                return;
            }

            // Create JDA instance with intents matching JS client configuration
            jda = JDABuilder.createDefault(token)
                    .enableIntents(
                        GatewayIntent.GUILD_MESSAGES,            // GatewayIntentBits.GuildMessages
                        GatewayIntent.MESSAGE_CONTENT,           // GatewayIntentBits.MessageContent
                        GatewayIntent.GUILD_MEMBERS              // GatewayIntentBits.GuildMembers
                    )
                    .addEventListeners(
                        new Main(),                              // Main message handler
                        new InteractionCreate(),                 // Slash command handler
                        new Ready(),                             // Ready event
                        new MessageCreate(),                     // Message create event for AI sessions
                        new MessageDelete(),                     // Message delete event
                        new MessageUpdate()                      // Message update event
                    )
                    .build();

            // Load events and commands - matches JS loadEvents/loadCommands
            EventLoader.loadEvents(jda);
            CommandLoader.loadCommands(jda);

            // Initialize quote scheduler - matches JS QuoteHandler.scheduleQOTD
            QuoteHandler.scheduleQOTD(jda);

            // Wait for bot to be ready
            jda.awaitReady();
            System.out.println("Bot siap! Login sebagai " + jda.getSelfUser().getAsTag());

            // Add shutdown hook for graceful shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down bot...");
                QuoteHandler.shutdown();
                jda.shutdown();
            }));

        } catch (Exception e) {
            System.err.println("Error starting bot: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handles prefix-based commands from messages.
     * Matches the JS index.js messageCreate handler exactly:
     *
     * client.on("messageCreate", async (message) => {
     *   if (message.author.bot) return;
     *   if (message.content.startsWith("f.x")) handleX(message);
     *   if (message.content.startsWith("f.ig")) handleIg(message);
     *   if (message.content.startsWith("f.fb")) handleFb(message);
     *   if (message.content.startsWith("f.tt")) handleTt(message);
     *   if (message.content.startsWith("f.yt")) handleYtDownload(message);
     * });
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Skip bot messages - matches JS: if (message.author.bot) return;
        if (event.getAuthor().isBot()) return;

        String content = event.getMessage().getContentDisplay();

        // Handle prefix commands in order of specificity
        // Important: Check more specific prefixes first (f.geminipropreview before f.geminipro)

        // Downloader commands - matches JS index.js
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
        }
        // AI commands - handled by AiHandler
        // Note: These are checked in AiHandler.handleAiChat which checks for:
        // f.geminipropreview, f.geminipro, f.geminiflash, f.llama, f.deepseek-r1, f.ai
        else if (content.startsWith("f.geminipropreview") ||
                 content.startsWith("f.geminipro") ||
                 content.startsWith("f.geminiflash") ||
                 content.startsWith("f.llama") ||
                 content.startsWith("f.deepseek-r1") ||
                 content.trim().equals("f.ai")) {
            AiHandler.handleAiChat(event);
        }
    }

    /**
     * Gets the JDA instance
     */
    public static JDA getJda() {
        return jda;
    }
}

