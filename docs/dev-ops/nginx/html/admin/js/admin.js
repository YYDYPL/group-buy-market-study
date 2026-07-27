document.addEventListener("DOMContentLoaded", () => {
    const token = sessionStorage.getItem("gbmAdminToken");
    if (!token) {
        location.replace("login.html");
        return;
    }

    const tbody = document.getElementById("productTableBody");
    const drawer = document.getElementById("configDrawer");
    const backdrop = document.getElementById("drawerBackdrop");
    const form = document.getElementById("configForm");
    const pageSize = 10;
    let currentPage = 1;
    let total = 0;
    let lastItems = [];
    let pendingConfirm = null;

    async function adminRequest(path, options) {
        const config = Object.assign({}, options || {});
        config.headers = Object.assign({}, config.headers || {}, {"X-Admin-Token": token});
        if (config.body && !(config.body instanceof FormData)) {
            config.headers["Content-Type"] = "application/json";
            config.body = JSON.stringify(config.body);
        }
        const response = await fetch(StoreApi.apiBaseUrl + path, config);
        let payload;
        try { payload = await response.json(); } catch (_) { throw new Error("后台服务响应格式错误"); }
        if (response.status === 401 || response.status === 503) {
            sessionStorage.removeItem("gbmAdminToken");
            location.replace("login.html");
            throw new Error(payload.info || "管理会话已失效");
        }
        if (!response.ok || payload.code !== "0000") {
            const error = new Error(payload.info || "请求失败");
            error.code = payload.code;
            throw error;
        }
        return payload.data;
    }

    async function loadProducts() {
        tbody.innerHTML = `<tr><td colspan="7" class="loading-cell">正在加载配置...</td></tr>`;
        const keyword = document.getElementById("keywordFilter").value.trim();
        const status = document.getElementById("statusFilter").value;
        const params = new URLSearchParams({page: String(currentPage), pageSize: String(pageSize)});
        if (keyword) params.set("keyword", keyword);
        if (status !== "") params.set("status", status);
        try {
            const page = await adminRequest(`/api/v1/gbm/admin/products?${params}`);
            lastItems = page.items || [];
            total = page.total || 0;
            renderTable(lastItems);
            renderPagination();
            await loadMetrics();
        } catch (error) {
            tbody.innerHTML = `<tr><td colspan="7" class="loading-cell error">${escapeHtml(error.message)}</td></tr>`;
            toast(error.message, "error");
        }
    }

    async function loadMetrics() {
        try {
            const all = await adminRequest("/api/v1/gbm/admin/products?page=1&pageSize=50");
            const items = all.items || [];
            document.getElementById("totalMetric").textContent = all.total || 0;
            document.getElementById("onlineMetric").textContent = items.filter(item => item.productStatus === 1).length;
            document.getElementById("draftMetric").textContent = items.filter(item => item.activityStatus === 0).length;
            document.getElementById("activityMetric").textContent = items.filter(item => item.activityId).length;
        } catch (_) {
            // 主列表已展示错误时不重复弹提示。
        }
    }

    function renderTable(items) {
        if (!items.length) {
            tbody.innerHTML = `<tr><td colspan="7" class="loading-cell">没有符合条件的商品配置</td></tr>`;
            return;
        }
        tbody.innerHTML = items.map(item => `
            <tr>
                <td>
                    <div class="table-product">
                        <img src="${StoreApi.assetUrl(item.mainImage, true)}" alt="">
                        <div><strong>${escapeHtml(item.goodsName)}</strong><small>${escapeHtml(item.goodsId)} · ${escapeHtml(item.category)}</small></div>
                    </div>
                </td>
                <td><div class="price-stack"><del>¥${StoreApi.money(item.originalPrice)}</del><strong>${item.payPrice == null ? "--" : "¥" + StoreApi.money(item.payPrice)}</strong></div></td>
                <td><div class="rule-cell"><strong>${escapeHtml(item.marketPlan || "--")} · ${escapeHtml(item.marketExpr || "--")}</strong><small>${item.target || "--"}人成团 / ${item.validTime || "--"}分钟</small></div></td>
                <td>${productBadge(item.productStatus)}</td>
                <td>${activityBadge(item.activityStatus)}</td>
                <td><code>v${item.version || 0}</code></td>
                <td>
                    <div class="row-actions">
                        <button data-action="edit" data-id="${escapeHtml(item.goodsId)}">编辑</button>
                        ${item.activityStatus === 0 ? `<button class="publish" data-action="publish" data-id="${escapeHtml(item.goodsId)}" data-version="${item.version}">发布</button>` : ""}
                        ${item.activityStatus === 0 ? `<button class="danger" data-action="abandon" data-id="${escapeHtml(item.goodsId)}" data-version="${item.version}">废弃</button>` : ""}
                        ${item.productStatus === 1 ? `<button class="danger" data-action="offline" data-id="${escapeHtml(item.goodsId)}" data-version="${item.version}">下架</button>` : ""}
                    </div>
                </td>
            </tr>
        `).join("");
    }

    function productBadge(status) {
        const map = {0:["草稿","draft"],1:["已上架","online"],2:["已下架","offline"]};
        const value = map[status] || ["未知","offline"];
        return `<span class="status-badge ${value[1]}">${value[0]}</span>`;
    }

    function activityBadge(status) {
        const map = {0:["待发布","draft"],1:["生效中","online"],2:["已下架","offline"],3:["已废弃","abandoned"]};
        const value = map[status] || ["未配置","offline"];
        return `<span class="status-badge ${value[1]}">${value[0]}</span>`;
    }

    function renderPagination() {
        const pages = Math.max(1, Math.ceil(total / pageSize));
        document.getElementById("pageSummary").textContent = `第 ${currentPage} / ${pages} 页，共 ${total} 条`;
        document.getElementById("prevPage").disabled = currentPage <= 1;
        document.getElementById("nextPage").disabled = currentPage >= pages;
    }

    function openDrawer(item) {
        form.reset();
        setDefaultDates();
        document.getElementById("version").value = item ? item.version || 0 : 0;
        document.getElementById("drawerKicker").textContent = item ? `编辑 ${item.goodsId}` : "新建商品";
        document.getElementById("drawerTitle").textContent = item ? item.goodsName : "商品与活动配置";
        form.elements.goodsId.readOnly = Boolean(item);
        if (item) fillForm(item);
        else {
            form.elements.source.value = "s01";
            form.elements.channel.value = "c01";
            form.elements.favorableRate.value = "99";
            form.elements.target.value = "2";
            form.elements.takeLimitCount.value = "3";
            form.elements.validTime.value = "30";
        }
        updateExprHint();
        updateImagePreview();
        resetTrial();
        drawer.classList.add("open");
        backdrop.classList.add("open");
        drawer.setAttribute("aria-hidden", "false");
    }

    function closeDrawer() {
        drawer.classList.remove("open");
        backdrop.classList.remove("open");
        drawer.setAttribute("aria-hidden", "true");
    }

    async function editProduct(goodsId) {
        try {
            const item = await adminRequest(`/api/v1/gbm/admin/products/${encodeURIComponent(goodsId)}`);
            openDrawer(item);
        } catch (error) {
            toast(error.message, "error");
        }
    }

    function fillForm(item) {
        const values = {
            goodsId:item.goodsId, goodsName:item.goodsName, originalPrice:item.originalPrice,
            category:item.category, subtitle:item.subtitle, mainImage:item.mainImage,
            salesCount:item.salesCount, favorableRate:item.favorableRate, sortOrder:item.sortOrder,
            serviceTagsText:(item.serviceTags || []).join(","),
            galleryImagesText:(item.galleryImages || []).join("\n"),
            activityName:item.activityName, target:item.target, takeLimitCount:item.takeLimitCount,
            validTime:item.validTime, source:item.source, channel:item.channel,
            discountName:item.discountName, discountType:item.discountType,
            discountDesc:item.discountDesc, marketPlan:item.marketPlan, marketExpr:item.marketExpr,
            startTime:toLocalDate(item.startTime), endTime:toLocalDate(item.endTime)
        };
        Object.entries(values).forEach(([name,value]) => {
            if (form.elements[name] && value != null) form.elements[name].value = value;
        });
    }

    function formPayload() {
        const data = new FormData(form);
        const lines = value => String(value || "").split(/\r?\n|,/).map(item => item.trim()).filter(Boolean);
        return {
            goodsId:data.get("goodsId").trim(),
            goodsName:data.get("goodsName").trim(),
            originalPrice:Number(data.get("originalPrice")),
            category:data.get("category"),
            subtitle:data.get("subtitle").trim(),
            mainImage:data.get("mainImage").trim(),
            galleryImages:lines(data.get("galleryImagesText")),
            salesCount:Number(data.get("salesCount") || 0),
            favorableRate:Number(data.get("favorableRate") || 0),
            serviceTags:lines(data.get("serviceTagsText")),
            sortOrder:Number(data.get("sortOrder") || 0),
            version:Number(data.get("version") || 0),
            activityName:data.get("activityName").trim(),
            groupType:0,
            takeLimitCount:Number(data.get("takeLimitCount")),
            target:Number(data.get("target")),
            validTime:Number(data.get("validTime")),
            startTime:data.get("startTime"),
            endTime:data.get("endTime"),
            discountName:data.get("discountName").trim(),
            discountDesc:data.get("discountDesc").trim(),
            discountType:Number(data.get("discountType")),
            marketPlan:data.get("marketPlan"),
            marketExpr:data.get("marketExpr").trim(),
            source:data.get("source").trim(),
            channel:data.get("channel").trim()
        };
    }

    async function trial() {
        try {
            const result = await adminRequest("/api/v1/gbm/admin/products/trial", {method:"POST", body:formPayload()});
            document.getElementById("trialOriginal").textContent = `¥${StoreApi.money(result.originalPrice)}`;
            document.getElementById("trialDeduction").textContent = `¥${StoreApi.money(result.deductionPrice)}`;
            document.getElementById("trialPay").textContent = `¥${StoreApi.money(result.payPrice)}`;
            document.getElementById("trialExplanation").textContent = result.explanation;
            document.getElementById("trialCard").classList.add("valid");
            return result;
        } catch (error) {
            resetTrial();
            document.getElementById("trialExplanation").textContent = error.message;
            toast(error.message, "error");
            throw error;
        }
    }

    async function saveDraft(event) {
        event.preventDefault();
        const button = document.getElementById("saveDraftButton");
        button.disabled = true;
        button.textContent = "保存中...";
        try {
            await trial();
            const saved = await adminRequest("/api/v1/gbm/admin/products/draft", {method:"POST", body:formPayload()});
            toast(`商品 ${saved.goodsId} 草稿保存成功`, "success");
            closeDrawer();
            loadProducts();
        } catch (_) {
            // trial/save 已展示具体错误。
        } finally {
            button.disabled = false;
            button.textContent = "保存为草稿";
        }
    }

    async function uploadImage(file) {
        if (!file) return;
        const formData = new FormData();
        formData.append("file", file);
        try {
            const result = await adminRequest("/api/v1/gbm/admin/images", {method:"POST", body:formData});
            form.elements.mainImage.value = result.url;
            updateImagePreview();
            toast("图片上传成功", "success");
        } catch (error) {
            toast(error.message, "error");
        }
    }

    function updateExprHint() {
        const hints = {
            ZJ:["例如 20","填写直减金额，例如 20"],
            MJ:["例如 59,20","按“门槛,减免额”填写，例如 59,20"],
            N:["例如 19.90","填写拼团固定成交价，例如 19.90"],
            ZK:["例如 0.69","填写 0 到 1 之间的折扣系数，例如 0.69"]
        };
        const hint = hints[form.elements.marketPlan.value];
        document.getElementById("marketExpr").placeholder = hint[0];
        document.getElementById("exprHint").textContent = hint[1];
    }

    function updateImagePreview() {
        const image = document.getElementById("imagePreview");
        const value = form.elements.mainImage.value.trim();
        image.src = value ? StoreApi.assetUrl(value, true) : "";
        image.classList.toggle("visible", Boolean(value));
    }

    function resetTrial() {
        document.getElementById("trialOriginal").textContent = "¥--";
        document.getElementById("trialDeduction").textContent = "¥--";
        document.getElementById("trialPay").textContent = "¥--";
        document.getElementById("trialExplanation").textContent = "填写价格和优惠表达式后点击试算";
        document.getElementById("trialCard").classList.remove("valid");
    }

    function confirmAction(title, message, action) {
        document.getElementById("confirmTitle").textContent = title;
        document.getElementById("confirmMessage").textContent = message;
        pendingConfirm = action;
        document.getElementById("confirmModal").classList.add("open");
    }

    async function changeStatus(action, goodsId, version) {
        try {
            await adminRequest(`/api/v1/gbm/admin/products/${encodeURIComponent(goodsId)}/${action}?version=${version}`, {method:"POST"});
            const messages = {
                publish: "发布成功，商城已可见",
                offline: "商品已下架",
                abandon: "草稿已废弃"
            };
            toast(messages[action] || "操作成功", "success");
            loadProducts();
        } catch (error) {
            toast(error.message, "error");
        }
    }

    function setDefaultDates() {
        const start = new Date();
        const end = new Date();
        end.setFullYear(end.getFullYear() + 1);
        form.elements.startTime.value = toInputDate(start);
        form.elements.endTime.value = toInputDate(end);
    }

    function toInputDate(date) {
        const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
        return local.toISOString().slice(0,16);
    }

    function toLocalDate(value) {
        if (!value) return "";
        return value.replace(" ", "T").slice(0,16);
    }

    function toast(message, type) {
        const box = document.getElementById("toast");
        box.textContent = message;
        box.className = `admin-toast show ${type || ""}`;
        clearTimeout(toast.timer);
        toast.timer = setTimeout(() => box.classList.remove("show"), 2600);
    }

    function escapeHtml(value) { return StoreApi.escapeHtml(value); }

    document.getElementById("newProductButton").addEventListener("click", () => openDrawer(null));
    document.getElementById("closeDrawer").addEventListener("click", closeDrawer);
    document.getElementById("cancelEdit").addEventListener("click", closeDrawer);
    backdrop.addEventListener("click", closeDrawer);
    form.addEventListener("submit", saveDraft);
    document.getElementById("trialButton").addEventListener("click", () => trial().catch(() => {}));
    document.getElementById("marketPlan").addEventListener("change", updateExprHint);
    form.elements.mainImage.addEventListener("input", updateImagePreview);
    document.getElementById("imageUpload").addEventListener("change", event => uploadImage(event.target.files[0]));
    document.getElementById("searchButton").addEventListener("click", () => { currentPage=1; loadProducts(); });
    document.getElementById("refreshAdminButton").addEventListener("click", loadProducts);
    document.getElementById("keywordFilter").addEventListener("keydown", event => { if (event.key === "Enter") { currentPage=1; loadProducts(); }});
    document.getElementById("prevPage").addEventListener("click", () => { if(currentPage>1){currentPage--;loadProducts();}});
    document.getElementById("nextPage").addEventListener("click", () => { if(currentPage*pageSize<total){currentPage++;loadProducts();}});
    document.getElementById("logoutButton").addEventListener("click", () => { sessionStorage.removeItem("gbmAdminToken"); location.replace("login.html"); });
    tbody.addEventListener("click", event => {
        const button = event.target.closest("button[data-action]");
        if (!button) return;
        const {action,id,version} = button.dataset;
        if (action === "edit") editProduct(id);
        if (action === "publish") confirmAction("发布活动配置", "发布后商城将立即切换到这份价格与拼团规则，确认继续吗？", () => changeStatus("publish", id, version));
        if (action === "offline") confirmAction("下架商品", "下架后商城不再接受新开团和参团，已锁定订单仍可结算或退款。", () => changeStatus("offline", id, version));
        if (action === "abandon") confirmAction("废弃活动草稿", "废弃后这份草稿不能再发布，但不会影响当前已经生效的活动。", () => changeStatus("abandon", id, version));
    });
    document.getElementById("confirmCancel").addEventListener("click", () => document.getElementById("confirmModal").classList.remove("open"));
    document.getElementById("confirmOk").addEventListener("click", async () => {
        document.getElementById("confirmModal").classList.remove("open");
        if (pendingConfirm) await pendingConfirm();
        pendingConfirm = null;
    });

    loadProducts();
});
