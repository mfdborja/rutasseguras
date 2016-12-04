package saferoute.log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

public class LogSR {

    public static final byte ERROR = 0;
    public static final byte INFO = 1;
    public static final byte ESTADISTICAS=2;

    public static void mensaje(String mensaje, byte tipo) {
        String salida;
        if (tipo == ERROR) {
            salida = "ERROR - " + new Date().toString() + " \n"
                    + mensaje+ " \n\n";
            try {
                FileOutputStream os = new FileOutputStream("datos/errores.log", true);
                os.write(salida.getBytes());
            } catch (FileNotFoundException ex) {
            } catch (IOException ex) {
                System.err.println("Error al crear el archivo de información. "+ex.getMessage());
            }

        } else if (tipo == INFO) {
            salida = "INFO - " + new Date().toString() + " \n"
                    + mensaje+ " \n\n";
            try {
                FileOutputStream os = new FileOutputStream("datos/info.log", true);
                os.write(salida.getBytes());
            } catch (FileNotFoundException ex) {
            } catch (IOException ex) {
                System.err.println("Error al crear el archivo de información. "+ex.getMessage());
            }
        }else if (tipo == ESTADISTICAS) {
            salida = "" + new Date().toString() +"\t"+ mensaje+ "\n";
            try {
                FileOutputStream os = new FileOutputStream("datos/estadisticas.log", true);
                os.write(salida.getBytes());
            } catch (FileNotFoundException ex) {
            } catch (IOException ex) {
                System.err.println("Error al crear el archivo de información. "+ex.getMessage());
            }
        }
    }
}
