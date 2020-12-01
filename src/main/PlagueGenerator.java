package main;

import arc.struct.Seq;
import arc.struct.StringMap;
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

import static mindustry.Vars.maps;
import static mindustry.Vars.world;


public class PlagueGenerator extends BaseGenerator{

    // elevation --->
    // temperature
    // |
    // v

    public static final int size = 601;
    int terrain_type;
    int map_type;

    Block[][] floors;
    Block[][] blocks;

    // Fix this generation later

    @Override
    public void generate(Tiles tiles, Seq<Tile> cores, Tile spawn, Team team, Sector sector, float difficulty) {

        GenerateInput in = new GenerateInput();

        tendrilFilter gapGen = new tendrilFilter();
        gapGen.block = Blocks.air;
        gapGen.floorGen = false;
        gapGen.falloff = (float) 0.2;
        gapGen.scl = (float) 40;
        gapGen.threshold = (float) 0.45;

        tendrilFilter mossGen = new tendrilFilter();
        mossGen.floor = Blocks.moss;
        mossGen.blockGen = false;

        int cx = size / 2;
        int cy = size / 2;
        double centreDist = 0;

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                Block wall;
                Block floor = Blocks.darksand;
                centreDist = Math.sqrt(Math.pow(cx - x, 2) + Math.pow(cy - y, 2));
                if (centreDist < 100) {
                    wall = Blocks.air;
                }else {
                    wall = Blocks.duneWall;
                }


                Block ore = Blocks.air;

                in.floor = floor;
                in.block = wall;
                in.overlay = ore;
                in.x = x;
                in.y = y;
                in.width = in.height = size;

                if (centreDist >= 100) {
                    gapGen.apply(in);
                }
                if (centreDist >= 150){
                    mossGen.apply(in);
                }

                if(x == 0 || x == size-1 || y == 0 || y == size-1){
                    in.block = Blocks.duneWall;
                }

                tiles.set(x,y, new Tile(x, y, in.floor.id, in.overlay.id, in.block.id));
            }
        }

        tiles.get(size/2,size/2).setNet(Blocks.coreFoundation, Team.crux, 0);
        tiles.get(size/2,size/2+10).setNet(Blocks.powerSource, Team.crux, 0);

        world.loadMap(new Map(StringMap.of("name", "Patient Zero", "author", "Recessive")));
    }

    public static void defaultOres(Tiles tiles){
        GenerateInput in = new GenerateInput();
        Seq<GenerateFilter> ores = new Seq<>();
        maps.addDefaultOres(ores);
        ores.each(o -> ((OreFilter) o).threshold -= 0.05f);
        ores.insert(0, new OreFilter() {{
            ore = Blocks.oreScrap;
            scl += 2 / 2.1F;
        }});

        in.floor = (Floor) Blocks.darksand;
        in.block = Blocks.duneWall ;
        in.width = in.height = size;

        for (int x = 0; x < tiles.width; x++) {
            for (int y = 0; y < tiles.height; y++) {
                in.overlay = Blocks.air;
                in.x = x;
                in.y = y;

                if(tiles.get(x,y).floor().isLiquid) continue;

                for (GenerateFilter f : ores) {
                    f.apply(in);
                }
                tiles.get(x,y).setOverlay(in.overlay);
            }
        }
    }

    private static void perimeterFlood(List<Tile> tileFlood, int[][] floodGrid, Tiles tiles){
        while(!tileFlood.isEmpty()){

            Tile t = tileFlood.remove(0);

            if (t.x+1 < tiles.width && floodGrid[t.x+1][t.y] == 0) {
                floodGrid[t.x+1][t.y] = 1;
                tileFlood.add(tiles.get(t.x+1,t.y));
            }
            if (t.x-1 > 0 && floodGrid[t.x-1][t.y] == 0) {
                floodGrid[t.x-1][t.y] = 1;
                tileFlood.add(tiles.get(t.x-1, t.y));
            }
            if (t.y+1 < tiles.width && floodGrid[t.x][t.y+1] == 0) {
                floodGrid[t.x][t.y+1] = 1;
                tileFlood.add(tiles.get(t.x, t.y+1));
            }
            if (t.y-1 > 0 && floodGrid[t.x][t.y-1] == 0) {
                floodGrid[t.x][t.y-1] = 1;
                tileFlood.add(tiles.get(t.x, t.y-1));
            }
        }
    }

    public static void inverseFloodFill(Tiles tiles, int cx, int cy){
        int[][] floodGrid = new int[size][size];
        for(int x = 0; x < tiles.width; x++){
            for(int y = 0; y < tiles.height; y++){
                if(tiles.get(x,y).block() instanceof StaticWall || tiles.get(x,y).floor().isDeep()){
                    floodGrid[x][y] = 2;
                }
            }
        }
        List<Tile> tileFlood = new ArrayList<>();
        tileFlood.add(tiles.get(cx, cy));
        perimeterFlood(tileFlood, floodGrid, tiles);

        for (int x = 0; x < tiles.width; x++) {
            for (int y = 0; y < tiles.height; y++) {
                if (floodGrid[x][y] == 0){
                    tiles.get(x,y).setBlock(Blocks.duneWall);
                }
            }
        }
    }
}

class tendrilFilter extends GenerateFilter{
    public float scl = 40, threshold = 0.5f, octaves = 3f, falloff = 0.5f;
    public Block floor = Blocks.stone, block = Blocks.stoneWall;

    public boolean blockGen = true;
    public boolean floorGen = true;

    @Override
    public FilterOption[] options() {
        return new FilterOption[0];
    }

    @Override
    public void apply(){
        float noise = noise(in.x, in.y, scl, 1f, octaves, falloff);

        if(noise > threshold){
            if(floorGen) in.floor = floor;
            if(blockGen) in.block = block;
        }
    }
}
