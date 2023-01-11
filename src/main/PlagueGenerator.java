package main;

import arc.struct.Seq;
import arc.struct.StringMap;
import arc.util.Log;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.game.Team;
import mindustry.maps.Map;
import mindustry.maps.filters.FilterOption;
import mindustry.maps.filters.GenerateFilter;
import mindustry.maps.filters.GenerateFilter.GenerateInput;
import mindustry.maps.filters.OreFilter;
import mindustry.maps.generators.BaseGenerator;
import mindustry.type.Sector;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.Tiles;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.environment.StaticWall;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static mindustry.Vars.maps;
import static mindustry.Vars.world;


public class PlagueGenerator{

    public static final int size = 601;

    public static final int[] a = IntStream.range(1, 100).toArray();

    public static void defaultOres(Tiles tiles){
        GenerateInput in = new GenerateInput();
        Seq<GenerateFilter> ores = new Seq<>();
        maps.addDefaultOres(ores);


        int i = (int) System.currentTimeMillis();
        for(GenerateFilter o : ores){
            ((OreFilter) o).threshold -= 0.05f;
            o.seed = i++;
        }

        ores.insert(0, new OreFilter() {{
            ore = Blocks.oreScrap;
        }});


        in.floor = (Floor) Blocks.darksand;
        in.block = Blocks.duneWall ;
        in.width = tiles.width;
        in.height = tiles.height;

        for (int x = 0; x < tiles.width; x++) {
            for (int y = 0; y < tiles.height; y++) {
                in.overlay = Blocks.air;
                in.x = x;
                in.y = y;

                if(tiles.get(x,y).floor().isLiquid || !tiles.get(x,y).floor().placeableOn) continue;

                for (GenerateFilter f : ores) {
                    f.apply(in);
                }
                tiles.get(x,y).setOverlay(in.overlay);
            }
        }
    }

}


