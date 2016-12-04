package saferoute.comunicacion;

import java.io.FileInputStream;
import java.util.List;
import org.apache.commons.mail.*;
import saferoute.bd.Propiedades;
import saferoute.log.LogSR;

/**
 * Manejo de envío de emails.
 */
public class ServidorEmailUgr {

    /**
     * Crea una instancia con el servidor de emails.
     */
    public ServidorEmailUgr() {
    }

    /**
     * Envía un email a varios usuarios ocultando que se ha enviado a varias personas.
     * @param destinatarios
     * @param asunto
     * @param mensaje 
     */
    public void enviarEmail(List<String> destinatarios, String asunto, String mensaje) {
        try {
            HtmlEmail email = new HtmlEmail();
            //Servidor
            email.setSmtpPort(Integer.parseInt(Propiedades.getPropiedades().leerPropiedad(Propiedades.mailpuerto)));
            email.setHostName(Propiedades.getPropiedades().leerPropiedad(Propiedades.mailservidor));
            email.setAuthenticator(new DefaultAuthenticator(Propiedades.getPropiedades().leerPropiedad(Propiedades.mailusuario), 
                    Propiedades.getPropiedades().leerPropiedad(Propiedades.mailusuarioclave)));
            email.setStartTLSRequired(true);

            //Incluir destinatarios como ocultos
            for (String dir : destinatarios) {
                email.addBcc(dir);
            }

            //Cabecera del mensaje
            email.addTo(Propiedades.getPropiedades().leerPropiedad(Propiedades.mailremitente), "Servicio de rutas seguras");
            email.setFrom(Propiedades.getPropiedades().leerPropiedad(Propiedades.mailremitente), "Servicio de rutas seguras");
            email.setSubject(asunto);

            //Mensaje
            email.setHtmlMsg(mensaje);

            // set the alternative message
            email.setTextMsg("Servicio de rutas seguras.");

            //Enviar
            email.send();

        } catch (EmailException ex) {
            LogSR.mensaje("Error al enviar un email. " + ex.getMessage(), LogSR.ERROR);
        }
    }

    /**
     * Envía un email a un usuario.
     * @param destinatario
     * @param asunto
     * @param mensaje 
     */
    public void enviarEmail(String destinatario, String asunto, String mensaje) {
        try {
            HtmlEmail email = new HtmlEmail();
            //Servidor
            email.setSmtpPort(Integer.parseInt(Propiedades.getPropiedades().leerPropiedad(Propiedades.mailpuerto)));
            email.setHostName(Propiedades.getPropiedades().leerPropiedad(Propiedades.mailservidor));
            email.setAuthenticator(new DefaultAuthenticator(Propiedades.getPropiedades().leerPropiedad(Propiedades.mailusuario), Propiedades.getPropiedades().leerPropiedad(Propiedades.mailusuarioclave)));
            email.setStartTLSRequired(true);

            //Incluir destinatarios 
            email.addTo(destinatario);

            //Cabecera del mensaje
            email.setFrom(Propiedades.getPropiedades().leerPropiedad(Propiedades.mailremitente), "Servicio de rutas seguras");
            email.setSubject(asunto);

            //Mensaje
            email.setHtmlMsg(mensaje);

            // set the alternative message
            email.setTextMsg("Servicio de rutas seguras.");

            //Enviar
            email.send();

        } catch (EmailException ex) {
            LogSR.mensaje("Error al enviar un email. " + ex.getMessage(), LogSR.ERROR);
        }
    }
    
    /**
     * Lee un fichero de texto.
     * @param ruta
     * @return 
     */
    public String leerFicheroTexto(String ruta){
        String texto="";
        try {
            FileInputStream fis = new FileInputStream(ruta);
            byte[] b = new byte[fis.available()];
            fis.read(b);
            texto = new String(b);
            
        } catch (Exception ex) {
            LogSR.mensaje("Error al leer el archivo "+ruta+"\n" + ex.getMessage(), LogSR.ERROR);
        }
        
        return texto;
    }
}
