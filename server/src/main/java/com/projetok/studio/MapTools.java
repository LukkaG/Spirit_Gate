package com.projetok.studio;
import javax.swing.*;
import java.awt.*;

/** Comandos de selecao global e dimensoes do mapa. */
final class MapTools {
    MapTools(WorldStudio ui){
        JButton select=new JButton("Selecionar toda a grade");select.setToolTipText("Seleciona todos os elementos e colisoes da camada ativa, em todo o mapa (Ctrl+A).");
        Runnable selectAll=()->{ui.canvas.selected.clear();ui.document.elements().stream().filter(e->e.layer().equals(ui.canvas.activeLayer)).forEach(e->ui.canvas.selected.add(e.id()));ui.document.hitboxes().stream().filter(h->h.layer().equals(ui.canvas.activeLayer)).forEach(h->ui.canvas.selected.add(h.id()));ui.inspectSelection();ui.canvas.repaint();ui.status.setText(ui.canvas.selected.size()+" elementos selecionados na camada ativa.");};
        select.addActionListener(e->selectAll.run());ui.toolbar.add(select);
        ui.canvas.getInputMap().put(KeyStroke.getKeyStroke("control A"),"selectAll");ui.canvas.getActionMap().put("selectAll",new AbstractAction(){public void actionPerformed(java.awt.event.ActionEvent e){selectAll.run();}});
        JButton resize=new JButton("Aumentar mapa");resize.setToolTipText("Aumenta largura e altura em pixels; preserva os elementos existentes e permite Desfazer.");
        resize.addActionListener(e->{try{
            var d=ui.document;JSpinner w=new JSpinner(new SpinnerNumberModel(d.width(),d.width(),8192,d.grid())),h=new JSpinner(new SpinnerNumberModel(d.height(),d.height(),8192,d.grid()));
            JPanel form=new JPanel(new GridLayout(0,2,6,6));form.add(new JLabel("Largura (pixels)"));form.add(w);form.add(new JLabel("Altura (pixels)"));form.add(h);
            if(JOptionPane.showConfirmDialog(ui,form,"Aumentar area do mapa (maximo 8192 por eixo)",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
            w.commitEdit();h.commitEdit();ui.change(d.resized((Integer)w.getValue(),(Integer)h.getValue()));ui.status.setText("Mapa: "+ui.document.width()+" x "+ui.document.height()+" pixels");
        }catch(Exception error){ui.error(error);}});ui.toolbar.add(resize);
    }
}
