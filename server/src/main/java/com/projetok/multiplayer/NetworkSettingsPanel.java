package com.projetok.multiplayer;

import com.projetok.rpg.RpgPreferences;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import javax.swing.*;

/** Preferências existentes e atalhos desta sessão, com transação explícita Aplicar/Cancelar. */
public final class NetworkSettingsPanel extends JPanel implements AutoCloseable {
    private final JFrame owner;
    private final GameInputRouter input;
    private final Path file;
    private final Consumer<RpgPreferences> apply;
    private final Runnable bindingsChanged,close;
    private final JSlider volume=new JSlider(0,100);
    private final JCheckBox muted=new JCheckBox("Silenciar áudio"),reduced=new JCheckBox("Reduzir animações e iluminação decorativas");
    private final JComboBox<String> resolution;
    private final JLabel status=new JLabel("Carregando preferências…");
    private final JButton save=new JButton("Aplicar"),cancel=new JButton("Cancelar");
    private final Map<String,Integer> draft=new LinkedHashMap<>();
    private final Map<String,JButton> captureButtons=new LinkedHashMap<>();
    private SwingWorker<RpgPreferences,Void> loading;
    private boolean saving;
    private int generation;
    public NetworkSettingsPanel(JFrame owner,GameInputRouter input,Path file,Consumer<RpgPreferences> apply,Runnable bindingsChanged,Runnable close) {
        super(new BorderLayout(8,10));this.owner=owner;this.input=input;this.file=file;this.apply=apply;this.bindingsChanged=bindingsChanged;this.close=close;
        setName("networkSettings");volume.setName("settingsVolume");volume.setMajorTickSpacing(25);volume.setPaintTicks(true);volume.setPaintLabels(true);volume.getAccessibleContext().setAccessibleName("Volume geral");
        muted.setName("settingsMuted");reduced.setName("settingsReduced");save.setName("settingsApply");cancel.setName("settingsCancel");
        var sizes=new LinkedHashSet<String>();sizes.add(owner.getWidth()+"x"+owner.getHeight());sizes.addAll(java.util.List.of("1366x768","1000x700","640x480"));resolution=new JComboBox<>(sizes.toArray(String[]::new));resolution.setName("settingsResolution");resolution.getAccessibleContext().setAccessibleName("Resolução da janela nesta sessão");
        var form=new JPanel();form.setLayout(new BoxLayout(form,BoxLayout.Y_AXIS));
        var audioInfo=new JTextArea("Volume geral disponível nesta versão. Música e efeitos não possuem canais separados no motor atual.");audioInfo.setEditable(false);audioInfo.setLineWrap(true);audioInfo.setWrapStyleWord(true);audioInfo.setRows(2);audioInfo.setFocusable(false);
        form.add(section("ÁUDIO",new JLabel("Volume geral"),volume,muted,audioInfo));form.add(Box.createVerticalStrut(14));
        form.add(section("ACESSIBILIDADE",reduced));form.add(Box.createVerticalStrut(14));
        form.add(section("VÍDEO — JANELA",resolution));form.add(Box.createVerticalStrut(14));
        var keys=new JPanel(new GridLayout(0,2,12,7));var names=new LinkedHashMap<String,String>();
        names.put("SPACE","Ataque básico");names.put("SHIFT","Shunpo ofensivo");names.put("Q","Shunpo defensivo");names.put("hotbar-use","Ação selecionada");names.put("inventory","Inventário");names.put("hud-J","Missões");names.put("hud-M","Minimapa");names.put("F","Conversar");names.put("E","Aceitar missão");names.put("C","Entregar missão");names.put("P","Portal");names.put("R","Retornar após derrota");
        for(int slot=1;slot<=5;slot++)names.put("domain-skill-"+slot,"Técnica do domínio "+slot);
        for(var entry:names.entrySet())if(input.shortcuts().containsKey(entry.getKey())){
            keys.add(new JLabel(entry.getValue()));var button=new JButton();button.setName("bind-"+entry.getKey());button.getAccessibleContext().setAccessibleName("Remapear "+entry.getValue());captureButtons.put(entry.getKey(),button);keys.add(button);
            button.addActionListener(e->{status.setText("Pressione a nova tecla; ESC cancela a captura.");button.setText("Aguardando tecla…");input.capture(code->{
                if(code!=KeyEvent.VK_ESCAPE){var next=new LinkedHashMap<>(draft);next.put(entry.getKey(),code);try{input.validate(next);draft.clear();draft.putAll(next);status.setText("Atalho preparado. Aplicar confirma; Cancelar descarta.");}catch(IllegalArgumentException invalid){status.setText(invalid.getMessage());}}
                else status.setText("Captura cancelada.");refreshKeys();
            });});
        }
        var controlSection=section("CONTROLES — NESTA SESSÃO",new JLabel("WASD/setas: mover ou ajustar área · TAB: alvo/foco"),new JLabel("ENTER: confirmar área/ativar · ESC: cancelar/fechar"),keys);
        var restore=new JButton("Restaurar padrões");restore.setName("settingsDefaults");restore.addActionListener(e->{loadValues(RpgPreferences.defaults());draft.clear();draft.putAll(input.defaults());refreshKeys();status.setText("Padrões preparados; confirme em Aplicar.");});controlSection.add(restore);form.add(controlSection);
        var scroll=new JScrollPane(form);scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);add(scroll);
        var footer=new JPanel(new BorderLayout(8,7));var buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));buttons.add(cancel);buttons.add(save);footer.add(buttons,BorderLayout.NORTH);footer.add(status);add(footer,BorderLayout.SOUTH);
        cancel.addActionListener(e->close.run());save.addActionListener(e->commit());GameHudStyle.skin(this);
    }
    private static JPanel section(String title,JComponent... rows){var p=new JPanel();p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));var heading=new JLabel(title);p.add(heading);p.add(Box.createVerticalStrut(7));for(var row:rows){row.setAlignmentX(LEFT_ALIGNMENT);p.add(row);p.add(Box.createVerticalStrut(5));}return p;}
    private void loadValues(RpgPreferences value){volume.setValue(value.volume());muted.setSelected(value.muted());reduced.setSelected(!value.environment());}
    private void refreshKeys(){captureButtons.forEach((id,b)->b.setText(KeyEvent.getKeyText(draft.getOrDefault(id,input.defaults().get(id)))));}
    public void begin(){int token=++generation;save.setEnabled(false);draft.clear();draft.putAll(input.shortcuts());refreshKeys();String size=owner.getWidth()+"x"+owner.getHeight();boolean found=false;for(int i=0;i<resolution.getItemCount();i++)if(resolution.getItemAt(i).equals(size))found=true;if(!found)resolution.addItem(size);resolution.setSelectedItem(size);status.setText("Carregando preferências…");
        loading=new SwingWorker<>(){protected RpgPreferences doInBackground()throws Exception{return RpgPreferences.load(file);}protected void done(){if(token!=generation||!owner.isDisplayable())return;
            try{loadValues(get());status.setText("Alterações só são confirmadas em Aplicar.");}catch(Exception error){loadValues(RpgPreferences.defaults());status.setText("Arquivo indisponível; padrões nesta janela. Cancelar preserva o arquivo.");}save.setEnabled(true);
        }};loading.execute();
    }
    private void commit(){
        if(saving||!save.isEnabled())return;input.cancelCapture();var keys=Map.copyOf(draft);
        try{input.validate(keys);}catch(IllegalArgumentException error){status.setText(error.getMessage());return;}
        var value=new RpgPreferences(volume.getValue(),muted.isSelected(),!reduced.isSelected());String[] size=((String)resolution.getSelectedItem()).split("x");int w=Integer.parseInt(size[0]),h=Integer.parseInt(size[1]);
        saving=true;save.setEnabled(false);cancel.setEnabled(false);status.setText("Salvando preferências…");int token=generation;
        new SwingWorker<Void,Void>(){protected Void doInBackground()throws Exception{value.save(file);return null;}protected void done(){
            saving=false;if(token!=generation||!owner.isDisplayable())return;save.setEnabled(true);cancel.setEnabled(true);
            try{get();input.remap(keys);bindingsChanged.run();apply.accept(value);owner.setSize(w,h);close.run();}catch(Exception error){status.setText("Não foi possível aplicar. Preferências da partida mantidas.");}
        }}.execute();
    }
    public boolean canClose(){return !saving;}
    @Override public void close(){generation++;if(loading!=null)loading.cancel(true);input.cancelCapture();}
}
