package com.projetok.multiplayer;

import com.projetok.rpg.*;
import java.awt.*;
import java.util.*;
import javax.swing.*;

/** Ficha e equipamentos confirmados; habilidades ficam em um painel próprio. */
public final class NetworkCharacterPanel extends JPanel {
    private final JLabel resources=new JLabel("Sincronizando personagem"),identity=new JLabel();
    private final Map<String,JLabel> equipment=new LinkedHashMap<>();
    private final Map<String,JButton> remove=new LinkedHashMap<>();
    private final ItemCatalog items=new ItemCatalog(RpgContent.builtin());
    private final JPanel attributes=new JPanel(new BorderLayout(6,6));
    private final LocalClient client;
    private final JLabel points=new JLabel("Consultando pontos…");
    private final JLabel combatEffects=new JLabel();
    private final Map<String,JLabel> values=new LinkedHashMap<>();
    private final Map<String,JButton> spend=new LinkedHashMap<>();
    private GameplayProtocol.Attributes received;
    private boolean pending;
    private String pendingAttribute;
    private GameplayProtocol.Energy energyProfile;
    private boolean energySynchronizing;
    public void energy(GameplayProtocol.Energy state,boolean synchronizing){energySynchronizing=synchronizing;if(state!=energyProfile){energyProfile=state;if(state!=null&&"energy".equals(pendingAttribute)&&!energySynchronizing){pending=false;pendingAttribute=null;}}values.get("energy").setText(state==null?"Energy (ENE): —":"Energy (ENE): "+state.trained());}
    public NetworkCharacterPanel(LocalClient client,NetworkProgressLabel progression){super(new BorderLayout(8,10));this.client=client;setName("characterSheet");
        var header=new JPanel(new GridLayout(0,1,0,6));var character=client.characterSelection();identity.setText(character==null?"Personagem":character.name()+" · "+CharacterRaces.get(character.race()).name());identity.setForeground(GameHudStyle.CYAN);header.add(identity);header.add(progression);header.add(resources);add(header,BorderLayout.NORTH);
        var form=new JPanel();form.setLayout(new BoxLayout(form,BoxLayout.Y_AXIS));form.add(new JLabel("EQUIPAMENTOS"));form.add(Box.createVerticalStrut(8));var slots=new JPanel(new GridLayout(0,2,8,8));
        for(var slot:java.util.List.of(Map.entry("weapon","Arma"),Map.entry("torso","Torso"),Map.entry("legs","Pernas"),Map.entry("accessory","Acessório"))){
            var row=new JPanel(new BorderLayout(6,4));row.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));var title=new JLabel(slot.getValue());title.setForeground(GameHudStyle.MUTED);row.add(title,BorderLayout.NORTH);var item=new JLabel("Vazio");item.setName("equipment-"+slot.getKey());equipment.put(slot.getKey(),item);row.add(item);
            var clear=new JButton("×");clear.setName("unequip-"+slot.getKey());clear.putClientProperty("spirit.compact",Boolean.TRUE);clear.setToolTipText("Desequipar "+slot.getValue().toLowerCase(Locale.ROOT));clear.getAccessibleContext().setAccessibleName(clear.getToolTipText());clear.addActionListener(e->client.gameplay(GameplayProtocol.Operation.UNEQUIP,slot.getKey()));clear.setEnabled(false);remove.put(slot.getKey(),clear);row.add(clear,BorderLayout.EAST);slots.add(row);
        }
        form.add(slots);form.add(Box.createVerticalStrut(16));attributes.setName("characterAttributes");points.setName("attributePoints");attributes.add(points,BorderLayout.NORTH);var rows=new JPanel(new GridLayout(0,1,4,8));
        String[][] definitions={{"strength","Força (STR)","+1 dano básico/desarmado a cada 2 pontos"},{"intelligence","Inteligência (INT)","+1 dano de domínio a cada 2 pontos"},{"agility","Agilidade (AGI)","+1% velocidade básica · +0,5 p.p. esquiva por ponto"},{"energy","Energy (ENE)","+2 energia máxima · +1% regeneração por ponto"}};
        for(var definition:definitions){var row=new JPanel(new BorderLayout(8,2));var labels=new JPanel(new GridLayout(0,1));var value=new JLabel(definition[1]+": —");value.setName("attribute-"+definition[0]);values.put(definition[0],value);labels.add(value);var effect=new JLabel(definition[2]);effect.setForeground(GameHudStyle.MUTED);labels.add(effect);row.add(labels);var add=new JButton("+");add.putClientProperty("spirit.compact",Boolean.TRUE);add.setName("allocate-"+definition[0]);add.getAccessibleContext().setAccessibleName("Adicionar 1 ponto em "+definition[1]);add.setToolTipText(definition[2]);add.setEnabled(false);add.addActionListener(e->{if(!pending&&client.allocateAttribute(definition[0])){pending=true;pendingAttribute=definition[0];spend.values().forEach(button->button.setEnabled(false));points.setText("Aguardando confirmação…");}});spend.put(definition[0],add);var buttonBox=new JPanel(new GridBagLayout());add.setPreferredSize(new Dimension(28,28));buttonBox.add(add);row.add(buttonBox,BorderLayout.EAST);rows.add(row);}
        combatEffects.setName("attributeCombatEffects");combatEffects.setToolTipText("Velocidade: máximo +100%. Esquiva por AGI: máximo 25%, apenas contra hits básicos. Regeneração de energia: máximo +100%.");attributes.add(rows);attributes.add(combatEffects,BorderLayout.SOUTH);form.add(new JLabel("ATRIBUTOS TREINADOS"));form.add(Box.createVerticalStrut(6));form.add(attributes);form.add(Box.createVerticalGlue());for(var component:form.getComponents())if(component instanceof JComponent c)c.setAlignmentX(LEFT_ALIGNMENT);
        var scroll=new JScrollPane(form);scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);add(scroll);
    }
    public void attributes(GameplayProtocol.Attributes state){if(state!=received){received=state;if(!"energy".equals(pendingAttribute)||!energySynchronizing){pending=false;pendingAttribute=null;}}boolean alive=client.connected()&&client.snapshot()!=null&&client.snapshot().actors().stream().anyMatch(a->a.id()==client.id()&&a.hp()>0);
        if(state==null){points.setText("Aguardando atributos do servidor");combatEffects.setText("");spend.values().forEach(button->button.setEnabled(false));return;}
        double speed=(com.projetok.domain.combat.AttributeCombatRules.basicSpeedMultiplier(state.agility())-1)*100,dodge=com.projetok.domain.combat.AttributeCombatRules.basicDodgeChance(state.agility())*100;
        combatEffects.setText(String.format(java.util.Locale.forLanguageTag("pt-BR"),"<html>Bônus básico/desarmado: +%d · Domínio: +%d<br>Velocidade básica: +%.0f%% · Esquiva básica: %.1f%%</html>",state.strength()/2,state.intelligence()/2,speed,dodge));
        if(energyProfile!=null)combatEffects.setText(combatEffects.getText().replace("</html>",String.format(java.util.Locale.forLanguageTag("pt-BR"),"<br>Energia máxima: %d · Regeneração: %.2f/s</html>",energyProfile.maximum()/10,com.projetok.domain.combat.EnergyRules.regenerationPerSecond(energyProfile.trained()))));
        if(!pending)points.setText("Pontos disponíveis: "+state.points());values.get("strength").setText("Força (STR): "+state.strength());values.get("intelligence").setText("Inteligência (INT): "+state.intelligence());values.get("agility").setText("Agilidade (AGI): "+state.agility());spend.forEach((key,button)->button.setEnabled(!pending&&alive&&state.points()>0&&(!key.equals("energy")||energyProfile!=null)));
    }
    public void refresh(GameplayProtocol.Sheet sheet,Protocol.Actor actor){int maximum=energyProfile==null?1000:energyProfile.maximum(),current=actor==null?0:energyProfile==null?Math.round(actor.stamina()):energyProfile.currentFor(client.snapshot(),client.id());resources.setText(actor==null?"Aguardando personagem":"HP "+actor.hp()+" / "+actor.maxHp()+" · "+(energySynchronizing?"Energia: sincronizando":"Energia "+Math.round(current/10f)+" / "+maximum/10));
        equipment.forEach((slot,label)->{String id=sheet==null?null:sheet.equipment().get(slot);var item=id==null?null:items.get(id);label.setText(item==null?"Vazio":item.name());label.setIcon(item==null?GameHudStyle.icon(slot.equals("weapon")?GameHudStyle.Glyph.SWORD:GameHudStyle.Glyph.ARMOR,24):GameHudStyle.icon(GameHudStyle.itemGlyph(item),24));label.setToolTipText(item==null?"Nenhum item equipado":item.description());remove.get(slot).setEnabled(sheet!=null&&item!=null);});
    }
}
