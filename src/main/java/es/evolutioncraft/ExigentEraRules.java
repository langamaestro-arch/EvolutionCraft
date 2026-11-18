package es.evolutioncraft;

import com.google.gson.JsonObject;

/**
 * Extra challenges only for unlocking Iron, Diamond and Netherite.
 * No timers, no random requirements. All counters are authoritative
 * existing gameplay telemetry, except natural ancient-debris mining,
 * which is saved per player. Never changes the Wood/Stone/Copper gates.
 */
public final class ExigentEraRules {
    private ExigentEraRules(){}

    public static boolean ready(Progress p,int target){
        return switch(target){
            case 3 -> p.naturalCopperOre>=96 &&
                p.overworldBiomes.size()>=10 &&
                p.visitedStructures.size()>=2 &&
                p.fish>=60 &&
                p.harvests>=100 &&
                p.breedings>=15;
            case 4 -> p.naturalDiamondOre>=16 &&
                p.explorationLevel()>=3 &&
                p.fish>=120 &&
                p.toolIron>=600 &&
                p.breedings>=50;
            case 5 -> p.naturalAncientDebris>=8 &&
                p.explorationLevel()>=4 &&
                p.fish>=250 &&
                p.toolDiamond>=900 &&
                p.netherBiomes.size()>=3;
            default -> true;
        };
    }
    private static void goal(JsonObject group,String label,boolean done){
        JsonObject entry=new JsonObject();
        entry.addProperty("text",label);
        entry.addProperty("done",done);
        group.getAsJsonArray("goals").add(entry);
    }
    public static void addGoals(JsonObject group,Progress p,int target){
        switch(target){
            case 3 -> {
                goal(group,"EXIGENTE: cobre natural ("+Math.min(p.naturalCopperOre,96)+"/96)",
                    p.naturalCopperOre>=96);
                goal(group,"EXIGENTE: biomas del mundo ("+Math.min(p.overworldBiomes.size(),10)+"/10)",
                    p.overworldBiomes.size()>=10);
                goal(group,"EXIGENTE: estructuras descubiertas ("+Math.min(p.visitedStructures.size(),2)+"/2)",
                    p.visitedStructures.size()>=2);
                goal(group,"EXIGENTE: peces pescados ("+Math.min(p.fish,60)+"/60)",p.fish>=60);
                goal(group,"EXIGENTE: cosechas ("+Math.min(p.harvests,100)+"/100)",
                    p.harvests>=100);
                goal(group,"EXIGENTE: crias de animales ("+Math.min(p.breedings,15)+"/15)",
                    p.breedings>=15);
            }
            case 4 -> {
                goal(group,"EXIGENTE: diamantes naturales ("+Math.min(p.naturalDiamondOre,16)+"/16)",
                    p.naturalDiamondOre>=16);
                goal(group,"EXIGENTE: exploracion nivel III",p.explorationLevel()>=3);
                goal(group,"EXIGENTE: peces pescados ("+Math.min(p.fish,120)+"/120)",p.fish>=120);
                goal(group,"EXIGENTE: usar herramientas de hierro ("+Math.min(p.toolIron,600)+"/600)",
                    p.toolIron>=600);
                goal(group,"EXIGENTE: crias de animales ("+Math.min(p.breedings,50)+"/50)",
                    p.breedings>=50);
            }
            case 5 -> {
                goal(group,"EXIGENTE: escombros ancestrales naturales ("+Math.min(p.naturalAncientDebris,8)+"/8)",
                    p.naturalAncientDebris>=8);
                goal(group,"EXIGENTE: exploracion nivel IV",p.explorationLevel()>=4);
                goal(group,"EXIGENTE: peces pescados ("+Math.min(p.fish,250)+"/250)",p.fish>=250);
                goal(group,"EXIGENTE: usar herramientas de diamante ("+Math.min(p.toolDiamond,900)+"/900)",
                    p.toolDiamond>=900);
                goal(group,"EXIGENTE: biomas del Nether ("+Math.min(p.netherBiomes.size(),3)+"/3)",
                    p.netherBiomes.size()>=3);
            }
            default -> {}
        }
    }
}
