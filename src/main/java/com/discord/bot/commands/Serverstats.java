package com.discord.bot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.awt.Color;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.time.Instant;

/**
 * Command to display server stats (CPU, RAM, uptime).
 */
public class Serverstats {

    public static CommandData getCommandData() {
        return Commands.slash("serverstats", "Displaying CPU and RAM usage from the bot server");
    }

    public static void execute(SlashCommandInteractionEvent interaction) {
        try {
            // Get system information
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            Runtime runtime = Runtime.getRuntime();

            // CPU Information
            int cpuCores = runtime.availableProcessors();
            String osName = System.getProperty("os.name");
            String osArch = System.getProperty("os.arch");
            String osVersion = System.getProperty("os.version");

            // CPU Usage (approximation using system load average)
            double cpuLoad = osBean.getSystemLoadAverage();
            double cpuUsage = (cpuLoad / cpuCores) * 100;
            if (cpuUsage < 0) cpuUsage = 0;

            // If we have access to com.sun.management, use it for more accurate readings
            double processCpuLoad = -1;
            if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
                processCpuLoad = sunOsBean.getCpuLoad() * 100;
            }

            // RAM Usage
            long totalMem = runtime.totalMemory();
            long freeMem = runtime.freeMemory();
            long maxMem = runtime.maxMemory();
            long usedMem = totalMem - freeMem;
            double memUsagePercent = ((double) usedMem / maxMem) * 100;

            // System memory if available
            long systemTotalMem = 0;
            long systemFreeMem = 0;
            if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
                systemTotalMem = sunOsBean.getTotalMemorySize();
                systemFreeMem = sunOsBean.getFreeMemorySize();
            }

            // Uptime
            long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
            long uptimeSeconds = uptimeMs / 1000;
            long days = uptimeSeconds / 86400;
            long hours = (uptimeSeconds % 86400) / 3600;
            long minutes = (uptimeSeconds % 3600) / 60;

            // Hostname
            String hostname;
            try {
                hostname = java.net.InetAddress.getLocalHost().getHostName();
            } catch (Exception e) {
                hostname = "Unknown";
            }

            // Format bytes to readable
            String jvmRamTotal = formatBytes(maxMem);
            String jvmRamUsed = formatBytes(usedMem);
            String systemRamTotal = systemTotalMem > 0 ? formatBytes(systemTotalMem) : "N/A";
            String systemRamFree = systemFreeMem > 0 ? formatBytes(systemFreeMem) : "N/A";

            // Build embed
            var embed = new EmbedBuilder()
                    .setTitle("📊 Server Stats")
                    .setColor(new Color(0x0099ff))
                    .addField("🖥️ OS", osName + " " + osVersion, false)
                    .addField("⚙️ CPU Cores", String.valueOf(cpuCores), true)
                    .addField("📈 CPU Usage", String.format("%.2f%%", processCpuLoad >= 0 ? processCpuLoad : cpuUsage), true)
                    .addField("\u200B", "\u200B", true)
                    .addField("💾 JVM RAM Max", jvmRamTotal, true)
                    .addField("📊 JVM RAM Used", jvmRamUsed, true)
                    .addField("📉 JVM RAM Usage", String.format("%.2f%%", memUsagePercent), true);

            // Add system memory if available
            if (systemTotalMem > 0) {
                long systemUsedMem = systemTotalMem - systemFreeMem;
                double systemMemUsagePercent = ((double) systemUsedMem / systemTotalMem) * 100;
                embed.addField("🖧 System RAM", formatBytes(systemUsedMem) + " / " + systemRamTotal +
                        String.format(" (%.1f%%)", systemMemUsagePercent), false);
            }

            embed.addField("⏱️ Uptime", days + "d " + hours + "h " + minutes + "m", false)
                    .addField("🖧 Platform", osName + " " + osArch, true)
                    .addField("🏷️ Hostname", hostname, true)
                    .addField("☕ Java Version", System.getProperty("java.version"), true)
                    .setTimestamp(Instant.now())
                    .setFooter("Stats Server Hosting");

            interaction.replyEmbeds(embed.build()).queue();

        } catch (Exception error) {
            System.err.println("Serverstats command error: " + error.getMessage());
            error.printStackTrace();
            interaction.reply("❌ Failed to fetch server stats: " + error.getMessage())
                    .setEphemeral(true).queue();
        }
    }

    private static String formatBytes(long bytes) {
        double gb = bytes / (1024.0 * 1024.0 * 1024.0);
        if (gb >= 1) {
            return String.format("%.2f GB", gb);
        }
        double mb = bytes / (1024.0 * 1024.0);
        return String.format("%.2f MB", mb);
    }
}
