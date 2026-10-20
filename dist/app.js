const configurations={anterior:{scale:.5,ms:150,name:'Anterior'},recomendada:{scale:.4,ms:220,name:'Recomendada'},menor:{scale:.35,ms:220,name:'Menor'},lenta:{scale:.4,ms:280,name:'Mais lenta'}};
const query=new URLSearchParams(location.search);let preset=configurations[query.get('preset')]?query.get('preset'):'recomendada';let started=false;
const status=document.querySelector('#status');
function select(name){preset=name;document.querySelectorAll('[data-preset]').forEach(button=>{const active=button.dataset.preset===preset;button.classList.toggle('selected',active);button.setAttribute('aria-pressed',String(active));});updateFeedback();}
document.querySelectorAll('[data-preset]').forEach(button=>button.addEventListener('click',()=>{if(!started)select(button.dataset.preset);}));
document.querySelector('#restart').onclick=()=>{const url=new URL(location.href);url.searchParams.set('preset',preset);location.href=url.href;};
document.querySelector('#fullscreen').onclick=()=>document.querySelector('#game-section').requestFullscreen().catch(()=>{status.textContent='Seu navegador não permitiu tela cheia; o teste continua disponível abaixo.';});
function loadRuntime(){return new Promise((resolve,reject)=>{const script=document.createElement('script');script.src='https://cjrtnc.leaningtech.com/4.3/loader.js';script.onload=resolve;script.onerror=()=>reject(new Error('Não foi possível baixar o ambiente do jogo. Confira sua conexão e tente novamente.'));document.head.appendChild(script);});}
document.querySelector('#start').onclick=async()=>{
 if(started)return;started=true;document.querySelector('#start').disabled=true;document.querySelectorAll('[data-preset]').forEach(b=>b.disabled=true);
 document.querySelector('#game-section').hidden=false;status.textContent='Preparando o jogo no navegador… Na primeira vez, o carregamento pode demorar.';
 try{
  await loadRuntime();const config=configurations[preset];
  const base='/app'+new URL('.',location.href).pathname;
  await cheerpjInit({version:17,status:'splash',javaProperties:['spiritgate.rpg.experimentalSprites=true',`spiritgate.rpg.spriteScale=${config.scale}`,`spiritgate.rpg.spriteFrameMillis=${config.ms}`,`jpackage.app-path=${base}browser.exe`,'spiritgate.rpg.dataDir=/files/spirit-gate-web'],overrideShortcuts:event=>!document.querySelector('#feedback-form').contains(document.activeElement)&&['F5','Space'].includes(event.code)});
  cheerpjCreateDisplay(1260,900,document.querySelector('#game'));
  status.textContent='Ambiente pronto. A janela do jogo aparecerá abaixo; clique nela para mover o personagem.';
  const code=await cheerpjRunMain('com.projetok.prototype.rpg.BrowserLauncher',base+'spirit-gate-web.jar');
  if(code!==0&&code!==undefined)throw new Error('O jogo terminou com código '+code+'. Recarregue para tentar novamente.');
  status.textContent='O jogo foi iniciado. Clique na janela abaixo e crie um personagem.';
 }catch(error){console.error(error);status.textContent=error.message||'O jogo não iniciou. Recarregue a página e tente novamente.';document.querySelector('#start').textContent='Recarregar teste';document.querySelector('#start').disabled=false;document.querySelector('#start').onclick=()=>location.reload();}
};
function feedback(){const c=configurations[preset];return `Spirit Gate — feedback de sprite\nConfiguração: ${c.name} (${c.scale*100}%, ${c.ms} ms/frame)\nTamanho: ${document.querySelector('#size-rating').value||'Não avaliado'}\nRitmo: ${document.querySelector('#pace-rating').value||'Não avaliado'}\nComentários: ${document.querySelector('#comments').value.trim()||'Sem comentários'}\nNavegador: ${navigator.userAgent}\nTela: ${innerWidth} × ${innerHeight}\nVersão: sprites-browser-079`;}
function updateFeedback(){document.querySelector('#github-feedback').href='https://github.com/LukkaG/Spirit_Gate/issues/new?title='+encodeURIComponent('Feedback de sprite — '+configurations[preset].name)+'&body='+encodeURIComponent(feedback());}
document.querySelector('#feedback-form').addEventListener('input',updateFeedback);
document.querySelector('#feedback-form').onsubmit=async event=>{event.preventDefault();try{await navigator.clipboard.writeText(feedback());document.querySelector('#feedback-status').textContent='Avaliação copiada. Envie para o responsável pelo projeto.';}catch{document.querySelector('#feedback-status').textContent='Seu navegador não permitiu copiar. Use o link do GitHub para abrir a avaliação preenchida.';}};
select(preset);
