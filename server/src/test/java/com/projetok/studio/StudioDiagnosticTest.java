package com.projetok.studio;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class StudioDiagnosticTest {
    @Test void errorsHaveEvidenceAndPreviewLimitationsAreVisible(){var e=new WorldDocument.Element("missing",WorldDocument.Kind.OBJECT,"a".repeat(64),"b".repeat(64),new WorldDocument.Crop(0,0,32,32),32,32,32,32,"ground",true,false,"","FRONT",Map.of());var d=StudioExample.document().withElements(List.of(e)).withMetadata(Map.of("preview.environment","NIGHT"));var problems=StudioValidator.validate(Map.of(d.id(),d),new AssetCatalog.Result(List.of(),List.of(),List.of()),true);assertTrue(problems.stream().anyMatch(p->p.element().equals(e.id())&&p.description().equals("Sprite inexistente")));assertTrue(problems.stream().anyMatch(p->p.description().contains("somente previa")));assertTrue(problems.stream().allMatch(p->!p.evidence().isBlank()&&!p.correction().isBlank()));}
    @Test void duplicateNpcAndUnsafeEventAreDiagnosed(){var d=StudioExample.document();var npc=d.elements().get(1);var elements=new ArrayList<>(d.elements());elements.add(npc.at("second",230,320));d=d.withElements(elements).withTriggers(List.of(new WorldDocument.Trigger("bad","ENTER","shell","","",0,0,32,32,true)));var issues=StudioValidator.validate(Map.of(d.id(),d),new AssetCatalog.Result(List.of(),List.of(),List.of()),true);assertTrue(issues.stream().anyMatch(p->p.description().contains("NPC repetido")));assertTrue(issues.stream().anyMatch(p->p.description().contains("Acao nao permitida")));}
}
