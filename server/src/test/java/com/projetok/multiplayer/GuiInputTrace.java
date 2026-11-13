package com.projetok.multiplayer;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayDeque;
import javax.swing.*;
import org.junit.jupiter.api.extension.*;

/** Evidência limitada de foco e sessão; observa AWT sem consumir nem criar eventos. */
final class GuiInputTrace implements BeforeEachCallback, AfterTestExecutionCallback, AfterEachCallback, ExecutionCondition {
    private final ArrayDeque<String> events = new ArrayDeque<>();
    private final AWTEventListener listener = this::record;
    private long started;
    /** Permite isolar uma invocação parametrizada sem alterar ações ou asserções. */
    @Override public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        String invocation = System.getProperty("spiritgate.test.guiInvocation");
        String id = context.getUniqueId();
        if (invocation != null && id.contains("test-template-invocation:")) {
            return id.endsWith("[test-template-invocation:#" + invocation + "]")
                    ? ConditionEvaluationResult.enabled("Invocação selecionada para diagnóstico")
                    : ConditionEvaluationResult.disabled("Outra invocação, fora desta rodada isolada");
        }
        return ConditionEvaluationResult.enabled("Seleção padrão");
    }
    private static GuiInputTrace current;
    private final java.beans.PropertyChangeListener focusChanges = change -> {
        if (java.util.Set.of("focusOwner", "focusedWindow", "activeWindow").contains(change.getPropertyName())) {
            append("property=" + change.getPropertyName() + " before=" + describe(change.getOldValue())
                    + " after=" + describe(change.getNewValue()));
        }
    };

    /** Resume identidade e condições reais do componente sem solicitar foco. */
    private static String describe(Object value) {
        if (!(value instanceof Component c)) return String.valueOf(value);
        return c.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(c))
                + ":" + c.getName() + " showing=" + c.isShowing() + " enabled=" + c.isEnabled()
                + " focusable=" + c.isFocusable() + " displayable=" + c.isDisplayable();
    }

    /** Registra o retorno da chamada existente; não acrescenta pedidos ou tentativas. */
    static void focusRequest(Component component, boolean accepted) {
        if (current != null) current.append("requestFocusInWindow accepted=" + accepted
                + " target=" + describe(component));
    }

    /** Mantém buffer limitado e relógio UTC correlacionável com o observador Windows. */
    private void append(String detail) {
        if (events.size() == 256) events.removeFirst();
        var manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        events.addLast(java.time.Instant.now() + " " + (System.nanoTime() - started) / 1_000_000
                + "ms pid=" + ProcessHandle.current().pid() + " " + Thread.currentThread().getName()
                + " " + detail + " focus=" + describe(manager.getFocusOwner())
                + " active=" + describe(manager.getActiveWindow())
                + " focusedWindow=" + describe(manager.getFocusedWindow()));
    }

    /** Instala o observador somente durante uma invocação do teste gráfico. */
    @Override public void beforeEach(ExtensionContext context) throws Exception {
        started = System.nanoTime();
        SwingUtilities.invokeAndWait(() -> {
            current = this;
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener(focusChanges);
            Toolkit.getDefaultToolkit().addAWTEventListener(listener,
                    AWTEvent.FOCUS_EVENT_MASK | AWTEvent.WINDOW_EVENT_MASK
                    | AWTEvent.WINDOW_FOCUS_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
            snapshot("início " + context.getDisplayName());
        });
    }

    /** Preserva eventos recentes com a thread e o tempo relativo de cada observação. */
    private void record(AWTEvent event) {
        append(event + state(event));
    }

    /** Anexa o estado no instante do evento, antes de o finally destruir as janelas. */
    private String state(AWTEvent event) {
        Component source = event.getSource() instanceof Component component ? component : null;
        Window window = source instanceof Window value ? value
                : source == null ? null : SwingUtilities.getWindowAncestor(source);
        if (!(window instanceof JFrame frame)) return "";
        var root = frame.getRootPane();
        var client = (LocalClient) root.getClientProperty("networkClient");
        var controls = (NetworkSkillControls) root.getClientProperty("networkSkillControls");
        var router = (GameInputRouter) root.getClientProperty("networkInputRouter");
        var panels = (GamePanels) root.getClientProperty("networkPanels");
        if (client == null) return "";
        var snapshot = client.snapshot();
        var actor = snapshot == null ? null : snapshot.actors().stream()
                .filter(value -> value.id() == client.id()).findFirst().orElse(null);
        var viewport = root.getClientProperty("networkPanel");
        return " frame=" + describe(frame) + " frameState=" + frame.getExtendedState()
                + " visible=" + frame.isVisible() + " viewport=" + describe(viewport)
                + " windowListeners=" + frame.getWindowListeners().length
                + " connected=" + client.connected() + " selected=" + client.selectedTarget()
                + " blocked=" + client.revisions().blocked() + " actor=" + actor
                + " sheet=" + (client.playerSheet() == null ? null : client.playerSheet().progress())
                + " energy=" + client.energyView() + " skills=" + client.skills()
                + " routerClosed=" + (router == null ? null : router.isClosed())
                + " modal=" + (panels == null ? null : panels.modal())
                + " aiming=" + (controls == null ? null : controls.aiming())
                + " feedback=" + (controls == null ? null : controls.feedbackText());
    }

    /** Lê componentes e contexto na EDT, sem alterar disponibilidade ou seleção. */
    private void snapshot(String phase) {
        var manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        System.out.println("[GUI-TRACE] " + phase + " thread=" + Thread.currentThread().getName()
                + " focus=" + manager.getFocusOwner() + " active=" + manager.getActiveWindow());
        for (var window : Window.getWindows()) {
            if (!(window instanceof JFrame frame) || !frame.isDisplayable()) continue;
            var root = frame.getRootPane();
            var client = (LocalClient) root.getClientProperty("networkClient");
            System.out.println("[GUI-TRACE] window=" + frame.getTitle() + " active=" + frame.isActive()
                    + " focused=" + frame.isFocused() + " bounds=" + frame.getBounds());
            if (client == null) continue;
            var router = (GameInputRouter) root.getClientProperty("networkInputRouter");
            var panels = (GamePanels) root.getClientProperty("networkPanels");
            var skills = (NetworkSkillControls) root.getClientProperty("networkSkillControls");
            System.out.println("[GUI-TRACE] connected=" + client.connected() + " region=" + client.region()
                    + " blocked=" + client.revisions().blocked() + " selected=" + client.selectedTarget()
                    + " routerClosed=" + (router == null ? null : router.isClosed())
                    + " modal=" + (panels == null ? null : panels.modal())
                    + " sheet=" + client.playerSheet() + " energy=" + client.energyView()
                    + " skills=" + client.skills() + " snapshot=" + client.snapshot());
            if (skills != null) {
                System.out.println("[GUI-TRACE] aiming=" + skills.aiming() + " feedback=" + skills.feedbackText());
                for (var button : skills.buttons()) System.out.println("[GUI-TRACE] slot=" + button.getName()
                        + " enabled=" + button.isEnabled() + " showing=" + button.isShowing()
                        + " focusable=" + button.isFocusable() + " tooltip=" + button.getToolTipText());
            }
        }
    }

    /** Em falha, inclui a sequência anterior; o modo explícito também registra sucessos. */
    @Override public void afterTestExecution(ExtensionContext context) throws Exception {
        if (context.getExecutionException().isPresent() || Boolean.getBoolean("spiritgate.test.guiTrace")) {
            SwingUtilities.invokeAndWait(() -> {
                snapshot("fim " + context.getDisplayName() + " failure=" + context.getExecutionException());
                events.forEach(line -> System.out.println("[GUI-AWT] " + line));
            });
        }
    }

    /** Remove o listener mesmo quando o teste ou seu encerramento falham. */
    @Override public void afterEach(ExtensionContext context) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Toolkit.getDefaultToolkit().removeAWTEventListener(listener);
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener(focusChanges);
            current = null;
        });
        events.clear();
    }
}
