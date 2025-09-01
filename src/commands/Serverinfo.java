package commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.awt.Color;
import java.time.format.DateTimeFormatter;

public class Serverinfo {

    public static SlashCommandData getCommandData() {
        return Commands.slash("serverinfo", "Displays server information")
                .setGuildOnly(true);
    }

    public static void execute(SlashCommandInteractionEvent interaction) {
        if (!interaction.isFromGuild()) {
            interaction.reply("This command can only be used within a server").setEphemeral(true).queue();
            return;
        }

        try {
            Guild guild = interaction.getGuild();

            guild.loadMembers().onSuccess(members -> {
                long textChannels = guild.getChannels().stream()
                    .filter(channel -> channel.getType() == ChannelType.TEXT)
                    .count();

                long voiceChannels = guild.getChannels().stream()
                    .filter(channel -> channel.getType() == ChannelType.VOICE)
                    .count();

                long categories = guild.getChannels().stream()
                    .filter(channel -> channel.getType() == ChannelType.CATEGORY)
                    .count();

                guild.retrieveOwner().queue(owner -> {
                    String iconUrl = guild.getIconUrl();

                    EmbedBuilder serverinfoEmbed = new EmbedBuilder()
                        .setColor(Color.RED)
                        .setAuthor(guild.getName(), null, iconUrl)
                        .addField("👑 Owner", owner.getUser().getAsTag(), true)
                        .addField("📅 Created",
                            guild.getTimeCreated().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")), true)
                        .addField("🏷️ Server ID", guild.getId(), true)
                        .addField("👥 Members", String.valueOf(guild.getMemberCount()), true)
                        .addField("🤖 Bots",
                            String.valueOf(members.stream().mapToInt(member -> member.getUser().isBot() ? 1 : 0).sum()), true)
                        .addField("📋 Channels",
                            "📝 Text: " + textChannels + "\n🔊 Voice: " + voiceChannels + "\n📁 Categories: " + categories, true)
                        .addField("🛡️ Roles", String.valueOf(guild.getRoles().size()), true)
                        .addField("😀 Emojis", String.valueOf(guild.getEmojis().size()), true)
                        .addField("🚀 Boosts",
                            "Level: " + guild.getBoostTier().getKey() + "\nBoosts: " + guild.getBoostCount(), true);

                    if (iconUrl != null) {
                        serverinfoEmbed.setThumbnail(iconUrl);
                    }

                    interaction.replyEmbeds(serverinfoEmbed.build()).queue();
                });
            });

        } catch (Exception error) {
            System.err.println("Serverinfo command error: " + error.getMessage());
            interaction.reply("❌ Failed to display server information").setEphemeral(true).queue();
        }
    }
}
