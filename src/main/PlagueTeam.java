package main;

import mindustry.game.Team;

import java.util.ArrayList;
import java.util.List;

public class PlagueTeam {
    protected Team team;
    protected ArrayList<CustomPlayer> players = new ArrayList<>();


    public PlagueTeam(Team team) {
        this.team = team;
    }

    public void addPlayer(CustomPlayer ply){
        players.add(ply);
    }

    public void removePlayer(CustomPlayer ply) {players.remove(ply);}

    public boolean hasPlayer(CustomPlayer ply) {return players.contains(ply);}

}
