package handlers;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import io.github.cdimascio.dotenv.Dotenv;

import java.awt.Color;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AiHandler {

  private static boolean aiStatus = true;
  private static final Dotenv dotenv = Dotenv.configure().load();
  private static final String GEMINI_API_KEY = dotenv.get("GEMINI_API_KEY");
  private static final String TOGETHER_API_KEY = dotenv.get("TOGETHER_API_KEY");

  private static final List<String> CODE_EXTENSIONS = Arrays.asList(
          ".txt", ".js", ".ts", ".jsx", ".tsx", ".py", ".java", ".c", ".cpp", ".cs",
          ".rb", ".go", ".php", ".swift", ".kt", ".kts", ".rs", ".scala", ".sh",
          ".bat", ".pl", ".lua", ".r", ".m", ".vb", ".dart", ".html", ".css", ".scss",
          ".less", ".json", ".xml", ".yml", ".yaml", ".md", ".ini", ".cfg", ".toml",
          ".sql", ".asm", ".s", ".h", ".hpp", ".vue", ".coffee", ".erl", ".ex", ".exs",
          ".fs", ".fsx", ".groovy", ".jl", ".lisp", ".clj", ".cljs", ".ml", ".mli",
          ".nim", ".ps1", ".psm1", ".psd1", ".rkt", ".vb", ".vbs", ".v", ".sv", ".svelte", ".jar"
  );

  private static final List<String> IMAGE_EXTENSIONS = Arrays.asList(
          ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".svg"
  );

  private static String formatThinkBlockquote(String text) {
    return text.replaceAll("<think>([\\s\\S]*?)</think>", (match) -> {
      String content = match.replaceAll("</?think>", "");
      return Arrays.stream(content.split("\n"))
              .map(line -> line.trim().isEmpty() ? "" : "-# " + line)
              .reduce("", (a, b) -> a + "\n" + b);
    });
  }

  private static String readAttachment(Message.Attachment attachment) {
    try {
      String url = attachment.getUrl();
      String name = attachment.getFileName().toLowerCase();

      Path tempDir = Paths.get("temp");
      if (!Files.exists(tempDir)) {
        Files.createDirectories(tempDir);
      }

      Path tempPath = tempDir.resolve(System.currentTimeMillis() + "_" + name);

      try (CloseableHttpClient client = HttpClients.createDefault()) {
        HttpGet request = new HttpGet(url);
        try (ClassicHttpResponse response = client.execute(request);
             InputStream inputStream = response.getEntity().getContent();
             FileOutputStream outputStream = new FileOutputStream(tempPath.toFile())) {

          inputStream.transferTo(outputStream);
        }
      }

      String text = "";

      if (CODE_EXTENSIONS.stream().anyMatch(name::endsWith)) {
        text = Files.readString(tempPath, StandardCharsets.UTF_8);
      } else if (IMAGE_EXTENSIONS.stream().anyMatch(name::endsWith)) {
        File file = tempPath.toFile();
        text = String.format("[Image file: %s, size: %d bytes]\n", name, file.length());

        if (!name.endsWith(".svg") && !name.endsWith(".gif")) {
          text += "\n[OCR Result Start]\n";
          try {
            Tesseract tesseract = new Tesseract();
            String ocrResult = tesseract.doOCR(file);
            text += ocrResult.trim().isEmpty() ? "(No text detected)" : ocrResult.trim();
          } catch (TesseractException e) {
            text += "(OCR failed: " + e.getMessage() + ")";
          }
          text += "\n[OCR Result End]";
        }
      } else if (name.endsWith(".pdf")) {
        try (PDDocument document = PDDocument.load(tempPath.toFile())) {
          PDFTextStripper stripper = new PDFTextStripper();
          text = stripper.getText(document);
        }
      } else {
        text = "[Unsupported file type]";
      }

      // Clean up temp file
      Files.deleteIfExists(tempPath);

      return text;

    } catch (Exception e) {
      return "[Failed to read attachment: " + e.getMessage() + "]";
    }
  }

  public static void handleAiChat(MessageReceivedEvent event) {
    String content = event.getMessage().getContentDisplay();

    if (!aiStatus && !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
      event.getChannel().sendMessage("AI feature is currently disabled. Contact admin to enable this feature.").queue();
      return;
    }

    if (content.trim().equals("f.ai")) {
      EmbedBuilder embed = new EmbedBuilder()
              .setTitle("How to Use AI Chat")
              .setDescription(
                      "**Use the following command to ask the AI:**\n\n" +
                              "**Gemini Pro:**\n" +
                              "`f.geminipro [your question]`\n" +
                              "**Gemini Flash:**\n" +
                              "`f.geminiflash [your question]`\n" +
                              "**Llama AI:**\n" +
                              "`f.llama [your question]`\n" +
                              "**DeepSeek R1:**\n" +
                              "`f.deepseek-r1 [your question]`\n" +
                              "_You can also attach files (documents and images) to include their content in your question!_"
              )
              .setColor(Color.BLUE);

      event.getChannel().sendMessageEmbeds(embed.build()).queue();
      return;
    }

    CompletableFuture.runAsync(() -> {
      try {
        StringBuilder fileContent = new StringBuilder();

        if (!event.getMessage().getAttachments().isEmpty()) {
          for (Message.Attachment attachment : event.getMessage().getAttachments()) {
            String attachmentContent = readAttachment(attachment);
            fileContent.append(String.format("--- File: %s ---\n%s\n--- End of %s ---\n\n",
                    attachment.getFileName(), attachmentContent, attachment.getFileName()));
          }
        }

        String prompt = content;
        if (fileContent.length() > 0) {
          prompt += "\n\n[File Content Start]\n" + fileContent.toString() + "[File Content End]";
        }

        if (content.startsWith("f.geminipro")) {
          handleGeminiResponse(event, "f.geminipro", prompt, "gemini-pro");
        } else if (content.startsWith("f.geminiflash")) {
          handleGeminiResponse(event, "f.geminiflash", prompt, "gemini-1.5-flash");
        } else if (content.startsWith("f.llama")) {
          handleLlamaResponse(event, "f.llama", prompt);
        } else if (content.startsWith("f.deepseek-r1")) {
          handleDeepSeekResponse(event, "f.deepseek-r1", prompt);
        }

      } catch (Exception e) {
        event.getChannel().sendMessage("An error occurred while processing your request: " + e.getMessage()).queue();
      }
    });
  }

  private static void handleGeminiResponse(MessageReceivedEvent event, String prefix, String prompt, String model) {
    // Simplified Gemini API call - actual implementation would use Google's Java client
    String userPrompt = prompt.substring(prefix.length()).trim();
    if (userPrompt.isEmpty()) {
      event.getChannel().sendMessage("Please provide a question after the command.").queue();
      return;
    }

    // Placeholder for actual Gemini API integration
    event.getChannel().sendMessage("Gemini response would be processed here for: " + userPrompt).queue();
  }

  private static void handleLlamaResponse(MessageReceivedEvent event, String prefix, String prompt) {
    String userPrompt = prompt.substring(prefix.length()).trim();
    if (userPrompt.isEmpty()) {
      event.getChannel().sendMessage("Please provide a question after the command.").queue();
      return;
    }

    // Placeholder for actual Llama API integration
    event.getChannel().sendMessage("Llama response would be processed here for: " + userPrompt).queue();
  }

  private static void handleDeepSeekResponse(MessageReceivedEvent event, String prefix, String prompt) {
    String userPrompt = prompt.substring(prefix.length()).trim();
    if (userPrompt.isEmpty()) {
      event.getChannel().sendMessage("Please provide a question after the command.").queue();
      return;
    }

    // Placeholder for actual DeepSeek API integration
    event.getChannel().sendMessage("DeepSeek response would be processed here for: " + userPrompt).queue();
  }
}
