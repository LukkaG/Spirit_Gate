import {chromium} from 'playwright-core';
import {mkdir,writeFile} from 'node:fs/promises';
await mkdir('qa',{recursive:true});const logs=[];
const browser=await chromium.launch({executablePath:'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',headless:true});
try{
 const context=await browser.newContext({viewport:{width:1440,height:1100}});const page=await context.newPage();
 page.on('console',m=>logs.push(m.type()+': '+m.text()));page.on('pageerror',e=>logs.push('PAGEERROR '+e.message));
 await page.goto(process.env.TEST_URL||'http://127.0.0.1:8789',{waitUntil:'networkidle'});
 await page.selectOption('#size-rating','Bom tamanho');await page.selectOption('#pace-rating','Bom ritmo');await page.fill('#comments','Teste automatizado local; nao enviado.');
 const link=await page.locator('#github-feedback').getAttribute('href');if(!decodeURIComponent(link).includes('40%, 220 ms/frame'))throw Error('Feedback parameters missing');
 await page.click('#start');await page.waitForTimeout(12000);
 if(logs.some(l=>/PAGEERROR|Exception in thread|Sprites RPG experimentais recusadas|Network error/.test(l)))throw Error('Java or sprites failure: '+logs.join('\n'));
 const game=page.locator('#game');await game.scrollIntoViewIfNeeded();await game.screenshot({path:'qa/idle.png'});
 await game.click({position:{x:220,y:400}});
 for(const [key,name] of [['d','right'],['s','front'],['a','left'],['w','back']]){
   await page.keyboard.down(key);await page.waitForTimeout(450);await game.screenshot({path:`qa/walk-${name}.png`});await page.keyboard.up(key);await page.waitForTimeout(150);
 }
 await page.keyboard.press('F5');await page.waitForTimeout(1500);await game.screenshot({path:'qa/save.png'});
 await page.screenshot({path:'qa/game-full.png',fullPage:true});
 const report={url:page.url(),feedbackPrepared:true,javaExceptions:logs.filter(l=>/PAGEERROR|Exception in thread/.test(l)),testedDirections:['right','front','left','back'],saveKeyDidNotReload:await page.locator('#start').isDisabled(),humanApproved:false};
 if(!report.saveKeyDidNotReload)throw Error('F5 reloaded page');await writeFile('qa/game-results.json',JSON.stringify(report,null,2));console.log(JSON.stringify(report));
 await writeFile('qa/browser.log',logs.join('\n'));
}finally{await writeFile('qa/browser-latest.log',logs.join('\n'));await browser.close();}
