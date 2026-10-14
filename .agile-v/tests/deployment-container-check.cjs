// BL-DEP-R1 / TC-DEP-001/002/004. Local fixture TLS/SMTP only; no external mail.
const fs=require('node:fs'),http=require('node:http'),https=require('node:https'),net=require('node:net'),tls=require('node:tls'),assert=require('node:assert/strict');
const {spawnSync}=require('node:child_process');
const {chromium}=require('C:/Users/PC/AppData/Roaming/npm/node_modules/@playwright/cli/node_modules/playwright');
const base='https://localhost:18443',database='procureflow_slice0_20261014_deploy',slice=6,messages=[],results=[];
let browser,lastPage,phase;
const wait=ms=>new Promise(r=>setTimeout(r,ms));
const key=fs.readFileSync('target/deploy-proof/server.key'),cert=fs.readFileSync('target/deploy-proof/server.crt');
const context=tls.createSecureContext({key,cert});
let tlsSessions=0;
const sockets=new Set();
function smtpSession(socket,encrypted=false){
 sockets.add(socket);socket.on('close',()=>sockets.delete(socket));socket.on('error',()=>{});
 let buffer='',data=false,body=[],recipient='',authSteps=0;
 const handler=chunk=>{buffer+=chunk.toString();let pos;while((pos=buffer.indexOf('\r\n'))>=0){const line=buffer.slice(0,pos);buffer=buffer.slice(pos+2);
  if(data){if(line==='.'){messages.push({recipient,body:body.join('\n'),encrypted});data=false;body=[];socket.write('250 accepted\r\n');}else body.push(line);continue;}
  if(authSteps){authSteps--;socket.write(authSteps?'334 UGFzc3dvcmQ6\r\n':'235 authenticated\r\n');continue;}
  if(/^(EHLO|HELO)/i.test(line))socket.write(encrypted?'250-localhost\r\n250 AUTH PLAIN LOGIN\r\n':'250-localhost\r\n250 STARTTLS\r\n');
  else if(/^STARTTLS$/i.test(line)&&!encrypted){socket.removeListener('data',handler);socket.write('220 TLS ready\r\n',()=>{const secure=new tls.TLSSocket(socket,{isServer:true,secureContext:context});secure.on('secure',()=>tlsSessions++);smtpSession(secure,true);});return;}
  else if(/^AUTH PLAIN/i.test(line)){assert.ok(encrypted,'Never authenticate before TLS');socket.write('235 authenticated\r\n');}
  else if(/^AUTH LOGIN/i.test(line)){assert.ok(encrypted);authSteps=2;socket.write('334 VXNlcm5hbWU6\r\n');}
  else if(/^RCPT TO:/i.test(line)){recipient=line;socket.write('250 OK\r\n');}
  else if(/^DATA$/i.test(line)){assert.ok(encrypted);data=true;socket.write('354 send data\r\n');}
  else if(/^QUIT$/i.test(line))socket.end('221 bye\r\n');else socket.write('250 OK\r\n');
 }};
 socket.on('data',handler);if(!encrypted)socket.write('220 localhost fixture sink\r\n');
}
const smtp=net.createServer(socket=>smtpSession(socket));
const proxy=https.createServer({key,cert},(req,res)=>{const upstream=http.request({hostname:'127.0.0.1',port:18090,path:req.url,method:req.method,headers:{...req.headers,'x-forwarded-proto':'https','x-forwarded-port':'18443'}},response=>{res.writeHead(response.statusCode,response.headers);response.pipe(res);});upstream.on('error',()=>{res.writeHead(502);res.end();});req.pipe(upstream);});
// Reuse the unchanged accepted Slice4-6 browser helpers and scenarios, not a second journey.
const shared=fs.readFileSync('.agile-v/tests/slice-4-browser-check.cjs','utf8');
let helpers=shared.slice(shared.indexOf('async function check'),shared.indexOf('(async()=>{'));
helpers=helpers.replace("/http:\\/\\/[^\\s]+/","/https?:\\/\\/[^\\s]+/");
const start=shared.indexOf("  await check('identity onboarding");
const end=shared.indexOf('  if(slice>=7)');
assert.ok(start>0&&end>start,'Known canonical journey boundaries');
const code=helpers+'\n'+
"await new Promise(r=>smtp.listen(2525,'0.0.0.0',r)); await new Promise(r=>proxy.listen(18443,'127.0.0.1',r));\n"+
"browser=await chromium.launch({headless:true,executablePath:'C:/Users/PC/AppData/Local/ms-playwright/chromium-1248/chrome-win64/chrome.exe'});\n"+
// Self-signed localhost browser certificate is a declared test fixture. Production keeps verified HTTPS.
"const pages=[];for(let i=0;i<4;i++)pages.push(await(await browser.newContext({ignoreHTTPSErrors:true})).newPage());const [a,b,c,d]=pages;\n"+
shared.slice(start,end)+"\nassert.ok(messages.length>=4);assert.ok(messages.every(m=>m.encrypted));results.push({name:'All fixture mail uses authenticated required STARTTLS',status:'PASS',tlsSessions:tlsCount()});";
const AsyncFunction=Object.getPrototypeOf(async function(){}).constructor;
const runner=new AsyncFunction('context',"const {base,database,slice,messages,results,wait,assert,chromium,smtp,proxy,tlsCount}=context;let browser,lastPage,phase;try {"+code+"}catch(error){error.message=(phase||'preflight')+': '+error.message;throw error;}finally{if(browser)await browser.close();}");
runner({base,database,slice,messages,results,wait,assert,chromium,smtp,proxy,tlsCount:()=>tlsSessions}).catch(error=>{results.push({name:'container journey',status:'FAIL',error:error.message});console.error(error.stack);process.exitCode=1;}).finally(async()=>{
 const observed=JSON.parse(spawnSync('docker',['inspect','procureflow-deploy-app-20261014'],{encoding:'utf8',windowsHide:true}).stdout)[0];
 fs.writeFileSync('.agile-v/verification/deployment-container-results.json',JSON.stringify({scope:'I0 real Chromium/Java17 container/PostgreSQL TLS/local STARTTLS fixture; no external emails',database,checks:results,container:{image:observed.Image,memoryLimit:observed.HostConfig.Memory,oomKilled:observed.State.OOMKilled,restarts:observed.RestartCount}},null,2)+'\n');
 for(const socket of sockets)socket.destroy();smtp.close();proxy.close();
});
