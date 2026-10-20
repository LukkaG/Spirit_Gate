package com.projetok.studio;
import java.awt.geom.Point2D;
/** Transformacao unica utilizada para renderizar e editar sob zoom/pan. */
public record StudioViewport(double zoom,double panX,double panY) {
    public StudioViewport {if(!Double.isFinite(zoom+panX+panY)||zoom<.1||zoom>8)throw new IllegalArgumentException("Viewport invalida");}
    /** Coordenada de mundo continua, sem arredondar antes de aplicar snap. */
    public Point2D.Double world(double x,double y){return new Point2D.Double((x-panX)/zoom,(y-panY)/zoom);}
    /** Coordenada de tela para diagnostico e gestos. */
    public Point2D.Double screen(double x,double y){return new Point2D.Double(x*zoom+panX,y*zoom+panY);}
    /** Zoom ancorado no cursor evita salto do ponto sob o mouse. */
    public StudioViewport zoomAt(double value,double x,double y){var p=world(x,y);double next=Math.max(.1,Math.min(8,value));return new StudioViewport(next,x-p.x*next,y-p.y*next);}
}
