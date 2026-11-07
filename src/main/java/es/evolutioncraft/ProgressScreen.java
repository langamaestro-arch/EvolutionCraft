package es.evolutioncraft;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Pantalla visual de evolución, alimentada exclusivamente con datos del servidor. */
public final class ProgressScreen extends Screen {
    private static final int WHITE=0xFFE7E2D7, GOLD=0xFFFFD46A, GREEN=0xFF70DC94;
    private static final int RED=0xFFFF777B, MUTED=0xFFABB3BC;
    private final JsonObject data;
    private int tab=0, stage=0, page=0;
    private Button previous, next;

    public ProgressScreen(String json) {
        super(Component.literal("Evolution Craft - Guia de progreso"));
        this.data=JsonParser.parseString(json).getAsJsonObject();
        this.stage=Math.min(data.get("era").getAsInt(),4);
    }
    private JsonArray goals() {
        if(tab==0) return data.getAsJsonObject("eras").getAsJsonArray("goals")
                .get(stage).getAsJsonObject().getAsJsonArray("goals");
        return data.getAsJsonObject(switch(tab) { case 1 -> "fishingGoals"; case 2 -> "agricultureGoals"; case 3 -> "husbandryGoals"; case 4 -> "netherGoals"; default -> "endGoals"; }).getAsJsonArray("goals");
    }
    private int completed(JsonArray goals) {
        int result=0;
        for(var item:goals) if(item.getAsJsonObject().get("done").getAsBoolean()) result++;
        return result;
    }
    private void changeTab(int t) { tab=t;page=0;rebuildWidgets(); }
    @Override protected void init() {
        int w=Math.min(390,width-20),x=(width-w)/2,y=Math.max(8,(height-305)/2);
        String[] tabs={"ERAS","PESCA","AGRICULTURA","GANADERIA","NETHER","END"};
        int tabW=(w-18)/3;
        for(int t=0;t<6;t++) {
            final int id=t;
            addRenderableWidget(Button.builder(Component.literal(tabs[t]),
                    b -> changeTab(id)).bounds(x+9+(t%3)*tabW,y+41+(t/3)*22,tabW-3,20).build());
        }
        if(tab==0) {
            addRenderableWidget(Button.builder(Component.literal("<"),b -> {
                if(stage>0) {stage--;page=0;rebuildWidgets();}
            }).bounds(x+9,y+89,24,19).build()).active=stage>0;
            addRenderableWidget(Button.builder(Component.literal(">"),b -> {
                if(stage<4) {stage++;page=0;rebuildWidgets();}
            }).bounds(x+w-33,y+89,24,19).build()).active=stage<4;
        }
        previous=addRenderableWidget(Button.builder(Component.literal("◀"),b -> {
            if(page>0){page--;rebuildWidgets();}
        }).bounds(x+9,y+237,27,20).build());
        next=addRenderableWidget(Button.builder(Component.literal("▶"),b -> {
            if((page+1)*9<goals().size()){page++;rebuildWidgets();}
        }).bounds(x+w-36,y+237,27,20).build());
        previous.active=page>0;
        next.active=(page+1)*9<goals().size();
        addRenderableWidget(Button.builder(Component.literal("Cerrar"),b -> onClose())
                .bounds(x+w/2-40,y+276,80,20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial) {
        int w=Math.min(390,width-20),x=(width-w)/2,y=Math.max(8,(height-305)/2);
        g.fill(0,0,width,height,0xB0000000);
        g.fill(x-3,y-3,x+w+3,y+305,0xFF6C5738);
        g.fill(x,y,x+w,y+302,0xFF17212A);
        g.fill(x+6,y+6,x+w-6,y+35,0xFF2C3B35);
        g.centeredText(font,"✦ EVOLUTION CRAFT ✦",width/2,y+11,GOLD);
        g.centeredText(font,"GUIA DE PROGRESO",width/2,y+23,MUTED);
        int era=data.get("era").getAsInt();
        String title=tab==0 ? "EDAD: "+Progress.ERA_NAMES[stage+1] :
                tab==1 ? "MAESTRIA DE PESCA" : tab==2 ? "AGRICULTURA" : tab==3 ? "GANADERIA" : tab==4 ? "PORTAL DEL NETHER" : "PORTAL DEL END";
        g.centeredText(font,title,width/2,y+96,GOLD);
        JsonArray goals=goals();
        int done=completed(goals),total=goals.size();
        g.fill(x+15,y+119,x+w-15,y+127,0xFF0D1419);
        if(total>0) g.fill(x+15,y+119,x+15+(w-30)*done/total,y+127,GREEN);
        g.text(font,done+" / "+total+" objetivos",x+15,y+133,WHITE);
        String desc=tab==0 ? "Era actual: "+Progress.ERA_NAMES[era] :
                tab==1 ? "Nivel de pesca: "+data.get("fishing").getAsInt()+" / 5" :
                tab==2 ? "Nivel de agricultura: "+data.get("agriculture").getAsInt()+" / 5" :
                tab==3 ? "Nivel de ganaderia: "+data.get("husbandry").getAsInt()+" / 5" :
                tab==4 ? (data.get("nether").getAsBoolean()?"DESBLOQUEADO":"BLOQUEADO") :
                (data.get("end").getAsBoolean()?"DESBLOQUEADO":"BLOQUEADO");
        g.text(font,desc,x+w-15-font.width(desc),y+133,
                tab>=4?(desc.equals("DESBLOQUEADO")?GREEN:RED):MUTED);
        g.fill(x+12,y+148,x+w-12,y+233,0xFF202D36);
        for(int i=page*9;i<Math.min(goals.size(),(page+1)*9);i++) {
            JsonObject goal=goals.get(i).getAsJsonObject();
            boolean ok=goal.get("done").getAsBoolean();
            int yy=y+153+(i-page*9)*9;
            g.text(font,ok?"✔":"✘",x+18,yy,ok?GREEN:RED);
            String label=goal.get("text").getAsString();
            if(label.length()>44) label=label.substring(0,41)+"...";
            g.text(font,label,x+33,yy,ok?GREEN:WHITE);
        }
        int pages=Math.max(1,(goals.size()+8)/9);
        g.centeredText(font,"Pagina "+(page+1)+" / "+pages,width/2,y+243,MUTED);
        g.centeredText(font,"Esc para salir · P para abrir · datos del servidor",width/2,y+265,MUTED);
        super.extractRenderState(g,mx,my,partial);
    }
    @Override public boolean isPauseScreen(){return false;}
}
