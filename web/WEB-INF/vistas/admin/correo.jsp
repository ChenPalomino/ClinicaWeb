<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List, java.util.Map, notificacion.EventoCorreo" %>
<%-- Diagnóstico del correo (Brevo): por qué un código de recuperación no llega. Solo Administrador. --%>
<%@ include file="/WEB-INF/vistas/plantilla/cabecera.jspf" %>
<%
    boolean modoReal = Boolean.TRUE.equals(request.getAttribute("modoReal"));
    @SuppressWarnings("unchecked") Map<String, String> configuracion = (Map<String, String>) request.getAttribute("configuracion");
    @SuppressWarnings("unchecked") Map<String, String> cuenta = (Map<String, String>) request.getAttribute("cuenta");
    @SuppressWarnings("unchecked") List<EventoCorreo> eventos = (List<EventoCorreo>) request.getAttribute("eventos");
    @SuppressWarnings("unchecked") List<String> correosPrueba = (List<String>) request.getAttribute("correosPrueba");
    String errorEventos = (String) request.getAttribute("errorEventos");
    String emailFiltro = (String) request.getAttribute("emailFiltro");
    String destinoSugerido = emailFiltro.isEmpty() ? perfilSesion.getCorreo() : emailFiltro;
%>

<% if (!correosPrueba.isEmpty()) { %>
<div class="alert alert-warning">
    <i class="fas fa-exclamation-triangle mr-2"></i>
    <strong><%= correosPrueba.size() %> cuenta<%= correosPrueba.size() == 1 ? "" : "s" %> con correo de prueba (@clinica.demo):</strong>
    <% for (int i = 0; i < correosPrueba.size(); i++) { %><%= i > 0 ? ", " : " " %>@<%= EscapeHtml.texto(correosPrueba.get(i)) %><% } %>.
    Esas direcciones no existen: los códigos de recuperación nunca les llegarán. Cámbialas por correos reales en
    <a href="<%= ctx %>/admin/usuarios">Usuarios</a> (o cada uno en Mi perfil).
</div>
<% } %>

<div class="row">
    <div class="col-lg-6">
        <div class="card">
            <div class="card-header"><h3 class="card-title"><i class="fas fa-cog mr-2"></i>Configuración</h3></div>
            <div class="card-body p-0">
                <table class="table table-sm mb-0">
                <% for (Map.Entry<String, String> e : configuracion.entrySet()) { %>
                    <tr><th class="pl-3 w-50"><%= EscapeHtml.texto(e.getKey()) %></th><td><%= EscapeHtml.texto(e.getValue()) %></td></tr>
                <% } %>
                </table>
            </div>
            <% if (!modoReal) { %>
            <div class="card-footer small text-muted">Para enviar correos reales define las variables de entorno BREVO_API_KEY y CORREO_REMITENTE y reinicia NetBeans/Tomcat.</div>
            <% } %>
        </div>
        <% if (modoReal) { %>
        <div class="card">
            <div class="card-header"><h3 class="card-title"><i class="fas fa-id-badge mr-2"></i>Cuenta de Brevo</h3></div>
            <div class="card-body p-0">
            <% if (cuenta.containsKey("error")) { %>
                <div class="alert alert-danger m-3 mb-3"><i class="fas fa-times-circle mr-2"></i><%= EscapeHtml.texto(cuenta.get("error")) %></div>
            <% } else { %>
                <table class="table table-sm mb-0">
                    <tr><th class="pl-3 w-50">Cuenta</th><td><%= EscapeHtml.texto(cuenta.getOrDefault("cuenta", "—")) %></td></tr>
                    <tr><th class="pl-3">Plan</th><td><%= EscapeHtml.texto(cuenta.getOrDefault("plan", "—")) %></td></tr>
                    <tr><th class="pl-3">Créditos disponibles</th><td><%= EscapeHtml.texto(cuenta.getOrDefault("creditos", "—")) %></td></tr>
                </table>
            <% } %>
            </div>
        </div>
        <% } %>
    </div>
    <div class="col-lg-6">
        <div class="card card-outline card-primary">
            <div class="card-header"><h3 class="card-title"><i class="fas fa-paper-plane mr-2"></i>Enviar un correo de prueba</h3></div>
            <div class="card-body">
                <form method="POST" action="<%= ctx %>/admin/correo" data-form-crud>
                    <input type="hidden" name="accion" value="probar">
                    <label for="destinatario">Enviar a</label>
                    <div class="input-group">
                        <input type="email" class="form-control" id="destinatario" name="destinatario" required maxlength="100"
                               value="<%= EscapeHtml.texto(destinoSugerido) %>" placeholder="tu.correo@gmail.com">
                        <div class="input-group-append"><button type="submit" class="btn btn-primary"><i class="fas fa-paper-plane mr-1"></i> Probar</button></div>
                    </div>
                    <small class="form-text text-muted">Muestra exactamente lo que responde Brevo y, abajo, si lo entregó.</small>
                </form>
            </div>
        </div>
        <div class="callout callout-info small">
            <strong>Si Brevo lo acepta pero no llega:</strong>
            <ol class="pl-3 mb-0 mt-1">
                <li>Mira abajo el estado: <em>Bloqueado</em> o <em>Rebote</em> indican el motivo.</li>
                <li>Revisa que el correo del usuario sea real (no @clinica.demo).</li>
                <li>Con un remitente @gmail.com, algunos proveedores lo envían a spam o lo rechazan; un dominio propio verificado en Brevo es lo más confiable.</li>
            </ol>
        </div>
    </div>
</div>

<% if (modoReal) { %>
<div class="card">
    <div class="card-header d-flex flex-wrap align-items-center">
        <h3 class="card-title mr-auto"><i class="fas fa-stream mr-2"></i>Últimos eventos de entrega (Brevo)</h3>
        <form method="GET" action="<%= ctx %>/admin/correo" class="form-inline">
            <input type="email" name="email" class="form-control form-control-sm mr-1" placeholder="Filtrar por correo" value="<%= EscapeHtml.texto(emailFiltro) %>">
            <button type="submit" class="btn btn-sm btn-secondary">Ver</button>
        </form>
    </div>
    <div class="card-body p-0">
    <% if (errorEventos != null) { %>
        <div class="alert alert-danger m-3"><i class="fas fa-times-circle mr-2"></i><%= EscapeHtml.texto(errorEventos) %></div>
    <% } else if (eventos.isEmpty()) { %>
        <div class="estado-vacio"><i class="fas fa-inbox text-muted"></i><p class="mb-0">Brevo no tiene eventos<%= emailFiltro.isEmpty() ? "" : " para ese correo" %> todavía.</p></div>
    <% } else { %>
        <div class="table-responsive">
        <table class="table table-sm table-hover mb-0">
            <thead class="thead-light"><tr><th>Fecha</th><th>Para</th><th class="d-none d-md-table-cell">Asunto</th><th>Estado</th><th>Motivo</th></tr></thead>
            <tbody>
            <% for (EventoCorreo e : eventos) { %>
                <tr>
                    <td class="small text-nowrap"><%= EscapeHtml.texto(e.getFecha().length() >= 16 ? e.getFecha().substring(0, 16).replace('T', ' ') : e.getFecha()) %></td>
                    <td class="small"><%= EscapeHtml.texto(e.getDestinatario()) %></td>
                    <td class="small d-none d-md-table-cell"><%= EscapeHtml.texto(e.getAsunto()) %></td>
                    <td><span class="badge <%= e.isProblema() ? "badge-danger" : ("delivered".equals(e.getEvento()) ? "badge-success" : "badge-light border") %>"><%= EscapeHtml.texto(e.getEventoLegible()) %></span></td>
                    <td class="small text-break"><%= EscapeHtml.texto(e.getMotivo().isEmpty() ? "—" : e.getMotivo()) %></td>
                </tr>
            <% } %>
            </tbody>
        </table>
        </div>
    <% } %>
    </div>
</div>
<% } %>
<%@ include file="/WEB-INF/vistas/plantilla/pie.jspf" %>
