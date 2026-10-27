/*
 * caja.js - ayudas de la pantalla de cobro (sin dependencias).
 *  - Búsqueda en el tarifario mientras se escribe (no se carga el catálogo entero).
 *  - Vuelto en vivo y montos rápidos (efectivo).
 *  - Cambiar la cantidad de una línea la guarda sola.
 *  - El botón "Cobrar" se desactiva al enviarse (evita un doble intento).
 */
(function () {
    "use strict";
    var form = document.getElementById("formCobro");
    var soles = function (n) {
        return n.toLocaleString("es-PE", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    };

    if (form) {
        var total = parseFloat(form.dataset.total || "0");
        var recibido = document.getElementById("montoRecibido");
        var vuelto = document.getElementById("vuelto");
        var mostrarVuelto = function () {
            if (!recibido || !vuelto) return;
            var texto = recibido.value.replace(",", ".").trim();
            var monto = parseFloat(texto);
            if (texto === "" || isNaN(monto)) { vuelto.textContent = ""; vuelto.className = "mt-2 vuelto"; return; }
            var diferencia = Math.round((monto - total) * 100) / 100;
            if (diferencia >= 0) {
                vuelto.textContent = "Vuelto: S/ " + soles(diferencia);
                vuelto.className = "mt-2 vuelto text-success font-weight-bold";
            } else {
                vuelto.textContent = "Faltan S/ " + soles(-diferencia);
                vuelto.className = "mt-2 vuelto text-danger";
            }
        };
        if (recibido) {
            recibido.addEventListener("input", mostrarVuelto);
            mostrarVuelto();
            Array.prototype.forEach.call(form.querySelectorAll("[data-monto]"), function (b) {
                b.addEventListener("click", function () { recibido.value = b.dataset.monto; mostrarVuelto(); recibido.focus(); });
            });
        }
        form.addEventListener("submit", function () {
            var boton = document.getElementById("botonCobrar");
            if (boton) {
                setTimeout(function () {       // después de que el navegador tome los datos del formulario
                    boton.disabled = true;
                    boton.innerHTML = '<i class="fas fa-spinner fa-spin mr-1"></i> Cobrando...';
                }, 0);
            }
        });
        var primerError = form.querySelector(".is-invalid");
        if (primerError) primerError.focus();
    }

    var formAgregar = document.getElementById("formAgregar");
    if (formAgregar) {
        var caja = document.getElementById("buscarItem");
        var lista = document.getElementById("resultadosItem");
        var oculto = document.getElementById("idItem");
        var elegido = document.getElementById("itemElegido");
        var boton = document.getElementById("botonAgregar");
        var temporizador = null, ultimo = "", activo = -1, opciones = [];

        var cerrar = function () { lista.hidden = true; caja.setAttribute("aria-expanded", "false"); activo = -1; };
        var elegir = function (it) {
            oculto.value = it.id;
            elegido.className = "small mt-1 text-success";
            caja.value = it.nombre;
            elegido.textContent = "Elegido: " + it.nombre + " · S/ " + it.precio + (it.stock !== null ? " · quedan " + it.stock : "");
            boton.disabled = false;
            cerrar();
            document.getElementById("cantidad").focus();
        };
        var marcar = function (i) {
            opciones.forEach(function (li, k) { li.classList.toggle("active", k === i); li.setAttribute("aria-selected", k === i ? "true" : "false"); });
            activo = i;
        };
        var pintar = function (items, texto) {
            lista.innerHTML = "";
            opciones = [];
            if (!items.length) {
                var vacio = document.createElement("li");
                vacio.className = "list-group-item small text-muted";
                vacio.textContent = "Nada coincide con «" + texto + "». Prueba con otra palabra o el código.";
                lista.appendChild(vacio);
            }
            items.forEach(function (it) {
                var li = document.createElement("li");
                var agotado = it.stock !== null && it.stock <= 0;
                li.className = "list-group-item list-group-item-action d-flex justify-content-between align-items-center" + (agotado ? " disabled text-muted" : "");
                li.setAttribute("role", "option");
                li.id = "itm" + it.id;
                var izq = document.createElement("div");
                var nombre = document.createElement("div"); nombre.textContent = it.nombre;
                var meta = document.createElement("small"); meta.className = "text-muted";
                meta.textContent = it.codigo + " · " + (it.tipo === "PRODUCTO" ? "Producto" : "Servicio") + " · " + it.categoria
                    + (it.stock !== null ? (agotado ? " · agotado" : " · quedan " + it.stock) : "");
                izq.appendChild(nombre); izq.appendChild(meta);
                var precio = document.createElement("strong"); precio.className = "monto text-nowrap ml-2"; precio.textContent = "S/ " + it.precio;
                li.appendChild(izq); li.appendChild(precio);
                if (!agotado) {
                    li.addEventListener("mousedown", function (e) { e.preventDefault(); elegir(it); });
                    li._item = it;
                    opciones.push(li);
                }
                lista.appendChild(li);
            });
            lista.hidden = false;
            caja.setAttribute("aria-expanded", "true");
            if (opciones.length) marcar(0);
        };
        var buscar = function () {
            var texto = caja.value.trim();
            if (texto.length < 2) { cerrar(); return; }
            if (texto === ultimo && !lista.hidden) return;
            ultimo = texto;
            fetch(formAgregar.dataset.urlItems + "&q=" + encodeURIComponent(texto), { headers: { "Accept": "application/json" }, credentials: "same-origin" })
                .then(function (r) { if (!r.ok) throw new Error(r.status); return r.json(); })
                .then(function (items) { if (caja.value.trim() === texto) pintar(items, texto); })
                .catch(function () {
                    lista.innerHTML = '<li class="list-group-item small text-danger">No se pudo buscar. Revisa tu conexión e inténtalo otra vez.</li>';
                    lista.hidden = false;
                });
        };
        caja.addEventListener("input", function () {
            oculto.value = ""; boton.disabled = true; elegido.textContent = "";
            clearTimeout(temporizador);
            temporizador = setTimeout(buscar, 250);
        });
        caja.addEventListener("keydown", function (e) {
            if (lista.hidden || !opciones.length) { if (e.key === "Enter" && !oculto.value) e.preventDefault(); return; }
            if (e.key === "ArrowDown") { e.preventDefault(); marcar(Math.min(activo + 1, opciones.length - 1)); }
            else if (e.key === "ArrowUp") { e.preventDefault(); marcar(Math.max(activo - 1, 0)); }
            else if (e.key === "Enter") { e.preventDefault(); if (activo >= 0) elegir(opciones[activo]._item); }
            else if (e.key === "Escape") { cerrar(); }
        });
        caja.addEventListener("blur", function () { setTimeout(cerrar, 150); });
        formAgregar.addEventListener("submit", function (e) {
            if (!oculto.value) { e.preventDefault(); caja.focus(); elegido.textContent = "Primero elige un servicio o producto de la lista."; elegido.className = "small mt-1 text-danger"; }
        });
    }

    // Cantidades: se guardan al cambiar (el botón queda para teclado y lectores de pantalla)
    Array.prototype.forEach.call(document.querySelectorAll("[data-auto-enviar]"), function (input) {
        input.addEventListener("change", function () {
            if (!input.checkValidity()) return;
            if (input.form.requestSubmit) input.form.requestSubmit(); else input.form.submit();
        });
    });
})();
