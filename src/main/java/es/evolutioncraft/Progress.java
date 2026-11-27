package es.evolutioncraft;

import java.util.HashSet;
import java.util.Set;

/** Estado serializable de cada jugador. No depende de las clases de Minecraft. */
public final class Progress {
    public static final String[] ERA_NAMES = {"Madera", "Piedra y cuero", "Cobre", "Hierro", "Diamante", "Netherita"};
    /** Administrador: -1 = progresion natural; 0..5 = era fijada expresamente. */
    public int adminEraOverride=-1;
    public java.util.Map<String,Integer> eliteMobKills=new java.util.HashMap<>();
    public boolean eliteMobMigrated, eliteNetherGrandfathered, eliteEndGrandfathered;
    public transient boolean eliteMobGateActive;
    public transient int[] eliteNetherRequirements, eliteEndRequirements;
    /** Administrative correction of individual era objectives: key era goal index. */
    public java.util.Map<String,Boolean> adminEraObjectives=new java.util.HashMap<>();

    public Set<String> crafted = new HashSet<>();
    public Set<String> trades = new HashSet<>();
    public Set<String> advancements = new HashSet<>();
    public Set<String> fishSpecies = new HashSet<>();
    public Set<String> fishingTreasures = new HashSet<>();
    public Set<String> bossKills = new HashSet<>();
    /** Nicktale 0.23.0 dimension guardians killed by this player. */
    public Set<String> nicktaleDimensionBossKills=new HashSet<>();
    public boolean nicktaleDimensionMigrated=false;
    public boolean nicktaleNetherGrandfathered=false;
    public boolean nicktaleEndGrandfathered=false;
    public transient boolean nicktaleDimensionGateActive=false;
    /** Distinct Nicktale bosses defeated personally, persisted per-player. */
    public Set<String> nicktaleEraBossKills=new HashSet<>();
    /** Preserves already unlocked eras when introducing the new boss gates. */
    public boolean nicktaleBossMigrated=false;
    public int nicktaleBossGrandfatherEra=0;
    public transient boolean nicktaleBossGateActive=false;
    public int fish;
    public int tradeCount;
    public String selectedBadge="";
    /** Canjes definitivos por mundo y UUID; nunca reiniciar estos conjuntos. */
    public Set<String> claimedRewards = new HashSet<>();
    /** Impide notificaciones repetidas después de reconectar. */
    public Set<String> notifiedRewards = new HashSet<>();
    public int cleanNights;
    public int harvestStreak;
    public int treasuresCaught;
    public boolean horseTamed;
    public int elderGuardianKills;
    public boolean obtainedSponge;
    public Set<String> netherKillTypes=new HashSet<>();
    public Set<String> consumedPotions=new HashSet<>();
    public boolean threeHighEnchantments;
    public boolean dragonSlain;
    public boolean wardenSlainWithoutDeath;
    public int lingeringBrews;
    public int deathCount;
    public String wardenAttemptId="";
    public int wardenAttemptDeaths;
    public String dragonAttemptId="";
    public int dragonAttemptDeaths;
    public int deathsWhenBossStarted;
    public Set<String> smithing=new HashSet<>();
    /** Registro persistente por jugador: biomas visitados realmente en cada dimension. */
    public Set<String> overworldBiomes = new HashSet<>();
    public Set<String> netherBiomes = new HashSet<>();
    public Set<String> visitedStructures = new HashSet<>();
    /** Sólo los descubrimientos hechos desde Alpha 2 tienen fecha y coordenadas. */
    public java.util.Map<String,String> discoveryDetails=new java.util.HashMap<>();
    /** Registro de jefes conocidos sin alterar los requisitos de progresión. */
    public Set<String> discoveredBosses=new HashSet<>();

    /** Registros de la futura maestria, misiones y alquimia. */
    public Set<String> completedMissions = new HashSet<>();
    public Set<String> secretChallenges = new HashSet<>();
    /** Contratos opcionales: progreso empieza al aceptar, separado de las eras. */
    public java.util.Map<String,Integer> contractProgress=new java.util.HashMap<>();
    public Set<String> activeContracts=new HashSet<>();
    public Set<String> claimedContracts=new HashSet<>();
    /** Recompensas de reputación reclamadas. Los puntos se derivan de contratos históricos. */
    public Set<String> claimedReputationRanks=new HashSet<>();
    /** Contratos épicos independientes: nunca se borran los contratos normales. */
    public java.util.Set<String> epicActive=new java.util.HashSet<>();
    public java.util.Set<String> epicClaimed=new java.util.HashSet<>();
    public java.util.Map<String,String> epicOffers=new java.util.HashMap<>();
    public java.util.Map<String,Integer> epicProgress=new java.util.HashMap<>();
    public long epicSequence=0L;
    /** Integración opcional Thoridrop. La migración conserva la era ya alcanzada. */
    public boolean thoridropMigrated=false;
    public int thoridropGrandfatherEra=0;
    public Set<Integer> thoridropTributes=new HashSet<>();
    public transient boolean thoridropGateActive=false;
    public transient int thoridropNormalWins=0,thoridropMythicWins=0,thoridropLegendaryWins=0;
    public transient long thoridropLastRefresh=-1;


    /** Tres ofertas persistentes por categoria. Los IDs son instancias únicas. */
    public java.util.Map<String,String> contractOffers=new java.util.HashMap<>();
    public java.util.Map<String,java.util.List<String>> contractRecentTemplates=new java.util.HashMap<>();
    /** Tipos de biomas y estructuras visitados desde la aceptación de cada contrato. */
    public java.util.Map<String,java.util.Set<String>> contractExplorationSeen=new java.util.HashMap<>();
    public long contractSequence=0L;


    public Set<String> potionEffects = new HashSet<>();
    public Set<String> potionTechniques = new HashSet<>();
    public int brewingBatches;
    public long lastBrewExtractionTick=-100000;
    public int toolWood, toolStone, toolCopper, toolIron, toolDiamond;
    public Set<String> toolTypes = new HashSet<>();
    public int naturalCopperOre, naturalDiamondOre;
    public int naturalAncientDebris;
    /** One-time migration: older saves retain the era earned before hard-mode gates. */
    public boolean exigentMigrated=false;
    public int exigentGrandfatherEra=0;
    public int nightsSurvived;
    public int nightTicksObserved;
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
    private int distinctTools(String material) {
        return (int) toolTypes.stream().filter(item -> item.startsWith(material + "_")).count();
    }
    public int toolLevel() {
        if (toolWood < 50) return 0;
        if (toolStone < 150 || distinctTools("stone") < 3) return 1;
        if (toolCopper < 250 || distinctTools("copper") < 3) return 2;
        if (toolIron < 400 || distinctTools("iron") < 3) return 3;
        if (toolDiamond < 600 || distinctTools("diamond") < 3) return 4;
        return 5;
    }
    public int alchemyLevel() {
        if (brewingBatches < 3 || potionEffects.size() < 2 || !crafted.contains("minecraft:brewing_stand")) return 0;
        if (brewingBatches < 8 || potionEffects.size() < 5
            || !potionEffects.contains("minecraft:healing")
            || !potionEffects.contains("minecraft:strength")) return 1;
        if (brewingBatches < 15 || potionEffects.size() < 8 ||
            !potionTechniques.containsAll(Set.of("extended", "strong", "splash"))) return 2;
        if (brewingBatches < 30 || potionEffects.size() < 12 ||
            !potionEffects.containsAll(Set.of("minecraft:invisibility","minecraft:slow_falling","minecraft:turtle_master"))) return 3;
        if (brewingBatches < 50 || potionEffects.size() < 15 ||
            lingeringBrews < 5) return 4;
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
    public int legacyEra() {
        if (!craftedTools("wooden") || !advancements.contains("minecraft:story/mine_stone") || toolLevel() < 1 || !completedMissions.contains("wood")) return 0;
        if (!craftedTools("stone") || !craftedArmor("leather") || tradeCount < 1 || !visitedVillage || toolLevel() < 2 || !completedMissions.contains("stone") || agricultureLevel() < 1 || husbandryLevel() < 1) return 1;
        if (!craftedTools("copper") || !craftedArmor("copper") || !tradedEquipment("iron") ||
                !advancements.contains("minecraft:story/smelt_iron") || toolLevel() < 3 || naturalCopperOre < 32 || agricultureLevel() < 2 || husbandryLevel() < 2) return 2;
        if (!craftedTools("iron") || !craftedArmor("iron") || !tradedEquipment("diamond") ||
                !advancements.contains("minecraft:story/mine_diamond") || toolLevel() < 4 || naturalDiamondOre < 5 || !completedMissions.contains("iron") || agricultureLevel() < 3 || husbandryLevel() < 3) return 3;
        if (!craftedTools("diamond") || !craftedArmor("diamond") ||
                !advancements.contains("minecraft:story/enter_the_nether") || toolLevel() < 5 ||
                !completedMissions.contains("ancient_debris")) return 4;
        return 5;
    }
    /** Both integrations are additive, independent and never revoke previously earned eras. */
    public int era() {
        if(adminEraOverride>=0 && adminEraOverride<ERA_NAMES.length)
            return adminEraOverride;
        int unlocked=legacyEra();
        if(thoridropGateActive){
            int result=Math.min(unlocked,2);
            for(int target=3;target<=unlocked;target++){
                if(target<=thoridropGrandfatherEra ||
                   (thoridropTributes!=null&&thoridropTributes.contains(target) &&
                    switch(target){case 3 -> thoridropNormalWins>=2;
                                   case 4 -> thoridropMythicWins>=1;
                                   case 5 -> thoridropLegendaryWins>=1;
                                   default -> true;}))result=target;
                else break;
            }
            unlocked=Math.max(result,Math.min(thoridropGrandfatherEra,unlocked));
        }
        // First read after the update snapshots the prior unlocked era.
        // Historical eras stay unlocked even when new objectives are pending.
        if(!exigentMigrated)return unlocked;
        int result=Math.min(unlocked,2);
        for(int target=3;target<=unlocked;target++){
            if(target<=exigentGrandfatherEra || ExigentEraRules.ready(this,target))
                result=target;
            else break;
        }
        unlocked=Math.max(result,Math.min(exigentGrandfatherEra,unlocked));
        if(!nicktaleBossGateActive || !nicktaleBossMigrated)return unlocked;
        result=Math.min(unlocked,2);
        for(int target=3;target<=unlocked;target++){
            if(target<=nicktaleBossGrandfatherEra ||
                NicktaleBossGate.defeated(this,target))result=target;
            else break;
        }
        return Math.max(result,Math.min(nicktaleBossGrandfatherEra,unlocked));
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
        return canNether(40,20);
    }
    public boolean canNether(EvolutionConfig config) {
        return canNether(config.netherAdvancements,config.netherBiomes);
    }
    private boolean canNether(int achievements,int biomeCount){
        boolean original=era() >= 4 && fishingLevel() >= 3 &&
               bossKills.contains("minecraft:warden") &&
               agricultureLevel() >= 3 && husbandryLevel() >= 3 &&
               advancements.size() >= achievements &&
               overworldBiomes.size() >= biomeCount && explorationLevel() >= 4;
        return original && EliteMobGate.ready(this,false) && (!nicktaleDimensionGateActive || !nicktaleDimensionMigrated ||
               nicktaleNetherGrandfathered || NicktaleDimensionGate.netherReady(this));
    }
    public boolean canEnd() {
        return canEnd(70,40,20);
    }
    public boolean canEnd(EvolutionConfig config) {
        return canEnd(config.endAdvancements,config.netherAdvancements,config.netherBiomes);
    }
    private boolean canEnd(int endAdvancements,int netherAdvancements,int netherBiomes){
        boolean original=canNether(netherAdvancements,netherBiomes) &&
               fishingLevel() == 5 && explorationLevel() >= 5 &&
               alchemyLevel() >= 3 && bossKills.contains("minecraft:wither") &&
               agricultureLevel() >= 5 && husbandryLevel() >= 5 &&
               advancements.size() >= endAdvancements &&
               advancements.contains("minecraft:nether/explore_nether");
        return original && (!nicktaleDimensionGateActive || !nicktaleDimensionMigrated ||
               nicktaleEndGrandfathered || NicktaleDimensionGate.endReady(this));
    }
    /**
     * Clasificacion por identificador de registro. Funciona sin depender de los JAR
     * externos y conserva el comportamiento original para las herramientas vanilla.
     *
     * toniweaponry: armas con prefijo de material. Sus escudos y municion
     * no tienen un material inequívoco y por ahora no se restringen.
     * nicktaleepicdungeons: armaduras de jefes y siete armas únicas.
     * elitemobs: solo agrega polvos; no introduce equipamiento bloqueable.
     */
    public static int requiredEra(String item) {
        if(item==null)return 0;
        // La cota de malla se obtiene por botines/intercambios y no sigue
        // el patron "iron_*", pero debe exigir exactamente la Era de Hierro.
        if(item.startsWith("minecraft:chainmail_") &&
            (item.endsWith("_helmet") || item.endsWith("_chestplate") ||
             item.endsWith("_leggings") || item.endsWith("_boots")))return 3;
        int colon=item.indexOf(':');
        if(colon<1 || colon==item.length()-1)return 0;
        String namespace=item.substring(0,colon);
        String name=item.substring(colon+1);
        if(namespace.equals("nicktaleepicdungeons")) {
            boolean armor=name.endsWith("_helmet") || name.endsWith("_chestplate") ||
                name.endsWith("_leggings") || name.endsWith("_boots");
            boolean weapon=java.util.Set.of("black_knight_runeblade","blueflame_axe",
                "buried_great_axe","cinderhorn_tail_blade","rune_greataxe",
                "shattered_spear","voidborne_scythe").contains(name);
            if(!armor && !weapon)return 0;
            // Equipo de jefes: requiere Diamante; los enemigos finales, Netherita.
            if(name.startsWith("voidborne_") || name.startsWith("eclipse_colossus_") ||
               name.startsWith("runebound_colossus_") || name.startsWith("abyssal_shagbeast_") ||
               name.startsWith("cerberus_") || name.startsWith("cinderhorn_demon_") ||
               name.startsWith("sultan_eternal_sands_") || name.startsWith("blueflame_elder_") ||
               name.equals("voidborne_scythe") || name.equals("blueflame_axe") ||
               name.equals("cinderhorn_tail_blade")) return 5;
            return 4;
        }
        boolean equipment=namespace.equals("minecraft")
            ? java.util.Arrays.stream(new String[]{"_pickaxe","_axe","_sword",
                "_shovel","_hoe","_helmet","_chestplate","_leggings","_boots"})
                .anyMatch(name::endsWith)
            : namespace.equals("toniweaponry")
                ? name.matches("(?:wooden|stone|copper|iron|golden|gold|diamond|netherite)_(?:dagger|rapier|sabre|katana|cutlass|longsword|greatsword|battleaxe|warhammer|flanged_mace|morning_star|halberd|spear|scythe)")
                : (name.matches("(?:wooden|stone|copper|iron|golden|gold|diamond|netherite)_.+")
                   && java.util.Arrays.stream(new String[]{"_sword","_axe","_pickaxe",
                       "_hoe","_shovel","_helmet","_chestplate","_leggings","_boots",
                       "_dagger","_rapier","_sabre","_katana","_cutlass","_longsword",
                       "_greatsword","_battleaxe","_warhammer","_mace","_morning_star",
                       "_halberd","_spear","_scythe","_blade"})
                       .anyMatch(name::endsWith));
        if(!equipment)return 0;
        if(name.startsWith("netherite_"))return 5;
        if(name.startsWith("diamond_"))return 4;
        if(name.startsWith("iron_") || name.startsWith("golden_") || name.startsWith("gold_"))return 3;
        if(name.startsWith("copper_"))return 2;
        if(name.startsWith("stone_") || name.startsWith("leather_"))return 1;
        return 0;
    }
}
