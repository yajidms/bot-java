package commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class Clean {

    public static SlashCommandData getCommandData() {
        return Commands.slash("clean", "Delete multiple messages")
                .addOption(OptionType.INTEGER, "amount", "Number of messages to delete (1-100)", true)
                .setDefaultPermissions(Permission.MANAGE_MESSAGES.getRawValue())
                .setGuildOnly(true);
    }

    public static void execute(SlashCommandInteractionEvent interaction) {
        if (!interaction.getMember().hasPermission(Permission.MANAGE_MESSAGES)) {
            interaction.reply("❌ You don't have permission to use this command!").setEphemeral(true).queue();
            return;
        }

        int amount = interaction.getOption("amount").getAsInt();

        if (amount < 1 || amount > 100) {
            interaction.reply("❌ Amount must be between 1 and 100.").setEphemeral(true).queue();
            return;
        }

        interaction.deferReply(true).queue();

        interaction.getChannel().getHistory().retrievePast(amount).queue(messages -> {
            if (messages.isEmpty()) {
                interaction.getHook().editOriginal("❌ No messages found to delete.").queue();
                return;
            }

            try {
                if (messages.size() == 1) {
                    messages.get(0).delete().queue(
                        success -> interaction.getHook().editOriginal("✅ Successfully deleted 1 message.").queue(),
                        error -> interaction.getHook().editOriginal("❌ Failed to delete message.").queue()
                    );
                } else {
                    interaction.getGuild().getTextChannelById(interaction.getChannel().getId())
                        .deleteMessages(messages).queue(
                        success -> interaction.getHook().editOriginal("✅ Successfully deleted " + messages.size() + " messages.").queue(),
                        error -> interaction.getHook().editOriginal("❌ Failed to delete messages. Messages might be too old.").queue()
                    );
                }
            } catch (Exception error) {
                System.err.println("Clean command error: " + error.getMessage());
                interaction.getHook().editOriginal("❌ Failed to delete messages.").queue();
            }
        }, error -> {
            interaction.getHook().editOriginal("❌ Failed to retrieve messages.").queue();
        });
    }
}
