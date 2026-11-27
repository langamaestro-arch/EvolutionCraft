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
    private static final int BG=FantasySkin.PARCHMENT, PANEL=FantasySkin.PARCHMENT, SIDEBAR=0xFFF1D6AE;
    private static final int CARD=FantasySkin.CARD, CARD2=0xFFF3D8AF, MUTED=FantasySkin.MUTED;
    private static final int TEXT=FantasySkin.INK, GOLD=0xFF9F5A20, GREEN=FantasySkin.GREEN;
    private static final int RED=0xFFA64C3F, DIM=0xFFAE8B68, EDGE=FantasySkin.FRAME;
    private static final String[] SECTIONS={"EVOLUCION","HABILIDADES","DIMENSIONES","LOGROS","SECRETOS","RECOMPENSAS","DESCUBRIMIENTOS","CONTRATOS","REPUTACION"};
    private static final Item[] SECTION_ICONS={Items.DIAMOND_PICKAXE,Items.ENCHANTED_BOOK,
        Items.ENDER_EYE,Items.BOOK,Items.NETHER_STAR,Items.CHEST,Items.COMPASS,Items.LECTERN,Items.NETHER_STAR};
    private static final int[][] SECTION_TABS={{0,10},{1,2,3,4,5,6},{7,8},{11},{9},{12},{13,14,15,16,17,18},{19,20,21,23},{22}};
    private static final String[] TITLES={"ERAS","PESCA","AGRICULTURA","GANADERIA",
        "EXPLORACION","HERRAMIENTAS","ALQUIMIA","NETHER","END","SECRETOS","MISIONES","RESUMEN","PREMIOS","OVERWORLD","NETHER","ESTRUCTURAS","JEFES","ESTADISTICAS","HISTORIAL","OFERTAS","ACTIVOS","COMPLETADOS","RANGOS","EPICOS"};
    private static final Item[] ICONS={Items.IRON_PICKAXE,Items.FISHING_ROD,Items.WHEAT,
        Items.WHEAT,Items.COMPASS,Items.DIAMOND_PICKAXE,Items.BREWING_STAND,
        Items.OBSIDIAN,Items.ENDER_EYE,Items.NETHER_STAR,Items.BOOK,Items.NETHER_STAR,Items.CHEST,
        Items.GRASS_BLOCK,Items.NETHERRACK,Items.COMPASS,Items.WITHER_SKELETON_SKULL,Items.WRITABLE_BOOK,Items.CLOCK,Items.PAPER,Items.WRITABLE_BOOK,Items.EMERALD,Items.NETHER_STAR,Items.WITHER_SKELETON_SKULL};

    private final JsonObject data;
    private int section=0,tab=0,stage=0,page=0;
    private String badge="";
    private int offerFilter=0;
    private static final String[] OFFER_FILTERS={"TODAS","PESCA","RECOLECCION","COMBATE","MINERIA"};
    private final List<Integer> pageStarts=new ArrayList<>();

    public ProgressScreen(String json){this(json,0);}
    public ProgressScreen(String json,int sectionToOpen){
        super(Component.literal("EvolutionCraft - Diario del aventurero"));
        this.data=JsonParser.parseString(json).getAsJsonObject();
        this.stage=Math.min(4,Math.max(0,getInt("era")));
        this.section=Math.max(0,Math.min(SECTION_TABS.length-1,sectionToOpen));
        this.tab=SECTION_TABS[this.section][0];
        if(data.has("selectedBadge")) badge=data.get("selectedBadge").getAsString();
    }
    public int getSection(){return section;}
    public int getTab(){return tab;}
    public int getOfferFilter(){return offerFilter;}
    public void setOfferFilter(int filter){offerFilter=Math.floorMod(filter,OFFER_FILTERS.length);page=0;}
    public void selectTab(int next){
        if(next>=19 && next<=21){
            this.section=7;
            this.tab=next;
            this.page=0;
            // init() calculará la paginación al abrir la pantalla.
        }
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
    private int navY(){return y()+48;}
    private int navHeight(){return 30;}
    private int navX(int i){return x()+14+i*(panelW()-28)/SECTIONS.length;}
    private int navW(int i){return navX(i+1)-navX(i)-2;}
    private static final String[] SHORT_SECTIONS={"Eras","Oficios","Mundos","Logros","Secretos","Premios","Diario","Contratos","Rangos"};
    private int tabsY(){return y()+112;}

    private JsonArray goals(){
        if(tab==23)return data.getAsJsonArray("epicContracts");
        if(tab==22)return data.getAsJsonObject("reputation").getAsJsonArray("goals");
        if(tab>=19 && tab<=21){
            String key=tab==19?"available":tab==20?"active":"completed";
            JsonArray all=data.getAsJsonObject("contracts").getAsJsonArray(key);
            if(tab!=19||offerFilter==0)return all;
            JsonArray filtered=new JsonArray();
            for(var obj:all){
                var row=obj.getAsJsonObject();
                if(row.has("category")&&row.get("category").getAsString().equals(OFFER_FILTERS[offerFilter]))
                    filtered.add(row);
            }
            return filtered;
        }
        if(tab>=13 && tab<=18){
            String key=switch(tab){
                case 13 -> "overworld";
                case 14 -> "nether";
                case 15 -> "structures";
                case 16 -> "bosses";
                case 17 -> "statistics";
                default -> "recent";
            };
            return data.getAsJsonObject("discoveries").getAsJsonArray(key);
        }
        if(tab==12)return data.getAsJsonObject("rewardGoals").getAsJsonArray("goals");
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
    private boolean secretEntry(JsonObject o){return o.has("rarity");}
    private int rarityColor(String rarity){
        return switch(rarity){
            case "Bronce" -> 0xFFD39A65;
            case "Plata" -> 0xFFC4D0E0;
            case "Oro" -> 0xFFF4D17B;
            default -> GOLD;
        };
    }
    private int rowHeight(JsonObject o){
        if(o.has("tribute"))return 44;
        if(o.has("threshold"))return 60;
        if(o.has("tier") && o.has("progress")){
            if(tab==23)return 78;
            int desc=wrap(o.get("description").getAsString(),Math.max(65,mainW()-96)).size();
            return Math.max(tab==20 && o.has("delivery") &&
                o.get("delivery").getAsBoolean()?90:56,46+desc*11);
        }
        if(o.has("detail")){
            int nameLines=wrap(o.get("text").getAsString(),Math.max(60,mainW()-100)).size();
            int details=wrap(o.get("detail").getAsString(),Math.max(60,mainW()-100)).size();
            return Math.max(43,10+11*(nameLines+details)+7);
        }
        if(tab==12 && o.has("xp")){
            return Math.max(48,31+11*wrap(o.get("items").getAsString(),Math.max(70,mainW()-126)).size());
        }
        if(secretEntry(o)){
            String second=o.get("done").getAsBoolean()
                ?"Distintivo: "+o.get("reward").getAsString()
                :"Pista: "+o.get("hint").getAsString();
            int lineCount=wrap(second,Math.max(55,mainW()-92)).size();
            return Math.max(47,32+lineCount*11);
        }
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
        for(int s=0;s<SECTIONS.length;s++){
            if(inside(mx,my,navX(s),navY(),navW(s),navHeight())){
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
        if(tab==22){
            JsonArray ranks=goals();
            int begin=pageStarts.get(page),end=page+1<pageStarts.size()?pageStarts.get(page+1):ranks.size();
            int cy=contentY()+4;
            for(int i=begin;i<end;i++){
                JsonObject rank=ranks.get(i).getAsJsonObject();
                int h=rowHeight(rank);
                if(rank.get("ready").getAsBoolean()&&
                    inside(mx,my,mainX()+mainW()-111,cy+4,100,19)){
                    var connection=net.minecraft.client.Minecraft.getInstance().getConnection();
                    if(connection!=null)connection.sendCommand("ecrango "+rank.get("id").getAsString());
                    return true;
                }
                cy+=h;
            }
        }
        if(tab==19 && inside(mx,my,mainX()+8,y()+157,mainW()-16,25)){
            offerFilter=(offerFilter+1)%OFFER_FILTERS.length;
            page=0;
            paginate();
            return true;
        }
        if(tab==23){
            JsonArray contracts=goals();
            int begin=pageStarts.get(page);
            int end=page+1<pageStarts.size()?pageStarts.get(page+1):contracts.size();
            int cy=contentY()+4;
            for(int i=begin;i<end;i++){
                JsonObject contract=contracts.get(i).getAsJsonObject();
                int h=rowHeight(contract);
                if(inside(mx,my,mainX()+mainW()-111,cy+4,100,19)){
                    String status=contract.get("epicStatus").getAsString();
                    String action=status.equals("offer")?
                        (contract.get("canAccept").getAsBoolean()?"aceptar":""):
                        status.equals("active")?
                            (contract.get("ready").getAsBoolean()?"reclamar":"abandonar"):"";
                    if(!action.isEmpty()){
                        var connection=net.minecraft.client.Minecraft.getInstance().getConnection();
                        if(connection!=null)connection.sendCommand(
                            "ecepico "+action+" "+contract.get("id").getAsString());
                        return true;
                    }
                }
                cy+=h;
            }
        }
        if(tab>=19 && tab<=21 && data.getAsJsonObject("contracts").get("enabled").getAsBoolean()){
            JsonArray contracts=goals();
            int begin=pageStarts.get(page);
            int end=page+1<pageStarts.size()?pageStarts.get(page+1):contracts.size();
            int cy=contentY()+4;
            for(int i=begin;i<end;i++){
                JsonObject contract=contracts.get(i).getAsJsonObject();
                int h=rowHeight(contract);
                boolean isDelivery=contract.has("delivery")&&contract.get("delivery").getAsBoolean();
                boolean abandonButton=tab==20&&isDelivery&&
                    inside(mx,my,mainX()+mainW()-111,cy+27,100,19);
                if(abandonButton||inside(mx,my,mainX()+mainW()-111,cy+4,100,19)){
                    String action=abandonButton?"abandonar":tab==19?"aceptar":tab==20?
                        contract.get("ready").getAsBoolean()?"reclamar":
                        isDelivery?"entregar":"abandonar":"";
                    if(!action.isEmpty() && (tab!=19||contract.get("canAccept").getAsBoolean())){
                        var connection=net.minecraft.client.Minecraft.getInstance().getConnection();
                        if(connection!=null)connection.sendCommand(
                            "eccontrato "+action+" "+contract.get("id").getAsString());
                        return true;
                    }
                }
                cy+=h;
            }
        }
        if(tab==12){
            JsonArray rewards=goals();
            int begin=pageStarts.get(page);
            int end=page+1<pageStarts.size()?pageStarts.get(page+1):rewards.size();
            int cy=contentY()+4;
            for(int i=begin;i<end;i++){
                JsonObject reward=rewards.get(i).getAsJsonObject();
                int h=rowHeight(reward);
                if(reward.get("ready").getAsBoolean() &&
                    inside(mx,my,mainX()+11,cy,mainW()-22,h-3)){
                    var connection=net.minecraft.client.Minecraft.getInstance().getConnection();
                    if(connection!=null)connection.sendCommand("ecreclamar "+reward.get("id").getAsString());
                    return true;
                }
                cy+=h;
            }
        }
        if(tab==0){
            JsonArray rows=goals();
            int from=pageStarts.get(page);
            int to=page+1<pageStarts.size()?pageStarts.get(page+1):rows.size();
            int yyRow=contentY()+4;
            for(int i=from;i<to;i++){
                JsonObject row=rows.get(i).getAsJsonObject();
                if(row.has("tribute")&&row.get("canDeliver").getAsBoolean()&&
                    inside(mx,my,mainX()+mainW()-111,yyRow+4,100,19)){
                    var conn=net.minecraft.client.Minecraft.getInstance().getConnection();
                    if(conn!=null)conn.sendCommand("ectributo entregar");
                    return true;
                }
                yyRow+=rowHeight(row);
            }
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
            case 19,20,21 -> 0xFFF1CD87;
            case 22 -> 0xFFD8B8F4;
            case 23 -> 0xFFC4483E;
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
            case 12 -> "RECOMPENSAS DEL AVENTURERO";
            case 13 -> "BIOMAS DEL OVERWORLD";
            case 14 -> "BIOMAS DEL NETHER";
            case 15 -> "ESTRUCTURAS DESCUBIERTAS";
            case 16 -> "JEFES DERROTADOS";
            case 17 -> "ESTADISTICAS DE AVENTURA";
            case 18 -> "ULTIMOS DESCUBRIMIENTOS";
            case 19 -> "TABLON DE CONTRATOS";
            case 20 -> "CONTRATOS ACTIVOS";
            case 21 -> "CONTRATOS COMPLETADOS";
            case 22 -> "REPUTACION DEL AVENTURERO";
            case 23 -> "CONTRATOS EPICOS Y JEFES";
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
            case 12 -> getInt("pendingRewards")+" premios por reclamar";
            case 13 -> goals().size()+" biomas registrados";
            case 14 -> goals().size()+" biomas registrados";
            case 15 -> goals().size()+" estructuras registradas";
            case 16 -> goals().size()+" jefes registrados";
            case 17 -> "Tu aventura en numeros";
            case 18 -> goals().size()+" registros recientes";
            case 19 -> "12 ofertas · 3 por categoria · 3 activos max.";
            case 20 -> "Activos: "+goals().size()+" / 3";
            case 21 -> "Reclamados: "+goals().size();
            case 23 -> "Desafios especiales · 1 activo · Reputacion y era";
            case 22 -> data.getAsJsonObject("reputation").get("rank").getAsString()+" · "+
                data.getAsJsonObject("reputation").get("points").getAsInt()+" puntos";
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
    private void corner(GuiGraphicsExtractor g,int xx,int yy,int dirX,int dirY,int color){
        fill(g,Math.min(xx,xx+dirX*15),yy,Math.max(xx,xx+dirX*15)+1,yy+1,color);
        fill(g,xx,Math.min(yy,yy+dirY*15),xx+1,Math.max(yy,yy+dirY*15)+1,color);
        fill(g,Math.min(xx+dirX*3,xx+dirX*8),yy+dirY*3,
            Math.max(xx+dirX*3,xx+dirX*8)+1,yy+dirY*3+1,color);
    }
    private String motto(){
        return switch(section){
            case 0 -> "Cada era comienza con un nuevo desafío";
            case 1 -> "La maestría se forja con experiencia";
            case 2 -> "Dos mundos esperan tu conquista";
            case 3 -> "Cada paso forma parte de tu historia";
            case 4 -> "El misterio recompensa a los valientes";
            case 5 -> "Tus hazañas merecen una recompensa";
            case 6 -> "Cada lugar descubierto cuenta tu historia";
            case 7 -> "Acepta contratos y recibe recompensas";
            default -> "Tu leyenda crece con cada contrato";
        };
    }
    /** Pixel-art rounded rectangles: no external textures or render APIs required. */
    /** La geometría de interacción permanece intacta; cambia sólo la apariencia. */
    private void rounded(GuiGraphicsExtractor g,int xx,int yy,int ww,int hh,int radius,int color){
        FantasySkin.card(g,xx,yy,ww,hh,false);
    }
    private void roundedBorder(GuiGraphicsExtractor g,int xx,int yy,int ww,int hh,
                               int radius,int edge,int middle){
        FantasySkin.card(g,xx,yy,ww,hh,edge==GOLD || edge==GREEN || edge==0xFFE4A94A);
    }
    private void outline(GuiGraphicsExtractor g,int xx,int yy,int ww,int hh,int c){
        if(ww>2&&hh>2)g.outline(xx,yy,ww,hh,c);
    }
    private void text(GuiGraphicsExtractor g,String s,int xx,int yy,int c){g.text(font,s,xx,yy,c,false);}
    private void center(GuiGraphicsExtractor g,String s,int xx,int yy,int c){g.text(font,s,xx-font.width(s)/2,yy,c,false);}
    private void item(GuiGraphicsExtractor g,Item i,int xx,int yy){g.item(new ItemStack(i),xx,yy);}

    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta){
        if(pageStarts.isEmpty())paginate();
        int xx=x(),yy=y(),ww=panelW(),hh=panelH(),sx=xx+10,sw=sidebarW(),mx=mainX(),mw=mainW();
        int accent=accent();
        fill(g,0,0,width,height,0xB9000000);
        FantasySkin.book(g,xx-4,yy-4,ww+8,hh+8,mx-7);
        FantasySkin.ribbon(g,xx+12,yy+5,ww-24,38);
        item(g,Items.ENCHANTED_BOOK,xx+22,yy+15);
        center(g,"EVOLUTIONCRAFT",xx+ww/2,yy+11,0xFF67331E);
        center(g,"CRONICAS DEL AVENTURERO",xx+ww/2,yy+24,0xFF814923);
        FantasySkin.card(g,xx+ww-35,yy+11,24,22,
            inside(mouseX,mouseY,xx+ww-35,yy+11,24,22));
        center(g,"X",xx+ww-23,yy+18,RED);
        for(int i=0;i<SECTIONS.length;i++){
            int nx=navX(i),ny=navY(),nw=navW(i);
            boolean active=section==i,hover=inside(mouseX,mouseY,nx,ny,nw,navHeight());
            FantasySkin.nav(g,nx,ny,nw,navHeight(),active||hover);
            item(g,SECTION_ICONS[i],nx+(nw-16)/2,ny+2);
            if(font.width(SHORT_SECTIONS[i])<=nw-2)
                center(g,SHORT_SECTIONS[i],nx+nw/2,ny+19,active?0xFF65341C:TEXT);
            if(active)fill(g,nx+4,ny+27,nx+nw-4,ny+29,0xFFB77A28);
        }
        center(g,"TU AVENTURA",sx+sw/2,yy+91,GOLD);
        fill(g,sx+8,yy+105,sx+sw-7,yy+106,0xFFBC8951);
        item(g,Items.DIAMOND_PICKAXE,sx+sw/2-8,yy+116);
        center(g,"Era actual",sx+sw/2,yy+138,MUTED);
        int eraY=yy+151;
        for(String line:wrap(Progress.ERA_NAMES[Math.max(0,Math.min(5,getInt("era")))],sw-14)){
            center(g,line,sx+sw/2,eraY,TEXT);eraY+=11;
        }
        int dy=eraY+12;
        if(dy+55<yy+hh-95){
            center(g,"DIMENSIONES",sx+sw/2,dy,GOLD);
            center(g,"Nether: "+(getBool("nether")?"Abierto":"Cerrado"),sx+sw/2,dy+17,getBool("nether")?GREEN:RED);
            center(g,"End: "+(getBool("end")?"Abierto":"Cerrado"),sx+sw/2,dy+32,getBool("end")?GREEN:RED);
        }
        int base=yy+hh-94;
        if(base>yy+170){
            center(g,"PROGRESO",sx+sw/2,base,GOLD);
            FantasySkin.progress(g,sx+10,base+16,sw-19,11,Math.min(100,getInt("overallPercent")),100,GREEN);
            center(g,getInt("overallPercent")+"% completado",sx+sw/2,base+31,MUTED);
        }
        if(section==4 && data.has("unlockedBadges")&&data.getAsJsonArray("unlockedBadges").size()>0){
            int by=yy+hh-58;
            FantasySkin.card(g,xx+12,by,sw-8,32,inside(mouseX,mouseY,xx+12,by,sw-8,32));
            center(g,"INSIGNIA",xx+12+(sw-8)/2,by+10,GOLD);
        }

        FantasySkin.card(g,mx+5,yy+80,mw-10,31,false);
        FantasySkin.ribbon(g,mx+10,yy+82,mw-20,14);
        
        item(g,ICONS[tab],mx+16,yy+85);
        text(g,title(),mx+40,yy+84,0xFF67361F);
        text(g,detail(),mx+40,yy+99,MUTED);
        // Emblema derecho: cambia con la sección activa.
        FantasySkin.card(g,mx+mw-38,yy+81,27,27,false);
        item(g,SECTION_ICONS[section],mx+mw-32,yy+86);
        int[] subtabs=SECTION_TABS[section];
        int cols=Math.min(3,subtabs.length);
        for(int i=0;i<subtabs.length;i++){
            int target=subtabs[i],tx=gridX(i%cols,cols),ty=tabsY()+(i/cols)*27;
            int tw=gridW(cols);
            boolean selected=tab==target,hover=inside(mouseX,mouseY,tx,ty,tw,24);
            FantasySkin.nav(g,tx,ty,tw,24,selected||hover);
            if(selected)fill(g,tx+7,ty+21,tx+tw-7,ty+22,0xFFE09B2E);
            item(g,ICONS[target],tx+7,ty+4);
            text(g,TITLES[target],tx+26,ty+8,selected?0xFF6D361C:MUTED);
        }
        if(tab==22){
            JsonObject rep=data.getAsJsonObject("reputation");
            int points=rep.get("points").getAsInt(),lower=rep.get("lower").getAsInt();
            int next=rep.get("next").getAsInt();
            int trackX=mx+18,trackY=yy+160,trackWidth=mw-36;
            FantasySkin.progress(g,trackX,trackY,trackWidth,9,points-lower,Math.max(1,next-lower),0xFFDDA43E);
            int progress=rep.get("maxRank").getAsBoolean()?trackWidth:
                (int)(trackWidth*((double)Math.max(0,points-lower)/Math.max(1,next-lower)));
            // El relleno del pergamino ya representa este progreso.
            center(g,rep.get("rank").getAsString()+" · "+points+
                (rep.get("maxRank").getAsBoolean()?" · rango maximo":" / "+next+" REP"),
                mx+mw/2,yy+174,MUTED);
        }
        if(tab!=0 && tab!=22){
            String phrase=tab==19?"FILTRO: "+OFFER_FILTERS[offerFilter]+"  < Cambiar >":motto();
            if(tab==19)FantasySkin.card(g,mx+8,yy+158,mw-16,23,true);
            if(font.width(phrase)<mw-30)
                center(g,phrase,mx+mw/2,yy+169,tab==19?GOLD:0xFF8FAAA9);
        }
        if(tab==0){
            FantasySkin.card(g,mx+8,yy+159,mw-16,23,true);
            center(g,Progress.ERA_NAMES[stage+1].toUpperCase(Locale.ROOT),
                mx+mw/2,yy+166,GOLD);
            if(stage>0)text(g,"<",mx+19,yy+165,TEXT);
            if(stage<4)text(g,">",mx+mw-27,yy+165,TEXT);
        }
        JsonArray list=goals();
        int done=completed(list),total=list.size();
        FantasySkin.progress(g,mx+8,yy+183,mw-16,9,done,Math.max(1,total),0xFFE1A849);
        text(g,tab==23?"Epicos: "+total:tab>=19?"Contratos: "+total:tab>=13?"Registros: "+total:done+" / "+total+" objetivos",mx+9,yy+195,MUTED);
        String side=tab==9?"Distintivo: "+badgeLabel():tab==11?"Progreso de todas las habilidades":"";
        if(!side.isEmpty()&&font.width(side)<mw-150)
            text(g,side,mx+mw-9-font.width(side),yy+195,GOLD);
        FantasySkin.card(g,mx+7,contentY()-1,mw-14,contentBottom()-contentY()+3,false);
        int begin=pageStarts.get(Math.max(0,Math.min(page,pageStarts.size()-1))),end=page+1<pageStarts.size()?pageStarts.get(page+1):list.size();
        int cy=contentY()+4;
        for(int i=begin;i<end;i++){
            JsonObject objective=list.get(i).getAsJsonObject();
            boolean ok=objective.get("done").getAsBoolean();
            String label=objective.get("text").getAsString();
            int height=rowHeight(objective),left=mx+11,cardW=mw-22;
            if(objective.has("tribute")){
                boolean ready=objective.get("canDeliver").getAsBoolean();
                int color=ok?GREEN:ready?GOLD:0xFF95643A;
                roundedBorder(g,left,cy,cardW,height-3,5,color,CARD);
                item(g,Items.AMETHYST_SHARD,left+11,cy+10);
                text(g,label,left+31,cy+7,ok?GREEN:TEXT);
                text(g,objective.get("description").getAsString(),left+31,cy+23,MUTED);
                boolean needsPriorEra=objective.has("needsPriorEra") &&
                    objective.get("needsPriorEra").getAsBoolean();
                String action=ok?"ENTREGADO":ready?"ENTREGAR":
                    needsPriorEra?"ERA "+objective.get("priorEra").getAsString().toUpperCase(Locale.ROOT):
                    "PENDIENTE";
                roundedBorder(g,left+cardW-103,cy+3,96,19,4,color,CARD2);
                text(g,action,left+cardW-8-font.width(action),cy+8,color);
            } else if(objective.has("threshold")){
                boolean unlocked=objective.get("unlocked").getAsBoolean();
                boolean ready=objective.get("ready").getAsBoolean();
                int color=ok?GREEN:ready?GOLD:unlocked?TEXT:MUTED;
                roundedBorder(g,left,cy,cardW,height-3,5,color,i%2==0?CARD:CARD2);
                fill(g,left+2,cy+7,left+5,cy+height-10,color);
                item(g,Items.NETHER_STAR,left+10,cy+11);
                text(g,label.toUpperCase(Locale.ROOT),left+31,cy+7,color);
                String action=ok?"RECLAMADO":ready?"RECLAMAR":"BLOQUEADO";
                roundedBorder(g,left+cardW-103,cy+3,96,19,4,color,0xFF182B37);
                text(g,action,left+cardW-8-font.width(action),cy+8,color);
                text(g,"Requiere "+objective.get("threshold").getAsInt()+" puntos",left+31,cy+23,MUTED);
                for(String line:wrap(objective.get("reward").getAsString(),Math.max(65,mw-114)))
                    {text(g,line,left+31,cy+38,unlocked?GREEN:MUTED);break;}
            } else if(objective.has("tier")&&objective.has("progress")){
                String tier=objective.get("tier").getAsString();
                int tierColor=tab==23?0xFFA63D34:tier.equals("ORO")?0xFF95601E:tier.equals("PLATA")?0xFF556B7C:0xFF985224;
                roundedBorder(g,left,cy,cardW,height-3,5,tierColor,i%2==0?CARD:CARD2);
                fill(g,left+2,cy+8,left+5,cy+height-11,tierColor);
                item(g,Items.PAPER,left+9,cy+10);
                text(g,label,left+31,cy+7,TEXT);
                String action=tab==23?
                    (objective.get("epicStatus").getAsString().equals("claimed")?"RECLAMADO":
                     objective.get("epicStatus").getAsString().equals("locked")?"BLOQUEADO":
                     objective.get("epicStatus").getAsString().equals("offer")?
                     (objective.get("canAccept").getAsBoolean()?"ACEPTAR":"BLOQUEADO"):
                     objective.get("ready").getAsBoolean()?"RECLAMAR":"ABANDONAR"):
                    tab==21?"RECLAMADO":tab==19?
                    (objective.get("canAccept").getAsBoolean()?"ACEPTAR":"BLOQUEADO"):
                    (objective.get("ready").getAsBoolean()?"RECLAMAR":
                    objective.has("delivery")&&objective.get("delivery").getAsBoolean()
                        ?"ENTREGAR":"ABANDONAR");
                roundedBorder(g,left+cardW-103,cy+3,96,19,4,tierColor,0xFF182B37);
                text(g,action,left+cardW-8-font.width(action),cy+8,tierColor);
                if(tab==20 && objective.has("delivery") && objective.get("delivery").getAsBoolean()){
                    roundedBorder(g,left+cardW-103,cy+26,96,19,4,MUTED,0xFF182B37);
                    String leave="ABANDONAR";
                    text(g,leave,left+cardW-8-font.width(leave),cy+31,MUTED);
                }
                String description=objective.get("description").getAsString();
                int detailY=cy+21;
                for(String line:wrap(description,Math.max(65,mw-96))){
                    text(g,line,left+31,detailY,MUTED);detailY+=11;
                }
                if(tab==20 && objective.has("inventory") && objective.has("missing")){
                    int owned=objective.get("inventory").getAsInt();
                    int missing=objective.get("missing").getAsInt();
                    String info=objective.get("ready").getAsBoolean()
                        ?"Materiales entregados · recompensa disponible"
                        :"Disponibles: "+owned+" · faltan "+missing;
                    text(g,info,left+31,detailY+2,missing==0?GREEN:MUTED);
                    detailY+=12;
                }
                String reward=objective.get("reward").getAsString();
                int count=objective.get("progress").getAsInt(),required=objective.get("target").getAsInt();
                String category=objective.has("category")?objective.get("category").getAsString():"";
                if(tab==23){
                    String req=objective.get("requires").getAsString();
                    boolean unlocked=objective.get("epicStatus").getAsString().equals("offer") ||
                        objective.get("epicStatus").getAsString().equals("active") ||
                        objective.get("epicStatus").getAsString().equals("claimed");
                    text(g,req,left+31,detailY+1,unlocked?GREEN:0xFF95422E);
                    text(g,tier+" · "+count+"/"+required+" · "+
                        objective.get("origin").getAsString(),left+31,detailY+13,tierColor);
                    text(g,reward,left+31,detailY+25,TEXT);
                }else{
                    String footer=category+" · "+tier+" · "+Math.min(count,required)+"/"+required+" · "+reward;
                    text(g,footer,left+31,detailY+2,tierColor);
                }
            } else if(objective.has("detail")){
                roundedBorder(g,left,cy,cardW,height-3,5,0xFF3C5261,i%2==0?CARD:CARD2);
                fill(g,left+2,cy+7,left+5,cy+height-10,GOLD);
                item(g,ICONS[tab],left+10,cy+Math.max(5,(height-17)/2));
                int yyRow=cy+7;
                for(String line:wrap(label,Math.max(60,mw-100))){
                    text(g,line,left+31,yyRow,TEXT);
                    yyRow+=11;
                }
                yyRow+=2;
                for(String line:wrap(objective.get("detail").getAsString(),Math.max(60,mw-100))){
                    text(g,line,left+31,yyRow,MUTED);
                    yyRow+=11;
                }
            } else if(tab==12 && objective.has("xp")){
                boolean ready=objective.get("ready").getAsBoolean();
                int border=ok?GREEN:ready?GOLD:DIM;
                boolean hover=inside(mouseX,mouseY,left,cy,cardW,height-3);
                roundedBorder(g,left,cy,cardW,height-3,5,
                    hover&&ready?GOLD:0xFF405664,ok?0xFF203B37:ready?CARD:CARD2);
                fill(g,left+2,cy+7,left+5,cy+height-10,border);
                item(g,ok?Items.EMERALD:Items.CHEST,left+10,cy+10);
                String prizeName=label;
                text(g,prizeName,left+31,cy+7,ok?GREEN:TEXT);
                String action=ok?"RECLAMADO":ready?"RECLAMAR":"BLOQUEADO";
                text(g,action,left+cardW-8-font.width(action),cy+7,border);
                String xpDetail=objective.get("xp").getAsInt()>0?
                    " + "+objective.get("xp").getAsInt()+" XP":"";
                String contents=objective.get("items").getAsString()+xpDetail;
                int ty=cy+22;
                for(String line:wrap(contents,Math.max(70,mw-126))){
                    text(g,line,left+31,ty,MUTED);ty+=11;
                }
            } else if(secretEntry(objective)){
                String rarity=objective.get("rarity").getAsString();
                int tierColor=rarityColor(rarity);
                boolean hover=inside(mouseX,mouseY,left,cy,cardW,height-3);
                roundedBorder(g,left,cy,cardW,height-3,5,
                    hover?tierColor:0xFF394A56,
                    ok?0xFF203B37:hover?0xFF2A3D4A:i%2==0?CARD:CARD2);
                fill(g,left+2,cy+8,left+5,cy+height-11,ok?GREEN:tierColor);
                item(g,ok?Items.NETHER_STAR:Items.ENCHANTED_BOOK,left+10,cy+10);
                String heading=ok?label:"??? · DESAFIO OCULTO";
                text(g,heading,left+31,cy+8,ok?GREEN:TEXT);
                String tier="["+rarity.toUpperCase(Locale.ROOT)+"]";
                text(g,tier,left+cardW-9-font.width(tier),cy+8,tierColor);
                String hint=ok?"Distintivo: "+objective.get("reward").getAsString():
                    "Pista: "+objective.get("hint").getAsString();
                int hintY=cy+24;
                for(String line:wrap(hint,Math.max(55,mw-92))){
                    text(g,line,left+31,hintY,ok?GREEN:MUTED);
                    hintY+=11;
                }
                if(hover && !ok)fill(g,left+7,cy+height-7,left+cardW-7,cy+height-6,tierColor);
            } else {
                List<String> lines=wrap(label,Math.max(45,mw-94));
                rounded(g,left,cy,cardW,height-3,4,
                    ok?0xFF213D39:i%2==0?CARD:CARD2);
                fill(g,left+2,cy+7,left+5,cy+height-10,ok?GREEN:DIM);
                item(g,objectiveItem(label),left+9,cy+Math.max(1,(height-17)/2));
                text(g,ok?"✔":"◇",left+31,cy+Math.max(1,(height-10)/2),ok?GREEN:RED);
                int textY=cy+Math.max(2,(height-lines.size()*11)/2);
                for(String line:lines){text(g,line,left+48,textY,ok?GREEN:TEXT);textY+=11;}
            }
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
        FantasySkin.diamond(g,mx+mw/2,footY+23,3,FantasySkin.PURPLE);
        super.extractRenderState(g,mouseX,mouseY,delta);
    }
    @Override public boolean isPauseScreen(){return false;}
}
