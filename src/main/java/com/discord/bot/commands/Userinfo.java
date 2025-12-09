package com.discord.bot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.awt.Color;
import java.time.format.DateTimeFormatter;

public class Userinfo {

    public static SlashCommandData getCommandData() {
        return Commands.slash("userinfo", "Display information about a user")
                .addOption(OptionType.USER, "user", "Select a user", false);
    }

    public static void execute(SlashCommandInteractionEvent interaction) {
        User targetUser = interaction.getOption("user") != null ?
            interaction.getOption("user").getAsUser() : interaction.getUser();
        Member targetMember = interaction.getGuild().getMember(targetUser);

        try {
            String displayName = targetUser.getGlobalName() != null ?
                targetUser.getGlobalName() : targetUser.getName();
            String avatarUrl = targetUser.getAvatarUrl() != null ?
                targetUser.getAvatarUrl() : targetUser.getDefaultAvatarUrl();

            EmbedBuilder embed = new EmbedBuilder()
                .setColor(new Color(0x00ffed))
                .setTitle("User Information")
                .setThumbnail(avatarUrl)
                .addField("Username", targetUser.getName(), true)
                .addField("Display Name", displayName, true)
                .addField("User ID", targetUser.getId(), true)
                .addField("Account Created",
                    targetUser.getTimeCreated().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")), true);

            if (targetMember != null) {
                embed.addField("Joined Server",
                    targetMember.getTimeJoined().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")), true);

                if (!targetMember.getRoles().isEmpty()) {
                    StringBuilder roles = new StringBuilder();
                    targetMember.getRoles().forEach(role -> roles.append(role.getAsMention()).append(" "));
                    embed.addField("Roles (" + targetMember.getRoles().size() + ")",
                        roles.toString().trim(), false);
                }
            }

            interaction.replyEmbeds(embed.build()).queue();

        } catch (Exception error) {
            System.err.println("Userinfo command error: " + error.getMessage());
            interaction.reply("❌ Failed to display user information").setEphemeral(true).queue();
        }
    }
}
