/*
 * Validaciones.js - ayudas y validación en el navegador.
 * IMPORTANTE: el servidor vuelve a validar todo, porque la validación
 * JavaScript se puede desactivar.
 *
 * Cada bloque revisa si su elemento existe, así un mismo archivo sirve para
 * login, registro, recuperación y nueva contraseña.
 */
document.addEventListener("DOMContentLoaded", function () {

    const $ = function (id) { return document.getElementById(id); };

    // ---------- Mostrar / ocultar contraseña ----------
    document.querySelectorAll(".toggle-password").forEach(function (boton) {
        boton.addEventListener("click", function () {
            const input = $(boton.dataset.target);
            const oculto = input.type === "password";
            input.type = oculto ? "text" : "password";
            boton.textContent = oculto ? "Ocultar" : "Mostrar";
            boton.setAttribute("aria-label", oculto ? "Ocultar contraseña" : "Mostrar contraseña");
            input.focus();
        });
    });

    // ---------- Aviso de Bloq Mayús (prevención de errores) ----------
    const avisoMayus = $("aviso-mayus");
    if (avisoMayus && $("password")) {
        ["keydown", "keyup"].forEach(function (evento) {
            $("password").addEventListener(evento, function (e) {
                if (e.getModifierState) {
                    avisoMayus.classList.toggle("visible", e.getModifierState("CapsLock"));
                }
            });
        });
    }

    // ---------- Evitar doble envío ----------
    function bloquearBotonAlEnviar(form, texto) {
        form.addEventListener("submit", function (e) {
            if (e.defaultPrevented) return;
            const boton = form.querySelector("button[type=submit]");
            if (boton) {
                boton.disabled = true;
                boton.textContent = texto || "Procesando...";
            }
        });
    }

    // ---------- Pestañas Paciente / Personal (login) ----------
    const tabs = document.querySelectorAll(".tab-acceso");
    if (tabs.length) {
        const textos = {
            paciente: {
                etiqueta: "DNI", placeholder: "8 dígitos, ej: 74839201", modo: "numeric",
                ayuda: "Ingresa con el DNI con el que te registraste."
            },
            personal: {
                etiqueta: "Usuario o DNI", placeholder: "Ej: jperez", modo: "text",
                ayuda: "Médicos, recepción y administración."
            }
        };
        function seleccionar(tipo, enfocar) {
            tabs.forEach(function (t) { t.setAttribute("aria-selected", String(t.dataset.tipo === tipo)); });
            const t = textos[tipo];
            $("tipoAcceso").value = tipo;
            $("etiqueta-usuario").textContent = t.etiqueta;
            $("nombreUsuario").placeholder = t.placeholder;
            $("nombreUsuario").setAttribute("inputmode", t.modo);
            $("ayuda-tab").textContent = t.ayuda;
            $("enlace-registro").hidden = tipo !== "paciente";
            $("ayuda-personal").hidden = tipo !== "personal";
            try { localStorage.setItem("clinica.tipoAcceso", tipo); } catch (e) { /* sin almacenamiento */ }
            if (enfocar) $("nombreUsuario").focus();
        }
        tabs.forEach(function (tab) {
            tab.addEventListener("click", function () { seleccionar(tab.dataset.tipo, true); });
            // Flechas izquierda/derecha para moverse entre pestañas con el teclado
            tab.addEventListener("keydown", function (e) {
                if (e.key === "ArrowRight" || e.key === "ArrowLeft") {
                    const otro = tab.dataset.tipo === "paciente" ? "personal" : "paciente";
                    seleccionar(otro, false);
                    document.querySelector('.tab-acceso[data-tipo="' + otro + '"]').focus();
                }
            });
        });
        // Si no viene indicada por el servidor, recordar la última pestaña usada en este navegador
        if (!/[?&]tipo=/.test(location.search)) {
            let guardado = null;
            try { guardado = localStorage.getItem("clinica.tipoAcceso"); } catch (e) { /* nada */ }
            if (guardado === "personal") seleccionar("personal", false);
        }
    }

    const formLogin = $("formLogin");
    if (formLogin) bloquearBotonAlEnviar(formLogin, "Ingresando...");

    const formRecuperar = $("formRecuperar");
    if (formRecuperar) bloquearBotonAlEnviar(formRecuperar, "Enviando código...");

    // ---------- Código de 6 dígitos ----------
    const codigo = $("codigo");
    if (codigo) {
        codigo.addEventListener("input", function () {
            codigo.value = codigo.value.replace(/\D/g, "").slice(0, 6);
        });
        bloquearBotonAlEnviar($("formCodigo"), "Verificando...");
    }

    // Botón "Reenviar código" con cuenta regresiva (estado visible del sistema)
    const btnReenviar = $("btnReenviar");
    if (btnReenviar) {
        let espera = parseInt(btnReenviar.dataset.espera || "0", 10);
        const textoOriginal = "Reenviar código";
        const tick = function () {
            if (espera > 0) {
                btnReenviar.disabled = true;
                btnReenviar.textContent = "Reenviar código en " + espera + " s";
                espera--;
                setTimeout(tick, 1000);
            } else {
                btnReenviar.disabled = false;
                btnReenviar.textContent = textoOriginal;
            }
        };
        tick();
    }

    // ---------- Reglas de campos ----------
    const reglas = {
        dni:       { regex: /^\d{8}$/, msg: "El DNI debe tener exactamente 8 números." },
        nombres:   { regex: /^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ' ]{2,80}$/, msg: "Solo letras, mínimo 2." },
        apellidos: { regex: /^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ' ]{2,80}$/, msg: "Solo letras, mínimo 2." },
        celular:   { regex: /^9\d{8}$/, msg: "9 números y debe empezar con 9." },
        correo:    { regex: /^[\w.+-]+@[\w-]+(\.[\w-]+)*\.[A-Za-z]{2,}$/, msg: "Escribe un correo válido, ej. nombre@gmail.com." },
        password:  { regex: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,64}$/, msg: "Cumple todos los requisitos de la lista." }
    };

    function mostrarError(campo, mensaje) {
        const input = $(campo);
        const span = $("error-" + campo);
        if (!input) return;
        input.classList.toggle("invalido", !!mensaje);
        input.classList.toggle("valido", !mensaje && input.value.length > 0);
        input.setAttribute("aria-invalid", mensaje ? "true" : "false");
        if (span) span.textContent = mensaje || "";
    }

    function validarCampo(campo) {
        const input = $(campo);
        if (campo === "confirmarPassword") {
            const ok = input.value.length > 0 && input.value === $("password").value;
            mostrarError(campo, ok ? "" : "Las contraseñas no coinciden.");
            return ok;
        }
        const ok = reglas[campo].regex.test(input.value.trim());
        mostrarError(campo, ok ? "" : reglas[campo].msg);
        return ok;
    }

    // Lista de requisitos de contraseña en vivo (registro y nueva contraseña)
    if ($("req-longitud") && $("password")) {
        const requisitos = {
            "req-longitud":  function (v) { return v.length >= 8; },
            "req-mayuscula": function (v) { return /[A-Z]/.test(v); },
            "req-minuscula": function (v) { return /[a-z]/.test(v); },
            "req-numero":    function (v) { return /\d/.test(v); }
        };
        $("password").addEventListener("input", function () {
            const v = $("password").value;
            Object.keys(requisitos).forEach(function (id) { $(id).classList.toggle("cumple", requisitos[id](v)); });
            if ($("confirmarPassword") && $("confirmarPassword").value.length > 0) validarCampo("confirmarPassword");
        });
    }

    // Activa validación en vivo y al enviar para los campos presentes en el formulario
    function prepararFormulario(form, campos) {
        campos.forEach(function (campo) {
            const input = $(campo);
            input.addEventListener("blur", function () { if (input.value.length > 0) validarCampo(campo); });
            input.addEventListener("input", function () { if (input.classList.contains("invalido")) validarCampo(campo); });
        });
        form.addEventListener("submit", function (e) {
            let primerError = null;
            campos.forEach(function (campo) { if (!validarCampo(campo) && !primerError) primerError = campo; });
            if (primerError) {
                e.preventDefault();
                $(primerError).focus();
            }
        });
        bloquearBotonAlEnviar(form, "Guardando...");
    }

    const formRegistro = $("formRegistro");
    if (formRegistro) {
        ["dni", "celular"].forEach(function (id) {
            $(id).addEventListener("input", function () { $(id).value = $(id).value.replace(/\D/g, ""); });
        });
        prepararFormulario(formRegistro, ["dni", "nombres", "apellidos", "celular", "correo", "password", "confirmarPassword"]);
    }

    const formNueva = $("formNuevaContrasena");
    if (formNueva) {
        prepararFormulario(formNueva, ["password", "confirmarPassword"]);
    }
});
