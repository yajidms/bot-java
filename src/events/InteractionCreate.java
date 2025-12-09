package events;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import commands.*;

public class InteractionCreate extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String commandName = event.getName();

        try {
            switch (commandName) {
                case "ping":
                    Ping.execute(event);
                    break;
                case "help":
                    Help.execute(event);
                    break;
                case "info":
                    Info.execute(event);
                    break;
                case "avatar":
                    Avatar.execute(event);
                    break;
                case "banner":
                    Banner.execute(event);
                    break;
                case "userinfo":
                    Userinfo.execute(event);
                    break;
                case "serverinfo":
                    Serverinfo.execute(event);
                    break;
                case "ban":
                    Ban.execute(event);
                    break;
                case "kick":
                    Kick.execute(event);
                    break;
                case "clean":
                    Clean.execute(event);
                    break;
                case "adzan":
                    Adzan.execute(event);
                    break;
                case "aichat":
                    Aichat.execute(event);
                    break;
                case "endchat":
                    Endchat.execute(event);
                    break;
                case "downloader":
                    Downloader.execute(event);
                    break;
                case "list-roles":
                    ListRoles.execute(event);
                    break;
                case "mute":
                    Mute.execute(event);
                    break;
                case "unmute":
                    Unmute.execute(event);
                    break;
                case "timeout":
                    Timeout.execute(event);
                    break;
                case "untimeout":
                    Untimeout.execute(event);
                    break;
                case "unban":
                    Unban.execute(event);
                    break;
                case "say":
                    Say.execute(event);
                    break;
                case "restart":
                    Retstart.execute(event);
                    break;
                case "set":
                    Setstatus.execute(event);
                    break;
                case "serverstats":
                    Serverstats.execute(event);
                    break;
                case "toggleembed":
                    Toggleembed.execute(event);
                    break;
                default:
                    System.err.println("Command " + commandName + " tidak ditemukan!");
                    return;
            }
        } catch (Exception error) {
            System.err.println("Error saat mengeksekusi command " + commandName + ": " + error.getMessage());

            if (event.isAcknowledged()) {
                event.getHook().editOriginal("Terjadi kesalahan saat menjalankan perintah!").queue();
            } else {
                event.reply("Terjadi kesalahan saat menjalankan perintah!").setEphemeral(true).queue();
            }
        }
    }
}
