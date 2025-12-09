package commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import handlers.AiChatState;
import handlers.AiHandler;
import io.github.cdimascio.dotenv.Dotenv;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.awt.Color;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * AI Chat command - starts a chat session with Gemini AI.
 */
public class Aichat {

    private static final Dotenv dotenv = Dotenv.configure().load();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static SlashCommandData getCommandData() {
        return Commands.slash("aichat", "Start a chat session with Gemini AI in this forum thread.")
                .addOption(OptionType.STRING, "initial_prompt", "First message or topic (optional).", false)
                .addOption(OptionType.ATTACHMENT, "file", "Upload a file (text, pdf, docx, etc) for the AI.", false);
    }

    public static void execute(SlashCommandInteractionEvent interaction) {
        CompletableFuture.runAsync(() -> handleAIChatBegin(interaction));
    }

    private static void handleAIChatBegin(SlashCommandInteractionEvent interaction) {
        var user = interaction.getUser();
        var channel = interaction.getChannel();
        String initialPrompt = interaction.getOption("initial_prompt") != null ?
                interaction.getOption("initial_prompt").getAsString() : "";
        Message.Attachment attachment = interaction.getOption("file") != null ?
                interaction.getOption("file").getAsAttachment() : null;
        String modelDisplayName = AiChatState.getGeminiModelName();

        try {
            // Only allow in forum threads
            if (interaction.getChannelType() != ChannelType.GUILD_PUBLIC_THREAD &&
                interaction.getChannelType() != ChannelType.GUILD_PRIVATE_THREAD) {
                interaction.reply("This command can only be used inside a forum post (thread).")
                        .setEphemeral(true).queue();
                return;
            }

            // Prevent multiple sessions in the same thread
            if (AiChatState.hasActiveChat(channel.getId())) {
                interaction.reply("There is already an active AI chat session in this thread.")
                        .setEphemeral(true).queue();
                return;
            }

            // Defer reply
            interaction.deferReply(true).queue();

            String fileContent = "";
            String fileReadingMessage = "";

            // Process attachment if provided
            if (attachment != null) {
                interaction.getHook().editOriginal("Reading file `" + attachment.getFileName() + "`...").queue();
                try {
                    fileContent = AiHandler.readAttachment(attachment);
                    fileReadingMessage = "\nFile `" + attachment.getFileName() + "` has been included.";
                } catch (Exception readError) {
                    fileReadingMessage = "\n⚠️ Failed to read file `" + attachment.getFileName() +
                            "`. Chat started without file.";
                    fileContent = "";
                }
            }

            // Build full initial prompt
            String fullInitialPrompt = initialPrompt;
            if (!fileContent.isEmpty() && attachment != null) {
                fullInitialPrompt += "\n\n--- File Content: " + attachment.getFileName() + " ---\n" +
                        fileContent + "\n--- End of File ---";
            }

            // Start the chat session
            AiChatState.startChat(channel.getId(), user.getId(), modelDisplayName);

            // Build welcome embed
            String attachmentNote = "";
            if (attachment != null) {
                attachmentNote = "\n\nFile `" + attachment.getFileName() + "`" +
                        (fileReadingMessage.contains("included") ? " has been included" : " failed to read and was not included") +
                        ".";
            }

            var welcomeEmbed = new EmbedBuilder()
                    .setColor(new Color(0x4285F4))
                    .setTitle("🤖 Gemini Chat Session Started")
                    .setDescription("Hello " + user.getAsMention() + "! You are now connected to " + modelDisplayName + "." +
                            attachmentNote +
                            "\n\nStart your conversation. Use `/endchat` to finish.")
                    .setFooter("Powered by Google Gemini (" + modelDisplayName + ")")
                    .setTimestamp(Instant.now())
                    .build();

            // Send initial prompt if any and get response
            String initialResponseText = null;
            if (!fullInitialPrompt.trim().isEmpty()) {
                try {
                    channel.sendTyping().queue();
                    initialResponseText = callGeminiApi(fullInitialPrompt);
                } catch (Exception error) {
                    initialResponseText = "*Error processing initial prompt: " + error.getMessage() + "*";
                }
            }

            // Send welcome embed
            channel.sendMessageEmbeds(welcomeEmbed).queue();

            // Send initial response if any
            if (initialResponseText != null && !initialResponseText.isEmpty()) {
                List<String> chunks = AiHandler.splitMessage(initialResponseText, 1990);
                for (String chunk : chunks) {
                    channel.sendMessage(chunk).queue();
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            // Edit the deferred reply
            interaction.getHook().editOriginal("Gemini chat session is ready in this thread." + fileReadingMessage).queue();

        } catch (Exception error) {
            System.err.println("Fatal error starting " + modelDisplayName + " session: " + error.getMessage());
            error.printStackTrace();

            String errorMsg = "Fatal error starting " + modelDisplayName + " session.";
            try {
                if (interaction.isAcknowledged()) {
                    interaction.getHook().editOriginal(errorMsg).queue();
                } else {
                    interaction.reply(errorMsg).setEphemeral(true).queue();
                }
            } catch (Exception e) {
                System.err.println("Failed to send error message: " + e.getMessage());
            }
        }
    }

    /**
     * Calls the Gemini API with the given prompt
     */
    private static String callGeminiApi(String prompt) throws Exception {
        String apiKey = AiChatState.getActiveGeminiKey();
        String modelId = AiChatState.getGeminiInternalId();
        String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + modelId +
                ":generateContent?key=" + apiKey;

        var requestBody = objectMapper.createObjectNode();
        var contents = requestBody.putArray("contents");
        var content = contents.addObject();
        var parts = content.putArray("parts");
        var part = parts.addObject();
        part.put("text", prompt);

        try (var client = HttpClients.createDefault()) {
            var request = new HttpPost(apiUrl);
            request.setHeader("Content-Type", "application/json");
            request.setEntity(new StringEntity(
                    objectMapper.writeValueAsString(requestBody),
                    ContentType.APPLICATION_JSON
            ));

            try (var response = client.execute(request)) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
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
    }
}

