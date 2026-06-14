// 제품 지식 서비스
import { PRODUCTS, PRODUCT_DETAILS } from '../mock/productData.js'

// 전체 제품 목록 조회
export async function getProducts() {
  return PRODUCTS
}

// 특정 제품 지식 상세 조회
export async function getProductKnowledge(productId) {
  const product = PRODUCT_DETAILS[productId]
  if (!product) throw new Error(`제품을 찾을 수 없습니다. (id: ${productId})`)
  return product
}

// 제품 지식 저장
export async function saveProductKnowledge(productId, payload) {
  return { ...PRODUCT_DETAILS[productId], ...payload, updatedAt: new Date().toISOString().slice(0, 10) }
}

// 신규 제품 생성
export async function createProduct(payload) {
  return { id: `prod-${Date.now()}`, ...payload }
}
