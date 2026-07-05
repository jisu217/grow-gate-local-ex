import assert from 'node:assert/strict'
import { resolveApiBase } from './apiClient.js'

assert.equal(
  resolveApiBase('', 'https://growgate.54.180.56.24.sslip.io', 'growgate.54.180.56.24.sslip.io'),
  'https://growgate.54.180.56.24.sslip.io/api',
)

assert.equal(
  resolveApiBase('http://localhost:8080', 'https://growgate.54.180.56.24.sslip.io', 'growgate.54.180.56.24.sslip.io'),
  'https://growgate.54.180.56.24.sslip.io/api',
)

assert.equal(
  resolveApiBase('http://localhost:8080', 'http://localhost:5173', 'localhost'),
  'http://localhost:8080/api',
)

assert.equal(
  resolveApiBase('https://api.example.com/api', 'https://growgate.local', 'growgate.local'),
  'https://api.example.com/api',
)

console.log('api client base URL regression checks passed')
