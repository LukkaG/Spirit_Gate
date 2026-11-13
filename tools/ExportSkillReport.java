import com.projetok.rpg.RpgSkillCatalog;
import com.projetok.rpg.RpgSkillDescriptions;
import com.projetok.multiplayer.NetworkSkillPresentation;
import java.nio.file.*;
import java.util.Properties;
import javax.imageio.ImageIO;

/** Exporta catálogo e quadros realmente carregados pelo jogo para o relatório ilustrado local. */
public final class ExportSkillReport {
    /** Usa o loader de produção, preservando a prioridade dos manifestos e todas as imagens originais. */
    public static void main(String[] args)throws Exception{
        Path root=Path.of(args[0]).toAbsolutePath().normalize(),output=root.resolve("docs/skills-report");
        Files.createDirectories(output);var clips=NetworkSkillPresentation.load(root);var data=new Properties();
        for(var skill:RpgSkillCatalog.all()){
            String id=skill.id();data.setProperty(id+".name",skill.name());data.setProperty(id+".description",skill.description());
            data.setProperty(id+".activation",RpgSkillDescriptions.activation(skill));data.setProperty(id+".effects",RpgSkillDescriptions.effects(skill));
            data.setProperty(id+".kit",skill.kit());data.setProperty(id+".slot",""+skill.slot());data.setProperty(id+".targeting",skill.targeting().name());
            data.setProperty(id+".energy",""+skill.energyCost());data.setProperty(id+".cooldown",RpgSkillDescriptions.seconds(skill.cooldownTicks()));
            data.setProperty(id+".range",""+skill.range());data.setProperty(id+".radius",""+skill.radius());
            data.setProperty(id+".damage",""+skill.baseDamage());data.setProperty(id+".healing",""+skill.healing());
            data.setProperty(id+".hits",""+skill.mechanics().hits());data.setProperty(id+".duration",RpgSkillDescriptions.seconds(skill.mechanics().durationTicks()));
            data.setProperty(id+".pattern",skill.mechanics().pattern().name());
            for(String suffix:new String[]{"",".travel",".impact",".ground"}){
                String source=id.equals("thunder_5")&&suffix.isEmpty()?"thunder_2":id+suffix;
                var clip=clips.get(source);if(clip==null)continue;
                data.setProperty(id+suffix+".frames",""+clip.frames());
                for(int i=0;i<3;i++){
                    int frame=Math.min(clip.frames()-1,(int)((clip.frames()-1)*(i+1)/4.0));
                    var image=clip.image().getSubimage(frame%clip.columns()*clip.frameWidth(),frame/clip.columns()*clip.frameHeight(),clip.frameWidth(),clip.frameHeight());
                    ImageIO.write(image,"PNG",output.resolve(id+suffix+"-"+i+".png").toFile());
                }
            }
        }
        try(var stream=Files.newOutputStream(output.resolve("catalog.xml"))){data.storeToXML(stream,"Catálogo de produção do Spirit Gate; 30 ticks/s","UTF-8");}
        System.out.println("Exportadas "+RpgSkillCatalog.all().size()+" habilidades para "+output);
    }
}
