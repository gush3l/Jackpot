package com.technovision.jackpot.system;

import com.technovision.jackpot.Jackpot;
import com.technovision.jackpot.messages.MessageHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

import static com.technovision.jackpot.Jackpot.PLUGIN;

public class Timer extends BukkitRunnable {

    public static int originalCountdown;
    public static int countdown;
    public static List<Integer> warnings;

    public Timer() {
        originalCountdown = MessageHandler.getJackpotValue("draw-time");
        countdown = originalCountdown;
        warnings = Jackpot.PLUGIN.getConfig().getIntegerList("jackpot.warnings");
    }

    @Override
    public void run() {
        if (warnings.contains(countdown)) {
            List<String> msg = MessageHandler.parseWarning();
            for (String line : msg) {
                Bukkit.broadcastMessage(line);
            }
        }
        if (countdown == 0) {
            Jackpot.JACKPOT.awardWinner();
            countdown = originalCountdown;
        }
        countdown--;
    }

    public String getTime() {
        return formatMilliSecondsToTime((long) countdown * 1000);
    }

    public String getShortTime() {
        long milliseconds = (long) countdown * 1000;
        int seconds = (int) (milliseconds / 1000);
        if (seconds >= 3600) {
            int hours = (int) ((milliseconds / (1000 * 60 * 60)) % 24);
            int minutes = (int) ((milliseconds / (1000 * 60)) % 60);
            if (minutes == 0) {
                return hours + "h";
            }
            return hours + "h, " + minutes + "m";
        } else if (seconds > 60) {
            int minutes = (int) ((milliseconds / (1000 * 60)) % 60);
            int sec = (int) (milliseconds / 1000) % 60;
            if (sec == 0) {
                return minutes + "m";
            }
            return minutes + "m, " + sec + "s";
        }
        return seconds + "s";
    }

    private String formatMilliSecondsToTime(long milliseconds) {
        int seconds = (int) (milliseconds / 1000) % 60;
        int minutes = (int) ((milliseconds / (1000 * 60)) % 60);
        int hours = (int) ((milliseconds / (1000 * 60 * 60)) % 24);
        return hours + "h, " + minutes + "m, " + seconds + "s";
    }
}
