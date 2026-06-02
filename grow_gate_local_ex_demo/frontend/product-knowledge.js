const API_BASE = window.location.protocol === "file:" ? "http://127.0.0.1:8000" : "";

const form = document.getElementById("productKnowledgeForm");
const listEl = document.getElementById("knowledgeList");
const previewEl = document.getElementById("featurePreview");
const countEl = document.getElementById("knowledgeCount");
const statusEl = document.getElementById("knowledgeStatus");
const sampleButton = document.getElementById("loadSampleKnowledge");

const splitList = (value) =>
  String(value || "")
    .split(/[\n,]/)
    .map((item) => item.trim())
    .filter(Boolean);

const formValue = (name) => form.elements[name]?.value || "";

const buildPayload = () => {
  const documentTitle = formValue("doc_title").trim();
  const documents = documentTitle
    ? [
        {
          title: documentTitle,
          document_type: formValue("doc_type").trim() || "evidence",
          reliability: formValue("doc_reliability").trim() || "medium",
          source_url: formValue("doc_url").trim(),
          summary: formValue("doc_summary").trim(),
        },
      ]
    : [];

  return {
    company_name: formValue("company_name").trim(),
    product_name: formValue("product_name").trim(),
    hs_code: formValue("hs_code").trim(),
    target_market: formValue("target_market").trim(),
    product_family: formValue("product_family").trim(),
    features: splitList(formValue("features")),
    certifications: splitList(formValue("certifications")),
    materials: splitList(formValue("materials")),
    buyer_requirements: splitList(formValue("buyer_requirements")),
    recall_keywords: splitList(formValue("recall_keywords")),
    fto_keywords: splitList(formValue("fto_keywords")),
    documents,
    owner: formValue("owner").trim() || "미지정",
    status: formValue("status").trim() || "draft",
  };
};

const setStatus = (message, kind = "") => {
  statusEl.textContent = message;
  statusEl.className = `form-status ${kind}`.trim();
};

async function loadKnowledge() {
  try {
    const response = await fetch(`${API_BASE}/product-knowledge`);
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const payload = await response.json();
    renderKnowledgeList(payload.items || []);
    countEl.textContent = payload.total || 0;
    if ((payload.items || []).length) {
      renderFeaturePreview(payload.items[0]);
    } else {
      renderFeaturePreview(null);
    }
  } catch (error) {
    listEl.innerHTML = `<p class="empty-state">백엔드 연결 후 제품 지식 목록을 불러올 수 있습니다.</p>`;
    previewEl.innerHTML = `<p class="empty-state">저장된 제품을 선택하면 RAG·ML 변환 결과가 표시됩니다.</p>`;
  }
}

function renderKnowledgeList(items) {
  if (!items.length) {
    listEl.innerHTML = `<p class="empty-state">아직 등록된 제품 지식이 없습니다.</p>`;
    return;
  }

  listEl.innerHTML = items
    .map(
      (item) => `
        <button class="knowledge-row" type="button" data-product-id="${item.product_id}">
          <span>${item.status}</span>
          <strong>${item.product_name}</strong>
          <small>${item.company_name} · HS ${item.hs_code} · v${item.version}</small>
        </button>
      `
    )
    .join("");

  listEl.querySelectorAll(".knowledge-row").forEach((button) => {
    button.addEventListener("click", () => {
      const selected = items.find((item) => item.product_id === button.dataset.productId);
      renderFeaturePreview(selected);
    });
  });
}

function renderFeaturePreview(item) {
  if (!item) {
    previewEl.innerHTML = `<p class="empty-state">제품 지식을 저장하면 AI Agent/RAG/ML 사전 지식 변환 결과가 표시됩니다.</p>`;
    return;
  }

  const featureRow = item.feature_preview?.feature_row || {};
  const documents = item.rag_pack?.documents || [];
  const terms = item.rag_pack?.knowledge_terms || [];

  previewEl.innerHTML = `
    <article class="preview-head">
      <span>${item.product_id}</span>
      <h3>${item.product_name}</h3>
      <p>${item.company_name} · ${item.target_market}</p>
    </article>
    <div class="feature-grid">
      ${Object.entries(featureRow)
        .map(([key, value]) => `<div><span>${key}</span><strong>${value}</strong></div>`)
        .join("")}
    </div>
    <section class="rag-preview">
      <h3>RAG 문서</h3>
      ${
        documents.length
          ? documents
              .map(
                (doc) => `
                  <article>
                    <span>${doc.document_type} · ${doc.reliability}</span>
                    <strong>${doc.title}</strong>
                    <p>${doc.summary || doc.chunk_text}</p>
                  </article>
                `
              )
              .join("")
          : `<p class="empty-state">등록된 근거 문서가 없습니다.</p>`
      }
    </section>
    <section class="term-preview">
      <h3>지식 키워드</h3>
      <div>${terms.map((term) => `<span>${term}</span>`).join("") || "<p>키워드 없음</p>"}</div>
    </section>
  `;
}

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  setStatus("저장 중...");
  try {
    const response = await fetch(`${API_BASE}/product-knowledge`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(buildPayload()),
    });
    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      throw new Error(error.detail || `HTTP ${response.status}`);
    }
    const item = await response.json();
    setStatus(`저장 완료: ${item.product_name} v${item.version}`, "success");
    await loadKnowledge();
    renderFeaturePreview(item);
  } catch (error) {
    setStatus(`저장 실패: ${error.message}`, "error");
  }
});

sampleButton.addEventListener("click", () => {
  form.reset();
  form.elements.company_name.value = "경남 배터리테크";
  form.elements.product_name.value = "리튬이온 배터리팩";
  form.elements.hs_code.value = "850760";
  form.elements.target_market.value = "Germany / EU";
  form.elements.product_family.value = "industrial battery pack";
  form.elements.features.value = "battery, electronics, industrial";
  form.elements.certifications.value = "KC, UN38.3, IEC 62133";
  form.elements.materials.value = "NCM cell, BMS, aluminum case";
  form.elements.buyer_requirements.value = "EU battery passport, carbon footprint, MSDS";
  form.elements.recall_keywords.value = "thermal runaway, short circuit, overheating";
  form.elements.fto_keywords.value = "battery pack cooling, BMS balancing";
  form.elements.doc_title.value = "UN38.3 시험성적서";
  form.elements.doc_type.value = "test_report";
  form.elements.doc_reliability.value = "high";
  form.elements.doc_url.value = "https://example.test/un38";
  form.elements.doc_summary.value = "리튬이온 배터리팩 항공·해상 운송 안전 시험 근거";
  form.elements.owner.value = "품질·인증팀";
  form.elements.status.value = "active";
});

loadKnowledge();
