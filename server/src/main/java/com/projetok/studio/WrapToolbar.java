package com.projetok.studio;
import javax.swing.*;
import java.awt.*;

/** Barra que quebra linhas conforme a largura, mantendo ferramentas acessiveis. */
final class WrapToolbar extends JToolBar {
    WrapToolbar(){setLayout(new FlowLayout(FlowLayout.LEFT,4,3));}
    @Override public Dimension getPreferredSize(){int limit=getParent()==null?1200:Math.max(300,getParent().getWidth()-16),x=0,height=6,row=0;for(Component c:getComponents())if(c.isVisible()){Dimension d=c.getPreferredSize();if(x+d.width+4>limit&&x>0){height+=row+6;x=0;row=0;}x+=d.width+4;row=Math.max(row,d.height);}return new Dimension(limit,height+row+6);}
}
