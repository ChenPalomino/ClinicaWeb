/*
 * citas.js - interacciones del módulo de CITAS (agenda, asistente de reserva, horario del médico).
 * Todo el texto que se muestra se arma con textContent (nunca innerHTML) para evitar inyección de código.
 */
(function () {
    "use strict";

    // ---------- Filtros que se aplican solos al cambiar (menos clics, Nielsen #7) ----------
    document.querySelectorAll("[data-autoenviar]").forEach(function (campo) {
        campo.addEventListener("change", function () {
            if (campo.form) campo.form.submit();
        });
    });

    // ---------- Agenda: ← → cambian de día/semana/mes y H vuelve a hoy (Nielsen #7, aceleradores) ----------
    var atajos = {};
    document.querySelectorAll("[data-atajo]").forEach(function (a) { atajos[a.dataset.atajo] = a; });
    if (atajos.anterior || atajos.siguiente) {
        document.addEventListener("keydown", function (e) {
            if (e.altKey || e.ctrlKey || e.metaKey || e.shiftKey || e.defaultPrevented) return;
            var t = e.target;
            if (t && (t.isContentEditable || /^(INPUT|TEXTAREA|SELECT|BUTTON)$/.test(t.tagName))) return;
            if (document.querySelector(".modal.show")) return;
            var destino = e.key === "ArrowLeft" ? atajos.anterior : e.key === "ArrowRight" ? atajos.siguiente
                        : (e.key === "h" || e.key === "H") ? atajos.hoy : null;
            if (destino) { e.preventDefault(); destino.click(); }
        });
    }

    function texto(id, valor) {
        var el = document.getElementById(id);
        if (el) el.textContent = valor || "";
    }

    // ---------- Asistente: clic en un turno -> ventana de confirmación (prevención de errores, Nielsen #5) ----------
    $(document).on("click", "[data-turno]", function () {
        var b = this.dataset;
        var form = document.getElementById("formReservar");
        if (!form) return;
        form.elements.medico.value = b.medico;
        form.elements.fecha.value = b.fecha;
        form.elements.hora.value = b.hora;
        texto("resFecha", b.fechaTexto);
        texto("resHora", b.horaTexto);
        texto("resMedico", b.medicoNombre);
        texto("resLugar", b.lugar);
        var boton = document.getElementById("btnConfirmarReserva");
        boton.disabled = false;
        boton.innerHTML = boton.dataset.textoOriginal || boton.innerHTML;
        $("#modalReservar").modal("show");
    });
    $("#modalReservar").on("shown.bs.modal", function () {
        var motivo = document.getElementById("motivoCita");
        if (motivo) motivo.focus();
    });

    // ---------- Cancelar una cita: ventana con motivos para elegir con un clic ----------
    $(document).on("click", "[data-cancelar-cita]", function () {
        var b = this.dataset;
        var form = document.getElementById("formCancelarCita");
        if (!form) return;
        form.elements.id.value = b.id;
        if (form.elements.regreso) form.elements.regreso.value = b.regreso || "";
        texto("cancelarResumen", b.resumen);
        form.querySelectorAll('input[name="motivo"]').forEach(function (r) { r.checked = false; });
        document.getElementById("motivoOtroGrupo").classList.add("d-none");
        form.elements.motivoOtro.value = "";
        var boton = form.querySelector('button[type="submit"]');
        boton.disabled = true;
        $("#modalCancelarCita").modal("show");
    });
    $(document).on("change", '#formCancelarCita input[name="motivo"]', function () {
        var otro = this.value === "OTRO";
        document.getElementById("motivoOtroGrupo").classList.toggle("d-none", !otro);
        if (otro) document.getElementById("motivoOtro").focus();
        validarCancelacion();
    });
    $(document).on("input", "#motivoOtro", validarCancelacion);
    function validarCancelacion() {
        var form = document.getElementById("formCancelarCita");
        var elegido = form.querySelector('input[name="motivo"]:checked');
        var ok = elegido && (elegido.value !== "OTRO" || form.elements.motivoOtro.value.trim().length >= 5);
        form.querySelector('button[type="submit"]').disabled = !ok;
    }

    // ---------- Médico: registrar atención o inasistencia con una nota opcional ----------
    $(document).on("click", "[data-atencion]", function () {
        var b = this.dataset;
        var form = document.getElementById("formAtencion");
        if (!form) return;
        form.elements.id.value = b.id;
        form.elements.accion.value = b.atencion;
        if (form.elements.regreso) form.elements.regreso.value = b.regreso || "";
        var atendida = b.atencion === "atendida";
        texto("atencionTitulo", atendida ? "Registrar atención" : "Marcar «No asistió»");
        texto("atencionResumen", b.resumen);
        document.getElementById("atencionNotaGrupo").classList.toggle("d-none", !atendida);
        var boton = document.getElementById("btnAtencion");
        boton.className = "btn " + (atendida ? "btn-success" : "btn-secondary");
        boton.textContent = atendida ? "Guardar como atendida" : "Sí, no asistió";
        boton.disabled = false;
        $("#modalAtencion").modal("show");
    });

    // ---------- Evitar doble envío en los formularios de citas ----------
    document.querySelectorAll("form[data-un-envio]").forEach(function (f) {
        f.addEventListener("submit", function () {
            var b = f.querySelector('button[type="submit"]');
            if (b) {
                if (!b.dataset.textoOriginal) b.dataset.textoOriginal = b.innerHTML;
                setTimeout(function () { b.disabled = true; b.textContent = "Guardando..."; }, 0);
            }
        });
    });

    // ---------- Horario semanal del médico ----------
    var tabla = document.getElementById("tablaHorario");
    if (tabla) {
        function refrescarDia(fila) {
            var on = fila.querySelector('input[type="checkbox"]').checked;
            fila.classList.toggle("dia-apagado", !on);
            fila.querySelectorAll('input[type="time"]').forEach(function (t) { t.disabled = !on; });
        }
        tabla.querySelectorAll("tr[data-dia]").forEach(function (fila) {
            fila.querySelector('input[type="checkbox"]').addEventListener("change", function () { refrescarDia(fila); });
            refrescarDia(fila);
        });
        var copiar = document.getElementById("copiarLunes");
        if (copiar) {
            copiar.addEventListener("click", function () {
                var lunes = tabla.querySelector('tr[data-dia="1"]');
                var horas = Array.prototype.map.call(lunes.querySelectorAll('input[type="time"]'), function (t) { return t.value; });
                for (var d = 2; d <= 5; d++) {
                    var fila = tabla.querySelector('tr[data-dia="' + d + '"]');
                    fila.querySelector('input[type="checkbox"]').checked = lunes.querySelector('input[type="checkbox"]').checked;
                    fila.querySelectorAll('input[type="time"]').forEach(function (t, i) { t.value = horas[i]; });
                    refrescarDia(fila);
                }
                copiar.textContent = "¡Copiado de martes a viernes!";
                setTimeout(function () { copiar.textContent = "Copiar el lunes a martes–viernes"; }, 2000);
            });
        }
    }

    // ---------- Tira de días: deja visible el día elegido ----------
    // (se mueve SOLO la tira: scrollIntoView podría desplazar también la página)
    var activo = document.querySelector(".tira-dias .dia-btn.activo");
    if (activo) {
        var tira = activo.parentElement;
        var izquierda = activo.offsetLeft - tira.offsetLeft - (tira.clientWidth - activo.offsetWidth) / 2;
        if (izquierda > 0) tira.scrollLeft = izquierda;
    }
})();
