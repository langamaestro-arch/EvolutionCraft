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
    private static void frame(GuiGraphicsExtractor g,Minecraft mc,String texture,String title,int x,int y,int w,int h){
        FantasySkin.fortressFrame(g,x,y,w,h);
        FantasySkin.panel(g,x+5,y+5,w-10,h-10);
        // A clean parchment inset covers the fixed decorative slots, allowing variable row counts.
        
        FantasySkin.ribbon(g,x+14,y+12,w-28,18);
        String t=fit(mc,title,w-54);
        g.text(mc.font,t,x+(w-mc.font.width(t))/2,y+17,WHITE,false);
    }
    private static void render(GuiGraphicsExtractor g,net.minecraft.client.DeltaTracker unused){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.gui.screen()!=null||state==null||g.guiWidth()<300||g.guiHeight()<165)return;
        int x=5,y=44,w=Math.min(200,Math.max(144,g.guiWidth()/3)),bottom=g.guiHeight()-48;
        JsonArray entries=state.getAsJsonArray("pending");
        JsonArray contracts=state.has("contracts")?state.getAsJsonArray("contracts"):new JsonArray();
        boolean show=contractsVisible&&contracts.size()>0;
        if(eraVisible){
            int available=bottom-y-(show?Math.min(115,Math.max(0,(bottom-y)/2)):0);
            if(available>=82){
                int count=Math.min(entries.size(),Math.min(Math.clamp(state.has("maxObjectives")?state.get("maxObjectives").getAsInt():24,4,24),Math.max(0,(available-82)/30)));
                int h=82+count*30;
                frame(g,mc,"hud_objectives","EVOLUTIONCRAFT",x,y,w,h);
                g.text(mc.font,fit(mc,"ERA: "+state.get("eraName").getAsString(),w-36),x+18,y+34,WHITE,false);
                for(int i=0;i<count;i++){
                    String label=entries.get(i).getAsString();int yy=y+46+i*30;
                    FantasySkin.card(g,x+14,yy,w-28,29,false);
                    art(g,icon(label),x+15,yy,28,28);
                    List<String> ls=lines(mc,label,w-65);
                    for(int j=0;j<Math.min(2,ls.size());j++)g.text(mc.font,fit(mc,ls.get(j)+(j==1&&ls.size()>2?"…":""),w-65),x+46,yy+4+j*10,WHITE,false);
                }
                String footer=entries.size()>count?"+"+(entries.size()-count)+" objetivos · P":entries.size()==0?"Sin objetivos pendientes":"J: ocultar · P: guía";
                g.text(mc.font,fit(mc,footer,w-36),x+18,y+h-23,MUTED,false);
                y+=h+5;
            }
        }
        if(show)renderContracts(g,mc,contracts,x,y,w,bottom);
    }
    private static void renderContracts(GuiGraphicsExtractor g,Minecraft mc,JsonArray contracts,int x,int y,int w,int bottom){
        int count=Math.min(contracts.size(),Math.min(4,Math.max(0,(bottom-y-70)/36)));
        if(bottom-y<70)return;
        int h=70+count*36;
        frame(g,mc,"hud_contracts","CONTRATOS ACTIVOS",x,y,w,h);
        for(int i=0;i<count;i++){
            JsonObject o=contracts.get(i).getAsJsonObject();
            int yy=y+34+i*36,current=o.get("current").getAsInt(),required=o.get("required").getAsInt();
            boolean ready=o.get("ready").getAsBoolean();
            String label=o.get("title").getAsString(),progress=ready?"LISTO":Math.min(current,required)+"/"+required;
            FantasySkin.card(g,x+14,yy,w-28,35,false);
            art(g,icon(label),x+15,yy+2,30,30);
            List<String> ls=lines(mc,label,w-66);
            for(int j=0;j<Math.min(2,ls.size());j++)g.text(mc.font,fit(mc,ls.get(j),w-66),x+48,yy+3+j*9,WHITE,false);
            int tw=mc.font.width(progress);
            FantasySkin.progress(g,x+48,yy+25,Math.max(5,w-66-tw-5),6,current,required,ready?GREEN:0xFFE7A83D);
            g.text(mc.font,progress,x+w-17-tw,yy+23,ready?GREEN:WHITE,false);
        }
        g.text(mc.font,fit(mc,(contracts.size()>count?"+"+(contracts.size()-count)+" contratos · ":"")+"K: ocultar",w-36),x+18,y+h-23,MUTED,false);
    }
}
