package es.evolutioncraft;

import com.google.gson.JsonObject;
import java.util.List;

/**
 * Nether: 3 Overworld bosses + 4 minibosses.
 * End: every 8 main bosses + every 11 minibosses in the Overworld,
 * plus two Nether bosses. Wither and Warden remain in Progress.
 * Nicktale Epic Dungeons 0.23.0 entity registry IDs.
 */
public final class NicktaleDimensionGate {
    private NicktaleDimensionGate(){}

    public record Boss(String id,String label,String structure){}

    private static Boss boss(String key,String label,String structure){
        return new Boss("nicktaleepicdungeons:"+key,label,structure);
    }

    public static final List<Boss> MAIN=List.of(
        boss("mutant_zombie","Zombi Mutante","Relicario de Cobre / Gruta de los Ecos Cristalinos"),
        boss("buried_axe_knight","Caballero del Hacha Sepultada","Coliseo de los Condenados"),
        boss("blueflame_elder","Anciano de la Llama Azul","Camposanto de las Animas"),
        boss("voidborne_lich","Lich del Vacio","Aguja del Nigromante"),
        boss("black_knight","Caballero Negro","Torreon de Obsidiana"),
        boss("armored_skeleton","Caballero Esqueletico","Castillo del Rey Esqueleto"),
        boss("cinderhorn_demon","Demonio de Cuernos de Brasa","Volcan del Senor de las Brasas"),
        boss("runebound_colossus","Coloso de las Runas","Fortaleza de la Calavera")
    );
    public static final List<Boss> MINI=List.of(
        boss("don_cascabel","Don Cascabel","Torre del Reloj de Cobre / Obelisco del Desierto"),
        boss("fortress_guard","Guardia de la Fortaleza","Bastion de Ladrillo / Baluarte Congelado"),
        boss("gravebreaker_brute","Quiebratumbas","Mina del Rey Enano / Laberinto del Minotauro"),
        boss("violet_flying_witch","Bruja del Vuelo Violeta","Torre del Eco Profundo / Torre de Brasas"),
        boss("vix_eclipse_fox","Vix, Zorro del Eclipse","Ciudad-Arbol Elfica / Ciudadela Celeste"),
        boss("abyssal_shagbeast","Barbafosa, Terror de la Bajamar","Catedral Abismal / Galeon Fantasma"),
        boss("cerberus","Cerbero de Llamas","Cripta de Cenizas / Catacumbas Rojas"),
        boss("zombie_hulk","Zombi Hulk","Cripta del Rey Olvidado / Tumulo Helado"),
        boss("sultan_eternal_sands","Sultan de las Arenas Eternas","Palacio del Sultan"),
        boss("eclipse_colossus","Coloso del Eclipse","Montana Enana / Torre de Babel"),
        boss("widow_of_the_well","Viuda del Pozo","Ciudad Hundida")
    );
    public static final List<Boss> NETHER=List.of(
        boss("fire_centaur","Centauro Arquero de Fuego","Bastion de las Llamas Gemelas (Nether)"),
        boss("volcanic_golem","Golem Volcanico","Bastion de las Llamas Gemelas (Nether)")
    );

    private static final List<Boss> NETHER_ENTRY=List.of(
        MAIN.get(0),MAIN.get(1),MAIN.get(2),
        MINI.get(0),MINI.get(1),MINI.get(2),MINI.get(3)
    );

    public static boolean isRequiredBoss(String id){
        return MAIN.stream().anyMatch(b->b.id().equals(id)) ||
               MINI.stream().anyMatch(b->b.id().equals(id)) ||
               NETHER.stream().anyMatch(b->b.id().equals(id));
    }

    public static boolean defeated(Progress p,String id){
        return (p.nicktaleDimensionBossKills!=null &&
                p.nicktaleDimensionBossKills.contains(id)) ||
               (p.nicktaleEraBossKills!=null &&
                p.nicktaleEraBossKills.contains(id));
    }
    public static int count(Progress p,List<Boss> bosses){
        return (int)bosses.stream().filter(b->defeated(p,b.id())).count();
    }
    public static boolean netherReady(Progress p){
        return NETHER_ENTRY.stream().allMatch(b->defeated(p,b.id()));
    }
    public static boolean endReady(Progress p){
        return MAIN.stream().allMatch(b->defeated(p,b.id())) &&
               MINI.stream().allMatch(b->defeated(p,b.id())) &&
               NETHER.stream().allMatch(b->defeated(p,b.id()));
    }
    private static void goal(JsonObject group,Boss boss,boolean complete){
        JsonObject o=new JsonObject();
        o.addProperty("text","Derrotar a "+boss.label());
        o.addProperty("detail","Estructura: "+boss.structure());
        o.addProperty("done",complete);
        group.getAsJsonArray("goals").add(o);
    }
    public static void addNetherGoals(JsonObject group,Progress p){
        for(Boss b:NETHER_ENTRY)goal(group,b,
            p.nicktaleNetherGrandfathered || defeated(p,b.id()));
    }
    public static void addEndGoals(JsonObject group,Progress p){
        for(Boss b:MAIN)goal(group,b,p.nicktaleEndGrandfathered||defeated(p,b.id()));
        for(Boss b:MINI)goal(group,b,p.nicktaleEndGrandfathered||defeated(p,b.id()));
        for(Boss b:NETHER)goal(group,b,p.nicktaleEndGrandfathered||defeated(p,b.id()));
    }
}
