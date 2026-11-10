package es.evolutioncraft;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Diario RPG de EvolutionCraft 2.0.
 * Navegación dibujada a mano: no usa los botones grises estándar.
 * Los requisitos y niveles proceden exclusivamente del servidor.
 */
public final class ProgressScreen extends Screen {
    private static final int BG=0xFF101924, PANEL=0xFF172634, SIDEBAR=0xFF101D2B;
    private static final int CARD=0xFF223441, CARD2=0xFF1B2C3A, MUTED=0xFFA7BAC8;
    private static final int TEXT=0xFFF2F1E7, GOLD=0xFFF4CE82, GREEN=0xFF7BD9A4;
    private static final int RED=0xFFE78B83, DIM=0xFF526979, EDGE=0xFF896C45;
    private static final String[] SECTIONS={"EVOLUCION","HABILIDADES","DIMENSIONES","LOGROS","SECRETOS"};
    private static final Item[] SECTION_ICONS={Items.DIAMOND_PICKAXE,Items.ENCHANTED_BOOK,
        Items.ENDER_EYE,Items.BOOK,Items.NETHER_STAR};
    private static final int[][] SECTION_TABS={{0,10},{1,2,3,4,5,6},{7,8},{11},{9}};
    private static final String[] TITLES={"ERAS","PESCA","AGRICULTURA","GANADERIA",
        "EXPLORACION","HERRAMIENTAS","ALQUIMIA","NETHER","END","SECRETOS","MISIONES","RESUMEN"};
    private static final Item[] ICONS={Items.IRON_PICKAXE,Items.FISHING_ROD,Items.WHEAT,
        Items.WHEAT,Items.COMPASS,Items.DIAMOND_PICKAXE,Items.BREWING_STAND,
        Items.OBSIDIAN,Items.ENDER_EYE,Items.NETHER_STAR,Items.BOOK,Items.NETHER_STAR};

    private final JsonObject data;
    private int section=0,tab=0,stage=0,page=0;
    private String badge="";
    private final List<Integer> pageStarts=new ArrayList<>();

    public ProgressScreen(String json){
        super(Component.literal("EvolutionCraft - Diario del aventurero"));
        this.data=JsonParser.parseString(json).getAsJsonObject();
        this.stage=Math.min(4,Math.max(0,getInt("era")));
        if(data.has("selectedBadge")) badge=data.get("selectedBadge").getAsString();
    }
    private int getInt(String key){return data.has(key)?data.get(key).getAsInt():0;}
    private boolean getBool(String key){return data.has(key)&&data.get(key).getAsBoolean();}
    private int panelW(){return Math.min(635,width-12);}
    private int panelH(){return Math.min(455,height-12);}
    private int x(){return (width-panelW())/2;}
    private int y(){return (height-panelH())/2;}
    private int sidebarW(){return Math.min(140,Math.max(100,panelW()/4));}
    private int mainX(){return x()+sidebarW()+19;}
    private int mainW(){return panelW()-sidebarW()-31;}
    private int contentY(){return y()+208;}
    private int contentBottom(){return y()+panelH()-57;}
    private int navY(){return y()+91;}
    private int tabsY(){return y()+112;}

    private JsonArray goals(){
        if(tab==0)return data.getAsJsonObject("eras").getAsJsonArray("goals")
            .get(stage).getAsJsonObject().getAsJsonArray("goals");
        if(tab==11){
            JsonArray summary=new JsonArray();
            addSummary(summary,"Era actual: "+Progress.ERA_NAMES[getInt("era")],getInt("era")==5);
            String[] names={"Pesca","Agricultura","Ganaderia","Exploracion","Herramientas","Alquimia"};
            String[] keys={"fishing","agriculture","husbandry","exploration","tools","alchemy"};
            for(int i=0;i<keys.length;i++)
                addSummary(summary,names[i]+": nivel "+getInt(keys[i])+" / 5",getInt(keys[i])>=5);
            addSummary(summary,"Nether: "+(getBool("nether")?"desbloqueado":"bloqueado"),getBool("nether"));
            addSummary(summary,"End: "+(getBool("end")?"desbloqueado":"bloqueado"),getBool("end"));
            addSummary(summary,"Avances originales: "+getInt("advancements"),getInt("advancements")>=70);
            addSummary(summary,"Desafios secretos: "+getInt("secretCount")+" / 15",getInt("secretCount")>=15);
            return summary;
        }
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
    private static void addSummary(JsonArray list,String text,boolean complete){
        JsonObject o=new JsonObject();o.addProperty("text",text);o.addProperty("done",complete);list.add(o);
    }
    private static int completed(JsonArray goals){
        int n=0;for(var item:goals)if(item.getAsJsonObject().get("done").getAsBoolean())n++;
        return n;
    }
    private List<String> wrap(String input,int maxWidth){
        List<String> lines=new ArrayList<>();
        StringBuilder line=new StringBuilder();
        for(String word:input.split(" ")){
            String joined=line.isEmpty()?word:line+" "+word;
            if(font.width(joined)<=maxWidth){
                line.setLength(0);line.append(joined);continue;
            }
            if(!line.isEmpty()){lines.add(line.toString());line.setLength(0);}
            for(int i=0;i<word.length();i++){
                char ch=word.charAt(i);
                if(!line.isEmpty()&&font.width(line.toString()+ch)>maxWidth){
                    lines.add(line.toString());line.setLength(0);
                }
                line.append(ch);
            }
        }
        if(!line.isEmpty())lines.add(line.toString());
        if(lines.isEmpty())lines.add("");
        return lines;
    }
    private int rowHeight(JsonObject o){
        return Math.max(29,10+11*wrap(o.get("text").getAsString(),Math.max(45,mainW()-94)).size());
    }
    private void paginate(){
        pageStarts.clear();pageStarts.add(0);
        JsonArray arr=goals();
        int used=0,available=Math.max(29,contentBottom()-contentY()-8);
        for(int i=0;i<arr.size();i++){
            int h=rowHeight(arr.get(i).getAsJsonObject());
            if(used>0 && used+h>available){pageStarts.add(i);used=0;}
            used+=h;
        }
        page=Math.max(0,Math.min(page,pageStarts.size()-1));
    }
    @Override protected void init(){
        // Minecraft inicializa la fuente y las dimensiones antes de representar el primer fotograma.
        paginate();
    }
    private void switchSection(int next){
        section=next;tab=SECTION_TABS[next][0];page=0;paginate();
    }
    private void switchTab(int next){tab=next;page=0;paginate();}
    private boolean inside(double mx,double my,int xx,int yy,int ww,int hh){
        return mx>=xx&&mx<xx+ww&&my>=yy&&my<yy+hh;
    }
    private int gridX(int col,int cols){
        int left=mainX()+9;
        return left+col*(mainW()-18)/cols;
    }
    private int gridW(int cols){return (mainW()-18)/cols-5;}

    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){
        if(event.button()!=1)return super.mouseClicked(event,doubleClick);
        if(pageStarts.isEmpty())paginate();
        double mx=event.x(),my=event.y();
        int xx=x(),yy=y(),ww=panelW(),hh=panelH();
        if(inside(mx,my,xx+ww-34,yy+10,23,22)){onClose();return true;}
        for(int s=0;s<5;s++){
            if(inside(mx,my,xx+12,navY()+s*43,sidebarW()-8,37)){
                switchSection(s);return true;
            }
        }
        int[] subs=SECTION_TABS[section];
        int cols=Math.min(3,subs.length);
        for(int i=0;i<subs.length;i++){
            int tx=gridX(i%cols,cols),ty=tabsY()+(i/cols)*27;
            if(inside(mx,my,tx,ty,gridW(cols),24)){
                switchTab(subs[i]);return true;
            }
        }
        if(tab==0){
            if(inside(mx,my,mainX()+10,y()+159,24,23)&&stage>0){
                stage--;page=0;paginate();return true;
            }
            if(inside(mx,my,mainX()+mainW()-34,y()+159,24,23)&&stage<4){
                stage++;page=0;paginate();return true;
            }
        }
        int fy=y()+hh-45;
        if(inside(mx,my,mainX()+7,fy,34,29)&&page>0){page--;return true;}
        if(inside(mx,my,mainX()+mainW()-41,fy,34,29)&&page<pageStarts.size()-1){
            page++;return true;
        }
        if(section==4&&inside(mx,my,xx+12,yy+hh-58,sidebarW()-8,34)){
            changeBadge();return true;
        }
        return super.mouseClicked(event,doubleClick);
    }
    @Override public boolean mouseScrolled(double mx,double my,double scrollX,double scrollY){
        if(pageStarts.isEmpty())paginate();
        if(inside(mx,my,mainX(),contentY(),mainW(),contentBottom()-contentY())&&scrollY!=0){
            page=Math.max(0,Math.min(pageStarts.size()-1,page+(scrollY<0?1:-1)));
            return true;
        }
        return super.mouseScrolled(mx,my,scrollX,scrollY);
    }
    private void changeBadge(){
        JsonArray unlocked=data.getAsJsonArray("unlockedBadges");
        if(unlocked==null||unlocked.isEmpty())return;
        int i=-1;
        for(int n=0;n<unlocked.size();n++)if(unlocked.get(n).getAsString().equals(badge))i=n;
        badge=unlocked.get((i+1)%unlocked.size()).getAsString();
        var conn=net.minecraft.client.Minecraft.getInstance().getConnection();
        if(conn!=null)conn.sendCommand("ecdistintivo "+badge);
    }
    private String badgeLabel(){
        return switch(badge){
            case "noche" -> "Superviviente";
            case "tesoros" -> "Pescador afortunado";
            case "comerciante" -> "Mercader";
            case "tierra" -> "Granjero";
            case "animales" -> "Domador";
            case "diamantes" -> "Minero experto";
            case "fronteras" -> "Explorador";
            case "oceano" -> "Conquistador";
            case "arcano" -> "Encantador";
            case "elixir" -> "Alquimista";
            case "terror" -> "Cazador de pesadillas";
            case "infierno" -> "Señor del Nether";
            case "dragon" -> "Matadragones";
            case "equipo" -> "Maestro herrero";
            case "evolucion" -> "Leyenda de EvolutionCraft";
            default -> "Sin distintivo";
        };
    }
    private int accent(){
        return switch(tab){
            case 1 -> 0xFF77C7EF;
            case 2 -> 0xFF8FDB84;
            case 3 -> 0xFFE1BD88;
            case 4 -> 0xFF89D7BA;
            case 5 -> 0xFFB9B8E9;
            case 6 -> 0xFFB895EC;
            case 7 -> 0xFFF18B68;
            case 8 -> 0xFFD6A4F5;
            default -> GOLD;
        };
    }
    private String title(){
        return switch(tab){
            case 0 -> "EVOLUCION DE ERAS";
            case 1 -> "MAESTRIA DE PESCA";
            case 2 -> "AGRICULTURA";
            case 3 -> "GANADERIA";
            case 4 -> "EXPLORACION";
            case 5 -> "MAESTRIA DE HERRAMIENTAS";
            case 6 -> "ALQUIMIA";
            case 7 -> "PORTAL DEL NETHER";
            case 8 -> "PORTAL DEL END";
            case 9 -> "DESAFIOS OCULTOS";
            case 10 -> "MISIONES DE AVENTURA";
            default -> "TU EVOLUCION";
        };
    }
    private String detail(){
        return switch(tab){
            case 0 -> "Era actual: "+Progress.ERA_NAMES[getInt("era")];
            case 1 -> "Nivel "+getInt("fishing")+" / 5";
            case 2 -> "Nivel "+getInt("agriculture")+" / 5";
            case 3 -> "Nivel "+getInt("husbandry")+" / 5";
            case 4 -> "Nivel "+getInt("exploration")+" / 5";
            case 5 -> "Nivel "+getInt("tools")+" / 5";
            case 6 -> "Nivel "+getInt("alchemy")+" / 5";
            case 7 -> getBool("nether")?"DESBLOQUEADO":"BLOQUEADO";
            case 8 -> getBool("end")?"DESBLOQUEADO":"BLOQUEADO";
            case 9 -> getInt("secretCount")+" / 15 secretos";
            case 10 -> "6 misiones de era";
            default -> getInt("overallPercent")+"% completado";
        };
    }
    private Item objectiveItem(String label){
        String s=label.toLowerCase(Locale.ROOT);
        if(s.startsWith("???"))return Items.NETHER_STAR;
        if(s.contains("pico"))return Items.IRON_PICKAXE;
        if(s.contains("hacha"))return Items.IRON_AXE;
        if(s.contains("espada"))return Items.IRON_SWORD;
        if(s.contains("pala"))return Items.IRON_SHOVEL;
        if(s.contains("azada"))return Items.IRON_HOE;
        if(s.contains("casco"))return Items.IRON_HELMET;
        if(s.contains("peto"))return Items.IRON_CHESTPLATE;
        if(s.contains("pantalones"))return Items.IRON_LEGGINGS;
        if(s.contains("botas"))return Items.IRON_BOOTS;
        if(s.contains("pesca")||s.contains("peces")||s.contains("especies"))return Items.FISHING_ROD;
        if(s.contains("tesoro"))return Items.NAME_TAG;
        if(s.contains("bioma")||s.contains("estructura")||s.contains("aldea"))return Items.COMPASS;
        if(s.contains("pocion")||s.contains("tanda")||s.contains("efecto")||s.contains("alquimia"))return Items.BREWING_STAND;
        if(s.contains("trigo")||s.contains("cosecha"))return Items.WHEAT;
        if(s.contains("zanahoria")||s.contains("patata"))return Items.CARROT;
        if(s.contains("calabaza"))return Items.PUMPKIN;
        if(s.contains("melon"))return Items.MELON_SLICE;
        if(s.contains("cacao"))return Items.COCOA_BEANS;
        if(s.contains("verruga"))return Items.NETHER_WART;
        if(s.contains("lobo"))return Items.BONE;
        if(s.contains("gato"))return Items.COD;
        if(s.contains("caballo"))return Items.SADDLE;
        if(s.contains("warden"))return Items.SCULK_CATALYST;
        if(s.contains("wither"))return Items.WITHER_SKELETON_SKULL;
        if(s.contains("netherita"))return Items.NETHERITE_INGOT;
        if(s.contains("nether"))return Items.OBSIDIAN;
        if(s.contains("end"))return Items.ENDER_EYE;
        if(s.contains("diamante"))return Items.DIAMOND;
        if(s.contains("hierro"))return Items.IRON_INGOT;
        if(s.contains("piedra"))return Items.COBBLESTONE;
        if(s.contains("comerciar")||s.contains("intercambiar"))return Items.EMERALD;
        return ICONS[tab];
    }
    private void fill(GuiGraphicsExtractor g,int x1,int y1,int x2,int y2,int c){g.fill(x1,y1,x2,y2,c);}
    /** Pixel-art rounded rectangles: no external textures or render APIs required. */
    private void rounded(GuiGraphicsExtractor g,int xx,int yy,int ww,int hh,int radius,int color){
        if(ww<=0||hh<=0)return;
        int r=Math.min(radius,Math.min(ww/2,hh/2));
        if(r<2){fill(g,xx,yy,xx+ww,yy+hh,color);return;}
        fill(g,xx+r,yy,xx+ww-r,yy+hh,color);
        fill(g,xx,yy+r,xx+ww,yy+hh-r,color);
        for(int dy=0;dy<r;dy++){
            double t=r-dy-0.5;
            int inset=Math.max(0,(int)Math.ceil(r-Math.sqrt(r*r-t*t)));
            fill(g,xx+inset,yy+dy,xx+ww-inset,yy+dy+1,color);
            fill(g,xx+inset,yy+hh-dy-1,xx+ww-inset,yy+hh-dy,color);
        }
    }
    private void roundedBorder(GuiGraphicsExtractor g,int xx,int yy,int ww,int hh,
                               int radius,int edge,int middle){
        rounded(g,xx,yy,ww,hh,radius,edge);
        rounded(g,xx+1,yy+1,ww-2,hh-2,Math.max(2,radius-1),middle);
    }
    private void outline(GuiGraphicsExtractor g,int xx,int yy,int ww,int hh,int c){
        if(ww>2&&hh>2)g.outline(xx,yy,ww,hh,c);
    }
    private void text(GuiGraphicsExtractor g,String s,int xx,int yy,int c){g.text(font,s,xx,yy,c);}
    private void center(GuiGraphicsExtractor g,String s,int xx,int yy,int c){g.centeredText(font,s,xx,yy,c);}
    private void item(GuiGraphicsExtractor g,Item i,int xx,int yy){g.item(new ItemStack(i),xx,yy);}

    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta){
        if(pageStarts.isEmpty())paginate();
        int xx=x(),yy=y(),ww=panelW(),hh=panelH(),sx=xx+10,sw=sidebarW(),mx=mainX(),mw=mainW();
        int accent=accent();
        fill(g,0,0,width,height,0xC6000000);
        fill(g,xx-5,yy-5,xx+ww+5,yy+hh+5,EDGE);
        fill(g,xx-3,yy-3,xx+ww+3,yy+hh+3,0xFF080F18);
        fill(g,xx,yy,xx+ww,yy+hh,BG);
        fill(g,xx+5,yy+5,xx+ww-5,yy+49,0xFF263C39);
        fill(g,xx+5,yy+47,xx+ww-5,yy+49,GOLD);
        item(g,Items.ENCHANTED_BOOK,xx+18,yy+19);
        text(g,"EVOLUTIONCRAFT",xx+43,yy+13,GOLD);
        text(g,"D I A R I O    D E L    A V E N T U R E R O",xx+43,yy+28,MUTED);
        rounded(g,xx+ww-35,yy+11,24,22,5,
            inside(mouseX,mouseY,xx+ww-35,yy+11,24,22)?0xFFAD474A:0xFF432D34);
        center(g,"X",xx+ww-23,yy+18,TEXT);

        fill(g,sx,yy+60,sx+sw,yy+hh-11,SIDEBAR);
        fill(g,sx+sw+4,yy+60,sx+sw+5,yy+hh-11,0xFF334656);
        text(g,"SECCIONES",sx+13,yy+70,MUTED);
        for(int i=0;i<5;i++){
            int ny=navY()+i*43;
            boolean selected=section==i,hover=inside(mouseX,mouseY,xx+12,ny,sw-8,37);
            int navColor=selected?0xFF314A59:hover?0xFF253B4D:0xFF172938;
            roundedBorder(g,xx+12,ny,sw-8,37,6,
                selected?accent:hover?0xFF567184:0xFF284253,navColor);
            if(selected)fill(g,xx+14,ny+7,xx+17,ny+30,accent);
            item(g,SECTION_ICONS[i],xx+19,ny+10);
            text(g,SECTIONS[i],xx+40,ny+14,selected?TEXT:MUTED);
        }
        int base=yy+hh-94;
        text(g,"PROGRESO GLOBAL",sx+10,base,GOLD);
        fill(g,sx+10,base+16,sx+sw-9,base+25,0xFF080F15);
        fill(g,sx+11,base+17,sx+11+(sw-20)*Math.min(100,getInt("overallPercent"))/100,
            base+24,GREEN);
        text(g,getInt("overallPercent")+"% completado",sx+10,base+31,MUTED);
        if(section==4 && data.has("unlockedBadges")&&data.getAsJsonArray("unlockedBadges").size()>0){
            int by=yy+hh-58;
            roundedBorder(g,xx+12,by,sw-8,32,6,GOLD,
                inside(mouseX,mouseY,xx+12,by,sw-8,32)?0xFF3E5043:0xFF263E36);
            text(g,"CAMBIAR INSIGNIA",xx+18,by+10,GOLD);
        }

        fill(g,mx-3,yy+60,mx+mw+3,yy+hh-11,PANEL);
        fill(g,mx+6,yy+66,mx+mw-6,yy+98,0xFF233746);
        fill(g,mx+6,yy+66,mx+9,yy+98,accent);
        item(g,ICONS[tab],mx+15,yy+74);
        text(g,title(),mx+39,yy+73,accent);
        text(g,detail(),mx+39,yy+87,MUTED);
        int[] subtabs=SECTION_TABS[section];
        int cols=Math.min(3,subtabs.length);
        for(int i=0;i<subtabs.length;i++){
            int target=subtabs[i],tx=gridX(i%cols,cols),ty=tabsY()+(i/cols)*27;
            int tw=gridW(cols);
            boolean selected=tab==target,hover=inside(mouseX,mouseY,tx,ty,tw,24);
            int tabColor=selected?0xFF3B5262:hover?0xFF304557:0xFF192D3D;
            roundedBorder(g,tx,ty,tw,24,5,selected?accent:0xFF354F60,tabColor);
            if(selected)fill(g,tx+7,ty+21,tx+tw-7,ty+22,accent);
            item(g,ICONS[target],tx+7,ty+4);
            text(g,TITLES[target],tx+26,ty+8,selected?TEXT:MUTED);
        }
        if(tab==0){
            fill(g,mx+8,yy+159,mx+mw-8,yy+182,0xFF1A2C3A);
            center(g,Progress.ERA_NAMES[stage+1].toUpperCase(Locale.ROOT),
                mx+mw/2,yy+166,GOLD);
            if(stage>0)text(g,"<",mx+19,yy+165,TEXT);
            if(stage<4)text(g,">",mx+mw-27,yy+165,TEXT);
        }
        JsonArray list=goals();
        int done=completed(list),total=list.size();
        fill(g,mx+8,yy+183,mx+mw-8,yy+191,0xFF0A151D);
        if(total>0)fill(g,mx+9,yy+184,mx+9+(mw-18)*done/total,yy+190,GREEN);
        text(g,done+" / "+total+" objetivos",mx+9,yy+195,MUTED);
        String side=tab==9?"Distintivo: "+badgeLabel():tab==11?"Progreso de todas las habilidades":"";
        if(!side.isEmpty()&&font.width(side)<mw-150)
            text(g,side,mx+mw-9-font.width(side),yy+195,GOLD);
        fill(g,mx+7,contentY()-1,mx+mw-7,contentBottom()+2,0xFF101E2A);
        int begin=pageStarts.get(Math.max(0,Math.min(page,pageStarts.size()-1))),end=page+1<pageStarts.size()?pageStarts.get(page+1):list.size();
        int cy=contentY()+4;
        for(int i=begin;i<end;i++){
            JsonObject objective=list.get(i).getAsJsonObject();
            boolean ok=objective.get("done").getAsBoolean();
            String label=objective.get("text").getAsString();
            List<String> lines=wrap(label,Math.max(45,mw-94));
            int height=rowHeight(objective),left=mx+11,right=mx+mw-11;
            fill(g,left,cy,left+(right-left),cy+height-3,
                ok?0xFF213D39:i%2==0?CARD:CARD2);
            fill(g,left,cy,left+3,cy+height-3,ok?GREEN:DIM);
            item(g,objectiveItem(label),left+9,cy+Math.max(1,(height-17)/2));
            text(g,ok?"✔":"◇",left+31,cy+Math.max(1,(height-10)/2),ok?GREEN:RED);
            int textY=cy+Math.max(2,(height-lines.size()*11)/2);
            for(String line:lines){text(g,line,left+48,textY,ok?GREEN:TEXT);textY+=11;}
            cy+=height;
        }
        int footY=yy+hh-45;
        boolean hasPrev=page>0,hasNext=page<pageStarts.size()-1;
        roundedBorder(g,mx+7,footY,34,29,5,hasPrev?accent:DIM,
            hasPrev?0xFF314B5B:0xFF1B2B37);
        roundedBorder(g,mx+mw-41,footY,34,29,5,hasNext?accent:DIM,
            hasNext?0xFF314B5B:0xFF1B2B37);
        center(g,"<",mx+24,footY+10,hasPrev?TEXT:DIM);
        center(g,">",mx+mw-24,footY+10,hasNext?TEXT:DIM);
        center(g,"PAGINA "+(page+1)+" / "+pageStarts.size(),
            mx+mw/2,footY+9,MUTED);
        super.extractRenderState(g,mouseX,mouseY,delta);
    }
    @Override public boolean isPauseScreen(){return false;}
}
