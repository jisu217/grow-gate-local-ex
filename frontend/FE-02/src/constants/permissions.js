export const PERMISSIONS = {
  'knowledge:create': ['owner', 'admin'],
  'knowledge:edit': ['owner', 'admin', 'editor'],
  'knowledge:view': ['owner', 'admin', 'editor', 'viewer'],

  'document:upload': ['owner', 'admin', 'editor'],
  'document:download': ['owner', 'admin', 'editor', 'viewer'],

  'diagnosis:run': ['owner', 'admin', 'editor'],
  'diagnosis:save': ['owner', 'admin'],

  'audit:view': ['owner', 'admin'],

  'member:invite': ['owner'],
  'member:remove': ['owner'],
}
