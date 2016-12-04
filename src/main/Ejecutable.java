package main;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import org.apache.log4j.BasicConfigurator;
import saferoute.base.Mapa;
import saferoute.bd.Propiedades;
import saferoute.log.LogSR;

public class Ejecutable {

    /**
     * Main de la aplicación
     *
     * @param args Argumentos de la aplicación
     */
    public static void main(String[] args) {
        //Variables para controlar el tiempo       
        Date inicio;
        Date fin;

        //Configurar log4j
        BasicConfigurator.configure();

        //Crear configuración
        if (!Propiedades.getPropiedades().getArchivo().exists()) {
            crearConfiguracion();
        }

        //Configurar instancia del mapa
        inicio = new Date();
        Mapa mapa = Mapa.getMapa();
        //Inicia el cron con las alertas meteorológicas
        mapa.activarAlertasTiempo();
        fin = new Date();

        //Información
        LogSR.mensaje("Instancia de mapa creada en (s): " + ((fin.getTime() - inicio.getTime()) / 1000.0), LogSR.INFO);

        //Escuchar conexiones continuamente
        try {
            ServerSocket escuchar = new ServerSocket(Integer.parseInt(Propiedades.getPropiedades().leerPropiedad(Propiedades.aplicacionpuerto)),
                    Integer.parseInt(Propiedades.getPropiedades().leerPropiedad(Propiedades.aplicacioncolasocket))/*,
                    InetAddress.getByName(Propiedades.getPropiedades().leerPropiedad(Propiedades.aplicacioniptomcat))*/);
            LogSR.mensaje("Esperando conexiones en " + escuchar.getInetAddress().toString() + ":" + escuchar.getLocalPort(), LogSR.INFO);

            //Bucle que espera conexiones
            try {
                while (true) {
                    Socket tempSocket = escuchar.accept();

                    //Lanzar hilo que atienda las peticiones
                    Manejador manejador = new Manejador(tempSocket, mapa);
                    LogSR.mensaje("Hilo abierto con: " + tempSocket.getRemoteSocketAddress().toString(), LogSR.INFO);
                    manejador.start();
                }
            } catch (Exception ex) {
                //Excepciones para un cliente
                LogSR.mensaje("Error al crear el socket de escucha para un cliente: "
                        + ex.getMessage(), LogSR.ERROR);
            }
        } catch (Exception ex) {
            //Excepciones para escuchar
            System.err.println("Error al crear el socket de escucha: "
                    + ex.getMessage());
            LogSR.mensaje("Error al crear el socket de escucha: "
                    + ex.getMessage(), LogSR.ERROR);
        }
    }

    /**
     * Crea un archivo de configuración con datos por defecto.
     */
    private static void crearConfiguracion() {
        //Si el archivo de configuración no existe se crea y se rellena con datos tipo
        //Configuración mail
        Propiedades.getPropiedades().leerPropiedad("mailremitente", "correodeladministrador@ugr.es");
        Propiedades.getPropiedades().leerPropiedad("mailservidor", "smtp.servidor.com");
        Propiedades.getPropiedades().leerPropiedad("mailpuerto", "xxxxx");
        Propiedades.getPropiedades().leerPropiedad("mailusuario", "usuarioparaelcorreo");
        Propiedades.getPropiedades().leerPropiedad("mailusuarioclave", "claveusuario");

        //Archivos para email
        Propiedades.getPropiedades().leerPropiedad("mensajepie", "datos/pie.html");
        Propiedades.getPropiedades().leerPropiedad("mensajenuevousuario", "datos/nuevousuario.html");
        Propiedades.getPropiedades().leerPropiedad("mensajenuevoadministrador", "datos/nuevoadministrador.html");
        Propiedades.getPropiedades().leerPropiedad("mensajetiempo", "datos/eltiempo.html");
        Propiedades.getPropiedades().leerPropiedad("mensajecambioclave", "datos/cambioclave.html");

        //Configuración de alertas meteorológicas
        Propiedades.getPropiedades().leerPropiedad("tiempourlxml", "ruta del xml con la previsión de aemet");
        Propiedades.getPropiedades().leerPropiedad("tiempodesfaseprevision", "1");
        Propiedades.getPropiedades().leerPropiedad("tiempoprobabilidadminima", "50");

        //Configuración de la aplicación
        Propiedades.getPropiedades().leerPropiedad("aplicacionmapa", "datos/mapa.pbf");
        Propiedades.getPropiedades().leerPropiedad("aplicacionbd", "datos/bdgen");
        Propiedades.getPropiedades().leerPropiedad("aplicacionpuerto", "25250");
        Propiedades.getPropiedades().leerPropiedad("aplicacionvariables", "datos/variables");
        Propiedades.getPropiedades().leerPropiedad("aplicacioncolasocket", "50");
        Propiedades.getPropiedades().leerPropiedad("aplicacioniptomcat", "127.0.0.1");

        System.out.println("Se ha creado un archivo de configuración para la plataforma. \n"
                + "Actualice los datos de configuración en: \n"
                + Propiedades.getPropiedades().getArchivo().getAbsolutePath() + " \ny reinicie el servidor.\n");
        System.exit(0);
    }
}
