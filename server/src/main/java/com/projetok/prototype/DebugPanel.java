package com.projetok.prototype;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Supplier;

/** Desenha uma visão descartável do mundo usando apenas Java2D (sem sprites externos). */
public final class DebugPanel extends JPanel {
    public static final int PANEL_WIDTH = DebugWorld.WIDTH + 300;
    public static final int PANEL_HEIGHT = DebugWorld.HEIGHT + 110;
    private static final Color BG = new Color(14, 22, 39);
    private static final Color TEXT = new Color(229, 239, 248);
    private static final Color ACCENT = new Color(70, 219, 235);
    private final Supplier<DebugSnapshot> source;
    private boolean showHitboxes;
    private boolean showHelp;
    private float ambientLight=1f;
    private java.util.List<String> customHud=java.util.List.of();
    private java.util.List<String> customFooter=java.util.List.of();
    /** Mensagens opcionais de modo experimental; não altera HUDs históricos por padrão. */
    public void setCustomFooter(String first,String second){customFooter=java.util.List.of(first,second);}
    private java.util.function.IntFunction<Color> actorPalette;
    public void setCustomHud(java.util.List<String> lines){customHud=java.util.List.copyOf(lines);}
    public void setActorPalette(java.util.function.IntFunction<Color> palette){actorPalette=palette;}
    /** Opcao visual explicitamente desativada por padrao na arena historica. */
    public void setAmbientLight(float value) {
        if(!Float.isFinite(value)||value<0.5f||value>1f)throw new IllegalArgumentException("Luz invalida");ambientLight=value;
    }
    /** Alterna ajuda sem pausar ou enviar comandos ao mundo. */
    public void toggleHelp() { showHelp=!showHelp;repaint(); }
    public boolean isHelpVisible() { return showHelp; }
    boolean hasLocalMap() { return mapRenderer!=null; }
    /** Texto de apresentação usa somente os valores já publicados pela simulação. */
    static java.util.List<String> helpLines(DebugSnapshot snapshot) {
        return java.util.List.of("SPIRIT GATE - AJUDA (F1 para fechar)",
                "WASD: mover | ESPACO: golpe | E: vigor fisico",
                "SHIFT: Passo Relampago | Q: Criostase | mouse: mirar na arena",
                "R: reiniciar | H: hitboxes | F1: ajuda | F2: diagnostico local",
                "HP: vida | VIGOR: recurso para acoes e habilidades",
                "SHIFT atravessa obstaculos leves; solidos e bordas interrompem.",
                "Congelamento e recargas aparecem no HUD; mire fora do HUD.",
                "HP "+snapshot.playerHp()+"/"+snapshot.playerMaxHp()+" | VIGOR "+Math.round(snapshot.stamina())+"/"+Math.round(snapshot.maxStamina()),
                "Q: "+snapshot.cryostasisHud().stateText()+" | SHIFT: "+snapshot.lightningStepHud().stateText());
    }
    private Supplier<DirectionalAnimation.Presentation> presentationSource;
    private ValidatedSpriteAtlas spriteAtlas;
    /** Optional presentation hook. Returning false preserves the existing renderer. */
    @FunctionalInterface public interface ActorRenderer {
        boolean draw(Graphics2D graphics, DebugSnapshot.ActorView actor);
    }
    private ActorRenderer actorRenderer;
    private java.util.function.Consumer<Graphics2D> worldOverlay;
    private java.util.function.Consumer<Graphics2D> worldUnderlay;
    private boolean physicalGeometry=true;
    /** Camada opcional de autoria, desativada nas entradas historicas. */
    public void setWorldUnderlay(java.util.function.Consumer<Graphics2D> underlay){worldUnderlay=underlay;}
    /** Oculta apenas o desenho diagnostico, nunca a colisao da simulacao. */
    public void setPhysicalGeometryVisible(boolean value){physicalGeometry=value;}
    /** Overlay opcional do RPG; nulo preserva a arena e o Coop. */
    public void setWorldOverlay(java.util.function.Consumer<Graphics2D> overlay){worldOverlay=overlay;}
    public void setActorRenderer(ActorRenderer renderer) { actorRenderer=renderer; }
    private LocalArenaMap.Renderer mapRenderer;
    private int cameraZoom=1;
    private int worldWidth=DebugWorld.WIDTH, worldHeight=DebugWorld.HEIGHT;
    private boolean culling=true;
    /** Referência de comparação visual; não altera os dados da simulação. */
    public void setCulling(boolean enabled) { culling=enabled; }
    private boolean visible(Graphics2D g,double x,double y,double w,double h) {
        return !culling || g.hitClip((int)Math.floor(x),(int)Math.floor(y),
                (int)Math.ceil(w)+2,(int)Math.ceil(h)+2);
    }

    /** Zoom inteiro opcional; padrao preserva coordenadas historicas. */
    public void setCameraZoom(int zoom) {
        if(zoom<1 || zoom>4) throw new IllegalArgumentException("Zoom invalido");
        cameraZoom=zoom; repaint();
    }
    ArenaViewport viewport(DebugSnapshot snapshot) {
        var player=snapshot.actors().stream().filter(DebugSnapshot.ActorView::player).findFirst();
        double x=player.map(a->(double)a.x()).orElse(480d),y=player.map(a->(double)a.y()).orElse(320d);
        return ArenaViewport.fit(getWidth()>0?getWidth():PANEL_WIDTH,getHeight()>0?getHeight():PANEL_HEIGHT,
                worldWidth,worldHeight,x,y,cameraZoom);
    }
    /** Conversao no instante do comando evita mira obsoleta se a camera se moveu. */
    java.awt.geom.Point2D.Double aimAt(float x,float y) {
        var v=viewport(source.get());
        return v.containsScreen(x,y)?v.screenToWorld(x,y):new java.awt.geom.Point2D.Double(Double.NaN,Double.NaN);
    }

    /** Seletor visual opcional de demonstracao, sem modificar o mundo a partir da imagem. */
    public void setLocalMap(LocalArenaMap map) {
        setLocalMap(map,true);
    }
    public void setLocalMap(LocalArenaMap map,boolean diagnosticTiles) {
        mapRenderer = map == null ? null : map.renderer(diagnosticTiles);
        worldWidth = map == null ? DebugWorld.WIDTH : map.width();
        worldHeight = map == null ? DebugWorld.HEIGHT : map.height();
    }

    /** Ativa somente um atlas ja validado; null mantem a geometria original. */
    public void setPresentation(Supplier<DirectionalAnimation.Presentation> source, ValidatedSpriteAtlas atlas) {
        presentationSource = source;
        spriteAtlas = atlas;
    }

    public DebugPanel(Supplier<DebugSnapshot> source) {
        this.source = source;
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setBackground(BG);
        setDoubleBuffered(true);
        setFocusable(true);
    }

    /** Permite inspecionar raio físico e alcance do ataque sem alterar a simulação. */
    public void setShowHitboxes(boolean visible) {
        this.showHitboxes = visible;
        repaint();
    }

    public boolean isShowHitboxes() { return showHitboxes; }

    @Override
    protected void paintComponent(Graphics original) {
        super.paintComponent(original);
        Graphics2D g = (Graphics2D) original.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (presentationSource == null) render(g, source.get());
            else render(g, presentationSource.get());
        } finally {
            g.dispose();
        }
    }

    /** Renderização pura a partir de um snapshot: também é utilizada no smoke visual. */
    public void render(Graphics2D g, DebugSnapshot s) {
        renderScene(g, s, java.util.List.of());
    }

    /** Desenha valores do mesmo tick; o render nunca avanca o sequenciador. */
    public void render(Graphics2D g, DirectionalAnimation.Presentation presentation) {
        renderScene(g, presentation.world(), presentation.animations());
    }

    private void renderScene(Graphics2D g, DebugSnapshot s, java.util.List<DirectionalAnimation.View> animations) {
        var viewport=viewport(s);
        Graphics2D canvas=(Graphics2D)g.create();
        try {
        canvas.translate(viewport.offsetX(),viewport.offsetY());
        canvas.scale(viewport.scale(),viewport.scale());
        canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        Graphics2D world=(Graphics2D)canvas.create();
        try {
        world.clipRect(0,0,DebugWorld.WIDTH,DebugWorld.HEIGHT);
        world.scale(viewport.zoom(),viewport.zoom());
        world.translate(-viewport.originX(),-viewport.originY());
        renderWorld(world,s,animations);
        if(ambientLight<1f){world.setColor(new Color(8,12,35,Math.round((1f-ambientLight)*255)));world.fillRect(0,0,worldWidth,worldHeight);}
        } finally {world.dispose();}
        if(customHud.isEmpty()){drawSidebar(canvas,s);drawBottom(canvas,s);}
        else {
            canvas.setColor(new Color(12,19,35));canvas.fillRect(960,0,300,750);canvas.fillRect(0,640,960,110);
            canvas.setFont(new Font("SansSerif",Font.BOLD,15));canvas.setColor(TEXT);int lineY=32;
            for(String line:customHud){canvas.drawString(line,978,lineY);lineY+=26;}
            canvas.setColor(ACCENT);canvas.drawString(s.lastEvent(),20,674);canvas.setColor(TEXT);canvas.drawString(customFooter.isEmpty()?"WASD explorar | Use a hotbar e os atalhos configurados na barra superior.":customFooter.get(0),20,704);
            canvas.drawString(customFooter.isEmpty()?(s.gameOver()?"Voce caiu. Pressione R para recuperar na vila.":"Explore, converse com a Ancia e prepare-se para o Santuario."):customFooter.get(1),20,730);
        }
        if(showHelp) {
            canvas.setColor(new Color(8,15,28,240));canvas.fillRoundRect(20,20,920,330,20,20);
            canvas.setColor(TEXT);canvas.setFont(new Font("SansSerif",Font.BOLD,16));
            int y=55;for(String line:helpLines(s)) {canvas.drawString(line,40,y);y+=31;}
        }
        } finally {canvas.dispose();}
    }

    private void renderWorld(Graphics2D g,DebugSnapshot s,java.util.List<DirectionalAnimation.View> animations) {
        drawWorld(g);
        if (mapRenderer != null) mapRenderer.drawLayers(g,0,4);
        if(worldUnderlay!=null)worldUnderlay.accept(g);
        if(physicalGeometry){drawObstacles(g, s);drawSoftObstacles(g, s);}
        drawIceZones(g, s);
        drawLightningTrails(g, s);
        for (DebugSnapshot.ActorView actor : s.actors()) {
            // Margem inclui nome, barras, alcance, sprite/pivot e marca de Freeze.
            if(spriteAtlas==null && !visible(g,actor.x()-100,actor.y()-100,200,200)) continue;
            DirectionalAnimation.View visual = null;
            for (var candidate : animations) if (candidate.actorId() == actor.id()) { visual=candidate; break; }
            if (actorRenderer != null && actorRenderer.draw(g, actor)) continue;
            if (spriteAtlas == null || visual == null || !spriteAtlas.draw(g, actor, visual)) drawActor(g, actor, s);
        }
        if (mapRenderer != null) mapRenderer.drawLayers(g,5,10);
        drawAttack(g, s.attack());
        drawEffects(g, s);
        if(worldOverlay!=null)worldOverlay.accept(g);
    }

    // --- Cenário ---

    /** Rastro amarelo com desvanecimento a partir dos tempos publicados, sem timers da GUI. */
    private void drawLightningTrails(Graphics2D original, DebugSnapshot snapshot) {
        Graphics2D g = (Graphics2D) original.create();
        try {
            g.clipRect(0, 0, worldWidth, worldHeight);
            for (var trail : snapshot.lightningTrails()) {
                float alpha = Math.max(0, Math.min(1, trail.remaining() / trail.duration()));
                double dx = trail.x1() - trail.x0(), dy = trail.y1() - trail.y0();
                double length = Math.hypot(dx, dy);
                if (length == 0) continue;
                Path2D bolt = new Path2D.Double();
                bolt.moveTo(trail.x0(), trail.y0());
                for (int i = 1; i < 6; i++) {
                    double fraction = i / 6.0, offset = (i % 2 == 0 ? -1 : 1) * 3;
                    bolt.lineTo(trail.x0() + fraction * dx - dy / length * offset,
                            trail.y0() + fraction * dy + dx / length * offset);
                }
                bolt.lineTo(trail.x1(), trail.y1());
                g.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(255, 205, 30, Math.round(alpha * 55)));
                g.draw(bolt);
                g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(255, 240, 110, Math.round(alpha * 245)));
                g.draw(bolt);
            }
        } finally { g.dispose(); }
    }

    /** Desenha gelo e pulsação apenas a partir do relógio publicado pela simulação. */
    private void drawIceZones(Graphics2D original, DebugSnapshot snapshot) {
        Graphics2D g = (Graphics2D) original.create();
        try {
            g.clipRect(0, 0, worldWidth, worldHeight);
            for (var zone : snapshot.iceZones()) {
                float r = zone.radius(), x = zone.x(), y = zone.y();
                float pulse = (float) (0.5 + 0.5 * Math.cos(zone.elapsed() * Math.PI * 4));
                g.setColor(new Color(125, 220, 255, 45 + Math.round(pulse * 20)));
                g.fill(new Ellipse2D.Float(x - r, y - r, r * 2, r * 2));
                g.setColor(new Color(190, 245, 255, 210));
                g.setStroke(new BasicStroke(3f));
                g.draw(new Ellipse2D.Float(x - r, y - r, r * 2, r * 2));
                g.setStroke(new BasicStroke(1.5f));
                g.setColor(new Color(165, 235, 255, 90));
                float ring = r * (0.65f + pulse * 0.15f);
                g.draw(new Ellipse2D.Float(x - ring, y - ring, ring * 2, ring * 2));
                for (int i = 0; i < 12; i++) {
                    double angle = i * Math.PI / 6;
                    float cx = x + (float) Math.cos(angle) * r * 0.92f;
                    float cy = y + (float) Math.sin(angle) * r * 0.92f;
                    g.drawLine(Math.round(cx - 4), Math.round(cy), Math.round(cx + 4), Math.round(cy));
                    g.drawLine(Math.round(cx), Math.round(cy - 4), Math.round(cx), Math.round(cy + 4));
                }
            }
        } finally { g.dispose(); }
    }

    /** Pinta solo, grade de referência e bordas da arena. */
    private void drawWorld(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(27, 54, 63),
                worldWidth, worldHeight, new Color(12, 39, 52)));
        g.fillRect(0, 0, worldWidth, worldHeight);
        g.setColor(new Color(255, 255, 255, 12));
        for (int x = 0; x <= worldWidth; x += 40) g.drawLine(x, 0, x, worldHeight);
        for (int y = 0; y <= worldHeight; y += 40) g.drawLine(0, y, worldWidth, y);
        // Lâminas de grama decorativas, sem colisão: os obstáculos sólidos ficam explícitos.
        g.setStroke(new BasicStroke(2f));
        g.setColor(new Color(61, 126, 118, 90));
        for (int x = 28; x < worldWidth; x += 77) {
            for (int y = 32; y < worldHeight; y += 89) {
                g.drawLine(x, y, x - 4, y - 7);
                g.drawLine(x, y, x + 4, y - 6);
            }
        }
        g.setColor(new Color(98, 183, 176));
        g.setStroke(new BasicStroke(4f));
        g.drawRect(1, 1, worldWidth - 2, worldHeight - 2);
    }

    /** Desenha pedras/paredes e destaca suas bordas sólidas no modo de inspeção. */
    private void drawObstacles(Graphics2D g, DebugSnapshot world) {
        for (DebugSnapshot.Obstacle o : world.obstacles()) {
            if(!visible(g,o.x()-3,o.y()-3,o.width()+14,o.height()+16)) continue;
            int x = Math.round(o.x()), y = Math.round(o.y());
            int w = Math.round(o.width()), h = Math.round(o.height());
            g.setColor(new Color(0, 0, 0, 65));
            g.fillRoundRect(x + 8, y + 10, w, h, 18, 18);
            g.setPaint(new GradientPaint(x, y, new Color(86, 107, 116),
                    x + w, y + h, new Color(39, 60, 78)));
            g.fillRoundRect(x, y, w, h, 18, 18);
            g.setColor(new Color(127, 172, 172));
            g.setStroke(new BasicStroke(3f));
            g.drawRoundRect(x, y, w, h, 18, 18);
            g.setColor(new Color(192, 226, 218, 56));
            for (int yy = y + 22; yy < y + h - 10; yy += 32) {
                g.drawLine(x + 18, yy, x + w - 18, yy);
            }
            if (showHitboxes) {
                g.setColor(new Color(255, 190, 35, 200));
                g.setStroke(new BasicStroke(2f));
                g.drawRect(x, y, w, h);
            }
        }
    }

    /** Distingue arbustos e postes leves; H mostra exatamente o retângulo físico publicado. */
    private void drawSoftObstacles(Graphics2D g, DebugSnapshot world) {
        for (var obstacle : world.softObstacles()) {
            if(!visible(g,obstacle.x()-6,obstacle.y()-3,obstacle.width()+18,obstacle.height()+16)) continue;
            int x = Math.round(obstacle.x()), y = Math.round(obstacle.y());
            int width = Math.round(obstacle.width()), height = Math.round(obstacle.height());
            g.setColor(new Color(0, 0, 0, 65));
            g.fillOval(x - 4, y + height - 5, width + 12, 12);
            if (obstacle.kind() == DebugSnapshot.SoftObstacleKind.BUSH) {
                g.setColor(new Color(45, 106, 69));
                g.fillRoundRect(x, y, width, height, 20, 20);
                g.setColor(new Color(81, 157, 86));
                g.fillOval(x, y, width * 2 / 3, height * 2 / 3);
                g.fillOval(x + width / 3, y + height / 5, width * 2 / 3, height * 2 / 3);
                g.setColor(new Color(142, 197, 110));
                g.setStroke(new BasicStroke(1.8f));
                g.drawRoundRect(x, y, width, height, 20, 20);
            } else {
                g.setColor(new Color(120, 86, 53));
                g.fillRoundRect(x, y, width, height, 4, 4);
                g.setColor(new Color(199, 162, 98));
                g.fillRoundRect(x, y, width, 6, 3, 3);
                g.setColor(new Color(224, 192, 137));
                g.setStroke(new BasicStroke(2f));
                g.drawLine(x + 4, y + 8, x + 4, y + height - 4);
            }
            if (showHitboxes) {
                g.setColor(new Color(149, 242, 118));
                g.setStroke(new BasicStroke(2f));
                g.drawRect(x, y, width, height);
            }
        }
        if (!world.softObstacles().isEmpty()) {
            g.setColor(new Color(183, 220, 170));
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g.drawString("OBSTACULOS LEVES  |  WASD bloqueia / SHIFT atravessa / H hitboxes", 34, 491);
        }
    }

    // --- Personagens e indicadores ---

    /** Desenha ator, direção e barra de HP somente a partir de dados publicados. */
    private void drawActor(Graphics2D g, DebugSnapshot.ActorView a, DebugSnapshot s) {
        if (!a.alive()) {
            // Derrota só aparece enquanto existe impacto real publicado nesta posição.
            float remaining=0;
            for(var effect:s.effects()) if(Math.hypot(effect.x()-a.x(),effect.y()-a.y())<=a.radius())
                remaining=Math.max(remaining,effect.remaining());
            if(remaining>0) {
                g.setColor(new Color(190,180,255,Math.min(220,Math.round(remaining*250))));
                g.setStroke(new BasicStroke(3f));
                float r=a.radius()+12+(0.7f-remaining)*24;
                g.draw(new Ellipse2D.Float(a.x()-r,a.y()-r,r*2,r*2));
                g.setFont(new Font("SansSerif",Font.BOLD,12));center(g,"DERROTADO",Math.round(a.x()),Math.round(a.y()-r-5));
            }
            return;
        }
        float x = a.x(), y = a.y();
        float r = a.radius();
        if (a.player()) {
            g.setColor(new Color(70, 216, 240, 30));
            g.fill(new Ellipse2D.Float(x - 32, y - 32, 64, 64));
        }
        g.setColor(new Color(0, 0, 0, 75));
        g.fill(new Ellipse2D.Float(x - r, y + r / 2f, r * 2, r * 0.85f));
        g.setPaint(new GradientPaint(x - r, y - r,
                a.player() ? new Color(104, 233, 255) : new Color(255, 131, 126),
                x + r, y + r,
                a.player() ? new Color(25, 104, 164) : new Color(151, 48, 85)));
        if(actorPalette!=null){Color color=actorPalette.apply(a.id());if(color!=null)g.setPaint(new GradientPaint(x-r,y-r,color.brighter(),x+r,y+r,color.darker()));}
        g.fill(new Ellipse2D.Float(x - r, y - r, r * 2, r * 2));
        g.setColor(a.player() ? new Color(220, 252, 255) : new Color(255, 191, 190));
        g.setStroke(new BasicStroke(2.3f));
        g.draw(new Ellipse2D.Float(x - r, y - r, r * 2, r * 2));
        g.setColor(new Color(236, 253, 255));
        g.fillOval(Math.round(x - 7), Math.round(y - 6), 5, 5);
        g.fillOval(Math.round(x + 4), Math.round(y - 6), 5, 5);
        if (!a.player() && s.statuses().stream().anyMatch(status ->
                status.actorId() == a.id() && status.freezeRemaining() > 0)) {
            drawFrozenActor(g, x, y, r);
        }
        if (a.player()) {
            float fx = s.facingX(), fy = s.facingY();
            g.setColor(new Color(180, 255, 255));
            g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(Math.round(x), Math.round(y),
                    Math.round(x + fx * 31), Math.round(y + fy * 31));
            if (showHitboxes) {
                g.setColor(new Color(118, 235, 255, 90));
                g.setStroke(new BasicStroke(1.4f));
                g.draw(new Ellipse2D.Float(x - DebugWorld.MELEE_REACH,
                        y - DebugWorld.MELEE_REACH,
                        DebugWorld.MELEE_REACH * 2, DebugWorld.MELEE_REACH * 2));
                double angle = Math.atan2(fy, fx);
                double half = Math.PI / 4;
                g.drawLine(Math.round(x), Math.round(y),
                        Math.round(x + (float) Math.cos(angle - half) * DebugWorld.MELEE_REACH),
                        Math.round(y + (float) Math.sin(angle - half) * DebugWorld.MELEE_REACH));
                g.drawLine(Math.round(x), Math.round(y),
                        Math.round(x + (float) Math.cos(angle + half) * DebugWorld.MELEE_REACH),
                        Math.round(y + (float) Math.sin(angle + half) * DebugWorld.MELEE_REACH));
            }
        }
        if (showHitboxes) {
            g.setColor(new Color(255, 220, 64, 210));
            g.setStroke(new BasicStroke(1.6f));
            g.draw(new Ellipse2D.Float(x - r, y - r, r * 2, r * 2));
        }
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.setColor(TEXT);
        center(g, customHud.isEmpty()?(a.player() ? "GUARDIAO" : "ESPIRITO " + (a.id() - 99)):a.name(),
                Math.round(x), Math.round(y - r - 19));
        if(customHud.isEmpty()||a.id()<500)drawBar(g, Math.round(x - 24), Math.round(y - r - 13), 48, 5,
                a.maxHp() == 0 ? 0 : (float) a.hp() / a.maxHp(),
                a.player() ? ACCENT : new Color(243, 99, 109));
    }

    /** Marca Freeze somente com dados publicados; não consulta componentes ou sorteia. */
    private void drawFrozenActor(Graphics2D original, float x, float y, float radius) {
        Graphics2D g = (Graphics2D) original.create();
        try {
            g.setColor(new Color(130, 225, 255, 100));
            g.fill(new Ellipse2D.Float(x - radius, y - radius, radius * 2, radius * 2));
            g.setColor(new Color(185, 245, 255));
            g.setStroke(new BasicStroke(3f));
            g.draw(new Ellipse2D.Float(x - radius - 4, y - radius - 4,
                    (radius + 4) * 2, (radius + 4) * 2));
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            center(g, "CONGELADO", Math.round(x), Math.round(y + radius + 20));
        } finally { g.dispose(); }
    }

    /** Corte visual: varre o cone frontal e desvanece sem aplicar dano. */
    private void drawAttack(Graphics2D original, DebugSnapshot.AttackView attack) {
        if (attack == null) return;
        Graphics2D g = (Graphics2D) original.create();
        try {
            g.clipRect(0, 0, worldWidth, worldHeight);
            g.translate(attack.x(), attack.y());
            g.rotate(Math.atan2(attack.facingY(), attack.facingX()));
            float progress = 1f - attack.remaining() / DebugWorld.ATTACK_DURATION;
            float radius = 42f + 22f * progress;
            int alpha = Math.round(255f * (1f - progress));
            double start = -45 + 30 * progress;
            Arc2D arc = new Arc2D.Float(-radius, -radius, radius * 2, radius * 2,
                    (float) start, 60f, Arc2D.OPEN);
            g.setStroke(new BasicStroke(12f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(70, 219, 235, alpha / 3));
            g.draw(arc);
            g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(225, 255, 255, alpha));
            g.draw(arc);
        } finally {
            g.dispose();
        }
    }

    /** Mostra valores de dano que duram menos de um segundo. */
    private void drawEffects(Graphics2D g, DebugSnapshot s) {
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        for (DebugSnapshot.EffectView e : s.effects()) {
            g.setColor(e.received() ? new Color(255, 130, 100) : new Color(245, 228, 123));
            float radius=8+Math.max(0,.7f-e.remaining())*24;
            g.setStroke(new BasicStroke(2f));
            g.draw(new Ellipse2D.Float(e.x()-radius,e.y()-radius,radius*2,radius*2));
            center(g, "-" + e.damage(), Math.round(e.x()),
                    Math.round(e.y() - (0.7f - e.remaining()) * 42));
        }
    }

    // --- Painéis ---

    /** Exibe HUD, controles, mortes e eventos vindos da simulação. */
    private void drawSidebar(Graphics2D g, DebugSnapshot s) {
        int x = DebugWorld.WIDTH;
        g.setColor(new Color(12, 19, 35));
        g.fillRect(x, 0, 300, PANEL_HEIGHT);
        g.setColor(new Color(46, 87, 96));
        g.drawLine(x, 0, x, PANEL_HEIGHT);
        g.setColor(ACCENT);
        g.setFont(new Font("SansSerif", Font.BOLD, 25));
        g.drawString("SPIRIT GATE", x + 27, 52);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(160, 188, 200));
        g.drawString("PROTOTIPO 2D  /  JAVA 17", x + 29, 72);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(TEXT);
        g.drawString("GUARDIAO", x + 27, 124);
        drawBar(g, x + 27, 138, 239, 15,
                (float) s.playerHp() / s.playerMaxHp(), new Color(61, 205, 176));
        g.setColor(new Color(180, 209, 220));
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.drawString("VIDA  " + s.playerHp() + " / " + s.playerMaxHp(), x + 27, 174);
        drawBar(g, x + 27, 190, 239, 12,
                s.stamina() / s.maxStamina(), new Color(235, 190, 65));
        g.setColor(new Color(235, 210, 145));
        g.drawString(String.format(java.util.Locale.ROOT, "VIGOR  %.1f / %.0f",
                s.stamina(), s.maxStamina()), x + 27, 221);
        g.setColor(new Color(180, 209, 220));
        g.drawString("ESPIRITOS DERROTADOS   " + s.defeated() + " / 3", x + 27, 245);
        g.drawString("SIMULACAO   " + (s.ticks() / 30) + " s", x + 29, 93);
        g.setColor(new Color(46, 76, 94));
        g.drawLine(x + 27, 258, x + 266, 258);
        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.setColor(ACCENT);
        g.drawString("CONTROLES", x + 27, 295);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(TEXT);
        g.drawString("W  A  S  D      Mover", x + 27, 326);
        g.drawString("ESPACO          Atacar", x + 27, 353);
        g.drawString("E                      Esforco (20)", x + 27, 380);
        g.drawString("H                      Hitboxes", x + 27, 407);
        g.drawString("R                      Reiniciar", x + 27, 434);
        g.setColor(new Color(46, 76, 94));
        g.drawLine(x + 27, 455, x + 266, 455);
        g.setColor(new Color(155, 187, 202));
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString("LOG DO SERVIDOR LOCAL", x + 27, 490);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        textLines(g, s.lastEvent(), x + 27, 514, 33);
        if (s.gameOver()) {
            g.setColor(new Color(250, 121, 121));
            g.setFont(new Font("SansSerif", Font.BOLD, 15));
            g.drawString("DERROTADO - TECLA R", x + 27, 612);
        } else if (s.defeated() == 3) {
            g.setColor(new Color(111, 238, 182));
            g.setFont(new Font("SansSerif", Font.BOLD, 15));
            g.drawString("ARENA CONCLUIDA!", x + 27, 612);
        }
    }

    /** Mostra o ataque disponível e o estado atual do cooldown. */
    private void drawBottom(Graphics2D g, DebugSnapshot s) {
        g.setColor(new Color(10, 21, 35));
        g.fillRect(0, DebugWorld.HEIGHT, DebugWorld.WIDTH, 110);
        g.setColor(new Color(66, 105, 118));
        g.drawLine(0, DebugWorld.HEIGHT, DebugWorld.WIDTH, DebugWorld.HEIGHT);
        g.setColor(ACCENT);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.drawString("GOLPE DESARMADO", 25, DebugWorld.HEIGHT + 29);
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.drawString("Alcance 74 px  |  Cone 90 graus  |  Sem custo de vigor", 25,
                DebugWorld.HEIGHT + 52);
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(new Color(205, 211, 171));
        g.drawString("Passo: atravessa atores e obstaculos leves; paredes e bordas bloqueiam.",
                25, DebugWorld.HEIGHT + 67);
        g.setColor(s.cooldown() > 0 ? new Color(245, 171, 108) : new Color(80, 219, 168));
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.drawString(s.cooldown() > 0 ? "RECARGA" : "PRONTO", 770, DebugWorld.HEIGHT + 39);
        drawSkillHud(g, s.cryostasisHud(), DebugWorld.HEIGHT + 83, new Color(175, 235, 255));
        drawSkillHud(g, s.lightningStepHud(), DebugWorld.HEIGHT + 102, new Color(255, 235, 110));
    }

    /** Desenha somente o estado e o retorno já publicados; não decide elegibilidade. */
    private static void drawSkillHud(Graphics2D g, DebugSnapshot.SkillHudView hud, int y, Color accent) {
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.setColor(accent);
        String label = hud.key() + "  " + hud.label() + " | Mouse | " + Math.round(hud.cost()) + " vigor";
        g.drawString(label, 25, y);
        int stateX = 25 + g.getFontMetrics().stringWidth(label) + 16;
        g.setColor(switch (hud.state()) {
            case READY -> new Color(80, 219, 168);
            case COOLDOWN -> new Color(245, 171, 108);
            case INSUFFICIENT_STAMINA, DEAD -> new Color(250, 121, 121);
            case ACTIVE -> new Color(255, 235, 110);
            case AWAITING_LANDING -> new Color(255, 187, 85);
        });
        g.drawString(hud.stateText(), stateX, y);
        int feedbackX = stateX + g.getFontMetrics().stringWidth(hud.stateText()) + 16;
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(new Color(205, 218, 227));
        g.drawString(hud.feedback().message(), feedbackX, y);
    }

    /** Pinta uma barra proporcional, limitando visualmente os valores entre 0 e 1. */
    private static void drawBar(Graphics2D g, int x, int y, int w, int h, float ratio, Color fill) {
        g.setColor(new Color(32, 46, 62));
        g.fillRoundRect(x, y, w, h, h, h);
        g.setColor(fill);
        g.fillRoundRect(x, y, Math.round(w * Math.max(0, Math.min(1, ratio))), h, h, h);
    }

    /** Centraliza texto sobre uma coordenada do mundo. */
    private static void center(Graphics2D g, String text, int centerX, int y) {
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, centerX - metrics.stringWidth(text) / 2, y);
    }

    /** Divide mensagens longas para evitar cortar o log no painel lateral. */
    private static void textLines(Graphics2D g, String text, int x, int y, int lineHeight) {
        int limit = 29;
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (line.length() + word.length() + 1 > limit) {
                g.drawString(line.toString(), x, y);
                y += lineHeight;
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (!line.isEmpty()) g.drawString(line.toString(), x, y);
    }
}
