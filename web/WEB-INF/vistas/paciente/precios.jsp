<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List, java.net.URLEncoder, modelo.dto.*, modelo.cobranza.CotizadorConsulta" %>
<%@ include file="/WEB-INF/vistas/plantilla/cabecera.jspf" %>
<%
    @SuppressWarnings("unchecked") PaginaDTO<ProductoServicioDTO> pagina = (PaginaDTO<ProductoServicioDTO>) request.getAttribute("pagina");
    @SuppressWarnings("unchecked") List<OpcionDTO> categorias = (List<OpcionDTO>) request.getAttribute("categorias");
    FiltroBusquedaDTO filtro = (FiltroBusquedaDTO) request.getAttribute("filtro");
    CoberturaDTO cobertura = (CoberturaDTO) request.getAttribute("cobertura");
    boolean errorDatos = request.getAttribute("errorDatos") != null;
    String urlLista = ctx + "/paciente/precios";
    String q = filtro == null || filtro.getTexto() == null ? "" : filtro.getTexto();
    Integer categoriaSel = filtro == null ? null : filtro.getGrupo();
    String base = "q=" + URLEncoder.encode(q, "UTF-8") + (categoriaSel == null ? "" : "&categoria=" + categoriaSel);
    boolean conCobertura = cobertura != null && cobertura.getPorcentaje() != null && cobertura.getPorcentaje().signum() > 0;
%>
<div class="card">
    <div class="card-header d-flex flex-wrap align-items-center">
        <h3 class="card-title mr-auto"><i class="fas fa-tags mr-2"></i>Precios de nuestros servicios</h3>
        <a href="<%= ctx %>/paciente/citas?accion=reservar" class="btn btn-primary btn-sm"><i class="fas fa-calendar-plus mr-1"></i>Reservar cita</a>
    </div>
    <div class="card-body border-bottom pb-2">
        <p class="small text-muted mb-2"><i class="fas fa-info-circle mr-1"></i>Precios en soles, con IGV incluido.
            <% if (conCobertura) { %>Con tu cobertura <strong><%= EscapeHtml.texto(cobertura.getNombre()) %></strong> se muestra también lo que pagarías tú.<% } %></p>
        <form method="GET" action="<%= urlLista %>" class="form-row align-items-end" role="search">
            <div class="col-md-6 mb-2">
                <label for="q" class="sr-only">Buscar</label>
                <div class="input-group">
                    <div class="input-group-prepend"><span class="input-group-text"><i class="fas fa-search"></i></span></div>
                    <input type="search" id="q" name="q" class="form-control" maxlength="60" placeholder="Ej. consulta, hemograma, radiografía" value="<%= EscapeHtml.texto(q) %>">
                    <% if (categoriaSel != null) { %><input type="hidden" name="categoria" value="<%= categoriaSel %>"><% } %>
                    <div class="input-group-append"><button type="submit" class="btn btn-secondary">Buscar</button></div>
                </div>
            </div>
        </form>
        <div class="chips-estado mb-2" aria-label="Categorías">
            <a class="chip-estado <%= categoriaSel == null ? "activo" : "" %>" href="<%= urlLista %>?q=<%= URLEncoder.encode(q, "UTF-8") %>">Todos</a>
            <% if (categorias != null) for (OpcionDTO c : categorias) { %>
            <a class="chip-estado <%= categoriaSel != null && categoriaSel == c.getId() ? "activo" : "" %>" href="<%= urlLista %>?q=<%= URLEncoder.encode(q, "UTF-8") %>&categoria=<%= c.getId() %>"><%= EscapeHtml.texto(c.getNombre()) %></a>
            <% } %>
        </div>
    </div>
    <div class="card-body p-0">
    <% if (errorDatos || pagina == null) { %>
        <div class="estado-vacio"><i class="fas fa-plug text-warning"></i><p class="mb-1"><strong>No pudimos cargar los precios.</strong></p>
            <p class="text-muted mb-0">Recarga la página en unos minutos.</p></div>
    <% } else if (pagina.getElementos().isEmpty()) { %>
        <div class="estado-vacio"><i class="fas fa-search text-muted"></i><p class="mb-1"><strong>No encontramos servicios con «<%= EscapeHtml.texto(q) %>».</strong></p>
            <a href="<%= urlLista %>">Ver todos</a></div>
    <% } else { %>
        <div class="table-responsive">
        <table class="table table-hover mb-0">
            <thead class="thead-light"><tr>
                <th scope="col">Servicio</th>
                <th scope="col" class="d-none d-md-table-cell">Categoría</th>
                <th scope="col" class="text-right">Precio</th>
                <% if (conCobertura) { %><th scope="col" class="text-right">Tú pagas</th><% } %>
            </tr></thead>
            <tbody>
            <% for (ProductoServicioDTO s : pagina.getElementos()) {
                   PrecioConsultaDTO cotizado = CotizadorConsulta.cotizar(s.getPrecio(), cobertura); %>
                <tr>
                    <td><strong><%= EscapeHtml.texto(s.getNombre()) %></strong>
                        <% if (!s.getNombreEspecialidad().isEmpty()) { %><div class="small text-muted"><%= EscapeHtml.texto(s.getNombreEspecialidad()) %></div><% } %>
                        <% if (!s.getDescripcion().isEmpty()) { %><div class="small text-muted"><%= EscapeHtml.texto(s.getDescripcion()) %></div><% } %></td>
                    <td class="d-none d-md-table-cell"><span class="badge badge-light border"><%= EscapeHtml.texto(s.getNombreCategoria()) %></span></td>
                    <td class="text-right text-nowrap <%= conCobertura ? "text-muted" : "font-weight-bold" %>">S/ <%= PrecioConsultaDTO.soles(s.getPrecio()) %></td>
                    <% if (conCobertura) { %><td class="text-right text-nowrap"><span class="precio-tag">S/ <%= PrecioConsultaDTO.soles(cotizado.getAPagar()) %></span></td><% } %>
                </tr>
            <% } %>
            </tbody>
        </table>
        </div>
    <% } %>
    </div>
    <%@ include file="/WEB-INF/vistas/comun/paginacion.jspf" %>
</div>
<%@ include file="/WEB-INF/vistas/plantilla/pie.jspf" %>
