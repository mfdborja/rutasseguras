package saferoute.bd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import saferoute.log.LogSR;

public class Propiedades {

    //Variables globales
    public static final String mailremitente = "mailremitente";
    public static final String mailservidor = "mailservidor";
    public static final String mailpuerto = "mailpuerto";
    public static final String mailusuario = "mailusuario";
    public static final String mailusuarioclave = "mailusuarioclave";
    public static final String mensajepie = "mensajepie";
    public static final String mensajenuevousuario = "mensajenuevousuario";
    public static final String mensajenuevoadministrador = "mensajenuevoadministrador";
    public static final String mensajetiempo = "mensajetiempo";
    public static final String mensajecambioclave = "mensajecambioclave";

    public static final String tiempourlxml = "tiempourlxml";
    public static final String tiempodesfaseprevision = "tiempodesfaseprevision";
    public static final String tiempoprobabilidadminima = "tiempoprobabilidadminima";

    public static final String aplicacionmapa = "aplicacionmapa";
    public static final String aplicacionbd = "aplicacionbd";
    public static final String aplicacionpuerto = "aplicacionpuerto";
    public static final String aplicacionvariables="aplicacionvariables";
    public static final String aplicacioncolasocket="aplicacioncolasocket";
    public static final String aplicacioniptomcat="aplicacioniptomcat";

    private static Propiedades miPropiedades = null;
    private Properties p;
    private final String ruta = "datos/configuraciongeneral";

    private Propiedades() {
        p = new Properties();
    }

    public static Propiedades getPropiedades() {
        if (miPropiedades == null) {
            miPropiedades = new Propiedades();
        }

        return miPropiedades;
    }

    /**
     * Guarda una propiedad en el archivo de configuración. Lo crea si es
     * necesario.
     *
     * @param key
     * @param valor
     */
    private void crearPropiedades(String key, String valor) {
        //Si existe propiedades
        File archivo = new File(ruta);

        if (archivo.exists()) {
            try {
                FileInputStream flujoEntrada = new FileInputStream(archivo);
                p.load(flujoEntrada);
            } catch (IOException ex) {
                LogSR.mensaje("Error al leer el archivo de configuración general. " + ex.getMessage(), LogSR.ERROR);
            }
        }

        //Crear la clave
        p.setProperty(key, valor);

        //Guardar archivo
        try {
            FileOutputStream flujoSalida = new FileOutputStream(archivo);
            p.store(flujoSalida, "Archivo de configuración general de la plataforma de rutas seguras.");
        } catch (IOException ex) {
            LogSR.mensaje("Error al guardar el archivo de configuración general. " + ex.getMessage(), LogSR.ERROR);
        }
    }

    public String leerPropiedad(String key){
        return leerPropiedad(key, "0");
    }
    public String leerPropiedad(String key, String valorDefecto) {
        //Si existe propiedades
        File archivo = new File(ruta);
        if (archivo.exists()) {
            try {
                FileInputStream flujoEntrada = new FileInputStream(archivo);
                p.load(flujoEntrada);
            } catch (IOException ex) {
                LogSR.mensaje("Error al leer el archivo de configuración general. " + ex.getMessage(), LogSR.ERROR);
            }
        } else {
            //Si no existe la propiedad no existe y se crea
            crearPropiedades(key, valorDefecto);
            return valorDefecto;
        }

        //Si llega aquí es que el archivo existe
        //Si no existe la clave
        if (p.getProperty(key) == null) {
            //Si no existe la propiedad no existe y se crea
            crearPropiedades(key, valorDefecto);
            return valorDefecto;
        }

        //Si llega aquí es que existe
        return p.getProperty(key);
    }

    public File getArchivo() {
        return new File(ruta);
    }

}
