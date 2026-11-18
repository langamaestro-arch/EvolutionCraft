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
    private static final int WIDTH=212;
    private static final int GOLD=0xFFFFD36B;
    private static final int WHITE=FantasySkin.INK;
    private static final int MUTED=FantasySkin.MUTED;
    private static final int GREEN=FantasySkin.GREEN;
    private static boolean contractsVisible=true;
    private static final KeyMapping TOGGLE_CONTRACTS=new KeyMapping(
        "key.evolutioncraft.toggle_contract_hud",InputConstants.KEY_K,KeyMapping.Category.MISC);
    private EraObjectivesHud(){}

    @SubscribeEvent
    public static void registerKey(RegisterKeyMappingsEvent event){
        event.register(TOGGLE_CONTRACTS);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event){
        if(Minecraft.getInstance().gui.screen()!=null)return;
        while(TOGGLE_CONTRACTS.consumeClick())contractsVisible=!contractsVisible;
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

    private static void render(GuiGraphicsExtractor g,net.minecraft.client.DeltaTracker unused){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null || mc.gui.screen()!=null || state==null)return;
        if(g.guiWidth()<300 || g.guiHeight()<165)return;

        int era=state.get("era").getAsInt();
        JsonArray entries=state.getAsJsonArray("pending");
        String eraName=state.get("eraName").getAsString();
        int x=8,y=52;
        int width=Math.min(WIDTH,Math.max(144,g.guiWidth()/3));
        int bottom=g.guiHeight()-51;  // No invadir barra de acceso rápido
        JsonArray contracts=state.has("contracts")?state.getAsJsonArray("contracts"):new JsonArray();
        boolean showContracts=contractsVisible && contracts.size()>0;
        int reserve=showContracts?Math.min(128,Math.max(0,bottom-y-74)):0;
        List<List<String>> rows=new ArrayList<>();
        int used=0,limit=Math.min(entries.size(),24);
        for(int i=0;i<limit;i++){
            List<String> row=lines(mc,entries.get(i).getAsString(),width-26);
            int needed=Math.max(16,row.size()*11+6);
            if(y+45+used+needed>bottom-20-reserve)break;
            rows.add(row);
            used+=needed;
        }
        int hidden=entries.size()-rows.size();
        int h=47+used+(hidden>0?15:0);
        if(era==5)h=58;
        h=Math.min(h,bottom-y-reserve);
        FantasySkin.panel(g,x,y,width,h);
        FantasySkin.ribbon(g,x+5,y+3,width-10,20);
        FantasySkin.decorations(g,x,y,width,h);
        g.text(mc.font,"EVOLUTIONCRAFT",x+29,y+9,0xFF5C2A1A);
        String subtitle=era==5?"Todas las eras completadas":"ERA: "+eraName.toUpperCase(java.util.Locale.ROOT);
        g.text(mc.font,subtitle,x+13,y+26,WHITE);
        if(era==5){
            g.text(mc.font,"¡Progreso de eras completo!",x+15,y+40,WHITE);
            if(showContracts)renderContracts(g,mc,contracts,x,y+h+7,width,bottom);
            return;
        }
        int lineY=y+41;
        for(List<String> row:rows){
            g.text(mc.font,"✦",x+12,lineY,0xFFB66A2E);
            for(String s:row){
                g.text(mc.font,s,x+27,lineY,WHITE);
                lineY+=11;
            }
            lineY+=6;
        }
        if(hidden>0){
            g.text(mc.font,"+"+hidden+" objetivos · Pulsa P",x+16,lineY,MUTED);
        }
        if(showContracts)renderContracts(g,mc,contracts,x,y+h+7,width,bottom);
    }

    private static void renderContracts(GuiGraphicsExtractor g,Minecraft mc,
                                        JsonArray contracts,int x,int y,int width,int bottom){
        int available=bottom-y;
        if(available<29)return;
        int count=Math.min(3,Math.min(contracts.size(),Math.max(0,(available-36)/27)));
        int panelH=35+count*27;
        if(panelH>available)return;
        FantasySkin.panel(g,x,y,width,panelH);
        FantasySkin.ribbon(g,x+5,y+3,width-10,20);
        FantasySkin.decorations(g,x,y,width,panelH);
        g.text(mc.font,"CONTRATOS ACTIVOS",x+22,y+9,0xFF592C1D);
        g.text(mc.font,"K: ocultar · P: gestionar",x+13,y+26,MUTED);
        int yy=y+39;
        for(int i=0;i<count;i++){
            JsonObject contract=contracts.get(i).getAsJsonObject();
            boolean ready=contract.get("ready").getAsBoolean();
            int current=contract.get("current").getAsInt();
            int required=contract.get("required").getAsInt();
            String name=contract.get("title").getAsString();
            String progress=ready?"LISTO":Math.min(current,required)+"/"+required;
            int color=ready?GREEN:WHITE;
            List<String> names=lines(mc,name,width-26);
            String shortName=names.isEmpty()?name:names.get(0);
            g.text(mc.font,shortName,x+17,yy,color);
            g.text(mc.font,progress,x+width-16-mc.font.width(progress),yy+11,ready?GREEN:FantasySkin.INK);
            int barLeft=x+17,barRight=x+width-16;
            int barY=yy+13;
            int barMax=barRight-barLeft-52;
            if(barMax>0)FantasySkin.progress(g,barLeft,barY,barMax,7,
                Math.min(current,required),required,ready?GREEN:0xFFE7A83D);
            yy+=27;
        }
    }
}
