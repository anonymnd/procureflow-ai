ALTER TABLE marketplace_offerings DROP CONSTRAINT marketplace_offerings_category_check;
ALTER TABLE marketplace_offerings ADD CONSTRAINT marketplace_offerings_category_check
    CHECK (category IN ('IT_EQUIPMENT', 'OFFICE_SUPPLIES', 'FURNITURE', 'INDUSTRIAL_SUPPLIES', 'OTHER',
                        'SAFETY_EQUIPMENT', 'PACKAGING', 'ELECTRICAL', 'TOOLS', 'CLEANING'));
