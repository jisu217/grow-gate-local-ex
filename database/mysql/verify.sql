USE growgate;

SELECT 'companies' AS table_name, COUNT(*) AS row_count FROM companies
UNION ALL SELECT 'users', COUNT(*) FROM users
UNION ALL SELECT 'memberships', COUNT(*) FROM memberships
UNION ALL SELECT 'products', COUNT(*) FROM products
UNION ALL SELECT 'product_knowledge', COUNT(*) FROM product_knowledge
UNION ALL SELECT 'certifications', COUNT(*) FROM certifications
UNION ALL SELECT 'fto', COUNT(*) FROM fto
UNION ALL SELECT 'buyer_requirements', COUNT(*) FROM buyer_requirements
UNION ALL SELECT 'knowledge_sources', COUNT(*) FROM knowledge_sources
UNION ALL SELECT 'audit_logs', COUNT(*) FROM audit_logs
UNION ALL SELECT 'diagnosis_runs', COUNT(*) FROM diagnosis_runs;

SELECT
  c.name AS company_name,
  u.email AS owner_email,
  m.role AS owner_role,
  p.name AS product_name,
  pk.trust_grade,
  dr.status AS latest_diagnosis_status,
  dr.target_country
FROM companies c
JOIN memberships m ON m.company_id = c.id
JOIN users u ON u.id = m.user_id
JOIN products p ON p.company_id = c.id
LEFT JOIN product_knowledge pk ON pk.product_id = p.id
LEFT JOIN diagnosis_runs dr ON dr.product_id = p.id
WHERE u.email = 'owner@growgate.local'
ORDER BY dr.created_at DESC;
