package utils;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import io.github.cdimascio.dotenv.Dotenv;
import commands.*;

import java.util.ArrayList;
import java.util.List;

public class RegisterCommands {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure().load();
        String token = dotenv.get("DISCORD_TOKEN");

        try {
            System.out.println("Loading commands...");

            List<CommandData> commands = new ArrayList<>();

            // Register all commands
            commands.add(Ping.getCommandData());
            commands.add(Help.getCommandData());
            commands.add(Info.getCommandData());
            commands.add(Avatar.getCommandData());
            commands.add(Banner.getCommandData());
            commands.add(Userinfo.getCommandData());
            commands.add(Serverinfo.getCommandData());
            commands.add(Ban.getCommandData());
            commands.add(Kick.getCommandData());
            commands.add(Clean.getCommandData());
            commands.add(Adzan.getCommandData());
            commands.add(Aichat.getCommandData());
            commands.add(Endchat.getCommandData());
            commands.add(Downloader.getCommandData());
            commands.add(ListRoles.getCommandData());
            commands.add(Mute.getCommandData());
            commands.add(Unmute.getCommandData());
            commands.add(Say.getCommandData());
            commands.add(Timeout.getCommandData());
            commands.add(Untimeout.getCommandData());
            commands.add(Unban.getCommandData());
            commands.add(Retstart.getCommandData());
            commands.add(Setstatus.getCommandData());
            commands.add(Serverstats.getCommandData());
            commands.add(Toggleembed.getCommandData());

            // Print command names
            commands.forEach(cmd -> System.out.println(cmd.getName()));

            System.out.println("\nRegistering " + commands.size() + " commands...");

            JDA jda = JDABuilder.createDefault(token).build();
            jda.awaitReady();

            jda.updateCommands().addCommands(commands).queue(
                success -> {
                    System.out.println("Commands registered successfully!");
                    jda.shutdown();
                },
                error -> {
                    System.err.println("Error registering commands: " + error.getMessage());
                    jda.shutdown();
                }
            );

        } catch (Exception error) {
            System.err.println("Error: " + error.getMessage());
        }
    }
}
