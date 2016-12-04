package saferoute.struct;

import java.io.Serializable;
import java.sql.Date;

/**
 * Estructura auxiliar para operar con un usuario
 */
public class Persona implements Serializable{

    public boolean administrador=false;
    public int idUsuario;
    public int idCreador;
    public int idColegio;
    public String nombreUsuario;
    public String email;
    public String clave;
    public String nombreReal;
    public String nombreNino;
    public byte sexoTutor;
    public byte sexoNino;
    public String direccion;
    public String localidad;
    public String provincia;
    public int codigoPostal;
    public double lat;
    public double lon;
    public int parentesco;
    public Date fnacPadre;
    public Date fnacNino;
    public String naciolidadPadre;
    public String nacionalidadHijo;
    public String profesion;
    public byte confirmado;
    public int alertaMail;
}
