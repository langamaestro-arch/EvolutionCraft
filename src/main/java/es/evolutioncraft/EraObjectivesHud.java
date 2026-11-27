package es.evolutioncraft;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel independiente del diario y del HUD ya existente.
 * Renderiza solo objetivos pendientes; actualizado por el servidor.
 */
@EventBusSubscriber(modid=EvolutionCraft.ID,value=Dist.CLIENT)
public final class EraObjectivesHud {
    private static JsonObject state;
    private static final int WIDTH=186;
    private static final int GOLD=0xFFFFD36B;
    private static final int WHITE=FantasySkin.INK;
    private static final int MUTED=FantasySkin.MUTED;
    private static final int GREEN=FantasySkin.GREEN;
    private static boolean contractsVisible=true;
    private static boolean eraVisible=true;
    private static final KeyMapping TOGGLE_ERA=new KeyMapping(
        "key.evolutioncraft.toggle_era_hud",InputConstants.KEY_J,KeyMapping.Category.MISC);
    private static final KeyMapping TOGGLE_CONTRACTS=new KeyMapping(
        "key.evolutioncraft.toggle_contract_hud",InputConstants.KEY_K,KeyMapping.Category.MISC);
    private EraObjectivesHud(){}

    @SubscribeEvent
    public static void registerKey(RegisterKeyMappingsEvent event){
        event.register(TOGGLE_CONTRACTS);
        event.register(TOGGLE_ERA);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event){
        if(Minecraft.getInstance().gui.screen()!=null)return;
        while(TOGGLE_CONTRACTS.consumeClick())contractsVisible=!contractsVisible;
        while(TOGGLE_ERA.consumeClick())eraVisible=!eraVisible;
    }

    @SubscribeEvent
    public static void registerPayload(RegisterClientPayloadHandlersEvent event){
        event.register(EraHudPayload.TYPE,(payload,context)->{
            state=JsonParser.parseString(payload.json()).getAsJsonObject();
        });
    }

    @SubscribeEvent
    public static void registerHud(RegisterGuiLayersEvent event){
        event.registerAboveAll(
            Identifier.fromNamespaceAndPath(EvolutionCraft.ID,"era_objectives"),
            EraObjectivesHud::render);
    }

    private static List<String> lines(Minecraft mc,String text,int maxWidth){
        List<String> result=new ArrayList<>();
        StringBuilder line=new StringBuilder();
        for(String word:text.split(" ")){
            String next=line.isEmpty()?word:line+" "+word;
            if(!line.isEmpty() && mc.font.width(next)>maxWidth){
                result.add(line.toString());
                line.setLength(0);
                line.append(word);
            }else{
                line.setLength(0);
                line.append(next);
            }
        }
        if(!line.isEmpty())result.add(line.toString());
        return result;
    }

    private static void art(GuiGraphicsExtractor g,String name,int x,int y,int w,int h){
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
            Identifier.fromNamespaceAndPath(EvolutionCraft.ID,"textures/gui/"+name+".png"),x,y,0f,0f,w,h,w,h);
    }
    private static String icon(String label){
        String s=java.text.Normalizer.normalize(label.toLowerCase(java.util.Locale.ROOT),java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","");
        if(s.contains("thoridrop"))return "objective_thoridrop";
        if(s.contains("tributo"))return "objective_tribute";
        if(s.contains("pesca")||s.contains("pez"))return "category_fishing";
        if(s.contains("bioma")||s.contains("explor")||s.contains("estructura"))return "hud_explore";
        if(s.contains("derrot")||s.contains("matar"))return "category_bosses";
        if(s.contains("cosecha")||s.contains("cultiv"))return "category_farming";
        if(s.contains("cria"))return "objective_breeding";
        if(s.contains("comerci"))return "objective_trade";
        String material=s.contains("netherita")?"netherite":s.contains("diamante")?"diamond":s.contains("hierro")?"iron":s.contains("cobre")?"copper":s.contains("piedra")?"stone":"wooden";
        if(s.contains("pico"))return material+"_pickaxe";
        if(s.contains("espada"))return material+"_sword";
        if(s.contains("azada"))return material+"_hoe";
        if(s.contains("hacha"))return material+"_axe";
        if(s.contains("pala"))return material+"_shovel";
        if(s.contains("hoguera"))return "objective_campfire";
        if(s.contains("noche")||s.contains("cama"))return "bed_food";
        if(s.contains("diamante"))return "hud_diamond";
        if(s.contains("hierro"))return "hud_iron";
        if(s.contains("cobre"))return "hud_copper";
        if(s.contains("piedra"))return "hud_stone";
        if(s.contains("madera"))return "hud_wood";
        return "objective_mastery";
    }
    private static String fit(Minecraft mc,String s,int w){
        if(mc.font.width(s)<=w)return s;
        while(!s.isEmpty()&&mc.font.width(s+"…")>w)s=s.substring(0,s.length()-1);
        return s+"…";
    }
    private static int pos(int size,int n,int base){return Math.round(size*n/(float)base);}
    private static void centered(GuiGraphicsExtractor g,Minecraft mc,String s,int x,int y,int w,int color){
        s=fit(mc,s,w);g.text(mc.font,s,x+(w-mc.font.width(s))/2,y,color,false);
    }
    private static void render(GuiGraphicsExtractor g,net.minecraft.client.DeltaTracker unused){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.gui.screen()!=null||state==null||g.guiWidth()<300||g.guiHeight()<165)return;
        JsonArray entries=state.getAsJsonArray("pending");
        JsonArray contracts=state.has("contracts")?state.getAsJsonArray("contracts"):new JsonArray();
        boolean show=contractsVisible&&contracts.size()>0;
        int x=5,y=12,bottom=g.guiHeight()-42;
        int panels=(eraVisible?1:0)+(show?1:0);
        if(panels==0)return;
        int w=Math.min(200,Math.min(g.guiWidth()/3,(bottom-y-6*(panels-1))*3/(4*panels)));
        if(w<90)return;
        int h=Math.round(w*4f/3f);
        if(eraVisible){
            art(g,"hud_objectives",x,y,w,h);
            centered(g,mc,"EVOLUTIONCRAFT",x+pos(w,110,400),y+pos(h,37,533),pos(w,180,400),0xFFFFE3A0);
            centered(g,mc,state.get("eraName").getAsString(),x+pos(w,137,400),y+pos(h,72,533),pos(w,126,400),WHITE);
            int count=Math.min(5,entries.size());
            for(int i=0;i<count;i++){
                String label=entries.get(i).getAsString();
                int cy=y+pos(h,160+i*67,533),cx=x+pos(w,90,400),sz=pos(w,52,400);
                art(g,icon(label),cx-sz/2,cy-sz/2,sz,sz);
                int tx=x+pos(w,123,400),tw=pos(w,171,400);
                List<String> ls=lines(mc,label,tw);
                int n=Math.min(2,ls.size());
                for(int j=0;j<n;j++)g.text(mc.font,fit(mc,ls.get(j)+(j==1&&ls.size()>2?"…":""),tw),tx,cy-n*5+j*10,WHITE,false);
            }
            centered(g,mc,entries.size()>count?"+"+(entries.size()-count)+" objetivos · P":"J: ocultar · P: guía",x+pos(w,65,400),y+pos(h,473,533),pos(w,270,400),WHITE);
            y+=h+6;
        }
        if(show){
            art(g,"hud_contracts",x,y,w,h);
            centered(g,mc,"CONTRATOS",x+pos(w,110,400),y+pos(h,37,533),pos(w,180,400),0xFFFFE3A0);
            centered(g,mc,"Activos",x+pos(w,137,400),y+pos(h,72,533),pos(w,126,400),WHITE);
            int count=Math.min(4,contracts.size());
            for(int i=0;i<count;i++){
                JsonObject o=contracts.get(i).getAsJsonObject();
                int cy=y+pos(h,159+i*77,533),cx=x+pos(w,92,400),sz=pos(w,55,400);
                art(g,icon(o.get("title").getAsString()),cx-sz/2,cy-sz/2,sz,sz);
                int tx=x+pos(w,128,400),tw=pos(w,165,400);
                int current=o.get("current").getAsInt(),required=o.get("required").getAsInt();
                boolean ready=o.get("ready").getAsBoolean();
                g.text(mc.font,fit(mc,o.get("title").getAsString(),tw),tx,cy-9,WHITE,false);
                String progress=ready?"LISTO":Math.min(current,required)+"/"+required;
                g.text(mc.font,fit(mc,progress,tw),tx,cy+2,ready?GREEN:WHITE,false);
            }
            centered(g,mc,contracts.size()>count?"+"+(contracts.size()-count)+" contratos · K":"K: ocultar",x+pos(w,100,400),y+pos(h,459,533),pos(w,200,400),WHITE);
        }
    }
}
