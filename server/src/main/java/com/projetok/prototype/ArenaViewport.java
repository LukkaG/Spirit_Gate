package com.projetok.prototype;

import java.awt.geom.Point2D;

/** Transformacao pura; sem relogio, entidades ou estado de camera acumulado. */
public record ArenaViewport(double originX, double originY, double zoom,
                            double scale, double offsetX, double offsetY,
                            double arenaWidth,double arenaHeight) {
    /** Preserva o canvas historico e os consumidores do construtor anterior. */
    public ArenaViewport(double originX,double originY,int zoom,double scale,double offsetX,double offsetY) {
        this(originX,originY,zoom,scale,offsetX,offsetY,DebugWorld.WIDTH,DebugWorld.HEIGHT);
        if(zoom<1||zoom>4)throw new IllegalArgumentException("Zoom historico invalido");
    }
    public ArenaViewport {
        if(!Double.isFinite(originX+originY+scale+offsetX+offsetY+arenaWidth+arenaHeight)||!Double.isFinite(zoom)
                ||scale<=0||zoom<.25||zoom>4
                ||arenaWidth<=0||arenaHeight<=0)
            throw new IllegalArgumentException("Viewport invalida");
    }
    /** Pixels logicos inteiros: janelas menores recortam a visao sem reduzir a arte. */
    public static ArenaViewport pixelPerfect(int screenWidth,int screenHeight,int worldWidth,int worldHeight,
                                             double targetX,double targetY,int zoom) {
        if(zoom<1||zoom>4)throw new IllegalArgumentException("Zoom inteiro invalido");
        return pixelPerfect(screenWidth,screenHeight,worldWidth,worldHeight,targetX,targetY,(double)zoom);
    }
    /** Modo comparativo: zoom fracionario mantem nearest-neighbor, mas os pixels nao sao uniformes. */
    public static ArenaViewport pixelPerfect(int screenWidth,int screenHeight,int worldWidth,int worldHeight,
                                             double targetX,double targetY,double zoom) {
        if(!Double.isFinite(zoom)||zoom<.25||zoom>4)throw new IllegalArgumentException("Zoom de teste: 0.25..4");
        var layout=fit(screenWidth,screenHeight,worldWidth,worldHeight,targetX,targetY,1);
        double layoutWidth=Math.max(1,Math.floor(DebugWorld.WIDTH*layout.scale()));
        double layoutHeight=Math.max(1,Math.floor(DebugWorld.HEIGHT*layout.scale()));
        // Em tela maior que o mundo, letterbox evita mirar alem dos seus limites.
        double width=Math.floor(Math.min(layoutWidth,worldWidth*zoom)),height=Math.floor(Math.min(layoutHeight,worldHeight*zoom));
        double visibleW=width/zoom,visibleH=height/zoom;
        double x=Math.floor(Math.max(0,Math.min(worldWidth-visibleW,targetX-visibleW/2)));
        double y=Math.floor(Math.max(0,Math.min(worldHeight-visibleH,targetY-visibleH/2)));
        return new ArenaViewport(x,y,zoom,1,Math.floor(layout.offsetX()+(layoutWidth-width)/2),
                Math.floor(layout.offsetY()+(layoutHeight-height)/2),width,height);
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
    /** HUD sobreposta: usa toda a superfície disponível, sem reservar a lateral histórica. */
    public static ArenaViewport immersive(int screenWidth,int screenHeight,int worldWidth,int worldHeight,
                                          double targetX,double targetY,double zoom) {
        if(screenWidth<1||screenHeight<1||worldWidth<1||worldHeight<1||!Double.isFinite(targetX+targetY+zoom)||zoom<.25||zoom>4)
            throw new IllegalArgumentException("Limites da câmera inválidos");
        double width=Math.min(screenWidth,Math.floor(worldWidth*zoom)),height=Math.min(screenHeight,Math.floor(worldHeight*zoom));
        double x=Math.floor(Math.max(0,Math.min(worldWidth-width/zoom,targetX-width/zoom/2)));
        double y=Math.floor(Math.max(0,Math.min(worldHeight-height/zoom,targetY-height/zoom/2)));
        return new ArenaViewport(x,y,zoom,1,Math.floor((screenWidth-width)/2),Math.floor((screenHeight-height)/2),width,height);
    }
    public Point2D.Double screenToWorld(double x,double y) {
        return new Point2D.Double((x-offsetX)/(zoom*scale)+originX,(y-offsetY)/(zoom*scale)+originY);
    }
    public boolean containsScreen(double x,double y) {
        return x>=offsetX && y>=offsetY && x<offsetX+arenaWidth*scale && y<offsetY+arenaHeight*scale;
    }
    /** Retângulo visível no mundo; HUD e letterbox não fazem parte da arena. */
    public java.awt.geom.Rectangle2D.Double visibleWorld() {
        return new java.awt.geom.Rectangle2D.Double(originX,originY,
                arenaWidth/zoom,arenaHeight/zoom);
    }
}
