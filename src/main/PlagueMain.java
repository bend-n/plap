package main;

import arc.*;
import arc.func.Boolf;
import arc.func.Cons;
import arc.math.Mathf;
import arc.net.Server;
import arc.struct.Seq;
import arc.util.*;
import com.sun.org.apache.xpath.internal.objects.XObject;
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
import mindustry.type.UnitType;
import mindustry.type.Weapon;
import mindustry.ui.fragments.HintsFragment;
import mindustry.world.Block;
import mindustry.world.Build;
import mindustry.world.Tile;
import mindustry.world.Tiles;
import mindustry.world.blocks.sandbox.PowerSource;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.blocks.units.Reconstructor;
import mindustry.world.blocks.units.UnitFactory;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.prefs.Preferences;

import static mindustry.Vars.*;

public class PlagueMain extends Plugin {

    private int teamsCount = 0;

    private final Rules rules = new Rules();

    private Seq<Weapon> polyWeapons;
    private Seq<Weapon> megaWeapon;
    private Seq<Weapon> quadWeapon;
    private Seq<Weapon> octWeapon;

    private float defaultFlareTime;

    private HashMap<String, CustomPlayer> uuidMapping = new HashMap<>();
    private HashMap<Team, PlagueTeam> teams = new HashMap<>();

    private Map<Team, String> colorMapping = new HashMap<Team, String>()
    {{
        put(Team.purple, "[scarlet]");
        put(Team.blue, "[royal]");
    }};

    private RTInterval corePlaceInterval = new RTInterval(20),
            tenMinInterval = new RTInterval(60 * 10),
            oneMinInterval = new RTInterval(60);

    private int multiplier = 1;
    private static DecimalFormat df = new DecimalFormat("0.00");

    private int winTime = 45; // In minutes

    private float realTime = 0f;
    private int seconds = 0;
    private boolean newRecord = false;

    private int[] plagueCore = new int[2];

    private mindustry.maps.Map loadedMap;

    private int currMap;
    private String mapID = "0";

    private int mapRecord;
    private int avgSurvived;
    private int mapPlays;

    private String leaderboardString;

    private Preferences prefs;

    private final DBInterface db = new DBInterface();

    private boolean pregame = true;
    private boolean gameover = false;
    private boolean hasWon = false;

    private static long startTime = System.currentTimeMillis();


    @Override
    public void init(){
        db.connect("users", "recessive", "8N~hT4=a\"M89Gk6@");

        initRules();
        leaderboardString = _leaderboardInit(5);

        netServer.assigner = (player, players) -> {
            if(uuidMapping.containsKey(player.uuid())){
                Team team = uuidMapping.get(player.uuid()).team;
                if(team == Team.blue && !pregame) return Team.purple;
                return team;
            }
            if(pregame){
                return Team.blue;
            }else{
                return Team.purple;
            }
        };

        netServer.admins.addActionFilter((action) -> {

            if(action.player != null && action.tile != null){
                /*if(cartesianDistance(action.tile.x, action.tile.y,
                        plagueCore[0], plagueCore[1]) < world.height()/4){
                    if(action.player.team() != Team.purple) return false;
                }*/
                if(action.tile.block() == Blocks.powerSource){
                    return false;
                }

                if(action.block != null && PlagueData.survivorBanned.contains(action.block)
                        && action.player.team() != Team.purple && action.player.team() != Team.blue){
                    return false;
                }
                if(action.block != null && PlagueData.plagueBanned.contains(action.block)
                        && action.player.team() == Team.purple){
                    return false;
                }

                /*if(action.block != null && action.block == Blocks.commandCenter
                        && action.player.playTime < 1000){
                    action.player.sendMessage("[accent]You must have [scarlet]1000 [accent]minutes of playtime " +
                            "to use a Command center!");
                    return false;
                }*/

            }

            return true;
        });

        int[] counts = {0};
        Events.on(EventType.Trigger.class, event ->{


            // Notification about placing a core, then starting game
            if(counts[0] < 6 && corePlaceInterval.get(seconds)){
                counts[0] ++;
                if(counts[0] == 6){
                    pregame = false;
                    for(Player ply : Groups.player){
                        if(ply.team() == Team.blue){
                            infect(uuidMapping.get(ply.uuid()), true);
                            updatePlayer(ply);
                        }
                    }

                    teams.remove(Team.blue);
                    if(teams.size() == 1){
                        endgame(new Seq<>());
                    }else{
                        Call.sendMessage("[accent]The game has started! [green]Survivors[accent] must survive for [gold]" +
                                winTime + "[accent] minutes to win!");
                    }




                }else{
                    Call.announce("[accent]You have [scarlet]" + (120 - counts[0]*20) +
                            " [accent]seconds left to place a core. Place any block to place a core.");
                }


            }

            realTime = System.currentTimeMillis() - startTime;
            seconds = (int) (realTime / 1000);

            if(!gameover && !hasWon && seconds > winTime * 60){
                hasWon = true;
                Groups.player.each((player) -> {
                    if(player.team() == Team.purple){
                        Call.infoMessage(player.con, "The survivors have evacuated all civilians and launched the inhibitors! " +
                                "The plague is super powerful now, finish what's left of the survivors!");
                    }else{
                        Call.infoMessage(player.con, "All civilians have been evacuated, and the inhibitors have been launched!" +
                                "The plague are now extremely powerful and will only get worse. Defend the noble few for as long as possible!");
                        CustomPlayer cPly = uuidMapping.get(player.uuid());
                        cPly.monthWins++;
                        cPly.wins++;
                        player.sendMessage("[gold]+1 wins[accent] for a total of [gold]" + cPly.monthWins + "[accent] wins!");
                        int addXp = 500 * (cPly.player.donatorLevel + 1);
                        cPly.xp += addXp;
                        cPly.player.sendMessage("[accent]+[scarlet]" + addXp + "xp[accent] for winning");
                    }

                });

                Call.sendMessage("[scarlet]The plague can now build and attack with air units!");
                // BUFF DA PLAGUE (enable air)

                UnitTypes.poly.weapons = polyWeapons;
                UnitTypes.mega.weapons = megaWeapon;
                UnitTypes.quad.weapons = quadWeapon;
                UnitTypes.oct.weapons = octWeapon;

                ((UnitFactory) Blocks.airFactory).plans.get(0).time = defaultFlareTime;

            }

            if(!gameover && !newRecord && seconds > mapRecord){
                newRecord = true;
                Call.sendMessage("[gold]New record![accent] Old record of[gold] "
                        + mapRecord/60 + "[accent] minutes and [gold]" + mapRecord % 60 + "[accent] seconds"
                        + "[accent] was beaten!");
            }

            if(tenMinInterval.get(seconds)) {
                multiplier *= 1.5;
                state.rules.unitDamageMultiplier *= 1.5;

                for (UnitType u : Vars.content.units()) {
                    u.health *= 1.5;
                }
                Call.sendMessage("[accent]Units now deal [scarlet]50%[accent] more damage and have [scarlet]50%[accent] more health " +
                        "for a total multiplier of [scarlet]" + df.format(multiplier) + "x");
            }

            if(oneMinInterval.get(seconds)){
                Call.infoPopup("[accent]Time survived: [orange]" + seconds/60 + "[accent] mins.\n" +
                                "All-time map record: [gold]" + mapRecord / 60 + "[accent] mins.",
                        60, 10, 120, 0, 140, 0);
            }

            });


        Events.on(EventType.PlayerJoinSecondary.class, event ->{
            if(!db.hasRow("mindustry_data", "uuid", event.player.uuid())){
                Log.info("New player, adding to local tables...");
                db.addEmptyRow("mindustry_data", "uuid", event.player.uuid());
            }

            HashMap<String, Object> entries = db.loadRow("mindustry_data", "uuid", event.player.uuid());


            if(!uuidMapping.containsKey(event.player.uuid())){
                uuidMapping.put(event.player.uuid(), new CustomPlayer(event.player));
            }
            CustomPlayer cPly = uuidMapping.get(event.player.uuid());
            cPly.player = event.player;
            cPly.team = cPly.player.team();
            cPly.rawName = event.player.name;
            cPly.xp = (int) entries.get("plagueXP");
            cPly.wins = (int) entries.get("plagueWins");
            cPly.monthWins = (int) entries.get("plagueMonthWins");

            if(!teams.get(cPly.team).hasPlayer(cPly)){
                teams.get(cPly.team).addPlayer(cPly);
            }

            updatePlayer(event.player);

            cPly.connected = true;





            if(event.player.team() == Team.blue){
                CoreBlock.playerSpawn(world.tile(plagueCore[0], plagueCore[1]), event.player);
            }

            event.player.sendMessage(leaderboardString);

            Call.infoPopup(event.player.con, "[accent]Time survived: [orange]" + seconds/60 + "[accent] mins.\n" +
                            "All-time map record: [gold]" + mapRecord / 60 + "[accent] mins.",
                    60, 10, 120, 0, 140, 0);
        });

        Events.on(EventType.PlayerLeave.class, event -> {

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
                        if(t.team != Team.purple){
                            for(CoreBlock.CoreBuild core : t.cores){
                                if(cartesianDistance(event.tile.x, event.tile.y, core.tile.x, core.tile.y) < 150){
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
                    updatePlayer(player);

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

        Events.on(EventType.BlockBuildEndEvent.class, event -> {
            if(event.unit.getPlayer() == null){
                return;
            }
            try {
                if(event.team == Team.purple && cartesianDistance(event.tile.x, event.tile.y,
                        plagueCore[0], plagueCore[1]) < world.height()/10){
                    event.tile.build.indestructible = true;
                }
            }catch(NullPointerException e){
                e.printStackTrace();
            }

            if(event.team != Team.purple && event.tile.block() == Blocks.airFactory){
                ((UnitFactory) event.tile.block()).plans.get(0).time = 129037f;
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

                for(CustomPlayer cPly : teams.get(Team.purple).players){
                    if(cPly.connected){
                        int addXp = 100 * (cPly.player.donatorLevel + 1);
                        cPly.player.sendMessage("[accent]+[scarlet]" + addXp + "xp[accent] for infecting survivors");
                        cPly.xp += addXp;
                    }
                }

                teams.remove(deadTeam);
                if(teams.size() == 1){
                    endgame(winners);
                }
            }
        });

        Events.on(EventType.UnitDestroyEvent.class, event ->{
            if(event.unit.getPlayer() != null && event.unit.team() == Team.blue){
                CoreBlock.playerSpawn(world.tile(plagueCore[0], plagueCore[1]), event.unit.getPlayer());
            }

        });

        Events.on(EventType.TapEvent.class, event ->{
            if(event.tile.block() == Blocks.vault && event.tile.team() != Team.purple){
                if(event.tile.build.items.has(Items.thorium, 997)){
                    event.tile.build.tile.setNet(Blocks.coreShard, event.tile.team(), 0);
                }
            }
        });

        Events.on(EventType.CustomEvent.class, event ->{
            if(event.value instanceof String[] && ((String[]) event.value)[0].equals("newName")){
                String[] val = (String[]) event.value;
                Player ply = uuidMapping.get(val[1]).player;
                CustomPlayer cPly = uuidMapping.get(val[1]);
                cPly.rawName = ply.name;
                ply.name = StringHandler.determineRank(cPly.xp) + " " + ply.name;
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
            int i = 0;
            for(mindustry.maps.Map map : maps.customMaps()){
                Log.info(i + ": " + map.name());
                i += 1;
            }

            if(args.length > 0){
                currMap = Integer.parseInt(args[0]);
            }else{
                currMap = Mathf.random(0, maps.customMaps().size-1);
            }


            logic.reset();
            mindustry.maps.Map map = maps.customMaps().get(currMap);
            world.loadMap(map);



            loadedMap = state.map;

            Tile tile = state.teams.cores(Team.purple).find(build -> build.block == Blocks.coreNucleus).tile;

            // Make cores and power source indestructible
            state.teams.cores(Team.purple).each(coreBuild -> coreBuild.indestructible = true);
            for(int x = 0; x < world.width(); x++){
                for(int y = 0; y < world.height(); y++){
                    Tile t = world.tile(x, y);
                    if(t.build != null && t.build.block.equals(Blocks.powerSource)){
                        t.build.indestructible = true;
                    }
                }
            }


            plagueCore[0] = tile.x;
            plagueCore[1] = tile.y;
            world.beginMapLoad();
            PlagueGenerator.defaultOres(world.tiles);
            world.endMapLoad();


            Log.info("Map " + loadedMap.name() + " loaded");
            rules.enemyCoreBuildRadius = loadedMap.rules().enemyCoreBuildRadius;

            state.rules = rules.copy();
            logic.play();

            netServer.openServer();

            prefs.putInt("mapchoice", currMap);
            mapID = loadedMap.file.name().split("_")[0];
            String[] keys = new String[]{"gamemode", "mapID"};
            Object[] vals = new Object[]{"plague", mapID};
            if(!db.hasRow("mindustry_map_data", keys, vals)){
                db.addEmptyRow("mindustry_map_data", keys, vals);
            }
            HashMap<String, Object> entries = db.loadRow("mindustry_map_data", keys, vals);
            checkExpiration();
            mapRecord = (int) entries.get("survivorRecord"); // Get map record
            avgSurvived = (int) entries.get("avgSurvived"); // Get average time survived
            mapPlays = (int) entries.get("plays"); // Get number of map plays

            tile.build.items.clear();

            teams.put(Team.purple, new PlagueTeam(Team.purple));
            teams.put(Team.blue, new PlagueTeam(Team.blue));
        });

        handler.register("setxp", "<uuid> <xp>", "Set the xp of a player", args -> {
            int newXp;
            String uuid = args[0];
            try{
                newXp = Integer.parseInt(args[1]);
            }catch(NumberFormatException e){
                Log.info("Invalid xp input '" + args[1] + "'");
                return;
            }

            if(uuidMapping.containsKey(uuid)){
                CustomPlayer cPly = uuidMapping.get(uuid);
                cPly.xp = newXp;
            }

            if(db.hasRow("mindustry_data", "uuid", uuid)){
                db.saveRow("mindustry_data", "uuid", uuid, "plagueXP", newXp);
                Log.info("Set uuid " + uuid + " to have xp of " + newXp);
            }else{
                Log.info("Cannot find uuid " + uuid);
            }



        });

        handler.register("manual_reset", "Perform a manual reset of xp and points", args -> {
            rankReset();
            winsReset();
            mapStatsReset();
            Log.info("ranks, monthly wins and monthly data have been reset");
        });


    }

    @Override
    public void registerClientCommands(CommandHandler handler){
        handler.<Player>register("infect", "Infect yourself", (args, player) -> {
            if(player.team() == Team.purple){
                player.sendMessage("[accent]Already infected!");
                return;
            }
            CustomPlayer cPly = uuidMapping.get(player.uuid());
            infect(cPly, true);
        });

        handler.<Player>register("xp", "Show your xp", (args, player) -> {
            CustomPlayer cPly = uuidMapping.get(player.uuid());
            int xp = cPly.xp;
            String nextRank = StringHandler.determineRank(xp+5000);
            player.sendMessage("[scarlet]" + xp + "[accent] xp\nReach [scarlet]" + (xp/5000+1)*5000 + "[accent] xp to rank up to " + nextRank + "[accent]!");
        });

        handler.<Player>register("stats", "Display stats about the current map", (args, player) -> {
            String s = "[accent]Map stats for: [white]" + state.map.name() + "\n" +
                    "[accent]Author: [white]" + state.map.author() + "\n" +
                    "[accent]Plays: [scarlet]" + mapPlays + "\n" +
                    "[accent]Average time survived: [scarlet]" + avgSurvived/60 + "[accent] minutes and [scarlet]" + avgSurvived % 60 + "[accent] seconds.\n" +
                    "[accent]Suvivor record: [scarlet]" + mapRecord/60 + "[accent] minutes and [scarlet]" + mapRecord % 60 + "[accent] seconds.";
            player.sendMessage(s);

        });

        handler.<Player>register("time", "Display the time now", (args, player) -> {
            String s = "[accent]Time: [scarlet]" + seconds/60 + "[accent] minutes and [scarlet]"
                    + seconds % 60 + "[accent] seconds.";
            player.sendMessage(s);
        });

        handler.<Player>register("leaderboard", "Display the leaderboard", (args, player) -> {
            player.sendMessage(leaderboardString);
        });

        handler.<Player>register("info", "Display info about the current game", (args, player) -> {
            player.sendMessage("[#4d004d]{[purple]AA[#4d004d]} [olive]Plague[accent] is a survival game mode with two teams," +
                    " the [scarlet]Plague [accent]and [green]Survivors[accent].\n\n" +
                    "The [scarlet]Plague[accent] build up their economy and make the biggest army possible, and try to" +
                    " break through the [green]Survivors[accent] defenses.\n\n" +
                    "The [green]Survivors[accent] build up a huge defense and last as long as possible. To become a " +
                    "[green]Survivor[accent], you must place a core in the first 2 minutes of the game, where you are " +
                    " allowed to choose your team. Place any block to place a core at the start of the game.\n\n" +
                    "Air factories are only able to make monos, and all air units do no damage.");
        });
    }

    void initRules(){

        rules.canGameOver = false;
        // rules.playerDamageMultiplier = 0;
        rules.buildSpeedMultiplier = 4;

        polyWeapons = UnitTypes.poly.weapons.copy();
        megaWeapon = UnitTypes.mega.weapons.copy();
        quadWeapon = UnitTypes.quad.weapons.copy();
        octWeapon = UnitTypes.oct.weapons.copy();

        defaultFlareTime = ((UnitFactory) Blocks.airFactory).plans.get(0).time;

        UnitTypes.alpha.weapons = new Seq<>();
        UnitTypes.beta.weapons = new Seq<>();
        UnitTypes.gamma.weapons = new Seq<>();

        UnitTypes.poly.weapons = new Seq<>();
        UnitTypes.mega.weapons = new Seq<>();
        UnitTypes.quad.weapons = new Seq<>();
        UnitTypes.oct.weapons = new Seq<>();

        UnitTypes.mega.payloadCapacity = 0f;
        UnitTypes.quad.payloadCapacity = 0f;
        UnitTypes.oct.payloadCapacity = 0f;

        rules.fire = false;
        rules.modeName = "Plague";

        ((UnitFactory) Blocks.airFactory).plans.get(0).time = 129037f;

        ((PowerSource) Blocks.powerSource).powerProduction = 696969f;

    }

    String _leaderboardInit(int limit){
        ResultSet rs = db.customQuery("SELECT * FROM mindustry_data ORDER BY plagueMonthWins DESC LIMIT " + limit);
        String s = "[accent]Leaderboard:\n";
        try{
            int i = 0;
            while(rs.next()){
                i ++;
                s += "\n[gold]" + (i) + "[white]:" + rs.getString("latestName") + "[accent]: [gold]" + rs.getString("plagueMonthWins") + "[accent] points";
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
        teams.get(Team.purple).addPlayer(cPly);


        if(cPly.connected){
            cPly.player.team(Team.purple);
            cPly.player.clearUnit();
            updatePlayer(cPly.player);
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
        for(Unit u : Groups.unit){
            if(u.team == team){
                u.kill();
            }
        }
    }

    private void updatePlayer(Player ply){

        if(ply.team() == Team.purple){
            Rules tempRules = rules.copy();
            tempRules.bannedBlocks = PlagueData.plagueBanned;
            for(int i=0; i<5; i++){ // Just making sure the packet gets there
                Call.setRules(ply.con, tempRules);
            }
        }else if (ply.team() != Team.blue){
            Rules tempRules = rules.copy();
            tempRules.bannedBlocks = PlagueData.survivorBanned;
            for(int i=0; i<5; i++){ // Just making sure the packet gets there
                Call.setRules(ply.con, tempRules);
            }
        }


        CustomPlayer cPly = uuidMapping.get(ply.uuid());
        ply.name = StringHandler.determineRank(cPly.xp) +
                colorMapping.getOrDefault(ply.team(), "[olive]") + " " + cPly.rawName;
    }

    private float cartesianDistance(float x, float y, float cx, float cy){
        return (float) Math.sqrt(Math.pow(x - cx, 2) + Math.pow(y - cy, 2) );
    }

    void savePlayerData(String uuid){
        if(!uuidMapping.containsKey(uuid)){
            Log.warn("uuid mapping does not contain uuid " + uuid + "! Not saving data!");
            return;
        }
        Log.info("PLAGUE: Saving " + uuid + " data...");
        CustomPlayer cPly = uuidMapping.get(uuid);
        cPly.team = cPly.player.team();

        String[] keys = {"plagueXP", "plagueMonthWins", "plagueWins"};
        Object[] vals = {cPly.xp, cPly.monthWins, cPly.wins};

        db.saveRow("mindustry_data", "uuid", uuid, keys, vals);
    }

    void endgame(Seq<CustomPlayer> winners){

        gameover = true;

        String[] keys = new String[]{"gamemode", "mapID"};
        Object[] vals = new Object[]{"plague", mapID};
        HashMap<String, Object> entries = db.loadRow("mindustry_map_data", keys, vals);
        int timeNow = seconds;

        for(CustomPlayer cPly : winners){
            if(!cPly.connected) continue;
            Call.infoMessage(cPly.player.con, "[green]You survived the longest\n" +
                    (newRecord ? "    [gold]New record!\n" : "") +
                    "[accent]Survive time: [scarlet]" + timeNow/60 + "[accent] minutes and [scarlet]" +
                    timeNow % 60 + "[accent] seconds.");
            int addXp = 500 * (cPly.player.donatorLevel + 1);
            cPly.player.sendMessage("[accent]+[scarlet]" + addXp + "xp[accent] for surviving the longest");
            cPly.xp += addXp;
            if(newRecord){
                addXp = 1000 * (cPly.player.donatorLevel + 1);
                cPly.player.sendMessage("[accent]+[scarlet]" + addXp + "xp[accent] for setting a record");
                cPly.xp += addXp;
            }

        }

        for(CustomPlayer cPly : teams.get(Team.purple).players){
            if(!winners.contains(cPly)){
                Call.infoMessage(cPly.player.con, "[accent]Game over!\nAll survivors have been infected. Loading new map...");
            }
        }
        int plays = (int) entries.get("plays");
        int avgSurvived = (int) entries.get("avgSurvived");
        if(timeNow > 60 * 5){
            plays ++;
            avgSurvived = (avgSurvived*(plays-1) + seconds)/plays;
        }

        int survivorRecord = (int) entries.get("survivorRecord");
        if(newRecord){
            survivorRecord = seconds;
        }


        prefs.putInt("mapchoice", PlagueData.getRandomWithExclusion(0, maps.customMaps().size, currMap));


        int finalSurvivorRecord = survivorRecord;
        int finalAvgSurvived = avgSurvived;
        int finalPlays = plays;
        Time.runTask(60f * 10f, () -> {

            for(Player player : Groups.player) {
                Call.connect(player.con, "aamindustry.play.ai", 6567);
            }

            db.saveRow("mindustry_map_data", keys, vals,
                    new String[]{"survivorRecord", "avgSurvived", "plays"},
                    new Object[]{finalSurvivorRecord, finalAvgSurvived, finalPlays});


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
            Log.info("New month - ranks, monthly wins and monthly data are reset automatically...");
        }
        prefs.putInt("month", currMonth);

    }

    void mapStatsReset(){
        String[] keys = new String[]{"gamemode", "mapID"};
        Object[] vals = new Object[]{"plague", mapID};
        db.saveRow("mindustry_map_data", keys, vals,
                new String[]{"survivorRecord", "avgSurvived", "plays"},
                new Object[]{0, 0, 0});
    }


    void rankReset(){
        // Reset ranks
        db.setColumn("mindustry_data", "plagueXP", 0);

        for(CustomPlayer cPly : uuidMapping.values()){
            cPly.xp = 0;
        }
    }

    void winsReset(){
        db.setColumn("mindustry_data", "plagueMonthWins", 0);

    }

}
