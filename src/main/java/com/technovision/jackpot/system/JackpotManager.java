package com.technovision.jackpot.system;

import com.technovision.jackpot.Jackpot;
import com.technovision.jackpot.Utils;
import com.technovision.jackpot.gui.ConfirmGUI;
import com.technovision.jackpot.messages.MessageHandler;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

import static com.technovision.jackpot.Jackpot.*;

public class JackpotManager implements CommandExecutor {

    public static LotteryBag<UUID> JACKPOT;
    public static long MONEY;
    public static long TOTAL_TICKETS;
    public static HashMap<String, Long> TICKETS;

    public JackpotManager() {
        MONEY = 0;
        TOTAL_TICKETS = 0;
        JACKPOT = new LotteryBag<>();
        TICKETS = new HashMap<>();
        PLUGIN.getCommand("jackpot").setExecutor(this);
    }

    public void awardWinner() {
        if (!JACKPOT.isEmpty()) {
            long prize = (long) (MONEY - (MONEY * MessageHandler.getJackpotDouble("tax-percent")));
            String prizeString = FORMATTER.format(prize);
            OfflinePlayer player = Bukkit.getOfflinePlayer(JACKPOT.getRandom());
            List<String> msg = MessageHandler.parseResults(prizeString, player.getName());
            for (String line : msg) {
                Bukkit.broadcastMessage(line);
            }
            if (PLUGIN.getConfig().getBoolean("title.enable")) {
                String title = PLUGIN.getConfig().getString("title.title")
                        .replace("{winner}",player.getName())
                        .replace("{prize}",prizeString);
                String subtitle = PLUGIN.getConfig().getString("title.subtitle")
                        .replace("{winner}",player.getName())
                        .replace("{prize}",prizeString);
                for (Player titlep : Bukkit.getOnlinePlayers()) {
                    titlep.sendTitle(MessageHandler.translateColors(title), MessageHandler.translateColors(subtitle));
                }
            }
            //Store Jackpot History Start
            List<String> history = PLUGIN.getConfig().getStringList("storage.jackpot-history");
            String ticketsHist = String.valueOf(TICKETS.get(player.getUniqueId().toString()).intValue());
            String totalTicketsHist = String.valueOf(TOTAL_TICKETS);
            String lastJackpot = Utils.serializeHistory(player.getName(),prizeString,ticketsHist,totalTicketsHist);
            history.add(lastJackpot);
            PLUGIN.getConfig().set("storage.jackpot-history",history);
            //Store Jackpot History End
            ECON.depositPlayer(player, prize);
        }
        MONEY = 0;
        TOTAL_TICKETS = 0;
        TICKETS.clear();
        JACKPOT = new LotteryBag<>();
    }

    public static void enterJackpot(Player player, long amt, long total) {
        ECON.withdrawPlayer(player, total);
        JACKPOT.addEntry(player.getUniqueId(), amt);
        MONEY += total;
        TOTAL_TICKETS += amt;
        if (TICKETS.containsKey(player.getUniqueId().toString())) {
            long oldAmt = TICKETS.get(player.getUniqueId().toString());
            TICKETS.put(player.getUniqueId().toString(), oldAmt + amt);
        } else {
            TICKETS.put(player.getUniqueId().toString(), amt);
        }
        player.sendMessage(MessageHandler.parseBuyMessage("buy-ticket", amt));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            if (cmd.getName().equalsIgnoreCase("jackpot")) {
                Player player = (Player) sender;
                if (args.length > 0) {
                    if (args[0].equalsIgnoreCase("buy") || args[0].equalsIgnoreCase("bet") || args[0].equalsIgnoreCase("place")) {
                        long amt = 1;
                        if (args.length >= 2) {
                            try {
                                amt = Long.parseLong(args[1]);
                                if (amt < 1) { throw new NumberFormatException(); }
                            } catch (NumberFormatException e) {
                                player.sendMessage(MessageHandler.translateColors(PLUGIN.getConfig().getString("messages.unknown-cmd")));
                                return true;
                            }
                        }
                        long total = MessageHandler.getJackpotValue("ticket-price") * amt;
                        if (ECON.getBalance(player) >= total) {
                            if (MessageHandler.getJackpotBoolean("confirm-gui")) {
                                player.openInventory(new ConfirmGUI(amt, total).getInventory());
                            } else {
                                enterJackpot(player, amt, total);
                            }
                        } else {
                            player.sendMessage(MessageHandler.parseBuyMessage("cannot-afford", amt));
                        }
                        return true;
                    }
                    if (args[0].equalsIgnoreCase("reload")) {
                        if (player.hasPermission("jackpot.reload") || player.isOp()) {
                            PLUGIN.reloadConfig();
                            PLUGIN.saveConfig();
                            player.sendMessage(MessageHandler.translateColors(PLUGIN.getConfig().getString("messages.reloaded")));
                        } else {
                            player.sendMessage(MessageHandler.translateColors(PLUGIN.getConfig().getString("messages.no-perm")));
                        }
                        return true;
                    }
                    if (args[0].equalsIgnoreCase("run")) {
                        if (player.hasPermission("jackpot.run") || player.isOp()) {
                            Jackpot.JACKPOT.awardWinner();
                            Timer.countdown = Timer.originalCountdown;
                            player.sendMessage(MessageHandler.translateColors(PLUGIN.getConfig().getString("messages.force-run")));
                        }else {
                            player.sendMessage(MessageHandler.translateColors(PLUGIN.getConfig().getString("messages.no-perm")));
                        }
                        return true;
                    }
                    if (args[0].equalsIgnoreCase("reset")) {
                        if (player.hasPermission("jackpot.reset") || player.isOp()) {
                            Bukkit.getServer().getPluginManager().disablePlugin(PLUGIN);
                            Bukkit.getServer().getPluginManager().enablePlugin(PLUGIN);
                            player.sendMessage(MessageHandler.translateColors(PLUGIN.getConfig().getString("messages.reset")));
                        }else {
                            player.sendMessage(MessageHandler.translateColors(PLUGIN.getConfig().getString("messages.no-perm")));
                        }
                        return true;
                    }
                    if (args[0].equalsIgnoreCase("help")) {
                        if (player.hasPermission("jackpot.help") || player.isOp()) {
                            for (String cs : PLUGIN.getConfig().getStringList("messages.help")){
                                player.sendMessage(MessageHandler.translateColors(cs));
                            }
                        }else {
                            player.sendMessage(MessageHandler.translateColors(PLUGIN.getConfig().getString("messages.no-perm")));
                            return true;
                        }
                    }
                    if (args[0].equalsIgnoreCase("history")) {
                        if (player.hasPermission("jackpot.history") || player.isOp()) {
                            for (String histHead : PLUGIN.getConfig().getStringList("messages.history-header")) {
                                player.sendMessage(MessageHandler.translateColors(histHead));
                            }
                            for (String histEntry : PLUGIN.getConfig().getStringList("storage.jackpot-history")) {
                                Utils.deserializeHistory(histEntry);
                                player.sendMessage(MessageHandler.translateColors(MessageHandler
                                        .historyEntry(Utils.playerNameHistory, Utils.prizeHistory, Utils.ticketsHistory, Utils.totalTicketsHistory)));
                            }
                            for (String histFooter : PLUGIN.getConfig().getStringList("messages.history-footer")) {
                                player.sendMessage(MessageHandler.translateColors(histFooter));
                            }
                        }else {
                            player.sendMessage(MessageHandler.translateColors(PLUGIN.getConfig().getString("messages.no-perm")));
                            return true;
                        }
                        return true;
                    }
                    player.sendMessage(MessageHandler.translateColors(PLUGIN.getConfig().getString("messages.unknown-cmd")));
                    return true;
                } else {
                    long amount = 0;
                    double percent = 0;
                    if (TICKETS.containsKey(player.getUniqueId().toString())) {
                        amount = TICKETS.get(player.getUniqueId().toString());
                        percent = ((double) amount / TOTAL_TICKETS) * 100;
                    }
                    String tax = String.valueOf((int) (MessageHandler.getJackpotDouble("tax-percent") * 100));
                    List<String> msg = MessageHandler.parseInfo(
                            FORMATTER.format(MONEY), tax, TOTAL_TICKETS, amount, (int) percent);
                    for (String line : msg) {
                        player.sendMessage(line);
                    }
                }
            }
        }
        return true;
    }
}
