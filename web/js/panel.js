/*
 * panel.js - comportamiento del dashboard (requiere jQuery + Bootstrap 4).
 *  - Ventana de confirmación para acciones (desactivar, bloquear...).
 *  - Mostrar/ocultar contraseña y requisitos en vivo.
 *  - Descripción del rol elegido y solo-números en DNI/celular.
 */
$(function () {

    // =====================================================================
    //  UNA CUENTA POR NAVEGADOR: todas las pestañas comparten la misma sesión.
    //  Si en otra pestaña se cierra sesión o se entra con otra cuenta, esta
    //  pestaña lo detecta y no deja trabajar con datos de la cuenta anterior.
    // =====================================================================
    const miCuenta = document.body.dataset.usuario;
    // Cada formulario de acción indica a qué cuenta pertenece esta pestaña (el servidor lo comprueba)
    document.querySelectorAll("form").forEach(function (form) {
        if ((form.getAttribute("method") || "").toUpperCase() !== "POST" || form.elements._cuenta) return;
        const oculto = document.createElement("input");
        oculto.type = "hidden"; oculto.name = "_cuenta"; oculto.value = miCuenta;
        form.appendChild(oculto);
    });
    function cuentaActiva() { try { return localStorage.getItem("clinica.sesion.activa"); } catch (e) { return null; } }
    function avisoPestana(sinSesion) {
        const anterior = document.getElementById("avisoPestana");
        if (anterior) anterior.remove();      // se actualiza (ej. primero se cerró sesión y luego entró otra cuenta)
        const aviso = document.createElement("div");
        aviso.id = "avisoPestana";
        aviso.className = "aviso-pestana";
        aviso.setAttribute("role", "alertdialog");
        aviso.setAttribute("aria-modal", "true");
        aviso.innerHTML = '<div class="card shadow-lg"><div class="card-body text-center p-4">'
            + '<i class="fas fa-user-lock fa-2x text-warning mb-3" aria-hidden="true"></i>'
            + '<h5>Esta pestaña quedó desactualizada</h5>'
            + '<p class="text-muted mb-3"></p>'
            + '<a class="btn btn-primary"></a></div></div>';
        aviso.querySelector("p").textContent = sinSesion
            ? "Se cerró la sesión en otra pestaña de este navegador."
            : "En otra pestaña de este navegador se inició sesión con otra cuenta. Un navegador mantiene una sola cuenta "
              + "abierta a la vez: para usar dos cuentas al mismo tiempo, abre la otra en una ventana privada o en otro navegador.";
        const boton = aviso.querySelector("a");
        boton.textContent = sinSesion ? "Iniciar sesión" : "Ir a la cuenta abierta";
        boton.href = cuerpoCtx() + (sinSesion ? "/jsp/login.jsp" : "/panel");
        document.body.appendChild(aviso);
        boton.focus();
    }
    function cuerpoCtx() { return document.body.dataset.ctx || ""; }
    function revisarPestana() {
        const activa = cuentaActiva();
        if (activa !== null && activa !== miCuenta) avisoPestana(activa === "0");
    }
    window.addEventListener("storage", function (e) { if (e.key === "clinica.sesion.activa") revisarPestana(); });
    window.addEventListener("focus", revisarPestana);
    document.addEventListener("visibilitychange", function () { if (!document.hidden) revisarPestana(); });

    // ---------- Tooltips ----------
    $('[data-toggle="tooltip"]').tooltip();

    // ---------- Confirmación de acciones ----------
    const datos = document.getElementById("datosConfirmar");
    $(document).on("click", "[data-confirmar]", function () {
        const b = this.dataset;
        const form = document.getElementById("formConfirmar");
        form.action = datos ? datos.dataset.action : window.location.pathname;
        form.elements.accion.value = b.confirmar;
        form.elements.id.value = b.id;
        form.elements.regreso.value = datos ? datos.dataset.regreso : "";

        document.getElementById("modalConfirmarTitulo").textContent = b.titulo || "Confirmar";

        // El mensaje se arma con textContent (nunca innerHTML) para evitar inyección de código
        const cuerpo = document.getElementById("modalConfirmarMensaje");
        cuerpo.textContent = "";
        const partes = (b.mensaje || "¿Confirmas esta acción?").split("{usuario}");
        cuerpo.appendChild(document.createTextNode(partes[0]));
        if (partes.length > 1) {
            const fuerte = document.createElement("strong");
            // data-nombre: pacientes, proveedores, productos (sin "@"); data-usuario: cuentas de acceso
            fuerte.textContent = b.nombre ? b.nombre : "@" + (b.usuario || "");
            cuerpo.appendChild(fuerte);
            cuerpo.appendChild(document.createTextNode(partes[1]));
        }

        const boton = document.getElementById("modalConfirmarBoton");
        boton.className = "btn " + (b.color || "btn-primary");
        boton.textContent = b.boton || "Confirmar";
        boton.disabled = false;
        $("#modalConfirmar").modal("show");
    });
    // Evita doble envío
    $("#formConfirmar").on("submit", function () {
        const boton = document.getElementById("modalConfirmarBoton");
        boton.disabled = true;
        boton.textContent = "Procesando...";
    });

    // ---------- Mostrar / ocultar contraseña ----------
    $(document).on("click", "[data-toggle-password]", function () {
        const input = document.getElementById(this.dataset.togglePassword);
        const oculto = input.type === "password";
        input.type = oculto ? "text" : "password";
        this.querySelector("i").className = oculto ? "fas fa-eye-slash" : "fas fa-eye";
        this.setAttribute("aria-label", oculto ? "Ocultar contraseña" : "Mostrar contraseña");
    });

    // ---------- Requisitos de contraseña en vivo ----------
    const password = document.getElementById("password");
    if (password) {
        const reglas = {
            longitud:  function (v) { return v.length >= 8; },
            mayuscula: function (v) { return /[A-Z]/.test(v); },
            minuscula: function (v) { return /[a-z]/.test(v); },
            numero:    function (v) { return /\d/.test(v); }
        };
        password.addEventListener("input", function () {
            document.querySelectorAll("[data-regla]").forEach(function (li) {
                li.classList.toggle("cumple", reglas[li.dataset.regla](password.value));
            });
        });
    }

    // ---------- Solo números ----------
    document.querySelectorAll("[data-solo-numeros]").forEach(function (input) {
        input.addEventListener("input", function () { input.value = input.value.replace(/\D/g, ""); });
    });

    // ---------- Descripción del rol elegido (reconocer antes que recordar) ----------
    const rol = document.getElementById("idRol");
    const descripcion = document.getElementById("descripcionRol");
    if (rol && descripcion) {
        const mostrar = function () {
            const op = rol.options[rol.selectedIndex];
            descripcion.textContent = op ? (op.dataset.descripcion || "") : "";
        };
        rol.addEventListener("change", mostrar);
        mostrar();
    }

    // ---------- Formularios CRUD: evitar doble envío y llevar al primer error ----------
    document.querySelectorAll("#formUsuario, form[data-form-crud]").forEach(function (form) {
        const primerError = form.querySelector(".is-invalid");
        if (primerError) primerError.focus();
        form.addEventListener("submit", function () {
            const boton = form.querySelector("button[type=submit]");
            boton.disabled = true;
            boton.innerHTML = '<i class="fas fa-spinner fa-spin mr-1"></i> Guardando...';
        });
    });

    // ---------- Campos que dependen de otro (ej. N.º de afiliación solo si no es "Particular") ----------
    // <div data-mostrar-si="idControl" data-valores="EPS,SEGURO">: se muestra si el valor (o data-extra
    // de la opción elegida) está en la lista. Oculto = deshabilitado, así no se envía.
    function valorDe(control) {
        if (control.tagName === "SELECT") {
            const op = control.options[control.selectedIndex];
            return op ? (op.dataset.extra || op.value) : "";
        }
        const marcado = control.querySelector("input:checked");
        return marcado ? marcado.value : (control.value || "");
    }
    document.querySelectorAll("[data-mostrar-si]").forEach(function (bloque) {
        const control = document.getElementById(bloque.dataset.mostrarSi);
        if (!control) return;
        const valores = bloque.dataset.valores.split(",");
        const actualizar = function () {
            const ver = valores.indexOf(valorDe(control)) >= 0;
            bloque.hidden = !ver;
            bloque.querySelectorAll("input, select, textarea").forEach(function (c) { c.disabled = !ver; });
        };
        $(control).on("change", actualizar);   // jQuery: también recibe el cambio de los botones de Bootstrap
        actualizar();
    });

    // ---------- Listas que se filtran según otro control (categorías según Servicio/Producto) ----------
    document.querySelectorAll("select[data-filtrar-por]").forEach(function (lista) {
        const control = document.getElementById(lista.dataset.filtrarPor);
        if (!control) return;
        const actualizar = function () {
            const v = valorDe(control);
            let primeraVisible = null;
            Array.prototype.forEach.call(lista.options, function (op) {
                const visible = !op.dataset.extra || op.dataset.extra === v;
                op.hidden = !visible; op.disabled = !visible;
                if (visible && op.value && !primeraVisible) primeraVisible = op;
            });
            const elegida = lista.options[lista.selectedIndex];
            if (!elegida || elegida.disabled) lista.value = primeraVisible ? primeraVisible.value : "";
        };
        $(control).on("change", actualizar);   // jQuery: también recibe el cambio de los botones de Bootstrap
        actualizar();
    });

    // ---------- Código sugerido según el tipo (SRV- servicio / PRD- producto) ----------
    const campoCodigo = document.querySelector("[data-codigo-servicio]");
    const grupoTipo = document.getElementById("tipo");
    if (campoCodigo && grupoTipo && campoCodigo.dataset.codigoServicio) {
        $(grupoTipo).on("change", function () {
            const sugeridos = [campoCodigo.dataset.codigoServicio, campoCodigo.dataset.codigoProducto, ""];
            // Solo se cambia si el usuario no escribió un código propio
            if (sugeridos.indexOf(campoCodigo.value.trim().toUpperCase()) >= 0) {
                campoCodigo.value = valorDe(grupoTipo) === "PRODUCTO" ? campoCodigo.dataset.codigoProducto : campoCodigo.dataset.codigoServicio;
            }
        });
    }

    // ---------- Edad calculada al escribir la fecha de nacimiento ----------
    document.querySelectorAll("[data-edad-de]").forEach(function (salida) {
        const campo = document.getElementById(salida.dataset.edadDe);
        const actualizar = function () {
            if (!campo.value) { salida.textContent = ""; return; }
            const n = new Date(campo.value + "T00:00:00"), hoy = new Date();
            let edad = hoy.getFullYear() - n.getFullYear();
            if (hoy.getMonth() < n.getMonth() || (hoy.getMonth() === n.getMonth() && hoy.getDate() < n.getDate())) edad--;
            salida.textContent = edad >= 0 && edad <= 120 ? edad + (edad === 1 ? " año" : " años") : "";
        };
        campo.addEventListener("input", actualizar);
        actualizar();
    });

    // =====================================================================
    //  TEMA CLARO / OSCURO  (preferencia PERSONAL: se guarda en la cuenta)
    // =====================================================================
    const cuerpo = document.body;
    // La versión anterior guardaba el tema en el navegador (lo compartían todas las cuentas): se borra
    try { localStorage.removeItem("clinica.tema"); } catch (e) { /* sin almacenamiento */ }
    // Si el navegador muestra la página desde su memoria (botón Atrás), se recarga para traer el tema de la cuenta actual
    window.addEventListener("pageshow", function (ev) { if (ev.persisted) window.location.reload(); });
    const consultaOscuro = window.matchMedia ? window.matchMedia("(prefers-color-scheme: dark)") : null;
    function esOscuro(tema) { return tema === "oscuro" || (tema === "sistema" && consultaOscuro && consultaOscuro.matches); }
    function aplicarTema(tema, guardar) {
        cuerpo.dataset.tema = tema;
        const oscuro = esOscuro(tema);
        cuerpo.classList.toggle("dark-mode", oscuro);
        const btn = document.getElementById("btnTema");
        if (btn) {
            btn.innerHTML = oscuro ? '<i class="fas fa-sun"></i>' : '<i class="fas fa-moon"></i>';
            const texto = oscuro ? "Cambiar a modo claro" : "Cambiar a modo oscuro";
            btn.title = texto; btn.setAttribute("aria-label", texto);
        }
        document.querySelectorAll('input[name="tema"]').forEach(function (r) { r.checked = r.value === tema; });
        document.dispatchEvent(new CustomEvent("temaCambiado"));
        if (guardar) {
            // Se guarda en la cuenta del usuario, en segundo plano (sin recargar la página)
            fetch(cuerpo.dataset.ctx + "/perfil", {
                method: "POST",
                headers: { "Content-Type": "application/x-www-form-urlencoded", "X-Requested-With": "fetch" },
                body: "accion=tema&tema=" + encodeURIComponent(tema) + "&_cuenta=" + encodeURIComponent(miCuenta)
            }).then(function (r) {
                if (r.status === 409) revisarPestana();                                  // pestaña de otra cuenta
                if (r.status === 401) {
                    window.location.href = cuerpo.dataset.ctx + "/jsp/login.jsp?error=" + (r.headers.get("X-Motivo") || "sesion");
                }
            }).catch(function () { /* sin conexión: se mantiene solo en esta página */ });
        }
    }
    aplicarTema(cuerpo.dataset.tema || "claro", false);
    $("#btnTema").on("click", function (e) {
        e.preventDefault();
        aplicarTema(cuerpo.classList.contains("dark-mode") ? "claro" : "oscuro", true);
    });
    $('input[name="tema"]').on("change", function () { aplicarTema(this.value, true); });
    if (consultaOscuro && consultaOscuro.addEventListener) {
        consultaOscuro.addEventListener("change", function () { if (cuerpo.dataset.tema === "sistema") aplicarTema("sistema", false); });
    }

    // ---------- Menú lateral: cada usuario recuerda si lo deja contraído o expandido ----------
    $(document).on("collapsed.lte.pushmenu shown.lte.pushmenu", function (e) {
        try {
            localStorage.setItem("clinica.menu." + cuerpo.dataset.usuario,
                e.type === "collapsed" ? "sidebar-collapse" : "sidebar-open");
        } catch (err) { /* sin almacenamiento */ }
    });

    // ---------- Fecha de hoy en la barra superior ----------
    const fechaHoy = document.getElementById("fechaHoy");
    if (fechaHoy) {
        const texto = new Date().toLocaleDateString("es-PE", { weekday: "long", day: "numeric", month: "long" });
        fechaHoy.innerHTML = '<i class="far fa-calendar-alt mr-1"></i>' + texto.charAt(0).toUpperCase() + texto.slice(1);
    }

    // ---------- Mensajes de éxito: se cierran solos a los 6 s ----------
    document.querySelectorAll('.alerta-flash[data-autocerrar="true"]').forEach(function (a) {
        setTimeout(function () { $(a).alert("close"); }, 6000);
    });

    // ---------- Contadores animados en las tarjetas ----------
    const reducirMovimiento = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    document.querySelectorAll("[data-contador]").forEach(function (el) {
        const fin = parseInt(el.dataset.contador, 10) || 0;
        if (reducirMovimiento || fin === 0) return;
        const inicio = performance.now(), duracion = 900;
        (function paso(t) {
            const avance = Math.min((t - inicio) / duracion, 1);
            el.textContent = Math.round(fin * (1 - Math.pow(1 - avance, 3)));
            if (avance < 1) requestAnimationFrame(paso);
        })(inicio);
    });

    // ---------- Gráficos del inicio (Chart.js) ----------
    const datosGraficos = document.getElementById("datosGraficos");
    if (datosGraficos && window.Chart) {
        const datos = JSON.parse(datosGraficos.textContent);
        const graficos = [];
        function colores() {
            const oscuro = document.body.classList.contains("dark-mode");
            return {
                barra: oscuro ? "#5b9bff" : "#1e5aa8",
                texto: oscuro ? "#cbd5e1" : "#475569",
                rejilla: oscuro ? "rgba(148,163,184,.18)" : "rgba(15,23,42,.08)"
            };
        }
        function opciones(horizontal) {
            const c = colores();
            const ejeValor = { beginAtZero: true, ticks: { precision: 0, color: c.texto }, grid: { color: c.rejilla }, border: { display: false } };
            const ejeCategoria = { ticks: { color: c.texto }, grid: { display: false }, border: { color: c.rejilla } };
            return {
                indexAxis: horizontal ? "y" : "x",
                responsive: true, maintainAspectRatio: false,
                animation: reducirMovimiento ? false : { duration: 700 },
                plugins: { legend: { display: false }, tooltip: { displayColors: false, padding: 10 } },
                scales: horizontal ? { x: ejeValor, y: ejeCategoria } : { x: ejeCategoria, y: ejeValor }
            };
        }
        function crear(id, etiquetas, valores, horizontal, nombre) {
            const lienzo = document.getElementById(id);
            if (!lienzo) return;
            graficos.push({ horizontal: horizontal, grafico: new Chart(lienzo, {
                type: "bar",
                data: { labels: etiquetas, datasets: [{ label: nombre, data: valores, backgroundColor: colores().barra,
                        borderRadius: 4, borderSkipped: "start", maxBarThickness: horizontal ? 22 : 28 }] },
                options: opciones(horizontal)
            }) });
        }
        crear("graficoDias", datos.dias.etiquetas, datos.dias.valores, false, "Usuarios nuevos");
        crear("graficoRoles", datos.roles.etiquetas, datos.roles.valores, true, "Usuarios activos");
        document.addEventListener("temaCambiado", function () {
            graficos.forEach(function (g) {
                g.grafico.data.datasets[0].backgroundColor = colores().barra;
                g.grafico.options = opciones(g.horizontal);
                g.grafico.update();
            });
        });
    }

    // ---------- Pestañas de "Mi perfil": la URL recuerda la pestaña ----------
    $('a[data-toggle="pill"]').on("shown.bs.tab", function () {
        const tab = this.id.replace("tab-", "");
        try { history.replaceState(null, "", window.location.pathname + "?tab=" + tab); } catch (e) { /* nada */ }
    });

    // ---------- Contador de caracteres ----------
    document.querySelectorAll("[data-contador-de]").forEach(function (campo) {
        const contador = document.getElementById(campo.dataset.contadorDe);
        const actualizar = function () { contador.textContent = campo.value.length; };
        campo.addEventListener("input", actualizar);
        actualizar();
    });

    // ---------- Foto de perfil: recorte cuadrado 300x300 en el navegador ----------
    const archivoFoto = document.getElementById("archivoFoto");
    if (archivoFoto) {
        const zona = document.getElementById("zonaArchivo");
        const errorFoto = document.getElementById("errorFoto");
        const btnGuardar = document.getElementById("btnGuardarFoto");
        function procesar(archivo) {
            errorFoto.textContent = "";
            if (!archivo) return;
            if (!/^image\/(jpeg|png|webp)$/.test(archivo.type)) { errorFoto.textContent = "Elige una imagen JPG, PNG o WEBP."; return; }
            if (archivo.size > 5 * 1024 * 1024) { errorFoto.textContent = "La imagen pesa más de 5 MB. Elige otra."; return; }
            const lector = new FileReader();
            lector.onload = function () {
                const img = new Image();
                img.onload = function () {
                    const lado = Math.min(img.width, img.height), destino = 300;
                    const lienzo = document.createElement("canvas");
                    lienzo.width = destino; lienzo.height = destino;
                    const ctx2d = lienzo.getContext("2d");
                    ctx2d.fillStyle = "#ffffff";
                    ctx2d.fillRect(0, 0, destino, destino);
                    ctx2d.drawImage(img, (img.width - lado) / 2, (img.height - lado) / 2, lado, lado, 0, 0, destino, destino);
                    const dataUrl = lienzo.toDataURL("image/jpeg", 0.85);
                    document.getElementById("fotoDatos").value = dataUrl;
                    document.getElementById("vistaFoto").innerHTML =
                        '<img class="avatar-iniciales avatar-foto avatar-xxl" alt="Vista previa de tu nueva foto" src="' + dataUrl + '">';
                    btnGuardar.disabled = false;
                    btnGuardar.focus();
                };
                img.onerror = function () { errorFoto.textContent = "No pudimos leer la imagen. Prueba con otra."; };
                img.src = lector.result;
            };
            lector.readAsDataURL(archivo);
        }
        archivoFoto.addEventListener("change", function () { procesar(archivoFoto.files[0]); });
        ["dragenter", "dragover"].forEach(function (ev) {
            zona.addEventListener(ev, function (e) { e.preventDefault(); zona.classList.add("arrastrando"); });
        });
        ["dragleave", "drop"].forEach(function (ev) {
            zona.addEventListener(ev, function (e) { e.preventDefault(); zona.classList.remove("arrastrando"); });
        });
        zona.addEventListener("drop", function (e) { procesar(e.dataTransfer.files[0]); });
        document.getElementById("formFoto").addEventListener("submit", function () {
            btnGuardar.disabled = true;
            btnGuardar.innerHTML = '<i class="fas fa-spinner fa-spin mr-1"></i> Subiendo...';
        });
    }

    // ---------- Formularios de perfil: evitar doble envío y llevar al primer error ----------
    ["formPerfilDatos", "formPassword"].forEach(function (id) {
        const f = document.getElementById(id);
        if (!f) return;
        const primerError = f.querySelector(".is-invalid");
        if (primerError) primerError.focus();
        f.addEventListener("submit", function () {
            const b = f.querySelector("button[type=submit]");
            b.disabled = true;
            b.innerHTML = '<i class="fas fa-spinner fa-spin mr-1"></i> Guardando...';
        });
    });
});
