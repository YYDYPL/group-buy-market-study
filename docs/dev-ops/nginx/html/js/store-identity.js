(function () {
    const USERS_KEY = "hjs_store_users";
    const ACTIVE_KEY = "hjs_store_active_user";

    function readUsers() {
        try {
            const users = JSON.parse(localStorage.getItem(USERS_KEY) || "[]");
            return Array.isArray(users) ? users.filter(user => user && user.userId) : [];
        } catch (error) {
            return [];
        }
    }

    function writeUsers(users) {
        localStorage.setItem(USERS_KEY, JSON.stringify(users));
    }

    function current() {
        const activeId = localStorage.getItem(ACTIVE_KEY) || "";
        return readUsers().find(user => user.userId === activeId) || null;
    }

    function register(userId, nickname) {
        const normalizedId = String(userId || "").trim();
        const normalizedName = String(nickname || "").trim();
        if (!/^[A-Za-z0-9_-]{2,24}$/.test(normalizedId)) {
            throw new Error("用户ID需为2-24位字母、数字、下划线或短横线");
        }
        if (!normalizedName || normalizedName.length > 16) {
            throw new Error("昵称需为1-16个字符");
        }
        const users = readUsers();
        if (users.some(user => user.userId === normalizedId)) {
            throw new Error("这个用户ID已经注册");
        }
        const user = {userId: normalizedId, nickname: normalizedName, createdAt: new Date().toISOString()};
        users.push(user);
        writeUsers(users);
        switchUser(normalizedId);
        return user;
    }

    function switchUser(userId) {
        const user = readUsers().find(item => item.userId === userId);
        if (!user) throw new Error("模拟用户不存在");
        localStorage.setItem(ACTIVE_KEY, user.userId);
        document.cookie = `username=${encodeURIComponent(user.userId)}; path=/; max-age=86400`;
        return user;
    }

    function remove(userId) {
        const users = readUsers().filter(user => user.userId !== userId);
        writeUsers(users);
        if (localStorage.getItem(ACTIVE_KEY) === userId) {
            localStorage.removeItem(ACTIVE_KEY);
            document.cookie = "username=; path=/; max-age=0";
        }
    }

    function safeReturnUrl(value, fallback) {
        const candidate = String(value || "").trim();
        if (!candidate || candidate.startsWith("//") || /^[a-z]+:/i.test(candidate)) {
            return fallback || "index.html";
        }
        return candidate;
    }

    function loginUrl(returnUrl) {
        return `login.html?returnUrl=${encodeURIComponent(safeReturnUrl(returnUrl, "index.html"))}`;
    }

    // 将旧版 username Cookie 迁移为第一个本地模拟身份。
    if (!readUsers().length) {
        const legacy = document.cookie.split(";")
            .map(item => item.trim())
            .find(item => item.startsWith("username="));
        const legacyId = legacy ? decodeURIComponent(legacy.slice("username=".length)) : "";
        if (/^[A-Za-z0-9_-]{2,24}$/.test(legacyId)) {
            writeUsers([{userId: legacyId, nickname: legacyId, createdAt: new Date().toISOString()}]);
            localStorage.setItem(ACTIVE_KEY, legacyId);
        }
    }

    window.StoreIdentity = {
        list: readUsers,
        current,
        register,
        switchUser,
        remove,
        safeReturnUrl,
        loginUrl
    };
})();
