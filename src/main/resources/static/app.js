const app = document.querySelector('#app');
const notice = document.querySelector('#notice');
let csrf;
let me;
let categoriesCache;
let queueState = {kind:null, page:0, size:20, q:'', status:''};
let queueReturnActive = false;
let analyticsState = {side:null, from:'', toExclusive:'', page:0, size:20};
let analyticsReturnActive = false;

async function request(path, {method = 'GET', body} = {}) {
  const headers = {'Accept': 'application/json'};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) headers['X-XSRF-TOKEN'] = csrf;
  const response = await fetch(path, {method, credentials: 'same-origin', headers,
    body: body === undefined ? undefined : JSON.stringify(body)});
  if (response.status === 204) return null;
  const content = await response.json().catch(() => ({}));
  if (!response.ok) {
    const error = new Error(content.message || (response.status === 401 ? 'Please sign in to continue.' : 'The request could not be completed.'));
    error.status = response.status;
    throw error;
  }
  return content;
}

function queryString(params) {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) if (value !== undefined && value !== '') search.set(key, value);
  return search.toString();
}

function message(text, type = '') {
  notice.className = type;
  notice.textContent = text;
}

function form(title, fields, button, submit, extra = '') {
  app.innerHTML = `<h1>${title}</h1>${extra}<form>${fields.map(f => `<label>${f.label}<input name="${f.name}" type="${f.type || 'text'}" ${f.required === false ? '' : 'required'} ${f.autocomplete ? `autocomplete="${f.autocomplete}"` : ''} ${f.max ? `maxlength="${f.max}"` : ''}></label>`).join('')}<button>${button}</button></form>`;
  app.querySelector('form').addEventListener('submit', async event => {
    event.preventDefault(); message('');
    const values = Object.fromEntries(new FormData(event.currentTarget));
    try { await submit(values); } catch (error) { message(error.message, 'error'); }
  });
}

function showSignIn() {
  form('Sign in', [{label:'Email',name:'email',type:'email',autocomplete:'email'}, {label:'Password',name:'password',type:'password',autocomplete:'current-password'}], 'Sign in', async values => {
    me = await request('/api/auth/login', {method:'POST',body:values});
    history.replaceState({}, '', '/'); message('Signed in.', 'success'); showWorkspace();
  }, '<p class="muted">Your organization administrator can invite you. New organizations can register below.</p><div class="actions"><button class="secondary" type="button" id="register">Register an organization</button></div>');
  app.querySelector('#register').addEventListener('click', showRegistration);
}

function showRegistration() {
  form('Create an organization', [
    {label:'Organization name',name:'organizationName',max:160}, {label:'Your name',name:'fullName',max:160},
    {label:'Email',name:'email',type:'email',autocomplete:'email'}, {label:'Password (12–128 characters)',name:'password',type:'password',autocomplete:'new-password'}
  ], 'Register', async values => {
    const result = await request('/api/auth/register', {method:'POST',body:values});
    history.replaceState({}, '', '/'); message(result.message, 'success'); showSignIn();
  }, '<p class="muted">You will become the first organization administrator after verifying your email. Passwords are accepted as entered (12–128 Unicode code points).</p><button class="secondary" type="button" id="back">Back to sign in</button>');
  app.querySelector('#back').addEventListener('click', showSignIn);
}

function showWorkspace() {
  app.setAttribute('aria-busy','true');
  app.innerHTML = '<h1>Loading organization access…</h1>';
  request('/api/auth/me').then(account => {
    me = account;
    if (!account.emailVerified) { app.innerHTML = `<h1>Verify your email</h1><p>Check ${escapeHtml(account.email)} for the verification link. Protected organization features are unavailable until verification.</p><button id="logout">Sign out</button>`; bindLogout(); return; }
    const marketplaceHost = (account.buyerGrant || account.supplierGrant ? '<div class="actions"><button type="button" id="procurement">Procurement workspace</button></div>' : '') + (account.supplierGrant ? '<div id="marketplace">Loading your supplier profile…</div>' : '');
    if (!account.organizationAdmin) {
      app.innerHTML = `<h1>${escapeHtml(account.organizationName)}</h1><p>Signed in as ${escapeHtml(account.fullName)} (${escapeHtml(account.email)}).</p><p class="muted">Your organization administrator manages employee access.</p><button id="logout">Sign out</button>${marketplaceHost}`;
      bindLogout(); bindProcurement(); if (account.supplierGrant) loadMarketplace(); return;
    }
    app.innerHTML = `<h1>${escapeHtml(account.organizationName)}</h1><p>Organization administrator · ${escapeHtml(account.email)}</p><div class="actions"><button id="invite" class="secondary">Invite employee</button><button id="logout">Sign out</button></div><div id="employees">Loading employees…</div>${marketplaceHost}`;
    bindLogout(); bindProcurement(); app.querySelector('#invite').addEventListener('click', showInvite); loadEmployees();
    if (account.supplierGrant) loadMarketplace();
  }).catch(error => { message(error.message,'error'); showSignIn(); }).finally(() => app.setAttribute('aria-busy','false'));
}

function escapeHtml(value) { return String(value ?? '').replace(/[&<>"']/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch])); }

const procurementBase = '/api/procurement';
function bindProcurement() { app.querySelector('#procurement')?.addEventListener('click', () => procurementAction(showProcurement)); }
async function procurementAction(action, conflictRefresh) {
  message(''); app.setAttribute('aria-busy', 'true');
  try { await action(); }
  catch (error) {
    if (error.status === 401) { me = null; showSignIn(); }
    else if (error.status === 409 && conflictRefresh) await conflictRefresh().catch(() => {});
    message(error.message, 'error');
  } finally { app.setAttribute('aria-busy', 'false'); }
}
function procurementPage(title, html) {
  app.innerHTML = `<h1>${escapeHtml(title)}</h1><div class="actions"><button type="button" class="secondary" id="proc-home">Procurement home</button><button type="button" class="secondary" id="workspace-home">Organization workspace</button>${queueReturnActive && title !== 'Operational queues' ? '<button type="button" class="secondary" id="queue-return">Return to queues</button>' : ''}${analyticsReturnActive && title !== 'Completed-order analytics' ? '<button type="button" class="secondary" id="analytics-return">Return to analytics</button>' : ''}</div>${html}`;
  app.querySelector('#proc-home').onclick = () => procurementAction(showProcurement);
  app.querySelector('#workspace-home').onclick = showWorkspace;
  app.querySelector('#queue-return')?.addEventListener('click', () => procurementAction(showOperationalQueue));
  app.querySelector('#analytics-return')?.addEventListener('click', () => procurementAction(showCompletedAnalytics));
}
async function showProcurement() {
  queueReturnActive = false;
  analyticsReturnActive = false;
  procurementPage('Procurement', `<div class="actions"><button id="queues">Operational queues</button><button id="orders">Orders</button><button id="completed-analytics">Completed-order analytics</button>${me.buyerGrant ? '<button id="contacts">Supplier contacts</button><button id="rfqs">Your RFQs</button>' : ''}${me.supplierGrant ? '<button id="invitations">Supplier invitations</button>' : ''}</div>`);
  app.querySelector('#queues').onclick = () => procurementAction(showOperationalQueue);
  app.querySelector('#orders').onclick = () => procurementAction(showOrders);
  app.querySelector('#completed-analytics').onclick = () => procurementAction(showCompletedAnalytics);
  app.querySelector('#contacts')?.addEventListener('click', () => procurementAction(showContacts));
  app.querySelector('#rfqs')?.addEventListener('click', () => procurementAction(showRfqs));
  app.querySelector('#invitations')?.addEventListener('click', () => procurementAction(showInvitations));
}
const queueStatuses = {
  RFQS:['DRAFT','SENT','AWARDED'],
  INVITATIONS:['INVITED','DRAFT','SUBMITTED','ACCEPTED'],
  ORDERS:['CREATED','CONFIRMED','PROCESSING','DISPATCHED','RECEIVED']
};
function permittedQueueKinds() {
  return [me.buyerGrant && 'RFQS', me.supplierGrant && 'INVITATIONS', (me.buyerGrant || me.supplierGrant) && 'ORDERS'].filter(Boolean);
}
async function showOperationalQueue() {
  const kinds = permittedQueueKinds();
  if (!kinds.includes(queueState.kind)) queueState = {...queueState, kind:kinds[0], page:0, status:''};
  queueReturnActive = true;
  procurementPage('Operational queues', '<p role="status">Loading queues…</p>');
  let data;
  try {
    data = await request(`${procurementBase}/workspace?${queryString(queueState)}`);
  } catch (error) {
    if (error.status === 401) throw error;
    procurementPage('Operational queues', `<p role="alert">${escapeHtml(error.message)}</p><button id="queue-retry">Retry</button>`);
    app.querySelector('#queue-retry').onclick = () => procurementAction(showOperationalQueue);
    return;
  }
  const statuses = queueStatuses[queueState.kind];
  const finalPage = Math.max(0, Math.ceil(data.total / data.size) - 1);
  procurementPage('Operational queues', `<p class="muted">Status counts cover this kind and title search. Open a record to see its current actions.</p>
    <form id="queue-search"><label>Queue kind<select id="queue-kind" name="kind">${kinds.map(kind => `<option value="${kind}" ${kind===queueState.kind?'selected':''}>${kind === 'RFQS' ? 'RFQs' : kind === 'INVITATIONS' ? 'Invitations' : 'Orders'}</option>`).join('')}</select></label>
    <label>Status<select id="queue-status" name="status"><option value="">All statuses</option>${statuses.map(status => `<option value="${status}" ${status===queueState.status?'selected':''}>${status}</option>`).join('')}</select></label>
    <label>Search title<input id="queue-q" name="q" value="${escapeHtml(queueState.q)}"></label>
    <label>Page size<select id="queue-size" name="size">${[10,20,50,100].map(size => `<option value="${size}" ${size===queueState.size?'selected':''}>${size}</option>`).join('')}</select></label>
    <button type="submit">Search</button></form>
    <h2>Status summary</h2><ul>${data.statusCounts.map(entry => `<li>${escapeHtml(entry.status)}: ${entry.count}</li>`).join('')}</ul>
    <p role="status">${data.total} matching record${data.total===1?'':'s'} · Page ${data.page+1} of ${finalPage+1}</p>
    <div class="cards">${data.rows.length ? data.rows.map(row => `<article class="card"><h2>${escapeHtml(row.title)}</h2><p>${escapeHtml(row.status)}${row.counterparty ? ' · ' + escapeHtml(row.counterparty) : ''}</p><button type="button" data-queue-open="${escapeHtml(row.id)}">Open ${queueState.kind === 'RFQS' ? 'RFQ' : queueState.kind === 'INVITATIONS' ? 'invitation' : 'order'}</button></article>`).join('') : '<p>No records match these filters.</p>'}</div>
    <div class="actions"><button type="button" id="queue-previous" ${data.page===0?'disabled':''}>Previous page</button><button type="button" id="queue-next" ${data.page>=finalPage?'disabled':''}>Next page</button></div>`);
  const reload = () => procurementAction(showOperationalQueue);
  app.querySelector('#queue-search').onsubmit = event => {
    event.preventDefault();
    const values = new FormData(event.currentTarget);
    const q = String(values.get('q') || '').trim();
    if (Array.from(q).length > 160) { message('Search title must be at most 160 characters.', 'error'); return; }
    queueState = {kind:String(values.get('kind')), page:0, size:Number(values.get('size')), q, status:String(values.get('status'))};
    reload();
  };
  app.querySelector('#queue-kind').onchange = () => {
    app.querySelector('#queue-status').value = '';
    app.querySelector('#queue-search').requestSubmit();
  };
  for (const id of ['queue-status','queue-size']) app.querySelector('#'+id).onchange = () => app.querySelector('#queue-search').requestSubmit();
  app.querySelector('#queue-previous').onclick = () => { queueState.page--; reload(); };
  app.querySelector('#queue-next').onclick = () => { queueState.page++; reload(); };
  app.querySelectorAll('[data-queue-open]').forEach(button => button.onclick = () => procurementAction(() => {
    const id = button.dataset.queueOpen;
    return queueState.kind === 'RFQS' ? showRfq(id) : queueState.kind === 'INVITATIONS' ? showInvitation(id) : showOrder(id);
  }));
}
async function showContacts() {
  const contacts = await request(`${procurementBase}/contacts`);
  procurementPage('Supplier contacts', `<p>These notes are private to your buying organization.</p><form id="contact-search"><label>Supplier name<input name="q"></label><button>Search suppliers</button></form><div id="contact-results"></div><div class="cards">${contacts.length ? contacts.map(c => `<article class="card"><h2>${escapeHtml(c.supplierDisplayName)}</h2><p>${escapeHtml(c.note)}</p><button type="button" data-remove-contact="${c.id}">Remove contact</button></article>`).join('') : '<p>No contacts saved.</p>'}</div>`);
  app.querySelector('#contact-search').onsubmit = event => {
    event.preventDefault(); const q = new FormData(event.currentTarget).get('q');
    procurementAction(async () => {
      const rows = await request(`${procurementBase}/recipients?${queryString({q})}`);
      app.querySelector('#contact-results').innerHTML = rows.length ? rows.map(r => `<form data-contact="${r.id}"><h2>${escapeHtml(r.displayName)}</h2><label>Private note<textarea name="note" maxlength="2000"></textarea></label><button>Save contact</button></form>`).join('') : '<p>No eligible suppliers found.</p>';
      app.querySelectorAll('[data-contact]').forEach(f => f.onsubmit = e => { e.preventDefault(); procurementAction(async () => { await request(`${procurementBase}/contacts`, {method:'POST', body:{supplierOrganizationId:Number(f.dataset.contact),note:new FormData(f).get('note')}}); await showContacts(); }); });
    });
  };
  app.querySelectorAll('[data-remove-contact]').forEach(b => b.onclick = () => procurementAction(async () => { await request(`${procurementBase}/contacts/${b.dataset.removeContact}`, {method:'DELETE'}); await showContacts(); }));
}
async function showRfqs() {
  const rows = await request(`${procurementBase}/rfqs`);
  procurementPage('Your RFQs', `<form id="new-rfq"><label>RFQ title<input name="title" required maxlength="160"></label><button>Create RFQ draft</button></form><div class="cards">${rows.length ? rows.map(r => `<article class="card"><h2>${escapeHtml(r.title)}</h2><p>${escapeHtml(r.status)} · ${r.items.length} items</p><button data-rfq="${r.id}">Open RFQ</button></article>`).join('') : '<p>No RFQs yet.</p>'}</div>`);
  app.querySelector('#new-rfq').onsubmit = e => { e.preventDefault(); const title = new FormData(e.currentTarget).get('title'); procurementAction(async () => { const r = await request(`${procurementBase}/rfqs`, {method:'POST',body:{title}}); await showRfq(r.id); }); };
  app.querySelectorAll('[data-rfq]').forEach(b => b.onclick = () => procurementAction(() => showRfq(b.dataset.rfq)));
}
function rfqItemFields(item = {}) {
  return `<label>Description<textarea name="description" required maxlength="2000">${escapeHtml(item.description || '')}</textarea></label><label>Quantity<input name="quantity" type="number" required min="1" max="1000000" step="1" value="${item.quantity || 1}"></label><label>Unit<input name="unit" required maxlength="40" value="${escapeHtml(item.unit || '')}"></label>`;
}
function readRfqItem(form) { const v = Object.fromEntries(new FormData(form)); return {...v,quantity:Number(v.quantity)}; }
async function showRfq(id) {
  const r = await request(`${procurementBase}/rfqs/${id}`), draft = r.status === 'DRAFT';
  procurementPage(r.title, `<p>Status: ${escapeHtml(r.status)}</p>${draft ? `<form id="rfq-title"><label>RFQ title<input name="title" required maxlength="160" value="${escapeHtml(r.title)}"></label><button>Save title</button></form>` : ''}<h2>Required items</h2>${r.items.map(i => draft ? `<form data-edit-item="${i.id}">${rfqItemFields(i)}<div class="actions"><button>Save item</button><button type="button" data-remove-item="${i.id}">Remove item</button></div></form>` : `<article class="card"><h3>${escapeHtml(i.description)}</h3><p>${i.quantity} ${escapeHtml(i.unit)}</p></article>`).join('')}${draft ? `<form id="add-rfq-item"><h2>Add item</h2>${rfqItemFields()}<button>Add item</button></form><h2>Select recipients</h2><form id="recipient-search"><label>Supplier name<input name="q"></label><button>Search recipients</button></form><div id="recipients"></div><button type="button" id="send-rfq">Send RFQ</button>` : '<h2>Submitted quotations</h2><div id="buyer-quotes">Loading responses…</div>'}`);
  const base = `${procurementBase}/rfqs/${id}`, refresh = () => showRfq(id);
  if (draft) {
    app.querySelector('#rfq-title').onsubmit = e => { e.preventDefault(); const title=new FormData(e.currentTarget).get('title'); procurementAction(async()=>{await request(base,{method:'PUT',body:{title}});await refresh();},refresh); };
    app.querySelector('#add-rfq-item').onsubmit = e => { e.preventDefault(); const body=readRfqItem(e.currentTarget); procurementAction(async()=>{await request(`${base}/items`,{method:'POST',body});await refresh();},refresh); };
    app.querySelectorAll('[data-edit-item]').forEach(f=>f.onsubmit=e=>{e.preventDefault(); const body=readRfqItem(f); procurementAction(async()=>{await request(`${base}/items/${f.dataset.editItem}`,{method:'PUT',body});await refresh();},refresh);});
    app.querySelectorAll('[data-remove-item]').forEach(b=>b.onclick=()=>procurementAction(async()=>{await request(`${base}/items/${b.dataset.removeItem}`,{method:'DELETE'});await refresh();},refresh));
    const selected=new Set(), labels=new Map();
    async function recipients(q='') {
      const [rows,contacts]=await Promise.all([request(`${procurementBase}/recipients?${queryString({q})}`),request(`${procurementBase}/contacts`)]);
      rows.forEach(o=>labels.set(o.id,o.displayName)); contacts.forEach(c=>labels.set(c.supplierOrganizationId,c.supplierDisplayName));
      const visible=new Set([...rows.map(o=>o.id),...contacts.map(c=>c.supplierOrganizationId),...selected]);
      app.querySelector('#recipients').innerHTML=visible.size ? [...visible].map(org=>`<label class="recipient"><input type="checkbox" value="${org}" ${selected.has(org)?'checked':''}>${escapeHtml(labels.get(org))}</label>`).join('') : '<p>No eligible suppliers found.</p>';
      app.querySelectorAll('#recipients input').forEach(input=>input.onchange=()=>{const org=Number(input.value);input.checked?selected.add(org):selected.delete(org);});
    }
    app.querySelector('#recipient-search').onsubmit=e=>{e.preventDefault();const q=new FormData(e.currentTarget).get('q');procurementAction(()=>recipients(q));};
    app.querySelector('#send-rfq').onclick=()=>procurementAction(async()=>{await request(`${base}/send`,{method:'POST',body:{recipientOrganizationIds:[...selected]}});await refresh();},refresh);
    await recipients();
  } else {
    const rows = await request(`${base}/quotations`);
    app.querySelector('#buyer-quotes').innerHTML=rows.length?rows.map(q=>`<article class="card"><h3>${escapeHtml(q.supplierDisplayName)}</h3><p>${escapeHtml(q.currency)} ${escapeHtml(q.subtotal)} subtotal excluding tax and shipping · ${q.expired?'Expired':'Valid'}</p><button data-quote="${q.id}">View quotation</button></article>`).join(''):'<p>No submitted quotations yet.</p>';
    app.querySelectorAll('[data-quote]').forEach(b=>b.onclick=()=>procurementAction(async()=>{const q=await request(`${base}/quotations/${b.dataset.quote}`);procurementPage('Submitted quotation',quotationDetail(q)+`<button id="back-rfq">Back to RFQ</button>`);app.querySelector('#back-rfq').onclick=()=>procurementAction(refresh);}));
  }
  if (!draft) await renderComparison(id);
}
async function renderComparison(rfqId) {
  const comparison=await request(`${procurementBase}/rfqs/${rfqId}/comparison`);
  const host=document.createElement('div');host.id='comparison';app.append(host);
  host.innerHTML=`<h2>Compare quotations</h2><p>Subtotals exclude tax and shipping. Different currencies are compared separately.</p>${comparison.orderId?`<p>This RFQ has been awarded.</p><button data-open-order="${comparison.orderId}">Open winning order</button>`:''}${comparison.groups.length?comparison.groups.map(group=>`<section><h3>${escapeHtml(group.currency)}</h3>${group.quotations.map(q=>`<article class="card"><h4>${escapeHtml(q.supplierDisplayName)}</h4><p>${escapeHtml(q.subtotal)} ${escapeHtml(q.currency)} · ${q.expired?'Expired':q.status==='ACCEPTED'?'Accepted':q.eligible?'Eligible':'Ineligible'}${q.lowestEligible?' · Lowest eligible subtotal in this currency':''}</p>${q.eligible?`<button data-review-quote="${q.id}">Review quotation for acceptance</button>`:''}</article>`).join('')}</section>`).join(''):'<p>No submitted quotations to compare.</p>'}`;
  host.querySelector('[data-open-order]')?.addEventListener('click',event=>procurementAction(()=>showOrder(event.currentTarget.dataset.openOrder)));
  host.querySelectorAll('[data-review-quote]').forEach(button=>button.onclick=()=>procurementAction(()=>reviewAward(rfqId,button.dataset.reviewQuote)));
}
async function reviewAward(rfqId,quotationId) {
  const q=await request(`${procurementBase}/rfqs/${rfqId}/quotations/${quotationId}`);
  procurementPage('Review quotation acceptance',quotationDetail(q)+`<p>Accepting this complete quotation awards the whole RFQ and creates one purchase order. Supplier confirmation and processing are subsequent actions.</p><div class="actions"><button id="accept-order">Accept quotation and create order</button><button class="secondary" id="cancel-award">Back to RFQ</button></div>`);
  app.querySelector('#cancel-award').onclick=()=>procurementAction(()=>showRfq(rfqId));
  const button=app.querySelector('#accept-order');button.onclick=()=>{
    if(button.disabled)return;button.disabled=true;
    procurementAction(async()=>{const order=await request(`${procurementBase}/rfqs/${rfqId}/quotations/${quotationId}/accept`,{method:'POST'});await showOrder(order.id);message('Quotation accepted and order created.','success');},()=>showRfq(rfqId)).finally(()=>{if(button.isConnected)button.disabled=false;});
  };
}
async function showOrders() {
  const orders=await request(`${procurementBase}/orders`);
  procurementPage('Orders',orders.length?`<div class="cards">${orders.map(o=>`<article class="card"><h2>${escapeHtml(o.rfqTitle)}</h2><p>${escapeHtml(o.buyerDisplayName)} → ${escapeHtml(o.supplierDisplayName)} · ${escapeHtml(o.status)}</p><p>${escapeHtml(o.currency)} ${escapeHtml(o.subtotal)} excluding tax and shipping</p><button data-open-order="${o.id}">Open order</button></article>`).join('')}</div>`:'<p>No orders yet.</p>');
  app.querySelectorAll('[data-open-order]').forEach(button=>button.onclick=()=>procurementAction(()=>showOrder(button.dataset.openOrder)));
}
async function showCompletedAnalytics() {
  const sides = [me.buyerGrant && 'BUYER', me.supplierGrant && 'SUPPLIER'].filter(Boolean);
  if (!sides.includes(analyticsState.side)) analyticsState.side = sides[0];
  if (!analyticsState.toExclusive) {
    const now = new Date();
    analyticsState.toExclusive = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate() + 1)).toISOString().slice(0, 10);
    analyticsState.from = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate() - 29)).toISOString().slice(0, 10);
  }
  analyticsReturnActive = true;
  const filters = `<form id="analytics-filters"><label>Organization side<select name="side">${sides.map(side => `<option value="${side}" ${side === analyticsState.side ? 'selected' : ''}>${side === 'BUYER' ? 'Buyer' : 'Supplier'}</option>`).join('')}</select></label>
    <label>From (UTC, inclusive)<input type="date" name="from" required value="${escapeHtml(analyticsState.from)}"></label>
    <label>To (UTC, exclusive)<input type="date" name="toExclusive" required value="${escapeHtml(analyticsState.toExclusive)}"></label>
    <label>Page size<select name="size">${[10,20,50,100].map(size => `<option value="${size}" ${size === analyticsState.size ? 'selected' : ''}>${size}</option>`).join('')}</select></label><button type="submit">Apply filters</button></form>`;
  function render(content) {
    procurementPage('Completed-order analytics', `<p>Count and captured order value for whole orders received in the selected UTC period. Order value excluding tax/shipping; not paid spend. To date is exclusive; choose a period of 1 to 366 days.</p>${filters}${content}`);
    app.querySelector('#analytics-filters').onsubmit = event => {
      event.preventDefault();
      const values = new FormData(event.currentTarget);
      analyticsState = {side:String(values.get('side')), from:String(values.get('from')), toExclusive:String(values.get('toExclusive')), page:0, size:Number(values.get('size'))};
      procurementAction(showCompletedAnalytics);
    };
  }
  procurementPage('Completed-order analytics', '<p role="status">Loading completed orders…</p>');
  let data;
  try { data = await request(`${procurementBase}/analytics/completed-orders?${queryString(analyticsState)}`); }
  catch (error) {
    if (error.status === 401) throw error;
    render(`<p role="alert" tabindex="-1" id="analytics-error">${escapeHtml(error.message)} Edit the filters or retry.</p><button id="analytics-retry">Retry</button>`);
    app.querySelector('#analytics-retry').onclick = () => procurementAction(showCompletedAnalytics);
    app.querySelector('#analytics-error').focus();
    return;
  }
  const lastPage = Math.max(0, Math.ceil(data.total / data.size) - 1);
  render(`<p role="status">${data.total} completed order${data.total === 1 ? '' : 's'} · Page ${data.page + 1} of ${lastPage + 1}</p>
    <h2>Captured value by currency</h2><ul>${data.totals.map(total => `<li>${escapeHtml(total.currency)} ${escapeHtml(total.amount)}</li>`).join('')}</ul>
    <h2>Source orders</h2><div class="cards">${data.rows.length ? data.rows.map(row => `<article class="card"><h3>${escapeHtml(row.rfqTitle)}</h3><p>${escapeHtml(row.counterparty)} · ${escapeHtml(row.currency)} ${escapeHtml(row.subtotal)} · Received ${escapeHtml(row.receivedAt)}</p><button type="button" data-analytics-order="${row.id}">Open order</button></article>`).join('') : '<p>No completed orders in this period and side.</p>'}</div>
    <div class="actions"><button type="button" id="analytics-prev" ${data.page === 0 ? 'disabled' : ''}>Previous page</button><button type="button" id="analytics-next" ${data.page >= lastPage ? 'disabled' : ''}>Next page</button></div>`);
  app.querySelector('#analytics-prev').onclick = () => { analyticsState.page--; procurementAction(showCompletedAnalytics); };
  app.querySelector('#analytics-next').onclick = () => { analyticsState.page++; procurementAction(showCompletedAnalytics); };
  app.querySelectorAll('[data-analytics-order]').forEach(button => button.onclick = () => procurementAction(() => showOrder(button.dataset.analyticsOrder)));
}
async function showOrder(id) {
  const order=await request(`${procurementBase}/orders/${id}`);
    procurementPage(`Order ${order.id}`,`<p>${escapeHtml(order.rfqTitle)}</p><p>Buyer: ${escapeHtml(order.buyerDisplayName)} · Supplier: ${escapeHtml(order.supplierDisplayName)}</p><p>Status: ${escapeHtml(order.status)} · Accepted: ${escapeHtml(order.acceptedAt)}</p>${quotationDetail(order,true)}<h2>Order history</h2><ol id="order-history">${order.history.map(event=>`<li>${escapeHtml(event.status)} · ${escapeHtml(event.occurredAt)}</li>`).join('')}</ol><p>Progress reports apply to the entire order. Partial deliveries, cancellation and disputes are not available here.</p><div class="actions">${order.allowedActions.map(action=>`<button data-order-action="${escapeHtml(action)}">${orderActionLabel(action)}</button>`).join('')}</div>`);
    app.querySelectorAll('[data-order-action]').forEach(button=>button.onclick=()=>{
      const action=button.dataset.orderAction;
      if(action==='dispatch'||action==='receive')reviewOrderAction(order,action);else submitOrderAction(order.id,action,button);
    });
}
  function orderActionLabel(action){return {'confirm':'Confirm order','start-processing':'Start processing','dispatch':'Mark entire order dispatched','receive':'Acknowledge entire order received'}[action];}
  function reviewOrderAction(order,action){
    procurementPage('Review order progress',`<p>Order ${order.id} · Buyer: ${escapeHtml(order.buyerDisplayName)} · Supplier: ${escapeHtml(order.supplierDisplayName)}</p>${quotationDetail(order,true)}<p>${action==='dispatch'?'You report that the entire order has been dispatched.':'You acknowledge receipt of the entire order and complete its fulfillment.'}</p><button id="final-order-action">${orderActionLabel(action)}</button><button id="cancel-order-action" class="secondary">Back to order</button>`);
    app.querySelector('#cancel-order-action').onclick=()=>procurementAction(()=>showOrder(order.id));
    const button=app.querySelector('#final-order-action');button.onclick=()=>submitOrderAction(order.id,action,button);
  }
  function submitOrderAction(id,action,button){
    if(button.disabled)return;button.disabled=true;
    procurementAction(async()=>{await request(`${procurementBase}/orders/${id}/${action}`,{method:'POST'});await showOrder(id);message('Order progress recorded.','success');},()=>showOrder(id)).finally(()=>{if(button.isConnected)button.disabled=false;});
  }
function quotationDetail(q,orderSnapshot=false) {
  return `${orderSnapshot?'':`<p>${escapeHtml(q.supplierDisplayName)} · ${escapeHtml(q.status)} · ${q.expired?'Expired':'Valid'}</p>`}<p>Currency: ${escapeHtml(q.currency)} · ${orderSnapshot?'Original offer valid until':'Valid until'}: ${escapeHtml(q.validUntil)}</p>${orderSnapshot?'':`<p>Submitted: ${escapeHtml(q.submittedAt || 'Not submitted')}</p>`}<table><thead><tr><th>Item</th><th>Quantity / unit</th><th>Unit price</th><th>Line total</th></tr></thead><tbody>${q.items.map(i=>`<tr><th scope="row">${escapeHtml(i.description)}</th><td>${i.quantity} ${escapeHtml(i.unit)}</td><td>${escapeHtml(i.unitPrice)}</td><td>${escapeHtml(i.lineTotal)}</td></tr>`).join('')}</tbody></table><p>Subtotal excluding tax and shipping: ${escapeHtml(q.currency)} ${escapeHtml(q.subtotal || 'Not priced')}</p><p>Terms: ${escapeHtml(q.terms || 'None')}</p>`;
}
async function showInvitations() {
  const rows=await request(`${procurementBase}/invitations`);
  procurementPage('Supplier invitations', `<div class="cards">${rows.length?rows.map(i=>`<article class="card"><h2>${escapeHtml(i.rfqTitle)}</h2><p>Buyer: ${escapeHtml(i.buyerDisplayName)}</p><button data-invitation="${i.id}">Open invitation</button></article>`).join(''):'<p>No invitations received.</p>'}</div>`);
  app.querySelectorAll('[data-invitation]').forEach(b=>b.onclick=()=>procurementAction(()=>showInvitation(b.dataset.invitation)));
}
async function showInvitation(id) {
  const invitation=await request(`${procurementBase}/invitations/${id}`),base=`${procurementBase}/invitations/${id}/quotation`;
  let q;
  try { q=await request(base); } catch(error) { if(error.status!==404) throw error; }
  if (!q) {
    procurementPage(invitation.rfqTitle, `<p>Buyer: ${escapeHtml(invitation.buyerDisplayName)}</p>${invitation.items.map(i=>`<p>${escapeHtml(i.description)} · ${i.quantity} ${escapeHtml(i.unit)}</p>`).join('')}<button id="create-quote">Create quotation draft</button>`);
    app.querySelector('#create-quote').onclick=()=>procurementAction(async()=>{await request(base,{method:'POST'});await showInvitation(id);},()=>showInvitation(id)); return;
  }
  if(q.status!=='DRAFT'){procurementPage(invitation.rfqTitle,quotationDetail(q));return;}
  const prices=new Map(q.items.map(i=>[i.rfqItemId,i.unitPrice]));
  const date=new Date(q.validUntil || Date.now()+7*86400000);date.setMinutes(date.getMinutes()-date.getTimezoneOffset());
  procurementPage(invitation.rfqTitle, `<p>Buyer: ${escapeHtml(invitation.buyerDisplayName)} · DRAFT</p><p>Quote all required quantities without substitutions. Prices exclude tax and shipping.</p><form id="quote-draft"><label>Currency<select name="currency">${['MAD','EUR','USD'].map(c=>`<option ${q.currency===c?'selected':''}>${c}</option>`).join('')}</select></label><label>Valid until (local time)<input name="validUntil" type="datetime-local" required value="${date.toISOString().slice(0,16)}"></label>${invitation.items.map(i=>`<label>${escapeHtml(i.description)} · ${i.quantity} ${escapeHtml(i.unit)} · Unit price<input name="price-${i.id}" inputmode="decimal" value="${escapeHtml(prices.get(i.id)||'')}" placeholder="e.g. 12.50"></label>`).join('')}<label>Terms (up to 2,000 characters)<textarea name="terms" maxlength="4000">${escapeHtml(q.terms||'')}</textarea></label><button>Save quotation draft</button></form>${quotationDetail(q)}<button type="button" id="submit-quote">Submit quotation</button><p>Submission freezes your terms. Save all item prices before submitting.</p>`);
  const refresh=()=>showInvitation(id);
  app.querySelector('#quote-draft').onsubmit=e=>{
    e.preventDefault();const values=Object.fromEntries(new FormData(e.currentTarget));
    const body={currency:values.currency,validUntil:new Date(values.validUntil).toISOString(),terms:values.terms,items:invitation.items.filter(i=>values[`price-${i.id}`]!=='').map(i=>({rfqItemId:i.id,unitPrice:values[`price-${i.id}`]}))};
    procurementAction(async()=>{await request(base,{method:'PUT',body});await refresh();message('Draft saved.','success');},refresh);
  };
  app.querySelector('#submit-quote').onclick=()=>procurementAction(async()=>{await request(`${base}/submit`,{method:'POST'});await refresh();message('Quotation submitted.','success');},refresh);
}

async function loadEmployees() {
  const rows = await request('/api/employees');
  const host = app.querySelector('#employees');
  if (!host) return;
  host.innerHTML = `<h2>Employees and access</h2><p class="muted">Buyer and supplier grants independently control purchasing and supplier operations.</p><table><thead><tr><th>Employee</th><th>Verified</th><th>Admin</th><th>Buyer</th><th>Supplier</th><th>Active</th><th></th></tr></thead><tbody>${rows.map(person => `<tr data-id="${person.id}"><th scope="row">${escapeHtml(person.fullName)}<br><span class="muted">${escapeHtml(person.email)}</span></th><td>${person.emailVerified?'Yes':'No'}</td>${['organizationAdmin','buyerGrant','supplierGrant','active'].map(key => `<td><input aria-label="${key}" type="checkbox" data-key="${key}" ${person[key]?'checked':''} ${person.id===me.id && key==='organizationAdmin'?'disabled':''}></td>`).join('')}<td><button class="save secondary">Save</button></td></tr>`).join('')}</tbody></table>`;
  host.querySelectorAll('.save').forEach(button => button.addEventListener('click', async () => {
    const row = button.closest('tr'); const changes = Object.fromEntries([...row.querySelectorAll('[data-key]')].map(input => [input.dataset.key,input.checked]));
    try { await request(`/api/employees/${row.dataset.id}/access`,{method:'PUT',body:changes}); message('Employee access updated. Their existing session has been revoked.','success'); await loadEmployees(); }
    catch(error) { message(error.message,'error'); }
  }));
}

function showInvite() {
  form('Invite an employee', [{label:'Employee email',name:'email',type:'email',autocomplete:'email'}], 'Send invitation', async values => {
    const result = await request('/api/employees/invitations',{method:'POST',body:values}); message(result.message,'success'); showWorkspace();
  }, '<button class="secondary" type="button" id="cancel">Cancel</button>');
  app.querySelector('#cancel').addEventListener('click', showWorkspace);
}

function bindLogout() { app.querySelector('#logout').addEventListener('click', async () => { try { await request('/api/auth/logout',{method:'POST'}); me = null; showSignIn(); message('Signed out.','success'); } catch(error) { message(error.message,'error'); } }); }

async function showCatalog(state = {}) {
  const q = state.q || '', category = state.category || '', page = state.page || 0;
  app.setAttribute('aria-busy','true');
  app.innerHTML = '<h1>Marketplace</h1><p>Loading…</p>';
  try {
    if (!categoriesCache) categoriesCache = await request('/api/public/categories');
    const result = await request(`/api/public/offerings?${queryString({q, category, page})}`);
    const options = categoriesCache.map(c => `<option value="${c.code}" ${c.code===category?'selected':''}>${escapeHtml(c.name)}</option>`).join('');
    const cards = result.items.map(item => `<div class="card"><h3><a href="#" data-offering="${item.id}">${escapeHtml(item.title)}</a></h3><p class="muted">${escapeHtml(item.categoryName)} · <a href="#" data-supplier="${item.supplierId}">${escapeHtml(item.supplierDisplayName)}</a></p><p>${escapeHtml(item.description)}</p></div>`).join('');
    app.innerHTML = `<h1>Marketplace</h1><form class="inline" id="search"><label>Search<input name="q" value="${escapeHtml(q)}" placeholder="Search offerings" maxlength="100"></label><label>Category<select name="category"><option value="">All categories</option>${options}</select></label><button>Search</button></form>` +
      (result.items.length ? `<div class="cards">${cards}</div>` : '<p class="muted">No offerings match your search.</p>') +
      `<div class="pager"><button class="secondary" id="prev" ${page<=0?'disabled':''}>Previous</button><span class="muted">Page ${result.page+1} of ${Math.max(result.totalPages,1)} · ${result.totalElements} result(s)</span><button class="secondary" id="next" ${page+1>=result.totalPages?'disabled':''}>Next</button></div>`;
    app.querySelector('#search').addEventListener('submit', event => { event.preventDefault(); const v = Object.fromEntries(new FormData(event.currentTarget)); showCatalog({...v, page:0}); });
    app.querySelector('#prev').addEventListener('click', () => showCatalog({q, category, page: page-1}));
    app.querySelector('#next').addEventListener('click', () => showCatalog({q, category, page: page+1}));
    app.querySelectorAll('[data-offering]').forEach(link => link.addEventListener('click', event => { event.preventDefault(); showOfferingDetail(link.dataset.offering); }));
    app.querySelectorAll('[data-supplier]').forEach(link => link.addEventListener('click', event => { event.preventDefault(); showSupplierDetail(link.dataset.supplier); }));
  } catch (error) { app.innerHTML = '<h1>Marketplace</h1>'; message(error.message, 'error'); }
  app.setAttribute('aria-busy','false');
}

async function showOfferingDetail(id) {
  app.innerHTML = '<p>Loading…</p>';
  try {
    const offering = await request(`/api/public/offerings/${id}`);
    app.innerHTML = `<button class="secondary" id="back">Back to marketplace</button><h1>${escapeHtml(offering.title)}</h1><p class="muted">${escapeHtml(offering.categoryName)} · <a href="#" id="supplier-link">${escapeHtml(offering.supplierDisplayName)}</a></p><p>${escapeHtml(offering.description)}</p>`;
    app.querySelector('#supplier-link').addEventListener('click', event => { event.preventDefault(); showSupplierDetail(offering.supplierId); });
  } catch (error) {
    app.innerHTML = `<button class="secondary" id="back">Back to marketplace</button><p class="error">${error.status===404 ? 'This offering is no longer available.' : escapeHtml(error.message)}</p>`;
  }
  app.querySelector('#back').addEventListener('click', () => showCatalog());
}

async function showSupplierDetail(id) {
  app.innerHTML = '<p>Loading…</p>';
  try {
    const supplier = await request(`/api/public/suppliers/${id}`);
    const offerings = supplier.offerings.map(o => `<li><a href="#" data-offering="${o.id}">${escapeHtml(o.title)}</a> <span class="muted">${escapeHtml(o.categoryName)}</span></li>`).join('');
    app.innerHTML = `<button class="secondary" id="back">Back to marketplace</button><h1>${escapeHtml(supplier.displayName)}</h1><p>${escapeHtml(supplier.description)}</p><h2>Published offerings</h2>${supplier.offerings.length ? `<ul>${offerings}</ul>` : '<p class="muted">No published offerings yet.</p>'}`;
    app.querySelectorAll('[data-offering]').forEach(link => link.addEventListener('click', event => { event.preventDefault(); showOfferingDetail(link.dataset.offering); }));
  } catch (error) {
    app.innerHTML = `<button class="secondary" id="back">Back to marketplace</button><p class="error">${error.status===404 ? 'This supplier profile is not public.' : escapeHtml(error.message)}</p>`;
  }
  app.querySelector('#back').addEventListener('click', () => showCatalog());
}

async function loadMarketplace() {
  const host = app.querySelector('#marketplace');
  if (!host) return;
  host.innerHTML = 'Loading your supplier profile…';
  if (!categoriesCache) { try { categoriesCache = await request('/api/public/categories'); } catch { categoriesCache = []; } }
  let profile = null;
  try { profile = await request('/api/marketplace/profile'); } catch (error) { if (error.status !== 404) { host.innerHTML = `<p class="error">${escapeHtml(error.message)}</p>`; return; } }
  host.innerHTML = `<h2>Supplier marketplace profile</h2>` +
    (profile ? `<span class="badge ${profile.published?'published':'draft'}">${profile.published?'Published':'Draft'}</span><p class="muted">Unpublishing hides every offering from the public catalog without changing their own state; republishing restores only the ones that are still individually published.</p>` : '<p class="muted">Create a profile before you can add offerings.</p>') +
    `<form id="profile-form"><label>Public description<textarea name="description" maxlength="2000" required>${escapeHtml(profile ? profile.description : '')}</textarea></label><div class="actions"><button>${profile ? 'Save' : 'Create profile'}</button>` +
    (profile ? `<button type="button" class="secondary" id="profile-toggle">${profile.published ? 'Unpublish' : 'Publish'} profile</button>` : '') + '</div></form>' +
    '<h2>Offerings</h2><div id="offering-form-host"></div><div id="offerings-list">Loading…</div>';
  host.querySelector('#profile-form').addEventListener('submit', async event => {
    event.preventDefault(); message('');
    const values = Object.fromEntries(new FormData(event.currentTarget));
    try { await request('/api/marketplace/profile', {method:'PUT', body: values}); message('Profile saved.','success'); loadMarketplace(); }
    catch (error) { message(error.message,'error'); }
  });
  if (profile) {
    host.querySelector('#profile-toggle').addEventListener('click', async () => {
      try { await request(`/api/marketplace/profile/${profile.published?'unpublish':'publish'}`, {method:'POST'}); message(`Profile ${profile.published?'unpublished':'published'}.`,'success'); loadMarketplace(); }
      catch (error) { message(error.message,'error'); }
    });
    renderOfferingCreateButton();
    await loadOfferings();
  } else {
    host.querySelector('#offerings-list').innerHTML = '<p class="muted">No offerings yet — create a profile first.</p>';
  }
}

function renderOfferingCreateButton() {
  const formHost = app.querySelector('#offering-form-host');
  formHost.innerHTML = '<button type="button" class="secondary" id="new-offering">New offering</button>';
  formHost.querySelector('#new-offering').addEventListener('click', () => renderOfferingForm());
}

function renderOfferingForm(existing) {
  const formHost = app.querySelector('#offering-form-host');
  const categoryOptions = categoriesCache.map(c => `<option value="${c.code}" ${existing && existing.category===c.code?'selected':''}>${escapeHtml(c.name)}</option>`).join('');
  formHost.innerHTML = `<form id="offering-form"><h3>${existing ? 'Edit offering' : 'New offering'}</h3><label>Title<input name="title" maxlength="160" value="${existing?escapeHtml(existing.title):''}" required></label><label>Description<textarea name="description" maxlength="4000" required>${existing?escapeHtml(existing.description):''}</textarea></label><label>Category<select name="category" required><option value="">Choose a category</option>${categoryOptions}</select></label><div class="actions"><button>${existing?'Save':'Create'}</button><button type="button" class="secondary" id="offering-cancel">Cancel</button></div></form>`;
  formHost.querySelector('#offering-form').addEventListener('submit', async event => {
    event.preventDefault(); message('');
    const values = Object.fromEntries(new FormData(event.currentTarget));
    try {
      if (existing) await request(`/api/marketplace/offerings/${existing.id}`, {method:'PUT', body: values});
      else await request('/api/marketplace/offerings', {method:'POST', body: values});
      message(existing ? 'Offering saved.' : 'Offering created.','success'); renderOfferingCreateButton(); await loadOfferings();
    } catch (error) { message(error.message,'error'); }
  });
  formHost.querySelector('#offering-cancel').addEventListener('click', () => renderOfferingCreateButton());
}

async function loadOfferings() {
  const host = app.querySelector('#offerings-list');
  if (!host) return;
  try {
    const offerings = await request('/api/marketplace/offerings');
    if (!offerings.length) { host.innerHTML = '<p class="muted">No offerings yet. Create your first one above.</p>'; return; }
    const categoryName = code => (categoriesCache.find(c => c.code === code) || {}).name || code;
    host.innerHTML = `<div class="cards">${offerings.map(o => `<div class="card"><h3>${escapeHtml(o.title)} <span class="badge ${o.published?'published':'draft'}">${o.published?'Published':'Draft'}</span></h3><p class="muted">${escapeHtml(categoryName(o.category))}</p><p>${escapeHtml(o.description)}</p><div class="actions"><button class="secondary" data-edit="${o.id}">Edit</button><button class="secondary" data-toggle="${o.id}" data-published="${o.published}">${o.published?'Unpublish':'Publish'}</button></div></div>`).join('')}</div>`;
    host.querySelectorAll('[data-edit]').forEach(button => button.addEventListener('click', () => renderOfferingForm(offerings.find(o => String(o.id) === button.dataset.edit))));
    host.querySelectorAll('[data-toggle]').forEach(button => button.addEventListener('click', async () => {
      const published = button.dataset.published === 'true';
      try { await request(`/api/marketplace/offerings/${button.dataset.toggle}/${published?'unpublish':'publish'}`, {method:'POST'}); message(`Offering ${published?'unpublished':'published'}.`,'success'); await loadOfferings(); }
      catch (error) { message(error.message,'error'); }
    }));
  } catch (error) { host.innerHTML = `<p class="error">${escapeHtml(error.message)}</p>`; }
}

async function initialize() {
  document.querySelector('#nav-marketplace').addEventListener('click', event => { event.preventDefault(); showCatalog(); });
  try {
    csrf = (await request('/api/auth/csrf')).token;
    const query = new URLSearchParams(location.search);
    if (query.has('verify')) {
      await request('/api/auth/verify',{method:'POST',body:{token:query.get('verify')}});
      history.replaceState({}, '', '/'); message('Email verified. You can now sign in.','success');
      showSignIn();
    } else if (query.has('invite')) {
      const token = query.get('invite');
      history.replaceState({}, '', '/');
      form('Accept organization invitation',[{label:'Your name',name:'fullName',max:160},{label:'Password (12–128 characters)',name:'password',type:'password',autocomplete:'new-password'}],'Accept invitation',async values=>{
        await request('/api/auth/invitations/accept',{method:'POST',body:{...values,token}}); message('Invitation accepted. Check your email to verify before signing in.','success'); showSignIn();
      });
    } else {
      try { me = await request('/api/auth/me'); showWorkspace(); }
      catch { showSignIn(); }
    }
  } catch(error) { message(error.message,'error'); showSignIn(); }
  app.setAttribute('aria-busy','false');
}

initialize();
