package com.projetok.studio;
import javax.swing.*;
import java.awt.*;
import java.util.*;

/** Hitboxes editaveis em dados, com snap opcional e previa de raio 18. */
final class CollisionTools {
    final WorldStudio ui;
    boolean directional;
    Point cell;
    final JToggleButton[] sides=new JToggleButton[4];
    static final int[] BITS={WorldDocument.Hitbox.UP,WorldDocument.Hitbox.DOWN,WorldDocument.Hitbox.LEFT,WorldDocument.Hitbox.RIGHT};
    /** Secao de colisao independente da biblioteca de arte. */
    void installPanel(){
        var panel=new JPanel(new BorderLayout(4,8));var arrows=new JPanel(new GridLayout(3,3,3,3));
        String[] labels={"\u2191","\u2193","\u2190","\u2192"};int[] locations={1,7,3,5};
        for(int i=0;i<9;i++){int at=-1;for(int k=0;k<4;k++)if(locations[k]==i)at=k;
            if(at<0){arrows.add(new JLabel(i==4?"Tile":"",SwingConstants.CENTER));continue;}
            int index=at;sides[index]=new JToggleButton(new CollisionArrow(BITS[index],24),true);sides[index].setPreferredSize(new Dimension(46,38));sides[index].getAccessibleContext().setAccessibleName("Borda "+labels[index]);sides[index].setToolTipText("Bloquear borda "+labels[index]+" nos dois sentidos");
            sides[index].addActionListener(e->{try{if(cell!=null)ui.change(setCell(ui.document,cell,mask()));}catch(IllegalArgumentException|IllegalStateException invalid){ui.error(invalid);}});arrows.add(sides[index]);
        }
        panel.add(arrows,BorderLayout.NORTH);var text=new JTextArea("Selecione Colisao na barra. Clique nas setas de cada celula para alternar os lados. Centro: aplicar a combinacao do painel. Botao direito: limpar. Quatro lados: volume solido. Cima livre: entrada por cima, respeitando o tamanho do personagem. Arte e fisica sao independentes.");text.setEditable(false);text.setLineWrap(true);text.setWrapStyleWord(true);panel.add(text);
        ui.inspectorTabs.addTab("Colisao",panel);ui.inspectorTabs.addChangeListener(e->{if(ui.inspectorTabs.getSelectedComponent()==panel&&ui.toolSelector!=null)ui.toolSelector.setSelectedItem("Colisao");});
    }
    int mask(){int value=0;for(int i=0;i<4;i++)if(sides[i].isSelected())value|=BITS[i];return value;}
    /** Seleciona a celula e alterna a borda mais proxima; centro aplica a selecao do painel. */
    void click(Point point,boolean clear){
        int grid=ui.document.grid();if(point.x<0||point.y<0||point.x>=ui.document.width()||point.y>=ui.document.height())return;
        cell=new Point(point.x/grid*grid,point.y/grid*grid);int x=point.x-cell.x,y=point.y-cell.y;
        String id=cellId(cell);int current=ui.document.hitboxes().stream().filter(h->h.id().equals(id)).mapToInt(WorldDocument.Hitbox::edges).findFirst().orElse(0);
        int next=mask();int edge=Math.min(Math.min(x,grid-1-x),Math.min(y,grid-1-y));
        if(clear)next=0;else if(edge<grid*.3){int bit=edge==y?WorldDocument.Hitbox.UP:edge==grid-1-y?WorldDocument.Hitbox.DOWN:edge==x?WorldDocument.Hitbox.LEFT:WorldDocument.Hitbox.RIGHT;next=current^bit;}
        ui.change(setCell(ui.document,cell,next));for(int i=0;i<4;i++)sides[i].setSelected((next&BITS[i])!=0);
    }
    static String cellId(Point p){return "cell_collision_"+p.x+"_"+p.y;}
    /** Substitui somente a fisica da celula, permitindo historico e remocao sem tocar nos tiles. */
    static WorldDocument setCell(WorldDocument d,Point point,int mask){
        int grid=d.grid(),x=Math.floorDiv(point.x,grid)*grid,y=Math.floorDiv(point.y,grid)*grid;
        var p=new Point(x,y);String id=cellId(p);var existing=d.hitboxes().stream().filter(h->h.id().equals(id)).findFirst();
        String layer=existing.map(WorldDocument.Hitbox::layer).orElseGet(()->d.layers().stream().filter(l->l.role()==WorldDocument.Role.COLLISION).findFirst().orElseThrow(()->new IllegalArgumentException("Camada COLLISION ausente")).id());
        EditorOperations.editable(d,layer);var boxes=new ArrayList<>(d.hitboxes());boxes.removeIf(h->h.id().equals(id));
        if(mask!=0)boxes.add(new WorldDocument.Hitbox(id,x,y,Math.min(grid,d.width()-x),Math.min(grid,d.height()-y),true,layer,mask));
        return d.withHitboxes(boxes);
    }
    /** Setas sao ferramentas de edicao; nunca se tornam sprites do mapa. */
    void overlay(Graphics2D g){
        if(!directional)return;var d=ui.document;int grid=d.grid();if(grid*Math.hypot(g.getTransform().getScaleX(),g.getTransform().getShearY())<12)return;var clip=g.getClipBounds();if(clip==null)clip=new Rectangle(0,0,d.width(),d.height());
        var cells=new HashMap<String,Integer>();for(var h:d.hitboxes())cells.put(h.id(),h.solid()?h.edges():0);
        for(int y=Math.max(0,clip.y/grid*grid);y<Math.min(d.height(),clip.y+clip.height);y+=grid)for(int x=Math.max(0,clip.x/grid*grid);x<Math.min(d.width(),clip.x+clip.width);x+=grid){
            int value=cells.getOrDefault(cellId(new Point(x,y)),0);double[] dx={.5,.5,.16,.84},dy={.16,.84,.5,.5};
            for(int i=0;i<4;i++)CollisionArrow.draw(g,x+dx[i]*grid,y+dy[i]*grid,Math.max(5,grid*.30),BITS[i],(value&BITS[i])!=0);
            if(cell!=null&&cell.x==x&&cell.y==y){g.setColor(Color.YELLOW);g.drawRect(x,y,grid,grid);}
        }
    }
    CollisionTools(WorldStudio ui){this.ui=ui;installPanel();ui.canvas.editOverlay=this::overlay;ui.action("Mapa","Nova colisao",()->edit(null));ui.action("Mapa","Editar colisao",()->{var selected=ui.document.hitboxes().stream().filter(h->ui.canvas.selected.contains(h.id())).findFirst().orElse(null);if(selected==null){String id=(String)JOptionPane.showInputDialog(ui,"Colisao","Editar",JOptionPane.PLAIN_MESSAGE,null,ui.document.hitboxes().stream().map(WorldDocument.Hitbox::id).toArray(String[]::new),null);if(id==null)return;selected=ui.document.hitboxes().stream().filter(h->h.id().equals(id)).findFirst().orElseThrow();}edit(selected);});
        ui.action("Ver","Colisoes",()->{ui.canvas.collisions=!ui.canvas.collisions;ui.canvas.repaint();});ui.action("Ver","Raio do jogador",()->{ui.canvas.playerPreview=!ui.canvas.playerPreview;ui.canvas.repaint();});ui.action("Editar","Alternar snap",()->{ui.snap=!ui.snap;ui.status.setText("Snap: "+ui.snap);});
    }
    void drag(Point a,Point b){var r=dragBounds(ui.document,a,b,ui.snap);add(new WorldDocument.Hitbox(EditorOperations.id(),r.x,r.y,r.width,r.height,true,layer()));}
    /** Mantem o gesto dentro do mapa, inclusive junto as bordas e em sentido inverso. */
    static Rectangle dragBounds(WorldDocument d,Point a,Point b,boolean snap){
        int x=Math.max(0,Math.min(d.width()-1,Math.min(a.x,b.x))),y=Math.max(0,Math.min(d.height()-1,Math.min(a.y,b.y)));
        int right=Math.max(x+1,Math.min(d.width(),Math.max(a.x,b.x))),bottom=Math.max(y+1,Math.min(d.height(),Math.max(a.y,b.y)));
        if(snap){x=x/d.grid()*d.grid();y=y/d.grid()*d.grid();right=Math.min(d.width(),Math.max(x+d.grid(),(int)Math.ceil((double)right/d.grid())*d.grid()));bottom=Math.min(d.height(),Math.max(y+d.grid(),(int)Math.ceil((double)bottom/d.grid())*d.grid()));}
        return new Rectangle(x,y,right-x,bottom-y);
    }
    String layer(){return ui.document.layers().stream().filter(l->l.role()==WorldDocument.Role.COLLISION).findFirst().orElseThrow(()->new IllegalArgumentException("Crie camada COLLISION")).id();}
    void add(WorldDocument.Hitbox hitbox){EditorOperations.editable(ui.document,hitbox.layer());var list=new ArrayList<>(ui.document.hitboxes());list.removeIf(h->h.id().equals(hitbox.id()));list.add(hitbox);ui.change(ui.document.withHitboxes(list));ui.status.setText("Colisao atualizada | Sobreposicoes: "+StudioGeometry.overlaps(ui.document).size());}
    void edit(WorldDocument.Hitbox old){int[] defaults=old==null?new int[]{Math.max(0,ui.lastCursor.x),Math.max(0,ui.lastCursor.y),32,32}:new int[]{old.x(),old.y(),old.width(),old.height()};JPanel panel=new JPanel(new GridLayout(0,2));JSpinner[] fields=new JSpinner[4];String[] names={"X","Y","Largura","Altura"};for(int i=0;i<4;i++){panel.add(new JLabel(names[i]));fields[i]=new JSpinner(new SpinnerNumberModel(defaults[i],0,8192,1));panel.add(fields[i]);}JCheckBox solid=new JCheckBox("Solido",old==null||old.solid());panel.add(solid);if(JOptionPane.showConfirmDialog(ui,panel,"Colisao explicita",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;int[] n=Arrays.stream(fields).mapToInt(f->(Integer)f.getValue()).toArray();if(ui.snap){n[0]=n[0]/ui.document.grid()*ui.document.grid();n[1]=n[1]/ui.document.grid()*ui.document.grid();}add(new WorldDocument.Hitbox(old==null?EditorOperations.id():old.id(),n[0],n[1],n[2],n[3],solid.isSelected(),old==null?layer():old.layer(),old==null?WorldDocument.Hitbox.ALL:old.edges()));}
}
