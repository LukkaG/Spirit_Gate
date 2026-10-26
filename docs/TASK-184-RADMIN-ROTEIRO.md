# TASK-184 — ensaio físico Radmin

Estado da execução física: **PENDENTE**. Este roteiro foi revisado contra as opções reais do launcher e os contratos das TASK-176/180. Não foi executado em dois computadores. O laboratório localhost da TASK-183 não preenche os campos abaixo.

Use duas instalações privadas novas e pareadas, preservando as anteriores. Não use saves pessoais nem projetos existentes. A executa Anfitrião e B executa Convidado. Registre versões e hashes antes de conectar; hashes dos ZIPs por papel podem diferir, enquanto JAR, conteúdo e assets comuns devem coincidir. Não atualize feed remoto e não use pacotes intermediários de V4.

| Campo | Máquina A | Máquina B |
| --- | --- | --- |
| Operador sintético / data e fuso | PENDENTE | PENDENTE |
| Windows / Java integrado | PENDENTE | PENDENTE |
| Pacote, versão, SHA-256 ZIP e JAR | PENDENTE | PENDENTE |
| Manifesto de assets e SHA-256 | PENDENTE | PENDENTE |
| Adaptador Radmin selecionado / IPv4 de teste | PENDENTE | PENDENTE |
| IP/porta de destino confirmados | PENDENTE | PENDENTE |
| Revisão inicial / publicada / rollback | PENDENTE | PENDENTE |
| Diretório novo de logs / capturas | PENDENTE | PENDENTE |

Confira o adaptador explicitamente na interface de hospedagem e o mesmo IP no convidado. Não use 127.0.0.1 entre máquinas. Registre somente o adaptador selecionado e o endereço necessário ao ensaio, sem exportar inventários de rede, contas ou credenciais. Falha de conectividade deve ser registrada; este roteiro não autoriza alterações de firewall, VPN ou roteador.

Para coletar stdout/stderr sem depender de wrappers PowerShell bloqueados, em um terminal PowerShell já aberto na raiz de cada **pacote privado novo** com `app/spiritgate.jar`, execute os comandos abaixo, um por vez. Se o layout do pacote diferir, registre o impedimento e consulte seu manifesto antes de adaptar caminhos. O nome exclusivo impede reutilizar evidências antigas.

```powershell
$ensaioDir = Join-Path (Get-Location) ('evidencia-radmin-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $ensaioDir | Out-Null
Get-FileHash -Algorithm SHA256 -LiteralPath '.\app\spiritgate.jar' | Format-List | Out-File (Join-Path $ensaioDir 'jar-sha256.txt')
# A: abrir o launcher com dados sintéticos isolados.
& '.\runtime\bin\java.exe' '-Dspiritgate.network.debug=true' "-Dspiritgate.rpg.dataDir=$ensaioDir\dados-sinteticos" '-cp' 'app/spiritgate.jar' 'com.projetok.prototype.rpg.RpgLauncher' 2>&1 | Tee-Object -FilePath (Join-Path $ensaioDir 'console-A.log')
# B: usar esta variante em vez da linha A.
& '.\runtime\bin\java.exe' '-Dspiritgate.guest=true' '-Dspiritgate.network.debug=true' "-Dspiritgate.rpg.dataDir=$ensaioDir\dados-sinteticos" '-cp' 'app/spiritgate.jar' 'com.projetok.prototype.rpg.RpgLauncher' 2>&1 | Tee-Object -FilePath (Join-Path $ensaioDir 'console-B.log')
```

Não execute ambas as linhas na mesma máquina para representar A/B. Os logs de console não contêm automaticamente todos os tempos de READY/ACTIVE; anote as fases e mensagens visíveis no Studio, capture F2 nos dois clientes e preserve o histórico de revisão criado **somente** na pasta sintética. Console vazio não comprova sucesso. Hash ausente deve ser marcado NÃO COLETADO, nunca inferido da aparência da tela. Não arquive pastas pessoais ou configurações de contas junto aos logs.

| Passo | Estímulo | Aceitação em A e B | Evidência / resultado |
| --- | --- | --- | --- |
| 1 | Hospedar A no adaptador escolhido; B ingressa com nome sintético | Dois jogadores, revisão base e sprites corretos | PENDENTE |
| 2 | Mover ambos e executar PvE/portal | Autoridade coerente, região correta, sem entidade duplicada | PENDENTE |
| 3 | Abrir Studio autorizado no host e Preview separado | Preview possui sessão/porta próprias | PENDENTE |
| 4 | Editar geometria/perfil em local desocupado e salvar apenas rascunho | Game Server e ambos os clientes conservam base | PENDENTE |
| 5 | Clicar **Salvar — aplicar ao jogo** | READY, ACTIVE, commit e RESUME confirmados; mesmos hashes | PENDENTE |
| 6 | Repetir movimento, contato com cenário, PvE e portal | Física e representação coerentes nos dois computadores | PENDENTE |
| 7 | Histórico → restaurar revisão no rascunho → Salvar | Rollback validado e confirmado nos três participantes | PENDENTE |
| 8 | Fechar cliente B durante preparo; retornar após abort | Sem commit falso; A coerente; retorno recebe revisão ativa | PENDENTE |
| 9 | Repetir desconexão na ativação, registrando a fase exata | Recuperação ou isolamento explícito, sem conteúdo parcial jogável | PENDENTE |
| 10 | Cliente histórico V1/V2/V3 no conteúdo inicial; tentar Salvar | Publicação bloqueada com causa clara, antigo permanece conectado | PENDENTE |
| 11 | Após revisão autorada, tentar ingresso histórico | Rejeição clara; sem downgrade ou alteração da revisão | PENDENTE |
| 12 | Encerrar Studio, clientes e host; reiniciar em pasta sintética | Somente revisão confirmada carregada; journal pendente bloqueia | PENDENTE |

Não coloque uma parede sobre jogador/inimigo para forçar sucesso; afaste-os por gameplay legítimo. Rollback passa pelas mesmas validações. Nunca edite `active.txt` com a partida aberta. Se houver journal pendente, feche a hospedagem e use a recuperação explícita prevista em [TASK-180-ROLLBACK.md](TASK-180-ROLLBACK.md).

Cada resultado deve conter PASS/FAIL/NÃO EXECUTADO, horário com fuso, revisão antes/depois, fase observada e links relativos para logs/capturas de **ambas** as máquinas. Registre sintomas e duração de pausas sem afirmar sincronização imperceptível. Falha grave de hash, perda de dados, commit falso ou privilégio indevido interrompe o ensaio; preserve arquivos e registre a proposta de correção.

Ao encerrar, feche processos e preserve a pasta exclusiva de evidências, inclusive falhas. Nenhuma alteração de configuração de rede foi prescrita, portanto não há configuração global a restaurar. Veredito físico só pode passar de PENDENTE após coleta real e revisão dos resultados A/B. Ausência de um cliente antigo disponível mantém os passos 10/11 NÃO EXECUTADOS.
