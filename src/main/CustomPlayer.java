package main;


import mindustry.gen.Player;

public class CustomPlayer {

    protected Player player;
    public boolean connected;
    public String rawName;
    public int startingXP;

    public CustomPlayer(Player player, int xp){
        this.player = player;
        this.connected = true;
        this.rawName = player.name;
        this.startingXP = xp;
    }



}
