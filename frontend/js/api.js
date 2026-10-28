const Api = {

    getToken() {
        return localStorage.getItem("inv_token");
    },

    setSession(authResponse) {
        localStorage.setItem("inv_token", authResponse.token);
        localStorage.setItem("inv_email", authResponse.email);
        localStorage.setItem("inv_nombre", authResponse.nombre);
        localStorage.setItem("inv_rol", authResponse.rol);
    },

    clearSession() {
        localStorage.removeItem("inv_token");
        localStorage.removeItem("inv_email");
        localStorage.removeItem("inv_nombre");
        localStorage.removeItem("inv_rol");
    },

    getRol() {
        return localStorage.getItem("inv_rol");
    },

    getNombre() {
        return localStorage.getItem("inv_nombre");
    },

    isAdmin() {
        return this.getRol() === "ADMIN";
    },

    isAutenticado() {
        return !!this.getToken();
    },

    async request(path, { method = "GET", body = null, auth = true } = {}) {
        const headers = { "Content-Type": "application/json" };
        if (auth) {
            const token = this.getToken();
            if (token) headers["Authorization"] = "Bearer " + token;
        }

        let response;
        try {
            response = await fetch(API_BASE_URL + path, {
                method,
                headers,
                body: body ? JSON.stringify(body) : undefined,
            });
        } catch (networkError) {
            throw new Error(
                "No se pudo conectar con el servidor (" + API_BASE_URL + "). " +
                "Verificá que el backend Spring Boot esté corriendo."
            );
        }

        if (response.status === 401 && !path.startsWith("/auth/")) {
            this.clearSession();
            if (!location.pathname.endsWith("index.html") && !location.pathname.endsWith("/")) {
                window.location.href = "index.html";
            }
            throw new Error("Sesión expirada. Iniciá sesión nuevamente.");
        }

        if (response.status === 204) {
            return null;
        }

        const contentType = response.headers.get("content-type") || "";
        const data = contentType.includes("application/json") ? await response.json() : null;

        if (!response.ok) {
            const mensaje = (data && (data.mensaje || data.error)) || "Ocurrió un error inesperado";
            const detalles = data && data.detalles ? " (" + data.detalles.join(", ") + ")" : "";
            throw new Error(mensaje + detalles);
        }

        return data;
    },

    get(path) { return this.request(path, { method: "GET" }); },
    post(path, body) { return this.request(path, { method: "POST", body }); },
    put(path, body) { return this.request(path, { method: "PUT", body }); },
    patch(path, body) { return this.request(path, { method: "PATCH", body }); },
    del(path) { return this.request(path, { method: "DELETE" }); },
};
