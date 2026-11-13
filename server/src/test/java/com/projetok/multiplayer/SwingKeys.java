package com.projetok.multiplayer;

import java.awt.*;
import java.awt.event.KeyEvent;
import javax.swing.SwingUtilities;

/** Injeta eventos na fila AWT, incluindo KEY_TYPED e os bindings Swing, sem chamar ActionMap. */
final class SwingKeys {
    private static int modifiers;
    static void press(int code)throws Exception{if(code==KeyEvent.VK_SHIFT)modifiers|=KeyEvent.SHIFT_DOWN_MASK;post(KeyEvent.KEY_PRESSED,code);char value=character(code);if(value!=KeyEvent.CHAR_UNDEFINED)post(KeyEvent.KEY_TYPED,code);drain();}
    static void release(int code)throws Exception{if(code==KeyEvent.VK_SHIFT)modifiers&=~KeyEvent.SHIFT_DOWN_MASK;post(KeyEvent.KEY_RELEASED,code);drain();}
    static void tap(int code)throws Exception{press(code);release(code);}
    private static void post(int type,int code)throws Exception{
        // TAB pode deixar focusOwner nulo durante a transferência AWT entre controles.
        // Aguarda um destinatário real; não chama ActionMap nem inventa um componente de fallback.
        var posted=new java.util.concurrent.atomic.AtomicBoolean();long deadline=System.nanoTime()+2_000_000_000L;
        while(!posted.get()&&System.nanoTime()<deadline){
            SwingUtilities.invokeAndWait(()->{
                Component focus=KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if(focus!=null){Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(new KeyEvent(focus,type,System.currentTimeMillis(),modifiers,type==KeyEvent.KEY_TYPED?KeyEvent.VK_UNDEFINED:code,character(code)));posted.set(true);}
            });
            if(!posted.get())Thread.sleep(10);
        }
        if(!posted.get())throw new AssertionError("Evento sem componente focado após aguardar a transferência AWT");
    }
    private static char character(int code){if(code>=KeyEvent.VK_A&&code<=KeyEvent.VK_Z)return (char)('a'+code-KeyEvent.VK_A);if(code>=KeyEvent.VK_0&&code<=KeyEvent.VK_9)return (char)code;return switch(code){case KeyEvent.VK_SPACE->' ';case KeyEvent.VK_ENTER->'\n';case KeyEvent.VK_TAB->'\t';default->KeyEvent.CHAR_UNDEFINED;};}
    static void drain()throws Exception{SwingUtilities.invokeAndWait(()->{});Thread.sleep(45);SwingUtilities.invokeAndWait(()->{});}
    static void clear(){modifiers=0;}
    private SwingKeys() { }
}
