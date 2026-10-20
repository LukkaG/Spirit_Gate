package com.projetok.studio;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.function.BiConsumer;

/** Recorte visual do tileset; somente metadados sao alterados. */
final class TilePicker {
    static void open(JFrame owner,AssetCatalog.Asset asset,BufferedImage image,BiConsumer<WorldDocument.Crop,java.util.List<WorldDocument.Crop>> selected) {
        if(image==null){JOptionPane.showMessageDialog(owner,"Aguarde o carregamento da imagem.");return;}
        JDialog dialog=new JDialog(owner,"Tileset: clique em um recorte",true);JPanel options=new JPanel(new GridLayout(2,8));
        String[] labels={"Tile W","Tile H","Margem","Espaco","Regiao X","Regiao Y","Regiao W","Regiao H"};int[] defaults={32,32,0,0,0,0,asset.width(),asset.height()};JSpinner[] values=new JSpinner[8];
        for(String label:labels)options.add(new JLabel(label));for(int i=0;i<8;i++){values[i]=new JSpinner(new SpinnerNumberModel(defaults[i],0,8192,1));options.add(values[i]);}
        final java.util.List<WorldDocument.Crop>[] crops=new java.util.List[]{java.util.List.of()};double scale=Math.min(1,Math.min(850d/image.getWidth(),600d/image.getHeight()));
        JPanel preview=new JPanel(){protected void paintComponent(Graphics original){super.paintComponent(original);Graphics2D g=(Graphics2D)original.create();try{g.scale(scale,scale);g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);g.drawImage(image,0,0,null);g.setColor(new Color(255,220,80));for(var c:crops[0])g.drawRect(c.x(),c.y(),c.width(),c.height());}finally{g.dispose();}}};preview.setPreferredSize(new Dimension((int)(image.getWidth()*scale),(int)(image.getHeight()*scale)));
        JButton update=new JButton("Atualizar grade");update.addActionListener(e->{try{int[] n=Arrays.stream(values).mapToInt(v->(Integer)v.getValue()).toArray();crops[0]=AssetCatalog.tiles(asset,n[0],n[1],n[2],n[3],new WorldDocument.Crop(n[4],n[5],n[6],n[7]));preview.repaint();}catch(Exception error){JOptionPane.showMessageDialog(dialog,error.getMessage());}});
        preview.addMouseListener(new MouseAdapter(){public void mousePressed(MouseEvent e){int x=(int)(e.getX()/scale),y=(int)(e.getY()/scale);crops[0].stream().filter(c->new Rectangle(c.x(),c.y(),c.width(),c.height()).contains(x,y)).findFirst().ifPresent(c->{selected.accept(c,crops[0]);dialog.dispose();});}});
        dialog.add(options,BorderLayout.NORTH);dialog.add(new JScrollPane(preview));dialog.add(update,BorderLayout.SOUTH);dialog.pack();dialog.setLocationRelativeTo(owner);update.doClick();dialog.setVisible(true);
    }
}
