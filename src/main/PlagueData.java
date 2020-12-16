package main;

import arc.math.Mathf;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Bullets;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.gen.Sounds;
import mindustry.type.ItemStack;
import mindustry.type.Weapon;
import mindustry.world.Block;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class PlagueData {

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
        survivorLoadout = ItemStack.list(Items.copper, 4000, Items.lead, 4000,
                Items.graphite, 2000, Items.titanium, 2000, Items.silicon, 1000, Items.metaglass, 1000);
    }

    public static final Seq<ItemStack> plagueLoadout;
    static {
        plagueLoadout = ItemStack.list(Items.copper, 4000, Items.lead, 4000,
                Items.graphite, 2000, Items.titanium, 2000, Items.silicon, 1000, Items.metaglass, 1000);
    }

    public static final ObjectSet<Block> survivorBanned;
    static {
        survivorBanned = ObjectSet.with(Blocks.commandCenter, Blocks.groundFactory, Blocks.navalFactory,
                Blocks.tetrativeReconstructor, Blocks.phaseWall, Blocks.phaseWallLarge,
                Blocks.logicDisplay, Blocks.largeLogicDisplay, Blocks.microProcessor, Blocks.logicProcessor, Blocks.hyperProcessor,
                Blocks.additiveReconstructor, Blocks.multiplicativeReconstructor, Blocks.exponentialReconstructor,
                Blocks.tetrativeReconstructor);
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
                Blocks.logicDisplay, Blocks.largeLogicDisplay, Blocks.microProcessor, Blocks.logicProcessor, Blocks.hyperProcessor);
    }

    public static final Weapon daggerWepaon = new Weapon("dagger-weapon") {
        {
            this.reload = 14.0F;
            this.alternate = true;
            this.bullet = Bullets.standardCopper;
        }};

    public static final Weapon titanWepaon = new Weapon("titan-weapon") {
        {
            this.shootSound = Sounds.flame;
            this.reload = 7F;
            this.alternate = true;
            this.recoil = 1.0F;
            this.ejectEffect = Fx.none;
            this.bullet = Bullets.basicFlame;
        }};

    public static final Weapon fortressWepaon = new Weapon("fortress-weapon") {
        {
            this.reload = 40.0F;
            this.alternate = true;
            this.recoil = 4.0F;
            this.shake = 2.0F;
            this.bullet = Bullets.artilleryDense;
            this.shootSound = Sounds.artillery;
        }};


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
