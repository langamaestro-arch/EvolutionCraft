package es.evolutioncraft;

import java.util.HashSet;
import java.util.Set;

/** Estado serializable de cada jugador. No depende de las clases de Minecraft. */
public final class Progress {
    public static final String[] ERA_NAMES = {"Madera", "Piedra y cuero", "Cobre", "Hierro", "Diamante", "Netherita"};
    public Set<String> crafted = new HashSet<>();
    public Set<String> trades = new HashSet<>();
    public Set<String> advancements = new HashSet<>();
    public Set<String> fishSpecies = new HashSet<>();
    public Set<String> fishingTreasures = new HashSet<>();
    public Set<String> bossKills = new HashSet<>();
    public int fish;
    public int tradeCount;
    /** Registro persistente por jugador: biomas visitados realmente en cada dimension. */
    public Set<String> overworldBiomes = new HashSet<>();
    public Set<String> netherBiomes = new HashSet<>();
    public Set<String> visitedStructures = new HashSet<>();
    /** Registros de la futura maestria, misiones y alquimia. */
    public Set<String> completedMissions = new HashSet<>();
    public Set<String> secretChallenges = new HashSet<>();
    public Set<String> potionEffects = new HashSet<>();
    public Set<String> potionTechniques = new HashSet<>();
    public int brewingBatches;
    public int toolWood, toolStone, toolCopper, toolIron, toolDiamond;
    public Set<String> toolTypes = new HashSet<>();
    public int naturalCopperOre, naturalDiamondOre;
    public int nightsSurvived;
    public long lastNightIndex = -1;
    public boolean visitedVillage;
    public int explorationLevel() {
        if (overworldBiomes.size() < 5) return 0;
        if (overworldBiomes.size() < 10 || !visitedVillage) return 1;
        if (overworldBiomes.size() < 15 || visitedStructures.size() < 2) return 2;
        if (overworldBiomes.size() < 20 || visitedStructures.size() < 4) return 3;
        if (overworldBiomes.size() < 30 || visitedStructures.size() < 6 || netherBiomes.size() < 3) return 4;
        return 5;
    }
    public int toolLevel() {
        if (toolWood < 50) return 0;
        if (toolStone < 150) return 1;
        if (toolCopper < 250) return 2;
        if (toolIron < 400) return 3;
        if (toolDiamond < 600) return 4;
        return 5;
    }
    public int alchemyLevel() {
        if (brewingBatches < 3 || potionEffects.size() < 2) return 0;
        if (brewingBatches < 8 || potionEffects.size() < 5
            || !potionEffects.contains("minecraft:healing")
            || !potionEffects.contains("minecraft:strength")) return 1;
        if (brewingBatches < 15 || potionEffects.size() < 8 ||
            !potionTechniques.containsAll(Set.of("extended", "strong", "splash"))) return 2;
        if (brewingBatches < 30 || potionEffects.size() < 12 ||
            !potionEffects.containsAll(Set.of("minecraft:invisibility","minecraft:slow_falling","minecraft:resistance"))) return 3;
        if (brewingBatches < 50 || potionEffects.size() < 15 ||
            !potionTechniques.contains("lingering")) return 4;
        return 5;
    }
    public int harvests;
    public int wheatHarvests;
    public Set<String> harvested = new HashSet<>();
    public int breedings;
    public Set<String> bred = new HashSet<>();
    public Set<String> tamed = new HashSet<>();
    public int agricultureLevel() {
        if (wheatHarvests < 32) return 0;
        if (!harvested.containsAll(Set.of("minecraft:wheat", "minecraft:carrots", "minecraft:potatoes", "minecraft:beetroots"))) return 1;
        if (harvests < 250 || !harvested.containsAll(Set.of("minecraft:melon", "minecraft:pumpkin"))) return 2;
        if (harvests < 500 || !harvested.containsAll(Set.of("minecraft:cocoa", "minecraft:nether_wart"))) return 3;
        if (harvests < 1000) return 4;
        return 5;
    }
    public int husbandryLevel() {
        if (breedings < 5) return 0;
        if (!bred.containsAll(Set.of("minecraft:cow", "minecraft:sheep", "minecraft:pig", "minecraft:chicken"))) return 1;
        if (breedings < 30 || !tamed.containsAll(Set.of("minecraft:wolf", "minecraft:cat"))) return 2;
        if (breedings < 75 || !bred.contains("minecraft:horse")) return 3;
        if (breedings < 150) return 4;
        return 5;
    }

    private boolean craftedTools(String material) {
        for (String tool : new String[]{"pickaxe", "axe", "sword", "shovel", "hoe"})
            if (!crafted.contains("minecraft:" + material + "_" + tool)) return false;
        return true;
    }
    private boolean craftedArmor(String material) {
        for (String part : new String[]{"helmet", "chestplate", "leggings", "boots"})
            if (!crafted.contains("minecraft:" + material + "_" + part)) return false;
        return true;
    }
    private boolean tradedEquipment(String material) {
        return trades.stream().anyMatch(s -> s.startsWith("minecraft:" + material + "_") &&
                (s.endsWith("_pickaxe") || s.endsWith("_axe") || s.endsWith("_sword") ||
                 s.endsWith("_shovel") || s.endsWith("_hoe")));
    }
    public int era() {
        if (!craftedTools("wooden") || !advancements.contains("minecraft:story/mine_stone") || toolLevel() < 1 || !completedMissions.contains("wood")) return 0;
        if (!craftedTools("stone") || !craftedArmor("leather") || tradeCount < 1 || !visitedVillage || toolLevel() < 2 || !completedMissions.contains("stone") || agricultureLevel() < 1 || husbandryLevel() < 1) return 1;
        if (!craftedTools("copper") || !craftedArmor("copper") || !tradedEquipment("iron") ||
                !advancements.contains("minecraft:story/smelt_iron") || toolLevel() < 3 || naturalCopperOre < 32 || agricultureLevel() < 2 || husbandryLevel() < 2) return 2;
        if (!craftedTools("iron") || !craftedArmor("iron") || !tradedEquipment("diamond") ||
                !advancements.contains("minecraft:story/mine_diamond") || toolLevel() < 4 || naturalDiamondOre < 5 || agricultureLevel() < 3 || husbandryLevel() < 3) return 3;
        if (!craftedTools("diamond") || !craftedArmor("diamond") ||
                !advancements.contains("minecraft:story/enter_the_nether")) return 4;
        return 5;
    }
    /** Cinco niveles acumulativos; todos los peces proceden de la pesca registrada. */
    public int fishingLevel() {
        if (fish < 25) return 0;
        if (fish < 60 || fishSpecies.size() < 3) return 1;
        if (fish < 120 || fishSpecies.size() < 4 || fishingTreasures.size() < 2) return 2;
        if (fish < 250 || fishingTreasures.size() < 3) return 3;
        if (fish < 500 || !advancements.contains("minecraft:husbandry/tactical_fishing")) return 4;
        return 5;
    }
    public boolean canNether() {
        return era() >= 4 && fishingLevel() >= 3 && bossKills.contains("minecraft:warden") &&
               agricultureLevel() >= 3 && husbandryLevel() >= 3 && advancements.size() >= 40 && overworldBiomes.size() >= 20 && explorationLevel() >= 4;
    }
    public boolean canEnd() {
        return canNether() && fishingLevel() == 5 && explorationLevel() >= 5 && alchemyLevel() >= 3 && bossKills.contains("minecraft:wither") &&
               agricultureLevel() >= 5 && husbandryLevel() >= 5 && advancements.size() >= 70 && advancements.contains("minecraft:nether/explore_nether");
    }
    public static int requiredEra(String item) {
        if (!item.startsWith("minecraft:")) return 0;
        String s = item.substring("minecraft:".length());
        for (String suffix : new String[]{"_pickaxe", "_axe", "_sword", "_shovel", "_hoe", "_helmet", "_chestplate", "_leggings", "_boots"}) {
            if (!s.endsWith(suffix)) continue;
            if (s.startsWith("netherite_")) return 5;
            if (s.startsWith("diamond_")) return 4;
            if (s.startsWith("iron_") || s.startsWith("golden_")) return 3;
            if (s.startsWith("copper_")) return 2;
            if (s.startsWith("stone_") || s.startsWith("leather_")) return 1;
        }
        return 0;
    }
}
