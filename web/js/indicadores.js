/*
 * indicadores.js - indicadores del inicio del administrador bajo demanda.
 * Nada se consulta al abrir la página: el indicador se pide al servidor (JSON) solo al elegirlo,
 * y Chart.js se descarga la primera vez que hace falta.
 */
(function () {
    "use strict";
    var script = document.getElementById("scriptIndicadores");
    var contenedor = document.getElementById("indicadores");
    if (!script || !contenedor) return;

    var URL_DATOS = script.dataset.url;
    var URL_CHART = "https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.js";
    var CLAVE = "clinica.indicadores.periodo";
    var resultado = document.getElementById("resultadoIndicador");
    var estado = document.getElementById("estadoIndicador");
    var lienzo = document.getElementById("graficoIndicador");
    var grafico = null, ultimo = null, actual = null, cargandoChart = null;
    var reducir = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;

    var PALETA = [["#1e5aa8", "#6ea8ff"], ["#c77700", "#f5b041"], ["#0f8b8d", "#4fd1c5"], ["#c2185b", "#f48fb1"],
                  ["#6a4fb3", "#b39ddb"], ["#2e7d32", "#81c784"], ["#607d8b", "#b0bec5"], ["#8d6e63", "#d7ccc8"]];

    var periodo = "30";
    try { periodo = localStorage.getItem(CLAVE) || "30"; } catch (e) { periodo = "30"; }

    function oscuro() { return document.body.classList.contains("dark-mode"); }
    function color(i) { return PALETA[i % PALETA.length][oscuro() ? 1 : 0]; }
    function formatear(v, formato) {
        var n = Number(v) || 0;
        return formato === "soles"
            ? "S/ " + n.toLocaleString("es-PE", { minimumFractionDigits: 2, maximumFractionDigits: 2 })
            : n.toLocaleString("es-PE", { maximumFractionDigits: 0 });
    }

    function marcarPeriodo() {
        contenedor.querySelectorAll("[data-periodo]").forEach(function (b) {
            var si = b.dataset.periodo === periodo;
            b.classList.toggle("active", si);
            b.setAttribute("aria-checked", si ? "true" : "false");
        });
    }

    function cargarChart() {
        if (window.Chart) return Promise.resolve();
        if (cargandoChart) return cargandoChart;
        cargandoChart = new Promise(function (ok, falla) {
            var s = document.createElement("script");
            s.src = URL_CHART;
            s.onload = function () { ok(); };
            s.onerror = function () { cargandoChart = null; falla(new Error("chart")); };
            document.head.appendChild(s);
        });
        return cargandoChart;
    }

    function mensaje(html, clase) {
        estado.innerHTML = html ? '<div class="alert ' + clase + ' py-2 mb-2">' + html + "</div>" : "";
    }

    function texto(etiqueta, valor) {
        var el = document.createElement(etiqueta);
        el.textContent = valor;
        return el;
    }

    function pintarResumen(d) {
        var caja = document.getElementById("resumenIndicador");
        caja.innerHTML = "";
        d.resumen.forEach(function (r) {
            var col = document.createElement("div");
            col.className = "col-6 col-md-3 mb-2";
            var tarjeta = document.createElement("div");
            tarjeta.className = "dato-indicador";
            tarjeta.appendChild(texto("small", r.etiqueta));
            tarjeta.appendChild(texto("strong", r.valor));
            if (r.detalle) tarjeta.appendChild(texto("span", r.detalle));
            col.appendChild(tarjeta);
            caja.appendChild(col);
        });
    }

    function pintarTabla(d) {
        var tabla = document.createElement("table");
        tabla.className = "table table-sm mb-0";
        var cab = document.createElement("tr");
        cab.appendChild(texto("th", ""));
        d.series.forEach(function (s) { var th = texto("th", s.nombre); th.className = "text-right"; cab.appendChild(th); });
        var thead = document.createElement("thead"); thead.appendChild(cab); tabla.appendChild(thead);
        var cuerpo = document.createElement("tbody");
        d.etiquetas.forEach(function (et, i) {
            var fila = document.createElement("tr");
            fila.appendChild(texto("td", et));
            d.series.forEach(function (s) { var td = texto("td", formatear(s.valores[i], d.formato)); td.className = "text-right monto"; fila.appendChild(td); });
            cuerpo.appendChild(fila);
        });
        tabla.appendChild(cuerpo);
        var caja = document.getElementById("tablaIndicador");
        caja.innerHTML = "";
        caja.appendChild(tabla);
    }

    function opcionesGrafico(d) {
        var texto = oscuro() ? "#cbd5e1" : "#475569";
        var rejilla = oscuro() ? "rgba(148,163,184,.18)" : "rgba(15,23,42,.08)";
        var horizontal = d.tipo === "barh";
        var eje = { beginAtZero: true, ticks: { color: texto, precision: d.formato === "soles" ? undefined : 0,
                    callback: function (v) { return d.formato === "soles" ? "S/ " + Number(v).toLocaleString("es-PE") : Number(v).toLocaleString("es-PE"); } },
                    grid: { color: rejilla }, border: { display: false } };
        var categoria = { ticks: { color: texto, autoSkip: true, maxRotation: 0 }, grid: { display: false }, border: { color: rejilla } };
        var base = {
            responsive: true, maintainAspectRatio: false,
            animation: reducir ? false : { duration: 500 },
            plugins: {
                legend: { display: d.series.length > 1 || d.tipo === "doughnut", position: d.tipo === "doughnut" ? "right" : "top",
                          labels: { color: texto, boxWidth: 12 } },
                tooltip: { callbacks: { label: function (c) {
                    var v = c.parsed && typeof c.parsed === "object" ? (horizontal ? c.parsed.x : c.parsed.y) : c.parsed;
                    return (c.dataset.label ? c.dataset.label + ": " : "") + formatear(v, d.formato);
                } } }
            }
        };
        if (d.tipo !== "doughnut") {
            base.indexAxis = horizontal ? "y" : "x";
            base.scales = horizontal ? { x: eje, y: categoria } : { x: categoria, y: eje };
        }
        return base;
    }

    function dibujar(d) {
        if (grafico && grafico.destroy) grafico.destroy();
        grafico = null;
        var dona = d.tipo === "doughnut";
        var tipo = d.tipo === "barh" ? "bar" : d.tipo;
        var datasets = d.series.map(function (s, i) {
            var ds = { label: s.nombre, data: s.valores };
            if (dona) {
                ds.backgroundColor = s.valores.map(function (_, k) { return color(k); });
                ds.borderColor = oscuro() ? "#1f2d3d" : "#ffffff";
            } else if (tipo === "line") {
                ds.borderColor = color(i); ds.backgroundColor = color(i) + "22"; ds.fill = true; ds.tension = 0.25;
                ds.pointRadius = d.etiquetas.length > 31 ? 0 : 3;
            } else {
                ds.backgroundColor = color(i); ds.borderRadius = 4; ds.maxBarThickness = 28;
            }
            return ds;
        });
        var alto = d.tipo === "barh" ? Math.max(220, d.etiquetas.length * (d.series.length > 1 ? 34 : 26) + 40) : 280;
        lienzo.parentNode.style.height = alto + "px";
        lienzo.setAttribute("aria-label", d.titulo + ", " + d.periodo);
        grafico = new window.Chart(lienzo, { type: tipo, data: { labels: d.etiquetas, datasets: datasets }, options: opcionesGrafico(d) });
    }

    function mostrar(d) {
        actual = d;
        document.getElementById("tituloIndicador").textContent = d.titulo;
        document.getElementById("periodoIndicador").textContent = d.periodo;
        document.getElementById("notaIndicador").textContent = d.nota || "";
        var enlace = document.getElementById("enlaceIndicador");
        enlace.hidden = !d.enlace;
        if (d.enlace) enlace.href = d.enlace;
        pintarResumen(d);
        pintarTabla(d);
        var vacio = d.vacio || !d.etiquetas.length;
        lienzo.parentNode.hidden = vacio;
        document.querySelector(".tabla-indicador").hidden = vacio;
        if (vacio) {
            mensaje('<i class="fas fa-inbox mr-1"></i>No hay datos en este periodo. Prueba con un periodo más largo.', "alert-light border");
            return;
        }
        mensaje("", "");
        cargarChart().then(function () { dibujar(d); }, function () {
            lienzo.parentNode.hidden = true;
            document.querySelector(".tabla-indicador").open = true;
            mensaje("No se pudo cargar el gráfico; te mostramos los datos en tabla.", "alert-warning");
        });
    }

    function pedir(codigo) {
        ultimo = codigo;
        contenedor.querySelectorAll("[data-indicador]").forEach(function (b) {
            var si = b.dataset.indicador === codigo;
            b.classList.toggle("activo", si);
            b.setAttribute("aria-pressed", si ? "true" : "false");
        });
        resultado.hidden = false;
        document.getElementById("resumenIndicador").innerHTML = "";
        mensaje('<i class="fas fa-spinner fa-spin mr-1"></i>Calculando…', "alert-light border");
        fetch(URL_DATOS + "?codigo=" + encodeURIComponent(codigo) + "&periodo=" + encodeURIComponent(periodo),
              { headers: { "Accept": "application/json" }, credentials: "same-origin" })
            .then(function (r) { return r.json().then(function (j) { if (!r.ok) throw new Error(j.error || r.status); return j; }); })
            .then(function (d) { if (ultimo === codigo) mostrar(d); })
            .catch(function (e) {
                mensaje(texto("span", (e && e.message && isNaN(e.message)) ? e.message : "No pudimos calcular el indicador.").outerHTML
                        + ' <button type="button" class="btn btn-link btn-sm p-0 ml-1" id="reintentarIndicador">Reintentar</button>', "alert-danger");
                var b = document.getElementById("reintentarIndicador");
                if (b) b.addEventListener("click", function () { pedir(codigo); });
            });
        if (resultado.scrollIntoView && window.innerWidth < 992) resultado.scrollIntoView({ behavior: reducir ? "auto" : "smooth", block: "start" });
    }

    contenedor.querySelectorAll("[data-indicador]").forEach(function (b) {
        b.addEventListener("click", function () { pedir(b.dataset.indicador); });
    });
    contenedor.querySelectorAll("[data-periodo]").forEach(function (b) {
        b.addEventListener("click", function () {
            periodo = b.dataset.periodo;
            try { localStorage.setItem(CLAVE, periodo); } catch (e) { /* sin almacenamiento */ }
            marcarPeriodo();
            if (ultimo) pedir(ultimo);
        });
    });
    document.addEventListener("temaCambiado", function () { if (actual && grafico) dibujar(actual); });
    marcarPeriodo();
})();
