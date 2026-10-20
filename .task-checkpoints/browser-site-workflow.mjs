import {spawn} from 'node:child_process';
import {realpathSync} from 'node:fs';
process.chdir('C:/Users/lucas/Documents/GitHub/Projeto-K/browser-preview');
// The sandbox and authorized network process use different Windows accounts.
// Trust only this task's isolated checkout, for this process tree; no global Git changes.
const configIndex=Number(process.env.GIT_CONFIG_COUNT||'0');
process.env[`GIT_CONFIG_KEY_${configIndex}`]='safe.directory';
process.env[`GIT_CONFIG_VALUE_${configIndex}`]=process.cwd().replaceAll('\\','/');
process.env.GIT_CONFIG_COUNT=String(configIndex+1);
console.log('Site checkout:',process.cwd());
await new Promise(resolve=>{const p=spawn('git',['rev-parse','--show-toplevel'],{env:{...process.env,GIT_TERMINAL_PROMPT:'0'},stdio:['ignore','pipe','pipe']});let out='',err='';p.stdout.on('data',x=>out+=x);p.stderr.on('data',x=>err+=x);p.on('close',code=>{console.log('Git diagnostic:',JSON.stringify({code,out,err,real:realpathSync('.')}));resolve();});});
await import('file:///C:/Users/lucas/.codex/plugins/cache/openai-curated-remote/sites/1.1.0/scripts/site-workflow.mjs');
