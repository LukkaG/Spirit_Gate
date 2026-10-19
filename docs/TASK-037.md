# TASK-037 - Auditoria da base real

Base encontrada: 0.36.0-rc1, TASK-036 concluida. Nao existe repositorio Git neste checkout. Fontes e documentos recuperaveis no checkpoint TASK-037-before.json; distribuicoes e assets anteriores preservados.

Baseline reproduzido: 241 testes Java e 25 Python, zero falhas. Logs completos TASK-037-java.log e TASK-037-python.log. Nenhuma regra ou baseline historico alterado.

Arquitetura: dominio RPG em com.projetok.rpg, sem Swing; apresentacao em com.projetok.prototype.rpg; persistencia local versionada separada da simulacao. Reutilizar MovementService, CollisionService, ArenaViewport e renderizacao Java2D. DebugWorld permanece exclusivo da arena historica. Nao reutilizar seus spawns e recompensas demonstrativos para progresso persistente.

Dependencias: modo -> identidade -> conteudo -> save -> inventario/equipamento -> PvE/progressao -> NPC/missoes/comercio -> regioes/masmorra -> interface/configuracoes. Rede somente depois do marco offline e em pacote separado.

Os servicos de combate da arena tem formulas historicas e dependem de componentes criados para demonstracao. Regras RPG deverao ser explicitas e isoladas; geometria e resolucao de movimento permanecem compartilhadas. Snapshots imutaveis atravessam a fronteira da UI; IO em executor separado.

Limitacoes: teste automatizado nao constitui homologacao humana. Nenhum acesso a PostgreSQL, rede publica ou sprites reprovados.
