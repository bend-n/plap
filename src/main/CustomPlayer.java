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
    public int controlledT5;
    public int bannedT5 = -1000;

    private Map<Team, String> colorMapping = new HashMap<Team, String>()
    {{
        put(Team.purple, "[scarlet]");
        put(Team.blue, "[royal]");
    }};

    public int plagueBuildScore = 0, survivorBuildScore = 0;

    public boolean connected = true;

    public CustomPlayer(Player player){
        this.player = player;
        this.rawName = player.name;
    }

    public void reset(){
        plagueBuildScore = 0;
        survivorBuildScore = 0;
        player.name = rawName;
        player.team(Team.blue);
    }

    public void updateName(){
        player.name = StringHandler.determinePrestige(prestige) + StringHandler.determineRank(xp) +
                colorMapping.getOrDefault(player.team(), "[olive]") + "\u00A0" + rawName;
    }

    public void addXP(int add, boolean plagueXp, String message){
        if(!connected){
            return;
        }
        if(plagueXp){
            if (plagueBuildScore < 7500){
                player.sendMessage("[accent]You must contribute more to [scarlet]Plague[accent] to receive XP!" +
                        " (" + plagueBuildScore + "/7500)");
                return;
            }
        }else{
            if (survivorBuildScore < 7500){
                player.sendMessage("[accent]You must contribute more to [olive]Survivors[accent] to receive XP!" +
                        " (" + survivorBuildScore + "/7500)");
                return;
            }
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
