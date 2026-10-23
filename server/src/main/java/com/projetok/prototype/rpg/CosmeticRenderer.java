package com.projetok.prototype.rpg;

import com.projetok.prototype.DirectionalAnimation.Direction;
import java.awt.Graphics2D;

/** Composição residente por planos, sem criar cópia do corpo para cada combinação. */
public final class CosmeticRenderer {
    private CosmeticRenderer() { }
    public static boolean draw(Graphics2D g,RpgPlayerSprites base,CosmeticCatalog catalog,CosmeticCatalog.Selection selection,Direction direction,int frame,boolean walking,float x,float y){
        if(base==null)return false;String pose=direction+"."+(walking?frame+1:0);double scale=base.scale();
        if(catalog!=null)catalog.draw(g,selection.hair(),pose,"hair_back",x,y,scale);
        boolean drawn=base.drawFrame(g,direction,frame,walking,x,y);
        if(catalog!=null){catalog.draw(g,selection.clothing(),pose,"clothing",x,y,scale);catalog.draw(g,selection.hair(),pose,"hair_front",x,y,scale);}return drawn;
    }
}
