package handlers;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import commands.*;

import java.util.ArrayList;
import java.util.List;

public class CommandLoader {

    public static void loadCommands(JDA client) {
        try {
            List<CommandData> commands = new ArrayList<>();

            // Load all commands
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

            // Register commands with Discord
            client.updateCommands().addCommands(commands).queue();

            System.out.println("Commands berhasil dimuat dan didaftarkan. Total: " + commands.size() + " commands");

        } catch (Exception e) {
            System.err.println("Error loading commands: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
