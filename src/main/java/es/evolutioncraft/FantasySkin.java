package es.evolutioncraft;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Fantasy parchment skin, drawn using the stable 26.3 GUI primitives.
 * Decorative art only: neither events nor interaction boxes are changed.
 * Pixel aligned, scalable and fully offline (no shader/texture dependency).
 */
public final class FantasySkin {
    private FantasySkin(){}
    public static final int INK=0xFF50301E;
    public static final int MUTED=0xFF865A3C;
    public static final int GOLD=0xFFFFC35B;
    public static final int PARCHMENT=0xFFF4E2C0;
    public static final int LIGHT=0xFFFFF0D2;
    public static final int FRAME=0xFF984C25;
    public static final int DARK=0xFF582D22;
    public static final int GREEN=0xFF4E873E;
    public static final int PURPLE=0xFFB88AF6;
    public static final int CARD=0xFFFFEDD0;
    private static void f(GuiGraphicsExtractor g,int x,int y,int w,int h,int c){
        if(w>0&&h>0)g.fill(x,y,x+w,y+h,c);
    }
    private static void edge(GuiGraphicsExtractor g,int x,int y,int w,int h,int c){
        if(w>1&&h>1)g.outline(x,y,w,h,c);
    }
    public static void diamond(GuiGraphicsExtractor g,int cx,int cy,int rad,int color){
        for(int yy=-rad;yy<=rad;yy++){
            int half=rad-Math.abs(yy);
            f(g,cx-half,cy+yy,2*half+1,1,0xFF6A3656);
            if(half>0)f(g,cx-half+1,cy+yy,2*half-1,1,color);
        }
        f(g,cx,cy-rad+1,1,rad,0xFFF8EAFF);
        f(g,cx-1,cy-1,1,1,0xFFFFFFFF);
    }
    private static void leaf(GuiGraphicsExtractor g,int x,int y,boolean flip){
        int step=flip?-1:1;
        f(g,x,y,2,9,0xFF527B28);
        f(g,x+step*2,y+1,3,2,0xFF43873B);
        f(g,x+step*3,y+2,3,2,0xFF89BA4E);
        f(g,x-step*2,y+5,3,2,0xFF3B7133);
        f(g,x-step*3,y+6,2,1,0xFFA4C565);
    }
    /** Nine-slice-like solid panel with ivory inset and dark timber border. */
    public static void panel(GuiGraphicsExtractor g,int x,int y,int w,int h){
        if(w<22||h<24)return;
        f(g,x+3,y+5,w-3,h-3,0x720E0805); // shadow
        f(g,x,y+4,w,h-8,DARK);
        f(g,x+4,y,w-8,h,0xFFB46B31);
        edge(g,x+1,y+2,w-2,h-4,0xFF633222);
        f(g,x+5,y+6,w-10,h-12,0xFFCC9B63);
        f(g,x+7,y+7,w-14,h-14,PARCHMENT);
        // Subtle aged-paper bands, few draw calls.
        for(int yy=y+10;yy<y+h-11;yy+=13){
            f(g,x+10,yy,w-20,1,0x16FFFFFF);
            f(g,x+12,yy+4,w-24,1,0x0F98683C);
        }
        f(g,x+6,y+6,w-12,2,LIGHT);
        f(g,x+7,y+h-9,w-14,2,0xFFB88857);
        f(g,x+2,y+8,3,h-17,0xFFE69E45);
        f(g,x+w-5,y+8,3,h-17,0xFFD27D36);
        // Golden corner joints
        for(int dx:new int[]{6,w-12}){
            f(g,x+dx,y+5,6,3,GOLD);
            f(g,x+dx,y+h-8,6,3,0xFFE9A23D);
        }
    }
    /** Crimson/orange metallic title banner; used in main window and HUD. */
    public static void ribbon(GuiGraphicsExtractor g,int x,int y,int w,int h){
        if(w<24||h<10)return;
        f(g,x+2,y+2,w-4,h-2,0xFF6E281D);
        f(g,x,y+2,w,h-5,0xFFA44320);
        f(g,x+4,y+1,w-8,h-4,0xFFF4A433);
        f(g,x+6,y+3,w-12,h-8,0xFFFFD16A);
        f(g,x+7,y+4,w-14,2,0xFFFFE4A2);
        f(g,x+7,y+h-7,w-14,2,0xFFE89128);
        edge(g,x+3,y+1,w-6,h-3,0xFFFFE1A1);
        diamond(g,x+11,y+h/2,2,0xFFFFF2B3);
        diamond(g,x+w-12,y+h/2,2,0xFFFFF2B3);
    }
    /** Parchment inset, for labels and list entries. */
    public static void card(GuiGraphicsExtractor g,int x,int y,int w,int h,boolean selected){
        if(w<10||h<7)return;
        f(g,x+2,y+3,w-2,h-2,0x40944A28);
        f(g,x,y+1,w,h-3,selected?0xFFB96936:0xFFBF9270);
        f(g,x+2,y+2,w-4,h-5,selected?0xFFFFD07B:LIGHT);
        f(g,x+4,y+3,w-8,2,selected?0xFFFFE39A:0xFFFFFFEA);
        f(g,x+4,y+h-5,w-8,1,selected?0xFFE9A744:0xFFD6B48C);
        f(g,x+2,y+5,1,h-10,0xFFFFF6E0);
        f(g,x+w-3,y+5,1,h-10,0xFFAE7456);
    }
    public static void nav(GuiGraphicsExtractor g,int x,int y,int w,int h,boolean selected){
        if(w<12||h<8)return;
        f(g,x+1,y+3,w-1,h-3,0x8872472D);
        f(g,x,y+2,w,h-5,selected?0xFFAE5B28:0xFFB8845A);
        f(g,x+2,y+1,w-4,h-3,selected?0xFFFFBA49:0xFFF5DDB8);
        f(g,x+4,y+3,w-8,h-7,selected?0xFFFFD774:0xFFFFEACB);
        f(g,x+6,y+4,w-12,1,0x99FFFFFF);
        diamond(g,x+7,y+h/2,2,selected?0xFFE9A434:0xFFBE8055);
    }
    public static void progress(GuiGraphicsExtractor g,int x,int y,int w,int h,
                                int current,int target,int color){
        if(w<5||h<3)return;
        f(g,x,y,w,h,0xFF80563E);
        f(g,x+1,y+1,w-2,h-2,0xFFCEB596);
        int px=(int)((long)(w-4)*Math.max(0,Math.min(current,target))/Math.max(1,target));
        if(px>0){
            f(g,x+2,y+2,px,Math.max(1,h-4),color);
            f(g,x+2,y+2,px,1,0x88FFFFFF);
        }
        f(g,x+1,y+h-2,w-2,1,0xFF9F7453);
    }
    /** Decorative ivy/crystals only at borders, never over text. */
    public static void decorations(GuiGraphicsExtractor g,int x,int y,int w,int h){
        if(w<50||h<40)return;
        leaf(g,x+6,y+17,false);
        leaf(g,x+w-7,y+19,true);
        leaf(g,x+7,y+h-22,false);
        leaf(g,x+w-9,y+h-22,true);
        diamond(g,x+4,y+29,3,PURPLE);
        diamond(g,x+w-5,y+31,3,PURPLE);
        diamond(g,x+w/2,y+h-6,3,GOLD);
    }
}
