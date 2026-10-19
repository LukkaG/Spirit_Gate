package com.projetok.prototype;

import java.awt.geom.Point2D;

/** Transformacao pura; sem relogio, entidades ou estado de camera acumulado. */
public record ArenaViewport(double originX, double originY, int zoom,
                            double scale, double offsetX, double offsetY) {
    public ArenaViewport {
        if(!Double.isFinite(originX+originY+scale+offsetX+offsetY) || scale<=0 || zoom<1 || zoom>4)
            throw new IllegalArgumentException("Viewport invalida");
    }
    /** Canvas completo preserva HUD; camera recorta apenas a arena logica. */
    public static ArenaViewport fit(int screenWidth,int screenHeight,int worldWidth,int worldHeight,
                                    double targetX,double targetY,int zoom) {
        if(screenWidth<=0 || screenHeight<=0 || worldWidth<DebugWorld.WIDTH || worldHeight<DebugWorld.HEIGHT ||
                !Double.isFinite(targetX+targetY) || zoom<1 || zoom>4) throw new IllegalArgumentException("Limites invalidos");
        double fit=Math.min((double)screenWidth/DebugPanel.PANEL_WIDTH,(double)screenHeight/DebugPanel.PANEL_HEIGHT);
        double scale=fit>=1?Math.floor(fit):fit;
        double visibleW=(double)DebugWorld.WIDTH/zoom,visibleH=(double)DebugWorld.HEIGHT/zoom;
        double x=Math.floor(Math.max(0,Math.min(worldWidth-visibleW,targetX-visibleW/2)));
        double y=Math.floor(Math.max(0,Math.min(worldHeight-visibleH,targetY-visibleH/2)));
        return new ArenaViewport(x,y,zoom,scale,(screenWidth-DebugPanel.PANEL_WIDTH*scale)/2,
                (screenHeight-DebugPanel.PANEL_HEIGHT*scale)/2);
    }
    public Point2D.Double worldToScreen(double x,double y) {
        return new Point2D.Double(offsetX+(x-originX)*zoom*scale,offsetY+(y-originY)*zoom*scale);
    }
    public Point2D.Double screenToWorld(double x,double y) {
        return new Point2D.Double((x-offsetX)/(zoom*scale)+originX,(y-offsetY)/(zoom*scale)+originY);
    }
    public boolean containsScreen(double x,double y) {
        return x>=offsetX && y>=offsetY && x<offsetX+DebugWorld.WIDTH*scale && y<offsetY+DebugWorld.HEIGHT*scale;
    }
    /** Retângulo visível no mundo; HUD e letterbox não fazem parte da arena. */
    public java.awt.geom.Rectangle2D.Double visibleWorld() {
        return new java.awt.geom.Rectangle2D.Double(originX,originY,
                (double)DebugWorld.WIDTH/zoom,(double)DebugWorld.HEIGHT/zoom);
    }
}
