(function () {
    const host = window.location.hostname || "127.0.0.1";
    const portParameter = new URLSearchParams(window.location.search).get("apiPort");
    const apiPort = /^\d{1,5}$/.test(portParameter || "")
        && Number(portParameter) > 0 && Number(portParameter) <= 65535
        ? portParameter
        : "8091";
    const apiBaseUrl = `${window.location.protocol === "https:" ? "https:" : "http:"}//${host}:${apiPort}`;

    async function request(path, options) {
        const response = await fetch(apiBaseUrl + path, options);
        let payload;
        try {
            payload = await response.json();
        } catch (error) {
            throw new Error(`服务响应格式错误（HTTP ${response.status}）`);
        }
        if (!response.ok || payload.code !== "0000") {
            const message = payload && payload.info ? payload.info : `请求失败（HTTP ${response.status}）`;
            const exception = new Error(message);
            exception.code = payload && payload.code;
            exception.status = response.status;
            throw exception;
        }
        return payload.data;
    }

    function get(path) {
        return request(path, {method: "GET"});
    }

    function post(path, body, headers) {
        return request(path, {
            method: "POST",
            headers: Object.assign({"Content-Type": "application/json"}, headers || {}),
            body: body === undefined ? undefined : JSON.stringify(body)
        });
    }

    function assetUrl(value, fromAdmin) {
        if (!value) return fromAdmin ? "../images/placeholder-product.svg" : "images/placeholder-product.svg";
        if (/^https?:\/\//i.test(value) || value.startsWith("data:")) return value;
        if (value.startsWith("/uploads/")) return apiBaseUrl + value;
        if (value.startsWith("/")) return value;
        return fromAdmin ? "../" + value.replace(/^\.\//, "") : value;
    }

    function money(value) {
        const amount = Number(value || 0);
        return amount.toFixed(amount % 1 === 0 ? 0 : 2);
    }

    function sales(value) {
        const count = Number(value || 0);
        return count >= 10000 ? `${(count / 10000).toFixed(count >= 100000 ? 0 : 1)}万+` : `${count}+`;
    }

    function cookie(name) {
        const target = `${name}=`;
        return document.cookie.split(";").map(item => item.trim()).find(item => item.startsWith(target))?.slice(target.length) || "";
    }

    function currentUser() {
        const identity = window.StoreIdentity && window.StoreIdentity.current();
        return identity ? identity.userId : decodeURIComponent(cookie("username") || "");
    }

    function randomNumber(length) {
        let result = "";
        for (let i = 0; i < length; i++) result += Math.floor(Math.random() * 10);
        return result;
    }

    function escapeHtml(value) {
        return String(value == null ? "" : value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    function showToast(message, type) {
        const toast = document.getElementById("toast");
        if (!toast) return;
        toast.textContent = message;
        toast.className = `toast show ${type || ""}`;
        clearTimeout(showToast.timer);
        showToast.timer = setTimeout(() => toast.classList.remove("show"), 2600);
    }

    window.StoreApi = {
        apiBaseUrl,
        apiPort,
        request,
        get,
        post,
        assetUrl,
        money,
        sales,
        currentUser,
        randomNumber,
        escapeHtml,
        showToast
    };
})();
