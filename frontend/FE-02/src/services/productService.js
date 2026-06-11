import { PRODUCTS, PRODUCT_DETAILS } from '../mock/productData.js'

// 연동 시 함수 내부를 fetch로 교체
export async function getProducts() {
  return PRODUCTS
}

export async function getProductKnowledge(productId) {
  const product = PRODUCT_DETAILS[productId]
  if (!product) throw new Error(`제품을 찾을 수 없습니다. (id: ${productId})`)
  return product
}

export async function saveProductKnowledge(productId, payload) {
  return { ...PRODUCT_DETAILS[productId], ...payload, updatedAt: new Date().toISOString().slice(0, 10) }
}

export async function createProduct(payload) {
  return { id: `prod-${Date.now()}`, ...payload }
}
