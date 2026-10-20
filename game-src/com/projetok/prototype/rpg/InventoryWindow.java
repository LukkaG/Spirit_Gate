package com.projetok.prototype.rpg;
import com.projetok.rpg.*;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import javax.swing.Timer;
/** Inventario real: UI apenas consulta snapshot e publica comandos. */
final class InventoryWindow extends JDialog {
    private final RpgRuntime runtime;
    private final ItemCatalog items=new ItemCatalog(RpgContent.builtin());
    private final DefaultListModel<String> slots=new DefaultListModel<>(),equipment=new DefaultListModel<>();
    private final JList<String> list=new JList<>(slots),equipped=new JList<>(equipment);
    private final JTextArea details=new JTextArea(5,45);
    private final JLabel result=new JLabel("Selecione um item");
    private final String[] slotNames={"weapon","torso","legs","accessory"};
    private Map<String,Integer> lastInventory=Map.of();private Map<String,String> lastEquipment=Map.of();
    private final Timer timer;
    InventoryWindow(JFrame owner,RpgRuntime runtime) {
        super(owner,"Inventario e equipamento",false);this.runtime=runtime;setDefaultCloseOperation(HIDE_ON_CLOSE);
        JPanel root=new JPanel(new BorderLayout(8,8));root.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));JPanel columns=new JPanel(new GridLayout(1,2,8,8));columns.add(new JScrollPane(list));columns.add(new JScrollPane(equipped));root.add(columns,BorderLayout.CENTER);
        details.setEditable(false);details.setLineWrap(true);details.setWrapStyleWord(true);JPanel bottom=new JPanel(new BorderLayout());bottom.add(new JScrollPane(details),BorderLayout.NORTH);
        JPanel buttons=new JPanel(new GridLayout(0,3,6,6));bottom.add(buttons,BorderLayout.CENTER);bottom.add(result,BorderLayout.SOUTH);root.add(bottom,BorderLayout.SOUTH);
        JButton use=new JButton("Usar"),equip=new JButton("Equipar"),remove=new JButton("Desequipar"),move=new JButton("Mover/separar"),pending=new JButton("Recolher pendentes");
        JSpinner target=new JSpinner(new SpinnerNumberModel(0,0,19,1)),quantity=new JSpinner(new SpinnerNumberModel(1,1,99,1));
        use.addActionListener(e->selected(id->w->w.use(id)));equip.addActionListener(e->selected(id->w->w.equip(id)));remove.addActionListener(e->{int index=equipped.getSelectedIndex();if(index>=0)perform(w->w.unequip(slotNames[index]));});
        move.addActionListener(e->{int from=list.getSelectedIndex(),to=(Integer)target.getValue(),amount=(Integer)quantity.getValue();if(from>=0)perform(w->w.inventory().move(from,to,amount));});
        pending.addActionListener(e->runtime.command(RpgWorld::collectLoot));
        for(JComponent c:new JComponent[]{use,equip,remove,new JLabel("Destino (0-19)"),target,quantity,move,pending})buttons.add(c);
        list.addListSelectionListener(e->showDetails());setContentPane(root);setSize(720,540);setLocationRelativeTo(owner);
        timer=new Timer(100,e->refresh());timer.start();refresh();
        owner.addWindowListener(new java.awt.event.WindowAdapter(){public void windowClosed(java.awt.event.WindowEvent e){timer.stop();dispose();}});
    }
    private String selectedId(){int index=list.getSelectedIndex();if(index<0)return null;String prefix=index+":";return runtime.snapshot().state().inventory().keySet().stream().filter(k->k.startsWith(prefix)).map(k->k.substring(prefix.length())).findFirst().orElse(null);}
    private void selected(java.util.function.Function<String,java.util.function.Function<RpgWorld,Boolean>> action){String id=selectedId();if(id!=null)perform(action.apply(id));}
    private void perform(java.util.function.Function<RpgWorld,Boolean> action){runtime.action(action).whenComplete((ok,error)->SwingUtilities.invokeLater(()->{result.setText(error!=null?error.getMessage():Boolean.TRUE.equals(ok)?"Operacao concluida":"Operacao recusada; estado preservado");refresh();}));}
    private void showDetails(){String id=selectedId();if(id==null){details.setText("Slot vazio");return;}var item=items.get(id);details.setText(item.name()+" | "+item.rarity()+" | "+item.category()+"\n"+item.description()+"\nValor: "+item.price()+" | Pilha maxima: "+item.maxStack()+" | Nivel minimo: "+item.minimumLevel());}
    private void refresh(){var v=runtime.snapshot();var inventory=v.state().inventory();var eq=v.state().equipment();
        if(!inventory.equals(lastInventory)||slots.size()!=20){int selection=list.getSelectedIndex();slots.clear();for(int i=0;i<20;i++){final String prefix=i+":";var entry=inventory.entrySet().stream().filter(e->e.getKey().startsWith(prefix)).findFirst();slots.addElement(i+": "+entry.map(e->items.get(e.getKey().substring(prefix.length())).name()+" x"+e.getValue()).orElse("[vazio]"));}lastInventory=inventory;list.setSelectedIndex(selection);showDetails();}
        if(!eq.equals(lastEquipment)||equipment.size()!=4){int selection=equipped.getSelectedIndex();equipment.clear();for(String slot:slotNames)equipment.addElement(slot+": "+(eq.containsKey(slot)?items.get(eq.get(slot)).name():"[vazio]"));lastEquipment=eq;equipped.setSelectedIndex(selection);}
    }
}
