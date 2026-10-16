-- Relabel only original, unedited fictional fixture fields. No schema/auth/grant change.
DO $ct$
DECLARE r demo_seed_completions%ROWTYPE; anchor timestamptz;
BEGIN
 PERFORM pg_advisory_xact_lock(792431002);
 SELECT * INTO r FROM demo_seed_completions WHERE fixture_key='production-demo-v1';
 IF NOT FOUND THEN RETURN; END IF;
 IF r.revision IS DISTINCT FROM 1 OR cardinality(r.organization_ids) IS DISTINCT FROM 3
  OR cardinality(r.account_ids) IS DISTINCT FROM 6
  OR (SELECT count(DISTINCT x) FROM unnest(r.organization_ids) x)<>3
  OR (SELECT count(DISTINCT x) FROM unnest(r.account_ids) x)<>6
  OR (SELECT count(*) FROM organizations WHERE id=ANY(r.organization_ids) AND created_at=r.completed_at-interval '1 hour')<>3
 THEN RAISE EXCEPTION 'Sample relabeling refused: incompatible completion receipt'; END IF;
 IF (SELECT count(*) FROM (VALUES (1,'demo.buyer@procureflow.invalid','amina.bennani@northstar.invalid','Demo Buyer Admin','Amina Bennani'),
(2,'demo.buyer.employee@procureflow.invalid','youssef.amrani@northstar.invalid','Demo Buyer Employee','Youssef Amrani'),
(3,'demo.atlas@procureflow.invalid','karim.mansouri@atlas-supply.invalid','Demo Atlas Admin','Karim El Mansouri'),
(4,'demo.atlas.employee@procureflow.invalid','salma.idrissi@atlas-supply.invalid','Demo Atlas Employee','Salma Idrissi'),
(5,'demo.meridian@procureflow.invalid','lina.haddad@meridian-goods.invalid','Demo Meridian Admin','Lina Haddad'),
(6,'demo.meridian.employee@procureflow.invalid','omar.tazi@meridian-goods.invalid','Demo Meridian Employee','Omar Tazi')) m(n,old_email,new_email,old_name,new_name)
  JOIN accounts a ON a.id=r.account_ids[m.n] AND a.organization_id=r.organization_ids[(m.n+1)/2]
   AND a.email IN(m.old_email,m.new_email))<>6
 THEN RAISE EXCEPTION 'Sample relabeling refused: identity binding changed'; END IF;
 anchor:=r.completed_at-interval '1 hour';
 UPDATE accounts a SET full_name=CASE WHEN a.full_name=m.old_name THEN m.new_name ELSE a.full_name END,
  email=CASE WHEN a.email=m.old_email THEN m.new_email ELSE a.email END
 FROM (VALUES (1,'demo.buyer@procureflow.invalid','amina.bennani@northstar.invalid','Demo Buyer Admin','Amina Bennani'),
(2,'demo.buyer.employee@procureflow.invalid','youssef.amrani@northstar.invalid','Demo Buyer Employee','Youssef Amrani'),
(3,'demo.atlas@procureflow.invalid','karim.mansouri@atlas-supply.invalid','Demo Atlas Admin','Karim El Mansouri'),
(4,'demo.atlas.employee@procureflow.invalid','salma.idrissi@atlas-supply.invalid','Demo Atlas Employee','Salma Idrissi'),
(5,'demo.meridian@procureflow.invalid','lina.haddad@meridian-goods.invalid','Demo Meridian Admin','Lina Haddad'),
(6,'demo.meridian.employee@procureflow.invalid','omar.tazi@meridian-goods.invalid','Demo Meridian Employee','Omar Tazi')) m(n,old_email,new_email,old_name,new_name)
 WHERE a.id=r.account_ids[m.n];
 UPDATE organizations o SET display_name=m.new_name FROM (VALUES (1,'Demo Buyer Workshop','Northstar Manufacturing'),
(2,'Demo Atlas Supply','Atlas Industrial Supply'),
(3,'Demo Meridian Goods','Meridian Business Goods')) m(n,old_name,new_name)
 WHERE o.id=r.organization_ids[m.n] AND o.display_name=m.old_name;
 UPDATE marketplace_profiles SET description='Industrial and workplace supplies with responsive quotation support.' WHERE organization_id=ANY(r.organization_ids[2:3])
  AND created_at=anchor AND description='Demo fictional supplier; no real fulfillment.';
 UPDATE marketplace_offerings o SET title=CASE WHEN o.title=m.old_name THEN m.new_name ELSE o.title END,
  description=CASE WHEN o.description='Demo fictional product for exploring sourcing; no real fulfillment.' THEN 'Commercial-grade supplies for workplace and facility operations.' ELSE o.description END
 FROM marketplace_profiles p,(VALUES (2,'IT_EQUIPMENT','Demo Atlas it equipment','Business Laptop 14-inch'),
(2,'OFFICE_SUPPLIES','Demo Atlas office supplies','A4 Copy Paper 80gsm'),
(2,'FURNITURE','Demo Atlas furniture','Ergonomic Task Chair'),
(2,'INDUSTRIAL_SUPPLIES','Demo Atlas industrial supplies','Heavy-Duty Fastener Kit'),
(2,'OTHER','Demo Atlas other','Workshop Maintenance Kit'),
(2,'SAFETY_EQUIPMENT','Demo Atlas safety equipment','Nitrile Safety Gloves'),
(2,'PACKAGING','Demo Atlas packaging','Corrugated Shipping Boxes'),
(2,'ELECTRICAL','Demo Atlas electrical','LED Panel Light 36W'),
(2,'TOOLS','Demo Atlas tools','Precision Hand Tool Set'),
(2,'CLEANING','Demo Atlas cleaning','Industrial Surface Cleaner 5L'),
(3,'IT_EQUIPMENT','Demo Meridian it equipment','Office Monitor 24-inch'),
(3,'OFFICE_SUPPLIES','Demo Meridian office supplies','Ballpoint Pen Set'),
(3,'FURNITURE','Demo Meridian furniture','Adjustable Office Desk'),
(3,'INDUSTRIAL_SUPPLIES','Demo Meridian industrial supplies','Stainless Steel Bolt Set'),
(3,'OTHER','Demo Meridian other','Facility Service Kit'),
(3,'SAFETY_EQUIPMENT','Demo Meridian safety equipment','Protective Safety Goggles'),
(3,'PACKAGING','Demo Meridian packaging','Reinforced Packing Tape'),
(3,'ELECTRICAL','Demo Meridian electrical','Commercial LED Downlight'),
(3,'TOOLS','Demo Meridian tools','Cordless Drill Kit'),
(3,'CLEANING','Demo Meridian cleaning','Multi-Surface Detergent 5L')) m(n,category,old_name,new_name)
 WHERE o.profile_id=p.id AND p.organization_id=r.organization_ids[m.n] AND p.created_at=anchor
  AND o.created_at=anchor AND o.category=m.category;
 UPDATE supplier_contacts SET note='Approved quotation contact.' WHERE buyer_organization_id=r.organization_ids[1]
  AND supplier_organization_id=ANY(r.organization_ids[2:3]) AND created_at=anchor AND note='Demo fictional supplier contact.';
 UPDATE rfq_items i SET description=m.spec FROM rfqs f,(VALUES ('Demo / fictional: Draft specifications','Office furniture procurement','Ergonomic task chair with adjustable lumbar support.'),
('Demo / fictional: Awaiting supplier responses','Warehouse protective equipment','Powder-free nitrile gloves, size L.'),
('Demo / fictional: Compare two quotations','Packaging materials replenishment','Double-wall corrugated shipping boxes, 40 x 30 x 30 cm.'),
('Demo / fictional: Order created','IT equipment refresh','Business laptop with 16 GB RAM and 512 GB SSD.'),
('Demo / fictional: Order processing','Production workshop supplies','Zinc-plated M8 fastener assortment for workshop use.'),
('Demo / fictional: Order dispatched','Lighting upgrade','36W LED panels with neutral white output.'),
('Demo / fictional: Order received','Office stationery restock','A4 white copy paper, 80 gsm, 500 sheets per ream.')) m(old_title,new_title,spec)
 WHERE i.rfq_id=f.id AND f.organization_id=r.organization_ids[1] AND f.created_at=anchor
  AND i.created_at=anchor AND f.title=m.old_title AND i.description='Demo fictional office supply specification; no real fulfillment.';
 UPDATE purchase_order_items i SET description=m.spec FROM purchase_orders o,rfqs f,(VALUES ('Demo / fictional: Draft specifications','Office furniture procurement','Ergonomic task chair with adjustable lumbar support.'),
('Demo / fictional: Awaiting supplier responses','Warehouse protective equipment','Powder-free nitrile gloves, size L.'),
('Demo / fictional: Compare two quotations','Packaging materials replenishment','Double-wall corrugated shipping boxes, 40 x 30 x 30 cm.'),
('Demo / fictional: Order created','IT equipment refresh','Business laptop with 16 GB RAM and 512 GB SSD.'),
('Demo / fictional: Order processing','Production workshop supplies','Zinc-plated M8 fastener assortment for workshop use.'),
('Demo / fictional: Order dispatched','Lighting upgrade','36W LED panels with neutral white output.'),
('Demo / fictional: Order received','Office stationery restock','A4 white copy paper, 80 gsm, 500 sheets per ream.')) m(old_title,new_title,spec)
 WHERE i.order_id=o.id AND o.rfq_id=f.id AND f.organization_id=r.organization_ids[1] AND f.created_at=anchor
  AND o.accepting_account_id=r.account_ids[1] AND o.accepted_at=anchor+interval '20 minutes'
  AND o.supplier_organization_id=ANY(r.organization_ids[2:3]) AND f.title=m.old_title AND i.description='Demo fictional office supply specification; no real fulfillment.';
 UPDATE purchase_orders o SET buyer_display_name=CASE WHEN o.buyer_display_name='Demo Buyer Workshop' THEN 'Northstar Manufacturing' ELSE o.buyer_display_name END,
  supplier_display_name=CASE WHEN o.supplier_organization_id=r.organization_ids[2] AND o.supplier_display_name='Demo Atlas Supply' THEN 'Atlas Industrial Supply' WHEN o.supplier_organization_id=r.organization_ids[3] AND o.supplier_display_name='Demo Meridian Goods' THEN 'Meridian Business Goods' ELSE o.supplier_display_name END,
  rfq_title=CASE WHEN o.rfq_title=m.old_title THEN m.new_title ELSE o.rfq_title END,
  terms=CASE WHEN o.terms='Demo / fictional transaction; no payment or real fulfillment. Illustrative terms only.' THEN 'Payment within 30 days. Tax and shipping quoted separately.' ELSE o.terms END
 FROM rfqs f,(VALUES ('Demo / fictional: Draft specifications','Office furniture procurement','Ergonomic task chair with adjustable lumbar support.'),
('Demo / fictional: Awaiting supplier responses','Warehouse protective equipment','Powder-free nitrile gloves, size L.'),
('Demo / fictional: Compare two quotations','Packaging materials replenishment','Double-wall corrugated shipping boxes, 40 x 30 x 30 cm.'),
('Demo / fictional: Order created','IT equipment refresh','Business laptop with 16 GB RAM and 512 GB SSD.'),
('Demo / fictional: Order processing','Production workshop supplies','Zinc-plated M8 fastener assortment for workshop use.'),
('Demo / fictional: Order dispatched','Lighting upgrade','36W LED panels with neutral white output.'),
('Demo / fictional: Order received','Office stationery restock','A4 white copy paper, 80 gsm, 500 sheets per ream.')) m(old_title,new_title,spec)
 WHERE o.rfq_id=f.id AND f.organization_id=r.organization_ids[1] AND f.created_at=anchor
  AND o.accepting_account_id=r.account_ids[1] AND o.accepted_at=anchor+interval '20 minutes'
  AND o.supplier_organization_id=ANY(r.organization_ids[2:3]) AND f.title=m.old_title;
 UPDATE quotations q SET terms='Payment within 30 days. Tax and shipping quoted separately.' FROM rfq_invitations i,rfqs f
 WHERE q.invitation_id=i.id AND i.rfq_id=f.id AND f.organization_id=r.organization_ids[1]
  AND f.created_at=anchor AND i.created_at=anchor AND i.supplier_organization_id=ANY(r.organization_ids[2:3])
  AND q.submitted_at=anchor+interval '10 minutes' AND q.terms='Demo / fictional transaction; no payment or real fulfillment. Illustrative terms only.';
 UPDATE rfqs f SET title=m.new_title FROM (VALUES ('Demo / fictional: Draft specifications','Office furniture procurement','Ergonomic task chair with adjustable lumbar support.'),
('Demo / fictional: Awaiting supplier responses','Warehouse protective equipment','Powder-free nitrile gloves, size L.'),
('Demo / fictional: Compare two quotations','Packaging materials replenishment','Double-wall corrugated shipping boxes, 40 x 30 x 30 cm.'),
('Demo / fictional: Order created','IT equipment refresh','Business laptop with 16 GB RAM and 512 GB SSD.'),
('Demo / fictional: Order processing','Production workshop supplies','Zinc-plated M8 fastener assortment for workshop use.'),
('Demo / fictional: Order dispatched','Lighting upgrade','36W LED panels with neutral white output.'),
('Demo / fictional: Order received','Office stationery restock','A4 white copy paper, 80 gsm, 500 sheets per ream.')) m(old_title,new_title,spec)
 WHERE f.organization_id=r.organization_ids[1] AND f.created_at=anchor AND f.title=m.old_title;
END $ct$;
