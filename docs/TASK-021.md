# TASK-021 - Consolidação e kit de homologação

18/10/2026. Validação automatizada local aprovada. Homologação humana pendente.

Release final: dist/SpiritGate-0.21.0-rc1-20261019T001737Z-546ce1c5-portable.zip

Tamanho: 29.391.539 bytes. SHA-256: 303f469b87aa0752d6dcd44e21ae5efef78295cf971884976939b1d8ae6cc534.

Clean build real, sem reutilizar JAR anterior: 223 JUnit, zero falhas/erros/ignorados. Repetição offline: mesmos 223 verdes. Python: 18 testes verdes. Auditor oficial do ZIP original repetido após compilação offline: PASS. Inventário, manifest, Java 17, PE x64 e bytecode corrente conferidos. Controle negativo em cópia nova rejeitado. ZIP 0.16.0-rc1 preservado.

Smokes reais Windows em pasta com espaços fora de Projeto-K, padrão e --map: JVM integrada comprovada, PATH somente Windows/System32, saídas 0, sem fechamento forçado. Evidências TASK-021-smoke.json, TASK-021-map-smoke.json e capturas correspondentes. Inspeção de rede por CIM indisponível; não afirmada ausência de sockets por esse controle. Sem teste em outro computador. Linux NÃO EXECUTADO.

Distribuição sintética testada em cópia temporária: pacote revisado fictício aceito pelo auditor completo, imagem adulterada recusada mesmo com SHA externo recalculado. Cópia eliminada pelo TemporaryDirectory; não é release. Release real contém zero PNG/atlas/personagem aprovado. Os 228 PNGs conservaram seus hashes; 99 arquivos Java históricos de domínio/testes comparados sem alteração.

Revisão final acrescentou marcadores de índice/pivot ao preview e recusou duração/pivot por frame incompatíveis com runtime no staging. Testes negativos de pacote foram isolados para cada caso. Regeneradas prévias em docs/revisao-fontes-021 e docs/autoria-018/revisao-final. Corrigida codificação dos relatórios 018/019 sem mudar resultados.

Atualizados README-PROTOTIPO, changelog, LEIA-ME do pacote e kit humano: roteiro, modelo de bug, notas e mapa opcional. Novo LEIA-ME explica resize, mira, fallback e limites da câmera. O primeiro candidato intermediário 539628a7 foi preservado para rastreabilidade; somente o candidato 546ce1c5 é indicado para entrega.

Artefatos finais: relatório consolidado MD/PDF, PDF/diff/checkpoint por task, logs, auditorias, inventário SHA e ZIP de entregas. Não houve push, publicação, acesso remoto, banco, credenciais ou mudança de multiplayer. Nenhuma TASK-022 iniciada.
