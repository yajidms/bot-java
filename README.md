# How to Use Furina Source Bot (Java Edition)

Welcome to the Furina bot source (Java version). This bot is ready to destroy your server with a variety of gajelas (unclear) features. Follow these steps to get started:
[for those who want to use this bot Furina, take this blue text or link](https://discord.com/oauth2/authorize?client_id=1368819487880642641&permissions=8&integration_type=0&scope=bot)

## 📋 Prerequisites

Make sure you have the following installed:

* **Java Development Kit (JDK):** Version 25 or later (required)
* **Maven:** Build automation tool for Java projects
* **IDE:** IntelliJ IDEA, Eclipse, or VS Code with Java extensions

## ⚙️ Installation

1.  **Clone Repository:** Get the bot code to your computer.
    ```bash
    git clone https://github.com/yajidms/bot-java.git
    cd bot-java
    ```
2.  **Install Dependencies:** Install all the packages the bot needs using Maven.
    ```bash
    mvn clean install
    ```

## 🔑 Important Configuration (`.env`)

Create a file called `.env` in the main directory of the project with the contents of the `API Key` and `Bot ID` and `User ID`. This is key for the bot to work properly!

```dotenv
# --- Key Credentials ---
DISCORD_TOKEN=
CLIENT_ID=

# --- API Keys (Important for AI & Downloader Features) ---
# Gemini
GEMINI_API_KEY=

# Together AI or Deepinfra (For Llama & DeepSeek)
TOGETHER_API_KEY=
DEEPINFRA_KEY=

# --- ID Channel & Role (Customize with your server) ---
# Logging
LOG_CHANNEL_ID=
DEV_LOG_CHANNEL_ID=

# Role
MUTED_ROLE_ID=
# Developer IDs (Separate with commas if more than one)
DEV_ID=ID1,ID2

# Guild (Server) IDs where the bot is active (Separate with commas if more than one)
GUILD_ID=
```

**Important:** Never share your `.env` file or bot token with anyone!

## ▶️ Running the Bot

Once the configuration is complete, run the bot with the command:

```bash
# Using Maven
mvn exec:java -Dexec.mainClass="com.discord.bot.Main"

# Or compile and run the JAR
mvn package
java -jar target/discord-bot-1.0.0.jar
```

## 📦 Dependencies

This bot uses the following main libraries:

| Library | Version | Description |
|---------|---------|-------------|
| JDA | 5.2.2 | Java Discord API |
| HttpClient5 | 5.4.1 | HTTP Client for API requests |
| Jackson | 2.18.2 | JSON Processing |
| dotenv-java | 3.1.0 | Environment Variables |
| PDFBox | 3.0.3 | PDF Processing |
| Tess4J | 5.13.0 | OCR for Images |
| Apache POI | 5.3.0 | Office Documents (docx, xlsx, pptx) |

## ✨ Main Features & Commands

This bot comes with various commands, accessible via `slash command` (`/`).

### 📥 Media Downloader (Slash Command)

Use the `/downloader` command to download media:

* `/downloader <url>`: Download video from various platforms
  * Supports: Instagram, TikTok, Facebook, Twitter/X, YouTube

*Example:* `/downloader url:https://www.instagram.com/reel/contoh123/`

### 🧠 AI Features (Slash Command)

Interact with advanced AI models:

* `/aichat [initial_prompt] [file]`: Start an interactive chat session with `Gemini 2.5 Flash` within a forum *thread*. Attach files if needed.
* `/endchat`: Ends the currently active `/aichat` session in the *thread*.

*Supported file types: txt, pdf, docx, xlsx, pptx, images*

### 🛠️ Utilities (Slash Command)

Everyday helper commands:

* `/help`: Displays a list of all available *slash commands*.
* `/info`: Displays information about this bot.
* `/ping`: Checks the bot's latency and `API Discord` latency.
* `/avatar [user] [type]`: View a user's global or server avatar.
* `/banner [user] [type]`: View a user's global or server banner.
* `/userinfo [user]`: Displays detailed information about a Discord user.
* `/serverinfo`: Displays detailed information about the current server.
* `/serverstats`: Displays server statistics.
* `/list-roles`: Displays a list of all roles on the server.
* `/adzan [city]`: Displays prayer times for a specific city in Indonesia.

### 🛡️ Moderation (Slash Command)

Commands for maintaining server order (requires permissions):

* `/ban <user> [time] [reason]`: Bans a user from the server (can be temporary).
* `/unban <user_id>`: Revokes a user's ban based on ID.
* `/kick <user> [reason]`: Kicks a user from the server.
* `/mute <user> [time] [reason]`: Prevents a user from sending messages (temporary or permanent).
* `/unmute <user>`: Revokes a user's muted status.
* `/timeout <user> <duration> [reason]`: Gives a user a timeout (cannot interact) for a specified duration.
* `/untimeout <user>`: Revokes a user's timeout status.
* `/clean <amount>`: Deletes a specified number of recent messages in the channel (requires `Administrator` permission).

### 🔒 Developer Commands (Slash Command)

Only for users whose IDs are listed in the `.env` file:

* `/restart`: Restarts the bot.
* `/say <message> [reply_to]`: Sends a message as the bot, can reply to another message.
* `/setstatus <type> <message> <duration>`: Sets the bot's online status and custom message.
* `/toggleembed <option>`: Enables or disables the automatic embed detection system.

## 📁 Project Structure

```
bot-java/
├── pom.xml 
├── .env
└── src/
    └── main/
        └── java/
            └── com/
                └── discord/
                    └── bot/
                        ├── Main.java 
                        ├── commands/
                        │   ├── Adzan.java
                        │   ├── Aichat.java
                        │   ├── Avatar.java
                        │   ├── Ban.java
                        │   ├── Banner.java
                        │   ├── Clean.java
                        │   ├── Downloader.java
                        │   ├── Endchat.java
                        │   ├── Help.java
                        │   ├── Info.java
                        │   ├── Kick.java
                        │   ├── ListRoles.java
                        │   ├── Mute.java
                        │   ├── Ping.java
                        │   ├── Restart.java
                        │   ├── Say.java
                        │   ├── Serverinfo.java
                        │   ├── Serverstats.java
                        │   ├── Setstatus.java
                        │   ├── Timeout.java
                        │   ├── Toggleembed.java
                        │   ├── Unban.java
                        │   ├── Unmute.java
                        │   ├── Untimeout.java
                        │   └── Userinfo.java
                        ├── events/
                        │   ├── InteractionCreate.java
                        │   ├── MessageCreate.java
                        │   ├── MessageDelete.java
                        │   ├── MessageUpdate.java
                        │   └── Ready.java
                        ├── handlers/
                        │   ├── AiChatState.java
                        │   ├── AiHandler.java
                        │   ├── CommandLoader.java
                        │   ├── DownloaderHandler.java
                        │   ├── EventLoader.java
                        │   ├── LogHandler.java
                        │   ├── QuoteHandler.java
                        │   ├── TwitterHandler.java
                        │   └── YtdlHandler.java
                        └── utils/
                            ├── Checkbots.java
                            ├── DeleteCommands.java
                            └── RegisterCommands.java
```

## 🔧 Utility Scripts

Located in `src/main/java/com/discord/bot/utils/`:

* `RegisterCommands.java`: Register all slash commands to Discord
* `DeleteCommands.java`: Delete all slash commands from Discord
* `Checkbots.java`: Check bot status and configuration

---

Congratulations, the bot is *not* ready! If you find any bugs or have suggestions, feel free to `report` them to this `repository`.

## 📝 Notes

* This is the **Java Edition** of the Furina bot, rewritten using **JDA (Java Discord API)**
* Requires Java 25 or later
* Uses Maven for dependency management
* All commands are now slash commands (no prefix commands)

---

gajelas, tutup aja nih commit
Berikut adalah resep sederhana untuk membuat roti jahe (gingerbread cookies) yang renyah dan beraroma khas rempah:

Bahan:

350 g tepung terigu serbaguna

1 sdt baking soda

2 sdt bubuk jahe

1 sdt bubuk kayu manis

½ sdt bubuk pala

½ sdt garam

125 g mentega, suhu ruang

100 g gula palem

100 g madu atau molase

1 butir telur

Cara Membuat:

Campur bahan kering:

Ayak tepung terigu, baking soda, bubuk jahe, kayu manis, pala, dan garam dalam satu wadah. Sisihkan.

Kocok mentega dan gula:

Gunakan mixer atau whisk untuk mengocok mentega dan gula palem hingga lembut dan mengembang.

Tambahkan bahan basah:

Masukkan madu/molase dan telur, lalu aduk rata.

Campurkan bahan kering ke bahan basah:

Masukkan campuran tepung sedikit demi sedikit sambil diaduk hingga menjadi adonan yang bisa dipulung.

Dinginkan adonan:

Bungkus adonan dengan plastik wrap dan simpan di kulkas selama 1 jam agar lebih mudah dibentuk.

Cetak dan panggang:

Panaskan oven ke suhu 175°C.

Gilas adonan dengan ketebalan sekitar 5 mm, lalu cetak sesuai selera (misalnya bentuk manusia roti jahe).

Susun di atas loyang yang sudah dialasi kertas baking.

Panggang hingga matang:

Panggang selama 8-12 menit, tergantung ketebalan adonan dan ukuran cetakan.

Biarkan dingin sebelum dihias.

Hias sesuai selera (opsional):

Gunakan royal icing (campuran putih telur dan gula halus) atau cokelat leleh untuk dekorasi.

Roti jahe ini cocok untuk camilan atau hadiah spesial, terutama saat musim liburan! Selamat mencoba!