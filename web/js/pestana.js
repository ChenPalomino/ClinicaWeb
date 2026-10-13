/*
 * pestana.js - UNA CUENTA POR PESTAÑA.
 *
 * El navegador comparte una sola cookie de sesión entre todas sus pestañas.
 * Para que cada pestaña tenga su propia cuenta (admin en una, médico en otra),
 * cada pestaña recibe un identificador propio guardado en sessionStorage
 * (sessionStorage es exclusivo de cada pestaña) y lo envía en el parámetro
 * "_t" de enlaces, formularios, redirecciones y llamadas fetch().
 * En el servidor, seguridad.SesionPorPestanaFilter separa los datos por pestaña.
 *
 * Se carga en el <head> de TODAS las páginas, antes que cualquier otro script.
 */
(function () {
    "use strict";
    var CLAVE = "clinica.pestana";
    var PARAMETRO = "_t";
    var VALIDO = /^[a-z0-9]{6,32}$/;

    function nuevoId() {
        var bytes = new Uint8Array(12);
        (window.crypto || window.msCrypto).getRandomValues(bytes);
        var id = "";
        for (var i = 0; i < bytes.length; i++) id += (bytes[i] % 36).toString(36);
        return id;
    }

    // 1. Identificador de ESTA pestaña. Una pestaña nueva (o un enlace pegado desde
    //    otra pestaña) nunca hereda el de otra: recibe uno nuevo y por lo tanto ve el login.
    var id = null;
    try { id = sessionStorage.getItem(CLAVE); } catch (e) { /* sin almacenamiento */ }
    if (!id || !VALIDO.test(id)) {
        id = nuevoId();
        try { sessionStorage.setItem(CLAVE, id); } catch (e) { /* sin almacenamiento */ }
    }

    var origen = location.origin;
    var ctx = "/" + (location.pathname.split("/")[1] || "");      // "/ClinicaWeb"

    /** Agrega (o corrige) "_t" en una URL de la misma aplicación. Las URL externas no se tocan. */
    function url(u) {
        if (u == null || u === "") return u;
        var a;
        try { a = new URL(String(u), location.href); } catch (e) { return u; }
        if (a.origin !== origin || (a.pathname !== ctx && a.pathname.indexOf(ctx + "/") !== 0)) return u;
        if (/^(javascript|mailto|tel):/i.test(String(u))) return u;
        a.searchParams.set(PARAMETRO, id);
        return a.pathname + a.search + a.hash;
    }

    // 2. Si la dirección no trae el "_t" de esta pestaña, se recarga con el correcto
    //    ANTES de mostrar nada (así nunca se ve la cuenta de otra pestaña).
    var actual = new URLSearchParams(location.search).get(PARAMETRO);
    if (actual !== id) {
        location.replace(url(location.href));
        return;
    }

    // 3. Enlaces: se corrigen al cargar y también justo al hacer clic (por si se crearon después)
    function corregirEnlace(a) {
        var href = a.getAttribute("href");
        if (!href || href.charAt(0) === "#") return;
        var nuevo = url(href);
        if (nuevo !== href) a.setAttribute("href", nuevo);
    }
    // 4. Formularios: un campo oculto "_t" (sirve para GET y POST)
    function corregirFormulario(f) {
        var campo = f.querySelector('input[name="' + PARAMETRO + '"]');
        if (!campo) {
            campo = document.createElement("input");
            campo.type = "hidden";
            campo.name = PARAMETRO;
            f.appendChild(campo);
        }
        campo.value = id;
        var accion = f.getAttribute("action");
        if (accion) f.setAttribute("action", url(accion));
    }
    function corregirTodo() {
        Array.prototype.forEach.call(document.querySelectorAll("a[href]"), corregirEnlace);
        Array.prototype.forEach.call(document.forms, corregirFormulario);
    }
    document.addEventListener("DOMContentLoaded", corregirTodo);
    document.addEventListener("click", function (e) {
        var a = e.target && e.target.closest ? e.target.closest("a[href]") : null;
        if (a) corregirEnlace(a);
    }, true);
    document.addEventListener("auxclick", function (e) {       // clic con la rueda (abrir en pestaña nueva)
        var a = e.target && e.target.closest ? e.target.closest("a[href]") : null;
        if (a) corregirEnlace(a);
    }, true);
    document.addEventListener("submit", function (e) { corregirFormulario(e.target); }, true);
    // form.submit() desde código no dispara "submit": se cubre también
    var enviarOriginal = HTMLFormElement.prototype.submit;
    HTMLFormElement.prototype.submit = function () {
        corregirFormulario(this);
        return enviarOriginal.apply(this, arguments);
    };

    // 5. fetch() en segundo plano (cambio de tema, etc.)
    if (window.fetch) {
        var fetchOriginal = window.fetch;
        window.fetch = function (recurso, opciones) {
            if (typeof recurso === "string") recurso = url(recurso);
            return fetchOriginal.call(this, recurso, opciones);
        };
    }

    // 6. history.replaceState/pushState (pestañas de "Mi perfil") conservan el "_t"
    ["replaceState", "pushState"].forEach(function (nombre) {
        var original = history[nombre];
        history[nombre] = function (estado, titulo, direccion) {
            return original.call(history, estado, titulo, direccion == null ? direccion : url(direccion));
        };
    });

    // Para los demás scripts (ej. redirecciones con location.href)
    window.ClinicaPestana = { id: id, url: url };
})();
