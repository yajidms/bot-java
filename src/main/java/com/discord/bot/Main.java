package com.discord.bot;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import com.discord.bot.handlers.CommandLoader;
import com.discord.bot.handlers.EventLoader;
import com.discord.bot.handlers.DownloaderHandler;
import com.discord.bot.handlers.YtdlHandler;
import com.discord.bot.handlers.TwitterHandler;
import com.discord.bot.handlers.AiHandler;
import com.discord.bot.handlers.QuoteHandler;
import com.discord.bot.events.InteractionCreate;
import com.discord.bot.events.Ready;
import com.discord.bot.events.MessageCreate;
import com.discord.bot.events.MessageDelete;
import com.discord.bot.events.MessageUpdate;

import java.util.HashMap;
import java.util.Map;

/**
 * Main entry point for the Discord bot.
 */
public class Main extends ListenerAdapter {
    private static JDA jda;
    public static Map<String, Object> commands = new HashMap<>();

    public static void main(String[] args) {
        try {
            // Load .env file
            Dotenv dotenv = Dotenv.configure().load();
            String token = dotenv.get("DISCORD_TOKEN");

            if (token == null || token.isEmpty()) {
                System.err.println("ERROR: DISCORD_TOKEN not found in environment variables!");
                return;
            }

            // Create JDA instance with intents
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
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        //skip bot messages : if (message.author.bot) return;
        if (event.getAuthor().isBot()) return;

        String content = event.getMessage().getContentDisplay();

        //downloader commands
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
