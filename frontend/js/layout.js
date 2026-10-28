const NAV_ITEMS = [
    { key: "dashboard", label: "Panel", href: "dashboard.html", icon: "📊" },
    { key: "productos", label: "Productos", href: "productos.html", icon: "📦" },
    { key: "categorias", label: "Categorías", href: "categorias.html", icon: "🏷️" },
    { key: "proveedores", label: "Proveedores", href: "proveedores.html", icon: "🚚" },
    { key: "ventas", label: "Ventas", href: "ventas.html", icon: "🧾" },
    { key: "ordenes", label: "Órdenes de compra", href: "ordenes-compra.html", icon: "📝" },
    { key: "movimientos", label: "Movimientos de stock", href: "movimientos.html", icon: "🔄" },
    { key: "reportes", label: "Reportes", href: "reportes.html", icon: "📈" },
    { key: "usuarios", label: "Usuarios", href: "usuarios.html", icon: "👤", adminOnly: true },
];

function requireAuth() {
    if (!Api.isAutenticado()) {
        window.location.href = "index.html";
        return false;
    }
    return true;
}

function initLayout(activeKey) {
    if (!requireAuth()) return;

    const sidebar = document.getElementById("sidebar");
    if (sidebar) {
        const isAdmin = Api.isAdmin();
        const navHtml = NAV_ITEMS
            .filter((item) => !item.adminOnly || isAdmin)
            .map(
                (item) => `<a href="${item.href}" data-nav="${item.key}" class="${item.key === activeKey ? "active" : ""}">${item.icon} ${item.label}</a>`
            )
            .join("");

        sidebar.innerHTML = `
            <h2>📋 Inventario<span>App</span></h2>
            <nav>${navHtml}</nav>
            <div class="sidebar-footer">
                <div><strong>${escapeHtml(Api.getNombre() || "")}</strong></div>
                <div>${Api.getRol() === "ADMIN" ? "Administrador" : "Empleado"}</div>
                <button id="btn-logout" class="secondary">Cerrar sesión</button>
            </div>
        `;

        document.getElementById("btn-logout").addEventListener("click", () => {
            Api.clearSession();
            window.location.href = "index.html";
        });

        actualizarAlertaStock();
    }
}

async function actualizarAlertaStock() {
    const link = document.querySelector('[data-nav="dashboard"]');
    if (!link) return;
    try {
        const stockBajo = await Api.get("/productos/stock-bajo");
        let badge = link.querySelector(".nav-badge");
        if (stockBajo.length === 0) {
            if (badge) badge.remove();
            return;
        }
        if (!badge) {
            badge = document.createElement("span");
            badge.className = "nav-badge";
            link.appendChild(badge);
        }
        const sinStock = stockBajo.filter((p) => p.stock === 0).length;
        badge.textContent = stockBajo.length;
        badge.classList.toggle("critico", sinStock > 0);
        badge.title = `${stockBajo.length} producto(s) con stock bajo` + (sinStock ? `, ${sinStock} sin stock` : "");
    } catch (err) {
    }
}

function abrirModal(titulo, contenidoHtml) {
    cerrarModal();
    const overlay = document.createElement("div");
    overlay.className = "modal-overlay";
    overlay.id = "modal-overlay";
    overlay.innerHTML = `
        <div class="modal wide" role="dialog" aria-modal="true">
            <div class="modal-header">
                <h3>${escapeHtml(titulo)}</h3>
                <button type="button" class="secondary" id="btn-cerrar-modal" title="Cerrar">✕</button>
            </div>
            ${contenidoHtml}
        </div>`;
    overlay.addEventListener("click", (e) => { if (e.target === overlay) cerrarModal(); });
    document.body.appendChild(overlay);
    document.getElementById("btn-cerrar-modal").addEventListener("click", cerrarModal);
    document.addEventListener("keydown", cerrarConEscape);
}

function cerrarModal() {
    const overlay = document.getElementById("modal-overlay");
    if (overlay) overlay.remove();
    document.removeEventListener("keydown", cerrarConEscape);
}

function cerrarConEscape(e) {
    if (e.key === "Escape") cerrarModal();
}

function badgeEstado(estado) {
    const clases = { CONFIRMADA: "ok", RECIBIDA: "ok", PENDIENTE: "warn", ANULADA: "danger", CANCELADA: "neutral" };
    return `<span class="badge ${clases[estado] || "neutral"}">${estado}</span>`;
}

function mostrarError(contenedorId, mensaje) {
    const el = document.getElementById(contenedorId);
    if (!el) return;
    el.textContent = mensaje;
    el.className = "alert error";
    el.classList.remove("hidden");
}

function mostrarExito(contenedorId, mensaje) {
    const el = document.getElementById(contenedorId);
    if (!el) return;
    el.textContent = mensaje;
    el.className = "alert success";
    el.classList.remove("hidden");
    setTimeout(() => el.classList.add("hidden"), 3500);
}

function ocultarAlerta(contenedorId) {
    const el = document.getElementById(contenedorId);
    if (el) el.classList.add("hidden");
}

function formatearMoneda(valor) {
    return new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS" }).format(valor || 0);
}

function formatearFecha(fechaIso) {
    if (!fechaIso) return "-";
    const fecha = new Date(fechaIso);
    return fecha.toLocaleString("es-AR");
}

function escapeHtml(texto) {
    if (texto === null || texto === undefined) return "";
    return String(texto)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;");
}
