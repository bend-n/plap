package main;

public class StringHandler {
    public static String determineRank(int xp){
        switch(xp / 15000){
            case 0: return "[accent]<[white]\uF861" + ((xp / 5000) % 3 + 1) + "[accent]>";
            case 1: return "[accent]<[white]\uF85B" + ((xp / 5000) % 3 + 1) + "[accent]>";
            case 2: return "[accent]<[white]\uF85C" + ((xp / 5000) % 3 + 1) + "[accent]>";
            case 3: return "[accent]<[white]\uF858" + ((xp / 5000) % 3 + 1) + "[accent]>";
            case 4: return "[accent]<[white]\uF857" + ((xp / 5000) % 3 + 1) + "[accent]>";
            case 5: return "[accent]<[white]\uF856" + ((xp / 5000) % 3 + 1) + "[accent]>";
            case 6: return "[accent]<[white]\uF7BE" + ((xp / 5000) % 3 + 1) + "[accent]>";
            case 7: return "[accent]<[gold]\uF85F" + ((xp / 5000) % 3 + 1) + "[accent]>";
        }
        switch(xp / 1000000){
            case 0: return "[accent]<[green]Horde\u00A0Slayer[accent]>[white]";
            case 1: return "[accent]<[green]Horde\u00A0[gold]Eradicator[accent]>[white]";
        }
        return "[accent]<[gray]No\u00A0Life[accent]>[white]";
    }

    public static String determinePrestige(int prestige){
        return prestige > 0 ? "\uF82C[gold]" + prestige : "";
    }
}
