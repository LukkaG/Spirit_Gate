package com.projetok.multiplayer;

import com.projetok.rpg.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/** Slots compactos; detalhes e ações aparecem no painel contextual do item. */
public final class NetworkInventoryPanel extends JPanel {
    private record Entry(String key,String id,ItemCatalog.Item item,int quantity){@Override public String toString(){return item.name()+" x"+quantity;}}
    enum Category { ARMOR,WEAPON,DROP,ALL }
    private Category category=Category.ALL;
    private boolean organized,rebuilding;
    private final java.util.Map<Category,JButton> categories=new java.util.EnumMap<>(Category.class);
    private final DefaultListModel<Entry> model=new DefaultListModel<>();
    private final JList<Entry> list=new JList<>(model);
    private final ItemCatalog items=new ItemCatalog(RpgContent.builtin());
    private final JTextField search=new JTextField();
    private final JLabel count=new JLabel(),itemName=new JLabel();
    private final JTextArea details=new JTextArea(4,20);
    private final JPanel popup;
    private final JLayeredPane board;
    private final JButton equip,use,close;
    private GameplayProtocol.Sheet previous;
    private Runnable openEquipment=()->{};
    public void equipment(Runnable open){openEquipment=open;}
    public NetworkInventoryPanel(LocalClient client){
        super(new BorderLayout(6,8));setPreferredSize(new Dimension(520,350));
        search.setName("inventorySearch");search.getAccessibleContext().setAccessibleName("Buscar item no inventário");var filter=new JPanel(new BorderLayout(6,0));filter.add(new JLabel("Buscar"),BorderLayout.WEST);filter.add(search);filter.add(count,BorderLayout.EAST);add(filter,BorderLayout.NORTH);
        list.setName("inventoryGrid");list.setLayoutOrientation(JList.HORIZONTAL_WRAP);list.setVisibleRowCount(-1);list.setFixedCellWidth(56);list.setFixedCellHeight(56);list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);list.setOpaque(false);
        list.getAccessibleContext().setAccessibleName("Itens: ícone e quantidade. Clique ou pressione ENTER para abrir os detalhes.");list.putClientProperty("spirit.customRenderer",Boolean.TRUE);list.setCellRenderer(new SlotRenderer());
        var scroll=new JScrollPane(list);scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        popup=new JPanel(new BorderLayout(8,8)){@Override protected void paintComponent(Graphics original){var g=GameHudStyle.graphics(original);try{GameHudStyle.panel(g,getWidth(),getHeight(),10);}finally{g.dispose();}}};popup.setName("inventoryItemPopup");popup.setOpaque(false);popup.setBorder(BorderFactory.createEmptyBorder(10,12,10,12));popup.setFocusCycleRoot(true);popup.setFocusTraversalPolicy(new LayoutFocusTraversalPolicy());
        close=new JButton("×");close.setName("inventoryItemClose");close.putClientProperty("spirit.compact",Boolean.TRUE);close.getAccessibleContext().setAccessibleName("Fechar detalhes do item");close.addActionListener(e->hideItem());
        var heading=new JPanel(new BorderLayout(6,0));heading.add(itemName);heading.add(close,BorderLayout.EAST);popup.add(heading,BorderLayout.NORTH);
        details.setName("inventoryDetails");details.setEditable(false);details.setLineWrap(true);details.setWrapStyleWord(true);details.setFocusable(false);var descriptionScroll=new JScrollPane(details);descriptionScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);popup.add(descriptionScroll);
        equip=GameHudStyle.iconButton("inventoryEquip","Equipar item",GameHudStyle.Glyph.SWORD,()->{var entry=list.getSelectedValue();if(entry!=null)client.gameplay(GameplayProtocol.Operation.EQUIP,entry.id());});
        use=GameHudStyle.iconButton("inventoryUse","Consumir item",GameHudStyle.Glyph.POTION,()->{var entry=list.getSelectedValue();if(entry!=null)client.gameplay(GameplayProtocol.Operation.USE,entry.id());});
        var actions=new JPanel(new FlowLayout(FlowLayout.LEFT,6,0));actions.add(equip);actions.add(use);popup.add(actions,BorderLayout.SOUTH);popup.setVisible(false);
        board=new JLayeredPane(){@Override public void doLayout(){scroll.setBounds(0,0,getWidth(),getHeight());positionPopup();}};board.setName("inventoryBoard");board.add(scroll,JLayeredPane.DEFAULT_LAYER);board.add(popup,JLayeredPane.POPUP_LAYER);add(board);
        list.addMouseListener(new MouseAdapter(){@Override public void mouseClicked(MouseEvent e){int index=list.locationToIndex(e.getPoint());if(index>=0&&list.getCellBounds(index,index).contains(e.getPoint())){list.setSelectedIndex(index);showItem();}else hideItem();}});
        list.getInputMap().put(KeyStroke.getKeyStroke("ENTER"),"item-details");list.getActionMap().put("item-details",new AbstractAction(){public void actionPerformed(ActionEvent e){showItem();}});
        popup.registerKeyboardAction(e->hideItem(),KeyStroke.getKeyStroke("ESCAPE"),WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        list.addListSelectionListener(e->{if(!e.getValueIsAdjusting()&&!rebuilding){selection();if(popup.isVisible())positionPopup();}});
        scroll.getViewport().addChangeListener(e->{if(popup.isVisible())positionPopup();});
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){public void insertUpdate(javax.swing.event.DocumentEvent e){rebuild();}public void removeUpdate(javax.swing.event.DocumentEvent e){rebuild();}public void changedUpdate(javax.swing.event.DocumentEvent e){rebuild();}});
        var footer=new JPanel(new FlowLayout(FlowLayout.LEFT,4,0));
        addCategory(footer,Category.ARMOR,"Armadura",GameHudStyle.Glyph.ARMOR);addCategory(footer,Category.WEAPON,"Armas",GameHudStyle.Glyph.SWORD);addCategory(footer,Category.DROP,"Itens de drop",GameHudStyle.Glyph.CRYSTAL);addCategory(footer,Category.ALL,"Todos",GameHudStyle.Glyph.BAG);
        footer.add(GameHudStyle.iconButton("inventoryOrganize","Organizar inventário: ordenar por categoria e nome",GameHudStyle.Glyph.SORT,()->{organized=true;rebuild();}));
        footer.add(GameHudStyle.iconButton("inventoryCollect","Recolher itens pendentes",GameHudStyle.Glyph.COLLECT,()->client.gameplay(GameplayProtocol.Operation.COLLECT,"pending")));
        footer.add(GameHudStyle.iconButton("inventoryEquipment","Ver equipamentos do personagem",GameHudStyle.Glyph.PROFILE,()->openEquipment.run()));add(footer,BorderLayout.SOUTH);selection();
    }
    private void addCategory(JPanel parent,Category value,String label,GameHudStyle.Glyph glyph){var button=GameHudStyle.iconButton("inventoryCategory"+value,label,glyph,()->{category=value;rebuild();});categories.put(value,button);parent.add(button);}
    static boolean matches(ItemCatalog.Item item,Category category){return switch(category){case ALL->true;case WEAPON->item.slot().equals("weapon");case ARMOR->item.category()==ItemCatalog.Category.EQUIPMENT&&!item.slot().equals("weapon");case DROP->item.category()!=ItemCatalog.Category.EQUIPMENT;};}
    public void update(GameplayProtocol.Sheet sheet){if(previous!=null&&sheet!=null&&previous.inventory().equals(sheet.inventory())&&previous.equipment().equals(sheet.equipment()))return;previous=sheet;rebuild();}
    private void rebuild(){String selected=list.getSelectedValue()==null?null:list.getSelectedValue().key();boolean opened=popup.isVisible();rebuilding=true;try{model.clear();
        categories.forEach((value,button)->{button.putClientProperty("spirit.active",value==category);button.getAccessibleContext().setAccessibleDescription(value==category?"Categoria ativa":"Categoria inativa");button.repaint();});
        if(previous!=null){var entries=new java.util.ArrayList<Entry>();previous.inventory().forEach((key,quantity)->{String id=key.substring(key.indexOf(':')+1);var item=items.get(id);if(matches(item,category)&&item.name().toLowerCase(java.util.Locale.ROOT).contains(search.getText().strip().toLowerCase(java.util.Locale.ROOT)))entries.add(new Entry(key,id,item,quantity));});
            entries.sort(organized?java.util.Comparator.comparing((Entry e)->e.item().category()).thenComparing(e->e.item().name()).thenComparing(Entry::key):java.util.Comparator.comparing(Entry::key));entries.forEach(model::addElement);}
        for(int i=0;i<model.size();i++)if(model.get(i).key().equals(selected)){list.setSelectedIndex(i);break;}
        boolean kept=list.getSelectedIndex()>=0;if(!kept&&!model.isEmpty())list.setSelectedIndex(0);popup.setVisible(opened&&kept);
    }finally{rebuilding=false;}count.setText(previous==null?"Sincronizando":model.size()+" tipos");selection();positionPopup();}
    private void selection(){var entry=list.getSelectedValue();equip.setEnabled(entry!=null&&entry.item().category()==ItemCatalog.Category.EQUIPMENT);use.setEnabled(entry!=null&&entry.item().category()==ItemCatalog.Category.CONSUMABLE);
        itemName.setText(entry==null?"Item":entry.item().name());itemName.setToolTipText(itemName.getText());itemName.setIcon(entry==null?null:GameHudStyle.icon(GameHudStyle.itemGlyph(entry.item()),22));
        details.setText(entry==null?"Nenhum item neste filtro.":entry.item().description()+"\n\nQuantidade: "+entry.quantity()+" · "+rarity(entry.item().rarity())+"\nNível mínimo: "+entry.item().minimumLevel());details.setCaretPosition(0);if(entry==null)popup.setVisible(false);
    }
    private static String rarity(ItemCatalog.Rarity rarity){return switch(rarity){case COMMON->"Comum";case UNCOMMON->"Incomum";case RARE->"Raro";};}
    public void hideItem(){popup.setVisible(false);list.requestFocusInWindow();}
    public boolean dismissItem(){if(!popup.isVisible())return false;hideItem();return true;}
    private void showItem(){if(list.getSelectedValue()==null)return;selection();popup.setVisible(true);positionPopup();close.requestFocusInWindow();board.repaint();}
    private void positionPopup(){if(!popup.isVisible()||board.getWidth()<9||board.getHeight()<9)return;int w=Math.min(280,board.getWidth()-8),h=Math.min(228,board.getHeight()-8);var cell=list.getCellBounds(list.getSelectedIndex(),list.getSelectedIndex());if(cell==null){popup.setVisible(false);return;}
        var bounds=SwingUtilities.convertRectangle(list,cell,board);int x=bounds.x+bounds.width+8;if(x+w>board.getWidth()-4)x=Math.max(4,bounds.x-w-8);int y=Math.max(4,Math.min(bounds.y,board.getHeight()-h-4));var next=new Rectangle(Math.max(4,Math.min(x,board.getWidth()-w-4)),y,w,h);if(!popup.getBounds().equals(next)){popup.setBounds(next);popup.revalidate();}}
    private static final class SlotRenderer extends JPanel implements ListCellRenderer<Entry>{
        private Entry entry;private boolean selected,focus;
        SlotRenderer(){setOpaque(false);}
        @Override public Component getListCellRendererComponent(JList<? extends Entry> l,Entry value,int index,boolean selected,boolean focus){entry=value;this.selected=selected;this.focus=focus;setToolTipText("Clique para detalhes");getAccessibleContext().setAccessibleName(value.toString());return this;}
        @Override protected void paintComponent(Graphics original){var g=GameHudStyle.graphics(original);try{int w=getWidth(),h=getHeight();g.setColor(new Color(21,52,70,80));g.fillRoundRect(3,3,w-7,h-7,7,7);g.setColor(selected?GameHudStyle.CYAN:new Color(54,105,127,100));g.drawRoundRect(3,3,w-7,h-7,7,7);if(focus){g.setColor(Color.WHITE);g.drawRoundRect(5,5,w-11,h-11,5,5);}g.setColor(GameHudStyle.CYAN);GameHudStyle.glyph(g,GameHudStyle.itemGlyph(entry.item()),(w-28)/2,9,28);g.setFont(new Font("SansSerif",Font.BOLD,10));String qty="×"+entry.quantity();int x=w-7-g.getFontMetrics().stringWidth(qty);g.setColor(new Color(4,17,29,230));g.fillRoundRect(x-2,h-19,w-x-3,14,3,3);g.setColor(GameHudStyle.TEXT);g.drawString(qty,x,h-8);}finally{g.dispose();}}
    }
}
