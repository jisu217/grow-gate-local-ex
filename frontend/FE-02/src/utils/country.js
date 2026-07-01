export const COUNTRY_OPTIONS = ['DE', 'US', 'JP', 'ASEAN', 'CN', 'EU']

const COUNTRY_ALIASES = {
  독일: 'DE',
  germany: 'DE',
  de: 'DE',
  미국: 'US',
  usa: 'US',
  us: 'US',
  일본: 'JP',
  japan: 'JP',
  jp: 'JP',
  중국: 'CN',
  china: 'CN',
  cn: 'CN',
  유럽: 'EU',
  europe: 'EU',
  eu: 'EU',
  asean: 'ASEAN',
  아세안: 'ASEAN',
}

export function normalizeTargetCountry(value) {
  const normalized = String(value ?? '').trim()
  const key = normalized.toLowerCase().replace(/\s+/g, '')
  return COUNTRY_ALIASES[key] ?? normalized.toUpperCase()
}
