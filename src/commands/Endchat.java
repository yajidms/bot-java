package commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import handlers.AiChatState;

public class Endchat {

    public static SlashCommandData getCommandData() {
        return Commands.slash("endchat", "End the current AI chat session");
    }

    public static void execute(SlashCommandInteractionEvent interaction) {
        try {
            String userId = interaction.getUser().getId();
            String channelId = interaction.getChannel().getId();

            AiChatState.ChatSession session = AiChatState.getOrCreateChatSession(userId, channelId);

            if (session.getConversationHistory().isEmpty()) {
                interaction.reply("❌ No active AI chat session found in this channel.").setEphemeral(true).queue();
                return;
            }

            AiChatState.removeChatSession(userId, channelId);
            interaction.reply("✅ AI chat session ended successfully!").queue();

        } catch (Exception error) {
            System.err.println("Endchat command error: " + error.getMessage());
            interaction.reply("❌ Failed to end AI chat session").setEphemeral(true).queue();
        }
    }
}
