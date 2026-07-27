document.addEventListener("DOMContentLoaded", () => {
    const identity = StoreIdentity.current();
    const identityLink = document.getElementById("identityLink");
    identityLink.textContent = identity ? identity.nickname : "选择用户";
    identityLink.href = StoreIdentity.loginUrl("index.html");
    const productGrid = document.getElementById("productGrid");
    const emptyState = document.getElementById("emptyState");
    const resultSummary = document.getElementById("resultSummary");
    const searchForm = document.getElementById("searchForm");
    const searchInput = document.getElementById("searchInput");
    const categoryNav = document.getElementById("categoryNav");
    const sortNav = document.getElementById("sortNav");
    let activeCategory = "全部";
    let activeSort = "default";
    let keyword = "";

    async function loadProducts() {
        renderSkeleton();
        emptyState.hidden = true;
        const params = new URLSearchParams({page: "1", pageSize: "24"});
        if (keyword) params.set("keyword", keyword);
        if (activeCategory !== "全部") params.set("category", activeCategory);
        if (activeSort !== "default") params.set("sort", activeSort);
        try {
            const page = await StoreApi.get(`/api/v1/gbm/store/products?${params}`);
            renderProducts(page.items || []);
            resultSummary.textContent = keyword || activeCategory !== "全部"
                ? `为你找到 ${page.total || 0} 件好货`
                : `已有 ${page.total || 0} 件商品参与今日拼团`;
        } catch (error) {
            productGrid.innerHTML = "";
            emptyState.hidden = false;
            emptyState.querySelector("h3").textContent = "商城服务暂时不可用";
            emptyState.querySelector("p").textContent = error.message;
            resultSummary.textContent = "加载失败，请稍后重试";
            StoreApi.showToast(error.message, "error");
        }
    }

    function renderProducts(products) {
        if (!products.length) {
            productGrid.innerHTML = "";
            emptyState.hidden = false;
            return;
        }
        emptyState.hidden = true;
        productGrid.innerHTML = products.map(product => `
            <a class="product-card" href="product-detail.html?goodsId=${encodeURIComponent(product.goodsId)}${StoreApi.apiPort === "8091" ? "" : `&apiPort=${StoreApi.apiPort}`}">
                <div class="product-image-wrap">
                    <img src="${StoreApi.assetUrl(product.mainImage)}" alt="${StoreApi.escapeHtml(product.goodsName)}" loading="lazy">
                    <span class="image-badge">${product.target || 2}人团</span>
                </div>
                <div class="product-card-body">
                    <h3><span>拼团</span>${StoreApi.escapeHtml(product.goodsName)}</h3>
                    <p>${StoreApi.escapeHtml(product.subtitle)}</p>
                    <div class="card-tags">${(product.serviceTags || []).slice(0, 2).map(tag => `<em>${StoreApi.escapeHtml(tag)}</em>`).join("")}</div>
                    <div class="card-price">
                        <div><small>¥</small><strong>${StoreApi.money(product.payPrice)}</strong><del>¥${StoreApi.money(product.originalPrice)}</del></div>
                        <span>已拼${StoreApi.sales(product.salesCount)}件</span>
                    </div>
                </div>
            </a>
        `).join("");
    }

    function renderSkeleton() {
        productGrid.innerHTML = Array.from({length: 6}, () => `<article class="product-card skeleton"></article>`).join("");
    }

    searchForm.addEventListener("submit", event => {
        event.preventDefault();
        keyword = searchInput.value.trim();
        loadProducts();
    });

    categoryNav.addEventListener("click", event => {
        const button = event.target.closest("button[data-category]");
        if (!button) return;
        categoryNav.querySelectorAll("button").forEach(item => item.classList.remove("active"));
        button.classList.add("active");
        activeCategory = button.dataset.category;
        loadProducts();
    });

    sortNav.addEventListener("click", event => {
        const button = event.target.closest("button[data-sort]");
        if (!button) return;
        sortNav.querySelectorAll("button").forEach(item => item.classList.remove("active"));
        button.classList.add("active");
        activeSort = button.dataset.sort;
        loadProducts();
    });

    document.getElementById("refreshButton").addEventListener("click", () => {
        keyword = "";
        activeCategory = "全部";
        activeSort = "default";
        searchInput.value = "";
        categoryNav.querySelectorAll("button").forEach((item, index) => item.classList.toggle("active", index === 0));
        sortNav.querySelectorAll("button").forEach((item, index) => item.classList.toggle("active", index === 0));
        loadProducts();
    });

    document.getElementById("heroAction").addEventListener("click", () => {
        document.querySelector(".section-heading").scrollIntoView({behavior: "smooth"});
    });

    emptyState.querySelector("button").addEventListener("click", () => {
        keyword = "";
        activeCategory = "全部";
        activeSort = "default";
        searchInput.value = "";
        sortNav.querySelectorAll("button").forEach((item, index) => item.classList.toggle("active", index === 0));
        loadProducts();
    });

    loadProducts();
});
