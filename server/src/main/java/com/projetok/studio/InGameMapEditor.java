package com.projetok.studio;

import com.projetok.multiplayer.*;
import com.projetok.prototype.DebugPanel;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.swing.*;

/** Ferramentas compactas sobre a partida existente, sem outro servidor, personagem ou viewport. */
final class InGameMapEditor implements AutoCloseable {
    final WorldStudio ui;
    final JFrame frame;
    final RevisionCoordinator target;
    final LivePublishing store;
    final LocalClient client;
    final DebugPanel world;
    final GamePanels panels;
    final GamePanels.Panel palette;
    final JCheckBox editing=new JCheckBox("Editar cenário (F4)",false);
    final JLabel status=new JLabel("Escolha uma ferramenta e ative a edição");
    private final javax.swing.Timer timer;
    private WorldDocument applied,observed;
    private long changedAt;
    private boolean busy,gesture,visit;
    private volatile boolean closed;
    private Object previousF4;
    private final Map<KeyStroke,Object> previousKeys=new LinkedHashMap<>();
    private final java.util.List<String> editorActions=new ArrayList<>();
    private final java.awt.event.WindowListener lifecycle;

    /** Reutiliza os componentes de autoria e a infraestrutura de painéis da HUD atual. */
    InGameMapEditor(WorldStudio ui,JFrame frame,RevisionCoordinator target,LivePublishing store){
        this.ui=ui;this.frame=frame;this.target=target;this.store=store;
        if(!(frame.getRootPane().getClientProperty("networkClient") instanceof LocalClient c)
                ||!(frame.getRootPane().getClientProperty("networkPanel") instanceof DebugPanel w)
                ||!(frame.getRootPane().getClientProperty("networkPanels") instanceof GamePanels p))throw new IllegalStateException("Abra o editor dentro de uma partida do anfitrião");
        client=c;world=w;panels=p;ui.inGameEditor=this;
        var revision=target.revision();var current=revision.bundle().maps().values().stream().filter(d->d.region().equals(client.region())).findFirst().orElseThrow();
        ui.knownMaps.clear();ui.knownMaps.putAll(revision.bundle().maps());ui.document=ui.history.reset(current);ui.fileTools.saved=current;applied=observed=current;
        ui.catalog=revision.bundle().assets();ui.filter();ui.layerTools.refresh();ui.warmImages();
        var body=new JPanel(new BorderLayout(4,6));var top=new JPanel(new GridLayout(0,1,4,4));top.add(editing);
        var tabs=new JTabbedPane();var tools=new JPanel(new GridLayout(0,1,4,6));
        tools.add(new JLabel("Ferramenta"));tools.add(ui.toolSelector);
        ui.toolSelector.addActionListener(e->setEditing(true));
        for(var component:ui.toolbar.getComponents())if(component instanceof JComboBox<?> combo&&combo.getItemCount()>0&&"Objeto real".equals(combo.getItemAt(0)))tools.add(combo);
        var layers=new JComboBox<String>();for(var layer:ui.document.orderedLayers())layers.addItem(layer.id());layers.setSelectedItem(ui.canvas.activeLayer);
        var updatingLayers=new boolean[1];layers.addActionListener(e->{if(updatingLayers[0])return;ui.canvas.activeLayer=(String)layers.getSelectedItem();for(int i=0;i<ui.layerTools.model.size();i++)if(ui.layerTools.model.get(i).id().equals(ui.canvas.activeLayer))ui.layerTools.list.setSelectedIndex(i);});
        ui.layerTools.list.addListSelectionListener(e->{if(updatingLayers[0]||ui.layerTools.list.getSelectedValue()==null)return;String selected=ui.layerTools.list.getSelectedValue().id();updatingLayers[0]=true;try{layers.removeAllItems();for(var layer:ui.document.orderedLayers())layers.addItem(layer.id());layers.setSelectedItem(selected);}finally{updatingLayers[0]=false;}});
        tools.add(new JLabel("Camada"));tools.add(layers);
        var grid=new JCheckBox("Mostrar grade",ui.canvas.grid);grid.addActionListener(e->ui.canvas.grid=grid.isSelected());tools.add(grid);
        button(tools,"Mapas / novo mapa",()->{tabs.setSelectedIndex(2);ui.inspectorTabs.setSelectedIndex(ui.inspectorTabs.indexOfTab("Mapas"));ui.adminMaps.refresh();});
        button(tools,"Desfazer",()->ui.change(ui.history.undo()));button(tools,"Refazer",()->ui.change(ui.history.redo()));
        button(tools,"Salvar mapa (.sgmap)",()->ui.fileTools.save(false,()->{}));button(tools,"Salvar mundo em disco",this::saveWorld);
        var advanced=new JPopupMenu();var bar=ui.getJMenuBar();while(bar.getMenuCount()>0){var item=bar.getMenu(0);bar.remove(item);advanced.add(item);}
        button(tools,"Mais opções",()->advanced.show(tools,0,tools.getHeight()));
        var library=new JPanel(new BorderLayout(4,4));var filters=new JPanel(new GridLayout(0,1,3,3));filters.add(ui.search);filters.add(ui.categories);library.add(filters,BorderLayout.NORTH);library.add(new JScrollPane(ui.library));
        tabs.addTab("Ferramentas",new JScrollPane(tools));tabs.addTab("Biblioteca",library);tabs.addTab("Opções",ui.inspectorTabs);
        ui.inspectorTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        ui.library.addListSelectionListener(e->{if(!e.getValueIsAdjusting()&&ui.library.getSelectedValue()!=null){tabs.setSelectedIndex(2);setEditing(true);}});
        ui.inspectorTabs.addChangeListener(e->{if(ui.inspectorTabs.getSelectedIndex()>=0&&"Colisao".equals(ui.inspectorTabs.getTitleAt(ui.inspectorTabs.getSelectedIndex())))setEditing(true);});
        editing.addActionListener(e->setEditing(editing.isSelected()));top.add(new JLabel("F4: alternar entre criar e jogar"));body.add(top,BorderLayout.NORTH);body.add(tabs);status.setFont(new Font("SansSerif",Font.PLAIN,10));body.add(status,BorderLayout.SOUTH);
        palette=panels.register("world-editor","Editor de mapa",body,340,540,false);
        transparent(body);
        palette.canClose(()->{boolean ready=!busy&&!ui.fileTools.busy&&ui.history.current()==applied;if(!ready)status.setText("Aguarde a última edição antes de fechar");return ready;});palette.onClose(ui::dispose);palette.showPanel();palette.setLocation(12,90);
        frame.getRootPane().putClientProperty("inGameMapEditor",ui);
        ui.canvas.gameCoordinates=e->{var point=world.aimAt(e.getX(),e.getY());return Double.isFinite(point.x)&&Double.isFinite(point.y)?new Point((int)Math.floor(point.x),(int)Math.floor(point.y)):new Point(-1,-1);};
        world.setEditorInput(this::mouse);world.setEditorOverlay(g->{if(editing.isSelected()&&client.region().equals(ui.document.region()))ui.canvas.gameOverlay(g);});
        var key=KeyStroke.getKeyStroke("F4");previousF4=world.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(key);world.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key,"editor-toggle");
        world.getActionMap().put("editor-toggle",new AbstractAction(){@Override public void actionPerformed(ActionEvent e){setEditing(!editing.isSelected());}});
        for(var spec:new String[][]{{"control Z","Desfazer"},{"control Y","Refazer"},{"control C","Copiar"},{"control V","Colar"},{"ESCAPE","Cancelar gesto"}}){
            var stroke=KeyStroke.getKeyStroke(spec[0]);String id="editor-"+spec[1];previousKeys.put(stroke,world.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(stroke));editorActions.add(id);world.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke,id);
            world.getActionMap().put(id,new AbstractAction(){@Override public void actionPerformed(ActionEvent e){if(!editing.isSelected())return;var action=ui.getRootPane().getActionMap().get(spec[1]);if(action!=null)action.actionPerformed(e);if(spec[0].equals("ESCAPE"))setEditing(false);}});
        }
        lifecycle=new WindowAdapter(){@Override public void windowClosed(WindowEvent e){ui.dispose();}};frame.addWindowListener(lifecycle);
        timer=new javax.swing.Timer(100,e->poll());timer.start();ui.adminMaps.refresh();ui.rescan();
    }
    private void button(JPanel panel,String label,Runnable action){var button=new JButton(label);button.addActionListener(e->{try{action.run();}catch(Exception error){ui.error(error);status.setText(error.getMessage());}});panel.add(button);}
    /** Mantém a transparência do painel da HUD também nos viewports das ferramentas reutilizadas. */
    private static void transparent(Component component){
        if(component instanceof JPanel panel)panel.setOpaque(false);
        if(component instanceof JViewport viewport)viewport.setOpaque(false);
        if(component instanceof JScrollPane scroll){scroll.setOpaque(false);scroll.getViewport().setOpaque(false);scroll.setBorder(BorderFactory.createLineBorder(new Color(84,205,233,65)));}
        if(component instanceof JTabbedPane tabs){tabs.setOpaque(false);tabs.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI(){
            @Override protected void paintTabBackground(Graphics g,int placement,int index,int x,int y,int w,int h,boolean selected){g.setColor(selected?new Color(21,62,79):new Color(8,27,44));g.fillRoundRect(x,y,w,h,7,7);}
            @Override protected void paintContentBorder(Graphics g,int placement,int selected){}
        });for(int i=0;i<tabs.getTabCount();i++)tabs.setForegroundAt(i,new Color(224,245,250));}
        if(component instanceof Container container)for(var child:container.getComponents())transparent(child);
    }
    /** Permite ao anfitrião caminhar com o mesmo personagem e voltar à criação por F4. */
    void setEditing(boolean value){
        if(closed)return;if(gesture){var cancel=ui.getRootPane().getActionMap().get("Cancelar gesto");if(cancel!=null)cancel.actionPerformed(null);}editing.setSelected(value);gesture=false;ui.canvas.selectionBox=null;
        if(value&&!visit&&!busy&&!ui.document.region().equals(client.region())){
            var current=target.revision().bundle().maps().values().stream().filter(d->d.region().equals(client.region())).findFirst();
            if(current.isPresent()){ui.knownMaps.put(ui.document.id(),ui.document);ui.document=ui.history.reset(current.get());applied=observed=ui.document;ui.fileTools.file=null;ui.fileTools.saved=ui.document;ui.canvas.selected.clear();ui.collisionTools.cell=null;ui.layerTools.refresh();ui.warmImages();}
        }
        frame.getRootPane().putClientProperty("worldEditorActive",value?Boolean.TRUE:null);
        if(frame.getRootPane().getClientProperty("networkInputRouter") instanceof GameInputRouter input)input.refreshContext();
        world.requestFocusInWindow();world.repaint();
    }
    /** Seleção de mapas continua na mesma conexão; a visita é validada pela capacidade local do servidor. */
    void openDocument(){observed=null;changedAt=System.nanoTime()-300_000_000L;visit=!client.region().equals(ui.document.region());setEditing(true);}
    private boolean mouse(MouseEvent event){
        if(!editing.isSelected()||closed)return false;
        if(event.getID()==MouseEvent.MOUSE_ENTERED||event.getID()==MouseEvent.MOUSE_EXITED)return false;
        if(event.getID()==MouseEvent.MOUSE_PRESSED)world.requestFocusInWindow();var point=ui.canvas.worldPoint(event);ui.canvas.cursor=point;ui.lastCursor=point;
        boolean inside=client.region().equals(ui.document.region())&&point.x>=0&&point.y>=0&&point.x<ui.document.width()&&point.y<ui.document.height();
        if(!inside){if(event.getID()==MouseEvent.MOUSE_RELEASED&&gesture){var cancel=ui.getRootPane().getActionMap().get("Cancelar gesto");if(cancel!=null)cancel.actionPerformed(null);gesture=false;}return true;}
        if(event.getID()==MouseEvent.MOUSE_PRESSED&&(SwingUtilities.isLeftMouseButton(event)||SwingUtilities.isRightMouseButton(event))){gesture=true;ui.canvas.pressed.accept(event);}
        else if(event.getID()==MouseEvent.MOUSE_DRAGGED&&gesture)ui.canvas.dragged.accept(event);
        else if(event.getID()==MouseEvent.MOUSE_RELEASED&&gesture){ui.canvas.released.accept(event);gesture=false;}
        ui.canvas.pointer.accept(point);world.repaint();return true;
    }
    /** Agrupa gestos confirmados e aplica uma revisão por vez; falhas mantêm a cena anterior. */
    private void poll(){
        if(closed||busy||ui.fileTools.busy||!ui.refresh.isEnabled())return;
        var draft=ui.history.current();
        if(draft!=observed){observed=draft;changedAt=System.nanoTime();}
        if(draft==applied&&!visit)return;
        if(System.nanoTime()-changedAt<200_000_000L||client.revisions().blocked())return;
        boolean visiting=visit;visit=false;busy=true;var base=target.revision();var assets=ui.catalog;status.setText("Aplicando ao mapa da partida...");
        new SwingWorker<LiveRevision,Void>(){
            protected LiveRevision doInBackground()throws Exception{
                var next=LiveMapEdits.replace(base,draft,assets,base.number()+1);
                if(!next.hash().equals(base.hash())){var result=LiveMapEdits.apply(target,base.hash(),next,()->closed);if(!result.accepted())throw new IllegalStateException(result.message());}
                if(visiting&&!closed){var result=target.visitEditorMap(client.id(),draft.id()).get(10,TimeUnit.SECONDS);if(!result.accepted())throw new IllegalStateException("Mapa atualizado, mas entrada indisponível: destino ocupado ou personagem em combate");}
                return target.revision();
            }
            protected void done(){busy=false;if(closed)return;try{var active=get();applied=draft;ui.knownMaps.putAll(active.bundle().maps());status.setText("Mapa atualizado na partida | Salvar mundo grava em disco");}catch(Exception error){applied=draft;status.setText("Edição recusada: "+root(error));ui.error(error);}world.repaint();}
        }.execute();
    }
    /** Persiste somente por ação explícita; pinceladas anteriores não criam versões em disco. */
    void saveWorld(){
        if(busy||ui.fileTools.busy){status.setText("Aguarde a edição atual antes de salvar");return;}
        busy=true;var draft=ui.history.current();var base=target.revision();var assets=ui.catalog;status.setText("Salvando mundo...");
        new SwingWorker<LivePublishing.Result,Void>(){
            protected LivePublishing.Result doInBackground()throws Exception{
                var next=LiveMapEdits.replace(base,draft,assets,base.number()+1);
                return store.publish(next,store.current(),base.hash(),target,LivePublishing.Fault.NONE,()->closed);
            }
            protected void done(){busy=false;if(closed)return;try{var result=get();status.setText(result.message());if(result.active())applied=draft;}catch(Exception error){status.setText("Falha ao salvar: "+root(error));ui.error(error);}}
        }.execute();
    }
    boolean busy(){return busy;}
    private static String root(Throwable failure){while(failure.getCause()!=null)failure=failure.getCause();return String.valueOf(failure.getMessage());}
    /** Retira somente ferramentas e interceptadores; a partida e as edições aceitas permanecem. */
    @Override public void close(){
        if(closed)return;closed=true;timer.stop();world.setEditorInput(null);world.setEditorOverlay(null);frame.removeWindowListener(lifecycle);
        frame.getRootPane().putClientProperty("worldEditorActive",null);frame.getRootPane().putClientProperty("inGameMapEditor",null);
        var key=KeyStroke.getKeyStroke("F4");if(previousF4==null)world.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).remove(key);else world.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key,previousF4);world.getActionMap().remove("editor-toggle");panels.unregister("world-editor");world.requestFocusInWindow();
        previousKeys.forEach((stroke,action)->{if(action==null)world.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).remove(stroke);else world.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke,action);});editorActions.forEach(world.getActionMap()::remove);
    }
}
