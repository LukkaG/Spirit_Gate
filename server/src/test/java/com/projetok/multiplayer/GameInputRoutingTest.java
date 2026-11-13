package com.projetok.multiplayer;

import java.awt.*;
import java.awt.event.*;
import java.time.Duration;
import java.util.concurrent.atomic.*;
import java.util.function.BooleanSupplier;
import javax.swing.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** Eventos AWT do teclado: a precedência não é simulada chamando apenas ActionMap. */
class GameInputRoutingTest {
    @Test void tabSelectsTargetsOnlyInTheWorldAndKeepsPanelAndTextNavigation()throws Exception{
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        try(var f=new Fixture()){
            var cycles=new AtomicInteger();var worldContext=new AtomicBoolean(true);SwingUtilities.invokeAndWait(()->f.input.targeting(cycles::incrementAndGet,worldContext::get));
            f.focus(f.world);f.key(KeyEvent.VK_TAB);assertEquals(1,cycles.get());assertTrue(f.focused(f.world));
            SwingKeys.press(KeyEvent.VK_TAB);SwingKeys.press(KeyEvent.VK_TAB);SwingKeys.release(KeyEvent.VK_TAB);assertEquals(2,cycles.get());
            f.focus(f.inventory);f.key(KeyEvent.VK_TAB);f.waitFor(()->f.focused(f.settings));assertEquals(2,cycles.get());
            worldContext.set(false);f.focus(f.inventory);f.key(KeyEvent.VK_TAB);f.waitFor(()->f.focused(f.settings));assertEquals(2,cycles.get());
            worldContext.set(true);f.focus(f.text);f.key(KeyEvent.VK_TAB);assertEquals(2,cycles.get());
            f.focus(f.inventory);SwingKeys.press(KeyEvent.VK_SHIFT);f.key(KeyEvent.VK_TAB);SwingKeys.release(KeyEvent.VK_SHIFT);assertEquals(2,cycles.get());assertEquals(0,f.shunpo.get());
        }
    }
    @Test void tabSpaceEnterTypingAndWindowScopeUseTheCorrectContext()throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        try(var f=new Fixture()) {
            f.focus(f.world);f.key(KeyEvent.VK_TAB);f.waitFor(()->f.focused(f.inventory));
            f.key(KeyEvent.VK_SPACE);assertEquals(1,f.attack.get());assertEquals(0,f.clicks.get());
            f.key(KeyEvent.VK_TAB);f.waitFor(()->f.focused(f.settings));f.key(KeyEvent.VK_SPACE);assertEquals(2,f.attack.get());assertEquals(0,f.clicks.get());
            f.key(KeyEvent.VK_TAB);f.waitFor(()->f.focused(f.skill));f.key(KeyEvent.VK_SPACE);assertEquals(3,f.attack.get());assertEquals(0,f.clicks.get());
            f.key(KeyEvent.VK_ENTER);assertEquals(1,f.clicks.get());
            SwingKeys.press(KeyEvent.VK_SPACE);SwingKeys.press(KeyEvent.VK_SPACE);SwingKeys.release(KeyEvent.VK_SPACE);f.drain();assertEquals(4,f.attack.get());
            f.key(KeyEvent.VK_I);assertEquals(1,f.inventoryAction.get());
            f.focus(f.text);f.key(KeyEvent.VK_W);f.key(KeyEvent.VK_SPACE);f.key(KeyEvent.VK_A);assertEquals("w a",f.onEdt(f.text::getText));assertEquals(4,f.attack.get());assertFalse(f.moving.get());
            SwingUtilities.invokeAndWait(()->{f.other.setVisible(true);f.other.toFront();});f.focus(f.outside);f.key(KeyEvent.VK_SPACE);assertEquals(1,f.outsideClicks.get());assertEquals(4,f.attack.get());
        }
    }
    @Test void captureModalUnavailableAndRecoveryNeverReplayHeldCommands()throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        try(var f=new Fixture()) {
            f.focus(f.world);SwingKeys.press(KeyEvent.VK_W);f.waitFor(f.moving::get);
            f.available.set(false);SwingUtilities.invokeAndWait(f.input::refreshContext);assertFalse(f.moving.get());
            f.available.set(true);SwingKeys.press(KeyEvent.VK_W);f.drain();assertFalse(f.moving.get());
            SwingKeys.release(KeyEvent.VK_W);f.key(KeyEvent.VK_W);assertFalse(f.moving.get());assertEquals(2,f.moveStarts.get());
            f.available.set(false);f.focus(f.inventory);f.key(KeyEvent.VK_SPACE);assertEquals(0,f.attack.get());assertEquals(0,f.clicks.get());f.key(KeyEvent.VK_I);assertEquals(1,f.inventoryAction.get());f.available.set(true);
            f.available.set(false);f.input.recoveryAvailable(()->true);f.key(KeyEvent.VK_R);assertEquals(1,f.recoveries.get());f.input.recoveryAvailable(()->false);f.key(KeyEvent.VK_R);assertEquals(1,f.recoveries.get());f.available.set(true);
            f.modal.set(true);f.focus(f.check);f.key(KeyEvent.VK_SPACE);assertTrue(f.onEdt(f.check::isSelected));assertEquals(0,f.attack.get());
            f.focus(f.capture);f.key(KeyEvent.VK_ENTER);f.key(KeyEvent.VK_SPACE);assertEquals(KeyEvent.VK_SPACE,f.captured.get());assertEquals(1,f.captureClicks.get());assertEquals(0,f.attack.get());
            f.modal.set(false);f.focus(f.inventory);f.key(KeyEvent.VK_SPACE);assertEquals(1,f.attack.get());
            f.key(KeyEvent.VK_SHIFT);assertEquals(1,f.shunpo.get());SwingKeys.press(KeyEvent.VK_SHIFT);f.key(KeyEvent.VK_TAB);SwingKeys.release(KeyEvent.VK_SHIFT);assertEquals(1,f.shunpo.get());
            SwingUtilities.invokeAndWait(()->{var next=new java.util.HashMap<>(f.input.shortcuts());next.put("SPACE",KeyEvent.VK_T);f.input.remap(next);});f.key(KeyEvent.VK_T);assertEquals(2,f.attack.get());
            assertThrows(IllegalArgumentException.class,()->{var next=new java.util.HashMap<>(f.input.shortcuts());next.put("SPACE",KeyEvent.VK_W);f.input.validate(next);});
        }
    }
    @Test void fiveDomainHotkeysAndCustomMappingKeepSingleDispatchAndAreaTabDoesNotSelect()throws Exception{
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        try(var f=new Fixture()){
            f.focus(f.world);
            for(int i=0;i<5;i++){SwingKeys.press(KeyEvent.VK_F5+i);SwingKeys.press(KeyEvent.VK_F5+i);SwingKeys.release(KeyEvent.VK_F5+i);assertEquals(1,f.domain.get(i));}
            SwingUtilities.invokeAndWait(()->{var next=new java.util.HashMap<>(f.input.shortcuts());next.put("domain-skill-1",KeyEvent.VK_F10);f.input.remap(next);});
            f.key(KeyEvent.VK_F5);assertEquals(1,f.domain.get(0));f.key(KeyEvent.VK_F10);assertEquals(2,f.domain.get(0));
            var cycles=new AtomicInteger();var aim=new AtomicBoolean(true);
            SwingUtilities.invokeAndWait(()->{f.input.targeting(cycles::incrementAndGet,()->true);f.input.aiming(aim::get,(k,p)->{},()->aim.set(false));});
            f.key(KeyEvent.VK_TAB);assertEquals(0,cycles.get());
            f.focus(f.text);f.key(KeyEvent.VK_F10);assertEquals(2,f.domain.get(0));
        }
    }
    static final class Fixture implements AutoCloseable {
        final JFrame frame=new JFrame("Roteamento da partida"),other=new JFrame("Janela externa");
        final JPanel world=new JPanel(new java.awt.FlowLayout());
        final JButton inventory=new JButton("Inventário"),settings=new JButton("Configurações"),skill=new JButton("Outra habilidade"),capture=new JButton("Capturar tecla"),outside=new JButton("Externo");
        final JTextField text=new JTextField(10);final JCheckBox check=new JCheckBox("Silenciar");
        final AtomicInteger attack=new AtomicInteger(),clicks=new AtomicInteger(),inventoryAction=new AtomicInteger(),outsideClicks=new AtomicInteger(),captureClicks=new AtomicInteger(),captured=new AtomicInteger(),moveStarts=new AtomicInteger(),shunpo=new AtomicInteger();
        final AtomicBoolean available=new AtomicBoolean(true),modal=new AtomicBoolean(),moving=new AtomicBoolean();
        final AtomicInteger recoveries=new AtomicInteger();
        final AtomicIntegerArray domain=new AtomicIntegerArray(5);
        final GameInputRouter input;
        final int listeners;
        /** A fotografia inicial vem depois dos windowClosed pendentes de outras janelas de teste. */
        Fixture()throws Exception {
            SwingKeys.drain();
            var initialListeners=new AtomicInteger();
            SwingUtilities.invokeAndWait(()->initialListeners.set(KeyboardFocusManager.getCurrentKeyboardFocusManager().getPropertyChangeListeners("focusOwner").length));
            listeners=initialListeners.get();
            AtomicReference<GameInputRouter> result=new AtomicReference<>();
            SwingUtilities.invokeAndWait(()->{
                world.setFocusable(true);world.add(inventory);world.add(settings);world.add(skill);world.add(text);world.add(check);world.add(capture);frame.setContentPane(world);frame.setSize(550,230);frame.setLocation(25,80);
                for(var b:new JButton[]{inventory,settings,skill})b.addActionListener(e->clicks.incrementAndGet());outside.addActionListener(e->outsideClicks.incrementAndGet());other.add(outside);other.setSize(320,140);other.setLocation(610,80);
                bind("SPACE","SPACE",()->attack.incrementAndGet());bind("W","Wtrue",()->{moving.set(true);moveStarts.incrementAndGet();});
                world.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released W"),"Wfalse");world.getActionMap().put("Wfalse",new AbstractAction(){public void actionPerformed(ActionEvent e){moving.set(false);}});
                bind("I","inventory",()->inventoryAction.incrementAndGet());bind("SHIFT","SHIFT",()->shunpo.incrementAndGet());bind("Q","Q",()->{});bind("1","hotbar-use",()->{});
                bind("R","R",()->recoveries.incrementAndGet());
                for(int i=0;i<5;i++){final int slot=i;bind("F"+(i+5),"domain-skill-"+(i+1),()->domain.incrementAndGet(slot));}
                var router=new GameInputRouter(frame,world,available::get,modal::get,()->moving.set(false));result.set(router);capture.addActionListener(e->{captureClicks.incrementAndGet();router.capture(captured::set);});router.install();frame.setVisible(true);
            });input=result.get();drain();
        }
        private void bind(String key,String action,Runnable run){world.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed "+key),action);world.getActionMap().put(action,new AbstractAction(){public void actionPerformed(ActionEvent e){run.run();}});}
        <T>T onEdt(java.util.concurrent.Callable<T> query)throws Exception{var result=new AtomicReference<T>();SwingUtilities.invokeAndWait(()->{try{result.set(query.call());}catch(Exception e){throw new RuntimeException(e);}});return result.get();}
        boolean focused(Component c){return KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()==c;}
        void focus(JComponent c)throws Exception{
            SwingUtilities.invokeAndWait(()->SwingUtilities.getWindowAncestor(c).toFront());
            long deadline=System.nanoTime()+Duration.ofSeconds(4).toNanos();
            while(!focused(c)&&System.nanoTime()<deadline){SwingUtilities.invokeAndWait(c::requestFocusInWindow);Thread.sleep(20);}
            assertTrue(focused(c),"Componente deve receber foco AWT antes da injeção de teclas");
        }
        void key(int key)throws Exception{SwingKeys.tap(key);}
        void drain()throws Exception{SwingKeys.drain();}
        void waitFor(BooleanSupplier query)throws Exception{long until=System.nanoTime()+Duration.ofSeconds(4).toNanos();while(!query.getAsBoolean()&&System.nanoTime()<until){Thread.sleep(20);}assertTrue(query.getAsBoolean(),"Condição de foco/input não atingida");}
        @Override public void close()throws Exception{SwingKeys.clear();SwingUtilities.invokeAndWait(()->{frame.dispose();other.dispose();});drain();assertTrue(input.isClosed());assertEquals(listeners,KeyboardFocusManager.getCurrentKeyboardFocusManager().getPropertyChangeListeners("focusOwner").length);}
    }
}
