package com.projetok.studio;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.nio.file.*;
import java.util.*;

/** Entrada desktop independente do Spirit Gate World Studio. */
public final class WorldStudio extends JFrame {
    final Path project;
    WorldDocument document=WorldDocument.empty("primeiro_mapa",960,640);
    final EditorHistory history=new EditorHistory(document);
    Point lastCursor=new Point(0,0);
    AssetCatalog.Result catalog=new AssetCatalog.Result(java.util.List.of(),java.util.List.of(),java.util.List.of());
    final AssetImages images=new AssetImages();
    final WorldCanvas canvas=new WorldCanvas(()->document,images);
    final DefaultListModel<AssetCatalog.Asset> assets=new DefaultListModel<>();
    final JList<AssetCatalog.Asset> library=new JList<>(assets);
    final JTextField search=new JTextField();final JComboBox<String> categories=new JComboBox<>(new String[]{"Todas","Sem categoria","Terrenos","Arvores","Construcoes","Decoracao","Personagens","NPCs","Inimigos","Interativos","Efeitos","Tilesets"});
    final TilePalette tilePalette=new TilePalette(this);
    final JTextArea details=new JTextArea();final JLabel status=new JLabel("Indexe os assets do projeto para comecar.");
    final JToolBar toolbar=new WrapToolbar();final JButton refresh=new JButton("Atualizar biblioteca");
    LayerTools layerTools;FileTools fileTools;
    AssetLibraryTools libraryTools;
    CollisionTools collisionTools;
    boolean snap=true;
    final Map<String,WorldDocument> knownMaps=new LinkedHashMap<>();
    final JPanel inspector=new JPanel(new BorderLayout());
    final JTabbedPane inspectorTabs=new JTabbedPane();
    AssetCatalog.Asset brush;
    WorldDocument.Crop brushCrop;
    java.util.List<WorldDocument.Crop> tilesetCrops=java.util.List.of();String activeTerrain="";
    /** Abre projeto local; indexacao e carregamento ocorrem em workers. */
    public WorldStudio(Path project) {
        super("Spirit Gate | World Studio 2.0");this.project=project.toAbsolutePath();
        StudioTheme.dialogs();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1400,900);setMinimumSize(new Dimension(900,600));setLocationRelativeTo(null);
        toolbar.setFloatable(false);toolbar.add(refresh);refresh.addActionListener(e->rescan());
        JButton minus=new JButton("Zoom -"),plus=new JButton("Zoom +");toolbar.add(minus);toolbar.add(plus);minus.addActionListener(e->canvas.zoom(canvas.viewport.zoom()/1.2));plus.addActionListener(e->canvas.zoom(canvas.viewport.zoom()*1.2));
        JCheckBox grid=new JCheckBox("Grade",true);grid.addActionListener(e->{canvas.grid=grid.isSelected();canvas.repaint();});toolbar.add(grid);
        JPanel left=new JPanel(new BorderLayout(4,4)),filters=new JPanel(new GridLayout(0,1,4,4));filters.add(search);filters.add(categories);left.add(filters,BorderLayout.NORTH);left.add(new JScrollPane(library));
        libraryTools=new AssetLibraryTools(this,filters);
        library.setFixedCellHeight(70);library.setCellRenderer((list,value,index,selected,focus)->{JLabel label=new JLabel(Path.of(value.path()).getFileName().toString(),libraryTools.icon(value),SwingConstants.LEFT);label.setOpaque(true);label.setForeground(StudioTheme.TEXT);label.setBackground(selected?StudioTheme.SELECTED:StudioTheme.FIELD);label.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(focus?StudioTheme.ACCENT:StudioTheme.FIELD),BorderFactory.createEmptyBorder(4,6,4,6)));label.setToolTipText(value.path()+" | "+value.width()+" x "+value.height());return label;});
        search.getDocument().addDocumentListener(new DocumentListener(){public void insertUpdate(DocumentEvent e){filter();}public void removeUpdate(DocumentEvent e){filter();}public void changedUpdate(DocumentEvent e){filter();}});categories.addActionListener(e->filter());
        library.addListSelectionListener(e->{if(!e.getValueIsAdjusting()){brush=library.getSelectedValue();if(brush!=null){tilesetCrops=java.util.List.of();activeTerrain="";brushCrop=new WorldDocument.Crop(0,0,brush.width(),brush.height());details.setText(brush.path()+"\n"+brush.width()+" x "+brush.height()+"\nCanal alfa: "+brush.alpha()+"\nCategoria: "+brush.category()+"\nSHA-256: "+brush.sha()+"\nIndexado: "+brush.indexedAt());tilePalette.showAsset(brush);inspectorTabs.setSelectedComponent(tilePalette);warmImages();}}});
        JButton category=new JButton("Definir categoria (F2)");category.addActionListener(e->defineCategory());left.add(category,BorderLayout.SOUTH);
        library.getInputMap().put(KeyStroke.getKeyStroke("F2"),"category");library.getActionMap().put("category",new AbstractAction(){public void actionPerformed(java.awt.event.ActionEvent e){defineCategory();}});
        details.setEditable(false);details.setLineWrap(true);details.setWrapStyleWord(true);inspector.add(new JScrollPane(details));inspector.setPreferredSize(new Dimension(260,400));
        JSplitPane center=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,canvas,inspector);center.setResizeWeight(1);JSplitPane all=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,left,center);all.setDividerLocation(270);all.setResizeWeight(0);
        setLayout(new BorderLayout(6,6));add(toolbar,BorderLayout.NORTH);add(all);add(status,BorderLayout.SOUTH);
        canvas.pointer=p->{lastCursor=p;status.setText(document.name()+" | "+p.x+", "+p.y+" | Grade "+document.grid()+" | Zoom "+Math.round(canvas.viewport.zoom()*100)+"% | Camada "+canvas.activeLayer);};
        canvas.pressed=e->{Point p=canvas.worldPoint(e);canvas.selected.clear();document.elements().stream().filter(a->new Rectangle(a.x(),a.y(),a.width(),a.height()).contains(p)).reduce((a,b)->b).ifPresent(a->{canvas.selected.add(a.id());details.setText(a.toString());});canvas.repaint();};
        new PaintTools(this);
        layerTools=new LayerTools(this);
        collisionTools=new CollisionTools(this);
        new InteractionTools(this);
        new PortalTools(this);
        fileTools=new FileTools(this);
        new PlaytestTools(this);
        new TerrainTools(this);
        new PrefabTools(this);
        new GroupTools(this);
        new AnimationTools(this);
        new EnvironmentTools(this);
        new NpcTools(this);
        new SpawnTools(this);
        new EventTools(this);
        new NavigationTools(this);
        new DiagnosticTools(this);
        new FinalTools(this);
        new MapTools(this);
        new StudioHelp(this);
        action("Missões","Abrir Quest Builder",()->new QuestBuilder(project).setVisible(true));
        action("Personagem","Aparência temporária (perfil sintético)",()->AppearancePreview.launch(project));
        search.setToolTipText("Buscar recursos pelo caminho ou nome");
        search.getAccessibleContext().setAccessibleName("Buscar recursos");
        categories.getAccessibleContext().setAccessibleName("Categoria de recursos");
        library.getAccessibleContext().setAccessibleName("Biblioteca de recursos");
        details.getAccessibleContext().setAccessibleName("Propriedades e diagnóstico");
        left.setBorder(BorderFactory.createTitledBorder("Biblioteca de recursos"));
        status.setBorder(BorderFactory.createEmptyBorder(7,10,7,10));
        toolbar.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        StudioTheme.apply(getContentPane());
        if(getJMenuBar()!=null) StudioTheme.apply(getJMenuBar());
        getContentPane().setBackground(StudioTheme.BACKGROUND);
    }
    void change(WorldDocument next){document=history.commit(Autotiling.refresh(next));if(layerTools!=null)layerTools.refresh();canvas.repaint();}
    void inspectSelection(){details.setText(document.elements().stream().filter(e->canvas.selected.contains(e.id())).map(Object::toString).collect(java.util.stream.Collectors.joining("\n")));}
    Map<String,WorldDocument> maps(){var maps=new LinkedHashMap<>(knownMaps);maps.put(document.id(),document);return Map.copyOf(maps);}
    void action(String group,String name,Runnable action){JMenuBar bar=getJMenuBar();if(bar==null){bar=new JMenuBar();setJMenuBar(bar);}JMenu menu=null;for(int i=0;i<bar.getMenuCount();i++)if(bar.getMenu(i).getText().equals(group))menu=bar.getMenu(i);if(menu==null){menu=new JMenu(group);bar.add(menu);}JMenuItem item=new JMenuItem(name);item.addActionListener(e->{try{action.run();}catch(Exception error){error(error);}});menu.add(item);}
    void filter(){assets.clear();var found=AssetCatalog.search(catalog,search.getText(),(String)categories.getSelectedItem());if(libraryTools!=null)found=libraryTools.filter(found);found.forEach(assets::addElement);}
    void defineCategory(){
        var asset=library.getSelectedValue();if(asset==null)return;
        JComboBox<String> choice=new JComboBox<>(new String[]{"Terrenos","Arvores","Construcoes","Decoracao","Personagens","NPCs","Inimigos","Interativos","Efeitos","Tilesets","Sem categoria"});choice.setSelectedItem(asset.category());
        if(JOptionPane.showConfirmDialog(this,choice,"Definir categoria da imagem",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        String next=(String)choice.getSelectedItem();catalog=new AssetCatalog.Result(catalog.assets().stream().map(a->a.id().equals(asset.id())?a.category(next):a).toList(),catalog.warnings(),catalog.changes());
        filter();if(fileTools!=null)fileTools.saveCategories();status.setText("Categoria definida: "+next);
    }
    void rescan(){refresh.setEnabled(false);status.setText("Indexando somente assets e recursos autorizados...");new SwingWorker<AssetCatalog.Result,Void>(){
        protected AssetCatalog.Result doInBackground()throws Exception{var previous=catalog;var result=AssetCatalog.scan(project,previous);Path categories=project.resolve("studio-projects/catalog.properties");if(java.nio.file.Files.isRegularFile(categories)&&java.nio.file.Files.size(categories)<=1_000_000&&categories.toRealPath().startsWith(project.toRealPath())){Properties saved=new Properties();try(var in=java.nio.file.Files.newBufferedReader(categories)){saved.load(in);}var known=previous.byId();result=new AssetCatalog.Result(result.assets().stream().map(a->{String c=saved.getProperty(a.id());return c!=null&&c.length()<=80&&!known.containsKey(a.id())?a.category(c):a;}).toList(),result.warnings(),result.changes());}return result;}
        protected void done(){try{catalog=get();images.clear();filter();warmImages();status.setText(catalog.assets().size()+" imagens | "+catalog.changes().size()+" alteracoes | "+catalog.warnings().size()+" avisos");if(!catalog.warnings().isEmpty())details.setText(String.join("\n",catalog.warnings()));}catch(Exception error){error(error);}finally{refresh.setEnabled(true);}}
    }.execute();}
    void warmImages(){var ids=new HashSet<String>();document.elements().forEach(e->{ids.add(e.asset());try{EnvironmentAnimation.sequence(e).ifPresent(s->ids.addAll(s.frames()));}catch(RuntimeException invalid){status.setText("Sequencia invalida em "+e.id()+"; consulte Diagnostico");}});if(brush!=null)ids.add(brush.id());var selected=catalog.byId();new SwingWorker<Void,Void>(){
        protected Void doInBackground()throws Exception{for(String id:ids)if(selected.containsKey(id))images.load(selected.get(id));return null;}
        protected void done(){try{get();tilePalette.repaint();canvas.repaint();}catch(Exception error){error(error);}}
    }.execute();}
    void error(Exception error){status.setText("Operacao recusada: "+(error.getCause()==null?error.getMessage():error.getCause().getMessage()));}
    /** Inicializa a ferramenta sem iniciar banco, servidor ou personagem. */
    public static void main(String[] args){String exe=System.getProperty("jpackage.app-path");Path root=args.length==2&&args[0].equals("--project")?Path.of(args[1]):exe==null?Path.of(""):Path.of(exe).toAbsolutePath().getParent();SwingUtilities.invokeLater(()->{var studio=new WorldStudio(root);studio.setVisible(true);studio.rescan();});}
}
