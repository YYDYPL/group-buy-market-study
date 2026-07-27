document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("identityForm");
    const list = document.getElementById("userList");
    const empty = document.getElementById("emptyUsers");
    const error = document.getElementById("formError");
    const returnUrl = StoreIdentity.safeReturnUrl(
        new URLSearchParams(location.search).get("returnUrl"), "index.html");

    function escapeHtml(value) {
        return String(value == null ? "" : value)
            .replace(/&/g, "&amp;").replace(/</g, "&lt;")
            .replace(/>/g, "&gt;").replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    function render() {
        const users = StoreIdentity.list();
        const active = StoreIdentity.current();
        document.getElementById("userCount").textContent = `${users.length} 个`;
        empty.hidden = users.length > 0;
        list.innerHTML = users.map(user => `
            <article class="user-item ${active && active.userId === user.userId ? "active" : ""}">
                <div class="user-avatar">${escapeHtml(user.nickname.slice(0, 1).toUpperCase())}</div>
                <div class="user-copy">
                    <strong>${escapeHtml(user.nickname)}</strong>
                    <code>${escapeHtml(user.userId)}</code>
                </div>
                ${active && active.userId === user.userId
                    ? '<span class="active-mark">当前</span>'
                    : `<button type="button" data-switch="${escapeHtml(user.userId)}">使用</button>`}
                <button class="remove-user" type="button" data-remove="${escapeHtml(user.userId)}" aria-label="删除用户">×</button>
            </article>
        `).join("");
    }

    form.addEventListener("submit", event => {
        event.preventDefault();
        error.textContent = "";
        try {
            StoreIdentity.register(
                document.getElementById("userId").value,
                document.getElementById("nickname").value);
            location.href = returnUrl;
        } catch (exception) {
            error.textContent = exception.message;
        }
    });

    list.addEventListener("click", event => {
        const switchButton = event.target.closest("[data-switch]");
        if (switchButton) {
            StoreIdentity.switchUser(switchButton.dataset.switch);
            location.href = returnUrl;
            return;
        }
        const removeButton = event.target.closest("[data-remove]");
        if (removeButton && confirm("只删除浏览器中的模拟身份，历史订单仍保留。确认删除？")) {
            StoreIdentity.remove(removeButton.dataset.remove);
            render();
        }
    });

    render();
});
