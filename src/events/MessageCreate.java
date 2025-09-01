package events;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import handlers.AiChatState;
import handlers.AiHandler;

public class MessageCreate extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || !event.isFromGuild()) return;

        try {
            // Handle AI chat sessions
            String channelId = event.getChannel().getId();
            String userId = event.getAuthor().getId();

            AiChatState.ChatSession chatSession = AiChatState.getOrCreateChatSession(userId, channelId);

            if (chatSession.getConversationHistory().length() > 0) {
                // Active AI chat session
                String messageContent = event.getMessage().getContentDisplay();

                if (!messageContent.isEmpty()) {
                    chatSession.addMessage("user", messageContent);

                    // Send typing indicator
                    event.getChannel().sendTyping().queue();

                    // Generate AI response (simplified)
                    String aiResponse = "AI response would be generated here for: " + messageContent;
                    chatSession.addMessage("assistant", aiResponse);

                    event.getChannel().sendMessage("🤖 " + aiResponse).queue();
                }
            }

            // Handle prefix-based AI commands
            AiHandler.handleAiChat(event);

        } catch (Exception error) {
            System.err.println("[MessageCreate] Error: " + error.getMessage());
        }
    }
}
