package es.evolutioncraft;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Optional Thoridrop bridge without a runtime dependency on its classes.
 * Thoridrop publishes authoritative per-player completed challenge counters:
 * wins_common, wins_rare, wins_legendary. Existing victories count.
 */
public final class ThoridropTribute {
    private ThoridropTribute(){}
    public static final String FRAGMENT="thoridrop:supply_fragment";
    public static int required(int target){
        return switch(target){case 3->5;case 4->20;case 5->50;default->0;};
    }
    private static int victories(Progress p,int target){
        return switch(target){
            case 3->p.thoridropNormalWins;
            case 4->p.thoridropMythicWins;
            case 5->p.thoridropLegendaryWins;
            default->0;
        };
    }
    private static int requiredVictories(int target){return target==3?2:1;}
    public static boolean active(ServerPlayer player){
        return EvolutionConfig.get(player).thoridropEnabled &&
            ModList.get().isLoaded("thoridrop");
    }
    private static int stat(UUID id,String field){
        try{
            Class<?> stats=Class.forName("com.langamaestro.thoridrop.DropStatistics");
            Method method=stats.getMethod("value",UUID.class,String.class);
            return Math.max(0,(Integer)method.invoke(null,id,field));
        }catch(ReflectiveOperationException|LinkageError|SecurityException e){
            return 0; // never trust fabricated victories
        }
    }
    public static void sync(ServerPlayer player,Progress p){
        boolean enabled=active(player);
        p.thoridropGateActive=enabled;
        if(!enabled)return;
        if(!p.thoridropMigrated){
            // Snapshot of the OLD earned era before enabling the new gate.
            p.thoridropGrandfatherEra=p.legacyEra();
            p.thoridropMigrated=true;
            if(p.thoridropTributes==null)p.thoridropTributes=new java.util.HashSet<>();
            ProgressStore.save(player);
        }
        long tick=player.level().getGameTime();
        if(p.thoridropLastRefresh<0 || tick<p.thoridropLastRefresh ||
            tick-p.thoridropLastRefresh>=40){
            p.thoridropNormalWins=stat(player.getUUID(),"wins_common");
            p.thoridropMythicWins=stat(player.getUUID(),"wins_rare");
            p.thoridropLegendaryWins=stat(player.getUUID(),"wins_legendary");
            p.thoridropLastRefresh=tick;
        }
    }
    public static int inventory(ServerPlayer player){
        int count=0;
        for(int i=0;i<36;i++){
            ItemStack stack=player.getInventory().getItem(i);
            if(!stack.isEmpty()&&BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(FRAGMENT))
                count+=stack.getCount();
        }
        return count;
    }
    public static boolean fulfilled(Progress p,int target){
        return target<=p.thoridropGrandfatherEra ||
            (p.thoridropTributes!=null&&p.thoridropTributes.contains(target)&&
             victories(p,target)>=requiredVictories(target));
    }
    public static boolean deliver(ServerPlayer player,int target){
        if(target<3||target>5||!active(player))return false;
        Progress p=ProgressStore.get(player);
        if(target<=p.thoridropGrandfatherEra || p.thoridropTributes.contains(target))
            return false;
        // Do not allow prepaying later eras; only the next locked transition.
        if(p.era()!=target-1)return false;
        int need=required(target);
        if(inventory(player)<need){
            player.sendSystemMessage(Component.literal("§c[EvolutionCraft] Fragmentos insuficientes: "+
                inventory(player)+"/"+need),true);
            return false;
        }
        int left=need;
        for(int i=0;i<36&&left>0;i++){
            ItemStack stack=player.getInventory().getItem(i);
            if(!stack.isEmpty()&&BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(FRAGMENT)){
                int take=Math.min(left,stack.getCount());
                stack.shrink(take);
                left-=take;
            }
        }
        player.getInventory().setChanged();
        p.thoridropTributes.add(target);
        ProgressStore.save(player);
        player.sendSystemMessage(Component.literal("§6[EvolutionCraft] Tributo entregado para "+
            Progress.ERA_NAMES[target]+". ¡Las victorias de Thoridrop también son necesarias!"),true);
        return true;
    }
    public static JsonObject stage(ServerPlayer player,Progress p,int target){
        JsonObject result=new JsonObject();
        boolean enabled=p.thoridropGateActive && target>=3&&target<=5;
        result.addProperty("enabled",enabled);
        result.addProperty("stage",target);
        if(!enabled)return result;
        int need=required(target);
        boolean paid=target<=p.thoridropGrandfatherEra ||
            (p.thoridropTributes!=null&&p.thoridropTributes.contains(target));
        int have=paid?need:Math.min(need,inventory(player));
        int wins=victories(p,target),winGoal=requiredVictories(target);
        result.addProperty("paid",paid);
        result.addProperty("have",have);
        result.addProperty("required",need);
        result.addProperty("wins",wins);
        result.addProperty("winGoal",winGoal);
        result.addProperty("done",fulfilled(p,target));
        result.addProperty("canDeliver",!paid&&p.era()==target-1&&have>=need);
        result.addProperty("kind",target==3?"normales":target==4?"miticos":"legendarios");
        return result;
    }
}
