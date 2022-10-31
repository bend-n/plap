package main;

import arc.math.Mathf;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import mindustry.type.ItemStack;
import mindustry.type.UnitType;
import mindustry.type.Weapon;
import mindustry.world.Block;

import java.util.*;

public class PlagueData {

    public static final List<PrestigeLevel> prestiges;
    static{
        prestiges = new ArrayList<>();
        prestiges.add(new PrestigeLevel("Alpha", UnitTypes.alpha, 1));
        prestiges.add(new PrestigeLevel("Beta 1", UnitTypes.beta, 4));
        prestiges.add(new PrestigeLevel("Beta 2", UnitTypes.beta, 3));
        prestiges.add(new PrestigeLevel("Beta 3", UnitTypes.beta, 2));
        prestiges.add(new PrestigeLevel("Beta 4", UnitTypes.beta, 1));
        prestiges.add(new PrestigeLevel("Poly 1", UnitTypes.poly, 4));
        prestiges.add(new PrestigeLevel("Poly 2", UnitTypes.poly, 3));
        prestiges.add(new PrestigeLevel("Poly 3", UnitTypes.poly, 2));
        prestiges.add(new PrestigeLevel("Poly 4", UnitTypes.poly, 1));
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

    public static final Seq<ItemStack> survivorLoadout;
    static {
        survivorLoadout = ItemStack.list(Items.copper, 3600, Items.lead, 3600,
                Items.graphite, 1800, Items.titanium, 1800, Items.silicon, 900, Items.metaglass, 900);
    }

    public static final Seq<ItemStack> plagueLoadout;
    static {
        plagueLoadout = ItemStack.list(Items.copper, 4000, Items.lead, 4000,
                Items.graphite, 2000, Items.titanium, 2000, Items.silicon, 1000, Items.metaglass, 1000);
    }

    public static final ObjectSet<Block> survivorBanned;
    static {
        survivorBanned = ObjectSet.with(Blocks.commandCenter, Blocks.groundFactory, Blocks.navalFactory,
                Blocks.logicDisplay, Blocks.largeLogicDisplay, // Blocks.microProcessor, Blocks.logicProcessor, Blocks.hyperProcessor,
                Blocks.multiplicativeReconstructor, Blocks.exponentialReconstructor, Blocks.tetrativeReconstructor);
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
                Blocks.logicDisplay, Blocks.largeLogicDisplay); // Blocks.microProcessor, Blocks.logicProcessor, Blocks.hyperProcessor);
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
