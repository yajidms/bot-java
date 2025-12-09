package com.discord.bot.utils;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.ApplicationInfo;
import net.dv8tion.jda.api.interactions.commands.Command;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.List;

public class Checkbots {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure().load();
        String token = dotenv.get("DISCORD_TOKEN");
        String clientId = dotenv.get("CLIENT_ID");

        System.out.println("🔍 Checking bot status and permissions...\n");

        try {
            JDA jda = JDABuilder.createDefault(token).build();
            jda.awaitReady();

            // 1. Check bot application
            System.out.println("1. Checking application information...");
            ApplicationInfo application = jda.retrieveApplicationInfo().complete();
            System.out.println("Bot Name: " + application.getName());
            System.out.println("Application ID: " + application.getId());
            System.out.println("Owner: " + (application.getOwner() != null ? application.getOwner().getName() : "Team"));

            // 2. Verify CLIENT_ID
            System.out.println("\n2. Verifying CLIENT_ID...");
            if (application.getId().equals(clientId)) {
                System.out.println("✅ CLIENT_ID matches with token");
            } else {
                System.out.println("❌ CLIENT_ID DOES NOT MATCH with token!");
                System.out.println("Token Application ID: " + application.getId());
                System.out.println(".env CLIENT_ID: " + clientId);
                jda.shutdown();
                return;
            }

            // 3. Check existing commands
            System.out.println("\n3. Checking existing slash commands...");
            try {
                List<Command> commands = jda.retrieveCommands().complete();
                System.out.println("Successfully retrieved commands: " + commands.size() + " command(s)");

                if (!commands.isEmpty()) {
                    System.out.println("Commands list:");
                    for (int i = 0; i < commands.size(); i++) {
                        Command cmd = commands.get(i);
                        System.out.println((i + 1) + ". " + cmd.getName() + " (ID: " + cmd.getId() + ")");
                    }
                } else {
                    System.out.println("No commands registered");
                }
            } catch (Exception cmdError) {
                System.out.println("Failed to retrieve commands:");
                System.out.println("Error: " + cmdError.getMessage());
            }

            // 4. Check bot permissions in guilds
            System.out.println("\n4. Checking guild memberships...");
            System.out.println("Bot is in " + jda.getGuilds().size() + " guild(s)");

            jda.getGuilds().forEach(guild -> {
                System.out.println("- " + guild.getName() + " (ID: " + guild.getId() + ")");
                System.out.println("  Members: " + guild.getMemberCount());
                System.out.println("  Bot permissions: " + guild.getSelfMember().getPermissions().toString());
            });

            System.out.println("\n✅ Bot status check completed!");
            jda.shutdown();

        } catch (Exception error) {
            System.err.println("Error checking bot status: " + error.getMessage());
            error.printStackTrace();
        }
    }
}
