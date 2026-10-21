package com.projetok.prototype.rpg;
import com.projetok.rpg.*;
import javax.swing.*;
import java.awt.*;
import java.util.Map;

/** Comércio publica uma única intenção por operação; o mundo confirma a transação. */
final class TradeWindow extends JDialog {
    private final RpgRuntime runtime;
    private final ItemCatalog catalog=new ItemCatalog(RpgContent.builtin());
    private final DefaultListModel<String> goods=new DefaultListModel<>(),owned=new DefaultListModel<>();
    private final JList<String> vendor=new JList<>(goods),player=new JList<>(owned);
    private final JLabel balance=new JLabel(),result=new JLabel("Selecione uma mercadoria ou item do jogador");
    private final JTextArea details=new JTextArea(5,40);
    private final JSpinner quantity=new JSpinner(new SpinnerNumberModel(1,1,99,1));
    private final JButton buy=new CharacterSelection.SpiritButton("Comprar",true),sell=new CharacterSelection.SpiritButton("Vender",false);
    private boolean pending;
    private Map<String,Integer> last=Map.of();

    TradeWindow(JFrame owner,RpgRuntime runtime){
        super(owner,"Comércio",false);this.runtime=runtime;setDefaultCloseOperation(HIDE_ON_CLOSE);
        JPanel root=new JPanel(new BorderLayout(8,8));root.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));root.add(balance,BorderLayout.NORTH);
        RpgWorld.merchantItems().stream().sorted().forEach(goods::addElement);
        for(JList<String> list:new JList[]{vendor,player}){
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.setCellRenderer((source,id,index,selected,focus)->{var item=catalog.get(id);JLabel line=new JLabel(item.name()+" | "+(source==vendor?"Compra "+RpgWorld.tradePrice(item,true):"x"+count(id)+" | Venda "+RpgWorld.tradePrice(item,false))+" moedas");line.setOpaque(true);line.setBackground(selected?new Color(18,66,85):SpiritTheme.BACKGROUND);line.setForeground(Color.WHITE);line.setBorder(BorderFactory.createLineBorder(focus?Color.WHITE:SpiritTheme.ACCENT));return line;});
            list.addListSelectionListener(e->{String id=list.getSelectedValue();if(id!=null){details.setText(InventoryWindow.describe(catalog.get(id)));details.setCaretPosition(0);}});
        }
        vendor.getAccessibleContext().setAccessibleName("Mercadorias do vendedor");player.getAccessibleContext().setAccessibleName("Itens vendáveis do jogador");
        JPanel columns=new JPanel(new GridLayout(1,2,8,8));columns.add(column("Vendedor / mercadorias",vendor));columns.add(column("Jogador / inventário",player));root.add(columns);
        JPanel bottom=new JPanel(new BorderLayout(4,4));details.setEditable(false);details.setLineWrap(true);details.setWrapStyleWord(true);bottom.add(new JScrollPane(details),BorderLayout.NORTH);
        JPanel actions=new JPanel(new FlowLayout());JButton close=new CharacterSelection.SpiritButton("Sair",false);close.addActionListener(e->setVisible(false));
        actions.add(new JLabel("Quantidade"));actions.add(quantity);actions.add(buy);actions.add(sell);actions.add(close);bottom.add(actions);bottom.add(result,BorderLayout.SOUTH);root.add(bottom,BorderLayout.SOUTH);
        buy.addActionListener(e->transact(true));sell.addActionListener(e->transact(false));SpiritTheme.dialog(this,root,buy);
        setSize(780,560);setLocationRelativeTo(owner);refresh();
        Timer timer=new Timer(150,e->{if(isVisible())refresh();});timer.start();owner.addWindowListener(new java.awt.event.WindowAdapter(){public void windowClosed(java.awt.event.WindowEvent e){timer.stop();dispose();}});
    }
    private JPanel column(String title,JList<String> list){JPanel panel=new JPanel(new BorderLayout(4,4));panel.add(new JLabel(title),BorderLayout.NORTH);panel.add(new JScrollPane(list));return panel;}
    private int count(String id){return runtime.snapshot().state().inventory().entrySet().stream().filter(e->e.getKey().substring(e.getKey().indexOf(':')+1).equals(id)).mapToInt(Map.Entry::getValue).sum();}
    private void refresh(){var view=runtime.snapshot();var inventory=view.state().inventory();balance.setText("Saldo: "+view.state().coins()+" moedas | "+InventoryWindow.inventoryStatus(inventory));
        if(!last.equals(inventory)){String selected=player.getSelectedValue();owned.clear();inventory.keySet().stream().map(k->k.substring(k.indexOf(':')+1)).distinct().sorted().filter(id->{var item=catalog.get(id);return item.price()>0&&item.category()!=ItemCatalog.Category.QUEST;}).forEach(owned::addElement);last=inventory;player.setSelectedValue(selected,true);}
        boolean active=view.activeNpc().equals("trader")&&view.state().hp()>0;buy.setEnabled(active&&!pending);sell.setEnabled(active&&!pending);quantity.setEnabled(!pending);
        if(!active)result.setText("Interaja com o comerciante para negociar.");
    }
    private void transact(boolean buying){
        if(pending)return;String id=(buying?vendor:player).getSelectedValue();if(id==null){result.setText("Selecione um item");return;}
        try{quantity.commitEdit();}catch(java.text.ParseException error){result.setText("Quantidade inválida");return;}
        int amount=(Integer)quantity.getValue();if(amount<1||amount>99){result.setText("Quantidade inválida");return;}
        pending=true;refresh();runtime.action(w->w.trade(id,amount,buying)).whenComplete((ok,error)->SwingUtilities.invokeLater(()->{
            pending=false;refresh();result.setText(error!=null?"Transação não confirmada":Boolean.TRUE.equals(ok)?"Transação concluída":buying&&runtime.snapshot().state().coins()<RpgWorld.tradePrice(catalog.get(id),true)*amount?"Saldo insuficiente":"Operação recusada: verifique capacidade, quantidade e distância");
        }));
    }
}
