package com.technovision.jackpot;

public class Utils {

    public static String playerNameHistory;
    public static String prizeHistory;
    public static String ticketsHistory;
    public static String totalTicketsHistory;

    public static String serializeHistory(String playerName,String prize,String tickets,String totalTickets){
        return playerName+":"+prize+":"+tickets+":"+totalTickets;
    }

    public static void deserializeHistory(String data){
        String[] string = data.split(":");
        if(string.length != 4) return;
        playerNameHistory = string[0];
        prizeHistory = string[1];
        ticketsHistory = string[2];
        totalTicketsHistory = string[3];
    }
}
