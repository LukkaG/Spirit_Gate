package com.projetok.prototype.rpg;

import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.Properties;

/** Entrada visual do modo local; nao autentica contas nem recebe senhas. */
public final class SpiritGateEntry extends JPanel {
    static final String ART="/rpg/login/spirit-gate-entry-urban-door-v3.png";
    final JTextField username=new JTextField(20);
    final JCheckBox remember=new JCheckBox("Lembrar perfil local");
    final JButton enter=new SpiritFlameButton();
    final JLabel status=new JLabel("Perfil local | RPG offline",SwingConstants.CENTER);
    private final BufferedImage background;
    SpiritGateEntry(BufferedImage background,Runnable onEnter){
        super(new GridBagLayout());this.background=background;setPreferredSize(new Dimension(1280,720));setBackground(new Color(5,12,24));
        JPanel content=new JPanel();content.setLayout(new BoxLayout(content,BoxLayout.Y_AXIS));content.setOpaque(false);content.setPreferredSize(new Dimension(360,430));
        JComponent emblem=new JComponent(){@Override protected void paintComponent(Graphics original){Graphics2D g=(Graphics2D)original.create();try{g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(new Color(110,230,245));g.setStroke(new BasicStroke(2));int cx=getWidth()/2;g.drawPolygon(new int[]{cx,cx+19,cx,cx-19},new int[]{4,29,54,29},4);g.drawArc(cx-11,15,22,29,0,180);g.drawLine(cx-11,29,cx-11,40);g.drawLine(cx+11,29,cx+11,40);g.drawLine(cx-14,40,cx+14,40);}finally{g.dispose();}}};emblem.setPreferredSize(new Dimension(64,60));emblem.setMaximumSize(new Dimension(400,60));emblem.setAlignmentX(.5f);content.add(emblem);
        JLabel title=new JLabel("SPIRIT GATE",SwingConstants.CENTER);title.setFont(new Font("Serif",Font.BOLD,40));title.setForeground(new Color(225,248,255));title.setAlignmentX(.5f);content.add(title);
        JLabel subtitle=new JLabel("O LIMIAR ENTRE DOIS MUNDOS");subtitle.setFont(new Font("SansSerif",Font.PLAIN,10));subtitle.setForeground(new Color(137,207,222));subtitle.setAlignmentX(.5f);content.add(subtitle);content.add(Box.createVerticalStrut(26));
        JPanel card=new JPanel(new GridBagLayout()){@Override protected void paintComponent(Graphics original){Graphics2D g=(Graphics2D)original.create();try{g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setPaint(new GradientPaint(0,0,new Color(8,26,44,235),0,getHeight(),new Color(3,12,25,215)));g.fillRoundRect(0,0,getWidth()-1,getHeight()-1,12,12);g.setColor(new Color(65,157,185));g.drawRoundRect(0,0,getWidth()-1,getHeight()-1,12,12);g.setColor(new Color(112,225,244));int cx=getWidth()/2;g.drawLine(cx-46,0,cx-8,0);g.drawLine(cx+8,0,cx+46,0);g.drawPolygon(new int[]{cx,cx+4,cx,cx-4},new int[]{0,4,8,4},4);}finally{g.dispose();}}};
        card.setOpaque(false);card.setBorder(BorderFactory.createEmptyBorder(20,22,18,22));
        GridBagConstraints c=new GridBagConstraints();c.gridx=0;c.weightx=1;c.fill=GridBagConstraints.HORIZONTAL;c.insets=new Insets(5,0,5,0);
        JLabel welcome=label("ATRAVESSE O PORTAL");welcome.setHorizontalAlignment(SwingConstants.CENTER);welcome.setFont(new Font("Serif",Font.BOLD,16));addRow(card,c,welcome);
        addRow(card,c,label("Nome do viajante"));styleField(username);username.setToolTipText("Identificador p\u00fablico local; n\u00e3o \u00e9 uma conta online.");addRow(card,c,username);
        username.getAccessibleContext().setAccessibleName("Identificador do perfil local");remember.setMnemonic('L');enter.setMnemonic('E');
        remember.setOpaque(false);remember.setForeground(new Color(187,220,233));remember.setFont(new Font("SansSerif",Font.PLAIN,12));addRow(card,c,remember);
        enter.addActionListener(e->onEnter.run());addRow(card,c,enter);
        status.setForeground(new Color(160,201,219));status.setFont(new Font("SansSerif",Font.PLAIN,11));addRow(card,c,status);card.setMaximumSize(new Dimension(360,card.getPreferredSize().height));content.add(card);add(content);
    }
    static JLabel label(String text){JLabel label=new JLabel(text);label.setForeground(new Color(200,229,239));label.setFont(new Font("SansSerif",Font.PLAIN,12));return label;}
    static void styleField(JTextField field){field.setBackground(new Color(10,27,43));field.setForeground(new Color(224,245,251));field.setCaretColor(Color.CYAN);field.setFont(new Font("SansSerif",Font.PLAIN,14));field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(42,86,108)),BorderFactory.createEmptyBorder(9,10,9,10)));}
    static void addRow(JPanel panel,GridBagConstraints c,JComponent component){c.gridy++;panel.add(component,c);}
    @Override protected void paintComponent(Graphics original){super.paintComponent(original);Graphics2D g=(Graphics2D)original.create();try{
        if(background!=null){double scale=Math.max((double)getWidth()/background.getWidth(),(double)getHeight()/background.getHeight());int w=(int)Math.ceil(background.getWidth()*scale),h=(int)Math.ceil(background.getHeight()*scale);g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.drawImage(background,(getWidth()-w)/2,(getHeight()-h)/2,w,h,null);}
        g.setPaint(new GradientPaint(0,0,new Color(0,5,15,30),0,getHeight(),new Color(0,5,15,130)));g.fillRect(0,0,getWidth(),getHeight());
    }finally{g.dispose();}}
    static BufferedImage loadArt() throws java.io.IOException{SpiritFlameButton.loadArtwork();try(var in=SpiritGateEntry.class.getResourceAsStream(ART)){if(in==null)throw new java.io.IOException("Arte de entrada ausente: "+ART);return ImageIO.read(in);}}
    static void open(){
        new SwingWorker<BufferedImage,Void>(){protected BufferedImage doInBackground()throws Exception{return loadArt();}protected void done(){
            BufferedImage image;try{image=get();}catch(Exception error){JOptionPane.showMessageDialog(null,"Nao foi possivel carregar a arte. Abrindo personagens locais.");RpgMenu.open();return;}
            JFrame frame=new JFrame("Spirit Gate | Portal de entrada");final SpiritGateEntry[] view=new SpiritGateEntry[1];Path settings=RpgMenu.dataHome().resolve("saves/entry.properties");
            view[0]=new SpiritGateEntry(image,()->{
                String name=view[0].username.getText().strip();if(name.length()>24){view[0].status.setText("Use ate 24 caracteres no nome.");return;}
                view[0].enter.setEnabled(false);boolean keep=view[0].remember.isSelected();
                new SwingWorker<Void,Void>(){protected Void doInBackground()throws Exception{Properties p=new Properties();if(keep)p.setProperty("username",name);Files.createDirectories(settings.getParent());Path tmp=Files.createTempFile(settings.getParent(),"entry-",".tmp");try{try(var out=Files.newBufferedWriter(tmp)){p.store(out,"Perfil local; nenhuma senha armazenada");}Files.move(tmp,settings,StandardCopyOption.REPLACE_EXISTING);}finally{Files.deleteIfExists(tmp);}return null;}protected void done(){try{get();System.setProperty("spiritgate.profile.name",name);frame.dispose();RpgMenu.open();}catch(Exception error){view[0].enter.setEnabled(true);view[0].status.setText("Falha ao salvar preferencia local.");}}}.execute();
            });
            new SwingWorker<Properties,Void>(){protected Properties doInBackground()throws Exception{Properties p=new Properties();if(Files.isRegularFile(settings)&&Files.size(settings)<16384)try(var in=Files.newBufferedReader(settings)){p.load(in);}return p;}protected void done(){try{String name=get().getProperty("username","");if(view[0].username.getText().isEmpty()){view[0].username.setText(name);view[0].remember.setSelected(!name.isBlank());}}catch(Exception ignored){view[0].status.setText("Preferencia local indisponivel.");}}}.execute();
            frame.setContentPane(view[0]);frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);frame.getRootPane().setDefaultButton(view[0].enter);frame.pack();var screen=Toolkit.getDefaultToolkit().getScreenSize();frame.setSize(Math.min(1280,screen.width-60),Math.min(760,screen.height-80));frame.setMinimumSize(new Dimension(480,540));frame.setLocationRelativeTo(null);frame.setVisible(true);
        }}.execute();
    }
    /** Exporta a tela real para revisao visual sem iniciar uma sessao de jogo. */
    public static void main(String[] args)throws Exception{if(args.length!=1)throw new IllegalArgumentException("Informe captura.png");BufferedImage bg=loadArt();SwingUtilities.invokeAndWait(()->{try{var panel=new SpiritGateEntry(bg,()->{});panel.setSize(1280,720);layout(panel);BufferedImage image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();panel.printAll(g);g.dispose();ImageIO.write(image,"png",Path.of(args[0]).toFile());}catch(Exception error){throw new RuntimeException(error);}});}
    static void layout(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container nested)layout(nested);}
}
