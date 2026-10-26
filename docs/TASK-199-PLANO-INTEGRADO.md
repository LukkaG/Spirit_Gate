# TASK-199 — prova integrada local

Reutilizar o cenário de produção auditado Task180PublishingProbe em ferramenta nova Task199PublishingProbe. Compilar contra o jar recém-construído e verificado da TASK-198, depois conferir que seu inventário de fontes coincide com a árvore atual. Projeto e saídas em pasta nova da 199; copiar apenas as duas raízes autorizadas de assets. Nenhum launcher pessoal ou projeto do usuário é aberto.

Servidor RpgSessionWorld/LocalSimulationServer via LauncherSession real, dois LocalClient com janelas RpgNetworkClientLauncher reais, WorldStudio real e Preview em porta diferente. Editar geometria/perfil e asset preinstalado; confirmar que edição/arquivo DRAFT não ativa; publicar e conferir hashes, imagens, perfil; mover ambos, hit PvE e portal; rollback confirmado. Acrescentar desconexão real do segundo cliente após rollback e nova janela/conexão com hash confirmado. Registrar hashes base/candidata/finais/reconexão e portas do Game Server/Preview junto às métricas existentes.

Capturas são composição das janelas reais produzidas por printAll, não prova humana de arte, janela do sistema ou Radmin físico. Cenários de falha durante barreiras estão nos testes 183/185/186 e no smoke do pacote 198; esta prova não acrescenta proxy externo nem configura rede. Fechar apenas processos e janelas criados pelo próprio harness.
