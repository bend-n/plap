package main;


import arc.struct.Seq;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.gen.Unit;

import java.util.HashMap;
import java.util.Map;

public class CustomPlayer {

    protected Player player;
    public String rawName;
    public Team team;
    public int xp;
    public int prestige;
    public int monthWins;
    public int wins;
    public boolean hudEnabled = true;

    public boolean wantsToPrestige = false;

    public Seq<Unit> followers = new Seq<>();

    private Map<Team, String> colorMapping = new HashMap<Team, String>()
    {{
        put(Team.purple, "[scarlet]");
        put(Team.blue, "[royal]");
    }};

    public int buildScore = 0;

    public boolean connected = true;

    public CustomPlayer(Player player){
        this.player = player;
        this.rawName = player.name;
    }

    public void updateName(){
        player.name = StringHandler.determinePrestige(prestige) + StringHandler.determineRank(xp) +
                colorMapping.getOrDefault(player.team(), "[olive]") + " " + rawName;
    }

    public void addXP(int add, String message){
        if(!connected){
            return;
        }
        if(buildScore < 15000){
            player.sendMessage("[accent]You must contribute more to receive XP!");
            return;
        }
        xp += add;
        player.sendMessage(message);
        if((xp - add)/15000 != xp/15000){
            Call.infoMessage(player.con, "[gold]You ranked up to " + StringHandler.determineRank(xp) + "[gold]!");
        }
        updateName();
    }

    public int rank(){
        int t = Math.min(xp/15000, 8);
        return t;
    }
}
