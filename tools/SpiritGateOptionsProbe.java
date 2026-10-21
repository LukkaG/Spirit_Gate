package com.projetok.prototype.rpg;
import com.projetok.rpg.*;
import javax.swing.*;
import java.awt.*;
import java.nio.file.*;
/** Opções reais: cancelar preserva bytes, aplicar persiste e altera só a janela. */
public final class SpiritGateOptionsProbe {
    /** Usa arquivo de preferências sintético e executa os controles reais. */
    public static void main(String[] args){try{run(args);System.exit(0);}catch(Throwable e){e.printStackTrace();System.exit(1);}}
    static void run(String[] args)throws Exception{
        Path output=Path.of(args[0]);Files.createDirectories(output);SpiritGateWindowsProbe.output=output;
        Path file=Files.createTempDirectory("options-probe-").resolve("preferences.properties");var original=new RpgPreferences(20,false,true);original.save(file);byte[] before=Files.readAllBytes(file);
        JFrame[] frame={null};var applied=new java.util.concurrent.atomic.AtomicReference<RpgPreferences>();
        SwingUtilities.invokeAndWait(()->{frame[0]=new JFrame("Opções sintéticas");frame[0].setSize(1000,620);frame[0].setVisible(true);});
        SwingUtilities.invokeLater(()->PreferencesDialog.open(frame[0],original,file,applied::set));
        SpiritGateWindowsProbe.waitFor(()->SpiritGateWindowsProbe.dialog("Opções do Spirit Gate")!=null);
        JDialog first=SpiritGateWindowsProbe.dialog("Opções do Spirit Gate");
        SwingUtilities.invokeAndWait(()->{SpiritGateWindowsProbe.find(first,JSlider.class,null).setValue(90);SpiritGateWindowsProbe.find(first,JButton.class,"Cancelar").doClick();});
        if(!java.util.Arrays.equals(before,Files.readAllBytes(file))||applied.get()!=null)throw new AssertionError("Cancelamento escreveu dados");
        SwingUtilities.invokeLater(()->PreferencesDialog.open(frame[0],original,file,applied::set));SpiritGateWindowsProbe.waitFor(()->SpiritGateWindowsProbe.dialog("Opções do Spirit Gate")!=null);
        JDialog dialog=SpiritGateWindowsProbe.dialog("Opções do Spirit Gate");
        SwingUtilities.invokeAndWait(()->{SpiritGateWindowsProbe.find(dialog,JButton.class,"Restaurar padrões").doClick();if(SpiritGateWindowsProbe.find(dialog,JSlider.class,null).getValue()!=40)throw new AssertionError("Padrões");SpiritGateWindowsProbe.find(dialog,JSlider.class,null).setValue(65);SpiritGateWindowsProbe.find(dialog,JComboBox.class,null).setSelectedItem("800x600");});
        SpiritGateWindowsProbe.capture(dialog,"opcoes");SwingUtilities.invokeAndWait(()->SpiritGateWindowsProbe.find(dialog,JButton.class,"Aplicar").doClick());SpiritGateWindowsProbe.waitFor(()->applied.get()!=null);
        if(RpgPreferences.load(file).volume()!=65||frame[0].getWidth()!=800||frame[0].getHeight()!=600)throw new AssertionError("Aplicação/persistência");
        SwingUtilities.invokeAndWait(()->frame[0].dispose());Files.writeString(output.resolve("probe.txt"),"PASS: cancelar preserva bytes, restaurar padrões apenas no formulário, volume 65 persistido/recarregado e resolução 800x600 aplicada. Dados temporários; sem alteração de saves.\n");
    }
}
