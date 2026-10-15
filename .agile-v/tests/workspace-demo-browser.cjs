// BL-WD-R1 / TC-WD-001..005. Local isolated demo only; no hosted accounts or mail.
const fs=require('node:fs'),assert=require('node:assert/strict'),{execFileSync}=require('node:child_process');
const {chromium}=require('C:/Users/PC/AppData/Roaming/npm/node_modules/@playwright/cli/node_modules/playwright');
const base='http://127.0.0.1:18140',password=fs.readFileSync('target/wd-demo-password.txt','utf8').trim();
const preview=JSON.parse(fs.readFileSync('target/wd-preview.json','utf8').replace(/^\uFEFF/,''));
assert.match(preview.database,/^procureflow_demo_[0-9]{14}_workspace$/);
const checks=[],errors=[];
function sql(text){return execFileSync('rtk',['proxy','docker','exec','postgresql','psql','-v','ON_ERROR_STOP=1','-U','ProcureFlow','-d',preview.database,'-c',text],{encoding:'utf8'});}
async function check(name,run){await run();checks.push({name,status:'PASS'});console.log('PASS '+name);}
async function api(page,path,method='GET',body){return page.evaluate(async({path,method,body})=>{
 const {token}=await fetch('/api/auth/csrf').then(r=>r.json());
 const response=await fetch(path,{method,headers:{'Content-Type':'application/json','X-XSRF-TOKEN':token},body:body===undefined?undefined:JSON.stringify(body)});
 const result=await response.json().catch(()=>null);if(!response.ok)throw new Error(method+' '+path+' '+response.status+' '+JSON.stringify(result));return result;
},{path,method,body});}
async function actor(browser,email){const context=await browser.newContext({viewport:{width:1440,height:1000}}),page=await context.newPage();
 page.on('pageerror',e=>errors.push(e.message));await page.goto(base+'/?view=login');await page.waitForFunction(()=>document.querySelector('.navigation-progress')?.textContent==='');await page.getByLabel('Email',{exact:true}).fill(email);await page.getByLabel('Password',{exact:true}).fill(password);await page.getByRole('button',{name:'Sign in',exact:true}).click();await page.locator('.workspace-sidebar').waitFor();return {page,context};}
async function nav(page,label){await page.locator('.workspace-sidebar').getByRole('link',{name:label,exact:true}).click();await page.waitForFunction(label=>[...document.querySelectorAll('.workspace-sidebar a')].some(a=>a.textContent===label&&a.getAttribute('aria-current')==='page'),label);await page.waitForFunction(()=>document.querySelector('#app')?.getAttribute('aria-busy')!=='true'&&document.querySelector('.navigation-progress')?.textContent==='');}
async function heading(page,name){await page.locator('#app').getByRole('heading',{name,exact:true}).waitFor();}
(async()=>{const browser=await chromium.launch({headless:true,executablePath:'C:/Users/PC/AppData/Local/ms-playwright/chromium-1248/chrome-win64/chrome.exe'});
 try{
 const buyer=await actor(browser,'buyer@procureflow.invalid'),p=buyer.page;
 await check('real buyer catalog-first, 20 products/10 categories, restored session and direct query',async()=>{
  await heading(p,'Marketplace');await p.locator('.product-card').first().waitFor();assert.equal(await p.locator('.product-card').count(),20);assert.equal(await p.locator('#search select option').count(),11);
  assert.equal(await p.locator('.workspace-sidebar [aria-current=page]').innerText(),'Browse products');
  assert.equal(await p.locator('#nav-marketplace').getAttribute('aria-current'),'page');
  await p.reload();await heading(p,'Marketplace');await p.goto(base+'/?view=orders');await heading(p,'Orders');assert.equal(await p.locator('#nav-marketplace').getAttribute('aria-current'),null);
  await p.goto(base+'/?view=workspace');await heading(p,'Demo Buyer Studio');
  await p.goto(base+'/');await heading(p,'Marketplace');
 });
 await check('catalog category/search/empty states and retry',async()=>{
  await p.locator('#search select').selectOption('PACKAGING');await p.locator('#search button').click();await p.getByText(/2 result\(s\)/).waitFor();assert.equal(await p.locator('.product-card').count(),2);
  await p.locator('#search input').fill('no-such-product');await p.locator('#search button').click();await p.getByText(/No offerings match/).waitFor();
  await p.route('**/api/public/offerings?**',r=>r.fulfill({status:503,json:{message:'Test unavailable'}}));await nav(p,'Browse products');await p.locator('#catalog-retry').waitFor();await p.unroute('**/api/public/offerings?**');await p.locator('#catalog-retry').click();await p.locator('.product-card').first().waitFor();
 });
 let rfq;
 await check('explicit product draft creation, duplicate prevention and saved editable item',async()=>{
  await p.locator('[data-request-product]').first().click();await heading(p,'Request a quotation');
  assert.ok((await p.locator('textarea[name=description]').inputValue()).includes('Demo product'));
  assert.equal((await api(p,'/api/procurement/rfqs')).length,0);
  await p.getByLabel('Request title').fill('Browser demo sourcing');await p.getByLabel('Quantity',{exact:true}).fill('3');
  await p.locator('#product-request').evaluate(f=>{f.requestSubmit();f.requestSubmit()});await heading(p,'Browser demo sourcing');await p.locator('[data-edit-item]').waitFor();
  const rows=await api(p,'/api/procurement/rfqs');assert.equal(rows.length,1);rfq=rows[0];assert.equal(rfq.items.length,1);assert.equal(rfq.items[0].quantity,3);
 });
 await check('supplier two product sets and direct profile navigation',async()=>{
  for(const email of ['supplier.one@procureflow.invalid','supplier.two@procureflow.invalid']){
   const a=await actor(browser,email);await heading(a.page,email.includes('.one')?'Atlas Demo Supply':'Meridian Demo Goods');
   await nav(a.page,'Your products');await a.page.locator('[data-edit]').first().waitFor();assert.equal(await a.page.locator('[data-edit]').count(),10);
   await nav(a.page,'Supplier profile');await heading(a.page,'Supplier profile');assert.equal(await a.page.locator('.workspace-sidebar [aria-current=page]').innerText(),'Supplier profile');assert.equal(await a.page.locator('#offering-form-host').count(),0);
   await nav(a.page,'Orders & tracking');await heading(a.page,'Orders');await a.context.close();
  }
 });
 const supplier=await actor(browser,'supplier.one@procureflow.invalid'),sp=supplier.page;
 await check('seeded supplier category edit; 2000/2001/4000 and Unicode product-to-RFQ boundaries',async()=>{
  const products=await api(sp,'/api/marketplace/offerings'),product=products[0],original={title:product.title,category:product.category,description:product.description};
  for(const length of [2000,2001,4000]){
   await api(sp,'/api/marketplace/offerings/'+product.id,'PUT',{...original,description:'x'.repeat(length)});
   await nav(p,'Browse products');await p.locator(`[data-request-product="${product.id}"]`).click();assert.equal((await p.getByLabel('Specifications').inputValue()).length,2000);
   assert.equal(await p.getByText(/description was shortened/).count(),length>2000?1:0);
  }
  await api(sp,'/api/marketplace/offerings/'+product.id,'PUT',{...original,category:'TOOLS',description:'😀'.repeat(2001)});
  await nav(p,'Browse products');await p.locator(`[data-request-product="${product.id}"]`).click();assert.equal(Array.from(await p.getByLabel('Specifications').inputValue()).length,2000);
  await p.getByLabel('Specifications').fill('x'.repeat(2001));await p.locator('#product-request button').click();await p.getByText(/Specifications must be at most/).waitFor();assert.equal((await api(p,'/api/procurement/rfqs')).length,1);
  await p.getByLabel('Specifications').fill('😀'.repeat(2000));await p.getByLabel('Request title').fill('Unicode boundary');await p.locator('#product-request button').click();await heading(p,'Unicode boundary');await p.locator('[data-edit-item]').waitFor();
  const saved=(await api(p,'/api/procurement/rfqs')).find(r=>r.title==='Unicode boundary');assert.equal(Array.from(saved.items[0].description).length,2000);
  await api(sp,'/api/marketplace/offerings/'+product.id,'PUT',original);
 });
 await check('partial item failure retains single draft and requires explicit repair',async()=>{
  await nav(p,'Browse products');await p.locator('[data-request-product]').first().click();await p.getByLabel('Request title').fill('Partial item proof');
  await p.route('**/api/procurement/rfqs/*/items',r=>r.fulfill({status:503,json:{message:'Test item failure'}}));await p.locator('#product-request button').click();await heading(p,'Partial item proof');await p.getByText(/item could not be saved/).waitFor();await p.unroute('**/api/procurement/rfqs/*/items');
  const rows=(await api(p,'/api/procurement/rfqs')).filter(r=>r.title==='Partial item proof');assert.equal(rows.length,1);assert.equal(rows[0].items.length,0);assert.equal(await p.locator('#product-request').count(),0);
 });
 await check('saved draft detail failure retries same ID without duplicate creation',async()=>{
  await nav(p,'Browse products');await p.locator('[data-request-product]').first().click();await p.getByLabel('Request title').fill('Detail retry proof');
  await p.route('**/api/procurement/rfqs/*',r=>r.request().method()==='GET'?r.fulfill({status:503,json:{message:'Test detail failure'}}):r.continue());
  await p.locator('#product-request button').click();await p.getByRole('button',{name:'Open saved draft',exact:true}).waitFor();
  await p.unroute('**/api/procurement/rfqs/*');await p.getByRole('button',{name:'Open saved draft',exact:true}).click();await heading(p,'Detail retry proof');
  assert.equal((await api(p,'/api/procurement/rfqs')).filter(r=>r.title==='Detail retry proof').length,1);
 });
 await check('bounded quotation navigation, received request, supplier create/save/submit, buyer comparison',async()=>{
  const recipients=await api(p,'/api/procurement/recipients');await api(p,`/api/procurement/rfqs/${rfq.id}/send`,'POST',{recipientOrganizationIds:recipients.map(r=>r.id)});
  const calls=[];p.on('request',r=>{if(r.url().includes('/api/procurement/'))calls.push(r.url())});await nav(p,'Quotations');await p.locator('[data-quotation-request]').first().waitFor();assert.equal(calls.filter(u=>u.includes('/workspace?')).length,1);assert.equal(calls.filter(u=>/\/rfqs\/\d/.test(u)).length,0);
  await p.locator('#quote-filter select[name=status]').selectOption('SENT');await p.locator('#quote-filter button').click();await p.locator('[data-quotation-request]').waitFor();assert.equal(await p.locator('[data-quotation-request]').count(),1);
  await nav(sp,'Received RFQs');await sp.locator('[data-invitation]').first().waitFor();await nav(sp,'Quotations');await sp.locator('[data-quotation-request]').first().click();await sp.locator('#create-quote').click();await sp.locator('#quote-draft').waitFor();await sp.locator('input[name^=price-]').fill('12.50');await sp.locator('#quote-draft button').click();await sp.getByText('Draft saved.',{exact:true}).waitFor();await sp.locator('#submit-quote').click();await sp.getByText('Quotation submitted.',{exact:true}).waitFor();
  await p.locator('[data-quotation-request]').click();await p.locator('#comparison').waitFor();assert.match(await p.locator('#comparison').innerText(),/Atlas Demo Supply/);
 });
 await check('own name and admin organization persistence, shell reflection, help',async()=>{
  await nav(p,'My profile');await p.locator('#account-profile input').fill('Browser Demo Buyer');await p.locator('#account-profile button').click();await p.getByText('Profile saved.',{exact:true}).waitFor();assert.equal(await p.locator('.account-name').innerText(),'Browser Demo Buyer');
  await p.locator('#organization-profile input').fill('Demo Buyer Studio Updated');await p.locator('#organization-profile button').click();await p.waitForFunction(()=>document.querySelector('.organization-label strong')?.textContent==='Demo Buyer Studio Updated');
  await p.reload();await heading(p,'My profile');assert.equal(await p.locator('#organization-profile input').inputValue(),'Demo Buyer Studio Updated');
  await p.locator('#organization-profile input').fill('Demo Buyer Studio');await p.locator('#organization-profile button').click();await p.waitForFunction(()=>document.querySelector('.organization-label strong')?.textContent==='Demo Buyer Studio');
  await nav(p,'Help & information');await heading(p,'How procurement works');assert.equal(await p.locator('.help-steps li').count(),4);
 });
 await check('real no-grant employee and dual-role navigation plus server employee denial',async()=>{
  sql(`insert into accounts(full_name,email,password_hash,organization_id,organization_admin,buyer_grant,supplier_grant,active,email_verified) select 'Fixture '||v.email,v.email,a.password_hash,a.organization_id,false,v.buyer,v.supplier,true,true from accounts a cross join (values ('employee@procureflow.invalid',false,false),('dual@procureflow.invalid',true,true)) v(email,buyer,supplier) where a.email='buyer@procureflow.invalid' on conflict(email) do nothing`);
  const employee=await actor(browser,'employee@procureflow.invalid');await heading(employee.page,'Demo Buyer Studio');assert.equal(await employee.page.locator('.workspace-sidebar a').count(),3);await nav(employee.page,'My profile');assert.equal(await employee.page.locator('#organization-profile').count(),0);
  const denied=await employee.page.evaluate(async()=>{const {token}=await fetch('/api/auth/csrf').then(r=>r.json());return (await fetch('/api/auth/organization',{method:'PUT',headers:{'Content-Type':'application/json','X-XSRF-TOKEN':token},body:JSON.stringify({displayName:'Forbidden'})})).status});assert.equal(denied,403);await employee.context.close();
  const dual=await actor(browser,'dual@procureflow.invalid');await heading(dual.page,'Marketplace');assert.equal(await dual.page.locator('.workspace-sidebar').getByRole('link',{name:'Your products',exact:true}).count(),1);await nav(dual.page,'Quotations');assert.equal(await dual.page.locator('#quote-filter select[name=kind] option').count(),2);await dual.page.locator('#quote-filter select[name=kind]').selectOption('INVITATIONS');await dual.page.locator('#quote-filter select[name=status]').waitFor();await dual.context.close();
 });
 await check('desktop/mobile layout, labelled controls, focus, catalog screenshot and revoked session',async()=>{
  await nav(p,'Browse products');await p.locator('.product-card').first().waitFor();await p.screenshot({path:'target/workspace-demo-desktop.png',fullPage:true});
  await p.setViewportSize({width:390,height:844});await p.screenshot({path:'target/workspace-demo-mobile.png',fullPage:true});assert.ok(await p.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'mobile no horizontal overflow');
  assert.ok(await p.getByLabel('Search',{exact:true}).count());assert.equal(await p.locator('.product-visual[aria-hidden=true]').count(),20);
  await api(p,'/api/auth/logout','POST');await p.locator('.workspace-sidebar').getByRole('link',{name:'Orders & tracking',exact:true}).click();await heading(p,'Sign in');assert.equal(await p.locator('.workspace-sidebar:visible').count(),0);
 });
 assert.deepEqual(errors,[]);await buyer.context.close();await supplier.context.close();
 }catch(e){checks.push({name:'failure',status:'FAIL',message:e.message});console.error(e);process.exitCode=1;}
 finally{await browser.close();fs.writeFileSync('.agile-v/verification/workspace-demo-browser.json',JSON.stringify({base,database:preview.database,observedAt:new Date().toISOString(),checks,pageErrors:errors},null,2));}
})();
