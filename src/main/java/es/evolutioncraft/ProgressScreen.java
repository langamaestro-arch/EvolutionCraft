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

/** Diario de aventuras EvolutionCraft: diseño híbrido Minecraft + RPG. */
public final class ProgressScreen extends Screen {
    private static final int WHITE=0xFFF4F0E4, GOLD=0xFFF5D07C, GREEN=0xFF82DE9B;
    private static final int RED=0xFFE78A80, MUTED=0xFFAAB8C4, BACK=0xFF17212C;
    private static final String[] TABS={"ERAS","PESCA","AGRICULTURA","GANADERIA","EXPLORACION",
        "HERRAMIENTAS","ALQUIMIA","NETHER","END","SECRETOS","MISIONES"};
    private static final Item[] ITEMS={Items.IRON_PICKAXE,Items.FISHING_ROD,Items.WHEAT,
        Items.WHEAT,Items.COMPASS,Items.DIAMOND_PICKAXE,Items.BREWING_STAND,
        Items.OBSIDIAN,Items.ENDER_EYE,Items.NETHER_STAR,Items.BOOK};
    private static final int[][] GROUPS={{0,10,7,8},{1,2,3,4,5,6},{9}};
    private static final String[] GROUP_NAMES={"EVOLUCION","HABILIDADES","LOGROS"};
    private static final Item[] GROUP_ICONS={Items.DIAMOND_PICKAXE,Items.ENCHANTED_BOOK,Items.NETHER_STAR};
    private final JsonObject data;
    private int group=0,tab=0,stage=0,page=0;
    private String selectedBadge="";
    private final List<Integer> starts=new ArrayList<>();

    public ProgressScreen(String json){
        super(Component.literal("EvolutionCraft - Diario del aventurero"));
        data=JsonParser.parseString(json).getAsJsonObject();
        stage=Math.min(data.get("era").getAsInt(),4);
        if(data.has("selectedBadge"))selectedBadge=data.get("selectedBadge").getAsString();
    }
    private int panelWidth(){return Math.min(555,width-16);}
    private int panelHeight(){return Math.min(451,height-12);}
    private int left(){return (width-panelWidth())/2;}
    private int top(){return (height-panelHeight())/2;}
    private int listTop(){return top()+208;}
    private int listBottom(){return top()+panelHeight()-67;}

    private JsonArray goals(){
        if(tab==0)return data.getAsJsonObject("eras").getAsJsonArray("goals")
            .get(stage).getAsJsonObject().getAsJsonArray("goals");
        String key=switch(tab){
            case 1 -> "fishingGoals";
            case 2 -> "agricultureGoals";
            case 3 -> "husbandryGoals";
            case 4 -> "explorationGoals";
            case 5 -> "toolGoals";
            case 6 -> "alchemyGoals";
            case 7 -> "netherGoals";
            case 8 -> "endGoals";
            case 9 -> "secretGoals";
            default -> "missionGoals";
        };
        return data.getAsJsonObject(key).getAsJsonArray("goals");
    }
    private static int complete(JsonArray list){
        int count=0;
        for(var e:list)if(e.getAsJsonObject().get("done").getAsBoolean())count++;
        return count;
    }
    private List<String> wrap(String text,int maxWidth){
        List<String> output=new ArrayList<>();
        StringBuilder current=new StringBuilder();
        for(String word:text.split(" ")){
            String proposed=current.isEmpty()?word:current+" "+word;
            if(font.width(proposed)<=maxWidth){current.setLength(0);current.append(proposed);continue;}
            if(!current.isEmpty()){output.add(current.toString());current.setLength(0);}
            StringBuilder piece=new StringBuilder();
            for(int i=0;i<word.length();i++){
                char ch=word.charAt(i);
                if(!piece.isEmpty()&&font.width(piece.toString()+ch)>maxWidth){
                    output.add(piece.toString());piece.setLength(0);
                }
                piece.append(ch);
            }
            current.append(piece);
        }
        if(!current.isEmpty())output.add(current.toString());
        if(output.isEmpty())output.add("");
        return output;
    }
    private int rowHeight(JsonObject obj){
        return Math.max(28,8+11*wrap(obj.get("text").getAsString(),Math.max(80,panelWidth()-103)).size());
    }
    private void paginate(){
        starts.clear(); starts.add(0);
        JsonArray goals=goals();
        int used=0,available=Math.max(28,listBottom()-listTop()-8);
        for(int i=0;i<goals.size();i++){
            int height=rowHeight(goals.get(i).getAsJsonObject());
            if(used>0&&used+height>available){starts.add(i);used=0;}
            used+=height;
        }
        page=Math.max(0,Math.min(page,starts.size()-1));
    }
    private String badgeName(String key){
        return switch(key){
            case "noche" -> "Superviviente";
            case "tesoros" -> "Pescador afortunado";
            case "comerciante" -> "Mercader";
            case "tierra" -> "Granjero incansable";
            case "animales" -> "Domador";
            case "diamantes" -> "Minero experto";
            case "fronteras" -> "Explorador legendario";
            case "oceano" -> "Conquistador del océano";
            case "arcano" -> "Encantador";
            case "elixir" -> "Alquimista excepcional";
            case "terror" -> "Cazador de pesadillas";
            case "infierno" -> "Señor del Nether";
            case "dragon" -> "Matadragones";
            case "equipo" -> "Maestro herrero";
            case "evolucion" -> "Leyenda de EvolutionCraft";
            default -> "Sin distintivo";
        };
    }
    private void changeBadge(){
        JsonArray unlocked=data.getAsJsonArray("unlockedBadges");
        if(unlocked==null || unlocked.isEmpty())return;
        int current=-1;
        for(int i=0;i<unlocked.size();i++){
            if(unlocked.get(i).getAsString().equals(selectedBadge)){current=i;break;}
        }
        selectedBadge=unlocked.get((current+1)%unlocked.size()).getAsString();
        var connection=net.minecraft.client.Minecraft.getInstance().getConnection();
        if(connection!=null)connection.sendCommand("ecdistintivo "+selectedBadge);
        rebuildWidgets();
    }
    private void selectGroup(int g){group=g;tab=GROUPS[g][0];page=0;rebuildWidgets();}
    private void selectTab(int t){tab=t;page=0;rebuildWidgets();}
    @Override protected void init(){
        paginate();
        int x=left(),y=top(),w=panelWidth(),tW=(w-24)/3;
        for(int g=0;g<3;g++){
            final int index=g;
            addRenderableWidget(Button.builder(Component.literal("     "+GROUP_NAMES[g]),
                bt->selectGroup(index)).bounds(x+11+g*tW,y+43,tW-4,23).build());
        }
        int[] sub=GROUPS[group];
        for(int i=0;i<sub.length;i++){
            final int target=sub[i];
            addRenderableWidget(Button.builder(Component.literal("    "+TABS[target]),
                bt->selectTab(target)).bounds(x+11+(i%3)*tW,y+75+(i/3)*24,tW-4,21).build());
        }
        if(tab==0){
            addRenderableWidget(Button.builder(Component.literal("<"),bt->{
                if(stage>0){stage--;page=0;rebuildWidgets();}
            }).bounds(x+13,y+132,23,20).build()).active=stage>0;
            addRenderableWidget(Button.builder(Component.literal(">"),bt->{
                if(stage<4){stage++;page=0;rebuildWidgets();}
            }).bounds(x+w-36,y+132,23,20).build()).active=stage<4;
        }
        addRenderableWidget(Button.builder(Component.literal("◀"),bt->{
            if(page>0){page--;rebuildWidgets();}
        }).bounds(x+14,y+panelHeight()-52,28,21).build()).active=page>0;
        addRenderableWidget(Button.builder(Component.literal("▶"),bt->{
            if(page<starts.size()-1){page++;rebuildWidgets();}
        }).bounds(x+w-42,y+panelHeight()-52,28,21).build()).active=page<starts.size()-1;
        if(group==2&&data.has("unlockedBadges")&&data.getAsJsonArray("unlockedBadges").size()>0){
            addRenderableWidget(Button.builder(Component.literal("Cambiar distintivo"),
                bt->changeBadge()).bounds(x+16,y+panelHeight()-29,147,20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Cerrar"),bt->onClose())
            .bounds(width/2-46,y+panelHeight()-29,92,20).build());
    }

    private int color(){
        return switch(tab){
            case 1 -> 0xFF75BDEC;
            case 2 -> 0xFF96D877;
            case 3 -> 0xFFD7B878;
            case 4 -> 0xFF8FD5AE;
            case 5 -> 0xFFC5A8ED;
            case 6 -> 0xFFBD9FED;
            case 7 -> 0xFFEB856B;
            case 8 -> 0xFFC4A5EF;
            case 9 -> GOLD;
            case 10 -> 0xFFF2C982;
            default -> GOLD;
        };
    }
    private String heading(){
        return switch(tab){
            case 0 -> "EDAD: "+Progress.ERA_NAMES[stage+1];
            case 1 -> "MAESTRIA DE PESCA";
            case 2 -> "CULTIVOS Y COSECHAS";
            case 3 -> "CRIA Y DOMESTICACION";
            case 4 -> "EXPLORACION DEL MUNDO";
            case 5 -> "MAESTRIA DE HERRAMIENTAS";
            case 6 -> "ALQUIMIA Y POCIONES";
            case 7 -> "DESBLOQUEAR EL NETHER";
            case 8 -> "DESBLOQUEAR EL END";
            case 9 -> "DESAFIOS SECRETOS";
            default -> "MISIONES DE LAS ERAS";
        };
    }
    private String detail(){
        return switch(tab){
            case 0 -> "Era actual: "+Progress.ERA_NAMES[data.get("era").getAsInt()];
            case 1 -> "Nivel "+data.get("fishing").getAsInt()+"/5";
            case 2 -> "Nivel "+data.get("agriculture").getAsInt()+"/5";
            case 3 -> "Nivel "+data.get("husbandry").getAsInt()+"/5";
            case 4 -> "Nivel "+data.get("exploration").getAsInt()+"/5";
            case 5 -> "Nivel "+data.get("tools").getAsInt()+"/5";
            case 6 -> "Nivel "+data.get("alchemy").getAsInt()+"/5";
            case 7 -> data.get("nether").getAsBoolean()?"DESBLOQUEADO":"BLOQUEADO";
            case 8 -> data.get("end").getAsBoolean()?"DESBLOQUEADO":"BLOQUEADO";
            case 9 -> selectedBadge.isEmpty()?data.get("secretCount").getAsInt()+"/15 descubiertos":"Distintivo: "+badgeName(selectedBadge);
            default -> "6 misiones de aventura";
        };
    }
    private Item icon(String label){
        String s=label.toLowerCase(Locale.ROOT);
        if(s.startsWith("???"))return Items.NETHER_STAR;
        if(s.contains("pico"))return Items.DIAMOND_PICKAXE;
        if(s.contains("hacha"))return Items.IRON_AXE;
        if(s.contains("espada"))return Items.IRON_SWORD;
        if(s.contains("pala"))return Items.IRON_SHOVEL;
        if(s.contains("azada"))return Items.IRON_HOE;
        if(s.contains("casco"))return Items.IRON_HELMET;
        if(s.contains("peto"))return Items.IRON_CHESTPLATE;
        if(s.contains("pantalones"))return Items.IRON_LEGGINGS;
        if(s.contains("botas"))return Items.IRON_BOOTS;
        if(s.contains("tesoro"))return Items.NAME_TAG;
        if(s.contains("pesca")||s.contains("peces")||s.contains("especies"))return Items.FISHING_ROD;
        if(s.contains("bioma")||s.contains("estructura")||s.contains("aldea"))return Items.COMPASS;
        if(s.contains("tanda")||s.contains("efecto")||s.contains("pocion")||s.contains("alquimia"))return Items.BREWING_STAND;
        if(s.contains("bloques con")||s.contains("herramientas"))return Items.IRON_PICKAXE;
        if(s.contains("trigo"))return Items.WHEAT;
        if(s.contains("zanahoria")||s.contains("patata")||s.contains("remolacha"))return Items.CARROT;
        if(s.contains("melon"))return Items.MELON_SLICE;
        if(s.contains("calabaza"))return Items.PUMPKIN;
        if(s.contains("cacao"))return Items.COCOA_BEANS;
        if(s.contains("verruga"))return Items.NETHER_WART;
        if(s.contains("cosecha")||s.contains("agricultura"))return Items.WHEAT;
        if(s.contains("lobo"))return Items.BONE;
        if(s.contains("gato"))return Items.COD;
        if(s.contains("caballo"))return Items.SADDLE;
        if(s.contains("criar")||s.contains("reproducci")||s.contains("ganaderia"))return Items.WHEAT;
        if(s.contains("warden"))return Items.SCULK_CATALYST;
        if(s.contains("wither"))return Items.WITHER_SKELETON_SKULL;
        if(s.contains("netherita"))return Items.NETHERITE_INGOT;
        if(s.contains("nether"))return Items.OBSIDIAN;
        if(s.contains("end"))return Items.ENDER_EYE;
        if(s.contains("hierro"))return Items.IRON_INGOT;
        if(s.contains("diamante"))return Items.DIAMOND;
        if(s.contains("comerciar")||s.contains("intercambiar"))return Items.EMERALD;
        if(s.contains("piedra"))return Items.COBBLESTONE;
        return ITEMS[tab];
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){
        int w=panelWidth(),h=panelHeight(),x=left(),y=top(),accent=color();
        g.fill(0,0,width,height,0xB9000000);
        g.fill(x-5,y-5,x+w+5,y+h+5,0xFF866B45);
        g.fill(x-3,y-3,x+w+3,y+h+3,0xFF0B1119);
        g.fill(x,y,x+w,y+h,BACK);
        g.fill(x+7,y+7,x+w-7,y+39,0xFF2A3E37);
        g.fill(x+7,y+37,x+w-7,y+39,GOLD);
        g.centeredText(font,"✦   E V O L U T I O N   C R A F T   ✦",width/2,y+11,GOLD);
        g.centeredText(font,"DIARIO DEL AVENTURERO",width/2,y+25,MUTED);

        int subWidth=(w-24)/3;
        for(int i=0;i<3;i++){
            int xx=x+11+i*subWidth,yy=y+43;
            if(group==i){g.outline(xx-1,yy-1,subWidth-2,25,GOLD);g.fill(xx,yy,xx+subWidth-4,yy+2,GOLD);}
            g.item(new ItemStack(GROUP_ICONS[i]),xx+5,yy+4);
        }
        int[] sub=GROUPS[group];
        for(int i=0;i<sub.length;i++){
            int xx=x+11+(i%3)*subWidth,yy=y+75+(i/3)*24,selected=sub[i];
            if(tab==selected)g.outline(xx-1,yy-1,subWidth-2,23,accent);
            g.item(new ItemStack(ITEMS[selected]),xx+5,yy+3);
        }
        g.fill(x+11,y+127,x+w-11,y+158,0xFF24333D);
        g.fill(x+11,y+127,x+14,y+158,accent);
        g.centeredText(font,heading(),width/2,y+136,accent);
        JsonArray all=goals();
        int done=complete(all),count=all.size();
        g.fill(x+18,y+168,x+w-18,y+180,0xFF0B151D);
        if(count>0)g.fill(x+19,y+169,x+19+(w-38)*done/count,y+179,GREEN);
        g.text(font,done+" / "+count+" objetivos",x+17,y+187,WHITE);
        String state=detail();
        g.text(font,state,x+w-17-font.width(state),y+187,
            (tab==7||tab==8)?(state.equals("DESBLOQUEADO")?GREEN:RED):MUTED);
        g.fill(x+11,listTop()-3,x+w-11,listBottom()+2,0xFF0F1922);
        int begin=starts.get(page),end=page+1<starts.size()?starts.get(page+1):all.size();
        int cy=listTop()+3;
        for(int i=begin;i<end;i++){
            JsonObject obj=all.get(i).getAsJsonObject();
            boolean ok=obj.get("done").getAsBoolean();
            String label=obj.get("text").getAsString();
            List<String> lines=wrap(label,Math.max(80,w-103));
            int rowH=rowHeight(obj);
            if(i%2==0)g.fill(x+15,cy-2,x+w-15,cy+rowH-2,0xFF243440);
            g.fill(x+15,cy-2,x+18,cy+rowH-2,ok?GREEN:0xFFBB6D67);
            g.item(new ItemStack(icon(label)),x+24,cy+Math.max(0,(rowH-17)/2));
            g.text(font,ok?"✔":"◇",x+46,cy+Math.max(0,(rowH-11)/2),ok?GREEN:RED);
            int textY=cy+Math.max(1,(rowH-lines.size()*11)/2);
            for(String line:lines){g.text(font,line,x+64,textY,ok?GREEN:WHITE);textY+=11;}
            cy+=rowH;
        }
        if(group==2)g.centeredText(font,
            "EvolutionCraft completado: "+data.get("overallPercent").getAsInt()+"%",width/2,y+h-64,GOLD);
        else g.centeredText(font,"Progreso real del servidor · Pulsa Esc para salir",width/2,y+h-64,MUTED);
        g.centeredText(font,"Pagina "+(page+1)+" / "+starts.size(),width/2,y+h-46,MUTED);
        super.extractRenderState(g,mx,my,partial);
    }
    @Override public boolean isPauseScreen(){return false;}
}
