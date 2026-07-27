document.addEventListener("DOMContentLoaded", () => {
    const params = new URLSearchParams(location.search);
    const goodsId = params.get("goodsId");
    const invitedTeamId = params.get("teamId") || "";
    const identity = StoreIdentity.current();
    const userId = identity ? identity.userId : "";
    const previewUserId = userId || "guest_preview";
    const groupList = document.getElementById("groupList");
    const startGroup = document.getElementById("startGroup");
    const buyAlone = document.getElementById("buyAlone");
    const paymentModal = document.getElementById("paymentModal");
    const lockModal = document.getElementById("lockResultModal");
    const teamModal = document.getElementById("teamModal");
    let product;
    let market;
    let currentOrder;
    let invitedTeam;
    let paymentOrder;
    let lockedOrder;
    let groupCarouselTimer;
    let countdownTimer;
    let groupCarouselIndex = 0;

    renderIdentity();

    async function loadDetail() {
        if (!goodsId) {
            renderFatal("链接中缺少商品ID，请从商城首页重新选择商品");
            return;
        }
        try {
            product = await StoreApi.get(`/api/v1/gbm/store/products/${encodeURIComponent(goodsId)}`);
            renderProduct(product);
        } catch (error) {
            renderFatal(error.message);
            return;
        }

        try {
            await refreshBusinessState();
            startGroup.disabled = false;
        } catch (error) {
            startGroup.disabled = true;
            groupList.innerHTML = emptyMessage("!", "拼团服务暂不可用", error.message);
            StoreApi.showToast(error.message, "error");
        }
    }

    async function refreshBusinessState() {
        const tasks = [loadMarket()];
        if (userId) tasks.push(loadUserOrders());
        if (invitedTeamId) tasks.push(loadInvitedTeam());
        await Promise.all(tasks);
    }

    async function loadMarket(retried) {
        try {
            market = await StoreApi.post("/api/v1/gbm/index/query_group_buy_market_config", {
                userId: previewUserId,
                source: product.source || "s01",
                channel: product.channel || "c01",
                goodsId
            });
            renderMarket(market);
        } catch (error) {
            if (!retried && error.code === "0006") {
                await delay(1100);
                return loadMarket(true);
            }
            throw error;
        }
    }

    async function loadUserOrders() {
        const result = await StoreApi.get(
            `/api/v1/gbm/store/users/${encodeURIComponent(userId)}/orders`
            + `?goodsId=${encodeURIComponent(goodsId)}&page=1&pageSize=50`);
        const items = result.items || [];
        currentOrder = invitedTeamId
            ? items.find(order => order.teamId === invitedTeamId && order.orderStatus !== 2)
            : items.find(order => order.orderStatus === 0)
                || items.find(order => order.orderStatus === 1)
                || items[0];
        renderCurrentOrder(currentOrder);
        return items;
    }

    async function loadInvitedTeam() {
        try {
            invitedTeam = await StoreApi.get(
                `/api/v1/gbm/store/teams/${encodeURIComponent(invitedTeamId)}`);
            if (market && invitedTeam.activityId !== market.activityId) {
                throw new Error("邀请团队与当前商品活动不匹配");
            }
            renderInvitedTeam(invitedTeam);
        } catch (error) {
            const section = document.getElementById("inviteTeamSection");
            section.hidden = false;
            document.getElementById("inviteTeamStatus").textContent = "不可加入";
            document.getElementById("inviteTeamCard").innerHTML =
                `<div class="invite-team-box unavailable"><strong>邀请团队不可用</strong><code>${StoreApi.escapeHtml(invitedTeamId)}</code><p>${StoreApi.escapeHtml(error.message)}</p></div>`;
        }
    }

    function renderIdentity() {
        const returnUrl = currentRelativeUrl();
        const actionUrl = StoreIdentity.loginUrl(returnUrl);
        document.getElementById("identitySwitch").href = actionUrl;
        document.getElementById("identityAction").href = actionUrl;
        if (!identity) return;
        document.getElementById("identityAvatar").textContent = identity.nickname.slice(0, 1).toUpperCase();
        document.getElementById("identityName").textContent = `${identity.nickname}（${identity.userId}）`;
    }

    function renderProduct(item) {
        document.title = `${item.goodsName} - HJS 拼团`;
        document.getElementById("goodsName").textContent = item.goodsName;
        document.getElementById("detailTitle").textContent = item.goodsName;
        document.getElementById("subtitle").textContent = item.subtitle || "精选拼团好货";
        document.getElementById("payPrice").textContent = StoreApi.money(item.payPrice);
        document.getElementById("originalPrice").textContent = `¥${StoreApi.money(item.originalPrice)}`;
        document.getElementById("salesCount").textContent = StoreApi.sales(item.salesCount);
        document.getElementById("activityName").textContent = item.activityName || "限时拼团";
        document.getElementById("priceExplanation").textContent = item.priceExplanation || "参与拼团享优惠";
        document.getElementById("activityRule").textContent =
            `${item.target || 2}人成团 · 每人最多参与${item.takeLimitCount || 1}次 · 队伍有效${item.validTime || 15}分钟`;
        document.getElementById("serviceTags").innerHTML = (item.serviceTags || [])
            .map(tag => `<span>✓ ${StoreApi.escapeHtml(tag)}</span>`).join("");
        buyAlone.querySelector("span").textContent = `¥${StoreApi.money(item.originalPrice)}`;
        startGroup.querySelector("span").textContent = `¥${StoreApi.money(item.payPrice)}`;

        const images = Array.from(new Set([item.mainImage].concat(item.galleryImages || []).filter(Boolean)));
        const galleryTrack = document.getElementById("galleryTrack");
        galleryTrack.innerHTML = images.map((image, index) => `
            <div class="gallery-slide">
                <img src="${StoreApi.assetUrl(image)}" alt="${StoreApi.escapeHtml(item.goodsName)} 商品图 ${index + 1}">
            </div>`).join("");
        document.getElementById("galleryCounter").textContent = `1/${Math.max(images.length, 1)}`;
        document.getElementById("galleryDots").innerHTML =
            images.map((_, index) => `<i class="${index === 0 ? "active" : ""}"></i>`).join("");
        galleryTrack.addEventListener("scroll", () => {
            const index = Math.round(galleryTrack.scrollLeft / Math.max(galleryTrack.clientWidth, 1));
            document.getElementById("galleryCounter").textContent = `${index + 1}/${images.length}`;
            document.querySelectorAll(".gallery-dots i")
                .forEach((dot, dotIndex) => dot.classList.toggle("active", dotIndex === index));
        }, {passive: true});
    }

    function renderMarket(data) {
        stopMarketTimers();
        const stats = data.teamStatistic || {};
        document.getElementById("teamStatistic").innerHTML = `
            <div><strong>${Number(stats.allTeamCount || 0)}</strong><span>累计开团</span></div>
            <div><strong>${Number(stats.allTeamCompleteCount || 0)}</strong><span>历史成团</span></div>
            <div><strong>${Number(stats.allTeamUserCount || 0)}</strong><span>累计参团</span></div>`;
        document.getElementById("payPrice").textContent = StoreApi.money(data.goods.payPrice);
        startGroup.querySelector("span").textContent = `¥${StoreApi.money(data.goods.payPrice)}`;
        const teams = randomizeTeams(uniqueTeams(data.teamList || []));
        if (!teams.length) {
            groupList.classList.add("is-one-team");
            groupList.innerHTML = emptyMessage(
                "拼", "暂时没有可加入的拼单", "现在开团，邀请好友一起享优惠",
                '<button type="button" data-start-group>去开团</button>');
            return;
        }
        const pages = [];
        for (let index = 0; index < teams.length; index += 2) {
            const page = teams.slice(index, index + 2);
            if (page.length === 1 && teams.length > 1) page.push(teams[0]);
            pages.push(page);
        }
        if (teams.length === 2) pages.push([teams[1], teams[0]]);
        groupList.innerHTML = `<div class="group-carousel-track">${pages.map(page => `
            <div class="group-carousel-page">
                ${page.map(renderGroupItem).join("")}
            </div>`).join("")}
        </div>`;
        groupList.classList.toggle("is-one-team", teams.length === 1);
        startCountdowns();
        startGroupCarousel(pages.length);
    }

    function renderGroupItem(team) {
        const remaining = Math.max((team.targetCount || 0) - (team.lockCount || 0), 0);
        const nickname = teamNickname(team.userId);
        return `
            <div class="group-item">
                <div class="avatar">${StoreApi.escapeHtml(nickname.slice(0, 1).toUpperCase() || "拼")}</div>
                <div class="group-user">
                    <strong>${StoreApi.escapeHtml(nickname)}</strong>
                    <p>还差 <em>${remaining}</em> 人锁满 · 已支付 ${team.completeCount || 0} 人 ·
                    <span class="countdown" data-seconds="${parseCountdown(team.validTimeCountdown)}">${StoreApi.escapeHtml(team.validTimeCountdown || "--:--:--")}</span></p>
                </div>
                <button type="button" data-team-id="${StoreApi.escapeHtml(team.teamId)}">去拼单</button>
            </div>`;
    }

    function uniqueTeams(teams) {
        const seen = new Set();
        return teams.filter(team => {
            if (!team || !team.teamId || seen.has(team.teamId)) return false;
            seen.add(team.teamId);
            return true;
        });
    }

    function randomizeTeams(teams) {
        const result = teams.slice();
        for (let index = result.length - 1; index > 0; index -= 1) {
            const target = Math.floor(Math.random() * (index + 1));
            [result[index], result[target]] = [result[target], result[index]];
        }
        return result;
    }

    function teamNickname(teamUserId) {
        const localUser = StoreIdentity.list().find(user => user.userId === teamUserId);
        return localUser ? localUser.nickname : `拼友 ${maskUserValue(teamUserId)}`;
    }

    function startGroupCarousel(pageCount) {
        groupCarouselIndex = 0;
        if (pageCount < 2) return;
        groupCarouselTimer = setInterval(() => {
            groupCarouselIndex = (groupCarouselIndex + 1) % pageCount;
            const track = groupList.querySelector(".group-carousel-track");
            if (track) track.style.transform = `translateY(-${groupCarouselIndex * 100}%)`;
        }, 4000);
    }

    function stopMarketTimers() {
        clearInterval(groupCarouselTimer);
        clearInterval(countdownTimer);
        groupCarouselTimer = null;
        countdownTimer = null;
        groupCarouselIndex = 0;
        groupList.classList.remove("is-one-team");
    }

    function renderCurrentOrder(order) {
        const section = document.getElementById("currentOrderSection");
        section.hidden = !order;
        if (!order) return;
        const badgeClass = order.orderStatus === 1 ? "paid" : order.orderStatus === 2 ? "closed" : "";
        document.getElementById("currentOrderCard").innerHTML = `
            <div class="order-status-box">
                <div class="order-status-head">
                    <strong>${StoreApi.escapeHtml(order.teamStatusText || "拼团队伍")}</strong>
                    <span class="status-badge ${badgeClass}">${StoreApi.escapeHtml(order.orderStatusText)}</span>
                </div>
                <code>交易号 ${StoreApi.escapeHtml(order.outTradeNo)}</code>
                <div class="order-progress">
                    <span>团队 ${order.lockCount || 0}/${order.targetCount || 0} 人锁单</span>
                    <strong>¥${StoreApi.money(order.payPrice)}</strong>
                </div>
                <div class="order-actions">
                    <button type="button" data-view-team="${StoreApi.escapeHtml(order.teamId)}">团队详情</button>
                    ${order.canCancel ? '<button type="button" data-order-action="cancel">取消锁单</button>' : ""}
                    ${order.canRefund ? '<button type="button" data-order-action="refund">申请退款</button>' : ""}
                    ${order.canPay ? '<button class="primary" type="button" data-order-action="pay">立即支付</button>' : ""}
                </div>
            </div>`;
    }

    function renderInvitedTeam(team) {
        const section = document.getElementById("inviteTeamSection");
        section.hidden = false;
        document.getElementById("inviteTeamStatus").textContent = team.statusText;
        const joined = userId && (team.members || [])
            .some(member => member.userId === userId && member.orderStatus !== 2);
        const avatars = (team.members || []).slice(0, 5)
            .map(member => `<span>${StoreApi.escapeHtml(member.userId.slice(0, 1).toUpperCase())}</span>`).join("");
        document.getElementById("inviteTeamCard").innerHTML = `
            <div class="invite-team-box ${team.canJoin ? "" : "unavailable"}">
                <div class="team-summary-head">
                    <strong>${team.lockCount || 0}/${team.targetCount || 0} 人已锁单</strong>
                    <span class="status-badge ${team.status === 1 ? "paid" : ""}">${StoreApi.escapeHtml(team.statusText)}</span>
                </div>
                <code>团队 ${StoreApi.escapeHtml(team.teamId)}</code>
                <div class="team-members-preview">${avatars || "<span>待</span>"}</div>
                <div class="invite-actions">
                    <button type="button" data-copy-team="${StoreApi.escapeHtml(team.teamId)}">复制邀请</button>
                    <button type="button" data-view-team="${StoreApi.escapeHtml(team.teamId)}">成员详情</button>
                    ${team.canJoin && !joined ? `<button class="primary" type="button" data-team-id="${StoreApi.escapeHtml(team.teamId)}">加入该团</button>` : ""}
                </div>
            </div>`;
    }

    async function lockOrder(teamId) {
        if (!userId) {
            location.href = StoreIdentity.loginUrl(currentRelativeUrl());
            return;
        }
        if (teamId && currentOrder && currentOrder.teamId === teamId && currentOrder.orderStatus !== 2) {
            if (currentOrder.orderStatus === 0) showExistingLock(currentOrder);
            else StoreApi.showToast("当前用户已经加入并支付了这个团队");
            return;
        }
        const modeText = teamId ? `加入团队 ${teamId}` : "发起一个新拼团";
        if (!confirm(`当前用户：${identity.nickname}（${userId}）\n确认${modeText}并锁定名额？`)) return;

        const outTradeNo = `${Date.now().toString().slice(-8)}${StoreApi.randomNumber(4)}`;
        try {
            const result = await StoreApi.post("/api/v1/gbm/trade/lock_market_pay_order", {
                userId,
                teamId: teamId || null,
                activityId: market.activityId,
                goodsId,
                source: product.source || "s01",
                channel: product.channel || "c01",
                outTradeNo,
                notifyConfigVO: {notifyType: "MQ", notifyMQ: "topic.team_success"}
            });
            lockedOrder = Object.assign({}, result, {outTradeNo, teamId: result.teamId});
            showLockResult(lockedOrder);
            await refreshBusinessState();
        } catch (error) {
            StoreApi.showToast(error.message, "error");
            if (teamId) await loadInvitedTeam();
        }
    }

    function showExistingLock(order) {
        lockedOrder = order;
        showLockResult({
            orderId: order.orderId,
            outTradeNo: order.outTradeNo,
            teamId: order.teamId,
            payPrice: order.payPrice
        });
    }

    function showLockResult(order) {
        document.getElementById("lockedOrderId").textContent = order.orderId || "--";
        document.getElementById("lockedTradeNo").textContent = order.outTradeNo || "--";
        document.getElementById("lockedTeamId").textContent = order.teamId || "--";
        document.getElementById("lockedAmount").textContent = `¥${StoreApi.money(order.payPrice)}`;
        openModal(lockModal);
    }

    function openPayment(order) {
        paymentOrder = order;
        document.getElementById("paymentAmount").textContent = `¥${StoreApi.money(order.payPrice)}`;
        document.getElementById("outTradeNo").textContent = order.outTradeNo;
        closeModal(lockModal);
        openModal(paymentModal);
    }

    async function settleOrder() {
        if (!paymentOrder) return;
        const button = document.getElementById("completePayment");
        button.disabled = true;
        button.textContent = "结算中...";
        try {
            await StoreApi.post("/api/v1/gbm/trade/settlement_market_pay_order", {
                source: paymentOrder.source || product.source || "s01",
                channel: paymentOrder.channel || product.channel || "c01",
                userId,
                outTradeNo: paymentOrder.outTradeNo,
                outTradeTime: new Date()
            });
            closeModal(paymentModal);
            StoreApi.showToast(`支付 ¥${StoreApi.money(paymentOrder.payPrice)} 成功`, "success");
            await delay(500);
            await refreshBusinessState();
        } catch (error) {
            StoreApi.showToast(error.message, "error");
        } finally {
            button.disabled = false;
            button.textContent = "我已完成支付";
        }
    }

    async function refundOrder(order, action) {
        const text = action === "cancel" ? "取消这笔待支付锁单并释放名额" : "申请退款";
        if (!confirm(`确认${text}？\n交易单号：${order.outTradeNo}`)) return;
        try {
            const result = await StoreApi.post("/api/v1/gbm/trade/refund_market_pay_order", {
                userId,
                outTradeNo: order.outTradeNo,
                source: order.source || product.source || "s01",
                channel: order.channel || product.channel || "c01"
            });
            StoreApi.showToast(result.info || (action === "cancel" ? "锁单已取消" : "退款成功"), "success");
            await delay(400);
            await refreshBusinessState();
        } catch (error) {
            StoreApi.showToast(error.message, "error");
        }
    }

    async function showTeam(teamId) {
        try {
            const team = await StoreApi.get(`/api/v1/gbm/store/teams/${encodeURIComponent(teamId)}`);
            const members = (team.members || []).map(member => {
                const mine = member.userId === userId;
                const statusClass = member.orderStatus === 1 ? "paid" : member.orderStatus === 2 ? "closed" : "";
                return `
                    <div class="member-row">
                        <b>${StoreApi.escapeHtml(member.userId.slice(0, 1).toUpperCase())}</b>
                        <div><strong>${mine ? "我 · " + StoreApi.escapeHtml(identity.nickname) : maskUser(member.userId)}</strong>
                        <small>${StoreApi.escapeHtml(member.outTradeNo)}</small></div>
                        <em class="${statusClass}">${StoreApi.escapeHtml(member.orderStatusText)}</em>
                    </div>`;
            }).join("");
            document.getElementById("teamModalBody").innerHTML = `
                <div class="team-detail-summary">
                    <strong>${StoreApi.escapeHtml(team.statusText)} · ${team.completeCount || 0}/${team.targetCount || 0} 人已支付</strong>
                    <span>${team.lockCount || 0} 人已锁单，团队编号 ${StoreApi.escapeHtml(team.teamId)}</span>
                </div>
                <div class="member-list">${members || "<p>暂无成员记录</p>"}</div>
                <div class="team-modal-actions">
                    <button type="button" data-copy-team="${StoreApi.escapeHtml(team.teamId)}">复制邀请链接</button>
                    ${team.canJoin ? `<button type="button" data-team-id="${StoreApi.escapeHtml(team.teamId)}">加入这个团</button>` : "<button type=\"button\" disabled>当前不可加入</button>"}
                </div>`;
            openModal(teamModal);
        } catch (error) {
            StoreApi.showToast(error.message, "error");
        }
    }

    async function copyInvite(teamId) {
        const url = new URL("product-detail.html", location.href);
        url.search = new URLSearchParams({goodsId, teamId}).toString();
        const text = url.toString();
        try {
            if (navigator.clipboard && window.isSecureContext) {
                await navigator.clipboard.writeText(text);
            } else {
                const area = document.createElement("textarea");
                area.value = text;
                area.style.position = "fixed";
                area.style.opacity = "0";
                document.body.appendChild(area);
                area.select();
                document.execCommand("copy");
                area.remove();
            }
            StoreApi.showToast("邀请链接已复制，切换其他用户后打开即可参团", "success");
        } catch (error) {
            StoreApi.showToast("复制失败，请从地址栏复制邀请链接", "error");
        }
    }

    function maskUser(value) {
        return StoreApi.escapeHtml(maskUserValue(value));
    }

    function maskUserValue(value) {
        const user = String(value || "拼友");
        return user.length <= 2 ? `${user[0]}*` : `${user.slice(0, 1)}***${user.slice(-1)}`;
    }

    function parseCountdown(value) {
        const parts = String(value || "").split(":").map(Number);
        return parts.length === 3 && parts.every(Number.isFinite)
            ? parts[0] * 3600 + parts[1] * 60 + parts[2] : 0;
    }

    function startCountdowns() {
        document.querySelectorAll(".countdown").forEach(element => {
            element.dataset.endsAt = String(Date.now() + Number(element.dataset.seconds || 0) * 1000);
        });
        updateCountdowns();
        countdownTimer = setInterval(updateCountdowns, 1000);
    }

    function updateCountdowns() {
        document.querySelectorAll(".countdown").forEach(element => {
            const seconds = Math.max(Math.ceil((Number(element.dataset.endsAt || 0) - Date.now()) / 1000), 0);
            if (seconds <= 0) {
                element.textContent = "已结束";
                return;
            }
            const hours = String(Math.floor(seconds / 3600)).padStart(2, "0");
            const minutes = String(Math.floor(seconds % 3600 / 60)).padStart(2, "0");
            const secs = String(seconds % 60).padStart(2, "0");
            element.textContent = `${hours}:${minutes}:${secs}`;
        });
    }

    function emptyMessage(icon, title, copy, action) {
        return `<div class="empty-group"><span>${icon}</span><div><strong>${StoreApi.escapeHtml(title)}</strong><p>${StoreApi.escapeHtml(copy)}</p></div>${action || ""}</div>`;
    }

    function openModal(element) {
        element.classList.add("open");
        element.setAttribute("aria-hidden", "false");
    }

    function closeModal(element) {
        element.classList.remove("open");
        element.setAttribute("aria-hidden", "true");
    }

    function currentRelativeUrl() {
        return `${location.pathname.split("/").pop() || "product-detail.html"}${location.search}`;
    }

    function delay(milliseconds) {
        return new Promise(resolve => setTimeout(resolve, milliseconds));
    }

    function renderFatal(message) {
        document.getElementById("detailPage").innerHTML = `
            <section class="fatal-state"><span>!</span><h1>商品暂时无法查看</h1><p>${StoreApi.escapeHtml(message)}</p><a href="index.html">返回商城首页</a></section>`;
        document.querySelector(".bottom-actions").hidden = true;
    }

    document.addEventListener("click", event => {
        const teamButton = event.target.closest("[data-team-id]");
        if (teamButton) {
            closeModal(teamModal);
            lockOrder(teamButton.dataset.teamId);
            return;
        }
        const viewButton = event.target.closest("[data-view-team]");
        if (viewButton) {
            showTeam(viewButton.dataset.viewTeam);
            return;
        }
        const copyButton = event.target.closest("[data-copy-team]");
        if (copyButton) {
            copyInvite(copyButton.dataset.copyTeam);
            return;
        }
        if (event.target.closest("[data-start-group]")) lockOrder(null);
        const actionButton = event.target.closest("[data-order-action]");
        if (actionButton && currentOrder) {
            if (actionButton.dataset.orderAction === "pay") openPayment(currentOrder);
            else refundOrder(currentOrder, actionButton.dataset.orderAction);
        }
    });

    startGroup.addEventListener("click", () => lockOrder(null));
    buyAlone.addEventListener("click", () => StoreApi.showToast("单独购买由外部商城承接，本项目演示拼团交易"));
    document.getElementById("completePayment").addEventListener("click", settleOrder);
    document.getElementById("cancelPayment").addEventListener("click", () => closeModal(paymentModal));
    document.getElementById("closeModal").addEventListener("click", () => closeModal(paymentModal));
    document.querySelectorAll("[data-close-lock]").forEach(button =>
        button.addEventListener("click", () => closeModal(lockModal)));
    document.getElementById("payAfterLock").addEventListener("click", () => {
        if (lockedOrder) openPayment(lockedOrder);
    });
    document.getElementById("inviteAfterLock").addEventListener("click", () => {
        if (lockedOrder) copyInvite(lockedOrder.teamId);
    });
    document.getElementById("closeTeamModal").addEventListener("click", () => closeModal(teamModal));
    [paymentModal, lockModal, teamModal].forEach(modal =>
        modal.addEventListener("click", event => {
            if (event.target === modal) closeModal(modal);
        }));

    loadDetail();
});
