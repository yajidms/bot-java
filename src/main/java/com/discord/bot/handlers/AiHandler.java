package com.discord.bot.handlers;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import io.github.cdimascio.dotenv.Dotenv;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

// Apache POI imports for Office documents
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFTextShape;

import java.awt.Color;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AiHandler {

    private static boolean aiStatus = true;
    private static final Dotenv dotenv = Dotenv.configure().load();
    private static final String GEMINI_API_KEY = dotenv.get("GEMINI_API_KEY");
    private static final String TOGETHER_API_KEY = dotenv.get("TOGETHER_API_KEY");
    private static final String LOG_CHANNEL_ID = dotenv.get("LOG_CHANNEL_ID");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Model configurations
    private static final String LLAMA_MODEL = "meta-llama/Llama-4-Maverick-17B-128E-Instruct-FP8";
    private static final String DEEPSEEK_MODEL = "deepseek-ai/DeepSeek-R1";

    private static final List<String> CODE_EXTENSIONS = Arrays.asList(
            ".txt", ".js", ".ts", ".jsx", ".tsx", ".py", ".java", ".c", ".cpp", ".cs",
            ".rb", ".go", ".php", ".swift", ".kt", ".kts", ".rs", ".scala", ".sh",
            ".bat", ".pl", ".lua", ".r", ".m", ".vb", ".dart", ".html", ".css", ".scss",
            ".less", ".json", ".xml", ".yml", ".yaml", ".md", ".ini", ".cfg", ".toml",
            ".sql", ".asm", ".s", ".h", ".hpp", ".vue", ".coffee", ".erl", ".ex", ".exs",
            ".fs", ".fsx", ".groovy", ".jl", ".lisp", ".clj", ".cljs", ".ml", ".mli",
            ".nim", ".ps1", ".psm1", ".psd1", ".rkt", ".vbs", ".v", ".sv", ".svelte", ".jar"
    );

    private static final List<String> IMAGE_EXTENSIONS = Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".svg"
    );

    /**
     * Formats <think>...</think> blocks into Discord -# blockquote format
     * Matches the JS formatThinkBlockquote function exactly
     */
    private static String formatThinkBlockquote(String text) {
        Pattern pattern = Pattern.compile("<think>([\\s\\S]*?)</think>");
        Matcher matcher = pattern.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String content = matcher.group(1);
            String[] lines = content.split("\n");
            StringBuilder replacement = new StringBuilder();
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    replacement.append("-# ").append(line).append("\n");
                }
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Splits message into chunks for Discord's 4096 char embed limit
     * Matches the JS splitMessage function
     */
    public static List<String> splitMessage(String text, int maxLength) {
        if (text == null) text = "";
        if (text.length() <= maxLength) {
            return List.of(text);
        }

        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        String[] lines = text.split("\n");

        for (String line : lines) {
            if (currentChunk.isEmpty() && line.length() > maxLength) {
                // Line is too long, split it by character
                for (int i = 0; i < line.length(); i += maxLength) {
                    chunks.add(line.substring(i, Math.min(i + maxLength, line.length())));
                }
            } else if (currentChunk.length() + line.length() + 1 <= maxLength) {
                currentChunk.append(line).append("\n");
            } else {
                if (!currentChunk.toString().trim().isEmpty()) {
                    chunks.add(currentChunk.toString().trim());
                }
                if (line.length() > maxLength) {
                    for (int i = 0; i < line.length(); i += maxLength) {
                        chunks.add(line.substring(i, Math.min(i + maxLength, line.length())));
                    }
                    currentChunk = new StringBuilder();
                } else {
                    currentChunk = new StringBuilder(line).append("\n");
                }
            }
        }

        if (!currentChunk.toString().trim().isEmpty()) {
            chunks.add(currentChunk.toString().trim());
        }

        if (chunks.isEmpty() && !text.isEmpty()) {
            for (int i = 0; i < text.length(); i += maxLength) {
                chunks.add(text.substring(i, Math.min(i + maxLength, text.length())));
            }
        }

        return chunks.stream().filter(s -> !s.isEmpty()).toList();
    }

    /**
     * Reads attachment content - supports text, images (with OCR), PDF, and Office documents
     * Matches the JS readAttachment function
     */
    public static String readAttachment(Message.Attachment attachment) {
        try {
            String url = attachment.getUrl();
            String name = attachment.getFileName().toLowerCase();

            Path tempDir = Paths.get("temp");
            if (!Files.exists(tempDir)) {
                System.out.println("[File Read] Creating temporary directory: " + tempDir);
                Files.createDirectories(tempDir);
            }

            Path tempPath = tempDir.resolve(System.currentTimeMillis() + "_" + attachment.getId() + "_" + name);
            System.out.println("[File Read] Downloading attachment: " + name + " to " + tempPath);

            // Download file
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpGet request = new HttpGet(url);
                try (ClassicHttpResponse response = client.execute(request);
                     InputStream inputStream = response.getEntity().getContent();
                     FileOutputStream outputStream = new FileOutputStream(tempPath.toFile())) {
                    byte[] data = inputStream.readAllBytes();
                    outputStream.write(data);
                    System.out.println("[File Read] Download successful: " + name + " (" + data.length + " bytes)");
                }
            }

            StringBuilder text = new StringBuilder("[File Name: ").append(name).append("]\n");
            File file = tempPath.toFile();
            long fileSize = file.length();

            if (fileSize == 0) {
                text.append("[Empty File]");
                System.out.println("[File Read] File " + name + " is empty.");
            } else if (CODE_EXTENSIONS.stream().anyMatch(name::endsWith)) {
                text.append(Files.readString(tempPath, StandardCharsets.UTF_8));
                System.out.println("[File Read] " + name + " read as text/code.");
            } else if (IMAGE_EXTENSIONS.stream().anyMatch(name::endsWith)) {
                text.append("[Image Info: size ").append(fileSize).append(" bytes]\n");

                if (!name.endsWith(".svg") && !name.endsWith(".gif")) {
                    try {
                        text.append("\n[OCR Result Start]\n");
                        Tesseract tesseract = new Tesseract();
                        // Set tessdata path if needed
                        String tessDataPath = System.getenv("TESSDATA_PREFIX");
                        if (tessDataPath != null) {
                            tesseract.setDatapath(tessDataPath);
                        }
                        tesseract.setLanguage("eng");
                        String ocrResult = tesseract.doOCR(file);
                        text.append(ocrResult.trim().isEmpty() ? "(Text not detected)" : ocrResult.trim());
                        text.append("\n[OCR Result End]");
                        System.out.println("[File Read] OCR finished: " + name);
                    } catch (TesseractException e) {
                        System.err.println("[File Read] OCR Error " + name + ": " + e.getMessage());
                        text.append("\n[OCR Failed: ").append(e.getMessage()).append("]");
                    }
                } else {
                    text.append("[OCR skipped for GIF/SVG]");
                }
            } else if (name.endsWith(".pdf")) {
                try (PDDocument document = Loader.loadPDF(file)) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    text.append(stripper.getText(document));
                    System.out.println("[File Read] " + name + " read as PDF.");
                } catch (Exception e) {
                    text.append("[Failed to read PDF: ").append(e.getMessage()).append("]");
                    System.err.println("[File Read] PDF Error " + name + ": " + e.getMessage());
                }
            } else if (name.endsWith(".docx")) {
                try (FileInputStream fis = new FileInputStream(file);
                     XWPFDocument doc = new XWPFDocument(fis)) {
                    StringBuilder docText = new StringBuilder();
                    for (XWPFParagraph para : doc.getParagraphs()) {
                        docText.append(para.getText()).append("\n");
                    }
                    text.append(docText.toString().isEmpty() ?
                        "[Word document processed but no text content found]" : docText.toString());
                    System.out.println("[File Read] " + name + " read as DOCX.");
                } catch (Exception e) {
                    text.append("[Failed to process Word document: ").append(e.getMessage()).append("]");
                    System.err.println("[File Read] DOCX Error " + name + ": " + e.getMessage());
                }
            } else if (name.endsWith(".doc")) {
                try (FileInputStream fis = new FileInputStream(file);
                     HWPFDocument doc = new HWPFDocument(fis);
                     WordExtractor extractor = new WordExtractor(doc)) {
                    String docText = extractor.getText();
                    text.append(docText.isEmpty() ?
                        "[Word document processed but no text content found]" : docText);
                    System.out.println("[File Read] " + name + " read as DOC.");
                } catch (Exception e) {
                    text.append("[Failed to process Word document: ").append(e.getMessage()).append("]");
                    System.err.println("[File Read] DOC Error " + name + ": " + e.getMessage());
                }
            } else if (name.endsWith(".xlsx")) {
                try (FileInputStream fis = new FileInputStream(file);
                     XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
                    StringBuilder excelText = new StringBuilder();
                    for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                        XSSFSheet sheet = workbook.getSheetAt(i);
                        excelText.append("--- Sheet: ").append(sheet.getSheetName()).append(" ---\n");
                        for (org.apache.poi.ss.usermodel.Row row : sheet) {
                            for (org.apache.poi.ss.usermodel.Cell cell : row) {
                                excelText.append(cell.toString()).append("\t");
                            }
                            excelText.append("\n");
                        }
                    }
                    text.append(excelText.toString().isEmpty() ?
                        "[Excel document processed but no text content found]" : excelText.toString());
                    System.out.println("[File Read] " + name + " read as XLSX.");
                } catch (Exception e) {
                    text.append("[Failed to process Excel document: ").append(e.getMessage()).append("]");
                    System.err.println("[File Read] XLSX Error " + name + ": " + e.getMessage());
                }
            } else if (name.endsWith(".xls")) {
                try (FileInputStream fis = new FileInputStream(file);
                     HSSFWorkbook workbook = new HSSFWorkbook(fis)) {
                    StringBuilder excelText = new StringBuilder();
                    for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                        HSSFSheet sheet = workbook.getSheetAt(i);
                        excelText.append("--- Sheet: ").append(sheet.getSheetName()).append(" ---\n");
                        for (org.apache.poi.ss.usermodel.Row row : sheet) {
                            for (org.apache.poi.ss.usermodel.Cell cell : row) {
                                excelText.append(cell.toString()).append("\t");
                            }
                            excelText.append("\n");
                        }
                    }
                    text.append(excelText.toString().isEmpty() ?
                        "[Excel document processed but no text content found]" : excelText.toString());
                    System.out.println("[File Read] " + name + " read as XLS.");
                } catch (Exception e) {
                    text.append("[Failed to process Excel document: ").append(e.getMessage()).append("]");
                    System.err.println("[File Read] XLS Error " + name + ": " + e.getMessage());
                }
            } else if (name.endsWith(".pptx")) {
                try (FileInputStream fis = new FileInputStream(file);
                     XMLSlideShow ppt = new XMLSlideShow(fis)) {
                    StringBuilder pptText = new StringBuilder();
                    int slideNum = 1;
                    for (XSLFSlide slide : ppt.getSlides()) {
                        pptText.append("--- Slide ").append(slideNum++).append(" ---\n");
                        for (XSLFShape shape : slide.getShapes()) {
                            if (shape instanceof XSLFTextShape textShape) {
                                pptText.append(textShape.getText()).append("\n");
                            }
                        }
                    }
                    text.append(pptText.toString().isEmpty() ?
                        "[PowerPoint document processed but no text content found]" : pptText.toString());
                    System.out.println("[File Read] " + name + " read as PPTX.");
                } catch (Exception e) {
                    text.append("[Failed to process PowerPoint document: ").append(e.getMessage()).append("]");
                    System.err.println("[File Read] PPTX Error " + name + ": " + e.getMessage());
                }
            } else if (name.endsWith(".ppt")) {
                try (FileInputStream fis = new FileInputStream(file);
                     HSLFSlideShow ppt = new HSLFSlideShow(fis)) {
                    StringBuilder pptText = new StringBuilder();
                    int slideNum = 1;
                    for (HSLFSlide slide : ppt.getSlides()) {
                        pptText.append("--- Slide ").append(slideNum++).append(" ---\n");
                        for (HSLFShape shape : slide.getShapes()) {
                            if (shape instanceof HSLFTextShape textShape) {
                                pptText.append(textShape.getText()).append("\n");
                            }
                        }
                    }
                    text.append(pptText.toString().isEmpty() ?
                        "[PowerPoint document processed but no text content found]" : pptText.toString());
                    System.out.println("[File Read] " + name + " read as PPT.");
                } catch (Exception e) {
                    text.append("[Failed to process PowerPoint document: ").append(e.getMessage()).append("]");
                    System.err.println("[File Read] PPT Error " + name + ": " + e.getMessage());
                }
            } else {
                text.append("[Unsupported file type]");
                System.out.println("[File Read] Type " + name + " is not supported.");
            }

            // Truncate if too long
            final int MAX_FILE_LEN = 20000;
            String result = text.toString();
            if (result.length() > MAX_FILE_LEN) {
                result = result.substring(0, MAX_FILE_LEN) + "\n\n[File Content " + name + " Truncated...]";
                System.out.println("[File Read] Content of " + name + " truncated.");
            }

            // Cleanup temp file
            try {
                Files.deleteIfExists(tempPath);
                System.out.println("[File Read] Deleted temp: " + tempPath);
            } catch (Exception e) {
                System.err.println("[File Read] Failed to delete temp " + tempPath + ": " + e.getMessage());
            }

            return result;

        } catch (Exception e) {
            System.err.println("[File Read] Failed to process attachment: " + e.getMessage());
            return "[Failed to read file: " + attachment.getFileName() + " - Error: " + e.getMessage() + "]";
        }
    }

    public static void handleAiChat(MessageReceivedEvent event) {
        String content = event.getMessage().getContentDisplay();

        // Check AI status
        if (!aiStatus && event.getMember() != null && !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.getChannel().sendMessage("AI feature is currently disabled. Contact admin to enable this feature.")
                    .queue();
            return;
        }

        // Tutorial message for f.ai
        if (content.trim().equals("f.ai")) {
            var tutorialEmbed = new EmbedBuilder()
                    .setTitle("How to Use AI Chat")
                    .setDescription("""
                            **Use the following command to ask the AI:**

                            **Gemini Pro Preview:**
                            `f.geminipropreview [your question]`
                            **Gemini Pro:**
                            `f.geminipro [your question]`
                            **Gemini Flash:**
                            `f.geminiflash [your question]`
                            **Llama AI:**
                            `f.llama [your question]`
                            **DeepSeek R1:**
                            `f.deepseek-r1 [your question]`
                            _You can also attach files (documents and images) to include their content in your question!_""")
                    .setColor(new Color(0x5865f2))
                    .build();

            event.getChannel().sendMessageEmbeds(tutorialEmbed).queue();
            return;
        }

        // Process in async
        CompletableFuture.runAsync(() -> {
            try {
                // Read attachments
                StringBuilder fileContent = new StringBuilder();
                if (!event.getMessage().getAttachments().isEmpty()) {
                    for (Message.Attachment attachment : event.getMessage().getAttachments()) {
                        try {
                            String attachmentContent = readAttachment(attachment);
                            fileContent.append(String.format(
                                    "--- File: %s ---\n%s\n--- End of %s ---\n\n",
                                    attachment.getFileName(), attachmentContent, attachment.getFileName()));
                        } catch (Exception e) {
                            fileContent.append(String.format(
                                    "--- File: %s ---\n[Failed to read attachment: %s]\n--- End of %s ---\n\n",
                                    attachment.getFileName(), e.getMessage(), attachment.getFileName()));
                        }
                    }
                }

                // Route to appropriate handler
                if (content.startsWith("f.geminipropreview")) {
                    handleGeminiResponse(event, "f.geminipropreview", fileContent.toString(),
                            "gemini-3-pro-preview", "Gemini 3.0 Pro Preview", "https://i.imgur.com/7FNd7DF.png");
                } else if (content.startsWith("f.geminipro")) {
                    handleGeminiResponse(event, "f.geminipro", fileContent.toString(),
                            "gemini-2.5-pro", "Gemini 2.5 Pro", "https://i.imgur.com/7FNd7DF.png");
                } else if (content.startsWith("f.geminiflash")) {
                    handleGeminiResponse(event, "f.geminiflash", fileContent.toString(),
                            "gemini-2.5-flash", "Gemini 2.5 Flash Preview", "https://i.imgur.com/7FNd7DF.png");
                } else if (content.startsWith("f.llama")) {
                    handleLlamaResponse(event, "f.llama", fileContent.toString());
                } else if (content.startsWith("f.deepseek-r1")) {
                    handleDeepSeekResponse(event, "f.deepseek-r1", fileContent.toString());
                }

            } catch (Exception e) {
                event.getChannel().sendMessage("An error occurred while processing your request: " + e.getMessage())
                        .queue();
            }
        });
    }

    private static void handleGeminiResponse(MessageReceivedEvent event, String prefix, String fileContent,
                                              String modelId, String modelName, String iconUrl) {
        String userQuestion = event.getMessage().getContentDisplay().substring(prefix.length()).trim();

        if (userQuestion.isEmpty() && event.getMessage().getAttachments().isEmpty()) {
            event.getChannel().sendMessage("Please write down the issue you want to ask after `" + prefix + "`.")
                    .queue();
            return;
        }

        // Combine prompt with file content
        String prompt = fileContent.isEmpty() ? userQuestion :
                userQuestion + "\n\n[File Content Start]\n" + fileContent + "[File Content End]";

        // Send thinking message
        var thinkingEmbed = new EmbedBuilder()
                .setDescription("thinking...")
                .setColor(new Color(0xffa500))
                .setAuthor("Powered by " + modelName, null, iconUrl)
                .setTimestamp(Instant.now())
                .build();

        event.getMessage().replyEmbeds(thinkingEmbed).queue(thinkingMessage -> {
            try {
                String answer = callGeminiApi(prompt, modelId);

                if (answer == null || answer.isEmpty()) {
                    throw new Exception("AI returned empty response");
                }

                sendResponseWithEdit(thinkingMessage, answer, modelName, iconUrl, event.getAuthor().getName());

                // Send log
                sendAiLog(event, modelName, userQuestion, 1);

            } catch (Exception e) {
                System.err.println(modelName + " Processing Error: " + e.getMessage());
                var errorEmbed = new EmbedBuilder()
                        .setTitle("Processing Error")
                        .setDescription("Failed to generate " + modelName + " response")
                        .addField("Error", e.getMessage().substring(0, Math.min(e.getMessage().length(), 1024)), false)
                        .addField("User", event.getAuthor().getAsMention(), false)
                        .setColor(Color.RED)
                        .build();
                thinkingMessage.editMessageEmbeds(errorEmbed).queue();
            }
        });
    }

    private static void handleLlamaResponse(MessageReceivedEvent event, String prefix, String fileContent) {
        String userQuestion = event.getMessage().getContentDisplay().substring(prefix.length()).trim();

        if (userQuestion.isEmpty() && event.getMessage().getAttachments().isEmpty()) {
            event.getChannel().sendMessage("Please write down the issue you want to ask after `f.llama`.")
                    .queue();
            return;
        }

        String prompt = fileContent.isEmpty() ? userQuestion :
                userQuestion + "\n\n[File Content Start]\n" + fileContent + "[File Content End]";

        try {
            String answer = callTogetherApi(prompt, LLAMA_MODEL);

            if (answer == null || answer.isEmpty()) {
                throw new Exception("AI returned empty response");
            }

            int partsSent = sendResponse(event, answer, "Llama 4 Maverick AI", "https://i.imgur.com/i0vcc7G.jpeg");
            sendAiLog(event, "Llama AI", userQuestion, partsSent);

        } catch (Exception e) {
            handleError(event, "Llama 4", e, userQuestion);
        }
    }

    private static void handleDeepSeekResponse(MessageReceivedEvent event, String prefix, String fileContent) {
        String userQuestion = event.getMessage().getContentDisplay().substring(prefix.length()).trim();

        if (userQuestion.isEmpty() && event.getMessage().getAttachments().isEmpty()) {
            event.getChannel().sendMessage("Please write down the issue you want to ask after `f.deepseek-r1`.")
                    .queue();
            return;
        }

        String prompt = fileContent.isEmpty() ? userQuestion :
                userQuestion + "\n\n[File Content Start]\n" + fileContent + "[File Content End]";

        try {
            String answer = callTogetherApi(prompt, DEEPSEEK_MODEL);

            if (answer == null || answer.isEmpty()) {
                throw new Exception("AI returned empty response");
            }

            // Apply think blockquote formatting for DeepSeek
            answer = formatThinkBlockquote(answer);

            int partsSent = sendResponse(event, answer, "DeepSeek R1", "https://i.imgur.com/yIilZ11.png");
            sendAiLog(event, "DeepSeek R1", userQuestion, partsSent);

        } catch (Exception e) {
            handleError(event, "DeepSeek R1", e, userQuestion);
        }
    }

    /**
     * Calls the Gemini API with the given prompt
     */
    private static String callGeminiApi(String prompt, String modelId) throws Exception {
        String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + modelId +
                ":generateContent?key=" + GEMINI_API_KEY;

        ObjectNode requestBody = objectMapper.createObjectNode();
        ArrayNode contents = requestBody.putArray("contents");
        ObjectNode content = contents.addObject();
        ArrayNode parts = content.putArray("parts");
        ObjectNode part = parts.addObject();
        part.put("text", prompt);

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(apiUrl);
            request.setHeader("Content-Type", "application/json");
            request.setEntity(new StringEntity(objectMapper.writeValueAsString(requestBody), ContentType.APPLICATION_JSON));

            try (ClassicHttpResponse response = client.execute(request)) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                JsonNode responseJson = objectMapper.readTree(jsonResponse);

                // Check for error
                if (responseJson.has("error")) {
                    throw new Exception(responseJson.get("error").get("message").asText());
                }

                // Extract text from response
                JsonNode candidates = responseJson.get("candidates");
                if (candidates != null && candidates.isArray() && !candidates.isEmpty()) {
                    JsonNode firstCandidate = candidates.get(0);
                    JsonNode contentNode = firstCandidate.get("content");
                    if (contentNode != null) {
                        JsonNode partsNode = contentNode.get("parts");
                        if (partsNode != null && partsNode.isArray() && !partsNode.isEmpty()) {
                            return partsNode.get(0).get("text").asText();
                        }
                    }
                }

                throw new Exception("Invalid response structure from Gemini API");
            }
        }
    }

    /**
     * Calls the Together AI API (for Llama and DeepSeek)
     */
    private static String callTogetherApi(String prompt, String model) throws Exception {
        String apiUrl = "https://api.together.ai/v1/chat/completions";

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);
        requestBody.put("stream", false);

        ArrayNode messages = requestBody.putArray("messages");
        ObjectNode message = messages.addObject();
        message.put("role", "user");

        // For Llama, content is an array with type/text objects
        if (model.contains("llama") || model.contains("Llama")) {
            ArrayNode contentArray = message.putArray("content");
            ObjectNode textContent = contentArray.addObject();
            textContent.put("type", "text");
            textContent.put("text", prompt);
        } else {
            message.put("content", prompt);
        }

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(apiUrl);
            request.setHeader("Content-Type", "application/json");
            request.setHeader("Authorization", "Bearer " + TOGETHER_API_KEY);
            request.setEntity(new StringEntity(objectMapper.writeValueAsString(requestBody), ContentType.APPLICATION_JSON));

            try (ClassicHttpResponse response = client.execute(request)) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                JsonNode responseJson = objectMapper.readTree(jsonResponse);

                // Check for error
                if (responseJson.has("error")) {
                    throw new Exception(responseJson.get("error").get("message").asText());
                }

                // Extract message content
                JsonNode choices = responseJson.get("choices");
                if (choices != null && choices.isArray() && !choices.isEmpty()) {
                    return choices.get(0).get("message").get("content").asText();
                }

                throw new Exception("Invalid response structure from Together API");
            }
        }
    }

    /**
     * Sends response with multiple parts if needed (for embed responses)
     */
    private static int sendResponse(MessageReceivedEvent event, String answer, String modelName, String iconUrl) {
        List<String> answerParts = splitMessage(answer, 4096);
        List<String> filteredParts = answerParts.stream().filter(p -> !p.trim().isEmpty()).toList();

        if (filteredParts.isEmpty()) {
            event.getChannel().sendMessage("AI returned empty response").queue();
            return 0;
        }

        // Send first embed
        var firstEmbed = new EmbedBuilder()
                .setTitle("Answer for " + event.getAuthor().getName())
                .setDescription(filteredParts.get(0))
                .setAuthor("Powered by " + modelName, null, iconUrl)
                .setFooter("AI-generated content may be inaccurate")
                .setTimestamp(Instant.now())
                .build();

        event.getMessage().replyEmbeds(firstEmbed).queue(lastMessage -> {
            // Send continuation parts
            sendContinuationParts(lastMessage, filteredParts, 1, modelName, iconUrl);
        });

        return filteredParts.size();
    }

    /**
     * Sends response by editing the thinking message
     */
    private static void sendResponseWithEdit(Message thinkingMessage, String answer, String modelName,
                                              String iconUrl, String username) {
        List<String> answerParts = splitMessage(answer, 4096);
        List<String> filteredParts = answerParts.stream().filter(p -> !p.trim().isEmpty()).toList();

        if (filteredParts.isEmpty()) {
            thinkingMessage.editMessage("AI returned empty response").queue();
            return;
        }

        // Edit thinking message with first part
        var firstEmbed = new EmbedBuilder()
                .setTitle("Answer for " + username)
                .setDescription(filteredParts.get(0))
                .setAuthor("Powered by " + modelName, null, iconUrl)
                .setFooter("AI-generated content may be inaccurate")
                .setTimestamp(Instant.now())
                .build();

        thinkingMessage.editMessageEmbeds(firstEmbed).queue(editedMessage -> {
            // Send continuation parts
            sendContinuationParts(editedMessage, filteredParts, 1, modelName, iconUrl);
        });
    }

    /**
     * Recursively sends continuation parts with delay
     */
    private static void sendContinuationParts(Message lastMessage, List<String> parts, int index,
                                               String modelName, String iconUrl) {
        if (index >= parts.size()) return;

        try {
            Thread.sleep(1000); // 1 second delay between parts
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        var continueEmbed = new EmbedBuilder()
                .setTitle("Continued Answer [Part " + (index + 1) + "]")
                .setDescription(parts.get(index))
                .setAuthor("Powered by " + modelName, null, iconUrl)
                .setFooter("AI-generated content may be inaccurate")
                .setTimestamp(Instant.now())
                .build();

        lastMessage.replyEmbeds(continueEmbed).queue(newMessage -> {
            sendContinuationParts(newMessage, parts, index + 1, modelName, iconUrl);
        });
    }

    private static void handleError(MessageReceivedEvent event, String modelName, Exception error, String userQuestion) {
        System.err.println(modelName + " Processing Error: " + error.getMessage());
        error.printStackTrace();

        var errorEmbed = new EmbedBuilder()
                .setTitle("⚠️ Processing Error")
                .setDescription("Failed to generate " + modelName + " response")
                .addField("Error", error.getMessage().substring(0, Math.min(error.getMessage().length(), 1024)), false)
                .addField("User", event.getAuthor().getAsMention(), false)
                .setColor(Color.RED)
                .build();

        event.getMessage().replyEmbeds(errorEmbed).queue();

        // Send error log
        if (LOG_CHANNEL_ID != null && event.getJDA().getTextChannelById(LOG_CHANNEL_ID) != null) {
            LogHandler.LogDetails logDetails = new LogHandler.LogDetails();
            logDetails.title = modelName + " AI Processing Failure";
            logDetails.description = "**Question:** " + userQuestion;
            logDetails.userId = event.getAuthor().getId();
            logDetails.messageId = event.getMessageId();
            logDetails.author = new LogHandler.LogDetails.Author(
                    event.getJDA().getSelfUser().getName(),
                    event.getJDA().getSelfUser().getAvatarUrl()
            );
            logDetails.fields = List.of(
                    new net.dv8tion.jda.api.entities.MessageEmbed.Field("Error", error.getMessage(), false),
                    new net.dv8tion.jda.api.entities.MessageEmbed.Field("Stack",
                            error.getStackTrace().length > 0 ?
                                    error.getStackTrace()[0].toString().substring(0, Math.min(1024, error.getStackTrace()[0].toString().length())) : "-", false)
            );
            LogHandler.sendLog(event.getJDA(), LOG_CHANNEL_ID, logDetails);
        }
    }

    private static void sendAiLog(MessageReceivedEvent event, String modelName, String userQuestion, int partsSent) {
        if (LOG_CHANNEL_ID == null) return;

        LogHandler.LogDetails logDetails = new LogHandler.LogDetails();
        logDetails.title = modelName + " Request Processed";
        logDetails.description = "**Question:** " + userQuestion;
        logDetails.userId = event.getAuthor().getId();
        logDetails.messageId = event.getMessageId();
        logDetails.author = new LogHandler.LogDetails.Author(
                event.getAuthor().getName(),
                event.getAuthor().getAvatarUrl()
        );
        logDetails.fields = List.of(
                new net.dv8tion.jda.api.entities.MessageEmbed.Field("User", "<@" + event.getAuthor().getId() + ">", true),
                new net.dv8tion.jda.api.entities.MessageEmbed.Field("Parts Sent", String.valueOf(partsSent), true)
        );
        LogHandler.sendLog(event.getJDA(), LOG_CHANNEL_ID, logDetails);
    }

    public static void toggleAiStatus(boolean status) {
        aiStatus = status;
    }

    public static boolean getAiStatus() {
        return aiStatus;
    }
}

