package com.projetok.prototype.rpg;

import com.projetok.multiplayer.NetworkPlayerVisuals;
import com.projetok.multiplayer.SessionCharacterSelection;
import com.projetok.prototype.DirectionalAnimation.Direction;
import com.projetok.rpg.CharacterRaces;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.swing.*;

/** Escolha temporaria de personagem, sem criar mundos locais ou acessar saves. */
public final class SessionCharacterCreation extends JPanel {
    final JTextField name=new JTextField(18);
    final Map<String,JRadioButton> races=new LinkedHashMap<>();
    final JComboBox<CosmeticCatalog.Definition> hair=new JComboBox<>();
    final JComboBox<CosmeticCatalog.Definition> clothing=new JComboBox<>();
    final JTextArea notice=new JTextArea(3,20);
    final JPanel preview;
    private final NetworkPlayerVisuals visuals;
    private final CosmeticCatalog catalog,previewCatalog;
    private Direction direction=Direction.FRONT;

    /** Os recursos devem estar carregados antes de construir o formulario na EDT. */
    public SessionCharacterCreation(NetworkPlayerVisuals visuals,CosmeticCatalog catalog) {
        this(visuals,catalog,catalog==null?null:catalog.onLogicalCanvas(64,64,32,60,NetworkPlayerVisuals.LEGACY_SCALE));
    }

    /** Permite preparar tambem as camadas de previa no worker de carregamento. */
    public SessionCharacterCreation(NetworkPlayerVisuals visuals,CosmeticCatalog catalog,CosmeticCatalog previewCatalog) {
        super(new BorderLayout(12,8));
        this.visuals=Objects.requireNonNull(visuals);this.catalog=catalog;this.previewCatalog=previewCatalog;
        setName("sessionCharacterCreation");setBackground(CharacterSelection.INK);setOpaque(true);
        JPanel heading=column();
        heading.add(CharacterSelection.label("PERSONAGEM DESTA PARTIDA",17));
        heading.add(CharacterSelection.label("Escolha seu nome, raça e aparência.",12));
        add(heading,BorderLayout.NORTH);

        JPanel fields=column();
        name.setName("sessionCharacterName");SpiritGateEntry.styleField(name);name.getAccessibleContext().setAccessibleName("Nome do personagem da partida");
        fields.add(CharacterSelection.label("Nome (3 a 24 caracteres)",13));fields.add(name);
        fields.add(Box.createVerticalStrut(8));fields.add(CharacterSelection.label("Raça",13));
        JPanel options=new JPanel(new GridLayout(0,2,4,4));options.setOpaque(false);ButtonGroup group=new ButtonGroup();
        for(var race:CharacterRaces.all()) {
            var button=new JRadioButton(race.name(),races.isEmpty());button.setOpaque(false);button.setForeground(Color.WHITE);
            button.setName("sessionRace_"+race.id());button.getAccessibleContext().setAccessibleName("Raça "+race.name());
            button.setToolTipText(race.description());group.add(button);races.put(race.id(),button);options.add(button);
            button.addActionListener(e->refreshRace());
        }
        fields.add(options);fields.add(Box.createVerticalStrut(8));
        populate(hair,"hair");populate(clothing,"clothing");
        hair.setName("sessionHair");hair.getAccessibleContext().setAccessibleName("Cabelo temporário");
        clothing.setName("sessionClothing");clothing.getAccessibleContext().setAccessibleName("Roupa temporária");
        fields.add(CharacterSelection.label("Cabelo temporário",13));fields.add(hair);
        fields.add(CharacterSelection.label("Roupa temporária",13));fields.add(clothing);
        for(Component field:fields.getComponents())if(field instanceof JComponent component) {
            component.setAlignmentX(Component.LEFT_ALIGNMENT);
            component.setMaximumSize(new Dimension(Integer.MAX_VALUE,component.getPreferredSize().height));
        }
        var scroll=new JScrollPane(fields);scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);scroll.getViewport().setOpaque(false);scroll.setMinimumSize(new Dimension(0,0));
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);scroll.getVerticalScrollBar().setUnitIncrement(18);

        preview=new JPanel(){@Override protected void paintComponent(Graphics graphics){super.paintComponent(graphics);paintPreview((Graphics2D)graphics);}};
        preview.setName("sessionCharacterPreview");preview.setOpaque(false);preview.setPreferredSize(new Dimension(220,230));
        preview.setMinimumSize(new Dimension(0,0));preview.getAccessibleContext().setAccessibleName("Prévia da raça e aparência escolhidas");
        JPanel art=new JPanel(new BorderLayout(4,4));art.setOpaque(false);art.add(preview);
        JButton turn=new CharacterSelection.SpiritButton("Alternar orientação",false);turn.setName("sessionPreviewDirection");
        turn.addActionListener(e->{var directions=Direction.values();direction=directions[(direction.ordinal()+1)%directions.length];preview.repaint();});
        art.add(turn,BorderLayout.SOUTH);
        JPanel center=new JPanel(new GridLayout(1,2,10,0));center.setOpaque(false);center.add(scroll);center.add(art);add(center);
        notice.setName("sessionRaceNotice");notice.setEditable(false);notice.setFocusable(false);notice.setLineWrap(true);notice.setWrapStyleWord(true);
        notice.setOpaque(false);notice.setForeground(SpiritTheme.ACCENT);notice.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,12));
        // A altura depende de linhas reservadas, nao da largura ainda zero no primeiro layout.
        notice.setPreferredSize(new Dimension(1,52));notice.setMinimumSize(new Dimension(0,52));
        add(notice,BorderLayout.SOUTH);
        hair.addActionListener(e->preview.repaint());clothing.addActionListener(e->preview.repaint());
        setPreferredSize(new Dimension(560,350));refreshRace();
    }

    private static JPanel column(){JPanel panel=new JPanel();panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));panel.setOpaque(false);return panel;}

    private void populate(JComboBox<CosmeticCatalog.Definition> combo,String type) {
        combo.addItem(new CosmeticCatalog.Definition("none",type,"Padrão da raça"));
        if(catalog!=null)for(var definition:catalog.all(type))
            if(catalog.warnings().stream().noneMatch(warning->warning.startsWith(definition.id()+"/")))combo.addItem(definition);
    }

    /** Preenche somente o campo da escolha atual; nenhuma preferencia e gravada. */
    public void setPlayerName(String value){name.setText(value==null?"":value);}

    public String selectedRace(){return races.entrySet().stream().filter(e->e.getValue().isSelected()).map(Map.Entry::getKey).findFirst().orElseThrow();}

    /** Seleciona uma das quatro racas reconhecidas pelo jogo. */
    public void setSelectedRace(String race){CharacterRaces.get(race);races.get(race).setSelected(true);refreshRace();}

    /** Recupera uma escolha da mesma interface, por exemplo depois de falha de conexao. */
    public void setSelection(SessionCharacterSelection value) {
        Objects.requireNonNull(value);setSelectedRace(value.race());setPlayerName(value.name());
        choose(hair,value.hair());choose(clothing,value.clothing());preview.repaint();
    }

    /** Valida os campos e recursos, deixando a confirmacao e a conexao a cargo do launcher. */
    public SessionCharacterSelection selection() {
        var info=visuals.renderInfo(selectedRace());
        if(!info.available())throw new IllegalArgumentException(info.detail());
        String selectedHair=selectedId(hair),selectedClothing=selectedId(clothing);
        if(selectedRace().equals("reaper")&&(!selectedHair.equals("none")||!selectedClothing.equals("none")))
            throw new IllegalArgumentException("Reaper ainda não possui acessórios compatíveis.");
        var result=new SessionCharacterSelection(name.getText(),selectedRace(),selectedHair,selectedClothing);
        if(catalog!=null)catalog.validate(result.cosmetics());
        else if(!selectedHair.equals("none")||!selectedClothing.equals("none"))throw new IllegalArgumentException("Catálogo de acessórios indisponível.");
        return result;
    }

    public String resourceNotice(){return notice.getText();}

    private static String selectedId(JComboBox<CosmeticCatalog.Definition> combo){return ((CosmeticCatalog.Definition)combo.getSelectedItem()).id();}
    private static void choose(JComboBox<CosmeticCatalog.Definition> combo,String id) {
        for(int index=0;index<combo.getItemCount();index++)if(combo.getItemAt(index).id().equals(id)){combo.setSelectedIndex(index);return;}
        throw new IllegalArgumentException("Acessório indisponível: "+id);
    }

    private void refreshRace() {
        var info=visuals.renderInfo(selectedRace());boolean accessories=info.available()&&visuals.supportsAccessories(selectedRace())&&catalog!=null;
        if(!accessories){hair.setSelectedIndex(0);clothing.setSelectedIndex(0);}
        hair.setEnabled(accessories&&hair.getItemCount()>1);clothing.setEnabled(accessories&&clothing.getItemCount()>1);
        notice.setText(info.detail()+(catalog==null&&!selectedRace().equals("reaper")?" Personalização indisponível; aparência base.":""));
        races.forEach((id,button)->button.setForeground(button.isSelected()?SpiritTheme.ACCENT:Color.WHITE));
        preview.repaint();
    }

    private void paintPreview(Graphics2D original) {
        Graphics2D g=(Graphics2D)original.create();
        try {
            int width=preview.getWidth(),height=preview.getHeight();if(width<1||height<1)return;
            g.setColor(new Color(9,27,42));g.fillRoundRect(0,0,width,height,16,16);
            var info=visuals.renderInfo(selectedRace());
            if(!info.available()){g.setColor(SpiritTheme.ACCENT);g.drawString("Prévia indisponível",8,Math.max(20,height/2));return;}
            int zoom=Math.max(1,Math.min(3,Math.min(Math.max(1,(width-20)/64),Math.max(1,(height-30)/64))));
            int x=width/2,y=Math.max(64,Math.min(height-18,(height+64*zoom)/2-4));
            g.setColor(new Color(40,132,159,80));g.fillOval(x-25*zoom,y-4,50*zoom,10);
            g.translate(x,y);g.scale(zoom,zoom);
            CosmeticRenderer.draw(g,visuals.sprites(selectedRace()),visuals.supportsAccessories(selectedRace())?previewCatalog:null,
                new CosmeticCatalog.Selection(selectedId(hair),selectedId(clothing)),direction,0,false,0,0);
        } finally {g.dispose();}
    }
}
