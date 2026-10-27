<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.*, java.time.*, java.net.URLEncoder, modelo.dto.*, modelo.citas.FechaTexto" %>
<%--
    ASISTENTE DE RESERVA (RF-04, RF-05) y de REPROGRAMACIÓN (RF-06).
    Lo usan recepción (con el paso "paciente") y el paciente desde el portal.
    Un paso a la vez, con la siguiente opción ya sugerida (el primer día con turnos):
    recepción reserva en 5 interacciones o menos (RNF-09). Los turnos ocupados se ven,
    pero no se pueden elegir (Nielsen #5); el clic en un turno pide confirmación.
--%>
<%@ include file="/WEB-INF/vistas/plantilla/cabecera.jspf" %>
<%!
    private String enc(String v) {
        try { return URLEncoder.encode(v == null ? "" : v, "UTF-8"); } catch (Exception e) { return ""; }
    }
%>
<%
    String rutaCitas = (String) request.getAttribute("rutaCitas");
    boolean modoPaciente = Boolean.TRUE.equals(request.getAttribute("modoPaciente"));
    CitaDTO mover = (CitaDTO) request.getAttribute("citaQueSeMueve");
    boolean reprogramar = mover != null;
    PacienteDTO paciente = (PacienteDTO) request.getAttribute("pacienteSel");
    @SuppressWarnings("unchecked") PaginaDTO<PacienteDTO> encontrados = (PaginaDTO<PacienteDTO>) request.getAttribute("pacientesEncontrados");
    @SuppressWarnings("unchecked") List<OpcionDTO> especialidades = (List<OpcionDTO>) request.getAttribute("especialidades");
    Integer espSel = (Integer) request.getAttribute("espSel");
    @SuppressWarnings("unchecked") List<MedicoDTO> medicos = (List<MedicoDTO>) request.getAttribute("medicos");
    Integer medicoSel = (Integer) request.getAttribute("medicoSel");
    @SuppressWarnings("unchecked") List<DiaDisponibleDTO> dias = (List<DiaDisponibleDTO>) request.getAttribute("dias");
    LocalDate desde = (LocalDate) request.getAttribute("desde");
    LocalDate fechaSel = (LocalDate) request.getAttribute("fechaSel");
    @SuppressWarnings("unchecked") List<AgendaMedicoDTO> turnos = (List<AgendaMedicoDTO>) request.getAttribute("turnos");
    LocalDate hoy = (LocalDate) request.getAttribute("hoy");
    LocalDate ultimoDia = (LocalDate) request.getAttribute("ultimoDia");
    int horas = (Integer) request.getAttribute("horasCancelacion");
    String q = request.getParameter("q") == null ? "" : request.getParameter("q").trim();
    @SuppressWarnings("unchecked") Map<Integer, PrecioConsultaDTO> precios = (Map<Integer, PrecioConsultaDTO>) request.getAttribute("precios");
    if (precios == null) precios = Map.of();
    PrecioConsultaDTO precioSel = espSel == null ? null : precios.getOrDefault(espSel, PrecioConsultaDTO.SIN_PRECIO);

    boolean pasoPacienteListo = modoPaciente || reprogramar || paciente != null;
    String nombreEsp = "";
    if (espSel != null) for (OpcionDTO o : especialidades) if (o.getId() == espSel) nombreEsp = o.getNombre();

    // Base de los enlaces del asistente: conserva lo ya elegido
    String base = rutaCitas + "?accion=" + (reprogramar ? "reprogramar&id=" + mover.getIdCita() : "reservar")
                + (paciente != null && !modoPaciente && !reprogramar ? "&paciente=" + paciente.getIdPaciente() : "");
    String conEsp = base + (espSel != null && !reprogramar ? "&esp=" + espSel : "");
    String conMedico = conEsp + (medicoSel != null ? "&medico=" + medicoSel : "");

    int pasoActual = !pasoPacienteListo ? 1 : (espSel == null ? 2 : 3);
    String[] nombresPasos = modoPaciente || reprogramar
            ? (reprogramar ? new String[]{"Misma especialidad", "Nuevo día y hora", "Confirmar"} : new String[]{"Especialidad", "Día y hora", "Confirmar"})
            : new String[]{"Paciente", "Especialidad", "Día y hora", "Confirmar"};
    int desplazamiento = modoPaciente || reprogramar ? 1 : 0;   // sin paso "Paciente"
%>

<div class="card">
    <div class="card-body py-2 d-flex flex-wrap align-items-center">
        <ol class="pasos mr-auto my-1" aria-label="Pasos para reservar">
            <% for (int i = 0; i < nombresPasos.length; i++) {
                   int n = i + 1 + desplazamiento;
                   String clase = n < pasoActual ? "hecho" : (n == pasoActual ? "actual" : ""); %>
            <li class="<%= clase %>" <%= n == pasoActual ? "aria-current=\"step\"" : "" %>><span class="n"><%= n < pasoActual ? "<i class=\"fas fa-check\"></i>" : String.valueOf(i + 1) %></span><%= nombresPasos[i] %></li>
            <% } %>
        </ol>
        <a href="<%= reprogramar ? rutaCitas + "?accion=ver&id=" + mover.getIdCita() : rutaCitas %>" class="btn btn-link btn-sm my-1">
            <i class="fas fa-times mr-1"></i><%= reprogramar ? "No cambiar" : "Salir sin reservar" %></a>
    </div>
</div>

<% if (reprogramar) { %>
<div class="callout callout-info">
    <h5 class="mb-1"><i class="fas fa-calendar-alt mr-2"></i>Cita actual <%= EscapeHtml.texto(mover.getCodigo()) %></h5>
    <p class="mb-0"><strong><%= FechaTexto.relativaCompleta(mover.getFecha(), hoy) %> a las <%= FechaTexto.hora(mover.getHoraInicio()) %></strong>
        · <%= EscapeHtml.texto(mover.getEspecialidad()) %> · Dr(a). <%= EscapeHtml.texto(mover.getNombreMedico()) %>
        <% if (!modoPaciente) { %> · Paciente: <strong><%= EscapeHtml.texto(mover.getNombrePaciente()) %></strong><% } %></p>
    <small class="text-muted">Elige el nuevo día y hora. El horario actual se libera solo cuando confirmes el cambio.</small>
</div>
<% } %>

<%-- ===================== PASO: PACIENTE (solo recepción) ===================== --%>
<% if (!modoPaciente && !reprogramar) { %>
<div class="card <%= paciente == null ? "card-outline card-primary" : "" %>">
    <div class="card-header"><h3 class="card-title"><span class="badge badge-<%= paciente == null ? "primary" : "success" %> mr-2">1</span>Paciente</h3></div>
    <div class="card-body">
    <% if (paciente != null) { %>
        <div class="d-flex flex-wrap align-items-center">
            <div class="mr-auto">
                <strong class="h6 mb-0"><%= EscapeHtml.texto(paciente.getNombreCompleto()) %></strong>
                <div class="small text-muted">DNI <%= EscapeHtml.texto(paciente.getDni()) %> · <%= EscapeHtml.texto(paciente.getNumeroHistoria()) %>
                    · <%= EscapeHtml.texto(paciente.getNombreCobertura()) %>
                    <% if (paciente.getCorreo().isBlank()) { %> · <span class="text-warning"><i class="fas fa-envelope mr-1"></i>sin correo (no recibirá la confirmación)</span><% } %></div>
            </div>
            <a href="<%= rutaCitas %>?accion=reservar" class="btn btn-outline-secondary btn-sm"><i class="fas fa-exchange-alt mr-1"></i>Cambiar paciente</a>
        </div>
    <% } else { %>
        <form method="GET" action="<%= rutaCitas %>" role="search">
            <input type="hidden" name="accion" value="reservar">
            <label for="q" class="small text-muted">Busca al paciente por DNI, nombre o N.º de historia</label>
            <div class="input-group">
                <input type="search" id="q" name="q" class="form-control" maxlength="60" autofocus value="<%= EscapeHtml.texto(q) %>"
                       placeholder="Ej. 45678912 o Torres">
                <div class="input-group-append"><button type="submit" class="btn btn-primary"><i class="fas fa-search mr-1"></i>Buscar</button></div>
            </div>
        </form>
        <% if (encontrados != null) { %>
            <% if (encontrados.getElementos().isEmpty()) { %>
            <div class="text-center py-3">
                <p class="mb-1"><strong>No encontramos a «<%= EscapeHtml.texto(q) %>».</strong></p>
                <a href="<%= ctx %>/recepcion/pacientes?accion=nuevo<%= q.matches("[0-9]{8}") ? "&dni=" + q : "" %>" class="btn btn-outline-primary btn-sm">
                    <i class="fas fa-user-plus mr-1"></i>Registrar paciente nuevo</a>
            </div>
            <% } else { %>
            <div class="list-group mt-3">
                <% for (PacienteDTO p : encontrados.getElementos()) { %>
                <a href="<%= rutaCitas %>?accion=reservar&paciente=<%= p.getIdPaciente() %>" class="list-group-item list-group-item-action d-flex align-items-center">
                    <div class="mr-auto"><strong><%= EscapeHtml.texto(p.getNombreCompleto()) %></strong>
                        <div class="small text-muted">DNI <%= EscapeHtml.texto(p.getDni()) %> · <%= EscapeHtml.texto(p.getNumeroHistoria()) %> · <%= EscapeHtml.texto(p.getNombreCobertura()) %></div></div>
                    <span class="btn btn-primary btn-sm">Elegir</span>
                </a>
                <% } %>
            </div>
            <% if (encontrados.getTotal() > encontrados.getElementos().size()) { %>
            <p class="small text-muted mt-2 mb-0">Hay <%= encontrados.getTotal() %> coincidencias: escribe el DNI para encontrarlo directo.</p>
            <% } %>
            <% } %>
        <% } %>
    <% } %>
    </div>
</div>
<% } %>

<% if (pasoPacienteListo) { %>
<%-- ===================== PASO: ESPECIALIDAD ===================== --%>
<% if (!reprogramar) { %>
<div class="card <%= espSel == null ? "card-outline card-primary" : "" %>">
    <div class="card-header"><h3 class="card-title"><span class="badge badge-<%= espSel == null ? "primary" : "success" %> mr-2"><%= modoPaciente ? 1 : 2 %></span>¿Qué especialidad necesitas?</h3></div>
    <div class="card-body">
    <% if (especialidades.isEmpty()) { %>
        <div class="estado-vacio py-3">
            <i class="fas fa-user-md text-muted"></i>
            <p class="mb-1"><strong>Por ahora no hay médicos con horario disponible.</strong></p>
            <p class="text-muted mb-0"><%= modoPaciente ? "Comunícate con recepción para separar tu cita." : "El administrador debe registrarlos en «Médicos y horarios»." %></p>
        </div>
    <% } else { %>
        <div class="opciones-grid">
        <% for (OpcionDTO e : especialidades) { boolean activo = espSel != null && espSel == e.getId(); %>
            <a class="opcion-btn <%= activo ? "activo" : "" %>" href="<%= base %>&esp=<%= e.getId() %>" <%= activo ? "aria-current=\"true\"" : "" %>>
                <i class="fas fa-stethoscope"></i>
                <% PrecioConsultaDTO pc = precios.getOrDefault(e.getId(), PrecioConsultaDTO.SIN_PRECIO); %>
                <span><strong><%= EscapeHtml.texto(e.getNombre()) %></strong>
                    <small class="d-block text-muted"><%= e.getExtra() %> <%= "1".equals(e.getExtra()) ? "médico" : "médicos" %></small>
                    <span class="precio-opcion"><% if (pc.isDefinido()) { %>S/ <%= PrecioConsultaDTO.soles(pc.getAPagar()) %><% if (pc.isConCobertura()) { %> <s class="text-muted font-weight-normal">S/ <%= PrecioConsultaDTO.soles(pc.getPrecioLista()) %></s><% } %><% } else { %><span class="text-muted font-weight-normal">Precio en caja</span><% } %></span></span>
            </a>
        <% } %>
        </div>
    <% } %>
    </div>
</div>
<% } %>

<%-- ===================== PASO: DÍA Y HORA ===================== --%>
<% if (espSel != null) { %>
<div class="card card-outline card-primary" id="paso-hora">
    <div class="card-header d-flex flex-wrap align-items-center">
        <h3 class="card-title mr-auto"><span class="badge badge-primary mr-2"><%= reprogramar || modoPaciente ? 2 : 3 %></span>Elige el día y la hora<%= nombreEsp.isEmpty() ? "" : " · " + EscapeHtml.texto(nombreEsp) %></h3>
    </div>
    <div class="card-body">
        <% if (precioSel != null) { %>
        <div class="precio-consulta mb-3" role="note">
            <i class="fas fa-tag" aria-hidden="true"></i>
            <div>
            <% if (precioSel.isDefinido()) { %>
                <span class="text-muted">Precio de la consulta<%= reprogramar ? "" : (modoPaciente ? "" : " para este paciente") %>:</span>
                <strong class="h5 mb-0 ml-1">S/ <%= PrecioConsultaDTO.soles(precioSel.getAPagar()) %></strong>
                <% if (precioSel.isConCobertura()) { %>
                <span class="small text-muted ml-1">Precio normal S/ <%= PrecioConsultaDTO.soles(precioSel.getPrecioLista()) %> · <%= EscapeHtml.texto(precioSel.getDetalleCobertura()) %></span>
                <% } %>
                <div class="small text-muted">Incluye IGV. Se paga en caja el día de la consulta<%= modoPaciente ? ", con efectivo, tarjeta o billetera digital" : "" %>.</div>
            <% } else { %>
                <span class="text-muted">El precio de esta consulta te lo indicarán en caja.</span>
            <% } %>
            </div>
        </div>
        <% } %>
        <% if (medicos != null && medicos.size() > 1) { %>
        <div class="mb-3">
            <span class="small text-muted d-block mb-1">Médico</span>
            <div class="opciones-grid">
                <a class="opcion-btn <%= medicoSel == null ? "activo" : "" %>" href="<%= conEsp %><%= fechaSel == null ? "" : "&fecha=" + fechaSel %>">
                    <i class="fas fa-users"></i><span><strong>Cualquiera</strong><small class="d-block text-muted">el primero disponible</small></span></a>
                <% for (MedicoDTO m : medicos) { boolean act = medicoSel != null && medicoSel == m.getIdMedico(); %>
                <a class="opcion-btn <%= act ? "activo" : "" %>" href="<%= conEsp %>&medico=<%= m.getIdMedico() %><%= fechaSel == null ? "" : "&fecha=" + fechaSel %>">
                    <%= m.isTieneFoto() ? "<img class=\"avatar-iniciales avatar-foto avatar-sm\" src=\"" + ctx + "/foto?u=" + m.getIdUsuario() + "\" alt=\"\" loading=\"lazy\">"
                                        : "<span class=\"avatar-iniciales avatar-sm\" aria-hidden=\"true\">" + EscapeHtml.texto(m.getIniciales()) + "</span>" %>
                    <span><strong><%= EscapeHtml.texto(m.getNombreConTitulo()) %></strong><small class="d-block text-muted"><%= EscapeHtml.texto(m.getConsultorio()) %></small></span>
                </a>
                <% } %>
            </div>
        </div>
        <% } %>

        <span class="small text-muted d-block mb-1">Día <span class="ml-1">(se muestran los turnos libres de cada día)</span></span>
        <div class="d-flex align-items-center">
            <% if (desde != null && desde.isAfter(hoy)) { %>
            <a class="btn btn-light btn-sm mr-1" href="<%= conMedico %>&desde=<%= desde.minusDays(14).isBefore(hoy) ? hoy : desde.minusDays(14) %>" title="Días anteriores" aria-label="Días anteriores"><i class="fas fa-chevron-left"></i></a>
            <% } %>
            <div class="tira-dias flex-grow-1" role="list">
            <% for (DiaDisponibleDTO d : dias) {
                   boolean act = d.getFecha().equals(fechaSel);
                   String clase = act ? "activo" : (d.getLibres() == 0 ? "sin-turnos" : "");
                   String etiqueta = d.getFecha().equals(hoy) ? "Hoy" : (d.getFecha().equals(hoy.plusDays(1)) ? "Mañana" : FechaTexto.diaCorto(d.getFecha())); %>
                <a role="listitem" class="dia-btn <%= clase %>" href="<%= conMedico %>&desde=<%= desde %>&fecha=<%= d.getFecha() %>#paso-hora"
                   <%= d.getLibres() == 0 ? "aria-disabled=\"true\" tabindex=\"-1\"" : "" %> <%= act ? "aria-current=\"date\"" : "" %>
                   title="<%= FechaTexto.largaTitulo(d.getFecha()) %>">
                    <span class="dia-nombre"><%= etiqueta %></span>
                    <span class="dia-num"><%= d.getFecha().getDayOfMonth() %></span>
                    <span class="dia-nombre"><%= FechaTexto.mesCorto(d.getFecha()) %></span>
                    <span class="dia-libres"><%= !d.isAtiende() ? "No atiende" : (d.getLibres() == 0 ? "Lleno" : d.getLibres() + " libres") %></span>
                </a>
            <% } %>
            </div>
            <% if (!dias.isEmpty() && dias.get(dias.size() - 1).getFecha().isBefore(ultimoDia)) { %>
            <a class="btn btn-light btn-sm ml-1" href="<%= conMedico %>&desde=<%= dias.get(dias.size() - 1).getFecha().plusDays(1) %>" title="Más días" aria-label="Más días"><i class="fas fa-chevron-right"></i></a>
            <% } %>
        </div>

        <hr>
        <% if (fechaSel == null) { %>
            <div class="estado-vacio py-3">
                <i class="far fa-calendar-times text-muted"></i>
                <p class="mb-1"><strong>No hay turnos libres en estos días.</strong></p>
                <p class="text-muted mb-0">Usa la flecha <i class="fas fa-chevron-right"></i> para ver los días siguientes<%= medicoSel != null ? " o elige «Cualquiera» en médico" : "" %>.</p>
            </div>
        <% } else if (turnos.isEmpty()) { %>
            <div class="estado-vacio py-3">
                <i class="far fa-calendar-times text-muted"></i>
                <p class="mb-1"><strong>El <%= FechaTexto.larga(fechaSel) %> no hay atención<%= fechaSel.equals(hoy) ? " (o ya pasaron los turnos de hoy)" : "" %>.</strong></p>
                <p class="text-muted mb-0">Elige otro día en la tira de arriba.</p>
            </div>
        <% } else { %>
            <h4 class="h6 mb-3"><i class="far fa-clock mr-1"></i>Turnos del <strong><%= FechaTexto.relativaCompleta(fechaSel, hoy).toLowerCase(FechaTexto.ES) %></strong></h4>
            <div class="row">
            <% for (AgendaMedicoDTO a : turnos) { MedicoDTO m = a.getMedico(); %>
                <div class="col-lg-<%= turnos.size() == 1 ? "12" : "6" %>">
                <div class="card medico-turnos shadow-none border">
                    <div class="card-header">
                        <%= m.isTieneFoto() ? "<img class=\"avatar-iniciales avatar-foto foto-ampliable\" src=\"" + ctx + "/foto?u=" + m.getIdUsuario() + "\" alt=\"Foto de " + EscapeHtml.texto(m.getNombreConTitulo()) + "\" loading=\"lazy\">"
                                            : "<span class=\"avatar-iniciales\" aria-hidden=\"true\">" + EscapeHtml.texto(m.getIniciales()) + "</span>" %>
                        <div>
                            <strong><%= EscapeHtml.texto(m.getNombreConTitulo()) %></strong>
                            <div class="small text-muted"><%= EscapeHtml.texto(m.getEspecialidad()) %> · <%= EscapeHtml.texto(m.getConsultorio()) %> · citas de <%= m.getDuracionCita() %> min</div>
                            <% if (!m.getDescripcion().isBlank()) { %><div class="small"><%= EscapeHtml.texto(m.getDescripcion()) %></div><% } %>
                        </div>
                    </div>
                    <div class="card-body">
                    <% if (a.getLibres() == 0) { %>
                        <p class="text-muted mb-0">Sin turnos libres este día.</p>
                    <% } %>
                    <% for (int bloque = 0; bloque < 2; bloque++) {
                           boolean maniana = bloque == 0;
                           List<TurnoDTO> grupo = new ArrayList<>();
                           for (TurnoDTO t : a.getTurnos()) if (t.isManiana() == maniana) grupo.add(t);
                           if (grupo.isEmpty() || a.getLibres() == 0) continue; %>
                        <span class="small text-muted d-block mb-1"><i class="fas fa-<%= maniana ? "sun" : "cloud-sun" %> mr-1"></i><%= maniana ? "Mañana" : "Tarde" %></span>
                        <div class="turnos-grid mb-2">
                        <% for (TurnoDTO t : grupo) {
                               String horaTxt = FechaTexto.hora(t.getHoraInicio());
                               if (t.isLibre()) { %>
                            <button type="button" class="btn btn-outline-primary turno-btn" data-turno
                                    data-medico="<%= m.getIdMedico() %>" data-fecha="<%= t.getFecha() %>" data-hora="<%= FechaTexto.horaIso(t.getHoraInicio()) %>"
                                    data-fecha-texto="<%= EscapeHtml.texto(FechaTexto.relativaCompleta(t.getFecha(), hoy)) %>"
                                    data-hora-texto="<%= horaTxt %> – <%= FechaTexto.hora(t.getHoraFin()) %>"
                                    data-medico-nombre="<%= EscapeHtml.texto(m.getNombreConTitulo() + " · " + m.getEspecialidad()) %>"
                                    data-lugar="<%= EscapeHtml.texto(m.getConsultorio()) %>"><%= horaTxt %></button>
                            <% } else { %>
                            <button type="button" class="btn btn-light turno-btn ocupado" disabled title="Ocupado"><%= horaTxt %><span class="sr-only"> (ocupado)</span></button>
                            <% } %>
                        <% } %>
                        </div>
                    <% } %>
                    </div>
                </div>
                </div>
            <% } %>
            </div>
            <p class="small text-muted mb-0"><i class="fas fa-info-circle mr-1"></i>Los horarios tachados ya están ocupados.
                <%= modoPaciente ? "Podrás cancelar o cambiar tu cita desde «Mis citas» hasta " + horas + " horas antes." : "" %></p>
        <% } %>
    </div>
</div>
<% } %>
<% } %>

<%-- ===================== CONFIRMAR ===================== --%>
<div class="modal fade" id="modalReservar" tabindex="-1" role="dialog" aria-labelledby="tituloReservar" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered" role="document">
        <form class="modal-content" method="POST" action="<%= rutaCitas %>" id="formReservar" data-un-envio>
            <div class="modal-header">
                <h5 class="modal-title" id="tituloReservar"><i class="fas fa-calendar-check text-primary mr-2"></i><%= reprogramar ? "¿Cambiar la cita a este horario?" : "Confirma la cita" %></h5>
                <button type="button" class="close" data-dismiss="modal" aria-label="Cerrar"><span aria-hidden="true">&times;</span></button>
            </div>
            <div class="modal-body">
                <dl class="row resumen-reserva mb-0">
                    <% if (!modoPaciente) { %>
                    <dt class="col-4">Paciente</dt><dd class="col-8"><%= EscapeHtml.texto(reprogramar ? mover.getNombrePaciente() : (paciente == null ? "" : paciente.getNombreCompleto())) %></dd>
                    <% } %>
                    <dt class="col-4">Día</dt><dd class="col-8" id="resFecha"></dd>
                    <dt class="col-4">Hora</dt><dd class="col-8" id="resHora"></dd>
                    <dt class="col-4">Médico</dt><dd class="col-8" id="resMedico"></dd>
                    <dt class="col-4">Lugar</dt><dd class="col-8" id="resLugar"></dd>
                    <% if (precioSel != null) { %>
                    <dt class="col-4">Precio</dt>
                    <dd class="col-8"><% if (precioSel.isDefinido()) { %>S/ <%= PrecioConsultaDTO.soles(precioSel.getAPagar()) %>
                        <small class="d-block text-muted font-weight-normal"><%= precioSel.isConCobertura() ? EscapeHtml.texto(precioSel.getDetalleCobertura()) + " · " : "" %>se paga en caja el día de la consulta</small>
                        <% } else { %><span class="font-weight-normal text-muted">Te lo indicarán en caja</span><% } %></dd>
                    <% } %>
                    <% if (reprogramar) { %>
                    <dt class="col-4 text-muted">Antes</dt><dd class="col-8 text-muted font-weight-normal"><s><%= FechaTexto.relativaCompleta(mover.getFecha(), hoy) %> · <%= FechaTexto.hora(mover.getHoraInicio()) %></s></dd>
                    <% } %>
                </dl>
                <% if (!reprogramar) { %>
                <div class="form-group mt-2 mb-0">
                    <label for="motivoCita" class="small text-muted"><%= modoPaciente ? "¿Qué te gustaría consultar? (opcional)" : "Motivo de la consulta (opcional)" %></label>
                    <textarea class="form-control" id="motivoCita" name="motivo" rows="2" maxlength="250" placeholder="Ej. dolor de garganta desde hace 3 días"></textarea>
                </div>
                <% } %>
                <% if (modoPaciente) { %>
                <p class="small text-muted mt-2 mb-0"><i class="fas fa-info-circle mr-1"></i>Llega 15 minutos antes con tu DNI. Si no puedes asistir,
                    cancela o cambia la cita hasta <%= horas %> horas antes desde «Mis citas».</p>
                <% } %>
            </div>
            <div class="modal-footer">
                <input type="hidden" name="accion" value="<%= reprogramar ? "reprogramar" : "reservar" %>">
                <% if (reprogramar) { %><input type="hidden" name="id" value="<%= mover.getIdCita() %>"><% } %>
                <% if (paciente != null && !modoPaciente) { %><input type="hidden" name="paciente" value="<%= paciente.getIdPaciente() %>"><% } %>
                <% if (espSel != null) { %><input type="hidden" name="esp" value="<%= espSel %>"><% } %>
                <input type="hidden" name="medico">
                <input type="hidden" name="fecha">
                <input type="hidden" name="hora">
                <button type="button" class="btn btn-outline-secondary" data-dismiss="modal">Elegir otra hora</button>
                <button type="submit" class="btn btn-primary" id="btnConfirmarReserva"><i class="fas fa-check mr-1"></i><%= reprogramar ? "Sí, cambiar" : "Confirmar reserva" %></button>
            </div>
        </form>
    </div>
</div>
<script src="<%= ctx %>/js/citas.js?v=<%= VersionAplicacion.RECURSOS %>" defer></script>
<%@ include file="/WEB-INF/vistas/plantilla/pie.jspf" %>
