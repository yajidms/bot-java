package commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import handlers.AiChatState;

/**
 * End chat command - ends the current AI chat session.
 * Matches the functionality of endchat.js exactly.
 */
public class Endchat {

    public static SlashCommandData getCommandData() {
        return Commands.slash("endchat", "End your current Gemini AI chat session in this thread.");
    }

    public static void execute(SlashCommandInteractionEvent interaction) {
        try {
            String channelId = interaction.getChannel().getId();
            String userId = interaction.getUser().getId();

            // Check if there's an active chat session - matches JS activeAIChats.get(channelId)
            var chatData = AiChatState.getActiveChat(channelId);

            // Validate session exists and belongs to this user
            // Matches JS: if (!chatData || chatData.userId !== interaction.user.id)
            if (chatData == null || !chatData.getUserId().equals(userId)) {
                interaction.reply("You do not have an active AI chat session in this thread.")
                        .setEphemeral(true)
                        .queue();
                return;
            }

            // End the chat session - matches JS activeAIChats.delete(channelId)
            AiChatState.endChat(channelId);

            // Send confirmation - matches JS response
            interaction.reply("Your AI chat session has ended. This thread will remain open for further discussion.")
                    .setEphemeral(true)
                    .queue();

        } catch (Exception error) {
            System.err.println("Endchat command error: " + error.getMessage());
            error.printStackTrace();
            interaction.reply("❌ Failed to end AI chat session")
                    .setEphemeral(true)
                    .queue();
        }
    }
}

