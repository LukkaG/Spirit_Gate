package com.projetok.studio;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.function.BiConsumer;

/** Imagem completa com selecao direta dos pinceis esquerdo e direito. */
final class TilePalette extends JPanel {
    final WorldStudio ui;
    AssetCatalog.Asset asset;
    final JLabel preview=new JLabel("Selecione uma imagem na biblioteca",SwingConstants.CENTER);
    final JSpinner tileWidth=new JSpinner(new SpinnerNumberModel(32,1,8192,1));
    final JSpinner tileHeight=new JSpinner(new SpinnerNumberModel(32,1,8192,1));
    final DefaultListModel<WorldDocument.Crop> model=new DefaultListModel<>();
    BiConsumer<WorldDocument.Crop,Boolean> choose=(c,r)->{};
    WorldDocument.Crop selected;
    final JPanel sheet=new JPanel(){@Override protected void paintComponent(Graphics original){
        super.paintComponent(original);if(asset==null)return;
        var image=TilePalette.this.ui.images.peek(asset.id());Graphics2D g=(Graphics2D)original.create();try{
            for(int y=0;y<asset.height();y+=16)for(int x=0;x<asset.width();x+=16){g.setColor((x/16+y/16)%2==0?Color.LIGHT_GRAY:Color.WHITE);g.fillRect(x,y,16,16);}
            if(image!=null)g.drawImage(image,0,0,null);
            g.setColor(new Color(255,220,80,150));int w=(Integer)tileWidth.getValue(),h=(Integer)tileHeight.getValue();
            for(int x=0;x<=asset.width();x+=w)g.drawLine(x,0,x,asset.height());for(int y=0;y<=asset.height();y+=h)g.drawLine(0,y,asset.width(),y);
            if(selected!=null){g.setColor(Color.CYAN);g.setStroke(new BasicStroke(2));g.drawRect(selected.x(),selected.y(),selected.width()-1,selected.height()-1);}
        }finally{g.dispose();}
    }};
    TilePalette(WorldStudio ui){
        super(new BorderLayout(4,4));this.ui=ui;setBorder(BorderFactory.createTitledBorder("Tileset completo"));
        JPanel header=new JPanel(new BorderLayout());preview.setPreferredSize(new Dimension(230,80));header.add(preview);
        JPanel sizes=new JPanel(new GridLayout(2,2));sizes.add(new JLabel("Largura do tile"));sizes.add(tileWidth);sizes.add(new JLabel("Altura do tile"));sizes.add(tileHeight);header.add(sizes,BorderLayout.SOUTH);add(header,BorderLayout.NORTH);
        tileWidth.addChangeListener(e->rebuild());tileHeight.addChangeListener(e->rebuild());
        sheet.setFocusable(true);
        var selectionMouse=new MouseAdapter(){Point anchor;boolean right;
            @Override public void mousePressed(MouseEvent e){
                if(asset==null||(!SwingUtilities.isLeftMouseButton(e)&&!SwingUtilities.isRightMouseButton(e)))return;
                if(e.getX()<0||e.getY()<0||e.getX()>=asset.width()||e.getY()>=asset.height())return;
                sheet.requestFocusInWindow();anchor=e.getPoint();right=SwingUtilities.isRightMouseButton(e);update(e.getPoint());
            }
            void update(Point point){
                if(anchor==null||asset==null)return;
                int w=(Integer)tileWidth.getValue(),h=(Integer)tileHeight.getValue();
                int px=Math.max(0,Math.min(asset.width()-1,point.x)),py=Math.max(0,Math.min(asset.height()-1,point.y));
                int x=Math.min(anchor.x,px)/w*w,y=Math.min(anchor.y,py)/h*h;
                int endX=Math.min(asset.width(),(Math.max(anchor.x,px)/w+1)*w),endY=Math.min(asset.height(),(Math.max(anchor.y,py)/h+1)*h);
                choose.accept(new WorldDocument.Crop(x,y,endX-x,endY-y),right);
            }
            @Override public void mouseDragged(MouseEvent e){update(e.getPoint());sheet.scrollRectToVisible(new Rectangle(e.getX(),e.getY(),1,1));}
            @Override public void mouseReleased(MouseEvent e){update(e.getPoint());anchor=null;}
        };sheet.addMouseListener(selectionMouse);sheet.addMouseMotionListener(selectionMouse);
        JPanel content=new JPanel(new BorderLayout());content.add(sheet,BorderLayout.NORTH);
        JScrollPane scroll=new JScrollPane(content);scroll.setPreferredSize(new Dimension(240,230));add(scroll);
        JPanel footer=new JPanel(new GridLayout(0,1));footer.add(new JLabel("Arraste para selecionar varios tiles"));
        JButton allLeft=new JButton("Imagem inteira: esquerdo"),allRight=new JButton("Imagem inteira: direito");
        allLeft.addActionListener(e->whole(false));allRight.addActionListener(e->whole(true));footer.add(allLeft);footer.add(allRight);add(footer,BorderLayout.SOUTH);
    }
    void whole(boolean right){if(asset!=null)choose.accept(new WorldDocument.Crop(0,0,asset.width(),asset.height()),right);}
    void showAsset(AssetCatalog.Asset asset){this.asset=asset;selected=null;preview.setIcon(null);preview.setText(asset.file().getFileName().toString());rebuild();}
    void rebuild(){
        model.clear();if(asset==null)return;int w=(Integer)tileWidth.getValue(),h=(Integer)tileHeight.getValue();
        // Inclui as bordas parciais para que toda a imagem continue acessivel.
        for(int y=0;y<asset.height()&&model.size()<65536;y+=h)for(int x=0;x<asset.width()&&model.size()<65536;x+=w){model.addElement(new WorldDocument.Crop(x,y,Math.min(w,asset.width()-x),Math.min(h,asset.height()-y)));}
        ui.tilesetCrops=java.util.Collections.list(model.elements());sheet.setPreferredSize(new Dimension(asset.width(),asset.height()));sheet.revalidate();repaint();
    }
    void select(WorldDocument.Crop crop){
        selected=crop;preview.setText(crop.width()+" x "+crop.height());BufferedImage image=asset==null?null:ui.images.peek(asset.id());
        if(image!=null){BufferedImage thumb=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);Graphics2D g=thumb.createGraphics();try{double scale=Math.min(64d/crop.width(),64d/crop.height());int w=Math.max(1,(int)(crop.width()*scale)),h=Math.max(1,(int)(crop.height()*scale));g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);g.drawImage(image,0,0,w,h,crop.x(),crop.y(),crop.x()+crop.width(),crop.y()+crop.height(),null);}finally{g.dispose();}preview.setIcon(new ImageIcon(thumb));}sheet.repaint();
    }
}
