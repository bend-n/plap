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
            case 6: return "[accent]<[gold]\uF85F" + ((xp / 5000) % 3 + 1) + "[accent]>";
        }
        return "[accent]<[green]Horde slayer[accent]>[white]";
    }
}
