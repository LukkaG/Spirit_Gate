package com.projetok.multiplayer;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.function.*;
import javax.swing.*;
import javax.swing.text.JTextComponent;

/** Prioridade de hotkeys restrita a uma partida; nunca intercepta launcher ou outra janela. */
public final class GameInputRouter implements KeyEventDispatcher,AutoCloseable {
    private record Binding(String press,String release,boolean window) { }
    private final JFrame owner;
    private final JComponent world;
    private final BooleanSupplier available,modal;
    private BooleanSupplier recoveryAvailable;
    private Runnable cycleTarget;
    private BooleanSupplier targetContext=()->false;
    private BooleanSupplier aiming=()->false;
    private BiConsumer<Integer,Boolean> aimDirection=(key,pressed)->{};
    private Runnable confirmAim=()->{};
    /** Reserva o teclado para o retículo enquanto uma habilidade de chão aguarda confirmação. */
    public void aiming(BooleanSupplier context,BiConsumer<Integer,Boolean> direction,Runnable confirm){aiming=context;aimDirection=direction;confirmAim=confirm;}
    public void targeting(Runnable cycle,BooleanSupplier context){cycleTarget=cycle;targetContext=context;}
    private final Runnable stop;
    private final KeyboardFocusManager manager=KeyboardFocusManager.getCurrentKeyboardFocusManager();
    private final Map<KeyStroke,Object> original=new LinkedHashMap<>();
    private final Map<Integer,Binding> bindings=new LinkedHashMap<>();
    private final Map<String,Integer> defaults=new LinkedHashMap<>(),shortcuts=new LinkedHashMap<>();
    private final Set<Integer> down=new HashSet<>(),quarantine=new HashSet<>();
    private final PropertyChangeListener focusListener=event->refreshContext();
    private Consumer<Integer> capture;
    private Supplier<? extends Component> modalRoot=()->null;
    private boolean closed,shiftTab,installed;
    private final WindowAdapter lifecycle=new WindowAdapter(){@Override public void windowClosed(WindowEvent event){close();}};
    public GameInputRouter(JFrame owner,JComponent world,BooleanSupplier available,BooleanSupplier modal,Runnable stop) {
        this.owner=owner;this.world=world;this.available=available;this.modal=modal;this.stop=stop;
        recoveryAvailable=available;
        for(String key:new String[]{"W","A","S","D","UP","DOWN","LEFT","RIGHT","SPACE","SHIFT","Q","1","I","J","ESCAPE","F","E","C","P","R","M","F2","F5","F6","F7","F8","F9"}) {
            var press=KeyStroke.getKeyStroke("pressed "+key);var release=KeyStroke.getKeyStroke("released "+key);
            Object action=world.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(press),up=world.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(release);
            if(action==null)continue;
            boolean window=Set.of("I","J","ESCAPE","M","F2").contains(key);
            var binding=new Binding(action.toString(),up==null?null:up.toString(),window);bindings.put(press.getKeyCode(),binding);
            original.put(press,action);original.put(release,up);
            world.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).remove(press);world.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).remove(release);
            if(!Set.of("W","A","S","D","UP","DOWN","LEFT","RIGHT","ESCAPE","F2").contains(key))defaults.put(binding.press(),press.getKeyCode());
        }
        shortcuts.putAll(defaults);
    }
    public void install(){if(closed||installed)return;installed=true;manager.addKeyEventDispatcher(this);manager.addPropertyChangeListener("focusOwner",focusListener);owner.addWindowListener(lifecycle);}
    public void modalRoot(Supplier<? extends Component> supplier){modalRoot=supplier;}
    /** Retorno após derrota continua disponível sem liberar movimento ou combate. */
    public void recoveryAvailable(BooleanSupplier value){recoveryAvailable=value;}
    private boolean scoped(Component focus){return focus!=null&&SwingUtilities.getWindowAncestor(focus)==owner;}
    private boolean editing(Component focus){return focus instanceof JTextComponent text&&text.isEditable()||focus instanceof JComboBox<?>||focus instanceof JSpinner||focus instanceof JSlider;}
    public void refreshContext(){if(Boolean.TRUE.equals(owner.getRootPane().getClientProperty("worldEditorActive"))){reset();return;}var focus=manager.getFocusOwner();if(!scoped(focus)||editing(focus)||modal.getAsBoolean()||!available.getAsBoolean())reset();}
    /** Soltar fora da janela também libera a quarentena, sem consumir eventos de outro aplicativo. */
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if(closed)return false;
        int code=event.getKeyCode();Component focus=manager.getFocusOwner();boolean inside=scoped(focus);
        if(event.getID()==KeyEvent.KEY_RELEASED&&quarantine.remove(code))return inside;
        if(!inside)return false;
        if(Boolean.TRUE.equals(owner.getRootPane().getClientProperty("worldEditorActive"))){reset();return false;}
        Component modalComponent=modalRoot.get();
        if(modal.getAsBoolean()&&modalComponent!=null&&!SwingUtilities.isDescendingFrom(focus,modalComponent)){reset();event.consume();return true;}
        if(capture!=null){
            if(event.getID()==KeyEvent.KEY_PRESSED){var callback=capture;capture=null;quarantine.add(code);callback.accept(code);}
            event.consume();return true;
        }
        if(event.getID()==KeyEvent.KEY_PRESSED&&code==KeyEvent.VK_TAB&&down.contains(KeyEvent.VK_SHIFT))shiftTab=true;
        if(code==KeyEvent.VK_TAB){
            // TAB seleciona somente com o viewport focado. Na HUD, a travessia acessível pertence ao Swing.
            if(cycleTarget!=null&&focus==world&&!aiming.getAsBoolean()&&!event.isShiftDown()&&!event.isControlDown()&&!event.isAltDown()&&!event.isMetaDown()&&!editing(focus)&&!modal.getAsBoolean()&&targetContext.getAsBoolean()&&available.getAsBoolean()){
                if(event.getID()==KeyEvent.KEY_PRESSED&&down.add(code))cycleTarget.run();if(event.getID()==KeyEvent.KEY_RELEASED)down.remove(code);event.consume();return true;
            }
            down.remove(code);return false;
        }
        if(editing(focus)){reset();return false;}
        if(aiming.getAsBoolean()&&!modal.getAsBoolean()&&available.getAsBoolean()&&!event.isControlDown()&&!event.isAltDown()&&!event.isMetaDown()){
            if(directionKey(code)){
                if(event.getID()==KeyEvent.KEY_PRESSED&&!quarantine.contains(code)&&down.add(code))aimDirection.accept(code,true);
                else if(event.getID()==KeyEvent.KEY_RELEASED&&down.remove(code))aimDirection.accept(code,false);
                event.consume();return true;
            }
            if(code==KeyEvent.VK_ENTER){if(event.getID()==KeyEvent.KEY_PRESSED&&down.add(code))confirmAim.run();if(event.getID()==KeyEvent.KEY_RELEASED)down.remove(code);event.consume();return true;}
            if(event.getID()==KeyEvent.KEY_TYPED&&"wasdWASD".indexOf(event.getKeyChar())>=0){event.consume();return true;}
        }
        if(event.getID()==KeyEvent.KEY_PRESSED&&code==KeyEvent.VK_ENTER&&focus instanceof AbstractButton button){if(down.add(code)&&button.isEnabled())button.doClick(0);event.consume();return true;}
        if(code==KeyEvent.VK_ENTER&&event.getID()==KeyEvent.KEY_RELEASED){down.remove(code);return focus instanceof AbstractButton;}
        // Os bindings de gameplay já foram retirados de WHEN_IN_FOCUSED_WINDOW:
        // no modal, devolver o evento permite operar checkbox/slider sem fallback para combate.
        if(modal.getAsBoolean()&&code!=KeyEvent.VK_ESCAPE){reset();return false;}
        if(event.isControlDown()||event.isAltDown()||event.isMetaDown()){reset();return false;}
        if(event.getID()==KeyEvent.KEY_TYPED){
            int typed=event.getKeyChar()==' '?KeyEvent.VK_SPACE:KeyEvent.getExtendedKeyCodeForChar(Character.toUpperCase(event.getKeyChar()));
            if(bindings.containsKey(typed)){event.consume();return true;}return false;
        }
        var binding=bindings.get(code);if(binding==null)return false;
        if(event.getID()==KeyEvent.KEY_RELEASED){
            boolean was=down.remove(code);
            if(was&&binding.release()!=null)invoke(binding.release());
            if(was&&code==KeyEvent.VK_SHIFT&&!shiftTab&&!modal.getAsBoolean()&&available.getAsBoolean())invoke(binding.press());
            event.consume();return true;
        }
        if(event.getID()!=KeyEvent.KEY_PRESSED)return false;
        if(!binding.window()&&!available.getAsBoolean()&&!(binding.press().equals("R")&&recoveryAvailable.getAsBoolean())){reset();quarantine.add(code);event.consume();return true;}
        if(quarantine.contains(code)){event.consume();return true;}
        if(down.add(code)){if(code==KeyEvent.VK_SHIFT)shiftTab=false;else invoke(binding.press());}
        event.consume();return true;
    }
    private void invoke(String action){var handler=world.getActionMap().get(action);if(handler!=null&&handler.isEnabled())handler.actionPerformed(new ActionEvent(world,ActionEvent.ACTION_PERFORMED,action));}
    private static boolean directionKey(int code){return Set.of(KeyEvent.VK_W,KeyEvent.VK_A,KeyEvent.VK_S,KeyEvent.VK_D,KeyEvent.VK_UP,KeyEvent.VK_DOWN,KeyEvent.VK_LEFT,KeyEvent.VK_RIGHT).contains(code);}
    /** Libera também o retículo antes de quarentenar teclas; o release posterior não pode deixar vetor preso. */
    public void reset(){if(!down.isEmpty()){for(int code:down)if(directionKey(code))aimDirection.accept(code,false);quarantine.addAll(down);down.clear();stop.run();}shiftTab=false;}
    public void capture(Consumer<Integer> next){reset();capture=next;}
    public void cancelCapture(){capture=null;reset();}
    public Map<String,Integer> shortcuts(){return Map.copyOf(shortcuts);}
    public Map<String,Integer> defaults(){return Map.copyOf(defaults);}
    public void validate(Map<String,Integer> proposed){
        if(!proposed.keySet().equals(defaults.keySet()))throw new IllegalArgumentException("Conjunto de atalhos inválido");
        var used=new HashSet<Integer>(Set.of(KeyEvent.VK_W,KeyEvent.VK_A,KeyEvent.VK_S,KeyEvent.VK_D,KeyEvent.VK_UP,KeyEvent.VK_DOWN,KeyEvent.VK_LEFT,KeyEvent.VK_RIGHT,KeyEvent.VK_ESCAPE,KeyEvent.VK_TAB,KeyEvent.VK_ENTER,KeyEvent.VK_F2));
        for(int code:proposed.values()){
            boolean allowed=code>=KeyEvent.VK_A&&code<=KeyEvent.VK_Z||code>=KeyEvent.VK_0&&code<=KeyEvent.VK_9||code>=KeyEvent.VK_F1&&code<=KeyEvent.VK_F12||code==KeyEvent.VK_SPACE||code==KeyEvent.VK_SHIFT;
            if(!allowed||!used.add(code))throw new IllegalArgumentException("Tecla reservada ou repetida: "+KeyEvent.getKeyText(code));
        }
    }
    public void remap(Map<String,Integer> proposed){validate(proposed);reset();var actions=new HashMap<String,Binding>();bindings.values().forEach(b->actions.put(b.press(),b));
        bindings.entrySet().removeIf(e->shortcuts.containsKey(e.getValue().press()));
        proposed.forEach((action,key)->bindings.put(key,actions.get(action)));shortcuts.clear();shortcuts.putAll(proposed);
    }
    public boolean isClosed(){return closed;}
    @Override public void close(){if(closed)return;reset();closed=true;capture=null;manager.removeKeyEventDispatcher(this);manager.removePropertyChangeListener("focusOwner",focusListener);owner.removeWindowListener(lifecycle);installed=false;
        original.forEach((key,value)->{if(value!=null)world.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key,value);});
    }
}
