package utils;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.Command;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.List;

public class DeleteCommands {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure().load();
        String token = dotenv.get("DISCORD_TOKEN");

        try {
            System.out.println("Menghapus semua Slash Command...");

            JDA jda = JDABuilder.createDefault(token).build();
            jda.awaitReady();

            List<Command> commands = jda.retrieveCommands().complete();

            if (commands.isEmpty()) {
                System.out.println("Tidak ada command yang perlu dihapus.");
                jda.shutdown();
                return;
            }

            System.out.println("Menghapus " + commands.size() + " command(s)...");

            for (Command command : commands) {
                command.delete().queue(
                    success -> System.out.println("Berhasil menghapus command: " + command.getName()),
                    error -> System.err.println("Gagal menghapus command " + command.getName() + ": " + error.getMessage())
                );
            }

            // Wait a bit for all deletions to complete
            Thread.sleep(2000);

            System.out.println("Semua Slash Command berhasil dihapus.");
            jda.shutdown();

        } catch (Exception error) {
            System.err.println("Error menghapus Slash Command: " + error.getMessage());
        }
    }
}
