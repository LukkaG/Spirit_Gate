import com.projetok.rpg.*;
import com.projetok.prototype.rpg.RpgRuntime;
import java.nio.file.*;
import java.util.concurrent.*;
public class AuditProbe {
 public static void main(String[] args) throws Exception {
  Path folder=Path.of(args[0]); Files.createDirectories(folder);
  var a=new RpgWorld(CharacterIdentity.create("Auditoria Um","guardian","azure"));
  var b=new RpgWorld(CharacterIdentity.create("Auditoria Dois","mystic","amber"));
  var repo=new CharacterSaves(folder);var sa=new LocalSaveStore(repo.newFile(a.save().identity()));var sb=new LocalSaveStore(repo.newFile(b.save().identity()));
  a.inventory().add("crystal",4);a.tick(1,0);sa.save(a.save());sb.save(b.save());
  if(repo.list().size()!=2||!sa.load().equals(a.save())||!sb.load().equals(b.save())||!RpgWorld.restore(sa.load()).save().equals(a.save()))throw new AssertionError("Isolamento/retomada");
  if(sb.load().inventory().equals(sa.load().inventory()))throw new AssertionError("Itens compartilhados");
  System.out.println("ISOLAMENTO_E_RETOMADA=PASS dois UUIDs; saves temporarios independentes");
  var runtime=new RpgRuntime();runtime.start();runtime.saveAndPause(new LocalSaveStore(folder.resolve("paused.properties"))).get(3,TimeUnit.SECONDS);
  var action=runtime.action(w->true);runtime.close();
  System.out.println("FUTURE_APOS_PAUSA_E_CLOSE_COMPLETO="+action.isDone());
  if(action.isDone())throw new AssertionError("Risco de ciclo de vida nao reproduzido");
 }
}
