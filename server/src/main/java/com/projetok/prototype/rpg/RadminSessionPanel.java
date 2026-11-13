package com.projetok.prototype.rpg;

import com.projetok.multiplayer.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.Callable;

/** Entrada modal da sessao; transfere servidor e jogador para a janela da partida. */
public final class RadminSessionPanel extends JPanel {
    private final JComboBox<String> source = new JComboBox<>();
    private final JTextField host = new JTextField("26.", 18), port = new JTextField("54557", 6);
    private final JLabel status = new JLabel("Carregando personagem e adaptadores..."), vpn = new JLabel("Verificando adaptadores...");
    private final JPanel characterHolder = new JPanel(new BorderLayout());
    private final JButton start, connect, join;
    private final JDialog dialog;
    private final Window owner;
    private final boolean hosting;
    private final Path project;
    private final Timer timer;
    private LocalSimulationServer server;
    private boolean busy, closed, confirmingClose;
    private long workGeneration;
    private final java.util.ArrayDeque<Runnable> deferredCompletions = new java.util.ArrayDeque<>();
    private Resources resources;
    private SessionCharacterCreation character;

    private record Resources(CosmeticCatalog catalog, RpgPlayerSprites sprites,
                             NetworkPlayerVisuals visuals, CosmeticCatalog previewCatalog) { }
    private record Initial(Resources resources, List<RadminAccess.Adapter> adapters) { }
    private record Connection(LocalClient client, Resources resources, String endpoint) implements AutoCloseable {
        @Override public void close() { client.close(); }
    }

    /** Mantem o ponto de entrada historico sem bloquear a chamada da EDT. */
    public static void open(boolean hosting) { open(null, hosting, "Viajante"); }

    /** Abre um unico modal pertencente a tela de entrada. */
    public static void open(Window owner, boolean hosting, String initialName) {
        if (hosting && Boolean.getBoolean("spiritgate.guest")) throw new IllegalStateException("Modo convidado nao hospeda");
        Runnable show = () -> {
            if (owner != null) for (Window window : owner.getOwnedWindows()) {
                if (window instanceof JDialog existing && existing.isDisplayable()
                        && existing.getContentPane() instanceof RadminSessionPanel) {
                    existing.toFront(); return;
                }
            }
            new RadminSessionPanel(owner, hosting, initialName);
        };
        if (SwingUtilities.isEventDispatchThread()) show.run(); else SwingUtilities.invokeLater(show);
    }

    private RadminSessionPanel(Window owner, boolean hosting, String initialName) {
        super(new BorderLayout(8, 10));
        this.owner = owner; this.hosting = hosting; project = RpgMenu.home();
        dialog = new JDialog(owner, "Spirit Gate | " + (hosting ? "Hospedar partida" : "Entrar no mundo"),
                owner == null ? Dialog.ModalityType.APPLICATION_MODAL : Dialog.ModalityType.DOCUMENT_MODAL);
        dialog.setName("radminSessionDialog");
        setBackground(new Color(5, 12, 24)); setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        JPanel connection = new JPanel(new GridBagLayout()); connection.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints(); c.gridx = 0; c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(3, 4, 3, 4);
        row(connection, c, new JLabel(hosting ? "HOSPEDAR PARTIDA" : "ENTRAR NO MUNDO"));
        row(connection, c, vpn); row(connection, c, new JLabel("Adaptador / meu IPv4 Radmin"));
        source.setName("radminSource"); row(connection, c, source);
        if (!hosting) { row(connection, c, new JLabel("IP do anfitriao")); host.setName("radminHost"); row(connection, c, host); }
        row(connection, c, new JLabel("Porta | capacidade: 2 jogadores")); port.setName("radminPort"); row(connection, c, port);
        add(connection, BorderLayout.NORTH);
        characterHolder.setOpaque(false); characterHolder.add(new JLabel("Carregando previa do personagem...", SwingConstants.CENTER));
        JScrollPane scroll = new JScrollPane(characterHolder); scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(getBackground()); scroll.getVerticalScrollBar().setUnitIncrement(20); add(scroll);
        JPanel footer = new JPanel(new BorderLayout(4, 8)); footer.setOpaque(false);
        status.setForeground(new Color(180, 230, 240)); status.setName("sessionStatus"); footer.add(status, BorderLayout.NORTH);
        JPanel actions = new JPanel(new GridLayout(0, 2, 6, 6)); actions.setOpaque(false);
        start = button(actions, "Iniciar servidor", this::start);
        join = button(actions, "Entrar com este personagem", () -> connect(true));
        connect = button(actions, "Conectar com este personagem", () -> connect(false));
        start.setVisible(hosting); join.setVisible(hosting); connect.setVisible(!hosting);
        if (hosting) actions.remove(connect); else { actions.remove(start); actions.remove(join); }
        if (hosting) {
            button(actions, "World Studio", this::openStudio);
            button(actions, "Encerrar servidor", () -> requestStop(false));
            button(actions, "Recuperar publicacao interrompida", () -> {
                if (server != null || busy) return;
                if (JOptionPane.showConfirmDialog(dialog, "Recuperar ponteiro anterior? O journal sera preservado.",
                        "Recuperar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                    work(() -> { new com.projetok.studio.LivePublishing(LauncherSession.revisionRoot(project, port(port.getText()))).recoverPreviousPointer(); return "Ponteiro anterior recuperado"; }, status::setText);
            });
        }
        button(actions, "Copiar IP", () -> { Object ip = source.getSelectedItem(); if (ip != null) Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(ip.toString()), null); });
        button(actions, "Atualizar adaptadores", this::inspect);
        button(actions, "Abrir Radmin VPN", () -> work(() -> {
            Path executable = radminExecutable();
            if (executable == null) throw new IllegalStateException("Radmin nao instalado nos caminhos conhecidos. Use o site oficial.");
            new ProcessBuilder(executable.toString()).start(); return "Radmin aberto";
        }, status::setText));
        button(actions, "Site oficial Radmin VPN", () -> work(() -> { Desktop.getDesktop().browse(URI.create("https://www.radmin-vpn.com/")); return "Site oficial aberto"; }, status::setText));
        button(actions, "Voltar", this::leave);
        footer.add(actions); add(footer, BorderLayout.SOUTH);
        dialog.setContentPane(this); dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        dialog.getRootPane().setDefaultButton(hosting ? join : connect);
        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "cancelSession");
        dialog.getRootPane().getActionMap().put("cancelSession", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { leave(); }
        });
        dialog.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) { leave(); }
            @Override public void windowClosed(WindowEvent event) { if (!closed) leave(); }
        });
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        dialog.setSize(Math.min(980, screen.width - 50), Math.min(880, screen.height - 70));
        dialog.setMinimumSize(new Dimension(600, 520)); dialog.setLocationRelativeTo(owner);
        timer = new Timer(250, e -> {
            start.setEnabled(!busy && !confirmingClose && resources != null && server == null);
            join.setEnabled(!busy && !confirmingClose && resources != null && server != null && server.running());
            connect.setEnabled(!busy && !confirmingClose && resources != null); source.setEnabled(!busy && !confirmingClose && server == null);
            port.setEditable(!busy && !confirmingClose && server == null);
            if (!busy && server != null && !server.running()) status.setText("Servidor interrompido: " + server.failure());
        }); timer.start();
        work(() -> new Initial(loadResources(), RadminAccess.inspect()), initial -> {
            resources = initial.resources(); character = new SessionCharacterCreation(resources.visuals(), resources.catalog(), resources.previewCatalog());
            character.setPlayerName(initialName == null || initialName.isBlank() ? "Viajante" : initialName);
            characterHolder.removeAll(); characterHolder.add(character); characterHolder.revalidate(); characterHolder.repaint();
            showAdapters(initial.adapters()); status.setText("Escolha o personagem desta sessao; nenhum save pessoal sera gravado");
        });
        // setVisible modal usa um loop de eventos proprio; agendar evita bloquear o chamador.
        SwingUtilities.invokeLater(() -> { if (!closed) dialog.setVisible(true); });
    }

    private static void row(JPanel panel, GridBagConstraints c, JComponent component) {
        c.gridy++; if (component instanceof JLabel) component.setForeground(new Color(190, 235, 245));
        if (component instanceof JTextField field) SpiritGateEntry.styleField(field); panel.add(component, c);
    }
    private JButton button(JPanel panel, String title, Runnable action) {
        JButton b = new CharacterSelection.SpiritButton(title, false); b.addActionListener(e -> action.run()); panel.add(b); return b;
    }
    private <T> void work(Callable<T> task, java.util.function.Consumer<T> success) {
        if (busy || closed || confirmingClose) return;
        long generation = ++workGeneration;
        busy = true;
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return task.call(); }
            @Override protected void done() {
                try {
                    T value = get();
                    finishWork(() -> {
                        try {
                            if (closed || generation != workGeneration) {
                                if (value instanceof AutoCloseable resource) resource.close();
                                return;
                            }
                            busy = false;
                            success.accept(value);
                        } catch (Exception error) {
                            if (!closed && generation == workGeneration) status.setText(problemMessage(error));
                        }
                    });
                } catch (Exception error) {
                    finishWork(() -> {
                        if (closed || generation != workGeneration) return;
                        busy = false;
                        status.setText(problemMessage(error));
                    });
                }
            }
        }.execute();
    }
    private void finishWork(Runnable completion) {
        // A confirmacao Swing mantem a EDT ativa; o resultado espera pela decisao do usuario.
        if (confirmingClose) deferredCompletions.addLast(completion); else completion.run();
    }
    static String problemMessage(Throwable error) {
        Throwable cause = error;
        while ((cause instanceof java.util.concurrent.ExecutionException || cause instanceof java.util.concurrent.CompletionException)
                && cause.getCause() != null) cause = cause.getCause();
        return cause instanceof BindException ? "Porta ocupada - escolha outra porta" :
                cause instanceof SocketTimeoutException || cause instanceof java.util.concurrent.TimeoutException ? "Tempo limite excedido" :
                cause instanceof ConnectException ? "Servidor indisponivel" :
                java.util.Objects.toString(cause.getMessage(), "Nao foi possivel concluir a operacao");
    }
    private void inspect() { work(RadminAccess::inspect, this::showAdapters); }
    private void showAdapters(List<RadminAccess.Adapter> adapters) {
        Object previous = source.getSelectedItem(); source.removeAllItems();
        for (var adapter : adapters) if (adapter.active()) for (var ip : adapter.addresses()) source.addItem(ip.getHostAddress());
        boolean retained = false;
        for (int index = 0; index < source.getItemCount(); index++) if (java.util.Objects.equals(previous, source.getItemAt(index))) retained = true;
        if (retained) source.setSelectedItem(previous); else source.setSelectedIndex(-1);
        vpn.setText(source.getItemCount() == 0 ? "Adaptador Radmin ativo nao encontrado" : "Selecione seu IPv4 Radmin");
        vpn.setToolTipText("O adaptador nao garante a mesma rede VPN; o handshake confirma a partida.");
    }
    private InetAddress selected() throws Exception {
        Object ip = source.getSelectedItem(); if (ip == null) throw new IllegalArgumentException("Selecione um IPv4 Radmin ativo");
        return RadminAccess.requireLocal(ipv4(ip.toString()));
    }
    public static InetAddress ipv4(String value) throws Exception {
        if (!value.matches("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}")) throw new IllegalArgumentException("IPv4 invalido");
        for (String part : value.split("\\.")) if (Integer.parseInt(part) > 255) throw new IllegalArgumentException("IPv4 invalido");
        return InetAddress.getByName(value);
    }
    public static int port(String value) {
        try { int p = Integer.parseInt(value); if (p >= 1 && p <= 65535) return p; } catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException("Porta deve ser 1..65535");
    }
    private Resources loadResources() throws Exception {
        var catalog = CosmeticCatalog.load(project.resolve("assets/dev/generated/corujao-iv-v1"));
        var sprites = RpgPlayerSprites.loadOptional(project.resolve("assets/dev/player-base"), 250, .5);
        var visuals = new NetworkPlayerVisuals(project.resolve("assets/dev/reaper"), sprites);
        return new Resources(catalog, sprites, visuals, catalog.onLogicalCanvas(64, 64, 32, 60, NetworkPlayerVisuals.LEGACY_SCALE));
    }
    private void start() {
        if (server != null || busy || resources == null) return;
        try {
            InetAddress ip = selected(); int number = port(port.getText()); status.setText("Iniciando servidor...");
            work(() -> LauncherSession.startExpanded(ip, number, resources.catalog(), project), next -> {
                server = next; status.setText("Servidor pronto - entre com o personagem escolhido");
            });
        } catch (Exception error) { status.setText(error.getMessage()); }
    }
    private void connect(boolean own) {
        if (busy || resources == null || character == null) return;
        try {
            if (own && (server == null || !server.running())) throw new IllegalStateException("Inicie o servidor primeiro");
            SessionCharacterSelection choice = character.selection();
            InetAddress local = selected(), destination = own ? local : ipv4(host.getText().strip());
            if (!RadminAccess.address(destination)) throw new IllegalArgumentException("Destino exige IPv4 Radmin 26.x.x.x");
            int number = own ? server.port() : port(port.getText());
            status.setText("Conectando com o personagem selecionado...");
            work(() -> new Connection(LocalClient.skillGame(destination, number, choice, false, true, local,
                    com.projetok.studio.AssetCatalog.scan(project, null)), resources,
                    destination.getHostAddress() + ":" + number), this::enterGame);
        } catch (Exception error) { status.setText(error.getMessage()); }
    }
    private void enterGame(Connection connection) {
        LocalClient next = connection.client();
        try {
            var lifecycle = new SessionGameLifecycle(server, project);
            JFrame game = RpgNetworkClientLauncher.open(next, connection.resources().sprites(), connection.endpoint(),
                    connection.resources().catalog(), next.characterSelection().cosmetics(), lifecycle::adopt,
                    new RpgNetworkClientLauncher.Presentation(connection.resources().visuals(), connection.resources().previewCatalog()));
            // A janela da partida agora possui o servidor; fechar as telas de entrada nao o encerra.
            server = null; closed = true; timer.stop();
            SessionWindowNavigation.enterGame(game, dialog);
        } catch (RuntimeException error) { next.close(); throw error; }
    }
    private void openStudio() {
        if (busy || closed || confirmingClose) return;
        try {
            if (server == null) throw new IllegalStateException("Inicie o servidor primeiro");
            if (JOptionPane.showConfirmDialog(dialog, "Autorizar World Studio neste servidor local?", "Autorizar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                LauncherSession.openStudio(server, project);
        } catch (Exception error) { status.setText(error.getMessage()); }
    }
    private boolean confirmStop() {
        return server == null || JOptionPane.showConfirmDialog(dialog, "Encerrar servidor e desconectar participantes?", "Encerrar sessao", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
    private void stop() {
        workGeneration++; busy = false;
        if (server != null) { server.close(); server = null; }
        status.setText("Servidor desligado");
    }
    private void requestStop(boolean closeDialog) {
        if (closed || confirmingClose || !closeDialog && server == null) return;
        confirmingClose = true;
        try {
            if (!confirmStop()) return;
            if (closeDialog) { closed = true; timer.stop(); }
            stop();
            if (closeDialog) dialog.dispose();
        } finally {
            confirmingClose = false;
            var pending = List.copyOf(deferredCompletions); deferredCompletions.clear();
            for (Runnable completion : pending) completion.run();
        }
    }
    private void leave() { requestStop(true); }
    private static Path radminExecutable() {
        for (String key : List.of("ProgramFiles(x86)", "ProgramFiles")) {
            String folder = System.getenv(key); if (folder == null) continue;
            Path path = Path.of(folder, "Radmin VPN", "RvRvpnGui.exe"); if (Files.isRegularFile(path)) return path;
        }
        return null;
    }
}
