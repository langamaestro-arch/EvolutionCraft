package es.evolutioncraft;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Optional 26.3 Sophisticated Backpacks integration.
 *
 * No dependency is linked at class load time. Only backpacks held in regular
 * player inventory slots 0..35, allowed by the current era, are eligible.
 * Extraction is simulated before committing any movement.
 */
public final class ContractMaterials {
    private ContractMaterials(){}

    private record Source(int parentSlot,int childSlot,int amount,Object handler) {}
    private static String itemId(ItemStack stack){
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
    private static boolean allowedBackpack(ServerPlayer player,ItemStack stack){
        String id=itemId(stack);
        return id.startsWith("sophisticatedbackpacks:")&&id.endsWith("_backpack")
            && RecipeEraRules.requiredEra(id)<=ProgressStore.get(player).era();
    }
    private static Object handler(ItemStack stack){
        try{
            // Keep the mod optional, and use the exact public wrapper API.
            Class<?> wrapper=Class.forName(
                "net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper");
            Object instance=wrapper.getMethod("fromStack",ItemStack.class).invoke(null,stack);
            return instance.getClass().getMethod("getInventoryHandler").invoke(instance);
        }catch(ReflectiveOperationException | LinkageError | SecurityException e){
            return null;
        }
    }
    private static int slots(Object inv){
        try{return (Integer)inv.getClass().getMethod("getSlots").invoke(inv);}
        catch(ReflectiveOperationException | LinkageError | SecurityException e){return 0;}
    }
    private static ItemStack get(Object inv,int slot){
        try{
            Object result=inv.getClass().getMethod("getStackInSlot",int.class).invoke(inv,slot);
            return result instanceof ItemStack stack?stack:ItemStack.EMPTY;
        }catch(ReflectiveOperationException | LinkageError | SecurityException e){
            return ItemStack.EMPTY;
        }
    }
    private static ItemStack extract(Object inv,int slot,int count,boolean simulate){
        try{
            Object result=inv.getClass().getMethod("extractItem",int.class,int.class,boolean.class)
                .invoke(inv,slot,count,simulate);
            return result instanceof ItemStack stack?stack:ItemStack.EMPTY;
        }catch(ReflectiveOperationException | LinkageError | SecurityException e){
            return ItemStack.EMPTY;
        }
    }

    /** Inventory first; then contents of unlocked carried backpacks. */
    private static List<Source> sources(ServerPlayer player,String target,boolean backpackAllowed){
        List<Source> found=new ArrayList<>();
        for(int i=0;i<36;i++){
            ItemStack stack=player.getInventory().getItem(i);
            if(!stack.isEmpty()&&itemId(stack).equals(target))
                found.add(new Source(i,-1,stack.getCount(),null));
        }
        if(!backpackAllowed)return found;
        for(int i=0;i<36;i++){
            ItemStack backpack=player.getInventory().getItem(i);
            if(backpack.isEmpty() || !allowedBackpack(player,backpack))continue;
            Object inv=handler(backpack);
            if(inv==null)continue;
            int size=Math.min(512,slots(inv));
            for(int slot=0;slot<size;slot++){
                ItemStack stack=get(inv,slot);
                if(stack.isEmpty()||!itemId(stack).equals(target))continue;
                int count=stack.getCount();
                if(count>0)found.add(new Source(i,slot,count,inv));
            }
        }
        return found;
    }
    public static int count(ServerPlayer player,String target){
        long count=0;
        for(Source source:sources(player,target,true))
            count+=source.amount();
        return (int)Math.min(Integer.MAX_VALUE,count);
    }

    /**
     * Two-phase check: preflight all sources (including simulated backpack
     * extraction), then perform extraction. No operation if quantity is short.
     * The caller is responsible for marking the contract completed afterwards.
     */
    public static boolean take(ServerPlayer player,String target,int needed){
        if(needed<=0)return false;
        List<Source> all=sources(player,target,true);
        int remaining=needed;
        List<Source> selected=new ArrayList<>();
        for(Source source:all){
            if(remaining==0)break;
            int amount=Math.min(remaining,source.amount());
            if(source.childSlot()>=0){
                ItemStack sample=extract(source.handler(),source.childSlot(),amount,true);
                if(sample.getCount()<amount || !itemId(sample).equals(target))continue;
            }
            selected.add(new Source(source.parentSlot(),source.childSlot(),amount,source.handler()));
            remaining-=amount;
        }
        if(remaining>0)return false;
        // Main-inventory materials are consumed last, keeping them available
        // if an optional backpack handler reports an extraction failure.
        for(Source source:selected){
            if(source.childSlot()<0)continue;
            ItemStack got=extract(source.handler(),source.childSlot(),source.amount(),false);
            if(got.getCount()!=source.amount() || !itemId(got).equals(target)){
                // Defensive fail closed: never mark the contract delivered.
                // Unexpected third-party handler failures need manual review.
                return false;
            }
        }
        for(Source source:selected){
            if(source.childSlot()>=0)continue;
            ItemStack stack=player.getInventory().getItem(source.parentSlot());
            if(stack.getCount()<source.amount()||!itemId(stack).equals(target))return false;
            stack.shrink(source.amount());
        }
        player.getInventory().setChanged();
        return true;
    }
}
