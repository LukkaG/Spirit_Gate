# Salvar, ativação e bloqueio de produção

**O Salvar desta entrega não faz hot reload no servidor RPG/Radmin V3.** O protocolo atual transmite atores/combate/cosméticos, mas não documentos, perfis, assets nem confirmação de revisão visual. RpgNetworkClientLauncher também apresenta a geometria sintética conhecida. Aplicar outro mapa somente no servidor faria cliente e física divergirem.

Por isso, sem um destino Live sintético explicitamente iniciado, LivePublishing retorna bloqueio antes de criar arquivos publicados. Mantém o rascunho, o jogo principal e a revisão anterior. Não há associação automática ao servidor aberto pelo launcher. Isso cumpre a alternativa prevista na TASK-179; não equivale a concluir o critério de ativação em produção.

## Transação de homologação local

O usuário pode criar um destino sintético separado no menu Servidor e então clicar Salvar. O destino exige clientes registrados pelo renderizador Live local; um cliente TCP comum é incompatível e impede publicação.

1. Congelar documento/perfil e revalidar dependências/hashes.
2. Adquirir trava de publicação e comparar o hash publicado esperado.
3. Reservar o destino na fronteira de tick; preparar candidatos e validar posições. Pausar o destino e recusar novas mutações/joins durante a reserva.
4. Criar versão nova, documentos canônicos e metadados em staging. Preservar todas as versões anteriores.
5. Pedir ativação da candidata e conferir o hash retornado pelo owner.
6. Preparar backup do ponteiro, gravar o novo ponteiro atomicamente e registrar confirmação de conteúdo.
7. Confirmar o commit no destino e liberar a simulação. Não há IO fallível de conteúdo depois dessa liberação.

Falhas antes do commit abortam a reserva e restauram as referências antigas, inclusive personagens e estado do encontro. A versão candidata é marcada como abortada e não passa por versão publicada. O arquivo ativo anterior é preservado. O código trata a corrida de confirmação tardia verificando a resposta do destino em vez de restaurar o arquivo para trás de um commit já confirmado.

Diretório novo: studio-projects/live-revisions. Cada versão registra hash, número, data UTC, fonte, perfil, compatibilidade, assets, validação e estado. current() confere o conteúdo do ponteiro e documentos, não apenas o nome do hash. Documentos originais e saves pessoais não são substituídos.

Histórico restaura uma revisão ao rascunho; aplicar rollback exige clicar Salvar e passa pelas mesmas validações. A recuperação explícita de pending.properties restaura o ponteiro anterior sem ativar conteúdo automaticamente e preserva o journal para auditoria. Ao iniciar um destino depois, uma revisão local já confirmada é carregada/validada pelo seu hash, em vez de mostrar silenciosamente uma base diferente como publicada.

Limite de durabilidade: transação de desenvolvimento em filesystem local e mesma JVM; não é consenso distribuído, nem suporte a servidor remoto persistente. Atomic rename/force de arquivo são usados; não foi homologada interrupção física de energia. Uma interrupção de processo após gravar o commit pode ser recuperada pela revisão verificada ao reiniciar o destino sintético.

## Proposta que requer aprovação

Para ligar Salvar ao Game Server real, aprovar uma evolução separada do contrato de conteúdo: negociação de capacidade de revisão, manifesto de mapa/perfil/assets, carregamento e validação no cliente, ACK por hash, fase de preparo/ativação por tick e rollback com confirmação. Clientes V1/V2/V3 devem continuar funcionando no conteúdo histórico e bloquear uma ativação incompatível enquanto conectados. A ponte administrativa deve continuar local e restrita; não usar o IP Radmin como autorização.

Nenhum desses novos frames de produção foi implementado nesta entrega. Nenhuma incompatibilidade de protocolo/save, endpoint VPN, publicação remota ou reescrita do backend principal foi feita sem autorização. A aprovação pedida é para esse contrato; não é autorização implícita de push ou release.
