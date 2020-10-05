package PlaguePlugin1;

import mindustry.entities.type.Player;
import mindustry.game.Team;

import java.util.ArrayList;
import java.util.List;

public class PlagueTeam {
    protected Team team;
    protected List<CustomPlayer> players = new ArrayList<>();


    public PlagueTeam(Team team) {
        this.team = team;
    }

    public void addPlayer(CustomPlayer ply){
        players.add(ply);
    }

    public void removePlayer(CustomPlayer ply) {players.remove(ply);}

}
