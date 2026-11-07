package es.evolutioncraft;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Evolution Craft: panel visual legible y adaptable.
 * Solo consume la instantanea enviada por el servidor, sin cambiar la progresion.
 */
public final class ProgressScreen extends Screen {
    private static final int WHITE=0xFFF4F0E4, GOLD=0xFFF2CC71, GREEN=0xFF77DA9B;
    private static final int RED=0xFFF18C83, MUTED=0xFFB0BBC5;
    private static final int BACK=0xFF17212A, INNER=0xFF22313B, EDGE=0xFF806844;
    private static final String[] TAB_NAMES={"ERAS","PESCA","AGRICULTURA","GANADERIA","NETHER","END"};
    private static final Item[] TAB_ICONS={Items.IRON_PICKAXE,Items.FISHING_ROD,Items.WHEAT,
            Items.WHEAT,Items.OBSIDIAN,Items.ENDER_EYE};

    private final JsonObject data;
    private int tab=0, stage=0, page=0;
    private Button previous,next;
    private final List<Integer> starts=new ArrayList<>();

    public ProgressScreen(String json) {
        super(Component.literal("Evolution Craft - Guia de progreso"));
        this.data=JsonParser.parseString(json).getAsJsonObject();
        this.stage=Math.min(data.get("era").getAsInt(),4);
    }

    private int panelWidth(){return Math.min(510,width-20);}
    private int panelHeight(){return Math.min(385,height-12);}
    private int left(){return (width-panelWidth())/2;}
    private int top(){return (height-panelHeight())/2;}
    private int listTop(){return top()+167;}
    private int listBottom(){return top()+panelHeight()-64;}

    private JsonArray goals() {
        if(tab==0) return data.getAsJsonObject("eras").getAsJsonArray("goals")
                .get(stage).getAsJsonObject().getAsJsonArray("goals");
        String key=switch(tab) {
            case 1 -> "fishingGoals";
            case 2 -> "agricultureGoals";
            case 3 -> "husbandryGoals";
            case 4 -> "netherGoals";
            default -> "endGoals";
        };
        return data.getAsJsonObject(key).getAsJsonArray("goals");
    }

    /** Word wrap uses actual Minecraft font metrics, not fixed character counts. */
    private List<String> wrap(String text,int maxWidth) {
        List<String> result=new ArrayList<>();
        StringBuilder line=new StringBuilder();
        for(String word:text.split(" ")) {
            String candidate=line.isEmpty()?word:line+" "+word;
            if(font.width(candidate)<=maxWidth) {
                line.setLength(0);
                line.append(candidate);
            } else {
                if(!line.isEmpty()){result.add(line.toString());line.setLength(0);}
                // Extremely long single tokens can still wrap, without losing characters.
                StringBuilder piece=new StringBuilder();
                for(int i=0;i<word.length();i++){
                    String c=word.substring(i,i+1);
                    if(font.width(piece.toString()+c)>maxWidth && !piece.isEmpty()){
                        result.add(piece.toString());
                        piece.setLength(0);
                    }
                    piece.append(c);
                }
                line.append(piece);
            }
        }
        if(!line.isEmpty()) result.add(line.toString());
        if(result.isEmpty()) result.add("");
        return result;
    }

    private int rowHeight(JsonObject goal){
        return Math.max(25,wrap(goal.get("text").getAsString(),Math.max(95,panelWidth()-94)).size()*11+7);
    }

    /** Build pages by consumed height so multi-line goals are never hidden. */
    private void paginate(){
        starts.clear();
        starts.add(0);
        JsonArray list=goals();
        int room=Math.max(27,listBottom()-listTop()-8),used=0;
        for(int i=0;i<list.size();i++){
            int h=rowHeight(list.get(i).getAsJsonObject());
            if(used>0 && used+h>room){starts.add(i);used=0;}
            used+=h;
        }
        page=Math.max(0,Math.min(page,starts.size()-1));
    }

    private void changeTab(int t){tab=t;page=0;rebuildWidgets();}
    @Override protected void init(){
        paginate();
        int w=panelWidth(),x=left(),y=top(),tw=(w-24)/3;
        for(int t=0;t<6;t++){
            int id=t;
            addRenderableWidget(Button.builder(Component.literal("   "+TAB_NAMES[t]),
                b->changeTab(id)).bounds(x+10+(t%3)*tw,y+42+(t/3)*24,tw-3,21).build());
        }
        if(tab==0){
            addRenderableWidget(Button.builder(Component.literal("<"),b->{
                if(stage>0){stage--;page=0;rebuildWidgets();}
            }).bounds(x+14,y+97,24,20).build()).active=stage>0;
            addRenderableWidget(Button.builder(Component.literal(">"),b->{
                if(stage<4){stage++;page=0;rebuildWidgets();}
            }).bounds(x+w-38,y+97,24,20).build()).active=stage<4;
        }
        previous=addRenderableWidget(Button.builder(Component.literal("<"),b->{
            if(page>0){page--;rebuildWidgets();}
        }).bounds(x+14,y+panelHeight()-52,29,20).build());
        next=addRenderableWidget(Button.builder(Component.literal(">"),b->{
            if(page<starts.size()-1){page++;rebuildWidgets();}
        }).bounds(x+w-43,y+panelHeight()-52,29,20).build());
        previous.active=page>0;
        next.active=page<starts.size()-1;
        addRenderableWidget(Button.builder(Component.literal("Cerrar"),b->onClose())
                .bounds(width/2-46,y+panelHeight()-29,92,20).build());
    }

    private int accent(){
        return switch(tab){
            case 1 -> 0xFF68B9E9;
            case 2 -> 0xFF91D778;
            case 3 -> 0xFFD4B576;
            case 4 -> 0xFFF18F62;
            case 5 -> 0xFFC3A3F3;
            default -> GOLD;
        };
    }

    private String heading(){
        return switch(tab){
            case 0 -> "SIGUIENTE EDAD: "+Progress.ERA_NAMES[stage+1];
            case 1 -> "MAESTRIA DE PESCA";
            case 2 -> "CULTIVOS Y COSECHAS";
            case 3 -> "CRIA Y DOMESTICACION";
            case 4 -> "PORTAL DEL NETHER";
            default -> "PORTAL DEL END";
        };
    }

    private String detail(){
        return switch(tab){
            case 0 -> "Era actual: "+Progress.ERA_NAMES[data.get("era").getAsInt()];
            case 1 -> "Pesca: "+data.get("fishing").getAsInt()+" / 5";
            case 2 -> "Agricultura: "+data.get("agriculture").getAsInt()+" / 5";
            case 3 -> "Ganaderia: "+data.get("husbandry").getAsInt()+" / 5";
            case 4 -> data.get("nether").getAsBoolean()?"DESBLOQUEADO":"BLOQUEADO";
            default -> data.get("end").getAsBoolean()?"DESBLOQUEADO":"BLOQUEADO";
        };
    }

    /** Uses vanilla item textures; icons are visual only and never unlock goals. */
    private Item icon(String label){
        String s=label.toLowerCase(Locale.ROOT);
        if(s.contains("pico"))return tool(s,Items.WOODEN_PICKAXE,Items.STONE_PICKAXE,Items.COPPER_PICKAXE,Items.IRON_PICKAXE,Items.DIAMOND_PICKAXE);
        if(s.contains("hacha"))return tool(s,Items.WOODEN_AXE,Items.STONE_AXE,Items.COPPER_AXE,Items.IRON_AXE,Items.DIAMOND_AXE);
        if(s.contains("espada"))return tool(s,Items.WOODEN_SWORD,Items.STONE_SWORD,Items.COPPER_SWORD,Items.IRON_SWORD,Items.DIAMOND_SWORD);
        if(s.contains("pala"))return tool(s,Items.WOODEN_SHOVEL,Items.STONE_SHOVEL,Items.COPPER_SHOVEL,Items.IRON_SHOVEL,Items.DIAMOND_SHOVEL);
        if(s.contains("azada"))return tool(s,Items.WOODEN_HOE,Items.STONE_HOE,Items.COPPER_HOE,Items.IRON_HOE,Items.DIAMOND_HOE);
        if(s.contains("casco"))return Items.IRON_HELMET;
        if(s.contains("peto"))return Items.IRON_CHESTPLATE;
        if(s.contains("pantalones"))return Items.IRON_LEGGINGS;
        if(s.contains("botas"))return Items.IRON_BOOTS;
        if(s.contains("pesca")||s.contains("pescar")||s.contains("peces")||s.contains("especies"))return Items.FISHING_ROD;
        if(s.contains("tesoros"))return Items.NAME_TAG;
        if(s.contains("trigo"))return Items.WHEAT;
        if(s.contains("zanahoria")||s.contains("patata")||s.contains("remolacha"))return Items.CARROT;
        if(s.contains("melon"))return Items.MELON_SLICE;
        if(s.contains("calabaza"))return Items.PUMPKIN;
        if(s.contains("cacao"))return Items.COCOA_BEANS;
        if(s.contains("verrugas"))return Items.NETHER_WART;
        if(s.contains("cosechas")||s.contains("agricultura"))return Items.WHEAT;
        if(s.contains("lobo"))return Items.BONE;
        if(s.contains("gato"))return Items.COD;
        if(s.contains("caballos"))return Items.SADDLE;
        if(s.contains("criar")||s.contains("reproducciones")||s.contains("ganaderia"))return Items.WHEAT;
        if(s.contains("warden"))return Items.SCULK_CATALYST;
        if(s.contains("wither"))return Items.WITHER_SKELETON_SKULL;
        if(s.contains("nether"))return Items.OBSIDIAN;
        if(s.contains("end"))return Items.ENDER_EYE;
        if(s.contains("hierro"))return Items.IRON_INGOT;
        if(s.contains("diamante"))return Items.DIAMOND;
        if(s.contains("intercambiar")||s.contains("comerciar"))return Items.EMERALD;
        if(s.contains("piedra"))return Items.COBBLESTONE;
        if(s.contains("biomas"))return Items.COMPASS;
        if(s.contains("avances"))return Items.BOOK;
        return TAB_ICONS[tab];
    }
    private Item tool(String s,Item wood,Item stone,Item copper,Item iron,Item diamond){
        if(s.contains("diamante"))return diamond;
        if(s.contains("hierro"))return iron;
        if(s.contains("cobre"))return copper;
        if(s.contains("piedra"))return stone;
        return wood;
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){
        int x=left(),y=top(),w=panelWidth(),h=panelHeight(),color=accent();
        g.fill(0,0,width,height,0xB9000000);
        g.fill(x-4,y-4,x+w+4,y+h+4,EDGE);
        g.fill(x-2,y-2,x+w+2,y+h+2,0xFF0C1217);
        g.fill(x,y,x+w,y+h,BACK);
        g.fill(x+7,y+7,x+w-7,y+37,0xFF2A3C36);
        g.fill(x+7,y+35,x+w-7,y+37,color);
        g.centeredText(font,"✦  E V O L U T I O N   C R A F T  ✦",width/2,y+11,GOLD);
        g.centeredText(font,"GUIA DEL AVENTURERO · PROGRESION",width/2,y+24,MUTED);

        int tw=(w-24)/3;
        for(int t=0;t<6;t++){
            int tx=x+10+(t%3)*tw,ty=y+42+(t/3)*24;
            if(t==tab) g.outline(tx-1,ty-1,tw-1,23,color);
            g.item(new ItemStack(TAB_ICONS[t]),tx+7,ty+3);
        }

        g.fill(x+10,y+92,x+w-10,y+123,0xFF22313B);
        g.centeredText(font,heading(),width/2,y+103,color);
        JsonArray goals=goals();
        int done=0;
        for(var item:goals) if(item.getAsJsonObject().get("done").getAsBoolean())done++;
        g.fill(x+16,y+129,x+w-16,y+139,0xFF0A141B);
        if(goals.size()>0)g.fill(x+17,y+130,x+17+(w-34)*done/goals.size(),y+138,GREEN);
        g.text(font,done+" / "+goals.size()+" objetivos",x+16,y+145,WHITE);
        String detail=detail();
        g.text(font,detail,x+w-16-font.width(detail),y+145,tab>=4?(detail.equals("DESBLOQUEADO")?GREEN:RED):MUTED);

        g.fill(x+11,listTop()-2,x+w-11,listBottom()+2,0xFF101A22);
        int begin=starts.get(page),end=page+1<starts.size()?starts.get(page+1):goals.size();
        int cy=listTop()+3;
        for(int i=begin;i<end;i++){
            JsonObject goal=goals.get(i).getAsJsonObject();
            boolean completed=goal.get("done").getAsBoolean();
            String label=goal.get("text").getAsString();
            List<String> lines=wrap(label,Math.max(95,w-94));
            int rh=rowHeight(goal);
            if(i%2==0)g.fill(x+14,cy-2,x+w-14,cy+rh-2,INNER);
            g.fill(x+14,cy-2,x+16,cy+rh-2,completed?GREEN:RED);
            g.item(new ItemStack(icon(label)),x+22,cy+Math.max(0,(rh-18)/2));
            g.text(font,completed?"✔":"✘",x+44,cy+Math.max(0,(rh-10)/2),completed?GREEN:RED);
            int txtY=cy+Math.max(1,(rh-lines.size()*11)/2);
            for(String line:lines){
                g.text(font,line,x+60,txtY,completed?GREEN:WHITE);
                txtY+=11;
            }
            cy+=rh;
        }
        g.centeredText(font,"Pagina "+(page+1)+" / "+starts.size(),width/2,y+h-46,MUTED);
        g.centeredText(font,"Objetivos reales del servidor · Esc para salir",width/2,y+h-63,MUTED);
        super.extractRenderState(g,mx,my,partial);
    }
    @Override public boolean isPauseScreen(){return false;}
}
