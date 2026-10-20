import {chromium} from 'playwright-core';
import {mkdir,writeFile} from 'node:fs/promises';
await mkdir('qa',{recursive:true});
const browser=await chromium.launch({executablePath:'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',headless:true});
const page=await browser.newPage({viewport:{width:1440,height:1100}});const logs=[];
page.on('console',m=>{logs.push(m.type()+': '+m.text());if(m.type()==='error')console.log(m.text().slice(0,500));});
page.on('pageerror',error=>{logs.push('PAGEERROR '+error.message);console.log(error.message);});
page.on('response',r=>{if(r.status()>=400)logs.push('HTTP '+r.status()+' '+r.url());});
await page.goto(process.env.TEST_URL||'http://127.0.0.1:8789',{waitUntil:'networkidle'});
await page.screenshot({path:'qa/landing.png',fullPage:true});await page.click('#start');
for(let attempt=0;attempt<24;attempt++){
 await page.waitForTimeout(5000);console.log('QA',attempt,await page.locator('#status').textContent());
 await page.screenshot({path:'qa/game-latest.png',fullPage:true});
 await writeFile('qa/browser.log',logs.join('\n'));
 if(logs.some(s=>s.includes('PAGEERROR')||s.includes('Exception in thread')))break;
}
await writeFile('qa/final-dom.html',await page.content());await browser.close();
