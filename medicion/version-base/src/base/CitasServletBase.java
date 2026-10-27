package base;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * VERSIÓN BASE (pretest del diseño preexperimental): módulo de citas escrito en estilo
 * procedimental, sin MVC ni patrones. La interfaz (HTML), las reglas y el SQL conviven en el servlet.
 * Solo se usa para medir complejidad y duplicación frente a la versión refactorizada; no se despliega.
 */
@WebServlet("/base/citas")
public class CitasServletBase extends HttpServlet {

    private Connection conectar() throws SQLException {
        String url = System.getenv("DB_URL");
        String usuario = System.getenv("DB_USER");
        String clave = System.getenv("DB_PASSWORD");
        if (url == null) {
            url = "jdbc:postgresql://localhost:5432/clinica_db";
        }
        if (usuario == null) {
            usuario = "postgres";
        }
        if (clave == null) {
            clave = "";
        }
        return DriverManager.getConnection(url, usuario, clave);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("idUsuario") == null) {
            response.sendRedirect(request.getContextPath() + "/jsp/login.jsp");
            return;
        }
        int rol = (Integer) session.getAttribute("idRol");
        int idUsuario = (Integer) session.getAttribute("idUsuario");
        String vista = request.getParameter("vista");
        if (vista == null || (!vista.equals("dia") && !vista.equals("semana") && !vista.equals("mes"))) {
            vista = "dia";
        }
        LocalDate fecha;
        try {
            fecha = request.getParameter("fecha") == null ? LocalDate.now() : LocalDate.parse(request.getParameter("fecha"));
        } catch (Exception e) {
            fecha = LocalDate.now();
        }
        LocalDate desde;
        LocalDate hasta;
        if (vista.equals("dia")) {
            desde = fecha;
            hasta = fecha;
        } else if (vista.equals("semana")) {
            desde = fecha.minusDays(fecha.getDayOfWeek().getValue() - 1);
            hasta = desde.plusDays(6);
        } else {
            desde = fecha.withDayOfMonth(1);
            hasta = fecha.withDayOfMonth(fecha.lengthOfMonth());
        }
        String estado = request.getParameter("estado");
        String texto = request.getParameter("q");
        String medico = request.getParameter("medico");
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println("<html><head><title>Citas</title></head><body>");
        out.println("<h1>Agenda de citas</h1>");
        if (vista.equals("dia")) {
            out.println("<h2>" + fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "</h2>");
        } else if (vista.equals("semana")) {
            out.println("<h2>Semana del " + desde.getDayOfMonth() + " al " + hasta.getDayOfMonth() + "</h2>");
        } else {
            out.println("<h2>Mes " + fecha.getMonthValue() + "/" + fecha.getYear() + "</h2>");
        }
        String sql = "SELECT c.id_cita, c.codigo_cita, c.fecha, c.hora_inicio, c.hora_fin, c.estado_cita, c.motivo, "
                + "dp.nombres || ' ' || dp.apellidos AS paciente, dp.dni, dm.nombres || ' ' || dm.apellidos AS medico, "
                + "e.nombre AS especialidad, m.consultorio, c.id_cuenta_atencion, dm.id_usuario AS usuario_medico "
                + "FROM tt_cita c JOIN tt_paciente p ON p.id_paciente = c.id_paciente "
                + "JOIN tt_datos_personales dp ON dp.id_datos_personales = p.id_datos_personales "
                + "JOIN tt_medico m ON m.id_medico = c.id_medico "
                + "JOIN tt_datos_personales dm ON dm.id_datos_personales = m.id_datos_personales "
                + "JOIN tm_especialidad e ON e.id_especialidad = c.id_especialidad "
                + "WHERE c.fecha BETWEEN ? AND ?";
        if (estado != null && !estado.isEmpty()) {
            sql += " AND c.estado_cita = ?";
        }
        if (texto != null && !texto.trim().isEmpty()) {
            sql += " AND (dp.dni = ? OR LOWER(dp.nombres || ' ' || dp.apellidos) LIKE ? OR c.codigo_cita = ?)";
        }
        if (rol == 3) {
            sql += " AND dm.id_usuario = ?";
        } else if (medico != null && !medico.isEmpty()) {
            sql += " AND c.id_medico = ?";
        }
        sql += " ORDER BY c.fecha, c.hora_inicio LIMIT 300";
        try (Connection cn = conectar(); PreparedStatement ps = cn.prepareStatement(sql)) {
            int i = 1;
            ps.setDate(i++, java.sql.Date.valueOf(desde));
            ps.setDate(i++, java.sql.Date.valueOf(hasta));
            if (estado != null && !estado.isEmpty()) {
                ps.setString(i++, estado);
            }
            if (texto != null && !texto.trim().isEmpty()) {
                ps.setString(i++, texto.trim());
                ps.setString(i++, "%" + texto.trim().toLowerCase() + "%");
                ps.setString(i++, texto.trim().toUpperCase());
            }
            if (rol == 3) {
                ps.setInt(i++, idUsuario);
            } else if (medico != null && !medico.isEmpty()) {
                ps.setInt(i++, Integer.parseInt(medico));
            }
            ResultSet rs = ps.executeQuery();
            Map<LocalDate, Integer> porDia = new TreeMap<>();
            int total = 0;
            int atendidas = 0;
            int canceladas = 0;
            int noAsistio = 0;
            int pendientes = 0;
            if (!vista.equals("mes")) {
                out.println("<table border='1'><tr><th>Hora</th><th>Paciente</th><th>Médico</th><th>Estado</th><th>Acciones</th></tr>");
            }
            while (rs.next()) {
                total++;
                String est = rs.getString("estado_cita");
                if (est.equals("ATENDIDA")) {
                    atendidas++;
                } else if (est.equals("CANCELADA")) {
                    canceladas++;
                } else if (est.equals("NO_ASISTIO")) {
                    noAsistio++;
                } else {
                    pendientes++;
                }
                LocalDate f = rs.getDate("fecha").toLocalDate();
                if (vista.equals("mes")) {
                    porDia.put(f, porDia.getOrDefault(f, 0) + 1);
                    continue;
                }
                LocalTime h = rs.getTime("hora_inicio").toLocalTime();
                String horaTexto;
                if (h.getHour() == 0) {
                    horaTexto = "12:" + String.format("%02d", h.getMinute()) + " a. m.";
                } else if (h.getHour() < 12) {
                    horaTexto = h.getHour() + ":" + String.format("%02d", h.getMinute()) + " a. m.";
                } else if (h.getHour() == 12) {
                    horaTexto = "12:" + String.format("%02d", h.getMinute()) + " p. m.";
                } else {
                    horaTexto = (h.getHour() - 12) + ":" + String.format("%02d", h.getMinute()) + " p. m.";
                }
                String estadoTexto;
                if (est.equals("RESERVADA")) {
                    estadoTexto = "Reservada";
                } else if (est.equals("CONFIRMADA")) {
                    estadoTexto = "En sala de espera";
                } else if (est.equals("ATENDIDA")) {
                    estadoTexto = "Atendida";
                } else if (est.equals("CANCELADA")) {
                    estadoTexto = "Cancelada";
                } else {
                    estadoTexto = "No asistió";
                }
                String nombre = rs.getString("paciente").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
                String med = rs.getString("medico").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
                out.println("<tr><td>" + (vista.equals("semana") ? f + " " : "") + horaTexto + "</td><td>" + nombre + " (DNI "
                        + rs.getString("dni") + ")</td><td>" + med + " - " + rs.getString("especialidad") + "</td><td>" + estadoTexto + "</td><td>");
                LocalDateTime inicio = LocalDateTime.of(f, h);
                LocalDateTime fin = LocalDateTime.of(f, rs.getTime("hora_fin").toLocalTime());
                if (rol == 1 || rol == 2) {
                    if (est.equals("RESERVADA") && f.equals(LocalDate.now())) {
                        out.println("<form method='post'><input type='hidden' name='accion' value='llegada'><input type='hidden' name='id' value='"
                                + rs.getInt("id_cita") + "'><button>Llegó</button></form>");
                    }
                    if ((est.equals("RESERVADA") || est.equals("CONFIRMADA")) && rs.getObject("id_cuenta_atencion") == null) {
                        out.println("<form method='post' action='" + request.getContextPath() + "/base/caja'><input type='hidden' name='accion' value='abrirDesdeCita'>"
                                + "<input type='hidden' name='idCita' value='" + rs.getInt("id_cita") + "'><button>Cobrar</button></form>");
                    }
                    if (est.equals("RESERVADA") && inicio.isAfter(LocalDateTime.now())) {
                        out.println("<a href='?accion=reprogramar&id=" + rs.getInt("id_cita") + "'>Reprogramar</a>");
                    }
                    if ((est.equals("RESERVADA") || est.equals("CONFIRMADA")) && !f.isBefore(LocalDate.now())) {
                        out.println("<form method='post'><input type='hidden' name='accion' value='cancelar'><input type='hidden' name='id' value='"
                                + rs.getInt("id_cita") + "'><input name='motivo'><button>Cancelar</button></form>");
                    }
                } else if (rol == 3 || rol == 1) {
                    if ((est.equals("RESERVADA") || est.equals("CONFIRMADA")) && !f.isAfter(LocalDate.now())) {
                        out.println("<form method='post'><input type='hidden' name='accion' value='atendida'><input type='hidden' name='id' value='"
                                + rs.getInt("id_cita") + "'><input name='nota'><button>Atendida</button></form>");
                    }
                    if (est.equals("RESERVADA") && fin.isBefore(LocalDateTime.now())) {
                        out.println("<form method='post'><input type='hidden' name='accion' value='noasistio'><input type='hidden' name='id' value='"
                                + rs.getInt("id_cita") + "'><button>No asistió</button></form>");
                    }
                }
                out.println("</td></tr>");
            }
            if (!vista.equals("mes")) {
                out.println("</table>");
            } else {
                out.println("<table border='1'><tr><th>Lun</th><th>Mar</th><th>Mié</th><th>Jue</th><th>Vie</th><th>Sáb</th><th>Dom</th></tr><tr>");
                LocalDate inicioGrid = desde.minusDays(desde.getDayOfWeek().getValue() - 1);
                for (LocalDate d = inicioGrid; !d.isAfter(hasta) || d.getDayOfWeek() != DayOfWeek.MONDAY; d = d.plusDays(1)) {
                    if (d.getDayOfWeek() == DayOfWeek.MONDAY && !d.equals(inicioGrid)) {
                        out.println("</tr><tr>");
                    }
                    int n = porDia.getOrDefault(d, 0);
                    out.println("<td>" + (d.getMonth() == fecha.getMonth() ? d.getDayOfMonth() : "") + (n > 0 ? " (" + n + " citas)" : "") + "</td>");
                }
                out.println("</tr></table>");
            }
            out.println("<p>Total: " + total + " · Atendidas: " + atendidas + " · Pendientes: " + pendientes
                    + " · Canceladas: " + canceladas + " · No asistieron: " + noAsistio + "</p>");
        } catch (SQLException e) {
            out.println("<p>Error al leer la agenda: " + e.getMessage() + "</p>");
        }
        if ("reservar".equals(request.getParameter("accion")) || "reprogramar".equals(request.getParameter("accion"))) {
            String esp = request.getParameter("esp");
            String fechaTurnos = request.getParameter("fechaTurnos");
            if (esp != null && fechaTurnos != null) {
                LocalDate dia = LocalDate.parse(fechaTurnos);
                try (Connection cn = conectar()) {
                    PreparedStatement pm = cn.prepareStatement("SELECT m.id_medico, m.duracion_cita, dm.nombres || ' ' || dm.apellidos AS nombre "
                            + "FROM tt_medico m JOIN tt_datos_personales dm ON dm.id_datos_personales = m.id_datos_personales "
                            + "WHERE m.id_especialidad = ? AND m.estado = 'Activo'");
                    pm.setInt(1, Integer.parseInt(esp));
                    ResultSet medicos = pm.executeQuery();
                    while (medicos.next()) {
                        int idMedico = medicos.getInt("id_medico");
                        int duracion = medicos.getInt("duracion_cita");
                        out.println("<h3>" + medicos.getString("nombre") + "</h3>");
                        PreparedStatement ph = cn.prepareStatement("SELECT hora_inicio, hora_fin FROM tt_horario_medico WHERE id_medico = ? AND dia_semana = ? ORDER BY hora_inicio");
                        ph.setInt(1, idMedico);
                        ph.setInt(2, dia.getDayOfWeek().getValue());
                        ResultSet horarios = ph.executeQuery();
                        PreparedStatement po = cn.prepareStatement("SELECT hora_inicio FROM tt_cita WHERE id_medico = ? AND fecha = ? AND estado_cita <> 'CANCELADA'");
                        po.setInt(1, idMedico);
                        po.setDate(2, java.sql.Date.valueOf(dia));
                        ResultSet ocupadas = po.executeQuery();
                        Set<LocalTime> tomadas = new HashSet<>();
                        while (ocupadas.next()) {
                            tomadas.add(ocupadas.getTime(1).toLocalTime());
                        }
                        while (horarios.next()) {
                            LocalTime h = horarios.getTime(1).toLocalTime();
                            LocalTime finBloque = horarios.getTime(2).toLocalTime();
                            while (!h.plusMinutes(duracion).isAfter(finBloque) && h.plusMinutes(duracion).isAfter(h)) {
                                boolean pasado = dia.equals(LocalDate.now()) && !LocalDateTime.of(dia, h).isAfter(LocalDateTime.now());
                                if (!pasado && !tomadas.contains(h)) {
                                    out.println("<button name='hora' value='" + h + "'>" + h + "</button>");
                                } else if (!pasado) {
                                    out.println("<button disabled>" + h + " (ocupado)</button>");
                                }
                                h = h.plusMinutes(duracion);
                            }
                        }
                    }
                } catch (SQLException e) {
                    out.println("<p>Error al calcular los turnos: " + e.getMessage() + "</p>");
                }
            }
        }
        out.println("</body></html>");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("idUsuario") == null) {
            response.sendRedirect(request.getContextPath() + "/jsp/login.jsp");
            return;
        }
        int rol = (Integer) session.getAttribute("idRol");
        int idUsuario = (Integer) session.getAttribute("idUsuario");
        String usuario = (String) session.getAttribute("nombreUsuario");
        String accion = request.getParameter("accion");
        String volver = request.getContextPath() + "/base/citas";
        String mensaje = null;
        try (Connection cn = conectar()) {
            if ("reservar".equals(accion)) {
                int idPaciente = Integer.parseInt(request.getParameter("paciente"));
                int idMedico = Integer.parseInt(request.getParameter("medico"));
                LocalDate fecha = LocalDate.parse(request.getParameter("fecha"));
                LocalTime hora = LocalTime.parse(request.getParameter("hora"));
                String motivo = request.getParameter("motivo") == null ? "" : request.getParameter("motivo").trim();
                if (rol != 1 && rol != 2 && rol != 4) {
                    mensaje = "Tu perfil no puede reservar citas.";
                } else if (motivo.length() > 250) {
                    mensaje = "El motivo puede tener hasta 250 caracteres.";
                } else if (fecha.isBefore(LocalDate.now())) {
                    mensaje = "Esa fecha ya pasó.";
                } else {
                    PreparedStatement pp = cn.prepareStatement("SELECT valor FROM tt_parametro_clinica WHERE id_parametro_clinica = 1");
                    ResultSet par = pp.executeQuery();
                    int diasMaximos = 60;
                    if (par.next()) {
                        diasMaximos = par.getInt(1);
                    }
                    if (fecha.isAfter(LocalDate.now().plusDays(diasMaximos))) {
                        mensaje = "Solo se puede reservar hasta " + diasMaximos + " días adelante.";
                    }
                    if (mensaje == null && rol == 4) {
                        PreparedStatement pu = cn.prepareStatement("SELECT p.id_paciente FROM tt_paciente p JOIN tt_datos_personales d "
                                + "ON d.id_datos_personales = p.id_datos_personales WHERE d.id_usuario = ?");
                        pu.setInt(1, idUsuario);
                        ResultSet ru = pu.executeQuery();
                        if (!ru.next() || ru.getInt(1) != idPaciente) {
                            mensaje = "Solo puedes reservar citas para ti.";
                        } else {
                            PreparedStatement pc = cn.prepareStatement("SELECT COUNT(*) FROM tt_cita WHERE id_paciente = ? AND estado_cita IN ('RESERVADA','CONFIRMADA') "
                                    + "AND (fecha > CURRENT_DATE OR (fecha = CURRENT_DATE AND hora_inicio > CURRENT_TIME))");
                            pc.setInt(1, idPaciente);
                            ResultSet rc = pc.executeQuery();
                            rc.next();
                            if (rc.getInt(1) >= 3) {
                                mensaje = "Ya tienes 3 citas pendientes.";
                            }
                        }
                    }
                    if (mensaje == null) {
                        PreparedStatement pa = cn.prepareStatement("SELECT p.estado FROM tt_paciente p WHERE p.id_paciente = ?");
                        pa.setInt(1, idPaciente);
                        ResultSet ra = pa.executeQuery();
                        if (!ra.next() || !"Activo".equals(ra.getString(1))) {
                            mensaje = "Elige un paciente activo.";
                        }
                    }
                    int duracion = 0;
                    int idEspecialidad = 0;
                    if (mensaje == null) {
                        PreparedStatement pm = cn.prepareStatement("SELECT duracion_cita, id_especialidad, estado FROM tt_medico WHERE id_medico = ?");
                        pm.setInt(1, idMedico);
                        ResultSet rm = pm.executeQuery();
                        if (!rm.next()) {
                            mensaje = "Elige un médico de la lista.";
                        } else if (!"Activo".equals(rm.getString("estado"))) {
                            mensaje = "Ese médico no está recibiendo citas por ahora.";
                        } else {
                            duracion = rm.getInt("duracion_cita");
                            idEspecialidad = rm.getInt("id_especialidad");
                        }
                    }
                    if (mensaje == null) {
                        PreparedStatement ph = cn.prepareStatement("SELECT hora_inicio, hora_fin FROM tt_horario_medico WHERE id_medico = ? AND dia_semana = ?");
                        ph.setInt(1, idMedico);
                        ph.setInt(2, fecha.getDayOfWeek().getValue());
                        ResultSet rh = ph.executeQuery();
                        boolean calza = false;
                        while (rh.next()) {
                            LocalTime ini = rh.getTime(1).toLocalTime();
                            LocalTime fin = rh.getTime(2).toLocalTime();
                            for (LocalTime t = ini; !t.plusMinutes(duracion).isAfter(fin) && t.plusMinutes(duracion).isAfter(t); t = t.plusMinutes(duracion)) {
                                if (t.equals(hora)) {
                                    calza = true;
                                }
                            }
                        }
                        if (!calza) {
                            mensaje = "El médico no tiene ese horario disponible.";
                        } else if (fecha.equals(LocalDate.now()) && !hora.isAfter(LocalTime.now())) {
                            mensaje = "Esa hora ya pasó.";
                        }
                    }
                    if (mensaje == null) {
                        PreparedStatement po = cn.prepareStatement("SELECT COUNT(*) FROM tt_cita WHERE id_medico = ? AND fecha = ? AND hora_inicio = ? AND estado_cita <> 'CANCELADA'");
                        po.setInt(1, idMedico);
                        po.setDate(2, java.sql.Date.valueOf(fecha));
                        po.setTime(3, Time.valueOf(hora));
                        ResultSet ro = po.executeQuery();
                        ro.next();
                        if (ro.getInt(1) > 0) {
                            mensaje = "Ese horario ya fue tomado.";
                        }
                    }
                    if (mensaje == null) {
                        PreparedStatement px = cn.prepareStatement("SELECT id_cita, id_medico, hora_inicio, hora_fin FROM tt_cita WHERE id_paciente = ? AND fecha = ? "
                                + "AND estado_cita IN ('RESERVADA','CONFIRMADA')");
                        px.setInt(1, idPaciente);
                        px.setDate(2, java.sql.Date.valueOf(fecha));
                        ResultSet rx = px.executeQuery();
                        while (rx.next() && mensaje == null) {
                            LocalTime oi = rx.getTime("hora_inicio").toLocalTime();
                            LocalTime of = rx.getTime("hora_fin").toLocalTime();
                            if (hora.isBefore(of) && oi.isBefore(hora.plusMinutes(duracion))) {
                                mensaje = "El paciente ya tiene una cita a esa hora.";
                            } else if (rx.getInt("id_medico") == idMedico) {
                                mensaje = "El paciente ya tiene una cita con ese médico ese día.";
                            }
                        }
                    }
                    if (mensaje == null) {
                        cn.setAutoCommit(false);
                        PreparedStatement pi = cn.prepareStatement("INSERT INTO tt_cita (codigo_cita, id_paciente, id_medico, id_especialidad, fecha, hora_inicio, hora_fin, "
                                + "motivo, estado_cita, canal, usuario_registro) VALUES ('CT-' || LPAD(nextval('tt_cita_id_cita_seq')::text, 6, '0'), ?, ?, ?, ?, ?, ?, ?, 'RESERVADA', ?, ?) "
                                + "RETURNING id_cita, codigo_cita");
                        pi.setInt(1, idPaciente);
                        pi.setInt(2, idMedico);
                        pi.setInt(3, idEspecialidad);
                        pi.setDate(4, java.sql.Date.valueOf(fecha));
                        pi.setTime(5, Time.valueOf(hora));
                        pi.setTime(6, Time.valueOf(hora.plusMinutes(duracion)));
                        pi.setString(7, motivo);
                        pi.setString(8, rol == 4 ? "PORTAL" : "RECEPCION");
                        pi.setString(9, usuario);
                        try {
                            ResultSet ri = pi.executeQuery();
                            ri.next();
                            int idCita = ri.getInt(1);
                            String codigo = ri.getString(2);
                            PreparedStatement pl = cn.prepareStatement("INSERT INTO tt_auditoria (accion, id_usuario, id_entidad, detalle, usuario_registro) VALUES ('CITA_RESERVADA', ?, ?, ?, ?)");
                            pl.setInt(1, idUsuario);
                            pl.setInt(2, idCita);
                            pl.setString(3, codigo + " " + fecha + " " + hora);
                            pl.setString(4, usuario);
                            pl.executeUpdate();
                            cn.commit();
                            PreparedStatement pd = cn.prepareStatement("SELECT d.correo, d.celular, d.nombres FROM tt_paciente p JOIN tt_datos_personales d "
                                    + "ON d.id_datos_personales = p.id_datos_personales WHERE p.id_paciente = ?");
                            pd.setInt(1, idPaciente);
                            ResultSet rd = pd.executeQuery();
                            if (rd.next()) {
                                String correo = rd.getString(1);
                                String celular = rd.getString(2);
                                String asunto = "Cita reservada " + codigo;
                                String cuerpo = "Hola " + rd.getString(3) + ", tu cita " + codigo + " quedó reservada para el " + fecha + " a las " + hora + ".";
                                if (correo != null && !correo.isEmpty()) {
                                    System.out.println("CORREO a " + correo + ": " + asunto + " - " + cuerpo);
                                }
                                if (celular != null && !celular.isEmpty()) {
                                    String sms = cuerpo.length() > 160 ? cuerpo.substring(0, 157) + "..." : cuerpo;
                                    System.out.println("SMS a " + celular + ": " + sms);
                                }
                            }
                            mensaje = "Cita " + codigo + " reservada.";
                        } catch (SQLException e) {
                            cn.rollback();
                            if ("23505".equals(e.getSQLState())) {
                                mensaje = "Ese horario acaba de ser reservado por otra persona.";
                            } else {
                                mensaje = "No se pudo reservar: " + e.getMessage();
                            }
                        } finally {
                            cn.setAutoCommit(true);
                        }
                    }
                }
            } else if ("reprogramar".equals(accion)) {
                int idCita = Integer.parseInt(request.getParameter("id"));
                int idMedico = Integer.parseInt(request.getParameter("medico"));
                LocalDate fecha = LocalDate.parse(request.getParameter("fecha"));
                LocalTime hora = LocalTime.parse(request.getParameter("hora"));
                PreparedStatement pc = cn.prepareStatement("SELECT c.*, dp.id_usuario AS usuario_paciente FROM tt_cita c JOIN tt_paciente p ON p.id_paciente = c.id_paciente "
                        + "JOIN tt_datos_personales dp ON dp.id_datos_personales = p.id_datos_personales WHERE c.id_cita = ?");
                pc.setInt(1, idCita);
                ResultSet rc = pc.executeQuery();
                if (!rc.next()) {
                    mensaje = "La cita no existe.";
                } else if (rol == 3) {
                    mensaje = "El médico no reprograma citas.";
                } else if (rol == 4 && rc.getInt("usuario_paciente") != idUsuario) {
                    mensaje = "La cita no existe o no puedes modificarla.";
                } else if (!"RESERVADA".equals(rc.getString("estado_cita"))) {
                    mensaje = "Solo se reprograma una cita reservada.";
                } else {
                    LocalDateTime inicio = LocalDateTime.of(rc.getDate("fecha").toLocalDate(), rc.getTime("hora_inicio").toLocalTime());
                    int idPaciente = rc.getInt("id_paciente");
                    if (rol == 4) {
                        PreparedStatement pp = cn.prepareStatement("SELECT horas_cancelacion_paciente FROM tt_parametro_clinica WHERE id_parametro_clinica = 1");
                        ResultSet rp = pp.executeQuery();
                        int horas = rp.next() ? rp.getInt(1) : 24;
                        if (Duration.between(LocalDateTime.now(), inicio).toHours() < horas) {
                            mensaje = "Faltan menos de " + horas + " horas: llama a recepción.";
                        } else if (rc.getInt("reprogramaciones") >= 2) {
                            mensaje = "Esta cita ya se reprogramó 2 veces. Llama a recepción.";
                        }
                    } else if (!inicio.isAfter(LocalDateTime.now())) {
                        mensaje = "La cita ya empezó.";
                    }
                    int duracion = 0;
                    if (mensaje == null) {
                        PreparedStatement pm = cn.prepareStatement("SELECT duracion_cita, id_especialidad, estado FROM tt_medico WHERE id_medico = ?");
                        pm.setInt(1, idMedico);
                        ResultSet rm = pm.executeQuery();
                        if (!rm.next() || rm.getInt("id_especialidad") != rc.getInt("id_especialidad")) {
                            mensaje = "Elige un médico de la misma especialidad.";
                        } else if (!"Activo".equals(rm.getString("estado"))) {
                            mensaje = "Ese médico no está recibiendo citas por ahora.";
                        } else {
                            duracion = rm.getInt("duracion_cita");
                        }
                    }
                    if (mensaje == null && fecha.isBefore(LocalDate.now())) {
                        mensaje = "Esa fecha ya pasó.";
                    }
                    if (mensaje == null && idMedico == rc.getInt("id_medico") && fecha.equals(rc.getDate("fecha").toLocalDate())
                            && hora.equals(rc.getTime("hora_inicio").toLocalTime())) {
                        mensaje = "Es el mismo horario.";
                    }
                    if (mensaje == null) {
                        PreparedStatement ph = cn.prepareStatement("SELECT hora_inicio, hora_fin FROM tt_horario_medico WHERE id_medico = ? AND dia_semana = ?");
                        ph.setInt(1, idMedico);
                        ph.setInt(2, fecha.getDayOfWeek().getValue());
                        ResultSet rh = ph.executeQuery();
                        boolean calza = false;
                        while (rh.next()) {
                            LocalTime ini = rh.getTime(1).toLocalTime();
                            LocalTime fin = rh.getTime(2).toLocalTime();
                            for (LocalTime t = ini; !t.plusMinutes(duracion).isAfter(fin) && t.plusMinutes(duracion).isAfter(t); t = t.plusMinutes(duracion)) {
                                if (t.equals(hora)) {
                                    calza = true;
                                }
                            }
                        }
                        if (!calza) {
                            mensaje = "El médico no tiene ese horario disponible.";
                        } else if (fecha.equals(LocalDate.now()) && !hora.isAfter(LocalTime.now())) {
                            mensaje = "Esa hora ya pasó.";
                        }
                    }
                    if (mensaje == null) {
                        PreparedStatement po = cn.prepareStatement("SELECT COUNT(*) FROM tt_cita WHERE id_medico = ? AND fecha = ? AND hora_inicio = ? AND estado_cita <> 'CANCELADA'");
                        po.setInt(1, idMedico);
                        po.setDate(2, java.sql.Date.valueOf(fecha));
                        po.setTime(3, Time.valueOf(hora));
                        ResultSet ro = po.executeQuery();
                        ro.next();
                        if (ro.getInt(1) > 0) {
                            mensaje = "Ese horario ya fue tomado.";
                        }
                    }
                    if (mensaje == null) {
                        PreparedStatement px = cn.prepareStatement("SELECT id_cita, id_medico, hora_inicio, hora_fin FROM tt_cita WHERE id_paciente = ? AND fecha = ? "
                                + "AND estado_cita IN ('RESERVADA','CONFIRMADA') AND id_cita <> ?");
                        px.setInt(1, idPaciente);
                        px.setDate(2, java.sql.Date.valueOf(fecha));
                        px.setInt(3, idCita);
                        ResultSet rx = px.executeQuery();
                        while (rx.next() && mensaje == null) {
                            LocalTime oi = rx.getTime("hora_inicio").toLocalTime();
                            LocalTime of = rx.getTime("hora_fin").toLocalTime();
                            if (hora.isBefore(of) && oi.isBefore(hora.plusMinutes(duracion))) {
                                mensaje = "El paciente ya tiene una cita a esa hora.";
                            } else if (rx.getInt("id_medico") == idMedico) {
                                mensaje = "El paciente ya tiene una cita con ese médico ese día.";
                            }
                        }
                    }
                    if (mensaje == null) {
                        PreparedStatement pu = cn.prepareStatement("UPDATE tt_cita SET id_medico = ?, fecha = ?, hora_inicio = ?, hora_fin = ?, "
                                + "reprogramaciones = reprogramaciones + 1, usuario_registro = ? WHERE id_cita = ? AND estado_cita = 'RESERVADA'");
                        pu.setInt(1, idMedico);
                        pu.setDate(2, java.sql.Date.valueOf(fecha));
                        pu.setTime(3, Time.valueOf(hora));
                        pu.setTime(4, Time.valueOf(hora.plusMinutes(duracion)));
                        pu.setString(5, usuario);
                        pu.setInt(6, idCita);
                        if (pu.executeUpdate() == 0) {
                            mensaje = "La cita cambió mientras la reprogramabas.";
                        } else {
                            PreparedStatement pl = cn.prepareStatement("INSERT INTO tt_auditoria (accion, id_usuario, id_entidad, detalle, usuario_registro) VALUES ('CITA_REPROGRAMADA', ?, ?, ?, ?)");
                            pl.setInt(1, idUsuario);
                            pl.setInt(2, idCita);
                            pl.setString(3, rc.getString("codigo_cita") + " a " + fecha + " " + hora);
                            pl.setString(4, usuario);
                            pl.executeUpdate();
                            PreparedStatement pd = cn.prepareStatement("SELECT d.correo, d.celular, d.nombres FROM tt_paciente p JOIN tt_datos_personales d "
                                    + "ON d.id_datos_personales = p.id_datos_personales WHERE p.id_paciente = ?");
                            pd.setInt(1, idPaciente);
                            ResultSet rd = pd.executeQuery();
                            if (rd.next()) {
                                String correo = rd.getString(1);
                                String celular = rd.getString(2);
                                String asunto = "Cita reprogramada " + rc.getString("codigo_cita");
                                String cuerpo = "Hola " + rd.getString(3) + ", tu cita " + rc.getString("codigo_cita") + " ahora es el " + fecha + " a las " + hora + ".";
                                if (correo != null && !correo.isEmpty()) {
                                    System.out.println("CORREO a " + correo + ": " + asunto + " - " + cuerpo);
                                }
                                if (celular != null && !celular.isEmpty()) {
                                    String sms = cuerpo.length() > 160 ? cuerpo.substring(0, 157) + "..." : cuerpo;
                                    System.out.println("SMS a " + celular + ": " + sms);
                                }
                            }
                            mensaje = "Cita reprogramada.";
                        }
                    }
                }
            } else if ("cancelar".equals(accion)) {
                int idCita = Integer.parseInt(request.getParameter("id"));
                String motivo = request.getParameter("motivo") == null ? "" : request.getParameter("motivo").trim();
                PreparedStatement pc = cn.prepareStatement("SELECT c.*, dp.id_usuario AS usuario_paciente FROM tt_cita c JOIN tt_paciente p ON p.id_paciente = c.id_paciente "
                        + "JOIN tt_datos_personales dp ON dp.id_datos_personales = p.id_datos_personales WHERE c.id_cita = ?");
                pc.setInt(1, idCita);
                ResultSet rc = pc.executeQuery();
                if (!rc.next()) {
                    mensaje = "La cita no existe.";
                } else if (rol == 3) {
                    mensaje = "El médico no cancela citas.";
                } else if (rol == 4 && rc.getInt("usuario_paciente") != idUsuario) {
                    mensaje = "La cita no existe o no puedes cancelarla.";
                } else if (!"RESERVADA".equals(rc.getString("estado_cita")) && !"CONFIRMADA".equals(rc.getString("estado_cita"))) {
                    mensaje = "Esta cita ya no está activa.";
                } else if (motivo.length() < 5 || motivo.length() > 250) {
                    mensaje = "Indica el motivo de la cancelación (de 5 a 250 caracteres).";
                } else {
                    LocalDateTime inicio = LocalDateTime.of(rc.getDate("fecha").toLocalDate(), rc.getTime("hora_inicio").toLocalTime());
                    if (rol == 4) {
                        PreparedStatement pp = cn.prepareStatement("SELECT horas_cancelacion_paciente FROM tt_parametro_clinica WHERE id_parametro_clinica = 1");
                        ResultSet rp = pp.executeQuery();
                        int horas = rp.next() ? rp.getInt(1) : 24;
                        if (!"RESERVADA".equals(rc.getString("estado_cita"))) {
                            mensaje = "Ya registraste tu llegada: habla con recepción.";
                        } else if (Duration.between(LocalDateTime.now(), inicio).toHours() < horas) {
                            mensaje = "Faltan menos de " + horas + " horas: llama a recepción.";
                        }
                    } else if (rc.getDate("fecha").toLocalDate().isBefore(LocalDate.now())) {
                        mensaje = "Esa cita ya pasó: márcala como No asistió.";
                    }
                    if (mensaje == null) {
                        PreparedStatement pu = cn.prepareStatement("UPDATE tt_cita SET estado_cita = 'CANCELADA', motivo_cancelacion = ?, fecha_cancelacion = NOW(), "
                                + "id_usuario_cancela = ? WHERE id_cita = ? AND estado_cita IN ('RESERVADA','CONFIRMADA')");
                        pu.setString(1, motivo);
                        pu.setInt(2, idUsuario);
                        pu.setInt(3, idCita);
                        if (pu.executeUpdate() == 0) {
                            mensaje = "La cita cambió mientras la cancelabas.";
                        } else {
                            PreparedStatement pl = cn.prepareStatement("INSERT INTO tt_auditoria (accion, id_usuario, id_entidad, detalle, usuario_registro) VALUES ('CITA_CANCELADA', ?, ?, ?, ?)");
                            pl.setInt(1, idUsuario);
                            pl.setInt(2, idCita);
                            pl.setString(3, rc.getString("codigo_cita") + " Motivo: " + motivo);
                            pl.setString(4, usuario);
                            pl.executeUpdate();
                            PreparedStatement pd = cn.prepareStatement("SELECT d.correo, d.celular, d.nombres FROM tt_paciente p JOIN tt_datos_personales d "
                                    + "ON d.id_datos_personales = p.id_datos_personales WHERE p.id_paciente = ?");
                            pd.setInt(1, rc.getInt("id_paciente"));
                            ResultSet rd = pd.executeQuery();
                            if (rd.next()) {
                                String correo = rd.getString(1);
                                String celular = rd.getString(2);
                                String asunto = "Cita cancelada " + rc.getString("codigo_cita");
                                String cuerpo = "Hola " + rd.getString(3) + ", tu cita " + rc.getString("codigo_cita") + " fue cancelada. Motivo: " + motivo;
                                if (correo != null && !correo.isEmpty()) {
                                    System.out.println("CORREO a " + correo + ": " + asunto + " - " + cuerpo);
                                }
                                if (celular != null && !celular.isEmpty()) {
                                    String sms = cuerpo.length() > 160 ? cuerpo.substring(0, 157) + "..." : cuerpo;
                                    System.out.println("SMS a " + celular + ": " + sms);
                                }
                            }
                            mensaje = "Cita cancelada.";
                        }
                    }
                }
            } else if ("llegada".equals(accion) || "atendida".equals(accion) || "noasistio".equals(accion)) {
                int idCita = Integer.parseInt(request.getParameter("id"));
                PreparedStatement pc = cn.prepareStatement("SELECT c.*, dm.id_usuario AS usuario_medico FROM tt_cita c JOIN tt_medico m ON m.id_medico = c.id_medico "
                        + "JOIN tt_datos_personales dm ON dm.id_datos_personales = m.id_datos_personales WHERE c.id_cita = ?");
                pc.setInt(1, idCita);
                ResultSet rc = pc.executeQuery();
                if (!rc.next()) {
                    mensaje = "La cita no existe.";
                } else {
                    String est = rc.getString("estado_cita");
                    LocalDate fecha = rc.getDate("fecha").toLocalDate();
                    LocalDateTime fin = LocalDateTime.of(fecha, rc.getTime("hora_fin").toLocalTime());
                    String nuevoEstado = null;
                    String nota = request.getParameter("nota") == null ? "" : request.getParameter("nota").trim();
                    if ("llegada".equals(accion)) {
                        if (rol != 1 && rol != 2) {
                            mensaje = "Solo recepción registra la llegada.";
                        } else if (!"RESERVADA".equals(est)) {
                            mensaje = "Esta cita ya no está reservada.";
                        } else if (!fecha.equals(LocalDate.now())) {
                            mensaje = "La llegada solo se registra el día de la cita.";
                        } else {
                            nuevoEstado = "CONFIRMADA";
                        }
                    } else {
                        boolean suMedico = rol == 3 && rc.getInt("usuario_medico") == idUsuario;
                        if (!suMedico && rol != 1) {
                            mensaje = "La cita no es de tu agenda.";
                        } else if (!"RESERVADA".equals(est) && !"CONFIRMADA".equals(est)) {
                            mensaje = "Esta cita ya no está activa.";
                        } else if (nota.length() > 500) {
                            mensaje = "La nota puede tener hasta 500 caracteres.";
                        } else if ("atendida".equals(accion)) {
                            if (fecha.isAfter(LocalDate.now())) {
                                mensaje = "Todavía no es el día de esta cita.";
                            } else {
                                nuevoEstado = "ATENDIDA";
                            }
                        } else {
                            if ("CONFIRMADA".equals(est)) {
                                mensaje = "El paciente ya llegó: márcala como atendida.";
                            } else if (!fin.isBefore(LocalDateTime.now())) {
                                mensaje = "Solo se marca No asistió cuando ya pasó la hora.";
                            } else {
                                nuevoEstado = "NO_ASISTIO";
                            }
                        }
                    }
                    if (nuevoEstado != null) {
                        PreparedStatement pu = cn.prepareStatement("UPDATE tt_cita SET estado_cita = ?, nota_atencion = COALESCE(NULLIF(?, ''), nota_atencion), "
                                + "fecha_llegada = CASE WHEN ? = 'CONFIRMADA' THEN NOW() ELSE fecha_llegada END, "
                                + "fecha_atencion = CASE WHEN ? = 'ATENDIDA' THEN NOW() ELSE fecha_atencion END "
                                + "WHERE id_cita = ? AND estado_cita = ?");
                        pu.setString(1, nuevoEstado);
                        pu.setString(2, nota);
                        pu.setString(3, nuevoEstado);
                        pu.setString(4, nuevoEstado);
                        pu.setInt(5, idCita);
                        pu.setString(6, est);
                        if (pu.executeUpdate() == 0) {
                            mensaje = "La cita cambió mientras la registrabas.";
                        } else {
                            PreparedStatement pl = cn.prepareStatement("INSERT INTO tt_auditoria (accion, id_usuario, id_entidad, detalle, usuario_registro) VALUES (?, ?, ?, ?, ?)");
                            pl.setString(1, "CONFIRMADA".equals(nuevoEstado) ? "CITA_LLEGADA" : "ATENDIDA".equals(nuevoEstado) ? "CITA_ATENDIDA" : "CITA_NO_ASISTIO");
                            pl.setInt(2, idUsuario);
                            pl.setInt(3, idCita);
                            pl.setString(4, rc.getString("codigo_cita"));
                            pl.setString(5, usuario);
                            pl.executeUpdate();
                            mensaje = "Listo.";
                        }
                    }
                }
            } else {
                mensaje = "Acción no reconocida.";
            }
        } catch (SQLException | RuntimeException e) {
            mensaje = "No se pudo completar la operación: " + e.getMessage();
        }
        session.setAttribute("mensaje", mensaje);
        response.sendRedirect(volver);
    }
}
