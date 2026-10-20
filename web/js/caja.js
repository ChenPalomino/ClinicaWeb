/*
 * caja.js - ayudas de la pantalla de cobro (sin dependencias).
 *  - Vuelto en vivo al escribir con cuánto paga el paciente (efectivo).
 *  - Cambiar la cantidad de una línea la guarda sola.
 *  - El botón "Cobrar" se desactiva al enviarse (evita un doble clic = doble intento).
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

    // Cantidades: se guardan al cambiar (el botón queda para teclado y lectores de pantalla)
    Array.prototype.forEach.call(document.querySelectorAll("[data-auto-enviar]"), function (input) {
        input.addEventListener("change", function () {
            if (!input.checkValidity()) return;
            if (input.form.requestSubmit) input.form.requestSubmit(); else input.form.submit();
        });
    });
})();
