package main;

import arc.math.Mathf;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import mindustry.maps.Maps;
import mindustry.maps.filters.*;
import mindustry.type.ItemStack;
import mindustry.type.UnitType;
import mindustry.type.Weapon;
import mindustry.world.Block;
import mindustry.world.blocks.environment.OreBlock;

import java.util.*;

public class PlagueData {

    public static final List<PrestigeLevel> prestiges;
    static{
        prestiges = new ArrayList<>();
        prestiges.add(new PrestigeLevel("Poly", UnitTypes.poly, 1));
        prestiges.add(new PrestigeLevel("Alpha 1", UnitTypes.alpha, 4));
        prestiges.add(new PrestigeLevel("Alpha 2", UnitTypes.alpha, 3));
        prestiges.add(new PrestigeLevel("Alpha 3", UnitTypes.alpha, 2));
        prestiges.add(new PrestigeLevel("Alpha 4", UnitTypes.alpha, 1));
        prestiges.add(new PrestigeLevel("Beta 1", UnitTypes.beta, 4));
        prestiges.add(new PrestigeLevel("Beta 2", UnitTypes.beta, 3));
        prestiges.add(new PrestigeLevel("Beta 3", UnitTypes.beta, 2));
        prestiges.add(new PrestigeLevel("Beta 4", UnitTypes.beta, 1));
        prestiges.add(new PrestigeLevel("Gamma 1", UnitTypes.gamma, 4));
        prestiges.add(new PrestigeLevel("Gamma 2", UnitTypes.gamma, 3));
        prestiges.add(new PrestigeLevel("Gamma 3", UnitTypes.gamma, 2));
        prestiges.add(new PrestigeLevel("Gamma 4", UnitTypes.gamma, 1));
        prestiges.add(new PrestigeLevel("Mega 1", UnitTypes.mega, 4));
        prestiges.add(new PrestigeLevel("Mega 2", UnitTypes.mega, 3));
        prestiges.add(new PrestigeLevel("Mega 3", UnitTypes.mega, 2));
        prestiges.add(new PrestigeLevel("Mega 4", UnitTypes.mega, 1));
    }




    public static final Map<String, Float> itemValues;
    static {
        Map<String, Float> aMap = new HashMap<>();
        aMap.put("copper", 0.5f);
        aMap.put("lead", 1f);
        aMap.put("metaglass", 1.5f);
        aMap.put("graphite", 1.5f);
        aMap.put("titanium", 1.5f);
        aMap.put("silicon", 2f);
        aMap.put("thorium", 1.5f);
        aMap.put("plastanium", 3f);
        aMap.put("phase-frabic", 5f);
        aMap.put("surge-alloy", 6f);
        itemValues = Collections.unmodifiableMap(aMap);
    }

    public static final Seq<ItemStack> survivorLoadoutSerpulo;
    static {
        survivorLoadoutSerpulo = ItemStack.list(Items.copper, 3600, Items.lead, 3600,
                Items.graphite, 1800, Items.titanium, 1800, Items.silicon, 900, Items.metaglass, 900);
    }

    public static final Seq<ItemStack> survivorLoadoutErekir;
    static {
        survivorLoadoutErekir = ItemStack.list(Items.beryllium, 1000, Items.graphite, 1000,
                Items.tungsten, 500, Items.thorium, 500);
    }

    public static final ObjectSet<Block> survivorBanned;
    static {
        survivorBanned = ObjectSet.with(Blocks.groundFactory, Blocks.navalFactory,
                Blocks.logicDisplay, Blocks.largeLogicDisplay, Blocks.canvas, // Blocks.microProcessor, Blocks.logicProcessor, Blocks.hyperProcessor,
                Blocks.multiplicativeReconstructor, Blocks.exponentialReconstructor, Blocks.tetrativeReconstructor,
                Blocks.shipAssembler, Blocks.mechAssembler, Blocks.tankAssembler, Blocks.tankFabricator, Blocks.shipFabricator,
                Blocks.mechFabricator, Blocks.tankRefabricator, Blocks.mechRefabricator, Blocks.shipRefabricator,
                Blocks.primeRefabricator);
    }

    public static final ObjectSet<Block> plagueBanned;
    static {
        plagueBanned = ObjectSet.with(Blocks.duo, Blocks.scatter, Blocks.scorch, Blocks.lancer, Blocks.arc,
                Blocks.swarmer, Blocks.salvo, Blocks.fuse, Blocks.cyclone, Blocks.spectre, Blocks.meltdown,
                Blocks.hail, Blocks.ripple, Blocks.shockMine, Blocks.parallax, Blocks.segment, Blocks.tsunami,
                Blocks.foreshadow,
                Blocks.battery, Blocks.batteryLarge, Blocks.combustionGenerator, Blocks.thermalGenerator,
                Blocks.steamGenerator, Blocks.differentialGenerator, Blocks.rtgGenerator, Blocks.solarPanel,
                Blocks.largeSolarPanel, Blocks.thoriumReactor, Blocks.impactReactor,
                Blocks.surgeWall, Blocks.surgeWallLarge, Blocks.thoriumWall, Blocks.thoriumWallLarge, Blocks.phaseWall,
                Blocks.phaseWallLarge, Blocks.titaniumWall, Blocks.titaniumWallLarge, Blocks.copperWallLarge,
                Blocks.copperWall, Blocks.door, Blocks.doorLarge, Blocks.plastaniumWall, Blocks.plastaniumWallLarge,
                Blocks.logicDisplay, Blocks.largeLogicDisplay, Blocks.canvas,
                Blocks.breach, Blocks.diffuse, Blocks.sublimate, Blocks.titan, Blocks.disperse, Blocks.afflict, Blocks.lustre,
                Blocks.scathe, Blocks.smite, Blocks.malign); // Blocks.microProcessor, Blocks.logicProcessor, Blocks.hyperProcessor);
    }

    public static final ObjectSet<GenerateFilter> erekirFilters;
    static{
        final int[] seedOffset = {(int) System.currentTimeMillis()};
        OreFilter berylliumFloorFilter = new OreFilter() {{
            ore = Blocks.oreBeryllium;
            seed = seedOffset[0]++;
        }};

        OreFilter berylliumWallFilter = new OreFilter() {{
            ore = Blocks.wallOreBeryllium;
            seed = seedOffset[0]++;
            threshold -= 0.1;
        }};

        OreFilter tungstenFloorFilter = new OreFilter() {{
            ore = Blocks.oreTungsten;
            seed = seedOffset[0]++;
        }};
        OreFilter tungstenWallFilter = new OreFilter() {{
            ore = Blocks.wallOreTungsten;
            seed = seedOffset[0]++;
            threshold -= 0.1;
        }};

        OreFilter thoriumFloorFilter = new OreFilter() {{
            ore = Blocks.oreCrystalThorium;
            seed = seedOffset[0]++;
        }};
        OreFilter thoriumWallFilter = new OreFilter() {{
            ore = Blocks.wallOreThorium;
            seed = seedOffset[0]++;
            threshold -= 0.1;
        }};

        OreFilter graphiteWallFilter = new OreFilter() {{
            ore = Blocks.graphiticWall;
            seed = seedOffset[0]++;
            threshold -= 0.1;
        }};

        OreFilter slagOreFilter = new OreFilter() {{
            ore = Blocks.slag;
            seed = seedOffset[0] ++;
            threshold += 0.075;
        }};

        OreFilter arkyciteOreFilter = new OreFilter() {{
            ore = Blocks.arkyciteFloor;
            seed = seedOffset[0] ++;
            threshold += 0.075;
        }};

        ScatterFilter ventFilter = new ScatterFilter() {{
            floor = Blocks.carbonVent;
            chance = 0.001f;
        }};





        erekirFilters = ObjectSet.with(
                berylliumFloorFilter, berylliumWallFilter,
                tungstenFloorFilter, tungstenWallFilter,
                thoriumFloorFilter, thoriumWallFilter,
                graphiteWallFilter,
                slagOreFilter, arkyciteOreFilter,
                ventFilter);
    }


    public static int getRandomWithExclusion(int start, int end, int... exclude) {
        int random = start + Mathf.random(end - start - exclude.length);
        for (int ex : exclude) {
            if (random < ex) {
                break;
            }
            random++;
        }
        return random;
    }


}

class PrestigeLevel {
    public String name;
    public UnitType unit;
    public int frequency;

    public PrestigeLevel(String name, UnitType unit, int frequency){
        this.name = name;
        this.unit = unit;
        this.frequency = frequency;
    }
}
