import {createServer} from 'node:http';
import {createReadStream} from 'node:fs';
import {stat} from 'node:fs/promises';
import path from 'node:path';
const root=path.resolve('dist');
const types={'.html':'text/html; charset=utf-8','.js':'text/javascript; charset=utf-8','.css':'text/css; charset=utf-8','.png':'image/png','.jar':'application/java-archive','.json':'application/json'};
createServer(async(req,res)=>{
 try{
  let name=decodeURIComponent(new URL(req.url,'http://localhost').pathname);if(name.endsWith('/'))name+='index.html';
  const file=path.resolve(root,'.'+name);if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return;}
  const info=await stat(file);if(!info.isFile())throw Error();
  const headers={'Content-Type':types[path.extname(file)]||'application/octet-stream','Accept-Ranges':'bytes','Cache-Control':'no-cache'};
  let start=0,end=info.size-1;const range=/^bytes=(\d*)-(\d*)$/.exec(req.headers.range||'');
  if(range){start=range[1]?Number(range[1]):Math.max(0,info.size-Number(range[2]));end=range[1]&&range[2]?Number(range[2]):info.size-1;if(start>end||end>=info.size){res.writeHead(416);res.end();return;}headers['Content-Range']=`bytes ${start}-${end}/${info.size}`;}
  headers['Content-Length']=end-start+1;res.writeHead(range?206:200,headers);if(req.method==='HEAD')res.end();else createReadStream(file,{start,end}).pipe(res);
 }catch{res.writeHead(404);res.end('Not found');}
}).listen(8789,'127.0.0.1',()=>console.log('http://127.0.0.1:8789'));
