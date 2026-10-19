package com.projetok.prototype.rpg;
import com.projetok.rpg.*;
import javax.swing.*;
import java.awt.*;
/** Diario e representacao da regiao atual; nao revela mapas futuros. */
final class ExplorationWindow extends JDialog {
    private final JTextArea journal=new JTextArea(),map=new JTextArea();
    private final JTabbedPane tabs=new JTabbedPane();
    private final RpgRuntime runtime;
    ExplorationWindow(JFrame owner,RpgRuntime runtime){
        super(owner,"Diario e exploracao",false);this.runtime=runtime;setDefaultCloseOperation(HIDE_ON_CLOSE);
        journal.setEditable(false);journal.setLineWrap(true);journal.setWrapStyleWord(true);map.setEditable(false);map.setFont(new Font(Font.MONOSPACED,Font.PLAIN,14));
        tabs.addTab("Diario",new JScrollPane(journal));tabs.addTab("Mapa atual",new JScrollPane(map));setContentPane(tabs);setSize(800,600);setLocationRelativeTo(owner);
        Timer timer=new Timer(200,e->{if(isVisible())refresh();});timer.start();owner.addWindowListener(new java.awt.event.WindowAdapter(){public void windowClosed(java.awt.event.WindowEvent e){timer.stop();dispose();}});
    }
    void showTab(int index){tabs.setSelectedIndex(index);refresh();setVisible(true);}
    private void refresh(){var v=runtime.snapshot();StringBuilder text=new StringBuilder("DIARIO DE "+v.identity().name()+"\n\n");
        for(var q:v.journal()){if(q.state()==QuestService.State.UNAVAILABLE){text.append("Missao ainda indisponivel\n\n");continue;}text.append(q.name()).append(" - ").append(q.state()).append(" [").append(q.progress()).append('/').append(q.required()).append("]\n").append(q.objective()).append("\nRecompensas na entrega a Ancia.\n\n");}journal.setText(text.toString());
        char[][] cells=new char[24][48];for(char[] row:cells)java.util.Arrays.fill(row,'.');
        for(int y=7;y<12;y++)for(int x=19;x<25;x++)cells[y][x]='#';
        for(var p:v.portals())mark(cells,p.x(),p.y(),'O');for(var n:v.npcs())mark(cells,n.x(),n.y(),'N');for(var o:v.objects())if(o.available())mark(cells,o.x(),o.y(),'C');
        for(var enemy:v.enemies())if(enemy.hp()>0&&Math.hypot(enemy.x()-v.x(),enemy.y()-v.y())<=280)mark(cells,enemy.x(),enemy.y(),'!');mark(cells,v.x(),v.y(),'@');
        StringBuilder grid=new StringBuilder(RpgContent.builtin().get("region",v.state().region()).name()+"\n");for(char[] row:cells)grid.append(row).append('\n');
        grid.append("@ voce | O portal [F] | N NPC [F] | C recurso [F]\n! inimigo proximo | # solido\n\nRegioes visitadas:\n");v.state().flags().stream().filter(f->f.startsWith("visited:")).sorted().forEach(f->grid.append(RpgContent.builtin().get("region",f.substring(8)).name()).append('\n'));map.setText(grid.toString());
    }
    private static void mark(char[][] cells,float x,float y,char marker){cells[Math.max(0,Math.min(23,(int)(y/640*24)))][Math.max(0,Math.min(47,(int)(x/960*48)))]=marker;}
}
