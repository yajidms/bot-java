package events;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import handlers.AiChatState;
import handlers.AiHandler;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.List;

/**
 * Event handler for message creation.
 */
public class MessageCreate extends ListenerAdapter {

    private static final Dotenv dotenv = Dotenv.configure().load();

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Skip bot messages and non-guild messages
        if (event.getAuthor().isBot() || !event.isFromGuild()) return;

        try {
            String channelId = event.getChannel().getId();
            String userId = event.getAuthor().getId();
            String messageContent = event.getMessage().getContentDisplay();

            // Check for active AI chat session
            if (AiChatState.hasActiveChat(channelId)) {
                var chatData = AiChatState.getActiveChat(channelId);

                // Only respond to the user who started the chat
                if (!userId.equals(chatData.getUserId())) return;

                // Build combined content for AI (including attachments)
                StringBuilder combinedContentForAI = new StringBuilder(messageContent);
                StringBuilder fileInfoText = new StringBuilder();

                // Read attachments in this message
                if (!event.getMessage().getAttachments().isEmpty()) {
                    // Send typing indicator
                    event.getChannel().sendTyping().queue();

                    StringBuilder fileContents = new StringBuilder();
                    StringBuilder fileNames = new StringBuilder();

                    // Show reading message
                    var readingMsg = event.getMessage().reply(
                            "Analyzing " + event.getMessage().getAttachments().size() + " file(s)..."
                    ).complete();

                    for (var attachment : event.getMessage().getAttachments()) {
                        if (!fileNames.isEmpty()) fileNames.append(", ");
                        fileNames.append("`").append(attachment.getFileName()).append("`");

                        try {
                            // Use AiHandler's readAttachment method
                            String content = AiHandler.readAttachment(attachment);
                            if (!content.startsWith("[Failed to read file")) {
                                fileContents.append("\n\n--- File: ")
                                        .append(attachment.getFileName())
                                        .append(" ---\n")
                                        .append(content)
                                        .append("\n--- End File ---");
                            } else {
                                fileContents.append("\n\n[Notify: Failed to read ")
                                        .append(attachment.getFileName())
                                        .append("]");
                                event.getChannel().sendMessage("⚠️ Failed to read file " + attachment.getFileName() + ".")
                                        .queue();
                            }
                        } catch (Exception readError) {
                            fileContents.append("\n\n[Error reading file: ")
                                    .append(attachment.getFileName())
                                    .append("]");
                            event.getChannel().sendMessage("⚠️ Error reading file " + attachment.getFileName() + ".")
                                    .queue();
                        }
                    }

                    // Delete reading message
                    readingMsg.delete().queue(null, t -> {});

                    if (!fileContents.isEmpty()) {
                        combinedContentForAI.append(fileContents);
                        fileInfoText.append(" (inc ").append(fileNames).append(")");
                    }
                }

                // Skip empty messages
                if (combinedContentForAI.toString().trim().isEmpty()) return;

                // Send typing indicator
                event.getChannel().sendTyping().queue();

                String modelName = chatData.getModelName();
                System.out.println("[AI Session Msg] Send to " + modelName + fileInfoText +
                        ". Len: " + combinedContentForAI.length());

                // Generate AI response using the Gemini API
                try {
                    String aiResponse = callGeminiForSession(combinedContentForAI.toString());

                    System.out.println("[AI Session Msg] Received from " + modelName +
                            ". Len: " + (aiResponse != null ? aiResponse.length() : 0));

                    if (aiResponse == null || aiResponse.trim().isEmpty()) {
                        return;
                    }

                    // Split and send response
                    List<String> chunks = AiHandler.splitMessage(aiResponse, 1990);
                    var replyTo = event.getMessage();

                    for (int i = 0; i < chunks.size(); i++) {
                        final int index = i;
                        final var currentReplyTo = replyTo;

                        replyTo.reply(chunks.get(index)).queue(sentMsg -> {
                            // Update replyTo for next iteration (handled through completion)
                        }, error -> {
                            System.err.println("[AI Session Msg] Failed to send chunk " + (index + 1) +
                                    ": " + error.getMessage());
                        });

                        // Add delay between messages
                        if (i < chunks.size() - 1) {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }

                } catch (Exception e) {
                    System.err.println("[AI Session Msg] Error: " + e.getMessage());
                    event.getMessage().reply("⚠️ Failed to generate response: " + e.getMessage()).queue();
                }
            }

        } catch (Exception error) {
            System.err.println("[MessageCreate] Error: " + error.getMessage());
            error.printStackTrace();
        }
    }

    /**
     * Calls Gemini API for chat session responses
     */
    private String callGeminiForSession(String prompt) {
        try {
            String apiKey = AiChatState.getActiveGeminiKey();
            String modelId = AiChatState.getGeminiInternalId();
            String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + modelId +
                    ":generateContent?key=" + apiKey;

            var objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var requestBody = objectMapper.createObjectNode();
            var contents = requestBody.putArray("contents");
            var content = contents.addObject();
            var parts = content.putArray("parts");
            var part = parts.addObject();
            part.put("text", prompt);

            try (var client = org.apache.hc.client5.http.impl.classic.HttpClients.createDefault()) {
                var request = new org.apache.hc.client5.http.classic.methods.HttpPost(apiUrl);
                request.setHeader("Content-Type", "application/json");
                request.setEntity(new org.apache.hc.core5.http.io.entity.StringEntity(
                        objectMapper.writeValueAsString(requestBody),
                        org.apache.hc.core5.http.ContentType.APPLICATION_JSON
                ));

                try (var response = client.execute(request)) {
                    String jsonResponse = org.apache.hc.core5.http.io.entity.EntityUtils.toString(response.getEntity());
                    var responseJson = objectMapper.readTree(jsonResponse);

                    if (responseJson.has("error")) {
                        throw new Exception(responseJson.get("error").get("message").asText());
                    }

                    var candidates = responseJson.get("candidates");
                    if (candidates != null && candidates.isArray() && !candidates.isEmpty()) {
                        var firstCandidate = candidates.get(0);
                        var contentNode = firstCandidate.get("content");
                        if (contentNode != null) {
                            var partsNode = contentNode.get("parts");
                            if (partsNode != null && partsNode.isArray() && !partsNode.isEmpty()) {
                                return partsNode.get(0).get("text").asText();
                            }
                        }
                    }

                    throw new Exception("Invalid response structure from Gemini API");
                }
            }
        } catch (Exception e) {
            System.err.println("Error calling Gemini API: " + e.getMessage());
            return null;
        }
    }
}

