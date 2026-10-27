<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.*, java.time.*, java.net.URLEncoder, modelo.dto.*, modelo.citas.FechaTexto, modelo.citas.vista.VistaAgendaStrategy, controlador.citas.AccionesCita, controlador.citas.AgendaCitas" %>
<%--
    AGENDA DE CITAS (RF-04, RF-08, RF-10, RF-18). La usan:
      - Recepción y Administrador (/recepcion/citas): todos los médicos, llegada, cobro, reprogramar, cancelar.
      - Médico (/medico/agenda): solo sus citas; registra la atención.
    Vista por defecto: HOY. Botones Día | Semana | Mes (Strategy); el mes solo muestra cuántas citas hay por día.
--%>
<%@ include file="/WEB-INF/vistas/plantilla/cabecera.jspf" %>
<%!
    private String enc(String v) {
        try { return URLEncoder.encode(v == null ? "" : v, "UTF-8"); } catch (Exception e) { return ""; }
    }
    private String nombreCorto(String n) {
        String[] p = n.trim().split("\\s+");
        return p.length <= 2 ? n : p[0] + " " + p[p.length >= 4 ? 2 : 1];
    }
%>
<%
    String rutaCitas = (String) request.getAttribute("rutaCitas");
    boolean errorDatos = request.getAttribute("errorDatos") != null;
    boolean modoMedico = "medico".equals(request.getAttribute("modoAgenda"));
    MedicoDTO medicoSesion = (MedicoDTO) request.getAttribute("medicoSesion");
    VistaAgendaStrategy vista = (VistaAgendaStrategy) request.getAttribute("vista");
    @SuppressWarnings("unchecked") List<VistaAgendaStrategy> vistas = (List<VistaAgendaStrategy>) request.getAttribute("vistas");
    LocalDate fecha = (LocalDate) request.getAttribute("fecha");
    LocalDate hoy = (LocalDate) request.getAttribute("hoy");
    LocalDateTime ahora = (LocalDateTime) request.getAttribute("ahora");
    if (vista == null) vista = modelo.citas.vista.VistaAgendaFactory.obtener("dia");
    if (vistas == null) vistas = modelo.citas.vista.VistaAgendaFactory.todas();
    if (hoy == null) hoy = LocalDate.now();
    if (fecha == null) fecha = hoy;
    if (ahora == null) ahora = LocalDateTime.now();
    FiltroCitaDTO filtro = (FiltroCitaDTO) request.getAttribute("filtroCita");
    @SuppressWarnings("unchecked") List<CitaDTO> citas = (List<CitaDTO>) request.getAttribute("citas");
    @SuppressWarnings("unchecked") Map<LocalDate, ResumenCitasDTO> porDia = (Map<LocalDate, ResumenCitasDTO>) request.getAttribute("porDia");
    ResumenCitasDTO resumen = (ResumenCitasDTO) request.getAttribute("resumen");
    @SuppressWarnings("unchecked") List<MedicoDTO> medicosFiltro = (List<MedicoDTO>) request.getAttribute("medicosFiltro");
    @SuppressWarnings("unchecked") List<MedicoDTO> medicosAdmin = (List<MedicoDTO>) request.getAttribute("medicosFiltroAdmin");
    Integer horasAttr = (Integer) request.getAttribute("horasCancelacion");
    int horas = horasAttr == null ? 24 : horasAttr;
    boolean agendaLista = !modoMedico || (medicoSesion != null && medicoSesion.isConfigurado());

    // Filtros que se conservan al cambiar de vista o de fecha
    String medicoSel = filtro == null || filtro.getIdMedico() == null ? "" : String.valueOf(filtro.getIdMedico());
    String estadoSel = filtro == null || filtro.getEstado() == null ? "" : filtro.getEstado();
    String q = filtro == null || filtro.getTexto() == null ? "" : filtro.getTexto();
    String filtros = (medicoSel.isEmpty() || (modoMedico && medicosAdmin == null) ? "" : "&medico=" + medicoSel)
                   + (estadoSel.isEmpty() ? "" : "&estado=" + estadoSel) + (q.isEmpty() ? "" : "&q=" + enc(q));
    String codigoVista = vista == null ? "dia" : vista.getCodigo();
    String regreso = AgendaCitas.parametros(request);
    String[][] leyenda = {{CitaDTO.RESERVADA, "Por llegar"}, {CitaDTO.CONFIRMADA, "En sala"}, {CitaDTO.ATENDIDA, "Atendida"},
                          {CitaDTO.NO_ASISTIO, "No asistió"}, {CitaDTO.CANCELADA, "Cancelada"}};
%>
<% if (!agendaLista) { %>
<div class="card">
    <div class="card-body estado-vacio">
        <i class="fas fa-user-md text-muted"></i>
        <p class="mb-1"><strong>Tu agenda aún no está lista.</strong></p>
        <p class="text-muted mb-0">El administrador debe registrar tu especialidad, consultorio y horario de atención
            en <em>Médicos y horarios</em>. Cuando lo haga, aquí verás tus citas.</p>
    </div>
</div>
<% } else { %>
<div class="card">
    <div class="card-header">
        <div class="agenda-barra">
            <div class="btn-group" role="group" aria-label="Moverse en la agenda">
                <a class="btn btn-outline-secondary btn-sm" href="<%= rutaCitas %>?vista=<%= codigoVista %>&fecha=<%= vista.anterior(fecha) %><%= filtros %>" title="Anterior (tecla ←)" aria-label="Anterior" data-atajo="anterior" aria-keyshortcuts="ArrowLeft"><i class="fas fa-chevron-left"></i></a>
                <a class="btn btn-outline-secondary btn-sm <%= vista.desde(hoy).equals(vista.desde(fecha)) ? "active" : "" %>" href="<%= rutaCitas %>?vista=<%= codigoVista %>&fecha=<%= hoy %><%= filtros %>" data-atajo="hoy" aria-keyshortcuts="h" title="Ir a hoy (tecla H)">Hoy</a>
                <a class="btn btn-outline-secondary btn-sm" href="<%= rutaCitas %>?vista=<%= codigoVista %>&fecha=<%= vista.siguiente(fecha) %><%= filtros %>" title="Siguiente (tecla →)" aria-label="Siguiente" data-atajo="siguiente" aria-keyshortcuts="ArrowRight"><i class="fas fa-chevron-right"></i></a>
            </div>
            <h2 class="titulo-agenda">
                <%= EscapeHtml.texto(vista.titulo(fecha)) %>
                <% if ("dia".equals(codigoVista) && fecha.equals(hoy)) { %><span class="badge badge-primary badge-hoy">HOY</span><% } %>
            </h2>
            <div class="btn-group btn-group-sm" role="group" aria-label="Tipo de vista">
                <% for (VistaAgendaStrategy v : vistas) { %>
                <a class="btn <%= v.getCodigo().equals(codigoVista) ? "btn-primary" : "btn-outline-primary" %>"
                   href="<%= rutaCitas %>?vista=<%= v.getCodigo() %>&fecha=<%= fecha %><%= filtros %>"
                   <%= v.getCodigo().equals(codigoVista) ? "aria-current=\"page\"" : "" %>><%= v.getNombre() %></a>
                <% } %>
            </div>
            <form method="GET" action="<%= rutaCitas %>" class="form-inline">
                <input type="hidden" name="vista" value="<%= codigoVista %>">
                <% if (!medicoSel.isEmpty()) { %><input type="hidden" name="medico" value="<%= medicoSel %>"><% } %>
                <% if (!estadoSel.isEmpty()) { %><input type="hidden" name="estado" value="<%= estadoSel %>"><% } %>
                <label for="irFecha" class="sr-only">Ir a una fecha</label>
                <input type="date" id="irFecha" name="fecha" class="form-control form-control-sm" value="<%= fecha %>" data-autoenviar title="Ir a una fecha">
            </form>
            <% if (!modoMedico) { %>
            <a href="<%= rutaCitas %>?accion=reservar" class="btn btn-primary btn-sm ml-md-auto"><i class="fas fa-calendar-plus mr-1"></i> Nueva cita</a>
            <% } %>
        </div>
    </div>

    <div class="card-body border-bottom py-2">
        <form method="GET" action="<%= rutaCitas %>" class="form-row align-items-end" role="search">
            <input type="hidden" name="vista" value="<%= codigoVista %>">
            <input type="hidden" name="fecha" value="<%= fecha %>">
            <% if (estadoSel.length() > 0) { %><input type="hidden" name="estado" value="<%= estadoSel %>"><% } %>
            <% if (medicosFiltro != null || medicosAdmin != null) {
                   List<MedicoDTO> lista = medicosFiltro != null ? medicosFiltro : medicosAdmin; %>
            <div class="col-sm-6 col-lg-4 mb-2">
                <label for="medico" class="small text-muted mb-1">Médico</label>
                <select id="medico" name="medico" class="form-control form-control-sm" data-autoenviar>
                    <% if (medicosFiltro != null) { %><option value="">Todos los médicos</option><% } %>
                    <% for (MedicoDTO m : lista) { %>
                    <option value="<%= m.getIdMedico() %>" <%= String.valueOf(m.getIdMedico()).equals(medicoSel) ? "selected" : "" %>>
                        <%= EscapeHtml.texto(m.getNombreConTitulo() + " · " + m.getEspecialidad()) %></option>
                    <% } %>
                </select>
            </div>
            <% } %>
            <% if (!"mes".equals(codigoVista)) { %>
            <div class="col-sm-6 col-lg-4 mb-2">
                <label for="q" class="small text-muted mb-1">Buscar paciente</label>
                <div class="input-group input-group-sm">
                    <input type="search" id="q" name="q" class="form-control" maxlength="60" value="<%= EscapeHtml.texto(q) %>"
                           placeholder="Nombre, DNI, historia o código (CT-…)">
                    <div class="input-group-append"><button class="btn btn-secondary" type="submit" aria-label="Buscar"><i class="fas fa-search"></i></button></div>
                </div>
            </div>
            <% } %>
            <% if (filtro != null && (filtro.getTexto() != null || filtro.getEstado() != null || (medicosFiltro != null && filtro.getIdMedico() != null))) { %>
            <div class="col-auto mb-2"><a href="<%= rutaCitas %>?vista=<%= codigoVista %>&fecha=<%= fecha %>" class="btn btn-link btn-sm">Quitar filtros</a></div>
            <% } %>
        </form>
        <% if (resumen != null) {
               String baseChip = rutaCitas + "?vista=" + codigoVista + "&fecha=" + fecha + (medicoSel.isEmpty() ? "" : "&medico=" + medicoSel) + (q.isEmpty() ? "" : "&q=" + enc(q));
               Object[][] chips = {
                   {"", "Todas", resumen.getProgramadas() + resumen.getCanceladas(), "fas fa-list"},
                   {CitaDTO.RESERVADA, "Por llegar", resumen.getReservadas(), CitaDTO.iconoEstado(CitaDTO.RESERVADA)},
                   {CitaDTO.CONFIRMADA, "En sala de espera", resumen.getConfirmadas(), CitaDTO.iconoEstado(CitaDTO.CONFIRMADA)},
                   {CitaDTO.ATENDIDA, "Atendidas", resumen.getAtendidas(), CitaDTO.iconoEstado(CitaDTO.ATENDIDA)},
                   {CitaDTO.NO_ASISTIO, "No asistieron", resumen.getNoAsistio(), CitaDTO.iconoEstado(CitaDTO.NO_ASISTIO)},
                   {CitaDTO.CANCELADA, "Canceladas", resumen.getCanceladas(), CitaDTO.iconoEstado(CitaDTO.CANCELADA)}}; %>
        <div class="chips-estado mt-1" aria-label="Resumen por estado (clic para filtrar)">
            <% for (Object[] ch : chips) {
                   boolean activo = ch[0].equals(estadoSel);
                   if (!activo && !"".equals(ch[0]) && (Integer) ch[2] == 0) continue; %>
            <a class="chip-estado <%= activo ? "activo" : "" %>" href="<%= baseChip %><%= "".equals(ch[0]) ? "" : "&estado=" + ch[0] %>">
                <i class="<%= ch[3] %>" aria-hidden="true"></i><span class="num"><%= ch[2] %></span> <%= ch[1] %></a>
            <% } %>
        </div>
        <% } %>
    </div>

    <div class="card-body p-0">
    <% if (errorDatos) { %>
        <div class="estado-vacio">
            <i class="fas fa-plug text-warning"></i>
            <p class="mb-1"><strong>No pudimos cargar la agenda.</strong></p>
            <p class="text-muted">Revisa la conexión con la base de datos y recarga la página.</p>
        </div>
    <% } else if ("mes".equals(codigoVista)) {
           LocalDate primero = vista.desde(fecha);
           LocalDate inicioGrid = primero.minusDays(primero.getDayOfWeek().getValue() - 1);
           LocalDate ultimo = vista.hasta(fecha);
           LocalDate finGrid = ultimo.plusDays(7 - ultimo.getDayOfWeek().getValue()); %>
        <div class="p-2 p-md-3">
            <div class="mes-grid" role="grid" aria-label="<%= EscapeHtml.texto(vista.titulo(fecha)) %>">
                <% for (String d : new String[]{"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"}) { %><div class="mes-encabezado" role="columnheader"><%= d %></div><% } %>
                <% for (LocalDate d = inicioGrid; !d.isAfter(finGrid); d = d.plusDays(1)) {
                       ResumenCitasDTO r = porDia == null ? null : porDia.get(d);
                       int total = r == null ? 0 : r.getProgramadas();
                       int pendientes = r == null ? 0 : r.getPendientes(); %>
                <a class="mes-celda <%= d.getMonth() != fecha.getMonth() ? "fuera-mes" : "" %> <%= d.equals(hoy) ? "es-hoy" : "" %> <%= d.getDayOfWeek().getValue() >= 6 ? "fin-semana" : "" %> <%= d.isBefore(hoy) ? "dia-pasado" : "" %>"
                   href="<%= rutaCitas %>?vista=dia&fecha=<%= d %><%= filtros %>" role="gridcell"
                   title="<%= FechaTexto.largaTitulo(d) %>: <%= total %> <%= total == 1 ? "cita" : "citas" %>">
                    <span class="mes-num"><%= d.getDayOfMonth() %></span>
                    <% if (d.equals(hoy)) { %><span class="badge badge-primary ml-1 align-text-top">Hoy</span><% } %>
                    <% if (total > 0) {
                           int atendidas = r.getAtendidas(), faltaron = r.getNoAsistio(); %>
                    <span class="mes-total"><strong><%= total %></strong> <small><%= total == 1 ? "cita" : "citas" %></small></span>
                    <span class="mes-barra" aria-hidden="true">
                        <% if (atendidas > 0) { %><span class="seg-ATENDIDA" style="flex:<%= atendidas %>"></span><% } %>
                        <% if (pendientes > 0) { %><span class="seg-RESERVADA" style="flex:<%= pendientes %>"></span><% } %>
                        <% if (faltaron > 0) { %><span class="seg-NO_ASISTIO" style="flex:<%= faltaron %>"></span><% } %>
                    </span>
                    <small class="mes-detalle text-muted d-block"><%= !d.isBefore(hoy) && pendientes > 0 ? pendientes + " por atender" : atendidas > 0 ? atendidas + " atendidas" : "" %></small>
                    <% } else if (d.getMonth() == fecha.getMonth() && !d.isBefore(hoy)) { %>
                    <small class="mes-libre d-block">Libre</small>
                    <% } %>
                </a>
                <% } %>
            </div>
            <div class="d-flex flex-wrap align-items-center mt-2 small text-muted" style="gap:.75rem">
                <span><i class="fas fa-info-circle mr-1"></i>Haz clic en un día para ver sus citas.</span>
                <span class="leyenda-mes"><span class="seg-ATENDIDA"></span>Atendidas <span class="seg-RESERVADA"></span>Por atender <span class="seg-NO_ASISTIO"></span>No asistieron</span>
            </div>
        </div>
    <% } else if ("semana".equals(codigoVista)) {
           Map<LocalDate, List<CitaDTO>> dias = new TreeMap<>();
           for (CitaDTO c : citas) dias.computeIfAbsent(c.getFecha(), k -> new ArrayList<>()).add(c); %>
        <div class="semana-grid">
            <% for (LocalDate d = vista.desde(fecha); !d.isAfter(vista.hasta(fecha)); d = d.plusDays(1)) {
                   List<CitaDTO> delDia = dias.getOrDefault(d, List.of()); %>
            <div class="semana-dia <%= d.equals(hoy) ? "es-hoy" : "" %>">
                <a class="semana-cabecera" href="<%= rutaCitas %>?vista=dia&fecha=<%= d %><%= filtros %>" title="Ver el día completo">
                    <small class="text-muted"><%= FechaTexto.diaCorto(d) %></small><span class="num"><%= d.getDayOfMonth() %></span>
                    <small class="text-muted"><%= delDia.isEmpty() ? "Sin citas" : delDia.size() + (delDia.size() == 1 ? " cita" : " citas") %></small>
                </a>
                <% if (delDia.isEmpty()) { %><div class="semana-libre"><%= d.isBefore(hoy) ? "—" : "Libre" %></div><% } %>
                <% for (CitaDTO c : delDia) { %>
                <a class="semana-cita estado-<%= c.getEstadoCita() %> <%= c.getFin().isBefore(ahora) && c.isActiva() ? "atrasada" : "" %>" href="<%= rutaCitas %>?accion=ver&id=<%= c.getIdCita() %>"
                   title="<%= EscapeHtml.texto(c.getNombrePaciente() + " · " + c.getEstadoLegible()) %>">
                    <i class="<%= c.getIconoEstado() %> icono-estado" aria-hidden="true"></i><span class="sr-only"><%= c.getEstadoLegible() %>: </span>
                    <strong><%= FechaTexto.hora(c.getHoraInicio()) %></strong><br>
                    <%= EscapeHtml.texto(nombreCorto(c.getNombrePaciente())) %>
                    <% if (!modoMedico) { %><br><small class="text-muted"><%= EscapeHtml.texto(c.getEspecialidad()) %></small><% } %>
                </a>
                <% } %>
            </div>
            <% } %>
        </div>
        <div class="leyenda-estados small text-muted px-3 py-2 border-top" aria-label="Leyenda de estados">
            <% for (String[] l : leyenda) { %><span class="semana-cita estado-<%= l[0] %> d-inline-block mb-0 mr-2"><i class="<%= CitaDTO.iconoEstado(l[0]) %> mr-1" aria-hidden="true"></i><%= l[1] %></span><% } %>
        </div>
    <% } else if (citas.isEmpty()) { %>
        <div class="estado-vacio">
            <i class="fas fa-calendar-day text-muted"></i>
            <% if (filtro.hayFiltros() && !(modoMedico && filtro.getTexto() == null && filtro.getEstado() == null)) { %>
            <p class="mb-1"><strong>Ninguna cita coincide con el filtro.</strong></p>
            <a href="<%= rutaCitas %>?vista=dia&fecha=<%= fecha %>">Quitar filtros</a>
            <% } else { %>
            <p class="mb-1"><strong><%= modoMedico ? "No tienes citas" : "No hay citas" %> <%= fecha.equals(hoy) ? "hoy" : "este día" %>.</strong></p>
            <p class="text-muted mb-2">Revisa la <a href="<%= rutaCitas %>?vista=semana&fecha=<%= fecha %><%= filtros %>">semana</a>
                o el <a href="<%= rutaCitas %>?vista=mes&fecha=<%= fecha %><%= filtros %>">mes</a> para ver las siguientes.</p>
            <% if (!modoMedico && !fecha.isBefore(hoy)) { %>
            <a href="<%= rutaCitas %>?accion=reservar" class="btn btn-primary btn-sm"><i class="fas fa-calendar-plus mr-1"></i> Reservar una cita</a>
            <% } %>
            <% } %>
        </div>
    <% } else {
           // La "siguiente" cita: la primera aún activa que no terminó (se resalta para ubicarse rápido)
           int idSiguiente = 0;
           CitaDTO siguiente = null;
           if (fecha.equals(hoy)) {
               for (CitaDTO c : citas) { if (c.isActiva() && c.getFin().isAfter(ahora)) { idSiguiente = c.getIdCita(); siguiente = c; break; } }
           }
           int totalDia = 0, atendidasDia = 0, faltaronDia = 0, enSala = 0;
           for (CitaDTO c : citas) {
               if (c.isCancelada()) continue;
               totalDia++;
               if (c.isAtendida()) atendidasDia++;
               if (c.isNoAsistio()) faltaronDia++;
               if (c.isConfirmada()) enSala++;
           }
           int columnas = modoMedico ? 4 : 5;
           boolean marcaAhora = fecha.equals(hoy) && "dia".equals(codigoVista);
           String turno = ""; %>
        <% if ("dia".equals(codigoVista) && totalDia > 0) { %>
        <div class="progreso-dia px-3 pt-3 pb-2 border-bottom">
            <div class="d-flex flex-wrap align-items-baseline" style="gap:.25rem 1rem">
                <strong><%= atendidasDia %> de <%= totalDia %> atendidas</strong>
                <% if (enSala > 0) { %><span class="text-warning"><i class="fas fa-user-clock mr-1"></i><%= enSala %> en sala de espera</span><% } %>
                <% if (siguiente != null) {
                       long minutos = java.time.Duration.between(ahora, siguiente.getInicio()).toMinutes(); %>
                <span class="text-primary"><i class="fas fa-arrow-right mr-1"></i>Siguiente: <strong><%= EscapeHtml.texto(nombreCorto(siguiente.getNombrePaciente())) %></strong>
                    a las <%= FechaTexto.hora(siguiente.getHoraInicio()) %><%= minutos > 0 ? " (en " + (minutos >= 60 ? minutos / 60 + " h " + minutos % 60 + " min" : minutos + " min") + ")" : minutos > -15 ? " (ahora)" : " (con retraso)" %></span>
                <% } else if (fecha.equals(hoy)) { %><span class="text-success"><i class="fas fa-check mr-1"></i>No quedan citas por atender hoy</span><% } %>
            </div>
            <div class="progress progress-xs mt-2" role="progressbar" aria-label="Avance del día" aria-valuemin="0" aria-valuemax="<%= totalDia %>" aria-valuenow="<%= atendidasDia + faltaronDia %>">
                <div class="progress-bar bg-success" style="width:<%= atendidasDia * 100 / totalDia %>%"></div>
                <div class="progress-bar bg-secondary" style="width:<%= faltaronDia * 100 / totalDia %>%"></div>
            </div>
        </div>
        <% } %>
        <div class="table-responsive">
        <table class="table table-hover mb-0 tabla-agenda">
            <thead class="thead-light">
                <tr>
                    <th scope="col">Hora</th>
                    <th scope="col">Paciente</th>
                    <% if (!modoMedico) { %><th scope="col" class="d-none d-md-table-cell">Médico</th><% } %>
                    <th scope="col">Estado</th>
                    <th scope="col" class="text-right"><span class="sr-only">Acciones</span></th>
                </tr>
            </thead>
            <tbody>
            <% for (CitaDTO c : citas) {
                   if ("dia".equals(codigoVista)) {
                       String t = c.getHoraInicio().getHour() < 13 ? "Mañana" : "Tarde";
                       if (!t.equals(turno)) { turno = t; %>
                <tr class="fila-turno"><th colspan="<%= columnas %>" scope="rowgroup"><i class="fas <%= "Mañana".equals(t) ? "fa-sun" : "fa-cloud-sun" %> mr-1" aria-hidden="true"></i><%= t %></th></tr>
                <%     }
                       if (marcaAhora && c.getInicio().isAfter(ahora)) { marcaAhora = false; %>
                <tr class="fila-ahora" aria-hidden="true"><td colspan="<%= columnas %>"><span>Ahora · <%= String.format("%02d:%02d", ahora.getHour(), ahora.getMinute()) %></span></td></tr>
                <%     }
                   }
                   AccionesCita ac = new AccionesCita(c, usuarioSesion, ahora, horas);
                   boolean pasada = !c.isActiva() || c.getFin().isBefore(ahora);
                   String resumenCita = c.getNombrePaciente() + " · " + FechaTexto.relativa(c.getFecha(), hoy) + " a las " + FechaTexto.hora(c.getHoraInicio())
                                      + " · " + c.getEspecialidad(); %>
                <tr class="<%= c.getIdCita() == idSiguiente ? "cita-siguiente" : (pasada ? "cita-pasada" : "") %> <%= c.isCancelada() ? "cita-cancelada" : "" %>">
                    <td>
                        <% if ("semana".equals(codigoVista)) { %><small class="d-block text-muted"><%= FechaTexto.diaCorto(c.getFecha()) %> <%= c.getFecha().getDayOfMonth() %></small><% } %>
                        <span class="hora"><%= FechaTexto.hora(c.getHoraInicio()) %></span>
                        <% if (c.getIdCita() == idSiguiente) { %><span class="badge badge-primary d-block mt-1">Siguiente</span><% } %>
                    </td>
                    <td>
                        <a href="<%= rutaCitas %>?accion=ver&id=<%= c.getIdCita() %>" class="font-weight-bold nombre-paciente"><%= EscapeHtml.texto(c.getNombrePaciente()) %></a>
                        <% if (!c.getObservacionesPaciente().isBlank()) { %>
                        <i class="fas fa-exclamation-triangle alerta-paciente ml-1" data-toggle="tooltip" title="<%= EscapeHtml.texto(c.getObservacionesPaciente()) %>" aria-label="Tiene una alerta"></i>
                        <% } %>
                        <div><small class="text-muted">DNI <%= EscapeHtml.texto(c.getDniPaciente()) %> · <%= EscapeHtml.texto(c.getNumeroHistoria()) %>
                            <% if (c.getEdadPaciente() != null) { %> · <%= c.getEdadPaciente() %> años<% } %>
                            <% if (c.isDelPortal()) { %> · <span title="Reservó desde el portal del paciente"><i class="fas fa-globe"></i> Portal</span><% } %></small></div>
                        <% if (!c.getMotivo().isBlank()) { %><div><small><i class="fas fa-comment-medical text-muted mr-1"></i><%= EscapeHtml.texto(c.getMotivo()) %></small></div><% } %>
                    </td>
                    <% if (!modoMedico) { %>
                    <td class="d-none d-md-table-cell">
                        <div><%= EscapeHtml.texto("Dr(a). " + c.getNombreMedico()) %></div>
                        <small class="text-muted"><%= EscapeHtml.texto(c.getEspecialidad()) %> · <%= EscapeHtml.texto(c.getConsultorio()) %></small>
                    </td>
                    <% } %>
                    <td>
                        <span class="badge <%= c.getClaseEstado() %>"><i class="<%= c.getIconoEstado() %> mr-1"></i><%= c.isConfirmada() ? "En sala de espera" : c.getEstadoLegible() %></span>
                        <% if (c.isConfirmada() && c.getFechaLlegada() != null) { %><small class="d-block text-muted">llegó <%= controlador.FormatoVista.fechaHora(c.getFechaLlegada()).substring(11) %></small><% } %>
                        <% if (!modoMedico && c.isCuentaPagada()) { %><small class="d-block text-success"><i class="fas fa-check mr-1"></i>Pagada</small>
                        <% } else if (!modoMedico && c.isTieneCuenta()) { %><small class="d-block text-warning">Por cobrar</small><% } %>
                    </td>
                    <td class="text-right text-nowrap">
                        <% if (modoMedico) { %>
                            <% if (ac.isAtender()) { %>
                            <button type="button" class="btn btn-success btn-sm" data-atencion="atendida" data-id="<%= c.getIdCita() %>"
                                    data-resumen="<%= EscapeHtml.texto(resumenCita) %>" data-regreso="<%= EscapeHtml.texto(regreso) %>"><i class="fas fa-check mr-1"></i>Atendida</button>
                            <% } %>
                            <% if (ac.isNoAsistio()) { %>
                            <button type="button" class="btn btn-outline-secondary btn-sm" data-atencion="noasistio" data-id="<%= c.getIdCita() %>"
                                    data-resumen="<%= EscapeHtml.texto(resumenCita) %>" data-regreso="<%= EscapeHtml.texto(regreso) %>" title="El paciente no vino">No asistió</button>
                            <% } %>
                            <a href="<%= rutaCitas %>?accion=ver&id=<%= c.getIdCita() %>" class="btn btn-outline-primary btn-sm" title="Ver la ficha"><i class="fas fa-file-medical"></i><span class="sr-only">Ver ficha</span></a>
                        <% } else { %>
                            <% if (ac.isLlegada()) { %>
                            <form method="POST" action="<%= rutaCitas %>" class="d-inline" data-un-envio>
                                <input type="hidden" name="accion" value="llegada"><input type="hidden" name="id" value="<%= c.getIdCita() %>">
                                <input type="hidden" name="regreso" value="<%= EscapeHtml.texto(regreso) %>">
                                <button type="submit" class="btn btn-warning btn-sm" title="Registrar que el paciente llegó"><i class="fas fa-user-check mr-1"></i>Llegó</button>
                            </form>
                            <% } %>
                            <% if (ac.isCobrar()) { %>
                            <form method="POST" action="<%= rutaCitas %>" class="d-inline" data-un-envio>
                                <input type="hidden" name="accion" value="cobrar"><input type="hidden" name="id" value="<%= c.getIdCita() %>">
                                <button type="submit" class="btn btn-outline-success btn-sm" title="Abrir la cuenta en caja con la consulta ya cargada"><i class="fas fa-cash-register mr-1"></i>Cobrar</button>
                            </form>
                            <% } else if (ac.isVerCuenta()) { %>
                            <a href="<%= ctx %>/caja/cuentas?accion=ver&id=<%= c.getIdCuentaAtencion() %>" class="btn btn-outline-secondary btn-sm" title="Ver su cuenta en caja"><i class="fas fa-receipt"></i><span class="sr-only">Cuenta</span></a>
                            <% } %>
                            <div class="btn-group">
                                <button type="button" class="btn btn-outline-secondary btn-sm dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false" aria-label="Más acciones">
                                    <i class="fas fa-ellipsis-h"></i></button>
                                <div class="dropdown-menu dropdown-menu-right">
                                    <a class="dropdown-item" href="<%= rutaCitas %>?accion=ver&id=<%= c.getIdCita() %>"><i class="fas fa-eye mr-2 text-muted"></i>Ver detalle</a>
                                    <% if (ac.isReprogramar()) { %><a class="dropdown-item" href="<%= rutaCitas %>?accion=reprogramar&id=<%= c.getIdCita() %>"><i class="fas fa-calendar-alt mr-2 text-muted"></i>Reprogramar</a><% } %>
                                    <% if (ac.isCancelar()) { %>
                                    <div class="dropdown-divider"></div>
                                    <button type="button" class="dropdown-item text-danger" data-cancelar-cita data-id="<%= c.getIdCita() %>"
                                            data-resumen="<%= EscapeHtml.texto(resumenCita) %>" data-regreso="<%= EscapeHtml.texto(regreso) %>"><i class="fas fa-times mr-2"></i>Cancelar cita</button>
                                    <% } %>
                                </div>
                            </div>
                        <% } %>
                    </td>
                </tr>
            <% } %>
            </tbody>
        </table>
        </div>
        <p class="small text-muted px-3 py-2 mb-0 d-none d-md-block atajos-agenda"><i class="far fa-keyboard mr-1"></i>Atajos: <kbd>←</kbd> <kbd>→</kbd> para cambiar de <%= vista.getNombre().toLowerCase() %>, <kbd>H</kbd> para volver a hoy.</p>
        <% if (citas.size() >= FiltroCitaDTO.MAXIMO_FILAS) { %>
        <p class="small text-muted p-2 mb-0">Se muestran las primeras <%= FiltroCitaDTO.MAXIMO_FILAS %> citas. Filtra por médico para ver menos.</p>
        <% } %>
    <% } %>
    </div>
</div>
<% if (!modoMedico) { %>
<p class="small text-muted"><i class="fas fa-lightbulb mr-1"></i>Para reservar más rápido, también puedes ir a <a href="<%= ctx %>/recepcion/pacientes">Pacientes</a>
    y usar el botón <i class="fas fa-calendar-plus"></i> del paciente.</p>
<% } %>
<% } %>
<%@ include file="/WEB-INF/vistas/comun/citas-modales.jspf" %>
<script src="<%= ctx %>/js/citas.js?v=<%= VersionAplicacion.RECURSOS %>" defer></script>
<%@ include file="/WEB-INF/vistas/plantilla/pie.jspf" %>
