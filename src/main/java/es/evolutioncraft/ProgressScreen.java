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
    private int panelW(){return Math.min(780,width-8);}
    private int panelH(){return Math.min(485,height-8);}
    private int x(){return (width-panelW())/2;}
    private int y(){return (height-panelH())/2;}
    private int px(int n){return x()+panelW()*n/1000;}
    private int py(int n){return y()+panelH()*n/1000;}
    private int leftX(){return px(85);}
    private int rightX(){return px(535);}
    private int pageW(){return panelW()*380/1000;}
    private int mainW(){return pageW();}
    private int contentY(){return py(365);}
    private int contentBottom(){return py(830);}
    private int selected=0, detailPage=0;
    private static final String[] SHORT_SECTIONS={"Eras","Oficios","Mundos","Logros","Secretos","Premios","Diario","Contratos","Rangos"};
    private static final String[] SECTION_ART={"category_eras","category_farming","category_nether","category_missions","medal_gold","category_rewards","category_discoveries","category_contracts","merit_three"};
    private static final String[] TAB_ART={"category_eras","category_fishing","category_farming","category_husbandry","category_exploration","iron_pickaxe","category_alchemy","category_nether","category_end","medal_gold","category_missions","category_eras","category_rewards","category_biomes","category_nether","category_exploration","category_bosses","category_discoveries","category_discoveries","category_contracts","category_contracts","category_contracts","merit_three","category_bosses"};
    private int navX(int i){return px(67)+i*(panelW()*866/1000)/9;}
    private int navW(){return panelW()*866/9000-2;}
    private int navY(){return py(135);}
    private int navHeight(){return Math.max(24,panelH()*95/1000);}
    private int subX(int i,int count){return leftX()+(i%Math.min(3,count))*pageW()/Math.min(3,count);}
    private int subW(int count){return pageW()/Math.min(3,count)-2;}
    private int subY(int i,int count){return py(240)+(i/Math.min(3,count))*19;}

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
    private int rowHeight(JsonObject o){return Math.max(23,Math.min(38,(contentBottom()-contentY())/5));}
    private int rowsPerPage(){return Math.max(1,(contentBottom()-contentY())/rowHeight(null));}
    private void paginate(){
        pageStarts.clear();pageStarts.add(0);
        for(int i=rowsPerPage();i<goals().size();i+=rowsPerPage())pageStarts.add(i);
        page=Math.max(0,Math.min(page,pageStarts.size()-1));
        selected=Math.max(pageStarts.get(page),Math.min(selected,Math.min(goals().size()-1,pageStarts.get(page)+rowsPerPage()-1)));
    }
    @Override protected void init(){paginate();}
    private void switchSection(int next){section=next;tab=SECTION_TABS[next][0];resetSelection();}
    private void switchTab(int next){tab=next;resetSelection();}
    private void resetSelection(){page=0;selected=0;detailPage=0;paginate();}
    private boolean inside(double mx,double my,int xx,int yy,int ww,int hh){return mx>=xx&&mx<xx+ww&&my>=yy&&my<yy+hh;}
    private void turn(int step){page=Math.max(0,Math.min(pageStarts.size()-1,page+step));selected=pageStarts.get(page);detailPage=0;}
    private JsonObject chosen(){JsonArray a=goals();return a.isEmpty()?null:a.get(Math.max(0,Math.min(selected,a.size()-1))).getAsJsonObject();}
    private static String val(JsonObject o,String k){return o.has(k)&&!o.get(k).isJsonNull()?o.get(k).getAsString():"";}
    private static boolean flag(JsonObject o,String k){return o.has(k)&&o.get(k).getAsBoolean();}
    private String action(JsonObject o){
        if(o==null)return "";
        if(tab==0&&flag(o,"tribute")&&flag(o,"canDeliver"))return "ENTREGAR";
        if(tab==12&&flag(o,"ready"))return "RECLAMAR";
        if(tab==22&&flag(o,"ready"))return "RECLAMAR";
        if(tab==23){String s=val(o,"epicStatus");return s.equals("offer")&&flag(o,"canAccept")?"ACEPTAR":s.equals("active")?(flag(o,"ready")?"RECLAMAR":"ABANDONAR"):"";}
        if(tab==19&&flag(o,"canAccept"))return "ACEPTAR";
        if(tab==20)return flag(o,"ready")?"RECLAMAR":flag(o,"delivery")?"ENTREGAR":"ABANDONAR";
        return "";
    }
    private void execute(JsonObject o,String a){
        if(o==null||a.isEmpty())return;
        String cmd= switch(tab){
            case 0 -> "ectributo entregar";
            case 12 -> "ecreclamar "+val(o,"id");
            case 22 -> "ecrango "+val(o,"id");
            case 23 -> "ecepico "+a.toLowerCase(Locale.ROOT)+" "+val(o,"id");
            case 19,20 -> "eccontrato "+a.toLowerCase(Locale.ROOT)+" "+val(o,"id");
            default -> "";
        };
        var connection=net.minecraft.client.Minecraft.getInstance().getConnection();
        if(connection!=null&&!cmd.isEmpty())connection.sendCommand(cmd);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){
        if(event.button()!=0&&event.button()!=1)return super.mouseClicked(event,doubleClick);
        if(pageStarts.isEmpty())paginate();
        double mx=event.x(),my=event.y();
        if(inside(mx,my,px(918),py(65),20,18)){onClose();return true;}
        for(int i=0;i<SECTIONS.length;i++)if(inside(mx,my,navX(i),navY(),navW(),navHeight())){switchSection(i);return true;}
        int[] subs=SECTION_TABS[section];
        for(int i=0;i<subs.length;i++)if(inside(mx,my,subX(i,subs.length),subY(i,subs.length),subW(subs.length),18)){switchTab(subs[i]);return true;}
        if(tab==0&&inside(mx,my,leftX(),py(320),20,18)){stage=Math.max(0,stage-1);resetSelection();return true;}
        if(tab==0&&inside(mx,my,leftX()+pageW()-20,py(320),20,18)){stage=Math.min(4,stage+1);resetSelection();return true;}
        if(tab==19&&inside(mx,my,leftX(),py(320),pageW(),18)){offerFilter=(offerFilter+1)%OFFER_FILTERS.length;resetSelection();return true;}
        if(inside(mx,my,leftX(),py(850),22,20)){turn(-1);return true;}
        if(inside(mx,my,leftX()+pageW()-22,py(850),22,20)){turn(1);return true;}
        if(inside(mx,my,leftX(),contentY(),pageW(),contentBottom()-contentY())){
            int idx=pageStarts.get(page)+(int)((my-contentY())/rowHeight(null));
            if(idx<goals().size()&&idx<pageStarts.get(page)+rowsPerPage()){selected=idx;detailPage=0;return true;}
        }
        if(inside(mx,my,rightX(),py(850),pageW()/2-2,20)){execute(chosen(),action(chosen()));return true;}
        if(inside(mx,my,rightX()+pageW()/2,py(850),pageW()/2,20)){
            if(tab==20&&chosen()!=null&&flag(chosen(),"delivery"))execute(chosen(),"ABANDONAR");
            else if(section==4)changeBadge();
            return true;
        }
        return super.mouseClicked(event,doubleClick);
    }
    @Override public boolean mouseScrolled(double mx,double my,double scrollX,double scrollY){
        if(scrollY==0)return false;
        if(inside(mx,my,leftX(),contentY(),pageW(),contentBottom()-contentY())){turn(scrollY<0?1:-1);return true;}
        if(inside(mx,my,rightX(),py(340),pageW(),py(830)-py(340))){detailPage=Math.max(0,detailPage+(scrollY<0?1:-1));return true;}
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
    private void texture(GuiGraphicsExtractor g,String name,int xx,int yy,int ww,int hh){
        if(ww<=0||hh<=0)return;
        var id=net.minecraft.resources.Identifier.fromNamespaceAndPath(EvolutionCraft.ID,"textures/gui/"+name+".png");
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,id,xx,yy,0f,0f,ww,hh,ww,hh);
    }
    private void rounded(GuiGraphicsExtractor g,int xx,int yy,int ww,int hh,int radius,int color){
        texture(g,"button",xx,yy,ww,hh);
    }
    private void roundedBorder(GuiGraphicsExtractor g,int xx,int yy,int ww,int hh,
                               int radius,int edge,int middle){
        texture(g,edge==GOLD || edge==GREEN || edge==0xFFE4A94A?"selected":"button",xx,yy,ww,hh);
    }
    private void outline(GuiGraphicsExtractor g,int xx,int yy,int ww,int hh,int c){
        if(ww>2&&hh>2)g.outline(xx,yy,ww,hh,c);
    }
    private void text(GuiGraphicsExtractor g,String s,int xx,int yy,int c){g.text(font,s,xx,yy,c,false);}
    private void center(GuiGraphicsExtractor g,String s,int xx,int yy,int c){g.text(font,s,xx-font.width(s)/2,yy,c,false);}
    private void item(GuiGraphicsExtractor g,Item i,int xx,int yy){g.item(new ItemStack(i),xx,yy);}

    private String art(JsonObject o){
        String s=val(o,"text").toLowerCase(Locale.ROOT);
        if(secretEntry(o)&&!flag(o,"done"))return "status_locked";
        if(s.contains("elitemobs")){
            if(s.contains("berserker"))return "elite_berserker";
            if(s.contains("campe"))return "elite_champion";
            if(s.contains("infernal"))return "elite_infernal";
            if(s.contains("tosco"))return "elite_tough";
            return "elite_elite";
        }
        if(s.contains("warden"))return "warden";
        if(s.contains("wither"))return "wither";
        if(s.contains("derrotar"))return "category_bosses";
        String material=s.contains("netherita")?"netherite":s.contains("diamante")?"diamond":s.contains("hierro")?"iron":s.contains("cobre")?"copper":s.contains("piedra")?"stone":s.contains("madera")?"wooden":stage==0?"wooden":stage==1?"stone":stage==2?"copper":stage==3?"iron":"diamond";
        if(s.contains("pico"))return material.equals("netherite")?"netherite_sword":material+"_pickaxe";
        if(s.contains("espada"))return material.equals("copper")?"copper_pickaxe":material+"_sword";
        if(s.contains("casco")||s.contains("peto")||s.contains("botas")||s.contains("pantalones")||s.contains("armadura"))return switch(material){case "copper"->"copper_armor";case "netherite"->"netherite_armor";case "iron","diamond"->"iron_armor";default->"leather_armor";};
        if(s.contains("mesa")&&s.contains("trabajo"))return "crafting_table";
        if(s.contains("encant"))return "enchanting_table";
        if(s.contains("herreria"))return "smithing_table";
        if(s.contains("horno"))return "furnace";
        if(s.contains("escudo"))return "shield";
        if(s.contains("arco"))return "bow";
        if(s.contains("cama")||s.contains("noche"))return "bed_food";
        if(s.contains("barca"))return "boat";
        if(s.contains("pesca"))return "category_fishing";
        if(s.contains("agricultura")||s.contains("cosecha"))return "category_farming";
        if(s.contains("ganader"))return "category_husbandry";
        if(s.contains("alquimia")||s.contains("poci"))return "category_alchemy";
        if(s.contains("escombro"))return "ancient_debris";
        if(s.contains("netherita"))return "netherite_ingot";
        if(s.contains("nether"))return "portal_nether";
        if(s.contains("end"))return "portal_end";
        if(s.contains("bioma")||s.contains("explor"))return "category_exploration";
        if(s.contains("diamante"))return "diamond";
        if(s.contains("hierro"))return "iron_ingot";
        if(s.contains("cobre"))return "copper_ingot";
        if(s.contains("madera"))return "wood";
        return TAB_ART[tab];
    }
    private String fit(String s,int w){if(font.width(s)<=w)return s;while(!s.isEmpty()&&font.width(s+"…")>w)s=s.substring(0,s.length()-1);return s+"…";}
    private void bar(GuiGraphicsExtractor g,int xx,int yy,int w,int h,int current,int max){
        texture(g,"progress_track",xx,yy,w,h);
        int fillW=(int)((long)Math.max(0,w-10)*Math.max(0,Math.min(current,max))/Math.max(1,max));
        if(fillW>0)texture(g,"progress_fill",xx+5,yy+3,fillW,Math.max(2,h-6));
    }
    private void control(GuiGraphicsExtractor g,String name,String label,int xx,int yy,int w,int h){
        texture(g,name,xx,yy,w,h);center(g,fit(label,w-8),xx+w/2,yy+(h-8)/2,TEXT);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta){
        if(pageStarts.isEmpty())paginate();
        fill(g,0,0,width,height,0xB9000000);
        texture(g,"book",x(),py(65),panelW(),panelH()*935/1000);
        int logoW=panelW()*29/100;
        texture(g,"logo",x()+(panelW()-logoW)/2,y(),logoW,panelH()*17/100);
        control(g,"action_normal","X",px(918),py(65),20,18);
        for(int i=0;i<SECTIONS.length;i++){
            int nx=navX(i),nw=navW(),ny=navY(),nh=navHeight();
            texture(g,section==i||inside(mouseX,mouseY,nx,ny,nw,nh)?"tab_active":"tab_normal",nx,ny,nw,nh);
            int sz=Math.min(23,nh-11);
            texture(g,SECTION_ART[i],nx+(nw-sz)/2,ny+2,sz,sz);
            center(g,fit(SHORT_SECTIONS[i],nw-4),nx+nw/2,ny+nh-10,TEXT);
        }
        int[] subs=SECTION_TABS[section];
        for(int i=0;i<subs.length;i++)control(g,tab==subs[i]?"tab_active":"tab_normal",TITLES[subs[i]],subX(i,subs.length),subY(i,subs.length),subW(subs.length),18);
        int lx=leftX(),rx=rightX(),pw=pageW();
        String heading=tab==0?Progress.ERA_NAMES[stage+1]:tab==19?"Filtro: "+OFFER_FILTERS[offerFilter]:title();
        if(tab==0){
            texture(g,"arrow_previous",lx,py(320),20,18);
            texture(g,"arrow_next",lx+pw-20,py(320),20,18);
            center(g,fit(heading,pw-45),lx+pw/2,py(320)+5,TEXT);
        }else if(tab==19)control(g,"action_normal",heading,lx,py(320),pw,18);
        else center(g,fit(heading,pw),lx+pw/2,py(320)+5,TEXT);
        JsonArray list=goals();
        int from=pageStarts.get(page),to=Math.min(list.size(),from+rowsPerPage());
        int rh=rowHeight(null);
        for(int i=from;i<to;i++){
            JsonObject o=list.get(i).getAsJsonObject();int ry=contentY()+(i-from)*rh;
            boolean ok=flag(o,"done");
            texture(g,i==selected||inside(mouseX,mouseY,lx,ry,pw,rh-2)?"action_highlight":"action_normal",lx,ry,pw,rh-2);
            int sz=Math.min(25,rh-6);
            texture(g,art(o),lx+5,ry+2,sz,sz);
            String label=secretEntry(o)&&!ok?"??? · Desafío oculto":val(o,"text");
            text(g,fit(label,pw-sz-35),lx+sz+9,ry+(rh-10)/2,TEXT);
            texture(g,ok?"status_complete":"status_pending",lx+pw-22,ry+(rh-18)/2,17,17);
        }
        texture(g,"arrow_previous",lx,py(850),22,20);
        texture(g,"arrow_next",lx+pw-22,py(850),22,20);
        center(g,"Página "+(page+1)+" / "+pageStarts.size(),lx+pw/2,py(850)+6,TEXT);
        center(g,"Progreso general: "+getInt("overallPercent")+"%",rx+pw/2,py(240),TEXT);
        bar(g,rx,py(268),pw,12,getInt("overallPercent"),100);
        JsonObject o=chosen();
        if(o!=null){
            boolean ok=flag(o,"done");
            String label=secretEntry(o)&&!ok?"Desafío oculto":val(o,"text");
            int sz=Math.min(34,panelH()*75/1000);
            texture(g,art(o),rx,py(310),sz,sz);
            int ty=py(310)+3;
            for(String line:wrap(label,Math.max(20,pw-sz-10))){if(ty+9>py(405))break;text(g,line,rx+sz+6,ty,TEXT);ty+=10;}
            texture(g,"separator_gold",rx,py(407),pw,6);
            List<String> lines=new ArrayList<>();
            lines.add("Estado: "+(ok?"Completado":"Pendiente"));
            if(o.has("progress")&&o.has("target"))lines.add("Progreso: "+val(o,"progress")+" / "+val(o,"target"));
            if(secretEntry(o)&&!ok){lines.addAll(wrap("Pista: "+val(o,"hint"),pw-12));}
            else for(String key:new String[]{"detail","description","requires","origin","reward","items","xp","inventory","missing","threshold"}){
                String v=val(o,key);if(!v.isEmpty())lines.addAll(wrap((key.equals("reward")?"Recompensa: ":key.equals("items")?"Objetos: ":key.equals("xp")?"XP: ":key.equals("threshold")?"Puntos: ":key.equals("inventory")?"Disponibles: ":key.equals("missing")?"Faltan: ":"")+v,pw-12));
            }
            int dy=py(440),maxLines=Math.max(1,(py(815)-dy)/11);
            detailPage=Math.min(detailPage,Math.max(0,(lines.size()-1)/maxLines));
            int first=detailPage*maxLines;
            for(int i=first;i<Math.min(lines.size(),first+maxLines);i++){text(g,lines.get(i),rx+4,dy,i==0?(ok?GREEN:RED):TEXT);dy+=11;}
            if(lines.size()>maxLines)center(g,"Detalles "+(detailPage+1)+"/"+((lines.size()+maxLines-1)/maxLines)+" · rueda",rx+pw/2,py(825),MUTED);
            String act=action(o);
            control(g,act.isEmpty()?"action_disabled":"action_highlight",act.isEmpty()?"Sin acción":act,rx,py(850),pw/2-2,20);
            String secondary=tab==20&&flag(o,"delivery")?"ABANDONAR":section==4?"INSIGNIA":"";
            if(!secondary.isEmpty())control(g,"action_normal",secondary,rx+pw/2,py(850),pw/2,20);
        }else center(g,"Sin objetivos",rx+pw/2,py(420),MUTED);
        super.extractRenderState(g,mouseX,mouseY,delta);
    }
    @Override public boolean isPauseScreen(){return false;}
}
