package main;


import mindustry.game.Team;
import mindustry.gen.Player;

public class CustomPlayer {

    protected Player player;
    public String rawName;
    public Team team;
    public int xp;
    public int monthWins;
    public int wins;
    public boolean hudEnabled = true;

    public boolean connected = true;

    public CustomPlayer(Player player){
        this.player = player;
        this.rawName = player.name;
    }
}
