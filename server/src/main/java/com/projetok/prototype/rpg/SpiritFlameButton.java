package com.projetok.prototype.rpg;

import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;

/** Botao de entrada estatico; a arte segue a referencia de chamas azuis. */
final class SpiritFlameButton extends JButton {
    static final String ART="/rpg/login/enter-blue-flames-static.png";
    private static volatile BufferedImage artwork;
    private static Rectangle artworkBounds;
    /** Pre-carrega junto do cenario fora da EDT. Nenhum timer ou simulacao. */
    static void loadArtwork() throws java.io.IOException {
        if(artwork!=null)return;
        try(var in=SpiritFlameButton.class.getResourceAsStream(ART)){
            if(in==null)throw new java.io.IOException("Arte do botao de entrada ausente: "+ART);
            BufferedImage image=ImageIO.read(in);if(image==null)throw new java.io.IOException("Arte do botao invalida");
            int first=image.getHeight(),last=-1;
            // Enquadra a moldura luminosa, evitando margens vazias do arquivo gerado.
            for(int y=0;y<image.getHeight();y++){
                int bright=0;for(int x=0;x<image.getWidth();x++){int pixel=image.getRGB(x,y);if((pixel>>>24)>180&&((pixel>>>8)&255)>150&&(pixel&255)>200)bright++;}
                if(bright>image.getWidth()*.6){first=Math.min(first,y);last=y;}
            }
            int top=last>=first?Math.max(0,first-14):0,bottom=last>=first?Math.min(image.getHeight(),last+15):image.getHeight();
            artworkBounds=new Rectangle(0,top,image.getWidth(),bottom-top);artwork=image;
        }
    }
    SpiritFlameButton(){
        super("Entrar");setContentAreaFilled(false);setOpaque(false);setBorderPainted(false);setFocusPainted(false);setRolloverEnabled(false);
        setForeground(new Color(237,249,255));setFont(new Font("Serif",Font.BOLD,22));setPreferredSize(new Dimension(340,62));
        setBorder(BorderFactory.createEmptyBorder(10,55,10,55));setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));setToolTipText("Entrar no Spirit Gate");
    }
    @Override protected void paintComponent(Graphics original){
        Graphics2D g=(Graphics2D)original.create();try{
            int w=getWidth(),h=getHeight();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            if(!isEnabled())g.setComposite(AlphaComposite.SrcOver.derive(.4f));
            if(artwork!=null){var crop=artworkBounds;g.drawImage(artwork,0,0,w,h,crop.x,crop.y,crop.x+crop.width,crop.y+crop.height,null);}
            else{g.setColor(new Color(2,15,26));g.fillRect(3,3,w-6,h-6);g.setColor(new Color(72,212,248));g.drawRect(3,3,w-7,h-7);}
            if(getModel().isPressed()){g.setColor(new Color(0,0,0,55));g.fillRect(3,3,w-6,h-6);}
            String text="E N T R A R";FontMetrics metrics=g.getFontMetrics();int x=(w-metrics.stringWidth(text))/2,y=(h-metrics.getHeight())/2+metrics.getAscent();
            g.setColor(new Color(0,5,12,210));g.drawString(text,x+1,y+1);g.setColor(getForeground());g.drawString(text,x,y);
            if(hasFocus()){g.setColor(new Color(184,241,255));g.setStroke(new BasicStroke(1,0,0,1,new float[]{2,3},0));g.drawRect(7,7,w-15,h-15);}
        }finally{g.dispose();}
    }
}
