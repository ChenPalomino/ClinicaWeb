<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.math.BigDecimal, controlador.MensajeFlash, modelo.dto.ComprobanteDTO, modelo.dto.DetalleCuentaDTO, modelo.dto.ParametroClinicaDTO, modelo.cobranza.MontoEnLetras, controlador.FormatoVista, seguridad.EscapeHtml, configuracion.VersionAplicacion" %>
<%--
    Representación impresa de la BOLETA o FACTURA (RF-15). Página independiente,
    pensada para imprimirse o guardarse como PDF desde el navegador.
--%>
<%
    ComprobanteDTO c = (ComprobanteDTO) request.getAttribute("comprobante");
    ParametroClinicaDTO par = (ParametroClinicaDTO) request.getAttribute("parametros");
    String volver = (String) request.getAttribute("volver");
    MensajeFlash flash = MensajeFlash.tomar(session);     // ej. "Cobro registrado... vuelto"
    String ctx = request.getContextPath();
    // % de IGV con el que se EMITIÓ (aunque el parámetro cambie después): IGV / base imponible
    String igv = c.getOpGravada().signum() > 0
            ? c.getIgv().multiply(BigDecimal.valueOf(100)).divide(c.getOpGravada(), 0, java.math.RoundingMode.HALF_UP).toPlainString()
            : par.getIgvPorcentaje().stripTrailingZeros().toPlainString();
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><%= c.getNumeroCompleto() %> - <%= EscapeHtml.texto(par.getNombreComercial()) %></title>
    <script src="<%= ctx %>/js/pestana.js?v=<%= VersionAplicacion.RECURSOS %>"></script>
    <style>
        :root { --tinta: #1f2937; --suave: #6b7280; --linea: #d1d5db; --marca: #0b6b5a; --fondo: #eef2f1; --papel: #ffffff; --anulado: #b91c1c; }
        * { box-sizing: border-box; }
        body { margin: 0; background: var(--fondo); color: var(--tinta); font: 14px/1.45 "Segoe UI", system-ui, -apple-system, sans-serif; padding: 16px; }
        .barra { max-width: 800px; margin: 0 auto 12px; display: flex; gap: 8px; flex-wrap: wrap; align-items: center; }
        .barra a, .barra button { font: inherit; padding: 8px 14px; border-radius: 6px; border: 1px solid var(--linea); background: var(--papel); color: var(--tinta); text-decoration: none; cursor: pointer; }
        .barra button.principal { background: var(--marca); border-color: var(--marca); color: #fff; }
        .barra .aviso { margin-left: auto; color: var(--suave); font-size: 12px; }
        .mensaje { max-width: 800px; margin: 0 auto 12px; padding: 10px 14px; border-radius: 6px; background: #e7f6ef; color: #0f5132; border: 1px solid #badbcc; font-weight: 600; }
        .mensaje-danger { background: #fdecea; color: #842029; border-color: #f5c2c7; }
        .mensaje-warning, .mensaje-info { background: #fff8e1; color: #664d03; border-color: #ffe69c; }
        .hoja { position: relative; max-width: 800px; margin: 0 auto; background: var(--papel); padding: 32px; border-radius: 4px; box-shadow: 0 1px 3px rgba(0,0,0,.12); overflow: hidden; }
        .encabezado { display: flex; gap: 24px; justify-content: space-between; flex-wrap: wrap; }
        .emisor h1 { margin: 0 0 4px; font-size: 20px; color: var(--marca); }
        .emisor p { margin: 0; color: var(--suave); font-size: 12.5px; }
        .recuadro { border: 2px solid var(--marca); border-radius: 6px; padding: 12px 18px; text-align: center; min-width: 230px; }
        .recuadro div { font-weight: 700; }
        .recuadro .tipo { color: var(--marca); margin: 4px 0; font-size: 13px; letter-spacing: .03em; }
        .recuadro .numero { font-family: ui-monospace, Consolas, monospace; font-size: 17px; }
        .datos { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 4px 24px; margin: 24px 0 16px; padding: 12px 0; border-top: 1px solid var(--linea); border-bottom: 1px solid var(--linea); font-size: 13px; }
        .datos span { color: var(--suave); display: inline-block; min-width: 92px; }
        table { width: 100%; border-collapse: collapse; font-size: 13px; }
        th { text-align: left; font-size: 11.5px; text-transform: uppercase; letter-spacing: .04em; color: var(--suave); border-bottom: 2px solid var(--linea); padding: 6px 4px; }
        td { padding: 7px 4px; border-bottom: 1px solid #eef0f2; vertical-align: top; }
        .num { text-align: right; font-variant-numeric: tabular-nums; white-space: nowrap; }
        .pie { display: flex; gap: 24px; justify-content: space-between; flex-wrap: wrap; margin-top: 16px; }
        .letras { flex: 1 1 300px; font-size: 12.5px; }
        .totales { min-width: 260px; font-size: 13px; }
        .totales div { display: flex; justify-content: space-between; padding: 3px 0; }
        .totales .total { font-weight: 700; font-size: 16px; border-top: 2px solid var(--tinta); margin-top: 4px; padding-top: 6px; }
        .nota { margin-top: 20px; padding-top: 12px; border-top: 1px dashed var(--linea); color: var(--suave); font-size: 11.5px; }
        .sello { position: absolute; top: 38%; left: 50%; transform: translate(-50%, -50%) rotate(-18deg); border: 5px solid var(--anulado); color: var(--anulado);
                 font-size: 54px; font-weight: 800; letter-spacing: .12em; padding: 4px 28px; opacity: .22; pointer-events: none; }
        @media print {
            body { background: #fff; padding: 0; }
            .barra, .mensaje { display: none; }
            .hoja { box-shadow: none; padding: 0; max-width: none; }
        }
        @media (max-width: 480px) { .hoja { padding: 18px; } .recuadro { width: 100%; } }
    </style>
</head>
<body>
<div class="barra">
    <a href="<%= volver %>">&larr; Volver</a>
    <button type="button" class="principal" onclick="window.print()">Imprimir o guardar PDF</button>
    <span class="aviso">Emitido por @<%= EscapeHtml.texto(c.getCajero()) %></span>
</div>
<% if (flash != null) { %>
<div class="mensaje mensaje-<%= flash.getTipo() %>" role="status"><%= EscapeHtml.texto(flash.getTexto()) %></div>
<% } %>

<main class="hoja" aria-label="<%= c.getTitulo() %> <%= c.getNumeroCompleto() %>">
    <% if (c.isAnulado()) { %><div class="sello" aria-hidden="true">ANULADO</div><% } %>
    <div class="encabezado">
        <div class="emisor">
            <h1><%= EscapeHtml.texto(par.getNombreComercial()) %></h1>
            <p><strong><%= EscapeHtml.texto(par.getRazonSocial()) %></strong></p>
            <p><%= EscapeHtml.texto(par.getDireccion()) %></p>
            <% if (!par.getTelefono().isEmpty()) { %><p>Teléfono <%= EscapeHtml.texto(par.getTelefono()) %></p><% } %>
        </div>
        <div class="recuadro">
            <div>RUC <%= EscapeHtml.texto(par.getRuc()) %></div>
            <div class="tipo"><%= c.getTitulo() %></div>
            <div class="numero"><%= c.getNumeroCompleto() %></div>
        </div>
    </div>

    <section class="datos">
        <div><span>Fecha:</span> <%= FormatoVista.fechaHora(c.getFechaEmision()) %></div>
        <div><span>Cuenta:</span> <%= EscapeHtml.texto(c.getNumeroCuenta()) %></div>
        <div><span><%= c.isFactura() ? "Razón social:" : "Cliente:" %></span> <%= EscapeHtml.texto(c.getNombreCliente()) %></div>
        <div><span><%= "-".equals(c.getTipoDocumento()) ? "Documento:" : c.getTipoDocumento() + ":" %></span> <%= c.getNumeroDocumento() == null ? "—" : EscapeHtml.texto(c.getNumeroDocumento()) %></div>
        <% if (c.getDireccionCliente() != null) { %><div><span>Dirección:</span> <%= EscapeHtml.texto(c.getDireccionCliente()) %></div><% } %>
        <div><span>Paciente:</span> <%= EscapeHtml.texto(c.getNombrePaciente()) %> (<%= EscapeHtml.texto(c.getNumeroHistoria()) %>)</div>
        <div><span>Cobertura:</span> <%= EscapeHtml.texto(c.getNombreCobertura()) %></div>
        <div><span>Moneda:</span> Soles (PEN)</div>
    </section>

    <table>
        <thead><tr><th class="num">Cant.</th><th>Descripción</th><th class="num">P. unit.</th><th class="num">Dscto./cobertura</th><th class="num">Importe</th></tr></thead>
        <tbody>
        <% for (DetalleCuentaDTO d : c.getDetalles()) {
               BigDecimal cubierto = d.getMontoAseguradora().add(d.getMontoDescuento()); %>
            <tr>
                <td class="num"><%= d.getCantidad() %></td>
                <td><%= EscapeHtml.texto(d.getDescripcion()) %><%= d.isAfectoIgv() ? "" : " <small>(exonerado)</small>" %></td>
                <td class="num"><%= FormatoVista.soles(d.getPrecioUnitario()) %></td>
                <td class="num"><%= cubierto.signum() > 0 ? "−" + FormatoVista.soles(cubierto) : "—" %></td>
                <td class="num"><%= FormatoVista.soles(d.getImporte()) %></td>
            </tr>
        <% } %>
        </tbody>
    </table>

    <div class="pie">
        <div class="letras">
            <p><strong>SON:</strong> <%= MontoEnLetras.convertir(c.getTotal()) %></p>
            <p>Pago: <%= EscapeHtml.texto(c.getMetodoPago()) %><%= c.getReferenciaPago() == null ? "" : " · " + EscapeHtml.texto(c.getReferenciaPago()) %>
               <% if (c.getMontoRecibido() != null) { %><br>Recibido S/ <%= FormatoVista.soles(c.getMontoRecibido()) %> · Vuelto S/ <%= FormatoVista.soles(c.getVuelto()) %><% } %></p>
            <% if (c.getMontoAseguradora().signum() > 0) { %>
            <p>La aseguradora cubre S/ <%= FormatoVista.soles(c.getMontoAseguradora()) %> de esta atención (no incluido en este comprobante).</p>
            <% } %>
        </div>
        <div class="totales">
            <div><span>Op. gravada</span><span class="num">S/ <%= FormatoVista.soles(c.getOpGravada()) %></span></div>
            <div><span>Op. exonerada</span><span class="num">S/ <%= FormatoVista.soles(c.getOpExonerada()) %></span></div>
            <div><span>IGV (<%= igv %> %)</span><span class="num">S/ <%= FormatoVista.soles(c.getIgv()) %></span></div>
            <div class="total"><span>Importe total</span><span class="num">S/ <%= FormatoVista.soles(c.getTotal()) %></span></div>
        </div>
    </div>

    <p class="nota">Representación impresa de la <%= c.isFactura() ? "factura" : "boleta de venta" %> electrónica <%= c.getNumeroCompleto() %>.
        Sistema académico de práctica (curso POO II): este documento no tiene validez tributaria ante SUNAT.</p>
</main>
</body>
</html>
