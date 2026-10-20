package evento;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/** Al iniciar la aplicación, suscribe los observadores al EventoSubject. */
@WebListener
public class ObservadoresListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        EventoSubject.getInstancia().suscribir(new AuditoriaObserver());
        EventoSubject.getInstancia().suscribir(new AccesoCambiadoObserver());
        EventoSubject.getInstancia().suscribir(new NotificadorCitasObserver());     // RF-09: avisos de citas
        System.out.println("ObservadoresListener: auditoría, control de accesos y avisos de citas activados.");
    }
}
