# Launcher Radmin com atualização

A integração usa o atualizador existente para abrir o RpgLauncher da TASK-177, que já contém Hospedar via Radmin VPN e Entrar em partida. Não muda o jogo, protocolo, firewall ou Radmin. A entrega foi preparada localmente; não houve publicação nem Git push.

## Para usar agora

1. Extraia o ZIP Anfitrião em uma pasta nova no seu computador e o ZIP Convidado no computador do amigo. Preserve as instalações anteriores.
2. Abra sempre **Jogar-SpiritGate.cmd**, na raiz SpiritGate-Atualizavel. Ele verifica a versão e abre o launcher Radmin. Não use executáveis dentro de versions como atalho.
3. No anfitrião, selecione **Hospedar via Radmin VPN**. No convidado, use **Entrar em partida**. As regras de adaptador/IP, handshake, porta e duas sessões permanecem as da TASK-176.
4. Os pacotes locais têm feedUrl vazio até a publicação. Eles já abrem a TASK-177 e não consultam prematuramente o canal remoto. Portanto ainda não recebem versões futuras pela internet.
5. Saves e preferências ficam em data/saves, fora de versions. Se precisar migrar saves antigos, feche o jogo, preserve a origem e copie somente os seus saves para data/saves. A integração não move nem altera saves pessoais automaticamente.

O modo da instalação fica em atualizacoes.json: role=anfitriao ou role=convidado. As duas instalações recebem o mesmo conteúdo e manifesto de jogo. O atualizador passa -Dspiritgate.guest=true no convidado, preservando a interface sem hospedagem/offline. Esse modo de interface não constitui isolamento de segurança do JAR.

## Ativar o canal após publicação autorizada

O canal documentado do projeto é https://raw.githubusercontent.com/LukkaG/Spirit_Gate/updates/latest.json. A entrega nova precisa ser publicada nele antes de ativar esse endereço nas novas instalações.

Após a publicação e sua verificação, o convidado mantém:

```json
{
  "feedUrl": "https://raw.githubusercontent.com/LukkaG/Spirit_Gate/updates/latest.json",
  "role": "convidado"
}
```

O anfitrião usa o mesmo endereço e role: anfitriao. O arquivo fica fora de versions e não é substituído pelas atualizações de jogo. Quando publicado, quem já usa o iniciador antigo poderá receber o novo launcher pelo mesmo canal, sem baixar novamente um pacote inicial só para mudar o nome do JAR. O iniciador antigo conserva a interface completa padrão; o novo bootstrap permite selecionar o modo convidado.

Alterar apenas o JSON não publica a TASK-177. Não há bloqueio geral de downgrade por número de versão no atualizador: não apontar os pacotes novos ao canal antes de atualizá-lo. Falha de download/hash mantém a versão instalada. A versão anterior é preservada; isto não é recuperação automática de todo possível erro de gameplay.

## Compatibilidade e próximas entregas

O preparador aceita o ZIP completo Anfitrião da TASK-177 e normaliza app/spiritgate.jar para app/spirit-gate-rpg.jar, caminho esperado pelos iniciadores antigos. Ajusta referências nos arquivos cfg/cmd/ps1 e gera hashes dos arquivos resultantes. O manifesto interno anterior, que teria caminhos/hashes desatualizados, não é reutilizado. Sprites e bytes do JAR não são modificados.

Anfitrião/Convidado são modos do bootstrap, não canais de jogo diferentes. O preparador exige o pacote completo como fonte para não substituir o conteúdo necessário ao servidor por um pacote limitado de convidado. Atualizações reutilizam arquivos com SHA-256 igual, inclusive Java e sprites. Um JAR alterado é baixado inteiro, não como patch binário.

Preparação local, sem publicação:

```powershell
python tools/integrar_launcher_radmin.py CAMINHO_DO_ZIP_ANFITRIAO dist/ENTREGA-NOVA --version NOVA-VERSAO
python tools/smoke_atualizavel_radmin.py dist/ENTREGA-NOVA/integracao.json
```

Sempre usar uma pasta de destino nova. integracao.json identifica ZIPs, SHA-256 e o canal comum em Anfitriao/canal. Não usar o feed incremental artificial do smoke como release.

Quando houver autorização explícita de publicação, preparar outra entrega com --feed-url apontando ao canal fixo, validar os artefatos e publicar a pasta Anfitrião com tools/publicar_atualizacoes_git.py, conforme ATUALIZADOR-GITHUB.md. O publicador existente recusa um pacote inicial sem o endereço remoto esperado. Não executar o publicador como parte do teste local.

## Verificações

Evidências em .task-checkpoints/radmin-atualizavel/: testes Python do preparador, testes PowerShell de integridade/falhas/modos, migração do bootstrap 0.78.1 original sem alterar seu script, preservação de save sentinela e versão anterior, atualização incremental de um JAR com reutilização dos demais arquivos, falha de integridade sem ativar versão parcial, telas reais dos dois modos e servidor com dois clientes usando Java integrado.

Homologação Radmin física entre dois computadores permanece pendente. As capturas automáticas são evidência de componentes, não aprovação visual humana.
