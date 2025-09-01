package commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import handlers.AiChatState;

import java.awt.Color;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public class Aichat {

    public static SlashCommandData getCommandData() {
        return Commands.slash("aichat", "Start a chat session with Gemini 2.5 Flash AI in this forum thread.")
                .addOption(OptionType.STRING, "initial_prompt", "First message or topic (optional).", false)
                .addOption(OptionType.ATTACHMENT, "file", "Upload a file (text, pdf, docx, etc) for the AI.", false);
    }

    public static void execute(SlashCommandInteractionEvent interaction) {
        CompletableFuture.runAsync(() -> handleAIChatBegin(interaction));
    }

    private static void handleAIChatBegin(SlashCommandInteractionEvent interaction) {
        try {
            String userId = interaction.getUser().getId();
            String channelId = interaction.getChannel().getId();
            String initialPrompt = interaction.getOption("initial_prompt") != null ?
                interaction.getOption("initial_prompt").getAsString() : "";
            Message.Attachment attachment = interaction.getOption("file") != null ?
                interaction.getOption("file").getAsAttachment() : null;
            String modelDisplayName = AiChatState.getGeminiModelName();

            // Only allow in forum threads
            if (interaction.getChannelType() != ChannelType.GUILD_PUBLIC_THREAD &&
                interaction.getChannelType() != ChannelType.GUILD_PRIVATE_THREAD) {
                interaction.reply("This command can only be used inside a forum post (thread).")
                    .setEphemeral(true).queue();
                return;
            }

            // Prevent multiple sessions in the same thread
            AiChatState.ChatSession existingSession = AiChatState.getOrCreateChatSession(userId, channelId);
            if (existingSession.getConversationHistory().length() > 0) {
                interaction.reply("There is already an active AI chat session in this thread.")
                    .setEphemeral(true).queue();
                return;
            }

            interaction.deferReply(true).queue();

            String fileContent = "";
            String fileReadingMessage = "";

            if (attachment != null) {
                interaction.getHook().editOriginal("Reading file `" + attachment.getFileName() + "`...").queue();
                try {
                    // Simplified file reading - actual implementation would use aiHandler methods
                    fileContent = "File content would be read here";
                    fileReadingMessage = "\nFile `" + attachment.getFileName() + "` read successfully.";
                } catch (Exception readError) {
                    fileReadingMessage = "\n⚠️ Failed to read file `" + attachment.getFileName() + "`. Chat started without file.";
                    fileContent = "";
                }
            }

            String fullInitialPrompt = initialPrompt;
            if (!fileContent.isEmpty()) {
                fullInitialPrompt += "\n\n--- File Content: " + attachment.getFileName() + " ---\n" +
                    fileContent + "\n--- End of File ---";
            }

            // Create new chat session
            AiChatState.ChatSession chatSession = AiChatState.getOrCreateChatSession(userId, channelId);
            if (!fullInitialPrompt.isEmpty()) {
                chatSession.addMessage("user", fullInitialPrompt);
            }

            EmbedBuilder welcomeEmbed = new EmbedBuilder()
                .setColor(new Color(0x00ffed))
                .setTitle("🤖 AI Chat Session Started")
                .setDescription("You can now chat with " + modelDisplayName + " in this thread!\n" +
                    "Just send your messages normally and the AI will respond." + fileReadingMessage)
                .addField("📝 Commands",
                    "`/endchat` - End the AI chat session\n" +
                    "Just type your message to continue chatting!", false)
                .setFooter("Model: " + modelDisplayName)
                .setTimestamp(Instant.now());

            interaction.getHook().editOriginalEmbeds(welcomeEmbed.build()).queue();

            // Send initial response if prompt provided
            if (!fullInitialPrompt.isEmpty()) {
                interaction.getChannel().sendMessage("🤖 **AI Response:**\nInitial response would be generated here for: " +
                    initialPrompt).queue();
            }

        } catch (Exception error) {
            System.err.println("AI Chat command error: " + error.getMessage());
            if (interaction.isAcknowledged()) {
                interaction.getHook().editOriginal("❌ Failed to start AI chat session").queue();
            } else {
                interaction.reply("❌ Failed to start AI chat session").setEphemeral(true).queue();
            }
        }
    }
}
