package es.evolutioncraft;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
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
    private static final int GOLD=0xFFF4CE82;
    private static final int WHITE=0xFFEDF1EF;
    private static final int MUTED=0xFFADC3CC;
    private EraObjectivesHud(){}

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
        int x=7,y=52;
        int width=Math.min(WIDTH,Math.max(144,g.guiWidth()/3));
        int bottom=g.guiHeight()-51;  // No invadir barra de acceso rápido
        List<List<String>> rows=new ArrayList<>();
        int used=0,limit=Math.min(entries.size(),24);
        for(int i=0;i<limit;i++){
            List<String> row=lines(mc,entries.get(i).getAsString(),width-26);
            int needed=Math.max(16,row.size()*11+6);
            if(y+32+used+needed>bottom-20)break;
            rows.add(row);
            used+=needed;
        }
        int hidden=entries.size()-rows.size();
        int h=36+used+(hidden>0?15:0);
        if(era==5)h=45;
        h=Math.min(h,bottom-y);
        g.fill(x,y,x+width,y+h,0xC811202C);
        g.fill(x,y,x+3,y+h,0xFF9B773F);
        g.fill(x+5,y+25,x+width-6,y+26,0xFF52616D);
        g.text(mc.font,"EVOLUTIONCRAFT",x+12,y+7,GOLD);
        String subtitle=era==5?"Todas las eras completadas":"ERA: "+eraName.toUpperCase(java.util.Locale.ROOT);
        g.text(mc.font,subtitle,x+12,y+18,MUTED);
        if(era==5){g.text(mc.font,"¡Progreso de eras completo!",x+12,y+31,WHITE);return;}
        int lineY=y+31;
        for(List<String> row:rows){
            g.text(mc.font,"◆",x+9,lineY,GOLD);
            for(String s:row){
                g.text(mc.font,s,x+23,lineY,WHITE);
                lineY+=11;
            }
            lineY+=6;
        }
        if(hidden>0){
            g.text(mc.font,"+"+hidden+" objetivos · Pulsa P",x+11,lineY,MUTED);
        }
    }
}
