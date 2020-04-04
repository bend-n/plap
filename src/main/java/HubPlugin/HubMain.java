package HubPlugin;

import arc.*;
import arc.func.Cons;
import arc.net.Server;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.Version;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.type.*;
import mindustry.game.EventType.*;
import mindustry.game.Gamemode;
import mindustry.game.Rules;
import mindustry.gen.*;
import mindustry.net.*;
import mindustry.net.Net;
import mindustry.plugin.Plugin;
import mindustry.world.Block;
import mindustry.world.blocks.defense.turrets.Turret;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static mindustry.Vars.*;

public class HubMain extends Plugin{

    private final Rules rules = new Rules();

    private final boolean[] serversUp = {false, false};

    private static int customPlayerCount = 0;

    private final static int playerCountTime = 30;

    private final static int timerPlayerCount = 0;
    private Interval interval = new Interval(1);

    // FFA pos: 2000, 2545
    @Override
    public void init(){
        // Disable bullet damage
        float distance;
        int ffax = 150*tilesize;
        int ffay = 225*tilesize;

        int plaguex = 150*tilesize;
        int plaguey = 62*tilesize;

        for (BulletType b : content.bullets()){
            b.damage = 0;
            b.splashDamage = 0;
        }

        try { // CREDIT TO QUEZLER FOR SHOWING ME THE RELEVANT CODE TO EDIT
            Field f = net.getClass().getDeclaredField("provider");
            f.setAccessible(true);
            Net.NetProvider prov = (Net.NetProvider) f.get(net);
            Field f1 = prov.getClass().getDeclaredField("server");
            f1.setAccessible(true);
            Server ser = (Server) f1.get(prov);
            ser.setDiscoveryHandler((address, handler2) -> {
                ByteBuffer buffer = customWriteServerData();
                buffer.position(0);
                handler2.respond(buffer);
            });
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        // Initialise player count
        updatePlayerCount();

        // Disable building
        netServer.admins.addActionFilter((action) -> {
            return false;
        });

        Events.on(Trigger.update, () -> {

            for (Player player : playerGroup.all()) {
                // ffa
                if(serversUp[0] && Math.sqrt(Math.pow(player.x - ffax, 2)+ Math.pow(player.y - ffay, 2)) < 140){
                    Call.onConnect(player.con, "aamindustry.play.ai", 6568);
                }
                // plague
                if(serversUp[1] && Math.sqrt(Math.pow(player.x - plaguex, 2)+ Math.pow(player.y - plaguey, 2)) < 100){
                    Call.onConnect(player.con, "aamindustry.play.ai", 6569);
                }

            }

            // Refresh server player count every 5 seconds and check server status
            if (interval.get(timerPlayerCount, playerCountTime)){
                updatePlayerCount();

                updateServerStatus();
            }

        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler){

        //register a simple reply command
        handler.<Player>register("ffa", "Connect to the FFA server", (args, player) -> {
            Call.onConnect(player.con, "aamindustry.play.ai", 6568);
        });

        handler.<Player>register("votekick", "Disabled", (args, player) -> {
            player.sendMessage("No");
        });

        handler.<Player>register("getpos", "Get x,y", (args, player) -> {
            player.sendMessage("(" + player.x + ", " + player.y + ")");
        });
    }

    private void updatePlayerCount(){
        customPlayerCount = playerGroup.size();
        net.pingHost("aamindustry.play.ai", 6568, this::addCount, e -> {});
        net.pingHost("aamindustry.play.ai", 6569, this::addCount, e -> {});
    }

    private void addCount(Host host){
        customPlayerCount += host.players;
    }

    private void updateServerStatus(){
        net.pingHost("aamindustry.play.ai", 6568, host -> { serversUp[0] = true; }, e -> {});
        net.pingHost("aamindustry.play.ai", 6569, host -> { serversUp[1] = true; }, e -> {});
    }

    public static ByteBuffer customWriteServerData(){
        String name = (headless ? Administration.Config.name.string() : player.name);
        String description = headless && !Administration.Config.desc.string().equals("off") ? Administration.Config.desc.string() : "";
        String map = world.getMap() == null ? "None" : world.getMap().name();

        ByteBuffer buffer = ByteBuffer.allocate(512);

        writeString(buffer, name, 100);
        writeString(buffer, map, 32);

        buffer.putInt(customPlayerCount);
        buffer.putInt(state.wave);
        buffer.putInt(Version.build);
        writeString(buffer, Version.type, 32);

        buffer.put((byte) Gamemode.bestFit(state.rules).ordinal());
        buffer.putInt(netServer.admins.getPlayerLimit());

        writeString(buffer, description, 100);
        return buffer;
    }

    private static void writeString(ByteBuffer buffer, String string, int maxlen){
        byte[] bytes = string.getBytes(charset);
        //todo truncating this way may lead to weird encoding errors at the ends of strings...
        if(bytes.length > maxlen){
            bytes = Arrays.copyOfRange(bytes, 0, maxlen);
        }

        buffer.put((byte)bytes.length);
        buffer.put(bytes);
    }
}
