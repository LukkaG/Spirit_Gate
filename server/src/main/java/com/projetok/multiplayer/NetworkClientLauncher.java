package com.projetok.multiplayer;
import com.projetok.prototype.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.util.*;
import javax.swing.Timer;
/** Janela experimental independente do RPG offline; nenhum servidor e iniciado aqui. */
public final class NetworkClientLauncher {
    private NetworkClientLauncher(){}
    public static void main(String[] args){SwingUtilities.invokeLater(()->{
        String host="127.0.0.1",name="Viajante";int port=54557;boolean allowLan=args.length==4&&args[3].equals("--lan");
        if(args.length==3||args.length==4){host=args[0];port=Integer.parseInt(args[1]);name=args[2];}
        else if(args.length==0){JTextField h=new JTextField(host),p=new JTextField("54557"),n=new JTextField(name);JCheckBox lan=new JCheckBox("Habilitar conexao LAN privada");JPanel form=new JPanel(new GridLayout(0,2));form.add(new JLabel("Servidor local"));form.add(h);form.add(new JLabel("Porta"));form.add(p);form.add(new JLabel("Nome transitorio"));form.add(n);form.add(lan);if(JOptionPane.showConfirmDialog(null,form,"Coop experimental - sem contas",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;host=h.getText();name=n.getText();allowLan=lan.isSelected();try{port=Integer.parseInt(p.getText());}catch(NumberFormatException error){JOptionPane.showMessageDialog(null,"Porta invalida");return;}}
        else throw new IllegalArgumentException("Uso: [host porta nome [captura-componentes.png]]");
        String address=host,playerName=name;int chosenPort=port;boolean lanEnabled=allowLan;
        new SwingWorker<LocalClient,Void>(){protected LocalClient doInBackground()throws Exception{return lanEnabled?LocalClient.lan(InetAddress.getByName(address),chosenPort,playerName):new LocalClient(InetAddress.getByName(address),chosenPort,playerName);}protected void done(){try{open(get(),address+":"+chosenPort,args.length==4&&!lanEnabled?args[3]:null);}catch(Exception error){JOptionPane.showMessageDialog(null,"Falha ao conectar: "+error.getMessage());}}}.execute();
    });}
    private static void open(LocalClient client,String endpoint,String capture){
        var template=DebugWorld.demo().snapshot();Protocol.State[] display={new Protocol.State(0,java.util.List.of())};
        DebugPanel panel=new DebugPanel(()->adapt(display[0],client.id(),template));panel.setActorPalette(id->id==client.id()?new Color(65,190,220):id==CoopWorld.BOSS_ID?new Color(220,90,90):new Color(90,200,160));
        JFrame frame=new JFrame("Spirit Gate | Coop experimental | "+client.id());frame.setContentPane(panel);frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);frame.setSize(1200,800);frame.setLocationRelativeTo(null);
        Set<String> held=new HashSet<>();float[] direction={0,0},aim={650,320};
        Runnable movement=()->{direction[0]=(held.contains("D")?1:0)-(held.contains("A")?1:0);direction[1]=(held.contains("S")?1:0)-(held.contains("W")?1:0);};
        for(String key:new String[]{"W","A","S","D","SPACE","SHIFT","Q"})for(boolean down:new boolean[]{true,false}){
            String id=key+down;panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke((down?"pressed ":"released ")+key),id);
            panel.getActionMap().put(id,new AbstractAction(){public void actionPerformed(ActionEvent e){boolean fresh=down&&held.add(key);if(!down)held.remove(key);movement.run();if(fresh)switch(key){case "SPACE"->client.send(Protocol.Action.ATTACK,0,0);case "SHIFT"->client.send(Protocol.Action.LIGHTNING_STEP,aim[0],aim[1]);case "Q"->client.send(Protocol.Action.CRYOSTASIS,aim[0],aim[1]);default->{}}}});
        }
        panel.addMouseMotionListener(new MouseMotionAdapter(){public void mouseMoved(MouseEvent e){var viewport=ArenaViewport.fit(Math.max(1,panel.getWidth()),Math.max(1,panel.getHeight()),960,640,480,320,1);if(viewport.containsScreen(e.getX(),e.getY())){var p=viewport.screenToWorld(e.getX(),e.getY());aim[0]=(float)p.x;aim[1]=(float)p.y;}}});
        frame.addWindowFocusListener(new WindowAdapter(){public void windowLostFocus(WindowEvent e){held.clear();movement.run();client.send(Protocol.Action.MOVE,0,0);}});
        RemotePresentation[] frames={null};long[] received={0};int[] count={0};
        Timer timer=new Timer(33,e->{var latest=client.snapshot();if(latest!=null){if(frames[0]==null){frames[0]=new RemotePresentation(latest,latest);received[0]=System.nanoTime();}else if(latest.tick()>frames[0].current().tick()){frames[0]=new RemotePresentation(frames[0].current(),latest);received[0]=System.nanoTime();}display[0]=frames[0].sample(Math.min(1,(System.nanoTime()-received[0])/100_000_000.0),client.id());}if(++count[0]%3==0)client.send(Protocol.Action.MOVE,direction[0],direction[1]);var self=display[0].actors().stream().filter(a->a.id()==client.id()).findFirst();panel.setCustomHud(java.util.List.of("COOP EXPERIMENTAL",endpoint,"Sessao "+client.id(),"Tick "+display[0].tick(),client.connected()?"Conectado":"Desconectado",self.map(a->"HP "+a.hp()+" / "+a.maxHp()).orElse("Aguardando snapshot"),self.map(a->"Vigor "+Math.round(a.stamina())+" | Moedas "+a.coins()).orElse(""),"WASD: movimento","SPACE: golpe","SHIFT: Passo Relampago","Q: Criostase","Mouse: alvo das habilidades","Estado vem do servidor","Sem contas ou saves offline"));panel.repaint();});
        frame.addWindowListener(new WindowAdapter(){public void windowClosed(WindowEvent e){timer.stop();client.close();}});timer.start();frame.setVisible(true);
        if(capture!=null){Timer shot=new Timer(2200,e->{try{var image=new java.awt.image.BufferedImage(frame.getWidth(),frame.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();frame.printAll(g);g.dispose();javax.imageio.ImageIO.write(image,"png",new java.io.File(capture));java.nio.file.Files.writeString(java.nio.file.Path.of(capture+".json"),"{\"windowShowing\":"+frame.isShowing()+",\"serverTick\":"+display[0].tick()+",\"session\":"+client.id()+",\"players\":"+display[0].actors().stream().filter(a->!a.boss()).count()+",\"componentCapture\":true,\"humanApproved\":false}");}catch(Exception error){error.printStackTrace();}finally{frame.dispose();}});shot.setRepeats(false);shot.start();}
    }
    static DebugSnapshot adapt(Protocol.State state,int local,DebugSnapshot template){
        return adapt(state,local,template,"Coop experimental - servidor autoritativo",100);
    }
    static DebugSnapshot adapt(Protocol.State state,int local,DebugSnapshot template,String description,float maxVigor){
        return adapt(state,local,template,description,maxVigor,null,"forest");
    }
    static DebugSnapshot adapt(Protocol.State state,int local,DebugSnapshot template,String description,float maxVigor,com.projetok.studio.WorldContent.Prepared content,String region){
        var actors=state.actors().stream().map(a->new DebugSnapshot.ActorView(a.id(),a.name(),a.x(),a.y(),a.boss()?24:content==null?18:content.revision().profile().actorRadius(),a.hp(),a.maxHp(),a.id()==local,a.hp()>0)).toList();var self=state.actors().stream().filter(a->a.id()==local).findFirst();int hp=self.map(Protocol.Actor::hp).orElse(0),max=self.map(Protocol.Actor::maxHp).orElse(1);
        var statuses=state.actors().stream().filter(a->(a.flags()&4)!=0).map(a->new DebugSnapshot.StatusView(a.id(),1)).toList();
        return new DebugSnapshot(actors,content==null?java.util.List.of(new DebugSnapshot.Obstacle(380,180,120,120)):content.revision().geometry().get(region).obstacles().stream().map(o->new DebugSnapshot.Obstacle(o.x(),o.y(),o.width(),o.height())).toList(),java.util.List.of(),hp,max,0,false,state.tick(),description,self.map(Protocol.Actor::facingX).orElse(1f),self.map(Protocol.Actor::facingY).orElse(0f),0,null,self.map(Protocol.Actor::stamina).orElse(0f),maxVigor,0,java.util.List.of(),statuses,self.map(a->(a.flags()&1)!=0).orElse(false),0,0,java.util.List.of(),self.map(a->(a.flags()&2)!=0).orElse(false),java.util.List.of(),template.cryostasisHud(),template.lightningStepHud());
    }
}
