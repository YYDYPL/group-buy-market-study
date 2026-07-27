document.addEventListener("DOMContentLoaded", () => {
    const identity = StoreIdentity.current();
    if (!identity) {
        location.href = StoreIdentity.loginUrl("orders.html");
        return;
    }

    const orderList = document.getElementById("orderList");
    const empty = document.getElementById("emptyOrders");
    const loadMoreButton = document.getElementById("loadMore");
    const paymentModal = document.getElementById("paymentModal");
    const teamModal = document.getElementById("teamModal");
    let activeStatus = "";
    let page = 1;
    let total = 0;
    let orders = [];
    let paymentOrder;

    renderIdentity();
    loadOrders(true);

    function renderIdentity() {
        document.getElementById("userAvatar").textContent = identity.nickname.slice(0, 1).toUpperCase();
        document.getElementById("userName").textContent = identity.nickname;
        document.getElementById("userId").textContent = identity.userId;
        const switchUrl = StoreIdentity.loginUrl("orders.html");
        document.getElementById("switchUser").href = switchUrl;
        document.getElementById("changeUser").href = switchUrl;
    }

    async function loadOrders(reset) {
        if (reset) {
            page = 1;
            orders = [];
            orderList.innerHTML = '<div class="order-skeleton"></div><div class="order-skeleton"></div>';
        }
        try {
            const statusQuery = activeStatus === "" ? "" : `&status=${activeStatus}`;
            const result = await StoreApi.get(
                `/api/v1/gbm/store/users/${encodeURIComponent(identity.userId)}/orders?page=${page}&pageSize=20${statusQuery}`);
            total = result.total || 0;
            orders = orders.concat(result.items || []);
            renderOrders();
        } catch (error) {
            orderList.innerHTML = "";
            empty.hidden = false;
            empty.querySelector("b").textContent = "订单加载失败";
            empty.querySelector("span").textContent = error.message;
            StoreApi.showToast(error.message, "error");
        }
    }

    function renderOrders() {
        empty.hidden = orders.length > 0;
        loadMoreButton.hidden = orders.length >= total;
        if (!orders.length) {
            orderList.innerHTML = "";
            return;
        }
        orderList.innerHTML = orders.map((order, index) => {
            const statusClass = order.orderStatus === 1 ? "paid" : order.orderStatus === 2 ? "closed" : "";
            return `
                <article class="order-card">
                    <div class="order-card-head">
                        <code>${StoreApi.escapeHtml(order.outTradeNo)}</code>
                        <span class="order-status ${statusClass}">${StoreApi.escapeHtml(order.orderStatusText)}</span>
                    </div>
                    <a class="order-product" href="product-detail.html?goodsId=${encodeURIComponent(order.goodsId)}&teamId=${encodeURIComponent(order.teamId)}">
                        <img src="${StoreApi.assetUrl(order.mainImage)}" alt="${StoreApi.escapeHtml(order.goodsName || order.goodsId)}">
                        <div class="product-copy">
                            <strong>${StoreApi.escapeHtml(order.goodsName || `商品 ${order.goodsId}`)}</strong>
                            <span>订单 ${StoreApi.escapeHtml(order.orderId)}</span>
                        </div>
                        <div class="order-price">¥${StoreApi.money(order.payPrice)}</div>
                    </a>
                    <div class="team-progress">
                        <p><strong>${StoreApi.escapeHtml(order.teamStatusText)}</strong>
                        ${order.lockCount || 0}/${order.targetCount || 0} 人锁单 · ${order.completeCount || 0} 人已支付</p>
                        <button type="button" data-team="${StoreApi.escapeHtml(order.teamId)}">成员详情</button>
                    </div>
                    <div class="order-actions">
                        ${order.canCancel ? `<button type="button" data-action="cancel" data-index="${index}">取消锁单</button>` : ""}
                        ${order.canRefund ? `<button class="refund" type="button" data-action="refund" data-index="${index}">申请退款</button>` : ""}
                        ${order.canPay ? `<button class="primary" type="button" data-action="pay" data-index="${index}">立即支付</button>` : ""}
                        ${!order.canCancel && !order.canRefund && !order.canPay ? `<button type="button" data-team="${StoreApi.escapeHtml(order.teamId)}">查看团队</button>` : ""}
                    </div>
                </article>`;
        }).join("");
    }

    function openPayment(order) {
        paymentOrder = order;
        document.getElementById("paymentAmount").textContent = `¥${StoreApi.money(order.payPrice)}`;
        document.getElementById("paymentGoods").textContent = order.goodsName || order.goodsId;
        document.getElementById("paymentTradeNo").textContent = order.outTradeNo;
        openModal(paymentModal);
    }

    async function settle() {
        if (!paymentOrder) return;
        const button = document.getElementById("confirmPayment");
        button.disabled = true;
        button.textContent = "结算中...";
        try {
            await StoreApi.post("/api/v1/gbm/trade/settlement_market_pay_order", {
                source: paymentOrder.source || "s01",
                channel: paymentOrder.channel || "c01",
                userId: identity.userId,
                outTradeNo: paymentOrder.outTradeNo,
                outTradeTime: new Date()
            });
            closeModal(paymentModal);
            StoreApi.showToast("支付结算成功", "success");
            await delay(500);
            await loadOrders(true);
        } catch (error) {
            StoreApi.showToast(error.message, "error");
        } finally {
            button.disabled = false;
            button.textContent = "我已完成支付";
        }
    }

    async function refund(order, action) {
        const operation = action === "cancel" ? "取消待支付锁单并释放团队名额" : "申请退款";
        if (!confirm(`确认${operation}？\n交易单号：${order.outTradeNo}`)) return;
        try {
            const result = await StoreApi.post("/api/v1/gbm/trade/refund_market_pay_order", {
                userId: identity.userId,
                outTradeNo: order.outTradeNo,
                source: order.source || "s01",
                channel: order.channel || "c01"
            });
            StoreApi.showToast(result.info || "操作成功", "success");
            await delay(400);
            await loadOrders(true);
        } catch (error) {
            StoreApi.showToast(error.message, "error");
        }
    }

    async function showTeam(teamId) {
        try {
            const team = await StoreApi.get(`/api/v1/gbm/store/teams/${encodeURIComponent(teamId)}`);
            const members = (team.members || []).map(member => {
                const mine = member.userId === identity.userId;
                const statusClass = member.orderStatus === 1 ? "paid" : "";
                return `
                    <div class="member-item">
                        <b>${StoreApi.escapeHtml(member.userId.slice(0, 1).toUpperCase())}</b>
                        <div><strong>${mine ? "我 · " + StoreApi.escapeHtml(identity.nickname) : maskUser(member.userId)}</strong>
                        <small>${StoreApi.escapeHtml(member.outTradeNo)}</small></div>
                        <em class="${statusClass}">${StoreApi.escapeHtml(member.orderStatusText)}</em>
                    </div>`;
            }).join("");
            document.getElementById("teamDetail").innerHTML = `
                <div class="team-summary"><strong>${StoreApi.escapeHtml(team.statusText)} · ${team.completeCount || 0}/${team.targetCount || 0} 人已支付</strong>
                <span>${team.lockCount || 0} 人锁单，团队编号 ${StoreApi.escapeHtml(team.teamId)}</span></div>
                <div class="member-list">${members || "<p>暂无团队成员</p>"}</div>
                <button class="team-copy" type="button" data-copy="${StoreApi.escapeHtml(team.teamId)}">复制邀请链接</button>`;
            openModal(teamModal);
        } catch (error) {
            StoreApi.showToast(error.message, "error");
        }
    }

    async function copyInvite(teamId) {
        const order = orders.find(item => item.teamId === teamId);
        if (!order) return;
        const url = new URL("product-detail.html", location.href);
        url.search = new URLSearchParams({goodsId: order.goodsId, teamId}).toString();
        try {
            if (navigator.clipboard && window.isSecureContext) {
                await navigator.clipboard.writeText(url.toString());
            } else {
                const area = document.createElement("textarea");
                area.value = url.toString();
                area.style.position = "fixed";
                area.style.opacity = "0";
                document.body.appendChild(area);
                area.select();
                document.execCommand("copy");
                area.remove();
            }
            StoreApi.showToast("邀请链接已复制", "success");
        } catch (error) {
            StoreApi.showToast("复制失败", "error");
        }
    }

    function maskUser(value) {
        const text = String(value || "拼友");
        return StoreApi.escapeHtml(text.length <= 2 ? `${text[0]}*` : `${text[0]}***${text.slice(-1)}`);
    }

    function openModal(modal) {
        modal.classList.add("open");
        modal.setAttribute("aria-hidden", "false");
    }

    function closeModal(modal) {
        modal.classList.remove("open");
        modal.setAttribute("aria-hidden", "true");
    }

    function delay(milliseconds) {
        return new Promise(resolve => setTimeout(resolve, milliseconds));
    }

    document.getElementById("orderTabs").addEventListener("click", event => {
        const button = event.target.closest("button[data-status]");
        if (!button) return;
        activeStatus = button.dataset.status;
        document.querySelectorAll("#orderTabs button").forEach(item => item.classList.toggle("active", item === button));
        loadOrders(true);
    });

    orderList.addEventListener("click", event => {
        const teamButton = event.target.closest("[data-team]");
        if (teamButton) {
            showTeam(teamButton.dataset.team);
            return;
        }
        const actionButton = event.target.closest("[data-action]");
        if (!actionButton) return;
        const order = orders[Number(actionButton.dataset.index)];
        if (!order) return;
        if (actionButton.dataset.action === "pay") openPayment(order);
        else refund(order, actionButton.dataset.action);
    });

    loadMoreButton.addEventListener("click", () => {
        page += 1;
        loadOrders(false);
    });
    document.getElementById("confirmPayment").addEventListener("click", settle);
    document.querySelectorAll("[data-close-modal]").forEach(button =>
        button.addEventListener("click", () => closeModal(paymentModal)));
    document.querySelectorAll("[data-close-team]").forEach(button =>
        button.addEventListener("click", () => closeModal(teamModal)));
    document.getElementById("teamDetail").addEventListener("click", event => {
        const button = event.target.closest("[data-copy]");
        if (button) copyInvite(button.dataset.copy);
    });
    [paymentModal, teamModal].forEach(modal => modal.addEventListener("click", event => {
        if (event.target === modal) closeModal(modal);
    }));
});
