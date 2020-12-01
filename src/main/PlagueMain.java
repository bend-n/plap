package main;

import arc.*;
import arc.func.Boolf;
import arc.func.Cons;
import arc.math.Mathf;
import arc.net.Server;
import arc.struct.Seq;
import arc.util.*;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.core.GameState;
import mindustry.core.Version;
import mindustry.entities.bullet.BulletType;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.Plugin;
import mindustry.net.*;
import mindustry.net.Net;
import mindustry.type.ItemStack;
import mindustry.type.Weapon;
import mindustry.world.Block;
import mindustry.world.Build;
import mindustry.world.Tile;
import mindustry.world.Tiles;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.blocks.units.UnitFactory;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.prefs.Preferences;

import static mindustry.Vars.*;

public class PlagueMain extends Plugin {

    private int teamsCount = 0;

    private final Rules rules = new Rules();

    private HashMap<String, CustomPlayer> uuidMapping = new HashMap<>();
    private HashMap<Team, PlagueTeam> teams = new HashMap<>();

    private Map<Team, String> colorMapping = new HashMap<Team, String>()
    {{
        put(Team.crux, "[scarlet]");
        put(Team.blue, "[royal]");
    }};

    private final static int corePlaceTime = 60 * 20, damageMultiplyTime = 60 * 60 * 30, secondTime = 60,
            tenMinTime = 60 * 60 * 10;
    private final static int timerCorePlace = 0, timerDamageMultiply = 1, timerSecond = 2, timerTenMin = 3;
    private Interval interval = new Interval(10);

    private float realTime = 0f;
    private int seconds = 0;
    private int record;
    private boolean newRecord = false;

    private float multiplier = 1;

    private int[] plagueCore = new int[2];

    private mindustry.maps.Map loadedMap;

    private int currMap;
    private String mapID = "0";

    private Preferences prefs;

    private final DBInterface mapDB = new DBInterface("map_data");
    private final DBInterface playerDB = new DBInterface("player_data");

    private boolean pregame = true;
    private boolean survivedToPoint = false;


    @Override
    public void init(){
        mapDB.connect("data/server_data.db");
        playerDB.connect(mapDB.conn);

        initRules();

        netServer.assigner = (player, players) -> {
            if(uuidMapping.containsKey(player.uuid())){
                Team team = uuidMapping.get(player.uuid()).player.team();
                if(team == Team.blue && !pregame) return Team.crux;
                return team;
            }
            if(pregame){
                return Team.blue;
            }else{
                return Team.crux;
            }
        };

        netServer.admins.addActionFilter((action) -> {
            if(action.player != null && action.block != null && action.block == Blocks.airFactory
                && (int) action.config == 0){
                return false;
            }

            if(action.player != null && action.tile != null){
                if(cartesianDistance(action.tile.x, action.tile.y,
                        plagueCore[0], plagueCore[1]) < world.height()/4){
                    if(action.player.team() != Team.crux) return false;
                }
                if(action.tile.block() == Blocks.powerSource){
                    return false;
                }

                if(action.block != null && PlagueData.survivorBanned.contains(action.block)
                        && action.player.team() != Team.crux && action.player.team() != Team.blue){
                    return false;
                }
                if(action.block != null && PlagueData.plagueBanned.contains(action.block)
                        && action.player.team() == Team.crux){
                    return false;
                }
                if(action.block != null && action.block == Blocks.commandCenter
                        && action.player.playTime < 1000){
                    action.player.sendMessage("[accent]You must have [scarlet]1000 [accent]minutes of playtime " +
                            "to use a Command center!");
                    return false;
                }



                CoreBlock.CoreBuild closestCore = state.teams.closestCore(action.tile.x, action.tile.y, action.player.team());
                if(closestCore != null && action.block != null){
                    float cx = closestCore.block.size % 2 == 0 ? (float) (closestCore.tile.x + 0.5) : closestCore.tile.x;
                    float cy = closestCore.block.size % 2 == 0 ? (float) (closestCore.tile.y + 0.5) : closestCore.tile.y;
                    if(cartesianDistance(action.tile.x, action.tile.y, cx, cy) < 7
                            && action.block == Blocks.unloader && action.player.team() == Team.crux) {
                        return false;
                    }
                }
            }

            return true;
        });

        netServer.admins.addChatFilter((player, text) ->{
            String col = colorMapping.getOrDefault(player.team(), "[olive]");
            String message = col + "[[" + player.name + col + "]: [white]" + text;
            Call.sendMessage(message);
            Log.info(message);
            return null;
        });
        int[] counts = {0};
        Events.on(EventType.Trigger.class, event ->{

            if(interval.get(timerDamageMultiply, damageMultiplyTime)){
                survivedToPoint = true;
                for(Team t : teams.keySet()){
                    if(t != Team.crux){
                        for(CustomPlayer cPly : teams.get(t).players){
                            if(cPly.connected){
                                cPly.player.sendMessage("[orange]You survived long enough to gain a point!");
                                playerDB.safePut(cPly.player.uuid(), "monthWins", (int) playerDB.safeGet(cPly.player.uuid(), "monthWins") + 1);
                                playerDB.safePut(cPly.player.uuid(), "allWins", (int) playerDB.safeGet(cPly.player.uuid(), "allWins") + 1);
                            }
                        }
                    }
                }
            }
            if(counts[0] < 6 && interval.get(timerCorePlace, corePlaceTime)){
                counts[0] ++;
                if(counts[0] == 6){
                    pregame = false;
                    for(Player ply : Groups.player){
                        if(ply.team() == Team.blue){
                            infect(uuidMapping.get(ply.uuid()), true);
                        }
                    }
                    if(teams.size() == 1){
                        endgame(new Seq<>());
                    }

                }else{
                    Call.sendMessage("[accent]You have [scarlet]" + (120 - counts[0]*20) +
                            " [accent]seconds left to place a core. Place any block to place a core.");
                }
            }

            realTime += Time.delta;
            seconds = (int) (realTime / 60);

            if(seconds > record && !newRecord){
                Call.sendMessage("[gold]New survivor record!");
                newRecord = true;
            }


            if(interval.get(timerTenMin, tenMinTime)){
                multiplier *= 1.05;
                state.rules.unitDamageMultiplier *= 1.05;
                state.rules.unitHealthMultiplier *= 1.05;
                Call.sendMessage("[accent]Units now deal [scarlet]5%[accent] more damage and have [scarlet]5%[accent] more health");
                for(Team t : teams.keySet()){
                    if(t != Team.crux){
                        for(CustomPlayer cPly : teams.get(t).players){
                            if(cPly.connected){
                                int addXp = 100 * (cPly.player.donateLevel + 1);
                                cPly.player.sendMessage("[accent]+[scarlet]" + addXp + "xp[accent] for surviving 10 minutes");
                                playerDB.safePut(cPly.player.uuid(),"xp", (int) playerDB.safeGet(cPly.player.uuid(),"xp") + addXp);
                            }
                        }
                    }
                }
            }

        });


        Events.on(EventType.PlayerJoinSecondary.class, event ->{
            if(!playerDB.hasRow(event.player.uuid())){
                Log.info("New player, adding to local tables...");
                playerDB.addRow(event.player.uuid());
            }

            playerDB.loadRow(event.player.uuid());


            if(!uuidMapping.containsKey(event.player.uuid())){
                uuidMapping.put(event.player.uuid(), new CustomPlayer(event.player, (int) playerDB.safeGet(event.player.uuid(),"xp") ));
            }
            CustomPlayer cPly = uuidMapping.get(event.player.uuid());
            cPly.player = event.player;

            playerDB.safePut(event.player.uuid(),"latestName", StringHandler.determineRank(cPly.startingXP) + event.player.name);

            updateName(event.player);





            if(event.player.team() == Team.blue){
                // event.player.respawn(world.getTiles()[plagueCore[0]][plagueCore[1]]);
                CoreBlock.playerSpawn(world.tile(plagueCore[0], plagueCore[1]), event.player);
                // Call.setPosition(event.player.con, plagueCore[0], plagueCore[1]);
            }

            cPly.connected = true;

            event.player.sendMessage(leaderboard(5));
        });

        Events.on(EventType.PlayerLeave.class, event -> {
            CustomPlayer cPly = uuidMapping.get(event.player.uuid());
            cPly.connected = false;
            savePlayerData(event.player.uuid());
        });

        Events.on(EventType.BuildSelectEvent.class, event ->{
            Player player = event.builder.getPlayer();
            if(player == null) return;

            if(event.team == Team.blue){
                event.tile.removeNet();
                if(Build.validPlace(Blocks.spectre, event.team, event.tile.x, event.tile.y, 0) && !event.breaking){
                    Team chosenTeam = null;
                    for(Teams.TeamData t : state.teams.getActive()){
                        if(t.team != Team.crux){
                            for(CoreBlock.CoreBuild core : t.cores){
                                if(cartesianDistance(event.tile.x, event.tile.y, core.tile.x, core.tile.y) < 100){
                                    chosenTeam = t.team;
                                    break;
                                }
                            }
                            if(chosenTeam != null){
                                break;
                            }
                        }
                    }

                    if(chosenTeam == null){
                        teamsCount++;
                        chosenTeam = Team.all[teamsCount+6];
                        teams.put(chosenTeam, new PlagueTeam(event.team));
                    }

                    teams.get(chosenTeam).addPlayer(uuidMapping.get(player.uuid()));

                    player.team(chosenTeam);
                    updateName(player);

                    event.tile.setNet(Blocks.coreFoundation, chosenTeam, 0);
                    state.teams.registerCore((CoreBlock.CoreBuild) event.tile.build);
                    if (state.teams.cores(chosenTeam).size == 1){
                        for(ItemStack stack : PlagueData.survivorLoadout){
                            // Call.transferItemTo(stack.item, stack.amount, event.tile.drawx(), event.tile.drawy(), event.tile);
                            Call.setItem(event.tile.build, stack.item, stack.amount);
                        }
                    }

                    /*player.setDead(true);
                    player.onRespawn(state.teams.cores(chosenTeam).get(0).tile);*/


                }
            }
        });

        Events.on(EventType.BlockDestroyEvent.class, event -> {
            if(event.tile.block() instanceof CoreBlock && event.tile.team().cores().size == 1){
                Team deadTeam = event.tile.team();
                Seq<CustomPlayer> winners = new Seq<CustomPlayer>();
                for(CustomPlayer cPly : teams.get(deadTeam).players){
                    if(teams.size() == 2 && cPly.connected){
                        winners.add(cPly);
                    }
                    infect(cPly, false);
                }

                killTiles(deadTeam);

                for(CustomPlayer cPly : teams.get(Team.crux).players){
                    if(cPly.connected){
                        int addXp = 100 * (cPly.player.donateLevel + 1);
                        cPly.player.sendMessage("[accent]+[scarlet]" + addXp + "xp[accent] for infecting survivors");
                        playerDB.safePut(cPly.player.uuid(),"xp", (int) playerDB.safeGet(cPly.player.uuid(),"xp") + addXp);
                    }
                }

                teams.remove(deadTeam);
                if(teams.size() == 1){
                    endgame(winners);
                }
            }
        });

        Events.on(EventType.UnitDestroyEvent.class, event ->{
            /*if(event.unit instanceof Player && event.unit.team() == Team.blue){
                event.unit.onRespawn(world.getTiles()[plagueCore[0]][plagueCore[1]]);
                Call.onPositionSet(((Player) event.unit).con, plagueCore[0], plagueCore[1]);
            }*/
            if(event.unit.getPlayer() != null && event.unit.team() == Team.blue){
                CoreBlock.playerSpawn(world.tile(plagueCore[0], plagueCore[1]), event.unit.getPlayer());
            }

        });

        Events.on(EventType.TapEvent.class, event ->{
            if(event.tile.block() == Blocks.vault && event.tile.team() != Team.crux){
                if(event.tile.build.items.has(Items.thorium, 997)){
                    event.tile.build.tile.setNet(Blocks.coreShard, event.tile.team(), 0);
                }
            }
        });
    }

    @Override
    public void registerServerCommands(CommandHandler handler){
        handler.register("plague", "[map]", "Host the plague game mode", args -> {
            if (!Vars.state.is(GameState.State.menu)) {
                Log.err("Stop the server first.");
                return;
            }

            prefs = Preferences.userRoot().node(this.getClass().getName());
            currMap = prefs.getInt("mapchoice",0);
            Log.info("0: Patient Zero");
            int i = 1;
            for(mindustry.maps.Map map : maps.customMaps()){
                Log.info(i + ": " + map.name());
                i += 1;
            }

            if(args.length != 0){
                currMap = Integer.parseInt(args[0]);
            }
            logic.reset();
            Blocks.powerSource.health = Integer.MAX_VALUE;
            if(currMap == 0){
                PlagueGenerator generator = new PlagueGenerator();
                world.loadGenerator(601, 601, generator::generate);
            }else{
                mindustry.maps.Map map = maps.customMaps().get(currMap-1);
                world.loadMap(map);
            }


            loadedMap = state.map;

            Tile tile = state.teams.cores(Team.crux).get(0).tile;
            plagueCore[0] = tile.x;
            plagueCore[1] = tile.y;
            world.beginMapLoad();
            PlagueGenerator.inverseFloodFill(world.tiles, plagueCore[0], plagueCore[1]);
            PlagueGenerator.defaultOres(world.tiles);
            tile.build.indestructible = true;
            world.endMapLoad();





            Log.info("Map " + loadedMap.name() + " loaded");

            state.rules = rules.copy();
            logic.play();

            netServer.openServer();

            prefs.putInt("mapchoice", currMap);
            mapID = currMap == 0 ? String.valueOf(currMap) : loadedMap.file.name().split("_")[0];
            if(!mapDB.hasRow(mapID)){
                mapDB.addRow(mapID);
            }
            mapDB.loadRow(mapID);
            checkExpiration();
            record = (int) mapDB.safeGet(mapID, "survivorRecord");

            tile.build.items.clear();
            /*for(ItemStack stack : PlagueData.plagueLoadout){
                Call.transferItemTo(stack.item, stack.amount, tile.drawx(), tile.drawy(), tile);
            }*/

            teams.put(Team.crux, new PlagueTeam(Team.crux));
        });

        handler.register("setxp", "<uuid> <xp>", "Set the xp of a player", args -> {
            int newXp;
            try{
                newXp = Integer.parseInt(args[1]);
            }catch(NumberFormatException e){
                Log.info("Invalid xp input '" + args[1] + "'");
                return;
            }

            if(!playerDB.entries.containsKey(args[0])){
                playerDB.loadRow(args[0]);
                playerDB.safePut(args[0],"xp", newXp);
                playerDB.saveRow(args[0]);
            }else{
                playerDB.safePut(args[0],"xp", newXp);
            }
            Log.info("Set uuid " + args[0] + " to have xp of " + args[1]);

        });

        handler.register("manual_reset", "Perform a manual reset of xp and points", args -> {
            rankReset();
            winsReset();
            mapDB.safePut(mapID, "survivorRecord", 0);
            mapDB.safePut(mapID, "avgSurvived", 0);
            mapDB.safePut(mapID, "plays", 0);
            Log.info("ranks, monthly wins and monthly data have been reset");
        });


    }

    @Override
    public void registerClientCommands(CommandHandler handler){
        handler.<Player>register("infect", "Infect yourself", (args, player) -> {
            if(player.team() == Team.crux){
                player.sendMessage("[accent]Already infected!");
                return;
            }
            CustomPlayer cPly = uuidMapping.get(player.uuid());
            infect(cPly, true);
        });

        handler.<Player>register("xp", "Show your xp", (args, player) -> {
            int xp = (int) playerDB.safeGet(player.uuid(),"xp");
            String nextRank = StringHandler.determineRank(xp+5000);
            player.sendMessage("[scarlet]" + xp + "[accent] xp\nReach [scarlet]" + (xp/5000+1)*5000 + "[accent] xp to reach " + nextRank + "[accent] rank.");
        });

        handler.<Player>register("stats", "Display stats about the current map", (args, player) -> {
            int avgTime = (int) mapDB.safeGet(mapID, "avgSurvived");
            int recordTime = (int) mapDB.safeGet(mapID, "survivorRecord");
            String s = "[accent]Map stats for: [white]" + state.map.name() + "\n" +
                    "[accent]Author: [white]" + state.map.author() + "\n" +
                    "[accent]Plays: [scarlet]" + mapDB.safeGet(mapID, "plays") + "\n" +
                    "[accent]Average time survived: [scarlet]" + avgTime/60 + "[accent] minutes and [scarlet]" + avgTime % 60 + "[accent] seconds.\n" +
                    "[accent]Suvivor record: [scarlet]" + recordTime/60 + "[accent] minutes and [scarlet]" + recordTime % 60 + "[accent] seconds.";
            player.sendMessage(s);

        });

        handler.<Player>register("time", "Display the time now", (args, player) -> {
            String s = "[accent]Time: [scarlet]" + seconds/60 + "[accent] minutes and [scarlet]"
                    + seconds % 60 + "[accent] seconds.";
            player.sendMessage(s);
        });

        handler.<Player>register("leaderboard", "Display the leaderboard", (args, player) -> {
            player.sendMessage(leaderboard(5));
        });

        handler.<Player>register("points", "Display your current points", (args, player) -> {
            int points = (int) playerDB.safeGet(player.uuid(), "monthWins");
            player.sendMessage("[scarlet]" + points + "[accent] points.");
        });

        handler.<Player>register("info", "Display info about the current game", (args, player) -> {
            player.sendMessage("[#4d004d]{[purple]AA[#4d004d]} [olive]Plague[accent] is a survival game mode with two teams," +
                    " the [scarlet]Plague [accent]and [green]Survivors[accent].\n\n" +
                    "The [scarlet]Plague[accent] build up their economy and make the biggest army possible, and try to" +
                    " break through the [green]Survivors[accent] defenses.\n\n" +
                    "The [green]Survivors[accent] build up a huge defense and last as long as possible. To become a " +
                    "[green]Survivor[accent], you must place a core in the first 2 minutes of the game, where you are " +
                    " allowed to choose your team. Place any block to place a core at the start of the game.");
        });
    }

    void initRules(){

        rules.canGameOver = false;
        // rules.playerDamageMultiplier = 0;
        rules.buildSpeedMultiplier = 2;

        UnitTypes.alpha.weapons = new Seq<>();
        UnitTypes.beta.weapons = new Seq<>();
        UnitTypes.gamma.weapons = new Seq<>();

        rules.fire = false;
        rules.modeName = "Plague";

        /*Block dagger = Vars.content.blocks().find(block -> block.name.equals("dagger-factory"));
        ((UnitFactory)(dagger)).maxSpawn = 1;

        UnitTypes.dagger.health *= 2;
        UnitTypes.dagger.weapon = PlagueData.daggerWepaon;

        Block titan = Vars.content.blocks().find(block -> block.name.equals("titan-factory"));
        ((UnitFactory)(titan)).maxSpawn = 1;

        UnitTypes.titan.health *= 2;
        UnitTypes.titan.weapon = PlagueData.titanWepaon;

        Block fortress = Vars.content.blocks().find(block -> block.name.equals("fortress-factory"));
        ((UnitFactory)(fortress)).maxSpawn = 1;

        UnitTypes.fortress.health *= 1.5;
        UnitTypes.fortress.weapon = PlagueData.fortressWepaon;*/

    }

    String leaderboard(int limit){
        ResultSet rs = playerDB.customQuery("select * from player_data order by monthWins desc limit " + limit);
        String s = "[accent]Leaderboard:\n";
        try{
            int i = 0;
            while(rs.next()){
                i ++;
                s += "\n[gold]" + (i) + "[white]:" + rs.getString("latestName") + "[accent]: [gold]" + rs.getString("monthWins") + "[accent] points";
            }
            rs.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return s;
    }

    void infect(CustomPlayer cPly, boolean remove){
        if(cPly.player.team() != Team.blue && remove){
            PlagueTeam cTeam = teams.get(cPly.player.team());
            if(cTeam.players.size() <= 1) killTiles(cPly.player.team());
            cTeam.removePlayer(cPly);
        }
        Call.sendMessage("[accent]" + cPly.player.name + "[white] was [red]infected[white]!");
        teams.get(Team.crux).addPlayer(cPly);


        if(cPly.connected){

            cPly.player.team(Team.crux);
            cPly.player.clearUnit();
            updateName(cPly.player);
        }

    }

    void killTiles(Team team){
        for(int x = 0; x < world.width(); x++){
            for(int y = 0; y < world.height(); y++){
                Tile tile = world.tile(x, y);
                if(tile.build != null && tile.team() == team){
                    Time.run(Mathf.random(60f * 6), tile.build::kill);
                }
            }
        }
    }

    private void updateName(Player ply){

        if(ply.team() == Team.crux){
            Rules tempRules = rules.copy();
            tempRules.bannedBlocks = PlagueData.plagueBanned;
            Call.setRules(ply.con, tempRules);
        }else if (ply.team() != Team.blue){
            Rules tempRules = rules.copy();
            tempRules.bannedBlocks = PlagueData.survivorBanned;
            Call.setRules(ply.con, tempRules);
        }


        CustomPlayer cPly = uuidMapping.get(ply.uuid());
        ply.name = StringHandler.determineRank(cPly.startingXP) +
                colorMapping.getOrDefault(ply.team(), "[olive]") +
                (ply.donateLevel != 0 ? cPly.rawName : Strings.stripColors(cPly.rawName));
    }

    private float cartesianDistance(float x, float y, float cx, float cy){
        return (float) Math.sqrt(Math.pow(x - cx, 2) + Math.pow(y - cy, 2) );
    }

    void savePlayerData(String uuid){
        if(!playerDB.entries.containsKey(uuid)){
            if(uuidMapping.containsKey(uuid)){
                Log.info(uuid + " data already saved!");
            }else{
                Log.info(uuid + " does not exist in player object or data!");
            }

            return;
        }
        Log.info("Saving " + uuid + " data...");
        CustomPlayer ply = uuidMapping.get(uuid);
        playerDB.saveRow(uuid);
    }

    void endgame(Seq<CustomPlayer> winners){

        int timeNow = seconds;
        for(CustomPlayer cPly : winners){
            Call.infoMessage(cPly.player.con, "[green]Congratulations! You survived the longest\n" +
                    (newRecord ? "    [gold]New record!\n" : "") +
                    "[accent]Survive time: [scarlet]" + timeNow/60 + "[accent] minutes and [scarlet]" +
                    timeNow % 60 + "[accent] seconds.");
            int addXp = 500 * (cPly.player.donateLevel + 1);
            cPly.player.sendMessage("[accent]+[scarlet]" + addXp + "xp[accent] for surviving the longest");
            if(newRecord){
                addXp = 500 * (cPly.player.donateLevel + 1);
                cPly.player.sendMessage("[accent]+[scarlet]" + addXp + "xp[accent] for setting a record");
            }

            playerDB.safePut(cPly.player.uuid(),"xp", (int) playerDB.safeGet(cPly.player.uuid(),"xp") + addXp);

        }

        for(CustomPlayer cPly : teams.get(Team.crux).players){
            if(!winners.contains(cPly)){
                Call.infoMessage(cPly.player.con, "[accent]Game over!\nAll survivors have been infected. Loading new map...");
            }
        }

        if(timeNow > 60 * 5){
            int plays = (int) mapDB.safeGet(mapID, "plays") + 1;
            int rolling = ((int) mapDB.safeGet(mapID, "avgSurvived")*(plays-1) + seconds)/plays;
            mapDB.safePut(mapID, "avgSurvived", rolling);
            mapDB.safePut(mapID, "plays", plays);
        }

        if(newRecord){
            mapDB.safePut(mapID, "survivorRecord", seconds);
        }
        prefs.putInt("mapchoice", PlagueData.getRandomWithExclusion(0, maps.customMaps().size, currMap));


        Time.runTask(60f * 10f, () -> {

            for(Player player : Groups.player) {
                Call.connect(player.con, "aamindustry.play.ai", 6567);
            }
            mapDB.saveRow(mapID);


            // in case any was missed (give a delay so players all leave)
            Time.runTask(60f * 1, () -> {
                for(Object uuid: playerDB.entries.keySet().toArray().clone()){
                    savePlayerData((String) uuid);
                }
            });


            // I shouldn't need this, all players should be gone since I connected them to hub
            // netServer.kickAll(KickReason.serverRestarting);
            Log.info("Game ended successfully.");
            Time.runTask(60f*2, () -> System.exit(2));
        });

    }

    // Long term stuff

    void checkExpiration(){
        prefs = Preferences.userRoot().node(this.getClass().getName());


        int prevMonth = prefs.getInt("month", 1);
        int currMonth = Calendar.getInstance().get(Calendar.MONTH);

        if(prevMonth != currMonth){
            rankReset();
            winsReset();
            mapDB.safePut(mapID, "survivorRecord", 0);
            mapDB.safePut(mapID, "avgSurvived", 0);
            mapDB.safePut(mapID, "plays", 0);
            Log.info("New month - ranks, monthly wins and monthly data are reset automatically...");
        }
        prefs.putInt("month", currMonth);

    }


    void rankReset(){
        // Reset ranks
        playerDB.setColumn("xp", 0);

        for(Object uuid: playerDB.entries.keySet().toArray()){
            playerDB.safePut((String) uuid,"xp", 0);
        }
    }

    void winsReset(){
        playerDB.setColumn("monthWins", 0);

        for(Object uuid: playerDB.entries.keySet().toArray()){
            playerDB.safePut((String) uuid,"monthWins", 0);
        }
    }

}
