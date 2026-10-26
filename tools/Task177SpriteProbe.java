import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
public class Task177SpriteProbe {
    public static void main(String[] args)throws Exception {
        var props=new Properties();try(var in=Files.newInputStream(Path.of("server/src/main/resources/rpg/experimental-player.properties"))){props.load(in);}
        System.out.println("pose,width,height,pivotX,pivotY,alphaLeft,alphaTop,alphaRight,alphaBottom,supportWidthWorld,supportOffsetX,supportOffsetY");
        for(var key:new TreeSet<>(props.stringPropertyNames())){
            var f=props.getProperty(key).split(",");if(f.length!=6)continue;
            var im=ImageIO.read(Path.of("assets/dev/player-base",f[0]).toFile());int left=im.getWidth(),right=0,top=im.getHeight(),bottom=0;
            for(int y=0;y<im.getHeight();y++)for(int x=0;x<im.getWidth();x++)if((im.getRGB(x,y)>>>24)!=0){left=Math.min(left,x);right=Math.max(right,x+1);top=Math.min(top,y);bottom=Math.max(bottom,y+1);}
            int fl=im.getWidth(),fr=0;for(int y=Math.max(0,bottom-12);y<bottom;y++)for(int x=0;x<im.getWidth();x++)if((im.getRGB(x,y)>>>24)!=0){fl=Math.min(fl,x);fr=Math.max(fr,x+1);}
            int px=Integer.parseInt(f[4]),py=Integer.parseInt(f[5]);
            System.out.printf(Locale.ROOT,"%s,%d,%d,%d,%d,%d,%d,%d,%d,%.2f,%.2f,%.2f%n",key,im.getWidth(),im.getHeight(),px,py,left,top,right,bottom,(fr-fl)*.5,((fl+fr)*.5-px)*.5,(bottom-6-py)*.5);
        }
    }
}
