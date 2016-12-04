package saferoute.struct;

import java.sql.Date;
import java.util.GregorianCalendar;

/**
 * Estructura auxiliar para información de tramos.
 */
public class Calle {
    public int id;
    public Date fechaActualizacion= new Date(new GregorianCalendar().getTimeInMillis());
    public int idcreador=-1;
    public int privado=0;
    public int elevar=0;
    public double latitud1=0d;
    public double longitud1=0d;
    public double latitud2=0d;
    public double longitud2=0d;
    public int densidadtrafico=5;
    public int acera=5;
    public int anchocalle=5;
    public int indicecriminalidad=5;
    public int ratioaccidentes=5;
    public int superficieadoquines=0;
    public int superficielisa=1;
    public int pasopeatones=5;
    public int semaforos=5;
    public int residencialcomercial=0;
    public int conservacionedificios=5;
    public int niveleconomico=5;
    public int iluminacion=5;
    public int velocidadmaxima=50;
    public int callepeatonal=0;
    public int carrilbici=0;
    public int calidadcarrilbici=5;
    public int separacioncalzadaacera=0;
    public int aparcamientoacera=1;
    public int badenes=0;
    public int radar=0;
    public int pendiente=10;
    public int confort=5;
    public int policía=0;
}
