// BL-S4-R1 / #18 / TC-S4-012..014. Real Chromium against an owned isolated app.
const {chromium}=require('C:/Users/PC/AppData/Roaming/npm/node_modules/@playwright/cli/node_modules/playwright');
const fs=require('node:fs'),net=require('node:net'),assert=require('node:assert/strict');
const {spawn,spawnSync}=require('node:child_process');
const slice=['5','6','7','8'].includes(process.env.PROCUREFLOW_BROWSER_SLICE)?Number(process.env.PROCUREFLOW_BROWSER_SLICE):4;
const database=process.env.PROCUREFLOW_BROWSER_DB || 'procureflow_slice0_20261012133000_s4browser',jdbc=`jdbc:postgresql://localhost:5332/${database}`;
const base='http://127.0.0.1:18084',messages=[],results=[];
let java,browser,lastPage,phase;
const wait=ms=>new Promise(r=>setTimeout(r,ms));
const smtp=net.createServer(socket=>{
  socket.write('220 localhost test sink\r\n');let buffer='',data=false,body=[],recipient='';
  socket.on('data',chunk=>{buffer+=chunk;let end;while((end=buffer.indexOf('\r\n'))>=0){const line=buffer.slice(0,end);buffer=buffer.slice(end+2);
    if(data){if(line==='.'){messages.push({recipient,body:body.join('\n')});body=[];data=false;socket.write('250 accepted\r\n');}else body.push(line);continue;}
    if(/^(EHLO|HELO)/i.test(line))socket.write('250 localhost\r\n');
    else if(/^RCPT TO:/i.test(line)){recipient=line;socket.write('250 OK\r\n');}
    else if(/^DATA$/i.test(line)){data=true;socket.write('354 send data\r\n');}
    else if(/^QUIT$/i.test(line))socket.end('221 bye\r\n');else socket.write('250 OK\r\n');
  }});
});
async function check(name,action){phase=name;await action();results.push({name,status:'PASS'});console.log('PASS '+name);}
function emailLink(raw){
  const separator=raw.indexOf('\n\n');assert.ok(separator>=0,'MIME header/body separator');
  const headers=raw.slice(0,separator),encoding=headers.match(/Content-Transfer-Encoding:\s*([\w-]+)/i)?.[1]?.toLowerCase();
  let body=raw.slice(separator+2);
  if(encoding==='quoted-printable')body=body.replace(/=\n/g,'').replace(/=([0-9A-F]{2})/gi,(_,h)=>String.fromCharCode(parseInt(h,16)));
  else if(encoding==='base64')body=Buffer.from(body.replace(/\s/g,''),'base64').toString('utf8');
  return body.match(/http:\/\/[^\s]+/)[0];
}
function mailLink(email){const mail=[...messages].reverse().find(m=>m.recipient.includes(email));assert.ok(mail);return emailLink(mail.body);}
async function login(page,email){await page.goto(base);await page.getByRole('heading',{name:'Sign in',exact:true}).waitFor();await page.getByLabel('Email',{exact:true}).fill(email);await page.getByLabel('Password',{exact:true}).fill('browser passphrase 123');await page.getByRole('button',{name:'Sign in',exact:true}).click();await page.locator('#employees tbody tr').first().waitFor();}
async function onboard(page,email,name,buyer,supplier){
  lastPage=page;await page.goto(base);await page.getByRole('button',{name:'Register an organization'}).click();
  await page.getByLabel('Organization name').fill(name);await page.getByLabel('Your name').fill('Operator');await page.getByLabel('Email',{exact:true}).fill(email);await page.getByLabel('Password (12').fill('browser passphrase 123');await page.getByRole('button',{name:'Register',exact:true}).click();await page.getByRole('heading',{name:'Sign in',exact:true}).waitFor();
  await page.goto(mailLink(email));await page.getByText('Email verified. You can now sign in.',{exact:true}).waitFor();await page.getByRole('heading',{name:'Sign in',exact:true}).waitFor();await login(page,email);
  const row=page.locator('#employees tbody tr').first();await row.getByLabel('buyerGrant',{exact:true}).setChecked(buyer);await row.getByLabel('supplierGrant',{exact:true}).setChecked(supplier);const saved=page.waitForResponse(r=>r.url().includes('/api/employees/')&&r.request().method()==='PUT');await row.getByRole('button',{name:'Save',exact:true}).click();assert.equal((await saved).status(),200);assert.equal((await page.context().request.get(base+'/api/auth/me')).status(),401);await login(page,email);
}
async function procurement(page,button){lastPage=page;await page.getByRole('button',{name:'Procurement workspace',exact:true}).click();await page.getByRole('button',{name:button,exact:true}).click();}
async function addItem(page,description,quantity,unit){const expected=await page.locator('[data-edit-item]').count()+1;const form=page.locator('#add-rfq-item');await form.getByLabel('Description',{exact:true}).fill(description);await form.getByLabel('Quantity',{exact:true}).fill(String(quantity));await form.getByLabel('Unit',{exact:true}).fill(unit);await form.getByRole('button',{name:'Add item',exact:true}).click();await page.waitForFunction(count=>document.querySelectorAll('[data-edit-item]').length===count&&document.querySelector('#add-rfq-item textarea')?.value==='',expected);}
(async()=>{
  // A plain bearer query starting with hexadecimal text must never be QP-decoded.
  assert.equal(emailLink('Content-Transfer-Encoding: 7bit\n\nhttp://127.0.0.1:18084/?verify=ABsynthetic'),'http://127.0.0.1:18084/?verify=ABsynthetic');
  assert.equal(emailLink('Content-Transfer-Encoding: quoted-printable\n\nhttp://127.0.0.1:18084/?verify=3DAB=\nsynthetic'),'http://127.0.0.1:18084/?verify=ABsynthetic');
  assert.match(database,new RegExp('^procureflow_slice0_[0-9]{14}_s'+slice+'browser$'));
  assert.deepEqual(Object.keys(process.env).filter(k=>/^(SPRING_DATASOURCE|SPRING_FLYWAY|SPRING_CONFIG|SPRING_APPLICATION_JSON|SPRING_PROFILES_ACTIVE|JAVA_TOOL_OPTIONS|JDK_JAVA_OPTIONS|_JAVA_OPTIONS)/i.test(k)),[]);
  const config=fs.readFileSync('src/main/resources/application.properties','utf8');assert.ok(!/^\s*spring\.(datasource\.hikari\.jdbc-url|flyway\.url|config\.import)\s*=/im.test(config));
  const probe=net.createServer();await new Promise((r,j)=>{probe.once('error',j);probe.listen(18084,'127.0.0.1',r);});await new Promise(r=>probe.close(r));
  await new Promise(r=>smtp.listen(2528,'127.0.0.1',r));
  const log=fs.openSync('target/s4-browser-app.log','w');
  java=spawn('C:/Program Files/Eclipse Adoptium/jdk-25.0.3.9-hotspot/bin/java.exe',['-jar','target/app-0.0.1-SNAPSHOT.jar','--server.address=127.0.0.1','--server.port=18084',`--spring.datasource.url=${jdbc}`,'--spring.mail.host=127.0.0.1','--spring.mail.port=2528','--spring.mail.properties.mail.smtp.auth=false','--spring.mail.properties.mail.smtp.starttls.enable=false'],{windowsHide:true,stdio:['ignore',log,log],env:{...process.env,PROCUREFLOW_COOKIE_SECURE:'false',PROCUREFLOW_PUBLIC_BASE_URL:base}});
  let started=false;for(let i=0;i<60;i++){assert.equal(java.exitCode,null);const output=fs.readFileSync('target/s4-browser-app.log','utf8');if(output.includes(`PID ${java.pid}`)&&output.includes(`Database: ${jdbc}`)&&output.includes('Tomcat started on port 18084')){started=true;break;}await wait(1000);}assert.ok(started,'Owned child must bind isolated DB and port');
  browser=await chromium.launch({headless:true,executablePath:'C:/Users/PC/AppData/Local/ms-playwright/chromium-1248/chrome-win64/chrome.exe'});
  for(const asset of ['/app.js','/styles.css']){const response=await fetch(base+asset);console.log('Asset '+asset+': '+response.status+' '+response.headers.get('content-type'));assert.equal(response.status,200);}
  const pages=[];for(let i=0;i<4;i++){const page=await(await browser.newContext()).newPage();page.on('pageerror',e=>console.log('PAGE ERROR '+e.message));page.on('console',m=>{if(m.type()==='error')console.log('BROWSER ERROR '+m.text());});pages.push(page);}const [a,b,c,d]=pages;
  await check('identity onboarding and dual-grant procurement navigation',async()=>{await onboard(a,'buyer@s4browser.test','Browser Buyer',true,true);await onboard(b,'b@s4browser.test','Browser Supplier B',false,true);await onboard(c,'c@s4browser.test','Browser Supplier C',false,true);await onboard(d,'d@s4browser.test','Browser Unrelated D',false,true);});
  await check('buyer private contacts, RFQ item edit/remove, two recipients and immutable send',async()=>{
    await procurement(a,'Supplier contacts');await a.locator('#contact-search input').fill('Supplier B');await a.locator('#contact-search button').click();await a.locator('[data-contact] textarea').fill('private buyer note');await a.locator('[data-contact] button').click();await a.getByRole('button',{name:'Remove contact',exact:true}).waitFor();
    await a.getByRole('button',{name:'Procurement home'}).click();await a.getByRole('button',{name:'Your RFQs',exact:true}).click();await a.getByLabel('RFQ title').fill('Browser bolts RFQ');await a.getByRole('button',{name:'Create RFQ draft'}).click();await a.locator('#add-rfq-item').waitFor();
    await addItem(a,'Temporary',1,'box');await a.getByRole('button',{name:'Remove item'}).click();await a.locator('[data-edit-item]').waitFor({state:'detached'});
    await addItem(a,'Bolts',3,'box');await a.locator('[data-edit-item]').first().getByLabel('Description').fill('Bolts <img src=x onerror=alert(1)>');await a.getByRole('button',{name:'Save item',exact:true}).click();await wait(300);
    await addItem(a,'Nuts',7,'pack');await a.locator('#recipients').getByLabel('Browser Supplier B',{exact:true}).check();await a.locator('#recipients').getByLabel('Browser Supplier C',{exact:true}).check();await a.getByRole('button',{name:'Send RFQ',exact:true}).click();await a.getByText('No submitted quotations yet.',{exact:true}).waitFor();assert.equal(await a.locator('#send-rfq').count(),0);assert.equal(await a.locator('article img').count(),0);
  });
  for(const [page,label]of [[b,'B'],[c,'C']])await check(`supplier ${label} invitation draft validation and frozen submission`,async()=>{
    await procurement(page,'Supplier invitations');await page.getByRole('button',{name:'Open invitation',exact:true}).click();await page.getByRole('button',{name:'Create quotation draft',exact:true}).click();await page.locator('#quote-draft').waitFor();
    const prices=page.locator('#quote-draft input[inputmode="decimal"]');await prices.nth(0).fill('0.001');await prices.nth(1).fill('0.20');await page.getByRole('button',{name:'Save quotation draft',exact:true}).click();await page.locator('#notice.error').waitFor();assert.equal(await prices.nth(0).inputValue(),'0.001');
    await prices.nth(0).fill('0.10');if(label==='B')await page.locator('#quote-draft textarea[name="terms"]').fill('😀'.repeat(2000));await page.getByRole('button',{name:'Save quotation draft',exact:true}).click();await page.getByText('Subtotal excluding tax and shipping: MAD 1.70',{exact:true}).waitFor();if(label==='B')assert.equal(await page.locator('#quote-draft textarea[name="terms"]').inputValue(),'😀'.repeat(2000));
    if(label==='B'){await a.getByRole('button',{name:'Procurement home'}).click();await a.getByRole('button',{name:'Your RFQs',exact:true}).click();await a.getByRole('button',{name:'Open RFQ',exact:true}).click();await a.getByText('No submitted quotations yet.',{exact:true}).waitFor();}
    if(label==='B'){
      const status=await page.evaluate(async()=>{const token=await(await fetch('/api/auth/csrf')).json();const inbox=await(await fetch('/api/procurement/invitations')).json();return(await fetch('/api/procurement/invitations/'+inbox[0].id+'/quotation/submit',{method:'POST',headers:{'X-XSRF-TOKEN':token.token}})).status;});assert.equal(status,200);
      await page.getByRole('button',{name:'Save quotation draft',exact:true}).click();await page.locator('#notice.error').waitFor();assert.match(await page.locator('#notice').innerText(),/cannot be changed/);
    }else{const submit=page.getByRole('button',{name:'Submit quotation',exact:true});await submit.focus();await page.keyboard.press('Enter');}
    await page.getByText(/Supplier [BC] · SUBMITTED/).waitFor();assert.equal(await page.locator('#quote-draft').count(),0);
  });
  await check('buyer sees both submitted responses and no hostile HTML; unrelated supplier has no invitation',async()=>{
    await a.getByRole('button',{name:'Procurement home'}).click();await a.getByRole('button',{name:'Your RFQs',exact:true}).click();await a.getByRole('button',{name:'Open RFQ',exact:true}).click();await a.locator('[data-quote]').first().waitFor();assert.equal(await a.locator('[data-quote]').count(),2);await a.locator('[data-quote]').first().click();await a.getByText('Subtotal excluding tax and shipping: MAD 1.70',{exact:true}).waitFor();assert.equal(await a.locator('img').count(),0);
    // Expired display is a browser read-projection fixture; real expiry equality is proved by controlled Clock API tests.
    await a.getByRole('button',{name:'Back to RFQ'}).click();await a.locator('[data-quote]').first().waitFor();
    const fixture='**/api/procurement/rfqs/*/quotations/*';await a.route(fixture,async route=>{const response=await route.fetch();const q=await response.json();q.expired=true;q.validUntil='2020-01-01T00:00:00Z';await route.fulfill({response,json:q});});
    await a.locator('[data-quote]').first().click();await a.getByText(/SUBMITTED · Expired/).waitFor();await a.unroute(fixture);
    await procurement(d,'Supplier invitations');await d.getByText('No invitations received.',{exact:true}).waitFor();
  });
  if(slice>=5)await check('comparison currency/expiry rendering, explicit keyboard award, pending guard, stale conflict and shared immutable order',async()=>{
    lastPage=a;await a.getByRole('button',{name:'Procurement home'}).click();await a.getByRole('button',{name:'Your RFQs',exact:true}).click();await a.getByRole('button',{name:'Open RFQ',exact:true}).click();
    await a.locator('[data-review-quote]').first().waitFor();assert.equal(await a.locator('[data-review-quote]').count(),2);assert.equal(await a.getByText(/Lowest eligible subtotal in this currency/).count(),1);
    const ids=await a.evaluate(async()=>{const rows=await(await fetch('/api/procurement/rfqs')).json();const comparison=await(await fetch('/api/procurement/rfqs/'+rows[0].id+'/comparison')).json();return{rfq:rows[0].id,quotes:comparison.groups[0].quotations.map(q=>q.id)};});
    // Read-projection fixture only: currency/expired rendering; exact server eligibility is API-tested.
    const fixture='**/api/procurement/rfqs/*/comparison';await a.route(fixture,async route=>{const response=await route.fetch();const data=await response.json();const original=data.groups[0].quotations;data.groups=[{currency:'MAD',quotations:[{...original[0],expired:true,eligible:false,lowestEligible:false}]},{currency:'EUR',quotations:[{...original[1],currency:'EUR',lowestEligible:true}]},{currency:'USD',quotations:[{...original[1],currency:'USD',lowestEligible:true}]}];await route.fulfill({response,json:data});});
    await a.getByRole('button',{name:'Procurement home'}).click();await a.getByRole('button',{name:'Your RFQs',exact:true}).click();await a.getByRole('button',{name:'Open RFQ',exact:true}).click();await a.locator('#comparison section').first().waitFor();assert.deepEqual(await a.locator('#comparison section h3').allTextContents(),['MAD','EUR','USD']);assert.equal(await a.locator('#comparison section').first().getByRole('button').count(),0);await a.unroute(fixture);
    await a.getByRole('button',{name:'Procurement home'}).click();await a.getByRole('button',{name:'Your RFQs',exact:true}).click();await a.getByRole('button',{name:'Open RFQ',exact:true}).click();await a.locator('[data-review-quote]').first().waitFor();
    const stale=await a.context().newPage();await stale.goto(base);await stale.getByRole('button',{name:'Procurement workspace',exact:true}).click();await stale.getByRole('button',{name:'Your RFQs',exact:true}).click();await stale.getByRole('button',{name:'Open RFQ',exact:true}).click();await stale.locator('[data-review-quote]').nth(1).click();await stale.locator('#accept-order').waitFor();
    await a.locator('[data-review-quote]').first().click();await a.getByRole('heading',{name:'Review quotation acceptance',exact:true}).waitFor();assert.equal(await a.locator('img').count(),0);
    const acceptance='**/api/procurement/rfqs/*/quotations/*/accept';let requests=0,release;const hold=new Promise(resolve=>release=resolve);await a.route(acceptance,async route=>{requests++;await hold;await route.continue();});
    const button=a.locator('#accept-order');await button.focus();await a.keyboard.press('Enter');await a.waitForFunction(()=>document.querySelector('#accept-order')?.disabled);assert.equal(await button.isDisabled(),true);await a.keyboard.press('Enter');assert.equal(requests,1);release();await a.getByText('Quotation accepted and order created.',{exact:true}).waitFor();await a.unroute(acceptance);assert.equal(await a.getByText(/Original offer valid until:/).count(),1);
    const order=await a.evaluate(async()=>{const rows=await(await fetch('/api/procurement/orders')).json();return rows[0];});assert.equal(order.status,'CREATED');assert.equal(order.subtotal,'1.70');assert.equal(order.supplierDisplayName,'Browser Supplier B');
    const replay=await a.evaluate(async ids=>{const token=await(await fetch('/api/auth/csrf')).json();const r=await fetch('/api/procurement/rfqs/'+ids.rfq+'/quotations/'+ids.quotes[0]+'/accept',{method:'POST',headers:{'X-XSRF-TOKEN':token.token}});return{status:r.status,order:await r.json()};},ids);assert.equal(replay.status,200);assert.deepEqual(replay.order,order);
    await stale.locator('#accept-order').click();await stale.locator('#notice.error').waitFor();await stale.getByText('This RFQ has been awarded.',{exact:true}).waitFor();assert.equal(await stale.locator('[data-review-quote]').count(),0);assert.equal(await stale.locator('[data-open-order]').getAttribute('data-open-order'),String(order.id));await stale.close();
    await b.getByRole('button',{name:'Procurement home'}).click();await b.getByRole('button',{name:'Supplier invitations',exact:true}).click();await b.getByRole('button',{name:'Open invitation',exact:true}).click();await b.getByText(/Supplier B · ACCEPTED/).waitFor();await b.getByRole('button',{name:'Procurement home'}).click();await b.getByRole('button',{name:'Orders',exact:true}).click();await b.getByRole('button',{name:'Open order',exact:true}).click();await b.getByRole('heading',{name:'Order '+order.id,exact:true}).waitFor();const shared=await b.evaluate(async id=>await(await fetch('/api/procurement/orders/'+id)).json(),order.id);assert.deepEqual({...shared,allowedActions:[]},{...order,allowedActions:[]});assert.deepEqual(shared.allowedActions,['confirm']);assert.deepEqual(order.allowedActions,[]);
    await c.getByRole('button',{name:'Procurement home'}).click();await c.getByRole('button',{name:'Orders',exact:true}).click();await c.getByText('No orders yet.',{exact:true}).waitFor();
  });
  if(slice>=6)await check('supplier fulfillment and explicit buyer receipt, keyboard pending guard and late replay',async()=>{
    const original=await a.evaluate(async()=> (await(await fetch('/api/procurement/orders')).json())[0]);
    async function projections(supplierActions,buyerActions,historyLength){const views=[];for(const page of [a,b])views.push(await page.evaluate(async id=>await(await fetch('/api/procurement/orders/'+id)).json(),original.id));assert.deepEqual(views[0].allowedActions,buyerActions);assert.deepEqual(views[1].allowedActions,supplierActions);assert.equal(views[0].history.length,historyLength);assert.deepEqual({...views[0],allowedActions:[]},{...views[1],allowedActions:[]});}
    assert.equal(await a.locator('[data-order-action]').count(),0);
    await projections(['confirm'],[],1);await b.getByRole('button',{name:'Confirm order',exact:true}).click();await b.getByRole('button',{name:'Start processing',exact:true}).waitFor();await projections(['start-processing'],[],2);await b.getByRole('button',{name:'Start processing',exact:true}).click();await b.getByRole('button',{name:'Mark entire order dispatched',exact:true}).waitFor();await projections(['dispatch'],[],3);await b.getByRole('button',{name:'Mark entire order dispatched',exact:true}).click();await b.getByRole('heading',{name:'Review order progress',exact:true}).waitFor();await b.locator('#final-order-action').click();await b.getByText(/Status: DISPATCHED/).waitFor();assert.equal(await b.locator('[data-order-action]').count(),0);await projections([],['receive'],4);
    await a.getByRole('button',{name:'Procurement home'}).click();await a.getByRole('button',{name:'Orders',exact:true}).click();await a.getByRole('button',{name:'Open order',exact:true}).click();await a.getByRole('button',{name:'Acknowledge entire order received',exact:true}).click();await a.getByRole('heading',{name:'Review order progress',exact:true}).waitFor();
    const stale=await a.context().newPage();await stale.goto(base);await stale.getByRole('button',{name:'Procurement workspace',exact:true}).click();await stale.getByRole('button',{name:'Orders',exact:true}).click();await stale.getByRole('button',{name:'Open order',exact:true}).click();await stale.getByRole('button',{name:'Acknowledge entire order received',exact:true}).click();await stale.locator('#final-order-action').waitFor();
    const endpoint='**/api/procurement/orders/*/receive';let calls=0,release;const hold=new Promise(r=>release=r);await a.route(endpoint,async route=>{calls++;await hold;await route.continue();});const button=a.locator('#final-order-action');await button.focus();await a.keyboard.press('Enter');await a.waitForFunction(()=>document.querySelector('#final-order-action')?.disabled);await a.keyboard.press('Enter');assert.equal(calls,1);release();await a.getByText(/Status: RECEIVED/).waitFor();await a.unroute(endpoint);await stale.locator('#final-order-action').click();await stale.getByText(/Status: RECEIVED/).waitFor();await stale.close();
    const buyer=await a.evaluate(async id=>await(await fetch('/api/procurement/orders/'+id)).json(),original.id);const supplier=await b.evaluate(async id=>await(await fetch('/api/procurement/orders/'+id)).json(),original.id);assert.deepEqual(buyer,supplier);assert.deepEqual(buyer.history.map(h=>h.status),['CREATED','CONFIRMED','PROCESSING','DISPATCHED','RECEIVED']);assert.deepEqual(buyer.items,original.items);assert.equal(buyer.subtotal,original.subtotal);assert.equal(buyer.terms,original.terms);assert.equal(await a.locator('#order-history li').count(),5);assert.equal(await a.locator('[data-order-action]').count(),0);
  });
  if(slice>=7)await check('operational queue filters, paging, detail return and current status',async()=>{
    await a.getByRole('button',{name:'Procurement home'}).click();await a.getByRole('button',{name:'Operational queues',exact:true}).click();await a.locator('#queue-kind').waitFor();
    assert.deepEqual(await a.locator('#queue-kind option').allTextContents(),['RFQs','Invitations','Orders']);
    await a.locator('#queue-kind').selectOption('ORDERS');await a.getByText('RECEIVED: 1',{exact:true}).waitFor();await a.getByRole('button',{name:'Open order',exact:true}).click();await a.getByText(/Status: RECEIVED/).waitFor();await a.getByRole('button',{name:'Return to queues'}).click();await a.locator('#queue-kind').waitFor();assert.equal(await a.locator('#queue-kind').inputValue(),'ORDERS');
    await a.evaluate(async()=>{const token=await(await fetch('/api/auth/csrf')).json();for(let i=0;i<12;i++){const r=await fetch('/api/procurement/rfqs',{method:'POST',headers:{'Content-Type':'application/json','X-XSRF-TOKEN':token.token},body:JSON.stringify({title:'Queue Item '+i})});if(r.status!==201)throw new Error('Queue draft creation '+r.status);}});
    await a.locator('#queue-kind').selectOption('RFQS');await a.locator('#queue-q').fill('Queue Item');await a.getByRole('button',{name:'Search',exact:true}).click();await a.getByText('DRAFT: 12',{exact:true}).waitFor();await a.locator('#queue-size').selectOption('10');await a.getByText('Page 1 of 2').waitFor();assert.equal(await a.locator('[data-queue-open]').count(),10);await a.getByRole('button',{name:'Next page'}).click();await a.getByText('Page 2 of 2').waitFor();assert.equal(await a.locator('[data-queue-open]').count(),2);
    const openedId=await a.getByRole('button',{name:'Open RFQ'}).first().getAttribute('data-queue-open');await a.getByRole('button',{name:'Open RFQ'}).first().click();await a.getByRole('button',{name:'Return to queues'}).waitFor();
    await a.evaluate(async id=>{const token=await(await fetch('/api/auth/csrf')).json();const r=await fetch('/api/procurement/rfqs/'+id,{method:'PUT',headers:{'Content-Type':'application/json','X-XSRF-TOKEN':token.token},body:JSON.stringify({title:'Queue Item refreshed'})});if(r.status!==200)throw new Error('Draft update '+r.status);},openedId);
    await a.getByRole('button',{name:'Return to queues'}).click();await a.getByText('Page 2 of 2').waitFor();assert.equal(await a.locator('#queue-q').inputValue(),'Queue Item');await a.getByRole('heading',{name:'Queue Item refreshed',exact:true}).waitFor();
    await a.locator('#queue-status').selectOption('SENT');await a.getByText('No records match these filters.').waitFor();assert.equal(await a.locator('#queue-q').inputValue(),'Queue Item');await a.locator('#queue-status').selectOption('DRAFT');await a.getByText('Page 1 of 2').waitFor();
    await b.getByRole('button',{name:'Procurement home'}).click();await b.getByRole('button',{name:'Operational queues',exact:true}).click();await b.locator('#queue-kind').waitFor();assert.deepEqual(await b.locator('#queue-kind option').allTextContents(),['Invitations','Orders']);await b.getByText('ACCEPTED: 1',{exact:true}).waitFor();await b.locator('#queue-kind').selectOption('ORDERS');await b.getByText('RECEIVED: 1',{exact:true}).waitFor();
  });
  if(slice>=8)await check('completed analytics side, UTC period, value and source return',async()=>{
    lastPage=a;await a.getByRole('button',{name:'Procurement home'}).click();await a.getByRole('button',{name:'Completed-order analytics',exact:true}).click();await a.locator('#analytics-filters').waitFor();
    assert.deepEqual(await a.locator('#analytics-filters select[name="side"] option').allTextContents(),['Buyer','Supplier']);
    const received=await a.evaluate(async()=>{const orders=await(await fetch('/api/procurement/orders')).json();return orders[0].history.find(h=>h.status==='RECEIVED').occurredAt.slice(0,10);});
    await a.locator('#analytics-filters input[name="from"]').fill(received);
    await a.locator('#analytics-filters input[name="toExclusive"]').fill(new Date(Date.parse(received+'T00:00:00Z')+86400000).toISOString().slice(0,10));
    await a.getByRole('button',{name:'Apply filters'}).click();await a.getByText('1 completed order · Page 1 of 1').waitFor();
    await a.getByText('MAD 1.70',{exact:true}).waitFor();assert.equal(await a.locator('[data-analytics-order]').count(),1);
    await a.locator('[data-analytics-order]').first().focus();await a.keyboard.press('Enter');await a.getByRole('button',{name:'Return to analytics'}).waitFor();
    await a.getByRole('button',{name:'Return to analytics'}).click();await a.locator('#analytics-filters').waitFor();assert.equal(await a.locator('#analytics-filters input[name="from"]').inputValue(),received);
    await a.locator('#analytics-filters select[name="side"]').selectOption('SUPPLIER');await a.getByRole('button',{name:'Apply filters'}).click();await a.getByText('No completed orders in this period and side.').waitFor();
    await b.getByRole('button',{name:'Procurement home'}).click();await b.getByRole('button',{name:'Completed-order analytics',exact:true}).click();await b.locator('#analytics-filters').waitFor();assert.deepEqual(await b.locator('#analytics-filters select[name="side"] option').allTextContents(),['Supplier']);
  });
  if(slice>=8)await check('analytics real mixed-currency/self fixtures, paging and fresh source return',async()=>{
    assert.match(database,/^procureflow_slice0_[0-9]{14}_s8browser$/);
    const seeded=spawnSync('docker',['exec','-i','postgresql','psql','-v','ON_ERROR_STOP=1','-U','ProcureFlow','-d',database],{input:fs.readFileSync('.agile-v/tests/slice-8-browser-fixtures.sql','utf8'),encoding:'utf8',windowsHide:true});
    assert.equal(seeded.status,0,seeded.stderr);
    lastPage=a;
    await a.locator('#analytics-filters select[name="side"]').selectOption('BUYER');
    await a.locator('#analytics-filters select[name="size"]').selectOption('10');
    await a.getByRole('button',{name:'Apply filters'}).click();await a.getByText('12 completed orders · Page 1 of 2',{exact:true}).waitFor();
    for(const text of ['MAD 5.70','EUR 4.00','USD 3.00'])await a.getByText(text,{exact:true}).waitFor();
    assert.equal(await a.locator('[data-analytics-order]').count(),10);assert.equal(await a.locator('#app img').count(),0);
    await a.getByText(/Captured supplier <img/).first().waitFor();
    await a.locator('#analytics-next').focus();await a.keyboard.press('Enter');await a.getByText('12 completed orders · Page 2 of 2',{exact:true}).waitFor();
    assert.equal(await a.locator('[data-analytics-order]').count(),2);assert.equal(await a.locator('#analytics-next').isDisabled(),true);
    const retained=await a.locator('#analytics-filters').evaluate(form=>Object.fromEntries(new FormData(form)));
    await a.locator('[data-analytics-order]').first().focus();await a.keyboard.press('Enter');await a.getByRole('button',{name:'Return to analytics'}).waitFor();
    // Real concurrent workflow change while detail is open; no analytics response interception.
    await a.evaluate(async()=>{const orders=await(await fetch('/api/procurement/orders')).json();const pending=orders.find(o=>o.status==='DISPATCHED');if(!pending)throw new Error('Missing pending fixture');const token=await(await fetch('/api/auth/csrf')).json();const response=await fetch('/api/procurement/orders/'+pending.id+'/receive',{method:'POST',headers:{'X-XSRF-TOKEN':token.token}});if(response.status!==200)throw new Error('Receipt '+response.status);});
    await a.getByRole('button',{name:'Return to analytics'}).click();await a.getByText('13 completed orders · Page 2 of 2',{exact:true}).waitFor();
    assert.deepEqual(await a.locator('#analytics-filters').evaluate(form=>Object.fromEntries(new FormData(form))),retained);
    assert.equal(await a.locator('[data-analytics-order]').count(),3);await a.getByText('USD 4.00',{exact:true}).waitFor();
    await a.locator('#analytics-prev').click();await a.getByText('13 completed orders · Page 1 of 2',{exact:true}).waitFor();assert.equal(await a.locator('#analytics-prev').isDisabled(),true);
    await a.locator('#analytics-filters select[name="side"]').selectOption('SUPPLIER');await a.getByRole('button',{name:'Apply filters'}).click();await a.getByText('12 completed orders · Page 1 of 2',{exact:true}).waitFor();
    await a.getByText(/Captured buyer ·/).first().waitFor();assert.equal(await a.getByText(/Captured supplier <img/).count(),0);assert.equal(await a.locator('#app img').count(),0);
  });
  if(slice>=8)await check('analytics invalid-period editing, server failure/retry and loading',async()=>{
    lastPage=a;
    const endpoint='**/api/procurement/analytics/completed-orders?*';
    const from=await a.locator('#analytics-filters input[name="from"]').inputValue();
    const to=await a.locator('#analytics-filters input[name="toExclusive"]').inputValue();
    await a.locator('#analytics-filters input[name="from"]').fill(to);await a.locator('#analytics-filters input[name="toExclusive"]').fill(from);
    await a.getByRole('button',{name:'Apply filters'}).click();await a.locator('#analytics-error').waitFor();
    assert.match(await a.locator('#analytics-error').innerText(),/Invalid analytics filters/);assert.equal(await a.locator('#analytics-filters input[name="from"]').inputValue(),to);
    await a.locator('#analytics-filters input[name="from"]').fill(from);await a.locator('#analytics-filters input[name="toExclusive"]').fill(to);
    await a.getByRole('button',{name:'Apply filters'}).click();await a.getByText('12 completed orders · Page 1 of 2',{exact:true}).waitFor();
    // Transport-only 500 fixture: actual API data above, explicit server error rendering here.
    await a.route(endpoint,route=>route.fulfill({status:500,contentType:'application/json',json:{code:'SERVER_ERROR',message:'Temporary analytics failure <img src=x>'}}));
    await a.getByRole('button',{name:'Apply filters'}).click();await a.locator('#analytics-error').waitFor();assert.match(await a.locator('#analytics-error').innerText(),/Temporary analytics failure <img/);assert.equal(await a.locator('#app img').count(),0);
    await a.unroute(endpoint);
    let release;const hold=new Promise(resolve=>release=resolve);await a.route(endpoint,async route=>{await hold;await route.continue();});
    await a.getByRole('button',{name:'Retry',exact:true}).click();await a.getByText('Loading completed orders…',{exact:true}).waitFor();release();await a.getByText('12 completed orders · Page 1 of 2',{exact:true}).waitFor();await a.unroute(endpoint);
  });
  await check('marketplace remains reachable and procurement handles expired session',async()=>{
    await a.getByRole('button',{name:'Organization workspace'}).click();await a.locator('#marketplace').waitFor();await a.locator('#nav-marketplace').click();await a.getByRole('heading',{name:'Marketplace',exact:true}).waitFor();
    await b.evaluate(async()=>{const token=await(await fetch('/api/auth/csrf')).json();await fetch('/api/auth/logout',{method:'POST',headers:{'X-XSRF-TOKEN':token.token}});});await b.getByRole('button',{name:'Procurement home'}).click();await b.getByRole('button',{name:slice>=8?'Completed-order analytics':slice>=7?'Operational queues':'Supplier invitations',exact:true}).click();await b.getByRole('heading',{name:'Sign in',exact:true}).waitFor();
  });
})().catch(async error=>{results.push({name:phase||'preflight',status:'FAIL',error:error.message});console.log(error.stack);if(lastPage){console.log(await lastPage.locator('h1').allTextContents());console.log(await lastPage.locator('#notice').innerText());await lastPage.screenshot({path:'target/s4-browser-failure.png'});}process.exitCode=1;}).finally(async()=>{
  fs.writeFileSync('target/s'+slice+'-browser-results.json',JSON.stringify({database,scope:'I0 owned Chromium/local SMTP; TC-S4-012..014, TC-S5-011/012 for slice>=5, TC-S6-007/008 for slice>=6, TC-S7-006/007 for slice>=7, TC-S8-006/007 for slice>=8; currency/expiry rendering fixtures disclosed, eligibility API-tested',checks:results},null,2));if(browser)await browser.close();if(java)java.kill();if(smtp.listening)smtp.close();
});
