import React, {useEffect, useRef, useState} from 'react';
import {createRoot} from 'react-dom/client';
import '@fontsource/manrope/latin-400.css';
import '@fontsource/manrope/latin-500.css';
import '@fontsource/manrope/latin-600.css';
import '@fontsource/manrope/latin-700.css';
import '@fontsource/manrope/latin-800.css';
import {mountFeatures} from './features.js';
import './styles.css';
import './workspace.css';

const paths = {
  arrow: 'M5 12h14m-6-6 6 6-6 6', grid: 'M3 3h7v7H3zm11 0h7v7h-7zM3 14h7v7H3zm11 0h7v7h-7z',
  search: 'm21 21-5-5M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0',
  file: 'M14 2H5v20h14V7zm0 0v5h5M8 12h8M8 16h6',
  box: 'm3 7 9-5 9 5v10l-9 5-9-5zm0 0 9 5 9-5m-9 5v10M7 4l10 6',
  users: 'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2m20 0v-2a4 4 0 0 0-3-3.87M9 3a4 4 0 1 1 0 8 4 4 0 0 1 0-8m8 .13a4 4 0 0 1 0 7.75',
  chart: 'M3 3v18h18M7 15l4-4 4 2 6-8', check: 'm5 12 4 4L19 6',
  flow: 'M4 3h6v6H4zm10 12h6v6h-6zM7 9v9h7M10 6h7v9',
  menu: 'M4 6h16M4 12h16M4 18h16', close: 'm6 6 12 12M6 18 18 6',
  shield: 'm12 2 8 4v6c0 5-8 10-8 10S4 17 4 12V6zm-4 10 3 3 5-6'
};
function Icon({name, size=20, ...props}) {
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" {...props}><path d={paths[name] || paths.file}/></svg>;
}
function Brand({onClick, inverse=false}) {
  return <a href="/" onClick={onClick} className={`brand ${inverse?'brand-inverse':''}`} aria-label="ProcureFlow home"><span className="brand-symbol" aria-hidden="true"><i/><i/><i/></span>Procure<span>Flow</span><span className="brand-dot">.</span></a>;
}
function ExampleSheet() {
  return <div className="hero-art" aria-label="Illustrative procurement workflow, not live account data">
    <div className="art-orbit" aria-hidden="true"/>
    <div className="sheet">
      <div className="sheet-head"><span className="sheet-brand"><span className="small-mark"/>ProcureFlow</span><span className="example-tag">Example workflow</span></div>
      <div className="sheet-content">
        <div className="sheet-label">REQUEST FOR QUOTATION</div>
        <h2>Your next great<br/>supplier starts here.</h2>
        <div className="sheet-description"><Icon name="file" size={17}/><span>Packaging for the next production run</span></div>
        <div className="quote-columns"><span>SUPPLIER</span><span>STATUS</span><span>YOUR NEXT STEP</span></div>
        <div className="quote-line"><span><b className="supplier-avatar">A</b>Atlas Packaging</span><span className="tag tag-purple">Submitted</span><Icon name="arrow" size={16}/></div>
        <div className="quote-line"><span><b className="supplier-avatar avatar-pink">M</b>Meridian Supply</span><span className="tag tag-purple">Submitted</span><Icon name="arrow" size={16}/></div>
        <div className="quote-line"><span><b className="supplier-avatar avatar-orange">N</b>Northline Studio</span><span className="tag tag-neutral">Invited</span><span className="line-dot">—</span></div>
        <div className="sheet-bottom"><span><Icon name="shield" size={16}/>Your decision. Your order.</span><span className="sheet-action">Compare quotations <Icon name="arrow" size={15}/></span></div>
      </div>
    </div>
    <div className="receipt-slip"><span className="receipt-check"><Icon name="check" size={20}/></span><span><strong>Every step, connected.</strong><small>Request → quotation → order</small></span></div>
    <span className="art-caption">A clearer view of what comes next.</span>
  </div>;
}
function Landing({go}) {
  const steps=[['Tell suppliers what you need.','Create a private RFQ with quantities and specifications. Invite the suppliers you want to hear from.','file'],['Compare with confidence.','Review submitted quotations together. You choose the supplier; an award creates the order.','chart'],['Keep the order moving.','Follow confirmation, processing and dispatch. Record receipt when the complete order arrives.','box']];
  return <div className="landing">
    <section className="hero content-width" aria-labelledby="hero-title">
      <div className="hero-copy"><h1 id="hero-title">Good sourcing.<br/>Great connections.<br/><span>One clear flow.</span></h1><p>From finding the right supplier to receiving your order. Bring your people, quotations and next steps together in one workspace.</p>
        <div className="hero-actions"><a className="button button-orange" href="?view=register" aria-label="Register an organization" onClick={e=>{e.preventDefault();go('register');}}>Start sourcing <Icon name="arrow"/></a><a className="button button-plain" href="?view=marketplace" onClick={e=>{e.preventDefault();go('marketplace');}}>Explore the marketplace <Icon name="search" size={18}/></a></div>
        <div className="hero-note"><span className="note-line"/>Made for buyers. Built for suppliers.</div>
      </div><ExampleSheet/>
    </section>
    <div className="capability-strip"><div className="content-width"><span><Icon name="users"/>Supplier discovery</span><span><Icon name="file"/>Private RFQs</span><span><Icon name="chart"/>Quotation comparison</span><span><Icon name="box"/>Order tracking</span></div></div>
    <section id="process" className="process-section content-width" aria-labelledby="process-title"><div className="section-heading"><h2 id="process-title">Less back-and-forth.<br/>More moving forward.</h2><p>A connected process for the work<br className="desktop-break"/> between “we need” and “received”.</p></div><ol className="process-list">{steps.map(([title,copy,icon],i)=><li key={title}><div className="step-top"><span className="step-number">0{i+1}</span><Icon name={icon} size={27}/></div><h3>{title}</h3><p>{copy}</p></li>)}</ol></section>
    <section className="workspace-story content-width" aria-labelledby="workspace-title"><div className="story-panel"><div className="story-top"><span>YOUR WORKSPACE</span><span className="story-live"><span/>Connected by design</span></div><h2 id="workspace-title">Different roles.<br/>Same page.</h2><p>Give your team the access they need. Keep supplier conversations private and your next action easy to find.</p><a href="?view=login" className="story-link" onClick={e=>{e.preventDefault();go('login');}}>Open your workspace <Icon name="arrow"/></a></div><div className="story-details"><div><Icon name="users" size={25}/><h3>The right access.</h3><p>Organization administrators manage employee access and buyer or supplier permissions.</p></div><div><Icon name="flow" size={25}/><h3>A visible next step.</h3><p>Operational queues keep RFQs, invitations and orders organized by their current status.</p></div><div><Icon name="shield" size={25}/><h3>Private stays private.</h3><p>Published offerings are discoverable. Commercial records stay within their authorized relationships.</p></div></div></section>
    <section className="closing-section"><div className="content-width"><div><h2>Make room for<br/><span>better sourcing.</span></h2><p>Your next supplier relationship starts with a conversation.</p></div><a href="?view=register" className="button button-orange" onClick={e=>{e.preventDefault();go('register');}}>Create your organization <Icon name="arrow"/></a></div></section>
  </div>;
}
function Shell() {
  const featureHost=useRef(null), noticeHost=useRef(null), controller=useRef(null), navRevision=useRef(0);
  const initialQuery=new URLSearchParams(location.search);
  const [screen,setScreen]=useState(initialQuery.has('verify')||initialQuery.has('invite')?'login':initialQuery.get('view')||'home');
  const [activeView,setActiveView]=useState('workspace');
  const [account,setAccount]=useState(null),[title,setTitle]=useState(''),[ready,setReady]=useState(false),[pending,setPending]=useState(false),[menu,setMenu]=useState(false);
  const isHome=screen==='home', isAuth=!isHome && ['Sign in','Create an organization','Accept organization invitation','Verify your email'].includes(title);
  async function go(target,{push=true}={}) {
    setMenu(false); const revision=++navRevision.current; setPending(true);
    try {
      const navigated=await controller.current?.navigate(target);
      if(revision!==navRevision.current||!navigated)return;
      setScreen(target);
      if(push)history.pushState({},'',target==='home'?'/':`/?view=${target}`);
      window.scrollTo({top:0,behavior:'instant'});
      requestAnimationFrame(()=>{const heading=target==='home'?document.querySelector('#hero-title'):featureHost.current?.querySelector('h1');if(heading){heading.tabIndex=-1;heading.focus({preventScroll:true});}});
    } finally {if(revision===navRevision.current)setPending(false);}
  }
  useEffect(()=>{
    let alive=true;
    const hadToken=initialQuery.has('verify')||initialQuery.has('invite');
    const requested=initialQuery.get('view');
    controller.current=mountFeatures(featureHost.current,noticeHost.current,info=>{if(!alive)return;setAccount(info.account);setTitle(info.title);setActiveView(info.activeView);});
    controller.current.ready.then(async()=>{
      if(!alive)return;
      if(requested && navRevision.current===0)await go(requested,{push:false});
      else if(!hadToken && navRevision.current===0 && controller.current.account && alive)setScreen('workspace');
      if(alive)setReady(true);
    });
    function pop(){const q=new URLSearchParams(location.search);go(q.get('view')||'home',{push:false});}
    function escape(event){if(event.key==='Escape')setMenu(false);}
    window.addEventListener('popstate',pop);window.addEventListener('keydown',escape);
    return()=>{alive=false;navRevision.current++;controller.current?.dispose();window.removeEventListener('popstate',pop);window.removeEventListener('keydown',escape);};
  },[]);
  const navItems=[...(account?.buyerGrant?[['marketplace','Browse products','search']]:[]),['workspace','Overview','grid'],...(account?.buyerGrant?[['rfqs','Sourcing requests','file'],['contacts','Supplier contacts','users']]:[]),...(account?.supplierGrant?[['products','Your products','box'],['invitations','Received RFQs','file']]:[]),...(account?.buyerGrant||account?.supplierGrant?[['quotations','Quotations','file'],['orders','Orders & tracking','box'],['queues','Operational queues','flow'],['analytics','Analytics','chart']]:[]),...(account?.supplierGrant?[['supplier-profile','Supplier profile','users']]:[]),['profile','My profile','users'],...(account?.organizationAdmin?[['team','Team & access','users']]:[]),['help','Help & information','shield']];
  const isWorkspace=!isHome&&!isAuth&&account;
  return <div className={`site ${isHome?'home-mode':isAuth?'auth-mode':'feature-mode'} ${isWorkspace?'workspace-mode':''}`}>
    <a className="skip-link" href="#main-content">Skip to content</a>
    <header className="site-header"><div className="header-inner"><Brand onClick={e=>{e.preventDefault();go('home');}}/>
      <button className="menu-toggle" aria-expanded={menu} aria-controls="primary-nav" aria-label={menu?'Close navigation':'Open navigation'} onClick={()=>setMenu(!menu)}><Icon name={menu?'close':'menu'}/></button>
      <nav id="primary-nav" className={menu?'nav-open':''} aria-label="Main navigation"><a id="nav-marketplace" href="?view=marketplace" onClick={e=>{e.preventDefault();go('marketplace');}} aria-current={!isHome && activeView==='marketplace'?'page':undefined}>Marketplace</a><a href="/#process">How it works</a><a href="?view=workspace" onClick={e=>{e.preventDefault();go(account?'workspace':'login');}}>Workspace</a><span className="nav-divider"/>{account?<a className="button button-purple" href="?view=workspace" onClick={e=>{e.preventDefault();go('workspace');}}>Your organization <Icon name="arrow" size={17}/></a>:<><a className="sign-in-link" href="?view=login" onClick={e=>{e.preventDefault();go('login');}}>Sign in</a><a className="button button-purple" href="?view=register" onClick={e=>{e.preventDefault();go('register');}}>Get started <Icon name="arrow" size={17}/></a></>}</nav>
    </div></header>
    <div className="navigation-progress" role="status">{pending?'Opening your next view…':!ready?'Connecting your workspace…':''}</div>
    <div className="notice-wrap"><div ref={noticeHost} id="notice" role="status" aria-live="polite"/></div>
    <main id="main-content" className={isWorkspace?'workspace-layout':''}>
      {isHome&&<Landing go={go}/>}
      <aside className="workspace-sidebar" hidden={!isWorkspace} aria-label="Workspace navigation"><div className="organization-label"><span className="organization-avatar">{account?.organizationName?.slice(0,1)||'P'}</span><span><strong>{account?.organizationName}</strong><small>{account?.organizationAdmin?'Organization administrator':'Organization member'}</small></span></div><div className="sidebar-label">YOUR WORK</div><nav>{navItems.map(([key,label,icon])=><a key={key} href={`?view=${key}`} onClick={e=>{e.preventDefault();go(key);}} className={activeView===key?'active':''} aria-current={activeView===key?'page':undefined}><Icon name={icon}/>{label}</a>)}</nav><div className="sidebar-bottom"><Icon name="shield"/><span>Your organization.<br/><strong>Your permissions.</strong></span></div></aside>
      <div className="feature-stage" hidden={isHome}>
        <aside className="auth-story" hidden={!isAuth}><span className="auth-story-logo"><Icon name="flow" size={34}/></span><h2>A clear path <br/>from request <br/>to <span>received.</span></h2><p>One place for your supplier relationships and the work that moves them forward.</p><div className="auth-story-bottom"><span>ProcureFlow</span><span>Sourcing, connected.</span></div></aside>
        <div className="feature-body"><div className="workspace-page-heading" hidden={isAuth}><span>ProcureFlow workspace</span><span className="account-name">{account?.fullName||'Public marketplace'}</span></div><section id="app" ref={featureHost} className="feature-view" aria-label="Application content" aria-busy="true"/></div>
      </div>
    </main>
    <footer className="site-footer"><div className="content-width"><Brand onClick={e=>{e.preventDefault();go('home');}}/><span>Sourcing, connected.</span><a href="?view=marketplace" onClick={e=>{e.preventDefault();go('marketplace');}}>Browse the marketplace <Icon name="arrow" size={16}/></a><small>© {new Date().getFullYear()} ProcureFlow</small></div></footer>
  </div>;
}
createRoot(document.getElementById('root')).render(<Shell/>);
