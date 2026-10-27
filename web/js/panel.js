/*
 * panel.js - comportamiento del dashboard (requiere jQuery + Bootstrap 4).
 *  - Ventana de confirmación para acciones (desactivar, bloquear...).
 *  - Mostrar/ocultar contraseña y requisitos en vivo.
 *  - Descripción del rol elegido y solo-números en DNI/celular.
 *  (js/pestana.js se carga antes: mantiene la cuenta propia de cada pestaña)
 */
$(function () {

    // =====================================================================
    //  CUENTA DE LA PÁGINA: cada pestaña tiene su propia cuenta (js/pestana.js).
    //  Cada formulario indica a qué cuenta pertenece la página, así una página
    //  antigua (botón "Atrás" tras cambiar de cuenta en esta pestaña) no puede
    //  hacer cambios con la cuenta nueva. El servidor lo comprueba (SesionVigente).
    // =====================================================================
    const miCuenta = document.body.dataset.usuario;
    document.querySelectorAll("form").forEach(function (form) {
        if ((form.getAttribute("method") || "").toUpperCase() !== "POST" || form.elements._cuenta) return;
        const oculto = document.createElement("input");
        oculto.type = "hidden"; oculto.name = "_cuenta"; oculto.value = miCuenta;
        form.appendChild(oculto);
    });
    function enPestana(u) { return window.ClinicaPestana ? window.ClinicaPestana.url(u) : u; }

    // ---------- Foto de perfil: clic para verla en grande ----------
    // Solo las fotos que no son ya un enlace o un menú (la del menú lateral lleva a "Mi perfil").
    function fotosAmpliables() {
        document.querySelectorAll("img.avatar-foto").forEach(function (img) {
            if (img.closest("a, [data-toggle], #vistaFoto")) return;
            img.classList.add("foto-ampliable");
            img.setAttribute("role", "button");
            img.setAttribute("tabindex", "0");
            img.setAttribute("aria-label", "Ver la foto en grande");
            img.title = "Ver la foto en grande";
        });
    }
    function abrirFoto(img) {
        let visor = document.getElementById("visorFoto");
        if (!visor) {
            visor = document.createElement("div");
            visor.id = "visorFoto";
            visor.className = "modal fade visor-foto";
            visor.tabIndex = -1;
            visor.setAttribute("role", "dialog");
            visor.setAttribute("aria-label", "Foto de perfil");
            visor.innerHTML = '<div class="modal-dialog modal-dialog-centered" role="document"><div class="modal-content">'
                + '<button type="button" class="close" data-dismiss="modal" aria-label="Cerrar"><span aria-hidden="true">&times;</span></button>'
                + '<img alt="Foto de perfil en grande"><p class="visor-nombre mb-0"></p></div></div>';
            document.body.appendChild(visor);
            // Esc cierra el visor incluso si se pulsa mientras todavía se está abriendo
            let cerrarAlMostrar = false;
            $(visor).on("shown.bs.modal", function () { if (cerrarAlMostrar) { cerrarAlMostrar = false; $(visor).modal("hide"); } });
            $(visor).on("hidden.bs.modal", function () { cerrarAlMostrar = false; });
            document.addEventListener("keydown", function (e) {
                if (e.key !== "Escape" || !visor.classList.contains("show")) return;
                cerrarAlMostrar = true;
                $(visor).modal("hide");
            });
        }
        visor.querySelector("img").src = img.src;
        const fila = img.closest("tr");
        const celda = fila ? fila.querySelector(".font-weight-bold") : null;   // en el listado: el nombre, sin etiquetas como "Tú"
        const nombre = img.dataset.nombre || (celda && celda.firstChild ? celda.firstChild.textContent.trim() : "");
        visor.querySelector(".visor-nombre").textContent = nombre;
        $(visor).modal("show");
    }
    fotosAmpliables();
    $(document).on("click", "img.foto-ampliable", function () { abrirFoto(this); });
    $(document).on("keydown", "img.foto-ampliable", function (e) {
        if (e.key === "Enter" || e.key === " ") { e.preventDefault(); abrirFoto(this); }
    });

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
                const visible = !op.dataset.extra || !v || op.dataset.extra === v;
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
                if (r.status === 409) window.location.reload();                          // la página era de otra cuenta
                if (r.status === 401) {
                    window.location.href = enPestana(cuerpo.dataset.ctx + "/jsp/login.jsp?error=" + (r.headers.get("X-Motivo") || "sesion"));
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

    // ---------- Copiar un dato (celular de la clínica, etc.) ----------
    document.querySelectorAll("[data-copiar]").forEach(function (b) {
        b.addEventListener("click", function () {
            const original = b.innerHTML;
            const listo = function () { b.textContent = "Copiado"; setTimeout(function () { b.innerHTML = original; }, 1600); };
            if (navigator.clipboard && navigator.clipboard.writeText) navigator.clipboard.writeText(b.dataset.copiar).then(listo, function () {});
        });
    });

    // ---------- QR de pago: se reduce en el navegador sin perder nitidez (PNG) ----------
    const archivoQr = document.getElementById("archivoQr");
    if (archivoQr) {
        const errorQr = document.getElementById("errorQr");
        const btnQr = document.getElementById("btnGuardarQr");
        archivoQr.addEventListener("change", function () {
            const archivo = archivoQr.files[0];
            errorQr.textContent = "";
            btnQr.disabled = true;
            if (!archivo) return;
            if (!/^image\/(jpeg|png|webp)$/.test(archivo.type)) { errorQr.textContent = "Elige una imagen PNG, JPG o WEBP."; return; }
            const lector = new FileReader();
            lector.onload = function () {
                const img = new Image();
                img.onload = function () {
                    const escala = Math.min(1, 700 / Math.max(img.width, img.height));
                    const lienzo = document.createElement("canvas");
                    lienzo.width = Math.round(img.width * escala); lienzo.height = Math.round(img.height * escala);
                    const c2 = lienzo.getContext("2d");
                    c2.fillStyle = "#ffffff"; c2.fillRect(0, 0, lienzo.width, lienzo.height);
                    c2.imageSmoothingEnabled = false;
                    c2.drawImage(img, 0, 0, lienzo.width, lienzo.height);
                    const dataUrl = lienzo.toDataURL("image/png");
                    if (dataUrl.length * 0.75 > 400000) { errorQr.textContent = "La imagen sigue siendo pesada. Recórtala para que se vea solo el QR."; return; }
                    document.getElementById("qrDatos").value = dataUrl;
                    const vista = document.getElementById("vistaQr");
                    vista.innerHTML = "";
                    const previa = document.createElement("img");
                    previa.src = dataUrl; previa.alt = "Vista previa del nuevo QR"; previa.className = "img-fluid qr-imagen my-2";
                    vista.appendChild(previa);
                    btnQr.disabled = false;
                    btnQr.focus();
                };
                img.onerror = function () { errorQr.textContent = "No pudimos leer la imagen. Prueba con otra."; };
                img.src = lector.result;
            };
            lector.readAsDataURL(archivo);
        });
    }

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
                    const lado = Math.min(img.width, img.height), destino = 600;   // 600 px: se ve nítida al ampliarla
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
