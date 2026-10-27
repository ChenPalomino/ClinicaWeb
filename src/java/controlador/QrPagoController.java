package controlador;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import modelo.servicios.BilleteraService;

import java.io.IOException;
import java.util.Optional;

@WebServlet(name = "QrPagoController", urlPatterns = {"/qr-pago"})
public class QrPagoController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            Optional<Object[]> qr = new BilleteraService().qr();
            if (qr.isEmpty()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            byte[] bytes = (byte[]) qr.get()[0];
            response.setContentType((String) qr.get()[1]);
            response.setHeader("Cache-Control", "private, max-age=86400");
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setContentLength(bytes.length);
            response.getOutputStream().write(bytes);
        } catch (RuntimeException e) {
            System.err.println("QrPagoController: " + e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
