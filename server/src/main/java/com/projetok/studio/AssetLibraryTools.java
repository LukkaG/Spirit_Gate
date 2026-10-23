package com.projetok.studio;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.swing.*;

/** Organização visual com preferências assíncronas e preview sem I/O na pintura. */
final class AssetLibraryTools {
    final WorldStudio ui;
    AssetLibraryPreferences preferences=AssetLibraryPreferences.empty();
    final JComboBox<String> view=new JComboBox<>(new String[]{"Todos os recursos","Favoritos","Recentes"});
    final JSlider size=new JSlider(32,96,64);
    final JButton favorite=new JButton("Favoritar selecionado"),preview=new JButton("Ampliar preview");
    boolean loading=true,saving,writable=true;AssetLibraryPreferences pending;
    AssetLibraryTools(WorldStudio ui,JPanel filters) {
        this.ui=ui;filters.add(view);size.setToolTipText("Tamanho da miniatura (32 a 96 px)");filters.add(size);filters.add(favorite);filters.add(preview);
        view.addActionListener(e->ui.filter());
        size.addChangeListener(e->{ui.library.setFixedCellHeight(size.getValue()+10);ui.library.repaint();if(!loading&&!size.getValueIsAdjusting()){preferences=preferences.size(size.getValue());persist();}});
        favorite.setEnabled(false);
        favorite.addActionListener(e->{var a=ui.library.getSelectedValue();if(a!=null){preferences=preferences.toggle(a.id());persist();ui.filter();}});
        preview.addActionListener(e->preview());
        ui.library.addListSelectionListener(e->{if(!loading&&!e.getValueIsAdjusting()){var a=ui.library.getSelectedValue();if(a!=null){preferences=preferences.used(a.id());persist();}}});
        new SwingWorker<AssetLibraryPreferences,Void>() {
            protected AssetLibraryPreferences doInBackground()throws Exception{return AssetLibraryPreferences.load(ui.project);}
            protected void done(){try{preferences=get();size.setValue(preferences.thumbnailSize());}catch(Exception e){writable=false;ui.error(e);}finally{loading=false;favorite.setEnabled(writable);ui.filter();}}
        }.execute();
    }
    java.util.List<AssetCatalog.Asset> filter(java.util.List<AssetCatalog.Asset> source) {
        return switch(view.getSelectedIndex()) {
            case 1->source.stream().filter(a->preferences.favorites().contains(a.id())).toList();
            case 2->preferences.recent().stream().flatMap(id->source.stream().filter(a->a.id().equals(id))).toList();
            default->source;
        };
    }
    Icon icon(AssetCatalog.Asset asset){int pixels=size.getValue();return new Icon(){public int getIconWidth(){return pixels;}public int getIconHeight(){return pixels;}public void paintIcon(Component c,Graphics original,int x,int y){var g=(Graphics2D)original.create();try{g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);g.drawImage(asset.thumbnail(),x,y,pixels,pixels,null);}finally{g.dispose();}}};}
    void persist(){if(!writable)return;pending=preferences;if(saving)return;var snapshot=pending;pending=null;saving=true;
        new SwingWorker<Void,Void>() {
            protected Void doInBackground()throws Exception{snapshot.save(ui.project);return null;}
            protected void done(){saving=false;try{get();}catch(Exception e){ui.error(e);}if(pending!=null)persist();}
        }.execute();
    }
    void preview(){var asset=ui.library.getSelectedValue();if(asset==null){ui.status.setText("Selecione um recurso para ampliar.");return;}preview.setEnabled(false);
        new SwingWorker<BufferedImage,Void>() {
            protected BufferedImage doInBackground()throws Exception{return ui.images.load(asset);}
            protected void done(){try{var image=get();JPanel panel=new JPanel(){protected void paintComponent(Graphics original){super.paintComponent(original);var g=(Graphics2D)original.create();try{for(int y=0;y<getHeight();y+=16)for(int x=0;x<getWidth();x+=16){g.setColor((x/16+y/16)%2==0?Color.GRAY:Color.DARK_GRAY);g.fillRect(x,y,16,16);}double scale=Math.min((double)getWidth()/image.getWidth(),(double)getHeight()/image.getHeight());g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);g.drawImage(image,0,0,(int)(image.getWidth()*scale),(int)(image.getHeight()*scale),null);}finally{g.dispose();}}};panel.setPreferredSize(new Dimension(512,512));JOptionPane.showMessageDialog(ui,panel,asset.path()+" | "+asset.width()+" x "+asset.height(),JOptionPane.PLAIN_MESSAGE);}catch(Exception e){ui.error(e);}finally{preview.setEnabled(true);}}
        }.execute();
    }
}
