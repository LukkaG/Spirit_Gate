# TASK-058 - Tres regioes jogaveis

Vila da Aurora, Bosque dos Ecos e Santuario Partido conectados por portais F. Ambientacao sintetica legivel, caminhos, pracas, casas/arvores/pedras e pontos de interesse rotulados. Mapas usam LocalArenaMap e DebugPanel existentes; nao ha nova fisica ou renderer. Limites 960x640 e obstaculo central compartilhado garantem compatibilidade de spawns.

Build RPG separado com JDK integrado, JAR com whitelist de pacotes locais e recursos RPG; nenhuma classe de banco/Kryonet. Builder legado foi ajustado para ignorar explicitamente o subpacote RPG e seus recursos, preservando pacote de arena independente. Arte pendente nao incorporada.

Testes do contrato dos tres mapas e suite completa. Distribuicao e evidencias do marco integrado registradas apos executar builder, sem alegar homologacao humana.
