class EditorItems {
    constructor({ tbody, totalEl, precioEditable, precioDe, opciones, avisarSinStock = false }) {
        this.tbody = tbody;
        this.totalEl = totalEl;
        this.precioEditable = precioEditable;
        this.precioDe = precioDe;
        this.opciones = opciones || ((productos) => productos
            .map((p) => `<option value="${p.id}">${escapeHtml(p.codigo)} - ${escapeHtml(p.nombre)}</option>`)
            .join(""));
        this.avisarSinStock = avisarSinStock;
        this.productos = [];
        this.filas = [];

        this.tbody.addEventListener("change", (e) => this.onCambio(e));
        this.tbody.addEventListener("input", (e) => this.onCambio(e));
        this.tbody.addEventListener("click", (e) => {
            const btn = e.target.closest("[data-quitar]");
            if (btn) this.quitar(Number(btn.dataset.quitar));
        });
    }

    setProductos(productos) {
        this.productos = productos;
        this.render();
    }

    producto(id) {
        return this.productos.find((p) => p.id === id);
    }

    agregar(productoId = null, cantidad = 1, precio = null) {
        const prod = productoId ? this.producto(productoId) : null;
        this.filas.push({
            productoId: prod ? prod.id : null,
            cantidad,
            precio: precio !== null ? precio : (prod ? Number(this.precioDe(prod)) : 0),
        });
        this.render();
    }

    quitar(indice) {
        this.filas.splice(indice, 1);
        this.render();
    }

    limpiar() {
        this.filas = [];
        this.render();
    }

    tieneProducto(productoId) {
        return this.filas.some((f) => f.productoId === productoId);
    }

    obtenerItems() {
        return this.filas.map((f) => ({ productoId: f.productoId, cantidad: f.cantidad, precio: f.precio }));
    }

    total() {
        return this.filas.reduce((suma, f) => suma + (f.cantidad || 0) * (f.precio || 0), 0);
    }

    onCambio(e) {
        const el = e.target;
        const indice = Number(el.dataset.fila);
        if (Number.isNaN(indice) || !this.filas[indice]) return;
        const fila = this.filas[indice];

        if (el.dataset.campo === "producto") {
            if (e.type !== "change") return;
            fila.productoId = el.value ? Number(el.value) : null;
            const prod = this.producto(fila.productoId);
            fila.precio = prod ? Number(this.precioDe(prod)) : 0;
            this.render();
            return;
        }

        if (el.dataset.campo === "cantidad") fila.cantidad = parseInt(el.value, 10) || 0;
        if (el.dataset.campo === "precio") fila.precio = parseFloat(el.value) || 0;

        this.tbody.querySelector(`[data-subtotal="${indice}"]`).textContent =
            formatearMoneda(fila.cantidad * fila.precio);
        this.marcarSinStock(indice);
        this.renderTotal();
    }

    marcarSinStock(indice) {
        if (!this.avisarSinStock) return;
        const fila = this.filas[indice];
        const prod = this.producto(fila.productoId);
        const aviso = this.tbody.querySelector(`[data-aviso="${indice}"]`);
        const falta = prod && fila.cantidad > prod.stock;
        aviso.textContent = falta ? `Solo hay ${prod.stock} en stock` : "";
    }

    render() {
        if (this.filas.length === 0) {
            this.tbody.innerHTML = `<tr><td colspan="5" class="empty-state">Agregá al menos un producto</td></tr>`;
            this.renderTotal();
            return;
        }

        const opciones = this.opciones(this.productos);
        this.tbody.innerHTML = this.filas.map((f, i) => `
            <tr>
                <td>
                    <select data-fila="${i}" data-campo="producto" required>
                        <option value="">-- Elegí un producto --</option>${opciones}
                    </select>
                    <small class="aviso-stock" data-aviso="${i}"></small>
                </td>
                <td style="width:110px;">
                    <input type="number" data-fila="${i}" data-campo="cantidad" min="1" max="100000" step="1" value="${f.cantidad}" required>
                </td>
                <td style="width:150px;">
                    ${this.precioEditable
                        ? `<input type="number" data-fila="${i}" data-campo="precio" min="0" max="99999999.99" step="0.01" value="${f.precio}" required>`
                        : formatearMoneda(f.precio)}
                </td>
                <td data-subtotal="${i}">${formatearMoneda(f.cantidad * f.precio)}</td>
                <td class="text-right"><button type="button" class="secondary" data-quitar="${i}" title="Quitar">✕</button></td>
            </tr>
        `).join("");

        this.filas.forEach((f, i) => {
            this.tbody.querySelector(`select[data-fila="${i}"]`).value = f.productoId || "";
            this.marcarSinStock(i);
        });
        this.renderTotal();
    }

    renderTotal() {
        this.totalEl.textContent = formatearMoneda(this.total());
    }
}

function tablaDetalleItems(items, etiquetaPrecio) {
    return `
        <table>
            <thead><tr><th>Producto</th><th>Cant.</th><th>${etiquetaPrecio}</th><th>Subtotal</th></tr></thead>
            <tbody>
                ${items.map((i) => `
                    <tr>
                        <td>${escapeHtml(i.productoCodigo)} - ${escapeHtml(i.productoNombre)}</td>
                        <td>${i.cantidad}</td>
                        <td>${formatearMoneda(i.precioUnitario)}</td>
                        <td>${formatearMoneda(i.subtotal)}</td>
                    </tr>`).join("")}
            </tbody>
        </table>`;
}
