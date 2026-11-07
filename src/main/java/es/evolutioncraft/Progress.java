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
        if (!craftedTools("wooden") || !advancements.contains("minecraft:story/mine_stone")) return 0;
        if (!craftedTools("stone") || !craftedArmor("leather") || tradeCount < 1) return 1;
        if (!craftedTools("copper") || !craftedArmor("copper") || !tradedEquipment("iron") ||
                !advancements.contains("minecraft:story/smelt_iron")) return 2;
        if (!craftedTools("iron") || !craftedArmor("iron") || !tradedEquipment("diamond") ||
                !advancements.contains("minecraft:story/mine_diamond")) return 3;
        if (!craftedTools("diamond") || !craftedArmor("diamond") ||
                !advancements.contains("minecraft:story/enter_the_nether")) return 4;
        return 5;
    }
    public int fishingLevel() {
        if (fish < 10) return 0;
        if (fishSpecies.size() < 4) return 1;
        if (fishingTreasures.size() < 3) return 2;
        if (fish < 100) return 3;
        if (!advancements.contains("minecraft:husbandry/tactical_fishing")) return 4;
        return 5;
    }
    public boolean canNether() {
        return era() >= 3 && fishingLevel() >= 3 && bossKills.contains("minecraft:warden") &&
               advancements.size() >= 20 && advancements.contains("minecraft:adventure/adventuring_time");
    }
    public boolean canEnd() {
        return canNether() && fishingLevel() == 5 && bossKills.contains("minecraft:wither") &&
               advancements.size() >= 45 && advancements.contains("minecraft:nether/explore_nether");
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
