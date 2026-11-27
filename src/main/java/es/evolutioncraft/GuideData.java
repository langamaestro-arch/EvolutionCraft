package es.evolutioncraft;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;

/** Snapshot generado por el servidor, único origen de verdad de la guía. */
public final class GuideData {
    private GuideData() {}
    private static JsonObject group(JsonObject root, String key, String name) {
        JsonObject result = new JsonObject();
        result.addProperty("name", name);
        result.add("goals", new JsonArray());
        root.add(key, result);
        return result;
    }
    private static void goal(JsonObject group, String name, boolean done) {
        JsonObject g = new JsonObject();
        g.addProperty("text", name);
        g.addProperty("done", done);
        group.getAsJsonArray("goals").add(g);
    }
    private static void equipment(JsonObject group, Progress p, String material, boolean armor, String label) {
        String[] suffixes = armor ? new String[]{"helmet", "chestplate", "leggings", "boots"} :
                new String[]{"pickaxe", "axe", "sword", "shovel", "hoe"};
        String[] names = armor ? new String[]{"Casco", "Peto", "Pantalones", "Botas"} :
                new String[]{"Pico", "Hacha", "Espada", "Pala", "Azada"};
        for (int i=0; i<suffixes.length; i++) goal(group, names[i]+" de "+label,
                p.crafted.contains("minecraft:"+material+"_"+suffixes[i]));
    }
    private static boolean trade(Progress p, String material) {
        return p.trades.stream().anyMatch(s -> s.startsWith("minecraft:"+material+"_") &&
                (s.endsWith("_pickaxe") || s.endsWith("_axe") || s.endsWith("_sword")
                || s.endsWith("_shovel") || s.endsWith("_hoe")));
    }
    public static String snapshot(ServerPlayer player) {
        Progress p=ProgressStore.get(player);
        EvolutionConfig config=EvolutionConfig.get(player);
        JsonObject root=new JsonObject();
        root.addProperty("era",p.era());
        root.addProperty("fishing",p.fishingLevel());
        root.addProperty("exploration",p.explorationLevel());
        root.addProperty("tools",p.toolLevel());
        root.addProperty("alchemy",p.alchemyLevel());
        root.addProperty("agriculture",p.agricultureLevel());
        root.addProperty("husbandry",p.husbandryLevel());
        root.addProperty("nether",p.canNether(config));
        root.addProperty("end",p.canEnd(config));
        root.addProperty("advancements",p.advancements.size());
        root.addProperty("fish",p.fish);
        root.add("discoveries",DiscoveryJournal.snapshot(p));
        root.add("contracts",AdventureContracts.snapshot(player));
        root.add("epicContracts",EpicContracts.snapshot(player));
        root.add("reputation",AdventurerReputation.snapshot(p));
        JsonObject eras=group(root,"eras","Progreso de eras");
        String[] mats={"wooden","stone","copper","iron","diamond"};
        String[] labels={"madera","piedra","cobre","hierro","diamante"};
        for(int era=0;era<5;era++) {
            JsonObject stage=new JsonObject();
            stage.addProperty("name",Progress.ERA_NAMES[era+1]);
            JsonArray goals=new JsonArray();
            stage.add("goals",goals);
            JsonObject tmp=new JsonObject();
            tmp.add("goals",goals);
            equipment(tmp,p,mats[era],false,labels[era]);
            if(era==1) equipment(tmp,p,"leather",true,"cuero");
            if(era>=2) equipment(tmp,p,mats[era],true,labels[era]);
            switch(era) {
                case 0 -> {
                    goal(tmp,"Avance: conseguir piedra",p.advancements.contains("minecraft:story/mine_stone"));
                    goal(tmp,"Mision: primera noche ("+(p.nightsSurvived>=1?"SI":"NO")+
                        ") y hoguera fabricada ("+
                        (p.crafted.contains("minecraft:campfire")?"SI":"NO")+")",
                        p.completedMissions.contains("wood"));
                    goal(tmp,"Maestria herramientas I",p.toolLevel()>=1);
                }
                case 1 -> {
                    goal(tmp,"Intercambiar con aldeanos ("+p.tradeCount+"/1)",p.tradeCount>=1);
                    goal(tmp,"Mision: descubrir una aldea y comerciar",p.completedMissions.contains("stone"));
                    goal(tmp,"Maestria herramientas II",p.toolLevel()>=2);
                }
                case 2 -> {
                    goal(tmp,"Comerciar una herramienta de hierro",trade(p,"iron"));
                    goal(tmp,"Avance: fundir hierro",p.advancements.contains("minecraft:story/smelt_iron"));
                    goal(tmp,"Extraer cobre ("+p.naturalCopperOre+"/32)",p.naturalCopperOre>=32);
                    goal(tmp,"Maestria herramientas III",p.toolLevel()>=3);
                }
                case 3 -> {
                    goal(tmp,"Comerciar una herramienta de diamante",trade(p,"diamond"));
                    goal(tmp,"Avance: conseguir diamante",p.advancements.contains("minecraft:story/mine_diamond"));
                    goal(tmp,"Extraer mineral de diamante ("+p.naturalDiamondOre+"/5)",p.naturalDiamondOre>=5);
                    goal(tmp,"Visitar ciudad antigua o camaras de prueba",p.completedMissions.contains("deep_structure"));
                    goal(tmp,"Maestria herramientas IV",p.toolLevel()>=4);
                }
                case 4 -> {
                    goal(tmp,"Avance: entrar al Nether",p.advancements.contains("minecraft:story/enter_the_nether"));
                    goal(tmp,"Extraer escombro ancestral",p.completedMissions.contains("ancient_debris"));
                    goal(tmp,"Maestria herramientas V",p.toolLevel()>=5);
                }
            }
            // Same server-side demanding rules used by Progress.era(): never display
            // a completed goal that cannot actually unlock its target era.
            if(era>=2)ExigentEraRules.addGoals(tmp,p,era+1);
            if(era>=2 && p.nicktaleBossGateActive)
                NicktaleBossGate.addGoal(tmp,p,era+1);
            if(era>=2){
                int target=era+1;
                JsonObject trib=ThoridropTribute.stage(player,p,target);
                if(trib.get("enabled").getAsBoolean()){
                    boolean paid=trib.get("paid").getAsBoolean();
                    boolean ready=trib.get("canDeliver").getAsBoolean();
                    int have=trib.get("have").getAsInt(),required=trib.get("required").getAsInt();
                    int wins=trib.get("wins").getAsInt(),goalWins=trib.get("winGoal").getAsInt();
                    goal(tmp,"Thoridrop: "+trib.get("kind").getAsString()+
                        " ("+Math.min(wins,goalWins)+"/"+goalWins+")",wins>=goalWins);
                    JsonObject delivery=new JsonObject();
                    delivery.addProperty("text","Tributo: fragmentos de suministro "+
                        (paid?"ENTREGADOS":have+"/"+required));
                    delivery.addProperty("done",paid);
                    delivery.addProperty("tribute",target);
                    delivery.addProperty("canDeliver",ready);
                    boolean prior=trib.get("needsPriorEra").getAsBoolean();
                    delivery.addProperty("needsPriorEra",prior);
                    delivery.addProperty("priorEra",trib.get("priorEra").getAsString());
                    delivery.addProperty("description",prior?
                        "Primero desbloquea "+trib.get("priorEra").getAsString():
                        paid?"Tributo entregado para "+Progress.ERA_NAMES[target]:
                        "Fragmentos para desbloquear "+Progress.ERA_NAMES[target]);
                    goals.add(delivery);
                }
            }
            if (era>=1 && era<=3) {
                goal(tmp,"Agricultura nivel "+era,p.agricultureLevel()>=era);
                goal(tmp,"Ganadería nivel "+era,p.husbandryLevel()>=era);
            }
            AdminEraObjectives.apply(stage,p,era+1);
            eras.getAsJsonArray("goals").add(stage);
        }
        JsonObject fish=group(root,"fishingGoals","Maestría de pesca");
        goal(fish,"Nivel I: pescar 25 peces ("+p.fish+"/25)",p.fish>=25);
        goal(fish,"Nivel II: pescar 60 peces ("+p.fish+"/60)",p.fish>=60);
        goal(fish,"Nivel II: 3 especies ("+p.fishSpecies.size()+"/3)",p.fishSpecies.size()>=3);
        goal(fish,"Nivel III: pescar 120 peces ("+p.fish+"/120)",p.fish>=120);
        goal(fish,"Nivel III: 4 especies ("+p.fishSpecies.size()+"/4)",p.fishSpecies.size()>=4);
        goal(fish,"Nivel III: 2 tesoros ("+p.fishingTreasures.size()+"/2)",p.fishingTreasures.size()>=2);
        goal(fish,"Nivel IV: pescar 250 peces ("+p.fish+"/250)",p.fish>=250);
        goal(fish,"Nivel IV: 3 tesoros ("+p.fishingTreasures.size()+"/3)",p.fishingTreasures.size()>=3);
        goal(fish,"Nivel V: pescar 500 peces ("+p.fish+"/500)",p.fish>=500);
        goal(fish,"Nivel V: avance Pesca táctica",p.advancements.contains("minecraft:husbandry/tactical_fishing"));
        JsonObject farm=group(root,"agricultureGoals","Agricultura");
        goal(farm,"Nivel I: cosechar 32 cosechas de trigo ("+p.wheatHarvests+"/32)",p.wheatHarvests>=32);
        goal(farm,"Nivel II: trigo, zanahoria, patata y remolacha ("+p.harvested.stream().filter(s->java.util.Set.of("minecraft:wheat","minecraft:carrots","minecraft:potatoes","minecraft:beetroots").contains(s)).count()+"/4)",p.harvested.containsAll(java.util.Set.of("minecraft:wheat","minecraft:carrots","minecraft:potatoes","minecraft:beetroots")));
        goal(farm,"Nivel III: 250 cosechas ("+p.harvests+"/250)",p.harvests>=250);
        goal(farm,"Nivel III: melones y calabazas",p.harvested.containsAll(java.util.Set.of("minecraft:melon","minecraft:pumpkin")));
        goal(farm,"Nivel IV: 500 cosechas ("+p.harvests+"/500)",p.harvests>=500);
        goal(farm,"Nivel IV: cacao y verrugas del Nether",p.harvested.containsAll(java.util.Set.of("minecraft:cocoa","minecraft:nether_wart")));
        goal(farm,"Nivel V: 1.000 cosechas ("+p.harvests+"/1000)",p.harvests>=1000);
        JsonObject animal=group(root,"husbandryGoals","Ganadería");
        goal(animal,"Nivel I: 5 reproducciones ("+p.breedings+"/5)",p.breedings>=5);
        goal(animal,"Nivel II: criar vacas, ovejas, cerdos y gallinas ("+p.bred.stream().filter(s->java.util.Set.of("minecraft:cow","minecraft:sheep","minecraft:pig","minecraft:chicken").contains(s)).count()+"/4)",p.bred.containsAll(java.util.Set.of("minecraft:cow","minecraft:sheep","minecraft:pig","minecraft:chicken")));
        goal(animal,"Nivel III: 30 reproducciones ("+p.breedings+"/30)",p.breedings>=30);
        goal(animal,"Nivel III: domesticar lobo y gato ("+p.tamed.size()+"/2)",p.tamed.containsAll(java.util.Set.of("minecraft:wolf","minecraft:cat")));
        goal(animal,"Nivel IV: 75 reproducciones ("+p.breedings+"/75)",p.breedings>=75);
        goal(animal,"Nivel IV: criar caballos",p.bred.contains("minecraft:horse"));
        goal(animal,"Nivel V: 150 reproducciones ("+p.breedings+"/150)",p.breedings>=150);
        JsonObject explore=group(root,"explorationGoals","Exploracion");
        goal(explore,"Nivel I: 5 biomas Overworld ("+p.overworldBiomes.size()+"/5)",p.overworldBiomes.size()>=5);
        goal(explore,"Nivel II: 10 biomas ("+p.overworldBiomes.size()+"/10)",p.overworldBiomes.size()>=10);
        goal(explore,"Nivel II: descubrir una aldea",p.visitedVillage);
        goal(explore,"Nivel III: 15 biomas ("+p.overworldBiomes.size()+"/15)",p.overworldBiomes.size()>=15);
        goal(explore,"Nivel III: 2 estructuras ("+p.visitedStructures.size()+"/2)",p.visitedStructures.size()>=2);
        goal(explore,"Nivel IV: 20 biomas ("+p.overworldBiomes.size()+"/20)",p.overworldBiomes.size()>=config.netherBiomes);
        goal(explore,"Nivel IV: 4 estructuras ("+p.visitedStructures.size()+"/4)",p.visitedStructures.size()>=4);
        goal(explore,"Nivel V: 30 biomas ("+p.overworldBiomes.size()+"/30)",p.overworldBiomes.size()>=30);
        goal(explore,"Nivel V: 6 estructuras ("+p.visitedStructures.size()+"/6)",p.visitedStructures.size()>=6);
        goal(explore,"Nivel V: 3 biomas Nether ("+p.netherBiomes.size()+"/3)",p.netherBiomes.size()>=3);
        JsonObject tools=group(root,"toolGoals","Maestria de herramientas");
        goal(tools,"Nivel I: 50 bloques con madera ("+p.toolWood+"/50)",p.toolWood>=50);
        goal(tools,"Nivel II: 150 bloques con piedra ("+p.toolStone+"/150)",p.toolStone>=150);
        goal(tools,"Nivel II: usar 3 herramientas de piedra",p.toolTypes.stream().filter(s->s.startsWith("stone_")).count()>=3);
        goal(tools,"Nivel III: 250 bloques con cobre ("+p.toolCopper+"/250)",p.toolCopper>=250);
        goal(tools,"Nivel III: usar 3 herramientas de cobre",p.toolTypes.stream().filter(s->s.startsWith("copper_")).count()>=3);
        goal(tools,"Nivel IV: 400 bloques con hierro ("+p.toolIron+"/400)",p.toolIron>=400);
        goal(tools,"Nivel IV: usar 3 herramientas de hierro",p.toolTypes.stream().filter(s->s.startsWith("iron_")).count()>=3);
        goal(tools,"Nivel V: 600 bloques con diamante ("+p.toolDiamond+"/600)",p.toolDiamond>=600);
        goal(tools,"Nivel V: usar 3 herramientas de diamante",p.toolTypes.stream().filter(s->s.startsWith("diamond_")).count()>=3);
        JsonObject alchemy=group(root,"alchemyGoals","Alquimia");
        goal(alchemy,"Nivel I: fabricar soporte de pociones",p.crafted.contains("minecraft:brewing_stand"));
        goal(alchemy,"Nivel I: 3 tandas y 2 efectos ("+p.brewingBatches+"/3, "+p.potionEffects.size()+"/2)",p.brewingBatches>=3 && p.potionEffects.size()>=2);
        goal(alchemy,"Nivel II: 8 tandas y 5 efectos ("+p.brewingBatches+"/8, "+p.potionEffects.size()+"/5)",p.brewingBatches>=8 && p.potionEffects.size()>=5);
        goal(alchemy,"Nivel II: curacion y fuerza",p.potionEffects.containsAll(java.util.Set.of("minecraft:healing","minecraft:strength")));
        goal(alchemy,"Nivel III: 15 tandas y 8 efectos ("+p.brewingBatches+"/15, "+p.potionEffects.size()+"/8)",p.brewingBatches>=15 && p.potionEffects.size()>=8);
        goal(alchemy,"Nivel III: redstone, glowstone y arrojadiza",p.potionTechniques.containsAll(java.util.Set.of("extended","strong","splash")));
        goal(alchemy,"Nivel IV: 30 tandas y 12 efectos",p.brewingBatches>=30 && p.potionEffects.size()>=12);
        goal(alchemy,"Nivel IV: invisibilidad, caída lenta y maestro tortuga",p.potionEffects.containsAll(java.util.Set.of("minecraft:invisibility","minecraft:slow_falling","minecraft:turtle_master")));
        goal(alchemy,"Nivel V: 50 tandas y 15 efectos",p.brewingBatches>=50 && p.potionEffects.size()>=15);
        goal(alchemy,"Nivel V: 5 pociones persistentes ("+p.lingeringBrews+"/5)",p.lingeringBrews>=5);
        root.add("netherGoals",DimensionObjectives.group(p,false,config.netherAdvancements,config.netherBiomes,config.endAdvancements));
        root.add("endGoals",DimensionObjectives.group(p,true,config.netherAdvancements,config.netherBiomes,config.endAdvancements));
        JsonObject missions=group(root,"missionGoals","Misiones especiales");
        goal(missions,"Madera: sobrevivir la primera noche y hacer una hoguera",p.completedMissions.contains("wood"));
        goal(missions,"Piedra: encontrar aldea y comerciar",p.completedMissions.contains("stone"));
        goal(missions,"Cobre: extraer 32 minerales de cobre y fundir hierro",p.completedMissions.contains("copper"));
        goal(missions,"Hierro: extraer 5 diamantes y visitar estructuras profundas",p.completedMissions.contains("iron"));
        goal(missions,"Diamante: entrar al Nether y extraer escombro ancestral",p.completedMissions.contains("nether"));
        goal(missions,"Netherita: equipamiento completo y herramientas V",p.completedMissions.contains("netherite"));
        JsonObject secrets=group(root,"secretGoals","Desafíos secretos");
        // El servidor solo revela el nombre y la recompensa cuando el reto está completado.
        // Las pistas explican los requisitos sin revelar el nombre ni la recompensa.
        String[][] hidden={
            {"noche","La noche es mía","Bronce","Permanece despierto desde el anochecer hasta el amanecer durante diez noches limpias. Evita dormir y procura sobrevivir.","Superviviente"},
            {"tesoros","La suerte del pescador","Bronce","Pesca en aguas abiertas con una caña hasta obtener cinco tesoros, como libros encantados o etiquetas. Los peces normales no cuentan.","Pescador afortunado"},
            {"comerciante","Comerciante legendario","Bronce","Visita aldeas y completa cincuenta intercambios con aldeanos. Puedes repetir ofertas y comerciar con distintos oficios.","Mercader"},
            {"tierra","Tierra fértil","Bronce","Recolecta cultivos maduros hasta alcanzar una racha de quinientas cosechas. Replanta y mantén la actividad agrícola.","Granjero incansable"},
            {"animales","Amigo de los animales","Bronce","Domestica un lobo con huesos, un gato con pescado crudo y un caballo montándolo hasta que aparezcan corazones.","Domador"},
            {"diamantes","Corazón de diamante","Plata","Extrae sesenta y cuatro bloques de mena de diamante natural en las profundidades. Los bloques colocados no cuentan.","Minero experto"},
            {"fronteras","Sin fronteras","Plata","Explora cuarenta biomas distintos entre el mundo normal y el Nether. Consulta los descubrimientos en el Diario.","Explorador legendario"},
            {"oceano","Señor de los océanos","Plata","Busca un monumento oceánico, derrota a un guardián anciano y consigue una esponja seca o mojada.","Conquistador del océano"},
            {"arcano","Poder arcano","Plata","Usa la mesa de encantamientos y el yunque para reunir tres encantamientos de nivel III o superior en una misma herramienta o espada.","Encantador"},
            {"elixir","Elixir de la vida","Plata","Bebe cinco tipos distintos de pociones y registra diez efectos diferentes. Experimenta con la alquimia.","Alquimista excepcional"},
            {"terror","El terror tiene miedo","Oro","Encuentra al Warden en la oscuridad profunda y derrótalo sin morir desde que comienzas a combatir contra ese ejemplar.","Cazador de pesadillas"},
            {"infierno","Infierno dominado","Oro","En el Nether, derrota al menos un blaze, un ghast y un esqueleto del Wither. Busca fortalezas y zonas abiertas.","Señor del Nether"},
            {"dragon","Cazador de dragones","Oro","Accede al End y derrota al dragón. Destruye los cristales que lo curan y aprovecha cuando descienda al portal.","Matadragones"},
            {"equipo","El equipo definitivo","Oro","Mejora en la mesa de herrería las cuatro piezas de armadura y las cinco herramientas de netherita. Alcanza maestría de herramientas V.","Maestro herrero"},
            {"evolucion","La evolución definitiva","Oro","Alcanza la era final, consigue nivel V en los seis oficios y completa los otros catorce desafíos secretos.","Leyenda de EvolutionCraft"}
        };
        for(String[] entry:hidden){
            boolean solved=p.secretChallenges.contains(entry[0]);
            JsonObject secret=new JsonObject();
            secret.addProperty("id",entry[0]);
            secret.addProperty("done",solved);
            secret.addProperty("text",solved?entry[1]:"??? · Desafío oculto");
            secret.addProperty("rarity",entry[2]);
            secret.addProperty("hint",solved?"":entry[3]);
            secret.addProperty("reward",solved?entry[4]:"");
            secrets.getAsJsonArray("goals").add(secret);
        }
        root.addProperty("secretCount",p.secretChallenges.size());
        root.addProperty("selectedBadge",p.selectedBadge);
        JsonArray badges=new JsonArray();
        for(String[] entry:hidden)if(p.secretChallenges.contains(entry[0]))badges.add(entry[0]);
        root.add("unlockedBadges",badges);
        root.addProperty("overallPercent",Math.min(100,100*
            (p.era()+p.fishingLevel()+p.agricultureLevel()+p.husbandryLevel()+
            p.explorationLevel()+p.toolLevel()+p.alchemyLevel()+
            p.secretChallenges.size()+p.completedMissions.stream().filter(s->
            java.util.Set.of("wood","stone","copper","iron","nether","netherite").contains(s)).count())
            /56));
        JsonObject rewards=group(root,"rewardGoals","Recompensas del aventurero");
        if(!config.rewardsEnabled){
            root.addProperty("pendingRewards",0);
            return root.toString();
        }
        if(p.claimedRewards==null)p.claimedRewards=new java.util.HashSet<>();
        for(Rewards.Prize prize:Rewards.available(p)){
            JsonObject reward=new JsonObject();
            reward.addProperty("id",prize.id());
            reward.addProperty("text",prize.title());
            reward.addProperty("done",p.claimedRewards.contains(prize.id()));
            reward.addProperty("ready",p.era()>=prize.minEra() &&
                !p.claimedRewards.contains(prize.id()));
            reward.addProperty("xp",prize.xp());
            reward.addProperty("minEra",prize.minEra());
            StringBuilder contents=new StringBuilder();
            for(var stack:prize.items()){
                if(!contents.isEmpty())contents.append(" + ");
                contents.append(stack.getCount()).append("x ")
                    .append(stack.getHoverName().getString());
            }
            reward.addProperty("items",contents.toString());
            rewards.getAsJsonArray("goals").add(reward);
        }
        root.addProperty("pendingRewards",Rewards.available(p).stream()
            .filter(prize->!p.claimedRewards.contains(prize.id()) &&
                p.era()>=prize.minEra()).count());
        return root.toString();
    }
}
