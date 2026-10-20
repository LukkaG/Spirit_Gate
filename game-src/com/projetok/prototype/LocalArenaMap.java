package com.projetok.prototype;

import com.projetok.domain.movement.CollisionCategory;
import com.projetok.domain.movement.CollisionService;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/** Mapa local versionado: aparencia nunca define a geometria autoritativa. */
public record LocalArenaMap(int width, int height, float spawnX, float spawnY,
                            List<Visual> visuals, List<Physical> physical) {
    /** Placeholder sintetico; tamanho e camada nao alteram a hitbox. */
    public record Visual(String id, int x, int y, int width, int height, int layer, int rgb) { }
    /** Retangulo fisico explicitamente tipado; WORLD_BOUNDARY e o limite do mundo. */
    public record Physical(String id, float x, float y, float width, float height,
                           CollisionCategory category, DebugSnapshot.SoftObstacleKind kind) { }

    public LocalArenaMap {
        visuals=List.copyOf(visuals);physical=List.copyOf(physical);
        if(width<DebugWorld.WIDTH || height<DebugWorld.HEIGHT || width>8192 || height>8192)
            throw new IllegalArgumentException("Limites do mundo: 960x640 ate 8192x8192");
        if(visuals.size()>4096 || physical.size()>4096) throw new IllegalArgumentException("Contagem excessiva");
        Set<String> ids=new HashSet<>();
        for(var v:visuals) {
            if(v.id()==null || v.id().isBlank() || !ids.add(v.id())) throw new IllegalArgumentException("ID visual duplicado/vazio");
            try { bounds(v.x(),v.y(),v.width(),v.height(),width,height); }
            catch(IllegalArgumentException e) {throw new IllegalArgumentException("visual "+v.id()+": x/y/width/height invalidos",e);}
            if(v.layer()<0 || v.layer()>10 || v.rgb()<0 || v.rgb()>0xffffff) throw new IllegalArgumentException("Camada/cor invalida");
        }
        ids.clear();int boundaries=0;
        for(var p:physical) {
            if(p.id()==null || p.id().isBlank() || !ids.add(p.id())) throw new IllegalArgumentException("ID fisico duplicado/vazio");
            try { bounds(p.x(),p.y(),p.width(),p.height(),width,height); }
            catch(IllegalArgumentException e) {throw new IllegalArgumentException("physical "+p.id()+": x/y/width/height invalidos",e);}
            if(p.category()==CollisionCategory.WORLD_BOUNDARY) {
                boundaries++;
                if(p.x()!=0 || p.y()!=0 || p.width()!=width || p.height()!=height) throw new IllegalArgumentException("Borda deve coincidir com mundo");
            } else if(p.category()!=CollisionCategory.SOFT_OBSTACLE && p.category()!=CollisionCategory.SOLID_OBSTACLE) throw new IllegalArgumentException("Categoria fisica invalida");
            if(p.category()==CollisionCategory.SOFT_OBSTACLE && p.kind()==null) throw new IllegalArgumentException("Leve exige tipo visual explicito");
        }
        if(boundaries!=1) throw new IllegalArgumentException("Exatamente uma borda explicita obrigatoria");
        List<Physical> bodies=physical.stream().filter(p->p.category()!=CollisionCategory.WORLD_BOUNDARY).toList();
        for(int i=0;i<bodies.size();i++) for(int j=i+1;j<bodies.size();j++) {
            var a=bodies.get(i);var b=bodies.get(j);
            if(a.x()<b.x()+b.width() && a.x()+a.width()>b.x() && a.y()<b.y()+b.height() && a.y()+a.height()>b.y()) throw new IllegalArgumentException("physical: sobreposicao "+a.id()+" / "+b.id());
        }
        float[][] spawns={{spawnX,spawnY,DebugWorld.PLAYER_RADIUS},{215,148,DebugWorld.MOB_RADIUS},{812,190,DebugWorld.MOB_RADIUS},{782,550,DebugWorld.MOB_RADIUS}};
        for(var s:spawns) {
            if(!Float.isFinite(s[0]) || !Float.isFinite(s[1]) || s[0]<s[2] || s[1]<s[2] || s[0]>width-s[2] || s[1]>height-s[2]) throw new IllegalArgumentException("Spawn fora do mapa");
            for(var b:bodies) {
                float dx=s[0]-Math.max(b.x(),Math.min(s[0],b.x()+b.width()));
                float dy=s[1]-Math.max(b.y(),Math.min(s[1],b.y()+b.height()));
                if(dx*dx+dy*dy<s[2]*s[2]) throw new IllegalArgumentException("Spawn ocupado");
            }
        }
        for(int i=0;i<spawns.length;i++) for(int j=i+1;j<spawns.length;j++) {
            float dx=spawns[i][0]-spawns[j][0],dy=spawns[i][1]-spawns[j][1],r=spawns[i][2]+spawns[j][2];
            if(dx*dx+dy*dy<r*r) throw new IllegalArgumentException("Spawns sobrepostos");
        }
    }
    private static void bounds(float x,float y,float w,float h,int width,int height) {
        if(!Float.isFinite(x+y+w+h) || x<0 || y<0 || w<=0 || h<=0 || x+w>width || y+h>height) throw new IllegalArgumentException("Retangulo fora dos limites");
    }
    /** Carrega apenas arquivo local Properties, com contagens limitadas e chaves estritas. */
    public static LocalArenaMap load(Path path) throws IOException {
        if(Files.size(path)>1_000_000) throw new IOException("Mapa excessivo");
        Properties p=new Properties() {
            @Override public synchronized Object put(Object key,Object value) {
                if(containsKey(key)) throw new IllegalArgumentException("Chave duplicada no mapa");
                return super.put(key,value);
            }
        };
        try(var reader=Files.newBufferedReader(path)) {p.load(reader);}
        try {
            String version=(String)p.remove("schemaVersion");
            if(!"1".equals(version) && !"2".equals(version)) throw new IllegalArgumentException("Schema desconhecido");
            int w=integer(p,"width"),h=integer(p,"height");
            if("1".equals(version) && (w!=960 || h!=640)) throw new IllegalArgumentException("V1 exige 960x640");
            if("2".equals(version) && w==960 && h==640) throw new IllegalArgumentException("V2 reservado a mapas ampliados; use V1 para 960x640");
            float sx=number(p,"spawnX"),sy=number(p,"spawnY");
            int nv=count(p,"visual.count"),np=count(p,"physical.count");
            List<Visual> visuals=new ArrayList<>();List<Physical> physical=new ArrayList<>();
            for(int i=0;i<nv;i++) {
                String prefix="visual."+i+".";
                visuals.add(new Visual(take(p,prefix+"id"),integer(p,prefix+"x"),integer(p,prefix+"y"),integer(p,prefix+"width"),integer(p,prefix+"height"),integer(p,prefix+"layer"),Integer.parseInt(take(p,prefix+"rgb"),16)));
            }
            for(int i=0;i<np;i++) {
                String prefix="physical."+i+".";
                String id=take(p,prefix+"id");float x=number(p,prefix+"x"),y=number(p,prefix+"y"),pw=number(p,prefix+"width"),ph=number(p,prefix+"height");
                CollisionCategory category=CollisionCategory.valueOf(take(p,prefix+"category"));
                String kind=(String)p.remove(prefix+"kind");
                physical.add(new Physical(id,x,y,pw,ph,category,kind==null?null:DebugSnapshot.SoftObstacleKind.valueOf(kind)));
            }
            if(!p.isEmpty()) throw new IllegalArgumentException("Chave desconhecida no mapa");
            return new LocalArenaMap(w,h,sx,sy,visuals,physical);
        } catch(IllegalArgumentException error) {throw new IOException("Mapa invalido: "+error.getMessage(),error);}
    }
    private static String take(Properties p,String key) {
        String value=(String)p.remove(key);if(value==null) throw new IllegalArgumentException("Chave ausente: "+key);return value;
    }
    private static int integer(Properties p,String key) {return Integer.parseInt(take(p,key));}
    private static float number(Properties p,String key) {return Float.parseFloat(take(p,key));}
    private static int count(Properties p,String key) {int n=integer(p,key);if(n<0 || n>4096) throw new IllegalArgumentException("Contagem invalida");return n;}
    /** Validador offline reutiliza exatamente o contrato carregado pelo jogo, sem abrir Swing. */
    public static void main(String[] args) {
        if(args.length!=1) throw new IllegalArgumentException("Uso: LocalArenaMap arquivo.properties");
        try {
            var map=load(Path.of(args[0]));
            System.out.println("VALIDO "+map.width()+"x"+map.height()+" spawn="+map.spawnX()+","+map.spawnY());
            for(int i=0;i<=10;i++) {final int layer=i;
                long count=map.visuals().stream().filter(v->v.layer()==layer).count();
                if(count>0) System.out.println("layer."+i+"="+count);
            }
            for(var category:CollisionCategory.values()) {
                long count=map.physical().stream().filter(p->p.category()==category).count();
                if(count>0) System.out.println(category+"="+count);
            }
        } catch(IOException | IllegalArgumentException error) {
            System.err.println("INVALIDO: "+error.getMessage());System.exit(1);
        }
    }
    /** Converte apenas descritores fisicos em obstaculos; borda pertence ao Space. */
    public List<CollisionService.Obstacle> obstacles() {
        return physical.stream().filter(p->p.category()!=CollisionCategory.WORLD_BOUNDARY)
                .map(p->new CollisionService.Obstacle(p.x(),p.y(),p.width(),p.height(),p.category())).toList();
    }
    /** Renderer de demonstracao usa tiles sinteticos em cache, sem ativar PNG candidato. */
    public Renderer renderer() {return new Renderer(this,true);}
    public Renderer renderer(boolean diagnosticTiles) {return new Renderer(this,diagnosticTiles);}
    public static final class Renderer {
        private record Tile(Visual visual,BufferedImage image) { }
        private final List<Tile> tiles;
        private final boolean diagnosticTiles;
        private Renderer(LocalArenaMap map,boolean diagnosticTiles) {
            this.diagnosticTiles=diagnosticTiles;
            tiles=map.visuals().stream().sorted(java.util.Comparator.comparingInt(Visual::layer)).map(v->{
                BufferedImage image=new BufferedImage(32,32,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();
                g.setColor(new Color(v.rgb()));g.fillRect(0,0,32,32);g.setColor(new Color(v.rgb()).brighter());
                if(diagnosticTiles){g.drawRect(0,0,31,31);g.drawLine(0,0,31,31);}g.dispose();return new Tile(v,image);
            }).toList();
        }
        /** Renderiza somente dados imutaveis; nenhum efeito no mundo ou na hitbox. */
        public void draw(Graphics2D original) {
            drawLayers(original,0,10);
        }
        /** Camadas 0-4 ficam atrás dos atores; 5-10 ficam à frente, antes dos efeitos. */
        public void drawLayers(Graphics2D original,int first,int last) {
            Graphics2D g=(Graphics2D)original.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                for(var tile:tiles) {var v=tile.visual();
                    if(v.layer()>=first && v.layer()<=last && g.hitClip(v.x(),v.y(),v.width(),v.height()))
                        g.drawImage(tile.image(),v.x(),v.y(),v.width(),v.height(),null);}
                if(first==0&&diagnosticTiles) {g.setColor(Color.WHITE);g.drawString("MAPA LOCAL DE TESTE - TILES SINTETICOS",24,24);}
            }finally{g.dispose();}
        }
    }
}
