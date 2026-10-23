import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import javax.imageio.ImageIO;

/** Gera placeholders pixelados determinísticos, sem editar corpo ou atributos de combate. */
public final class GenerateTemporaryCosmetics {
    record Style(String id,String name,String type,Color color) { }
    static final java.util.List<Style> STYLES=java.util.List.of(
        new Style("hair_short","Curto","hair",new Color(67,38,27)),new Style("hair_medium","Medio","hair",new Color(49,45,83)),new Style("hair_spiky","Espetado","hair",new Color(24,71,74)),
        new Style("tunic","Tunica basica","clothing",new Color(30,107,128)),new Style("jacket","Jaqueta","clothing",new Color(153,92,48)),new Style("light_armor","Armadura leve","clothing",new Color(80,105,132)));
    static String sha(Path file)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));}
    static Rectangle bounds(BufferedImage image){int l=image.getWidth(),r=-1,t=image.getHeight(),b=-1;for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)if((image.getRGB(x,y)>>>24)>=128){l=Math.min(l,x);r=Math.max(r,x);t=Math.min(t,y);b=Math.max(b,y);}if(r<l)throw new IllegalArgumentException("Corpo vazio");return new Rectangle(l,t,r-l+1,b-t+1);}
    static BufferedImage layer(BufferedImage body,Style style,String direction,String part){
        int w=body.getWidth(),h=body.getHeight();var image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);var box=bounds(body);int neck=box.y+(int)(box.height*.43),best=w;
        for(int y=box.y+(int)(box.height*.32);y<box.y+(int)(box.height*.56);y++){int l=w,r=-1;for(int x=0;x<w;x++)if((body.getRGB(x,y)>>>24)>=128){l=Math.min(l,x);r=x;}if(r>=l&&r-l<best){best=r-l;neck=y;}}
        int left=w,right=0;for(int y=box.y;y<neck;y++)for(int x=0;x<w;x++)if((body.getRGB(x,y)>>>24)>=128){left=Math.min(left,x);right=Math.max(right,x);}
        int center=(left+right)/2,top=Math.max(2,box.y-3),forehead=box.y+Math.max(12,(neck-box.y)/3);var g=image.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);g.setColor(style.color());
        try{
            if(style.type().equals("hair")){
                if(part.equals("hair_back")){int bottom=style.id().equals("hair_medium")?Math.min(h-4,neck+18):forehead+6;g.fillRect(Math.max(1,left+1),top+8,Math.max(1,right-left-1),Math.max(1,bottom-top-8));}
                else {
                    int[] xs={left+2,left+7,center-7,center,center+7,right-7,right-2,right-4,right-9,center,left+9,left+4};
                    int[] ys={forehead-4,top+3,style.id().equals("hair_spiky")?Math.max(1,top-2):top,top+4,style.id().equals("hair_spiky")?Math.max(1,top-1):top,top+3,forehead-4,forehead+6,forehead,forehead+3,forehead,forehead+6};
                    g.fillPolygon(xs,ys,xs.length);if(direction.equals("BACK"))g.fillRect(left+5,forehead-3,Math.max(1,right-left-10),Math.max(1,neck-forehead+5));
                    if(style.id().equals("hair_medium")){g.fillRect(left+2,forehead-2,6,Math.max(2,neck-forehead));g.fillRect(right-7,forehead-2,6,Math.max(2,neck-forehead));}
                    g.setColor(style.color().brighter());g.drawLine(left+10,top+7,center,top+4);g.drawLine(center+3,top+5,right-10,top+8);
                }
            }else{
                int torsoLeft=center-17,torsoRight=center+17,waist=box.y+(int)(box.height*.76),bottom=style.id().equals("tunic")?Math.min(h-3,waist+10):waist;
                g.fillPolygon(new int[]{center-10,torsoLeft,torsoLeft-3,torsoLeft+1,torsoRight-1,torsoRight+3,torsoRight,center+10},new int[]{neck+2,neck+8,bottom-2,bottom,bottom,bottom-2,neck+8,neck+2},8);
                if(style.id().equals("jacket")&&!direction.equals("BACK")){g.setComposite(AlphaComposite.Clear);g.fillRect(center-2,neck+2,5,Math.max(1,bottom-neck));g.setComposite(AlphaComposite.SrcOver);}
                g.setColor(style.color().brighter());g.drawLine(torsoLeft+3,neck+11,torsoRight-3,neck+11);if(style.id().equals("light_armor")){g.drawLine(center,neck+3,center,bottom-2);g.drawLine(torsoLeft+3,bottom-5,torsoRight-3,bottom-5);}
            }
        }finally{g.dispose();}
        if(style.type().equals("clothing"))for(int y=0;y<h;y++)for(int x=0;x<w;x++)if((body.getRGB(x,y)>>>24)<128)image.setRGB(x,y,0);
        return image;
    }
    public static void main(String[] args)throws Exception {
        Path project=Path.of(args[0]).toAbsolutePath(),output=Path.of(args[1]);Files.createDirectory(output);Properties source=new Properties();try(var in=Files.newInputStream(project.resolve("server/src/main/resources/rpg/experimental-player.properties"))){source.load(in);}
        var manifest=new Properties();manifest.setProperty("schema","1");manifest.setProperty("temporary","true");int count=0;java.util.List<String> keys=source.stringPropertyNames().stream().filter(k->k.matches("(FRONT|BACK|LEFT|RIGHT)\\.[0-9]+")).sorted().toList();
        for(var style:STYLES){manifest.setProperty("cosmetic."+style.id(),style.type()+"|"+style.name()+"|TEMPORARIO");for(String key:keys){String[] fields=source.getProperty(key).split(",");Path original=project.resolve("assets/dev/player-base").resolve(fields[0]);if(!sha(original).equals(fields[1]))throw new IllegalArgumentException("Original divergente: "+original);var body=ImageIO.read(original.toFile());if(body.getWidth()!=Integer.parseInt(fields[2])||body.getHeight()!=Integer.parseInt(fields[3]))throw new IllegalArgumentException("Dimensões divergentes");String direction=key.substring(0,key.indexOf('.'));for(String part:style.type().equals("hair")?java.util.List.of("hair_back","hair_front"):java.util.List.of("clothing")){
                Path relative=Path.of(style.type(),style.id(),part+"-"+key+".png"),target=output.resolve(relative);Files.createDirectories(target.getParent());ImageIO.write(layer(body,style,direction,part),"png",target.toFile());
                manifest.setProperty("frame."+count++,style.id()+"|"+key+"|"+part+"|"+relative.toString().replace('\\','/')+"|"+sha(target)+"|"+fields[2]+"|"+fields[3]+"|"+fields[4]+"|"+fields[5]+"|TEMPORARIO");
            }}}
        manifest.setProperty("count",""+count);try(var out=Files.newOutputStream(output.resolve("manifest.properties"))){manifest.store(out,"TEMPORARIO - gerado por Java2D - sem aprovacao artistica");}
        Files.writeString(output.resolve("README.txt"),"TEMPORARIO: placeholders deterministas em Java2D. Quatro direcoes, frames reais e pivos originais. Sem antialiasing, atributos de combate ou alteracao do corpo base. Substituicao futura depende do responsavel no Aseprite.\n");System.out.println("Generated: "+count+" frames/layers; "+keys.size()+" poses reais; "+output);
    }
}
