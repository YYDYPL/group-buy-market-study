(function () {
    "use strict";

    const SESSION_KEY = "group-buy-flow-state-v1";
    const SUCCESS_CODE = "0000";
    const API_PATHS = {
        trial: "/api/v1/gbm/index/query_group_buy_market_config",
        lock: "/api/v1/gbm/trade/lock_market_pay_order",
        settle: "/api/v1/gbm/trade/settlement_market_pay_order",
        refund: "/api/v1/gbm/trade/refund_market_pay_order"
    };
    const QUEUES = {
        success: "group_buy_market_queue_2_topic_team_success",
        refund: "group_buy_market_queue_2_topic_team_refund"
    };
    const STATUS_TEXT = {
        idle: "待锁单",
        locking: "锁单中",
        locked: "已锁单",
        settling: "结算中",
        paid: "已结算",
        refunding: "退款中",
        refunded: "已退款"
    };

    let state = loadState();
    let busy = false;

    const elements = {};

    document.addEventListener("DOMContentLoaded", init);

    function init() {
        cacheElements();
        bindEvents();
        syncSettingsForm();
        render();
        checkApplication(false);
    }

    function cacheElements() {
        [
            "apiStatus", "openSettingsButton", "resetButton", "activityBadge", "payPrice",
            "originalPrice", "deductionPrice", "allTeamCount", "allTeamCompleteCount",
            "allTeamUserCount", "trialButton", "autoRunButton", "workflowSteps", "modeCreate",
            "modeJoin", "joinTeamPanel", "joinTeamId", "useFirstTeamButton", "currentTeamId",
            "lockProgressText", "lockProgressBar", "settleProgressText", "settleProgressBar",
            "addParticipantButton", "participantRows", "lockNextButton", "settleNextButton",
            "refundNextButton", "refreshMqButton", "mqHint", "successPublish", "successAck",
            "successMessages", "refundPublish", "refundAck", "refundMessages", "rabbitConsoleLink",
            "availableTeams", "clearLogButton", "requestLog", "settingsDialog", "apiBaseInput",
            "rabbitBaseInput", "rabbitUserInput", "rabbitPasswordInput", "goodsIdInput", "sourceInput",
            "channelInput", "saveSettingsButton", "toastRegion"
        ].forEach((id) => {
            elements[id] = document.getElementById(id);
        });
    }

    function bindEvents() {
        elements.openSettingsButton.addEventListener("click", () => elements.settingsDialog.showModal());
        elements.saveSettingsButton.addEventListener("click", saveSettings);
        elements.resetButton.addEventListener("click", resetFlow);
        elements.trialButton.addEventListener("click", runTrial);
        elements.autoRunButton.addEventListener("click", runToTeamComplete);
        elements.lockNextButton.addEventListener("click", lockNext);
        elements.settleNextButton.addEventListener("click", settleNext);
        elements.refundNextButton.addEventListener("click", refundNext);
        elements.addParticipantButton.addEventListener("click", addParticipant);
        elements.refreshMqButton.addEventListener("click", () => refreshMq(false));
        elements.clearLogButton.addEventListener("click", () => {
            state.logs = [];
            persist();
            renderLogs();
        });
        elements.modeCreate.addEventListener("change", updateTeamMode);
        elements.modeJoin.addEventListener("change", updateTeamMode);
        elements.joinTeamId.addEventListener("input", (event) => {
            state.joinTeamId = event.target.value.trim();
            persist();
        });
        elements.useFirstTeamButton.addEventListener("click", useFirstAvailableTeam);
        elements.participantRows.addEventListener("click", handleParticipantAction);
        elements.participantRows.addEventListener("change", handleParticipantInput);
        elements.availableTeams.addEventListener("click", handleAvailableTeamAction);
    }

    function createInitialState() {
        return {
            config: {
                apiBase: "http://127.0.0.1:8091",
                rabbitBase: "http://127.0.0.1:15672",
                rabbitUser: "admin",
                rabbitPassword: "admin",
                goodsId: "9890001",
                source: "s01",
                channel: "c01"
            },
            mode: "create",
            joinTeamId: "",
            teamId: "",
            trial: null,
            participants: createParticipants(3),
            mq: {
                success: null,
                refund: null,
                baseline: null,
                error: ""
            },
            logs: []
        };
    }

    function createParticipants(count) {
        const stamp = Date.now().toString();
        const userSeed = stamp.slice(-8);
        const tradeSeed = stamp.slice(-10);
        return Array.from({ length: count }, (_, index) => ({
            id: `${stamp}-${index}`,
            userId: `web${String.fromCharCode(65 + index)}${userSeed}`,
            outTradeNo: `${tradeSeed}${String(index + 1).padStart(2, "0")}`.slice(-12),
            status: "idle",
            orderId: "",
            teamId: "",
            payPrice: null,
            error: ""
        }));
    }

    function loadState() {
        try {
            const saved = sessionStorage.getItem(SESSION_KEY);
            if (!saved) return createInitialState();
            const parsed = JSON.parse(saved);
            return Object.assign(createInitialState(), parsed, {
                config: Object.assign(createInitialState().config, parsed.config || {}),
                mq: Object.assign(createInitialState().mq, parsed.mq || {})
            });
        } catch (error) {
            return createInitialState();
        }
    }

    function persist() {
        sessionStorage.setItem(SESSION_KEY, JSON.stringify(state));
    }

    function normalizeBase(value) {
        return String(value || "").trim().replace(/\/+$/, "");
    }

    function syncSettingsForm() {
        elements.apiBaseInput.value = state.config.apiBase;
        elements.rabbitBaseInput.value = state.config.rabbitBase;
        elements.rabbitUserInput.value = state.config.rabbitUser;
        elements.rabbitPasswordInput.value = state.config.rabbitPassword;
        elements.goodsIdInput.value = state.config.goodsId;
        elements.sourceInput.value = state.config.source;
        elements.channelInput.value = state.config.channel;
        elements.rabbitConsoleLink.href = state.config.rabbitBase;
    }

    async function saveSettings() {
        const required = [elements.apiBaseInput, elements.rabbitBaseInput, elements.goodsIdInput, elements.sourceInput, elements.channelInput];
        if (required.some((input) => !input.reportValidity())) return;

        state.config = {
            apiBase: normalizeBase(elements.apiBaseInput.value),
            rabbitBase: normalizeBase(elements.rabbitBaseInput.value),
            rabbitUser: elements.rabbitUserInput.value.trim(),
            rabbitPassword: elements.rabbitPasswordInput.value,
            goodsId: elements.goodsIdInput.value.trim(),
            source: elements.sourceInput.value.trim(),
            channel: elements.channelInput.value.trim()
        };
        state.trial = null;
        state.mq = { success: null, refund: null, baseline: null, error: "" };
        persist();
        elements.settingsDialog.close();
        syncSettingsForm();
        render();
        await checkApplication(true);
    }

    async function checkApplication(showResult) {
        setApiStatus("is-idle", "检查连接");
        try {
            // Actuator does not expose CORS in the dev profile. An opaque request
            // verifies network reachability without weakening Actuator security.
            await fetchWithTimeout(`${state.config.apiBase}/actuator/health`, {
                method: "GET",
                mode: "no-cors"
            }, 5000);
            setApiStatus("is-up", "应用可访问");
            if (showResult) toast("应用地址可以访问", "success");
        } catch (error) {
            setApiStatus("is-down", "应用不可访问");
            if (showResult) toast(`应用地址不可访问：${error.message}`, "error");
        }
    }

    function setApiStatus(className, text) {
        elements.apiStatus.className = `connection-status ${className}`;
        elements.apiStatus.innerHTML = `<span class="status-dot"></span>${escapeHtml(text)}`;
    }

    async function runTrial() {
        if (busy) return;
        const first = state.participants[0];
        if (!first || !first.userId.trim()) {
            toast("请先填写首位参与者用户 ID", "error");
            return;
        }

        setBusy(true);
        try {
            const response = await apiRequest(API_PATHS.trial, {
                userId: first.userId.trim(),
                source: state.config.source,
                channel: state.config.channel,
                goodsId: state.config.goodsId
            }, "商品试算");
            state.trial = response.data;
            state.participants.forEach((participant) => { participant.error = ""; });
            setApiStatus("is-up", "应用已连接");
            persist();
            render();
            toast(`试算成功，活动 ${response.data.activityId}`, "success");
            await refreshMq(true);
        } catch (error) {
            setApiStatus("is-down", "接口调用失败");
            toast(error.message, "error");
        } finally {
            setBusy(false);
        }
    }

    async function lockParticipant(participantId, manageBusy = true) {
        const participant = findParticipant(participantId);
        if (!participant || participant.status !== "idle") return false;
        if (!state.trial) {
            toast("请先完成商品试算", "error");
            return false;
        }
        if (!participant.userId.trim() || !participant.outTradeNo.trim()) {
            toast("用户 ID 和外部交易号不能为空", "error");
            return false;
        }
        if (participant.outTradeNo.trim().length !== 12) {
            toast("外部交易号必须为 12 位", "error");
            return false;
        }

        const targetTeamId = state.teamId || (state.mode === "join" ? state.joinTeamId.trim() : "");
        if (state.mode === "join" && !targetTeamId) {
            toast("加入已有团时必须填写团队 ID", "error");
            return false;
        }

        if (manageBusy) setBusy(true);
        participant.status = "locking";
        participant.error = "";
        render();

        try {
            const response = await apiRequest(API_PATHS.lock, {
                userId: participant.userId.trim(),
                teamId: targetTeamId || null,
                activityId: state.trial.activityId,
                goodsId: state.config.goodsId,
                source: state.config.source,
                channel: state.config.channel,
                outTradeNo: participant.outTradeNo.trim(),
                notifyConfigVO: {
                    notifyType: "MQ",
                    notifyMQ: "topic.team_success",
                    notifyUrl: null
                }
            }, `锁单 / ${participant.userId}`);

            participant.status = "locked";
            participant.orderId = response.data.orderId || "";
            participant.teamId = response.data.teamId || targetTeamId;
            participant.payPrice = response.data.payPrice;
            state.teamId = participant.teamId || state.teamId;
            persist();
            render();
            toast(`${participant.userId} 锁单成功`, "success");
            return true;
        } catch (error) {
            participant.status = "idle";
            participant.error = error.message;
            persist();
            render();
            toast(error.message, "error");
            return false;
        } finally {
            if (manageBusy) setBusy(false);
        }
    }

    async function settleParticipant(participantId, manageBusy = true) {
        const participant = findParticipant(participantId);
        if (!participant || participant.status !== "locked") return false;

        if (manageBusy) setBusy(true);
        participant.status = "settling";
        participant.error = "";
        render();

        try {
            await apiRequest(API_PATHS.settle, {
                source: state.config.source,
                channel: state.config.channel,
                userId: participant.userId,
                outTradeNo: participant.outTradeNo,
                outTradeTime: new Date().toISOString()
            }, `支付结算 / ${participant.userId}`);
            participant.status = "paid";
            persist();
            render();
            toast(`${participant.userId} 结算成功`, "success");

            if (state.participants.every((item) => ["paid", "refunded"].includes(item.status))) {
                await pollMq("success");
            }
            return true;
        } catch (error) {
            participant.status = "locked";
            participant.error = error.message;
            persist();
            render();
            toast(error.message, "error");
            return false;
        } finally {
            if (manageBusy) setBusy(false);
        }
    }

    async function refundParticipant(participantId, manageBusy = true) {
        const participant = findParticipant(participantId);
        if (!participant || !["locked", "paid"].includes(participant.status)) return false;
        const previousStatus = participant.status;

        if (manageBusy) setBusy(true);
        participant.status = "refunding";
        participant.error = "";
        render();

        try {
            const response = await apiRequest(API_PATHS.refund, {
                userId: participant.userId,
                outTradeNo: participant.outTradeNo,
                source: state.config.source,
                channel: state.config.channel
            }, `退款 / ${participant.userId}`);
            participant.status = "refunded";
            participant.refundInfo = response.data ? response.data.info : "";
            persist();
            render();
            toast(`${participant.userId} 退款成功`, "success");
            await pollMq("refund");
            return true;
        } catch (error) {
            participant.status = previousStatus;
            participant.error = error.message;
            persist();
            render();
            toast(error.message, "error");
            return false;
        } finally {
            if (manageBusy) setBusy(false);
        }
    }

    async function lockNext() {
        const participant = state.participants.find((item) => item.status === "idle");
        if (participant) await lockParticipant(participant.id);
    }

    async function settleNext() {
        const participant = state.participants.find((item) => item.status === "locked");
        if (participant) await settleParticipant(participant.id);
    }

    async function refundNext() {
        const participant = state.participants.find((item) => item.status === "paid") ||
            state.participants.find((item) => item.status === "locked");
        if (participant) await refundParticipant(participant.id);
    }

    async function runToTeamComplete() {
        if (busy || !state.trial) return;
        setBusy(true);
        try {
            for (const participant of state.participants) {
                if (participant.status === "idle") {
                    const locked = await lockParticipant(participant.id, false);
                    if (!locked) return;
                    await delay(220);
                }
            }
            for (const participant of state.participants) {
                if (participant.status === "locked") {
                    const settled = await settleParticipant(participant.id, false);
                    if (!settled) return;
                    await delay(260);
                }
            }
            toast("全部订单已结算，成团链路执行完成", "success");
        } finally {
            setBusy(false);
        }
    }

    async function apiRequest(path, body, label) {
        const startedAt = performance.now();
        let responsePayload = null;
        let ok = false;
        let errorMessage = "";
        try {
            const response = await fetchWithTimeout(`${state.config.apiBase}${path}`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(body)
            }, 15000);
            const text = await response.text();
            try {
                responsePayload = text ? JSON.parse(text) : null;
            } catch (error) {
                responsePayload = { raw: text };
            }
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            if (!responsePayload || responsePayload.code !== SUCCESS_CODE) {
                throw new Error(`${responsePayload && responsePayload.code ? responsePayload.code : "ERROR"}：${responsePayload && responsePayload.info ? responsePayload.info : "接口处理失败"}`);
            }
            ok = true;
            return responsePayload;
        } catch (error) {
            errorMessage = error.name === "AbortError" ? "请求超时，请检查应用状态" : error.message;
            throw new Error(`${label}失败：${errorMessage}`);
        } finally {
            addLog({
                label,
                path,
                request: body,
                response: responsePayload || { error: errorMessage },
                ok,
                duration: Math.round(performance.now() - startedAt)
            });
        }
    }

    async function fetchWithTimeout(url, options, timeout) {
        const controller = new AbortController();
        const timer = window.setTimeout(() => controller.abort(), timeout);
        try {
            return await fetch(url, Object.assign({}, options, { signal: controller.signal }));
        } finally {
            window.clearTimeout(timer);
        }
    }

    function addLog(entry) {
        state.logs.unshift(Object.assign({
            id: `${Date.now()}-${Math.random()}`,
            time: new Date().toLocaleTimeString("zh-CN", { hour12: false })
        }, entry));
        state.logs = state.logs.slice(0, 40);
        persist();
        renderLogs();
    }

    async function refreshMq(setBaseline) {
        elements.refreshMqButton.disabled = true;
        try {
            const [success, refund] = await Promise.all([
                queryQueue(QUEUES.success),
                queryQueue(QUEUES.refund)
            ]);
            state.mq.success = success;
            state.mq.refund = refund;
            state.mq.error = "";
            if (setBaseline || !state.mq.baseline) {
                state.mq.baseline = {
                    successPublish: success.publish,
                    refundPublish: refund.publish
                };
            }
            persist();
            renderMq();
            return true;
        } catch (error) {
            state.mq.error = error.message;
            persist();
            renderMq();
            return false;
        } finally {
            elements.refreshMqButton.disabled = busy;
        }
    }

    async function queryQueue(queueName) {
        const credentials = btoa(`${state.config.rabbitUser}:${state.config.rabbitPassword}`);
        const url = `${state.config.rabbitBase}/api/queues/%2F/${encodeURIComponent(queueName)}`;
        const response = await fetchWithTimeout(url, {
            method: "GET",
            headers: { Authorization: `Basic ${credentials}` }
        }, 6000);
        if (!response.ok) throw new Error(`RabbitMQ HTTP ${response.status}`);
        const payload = await response.json();
        const stats = payload.message_stats || {};
        return {
            publish: numberOrZero(stats.publish),
            ack: numberOrZero(stats.ack),
            messages: numberOrZero(payload.messages),
            consumers: numberOrZero(payload.consumers)
        };
    }

    async function pollMq(queueType) {
        const baselineKey = queueType === "success" ? "successPublish" : "refundPublish";
        const baseline = state.mq.baseline ? numberOrZero(state.mq.baseline[baselineKey]) : null;
        for (let attempt = 0; attempt < 5; attempt += 1) {
            const readable = await refreshMq(false);
            if (!readable) return;
            const current = state.mq[queueType];
            if (baseline === null || (current.publish > baseline && current.ack >= current.publish && current.messages === 0)) return;
            await delay(700);
        }
    }

    function updateTeamMode(event) {
        if (!event.target.checked) return;
        if (state.teamId) {
            render();
            toast("团队已经锁定，本轮不能切换组团方式", "error");
            return;
        }
        state.mode = event.target.value;
        persist();
        render();
    }

    function useFirstAvailableTeam() {
        const teams = state.trial && Array.isArray(state.trial.teamList) ? state.trial.teamList : [];
        if (!teams.length) {
            toast("当前没有可参与团队", "error");
            return;
        }
        chooseAvailableTeam(teams[0].teamId);
    }

    function chooseAvailableTeam(teamId) {
        if (state.teamId) {
            toast("本轮已经开始锁单，不能更换团队", "error");
            return;
        }
        state.mode = "join";
        state.joinTeamId = String(teamId);
        persist();
        render();
        toast(`已选择团队 ${teamId}`, "success");
    }

    function addParticipant() {
        if (busy || state.participants.some((item) => item.status !== "idle")) {
            toast("开始锁单后不能调整参与者", "error");
            return;
        }
        const next = createParticipants(1)[0];
        next.userId = `web${String.fromCharCode(65 + state.participants.length)}${Date.now().toString().slice(-8)}`;
        next.outTradeNo = `${Date.now().toString().slice(-10)}${String(state.participants.length + 1).padStart(2, "0")}`.slice(-12);
        state.participants.push(next);
        persist();
        render();
    }

    function handleParticipantInput(event) {
        const row = event.target.closest("[data-participant-id]");
        if (!row) return;
        const participant = findParticipant(row.dataset.participantId);
        if (!participant || participant.status !== "idle") return;
        const field = event.target.dataset.field;
        if (field === "userId" || field === "outTradeNo") {
            participant[field] = event.target.value.trim();
            participant.error = "";
            persist();
        }
    }

    async function handleParticipantAction(event) {
        const button = event.target.closest("button[data-action]");
        if (!button || busy) return;
        const participantId = button.closest("[data-participant-id]").dataset.participantId;
        if (button.dataset.action === "lock") await lockParticipant(participantId);
        if (button.dataset.action === "settle") await settleParticipant(participantId);
        if (button.dataset.action === "refund") await refundParticipant(participantId);
        if (button.dataset.action === "remove") removeParticipant(participantId);
    }

    function removeParticipant(participantId) {
        if (state.participants.length <= 1) {
            toast("至少保留一位参与者", "error");
            return;
        }
        const participant = findParticipant(participantId);
        if (!participant || participant.status !== "idle") return;
        state.participants = state.participants.filter((item) => item.id !== participantId);
        persist();
        render();
    }

    function handleAvailableTeamAction(event) {
        const button = event.target.closest("button[data-team-id]");
        if (button) chooseAvailableTeam(button.dataset.teamId);
    }

    function findParticipant(participantId) {
        return state.participants.find((item) => item.id === participantId);
    }

    function resetFlow() {
        if (busy) return;
        const config = state.config;
        state = createInitialState();
        state.config = config;
        persist();
        syncSettingsForm();
        render();
        toast("已创建一组新的参与者和交易号", "success");
    }

    function setBusy(value) {
        busy = value;
        renderControls();
    }

    function render() {
        renderProduct();
        renderTeamMode();
        renderParticipants();
        renderTeamSummary();
        renderWorkflow();
        renderAvailableTeams();
        renderMq();
        renderLogs();
        renderControls();
    }

    function renderProduct() {
        const trial = state.trial;
        const goods = trial && trial.goods;
        const stats = trial && trial.teamStatistic;
        elements.activityBadge.textContent = trial ? `活动 ${trial.activityId}` : "活动待试算";
        elements.payPrice.textContent = goods ? formatMoney(goods.payPrice) : "--";
        elements.originalPrice.textContent = goods ? `原价 ¥${formatMoney(goods.originalPrice)}` : "原价 --";
        elements.deductionPrice.textContent = goods ? `优惠 ¥${formatMoney(goods.deductionPrice)}` : "优惠 --";
        elements.allTeamCount.textContent = stats ? stats.allTeamCount : "--";
        elements.allTeamCompleteCount.textContent = stats ? stats.allTeamCompleteCount : "--";
        elements.allTeamUserCount.textContent = stats ? stats.allTeamUserCount : "--";
    }

    function renderTeamMode() {
        elements.modeCreate.checked = state.mode === "create";
        elements.modeJoin.checked = state.mode === "join";
        elements.modeCreate.disabled = Boolean(state.teamId) || busy;
        elements.modeJoin.disabled = Boolean(state.teamId) || busy;
        elements.joinTeamPanel.hidden = state.mode !== "join";
        elements.joinTeamId.value = state.joinTeamId;
        elements.joinTeamId.disabled = Boolean(state.teamId) || busy;
    }

    function renderParticipants() {
        elements.participantRows.innerHTML = state.participants.map((participant, index) => {
            const isBusy = ["locking", "settling", "refunding"].includes(participant.status);
            const editable = participant.status === "idle" && !busy;
            const primaryAction = getParticipantAction(participant);
            return `
                <div class="participant-row${isBusy ? " is-busy" : ""}" role="row" data-participant-id="${escapeHtml(participant.id)}">
                    <div class="participant-user">
                        <span class="participant-number">${index + 1}</span>
                        <input type="text" data-field="userId" value="${escapeHtml(participant.userId)}" ${editable ? "" : "disabled"} aria-label="第 ${index + 1} 位用户 ID">
                    </div>
                    <label class="participant-trade">
                        <span>外部交易号</span>
                        <input type="text" data-field="outTradeNo" value="${escapeHtml(participant.outTradeNo)}" maxlength="12" ${editable ? "" : "disabled"} aria-label="第 ${index + 1} 位外部交易号">
                    </label>
                    <span class="status-label ${statusClass(participant.status)}">${STATUS_TEXT[participant.status] || participant.status}</span>
                    <div class="order-cell">
                        <strong title="${escapeHtml(participant.orderId || "")}">${escapeHtml(participant.orderId || "尚未生成订单")}</strong>
                        <small>${participant.payPrice !== null ? `支付价 ¥${formatMoney(participant.payPrice)}` : escapeHtml(participant.refundInfo || "等待服务端返回")}</small>
                    </div>
                    <div class="row-actions">
                        ${primaryAction}
                        ${editable && state.participants.length > 1 ? '<button class="row-action remove-button" type="button" data-action="remove" title="移除参与者" aria-label="移除参与者">&#215;</button>' : ""}
                    </div>
                    ${participant.error ? `<p class="participant-error">${escapeHtml(participant.error)}</p>` : ""}
                </div>`;
        }).join("");
    }

    function getParticipantAction(participant) {
        if (participant.status === "idle") return '<button class="row-action" type="button" data-action="lock">锁单</button>';
        if (participant.status === "locked") return '<button class="row-action" type="button" data-action="settle">结算</button><button class="row-action danger" type="button" data-action="refund">释放</button>';
        if (participant.status === "paid") return '<button class="row-action danger" type="button" data-action="refund">退款</button>';
        return "";
    }

    function renderTeamSummary() {
        const total = state.participants.length;
        const locked = state.participants.filter((item) => item.status !== "idle" && item.status !== "refunded").length;
        const settled = state.participants.filter((item) => item.status === "paid").length;
        elements.currentTeamId.textContent = state.teamId || (state.mode === "join" && state.joinTeamId ? state.joinTeamId : "尚未创建");
        elements.lockProgressText.textContent = `${locked} / ${total}`;
        elements.settleProgressText.textContent = `${settled} / ${total}`;
        elements.lockProgressBar.style.width = `${total ? (locked / total) * 100 : 0}%`;
        elements.settleProgressBar.style.width = `${total ? (settled / total) * 100 : 0}%`;
    }

    function renderWorkflow() {
        const trialDone = Boolean(state.trial);
        const lockDone = state.participants.length > 0 && state.participants.every((item) => !["idle", "locking"].includes(item.status));
        const settledAtLeastOnce = state.participants.some((item) => ["paid", "refunded"].includes(item.status));
        const settleDone = state.participants.length > 0 && state.participants.every((item) => ["paid", "refunded"].includes(item.status));
        const notifyObserved = mqIncrementObserved("success");
        const refundDone = state.participants.some((item) => item.status === "refunded");
        const completed = { trial: trialDone, lock: lockDone, settle: settleDone, notify: notifyObserved, refund: refundDone };
        let active = "trial";
        if (trialDone) active = "lock";
        if (lockDone || settledAtLeastOnce) active = "settle";
        if (settleDone) active = "notify";
        if (notifyObserved || refundDone) active = "refund";

        elements.workflowSteps.querySelectorAll("li").forEach((item) => {
            const step = item.dataset.step;
            item.classList.toggle("is-complete", completed[step]);
            item.classList.toggle("is-active", step === active && !completed[step]);
        });
    }

    function renderAvailableTeams() {
        const teams = state.trial && Array.isArray(state.trial.teamList) ? state.trial.teamList : [];
        if (!teams.length) {
            elements.availableTeams.innerHTML = `<p class="empty-state">${state.trial ? "当前没有可参与团队，可直接发起新团。" : "完成试算后显示当前可参与团队。"}</p>`;
            return;
        }
        elements.availableTeams.innerHTML = teams.map((team) => `
            <div class="available-team">
                <div class="available-team-head"><code>${escapeHtml(team.teamId)}</code><strong>${team.lockCount} / ${team.targetCount}</strong></div>
                <p>发起人 ${escapeHtml(team.userId)} · 剩余 ${escapeHtml(team.validTimeCountdown || "--")}</p>
                <button class="button button-secondary" type="button" data-team-id="${escapeHtml(team.teamId)}" ${state.teamId ? "disabled" : ""}>选择此团队</button>
            </div>`).join("");
    }

    function renderMq() {
        renderQueue("success", state.mq.success);
        renderQueue("refund", state.mq.refund);
        elements.rabbitConsoleLink.href = state.config.rabbitBase;
        if (state.mq.error) {
            elements.mqHint.textContent = `浏览器暂时无法读取管理 API：${state.mq.error}`;
        } else if (state.mq.success && state.mq.refund) {
            elements.mqHint.textContent = `队列在线，消费者 ${state.mq.success.consumers} / ${state.mq.refund.consumers}。统计为 RabbitMQ 当前累计值。`;
        } else {
            elements.mqHint.textContent = "结算成团及退款后，可在此核对消息发布、确认和积压。";
        }
    }

    function renderQueue(type, queue) {
        elements[`${type}Publish`].textContent = queue ? queue.publish : "--";
        elements[`${type}Ack`].textContent = queue ? queue.ack : "--";
        elements[`${type}Messages`].textContent = queue ? queue.messages : "--";
    }

    function mqIncrementObserved(type) {
        if (!state.mq.baseline || !state.mq[type]) return false;
        const baseline = type === "success" ? state.mq.baseline.successPublish : state.mq.baseline.refundPublish;
        return state.mq[type].publish > numberOrZero(baseline) && state.mq[type].messages === 0;
    }

    function renderLogs() {
        if (!state.logs.length) {
            elements.requestLog.innerHTML = '<p class="empty-state">尚未发起接口请求。</p>';
            return;
        }
        elements.requestLog.innerHTML = state.logs.map((entry) => `
            <details class="log-entry${entry.ok ? "" : " is-error"}">
                <summary>
                    <span class="log-method">POST</span>
                    <span class="log-path">${escapeHtml(entry.label)} · ${escapeHtml(entry.path)}</span>
                    <span class="log-result">${entry.ok ? "成功" : "失败"} / ${entry.duration}ms</span>
                    <time class="log-time">${escapeHtml(entry.time)}</time>
                </summary>
                <div class="log-detail">
                    <div><strong>REQUEST</strong><pre>${escapeHtml(JSON.stringify(entry.request, null, 2))}</pre></div>
                    <div><strong>RESPONSE</strong><pre>${escapeHtml(JSON.stringify(entry.response, null, 2))}</pre></div>
                </div>
            </details>`).join("");
    }

    function renderControls() {
        const hasIdle = state.participants.some((item) => item.status === "idle");
        const hasLocked = state.participants.some((item) => item.status === "locked");
        const hasRefundable = state.participants.some((item) => ["locked", "paid"].includes(item.status));
        const adjustable = state.participants.every((item) => item.status === "idle");
        elements.trialButton.disabled = busy;
        elements.autoRunButton.disabled = busy || !state.trial || !state.participants.some((item) => ["idle", "locked"].includes(item.status));
        elements.lockNextButton.disabled = busy || !state.trial || !hasIdle;
        elements.settleNextButton.disabled = busy || !hasLocked;
        elements.refundNextButton.disabled = busy || !hasRefundable;
        elements.addParticipantButton.disabled = busy || !adjustable;
        elements.refreshMqButton.disabled = busy;
        elements.resetButton.disabled = busy;
    }

    function statusClass(status) {
        if (["locking", "settling", "refunding"].includes(status)) return "busy";
        return status;
    }

    function formatMoney(value) {
        const number = Number(value);
        return Number.isFinite(number) ? number.toFixed(2) : "--";
    }

    function numberOrZero(value) {
        const number = Number(value);
        return Number.isFinite(number) ? number : 0;
    }

    function delay(milliseconds) {
        return new Promise((resolve) => window.setTimeout(resolve, milliseconds));
    }

    function escapeHtml(value) {
        return String(value === null || value === undefined ? "" : value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    function toast(message, type) {
        while (elements.toastRegion.children.length >= 3) {
            elements.toastRegion.firstElementChild.remove();
        }
        const item = document.createElement("div");
        item.className = `toast${type ? ` is-${type}` : ""}`;
        item.textContent = message;
        elements.toastRegion.appendChild(item);
        window.setTimeout(() => item.remove(), 4200);
    }
}());
