package saferoute.bd;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;
import saferoute.log.LogSR;
import saferoute.struct.Calle;
import saferoute.struct.Persona;

/**
 * Permite la interacción con la base de datos del programa
 */
public class BaseDatos {

    //Variables estáticas
    public static final byte VALOR_DESCONOCIDO = -1;
    //public static final byte VALOR_NO_CAMBIA = -2;
    public static final byte DATO_PUBLICO = -1;
    public static final byte DATO_PARA_REVISION = -2;
    public static final byte SEXO_HOMBRE = 0;
    public static final byte SEXO_MUJER = 1;
    public static final byte FALSO = 0;
    public static final byte VERDADERO = 1;
    public static final byte PADRE_MADRE = 0;
    public static final byte ABUELO = 1;
    public static final byte TIO = 2;
    public static final byte PRIMO = 3;
    public static final byte HERMANO = 4;
    public static final byte AMIGO_DEL_PADRE = 5;
    public static final byte TUTOR_LEGAL = 6;

    //Conexión a la BD
    private Connection conexion;
    //Existe previamente la BD o se ha de crear
    boolean existe = false;

    /*
     * Base de datos
     */
    /**
     * Crea un objeto para interactuar con la base de datos.
     *
     */
    public BaseDatos() {
        //Si no existe anteriormente se crea
        File archivoBD = new File(Propiedades.getPropiedades().leerPropiedad(Propiedades.aplicacionbd));
        if (archivoBD.exists()) {
            existe = true;
        }
    }

    /**
     * Crea una conexión con la BD. Si no existiera la bd la crea.
     *
     * @return si se ha realizado con éxito.
     */
    public boolean conectarLaBD() {
        boolean exito = false;
        try {
            //Crear conexión
            Class.forName("org.sqlite.JDBC");
            conexion = (Connection) DriverManager.getConnection("jdbc:sqlite:" + Propiedades.getPropiedades().leerPropiedad(Propiedades.aplicacionbd));
            conexion.setAutoCommit(false);
            
            //Crear tablas si es la primera vez
            if (!existe) {
                crearBD();
            }
            exito = true;
        } catch (ClassNotFoundException | SQLException ex) {
            LogSR.mensaje("Error al conectar la base de datos. " + ex.getMessage(), LogSR.ERROR);
        }
        return exito;
    }

    /**
     * Desconecta la BD de forma segura.
     */
    public void desconectarBD() {
        if (conexion != null) {
            try {
                conexion.commit();
                conexion.close();
            } catch (SQLException ex) {
                LogSR.mensaje("Error al cerrar la BD. " + ex.getMessage(), LogSR.ERROR);
            }
        }
    }

    /**
     * Crea las tablas de la bd.
     */
    private void crearBD() {
        try {
            //Creando las tablas
            PreparedStatement st;

            //Tabla usuarios
            st = conexion.prepareStatement("create table usuarios(\n"
                    + "id integer, --Identificador único del usuario\n"
                    + "idcreador integer, --Id del administrador que lo crea\n"
                    + "idcolegio integer, --Colegio asociado al niño\n"
                    + "nombreusuario varchar(25), --Nombre que será mostrado a los usuarios \n"
                    + "clave varchar(150), --Clave codificada de un usuario\n"
                    + "email varchar(320), --Debe ser único, sirve para logearse\n"
                    + "nombrereal varchar(150),\n"
                    + "nombrenino varchar(50), --Para mostrar como padre de ...\n"
                    + "sexotutor integer, -- 0 para hombre, 1 para mujer\n"
                    + "sexonino integer,\n"
                    + "direccion varchar(150),\n"
                    + "localidad varchar(50),\n"
                    + "provincia varchar(50),\n"
                    + "codigopostal integer,\n"
                    + "latitud numeric(12,8),\n"
                    + "longitud numeric(12,8),\n"
                    + "parentesco integer, --Padre/Madre 0, abuelo 1, tio 2, Primo 3, Hermano 4, Amigo del padre 5, tutor legal 6\n"
                    + "fnacimientopadre date,\n"
                    + "fnacimientonino date,\n"
                    + "nacionalidadpadre date,\n"
                    + "nacionalidadhijo date,\n"
                    + "profesion varchar(25),\n"
                    + "confirmado integer, --0 si no ha confimado su email, 1 si lo ha confirmado\n"
                    + "activo integer, --0 si el usuario no es utilizable, 1 si el usuario es funcional (por defecto)\n"
                    + "fechaCreacion date,"
                    + "alertaMail integer"
                    + ")"
            );
            st.execute();
            st.close();

            //Tabla administradores
            st = conexion.prepareStatement("create table administradores(id integer)"
            );
            st.execute();
            st.close();

            //Tabla lugares
            st = conexion.prepareStatement("create table lugares(\n"
                    + "id integer,--Id único para el lugar en la bd\n"
                    + "tipo integer, -- 0 colegio, 1 monitor, 3 instituto, 4 guardería, 5 universidad, 6 museo, 7 polideportivo, 8 parque, 9 otros\n"
                    + "nombre varchar(100), --Nombre que se mostrará\n"
                    + "latitud numeric(12,8),\n"
                    + "longitud numeric(12,8)\n"
                    + ")"
            );
            st.execute();
            st.close();

            //Tabla iconos
            st = conexion.prepareStatement("create table iconos(\n"
                    + "id integer,\n"
                    + "idcreador integer, -- -1 si es público, -2 si un usuario privado lo propone para público y espera aceptación, id del usuario creador si es privado\n"
                    + "tipo integer, --Grado de alerta\n"
                    + "comentario varchar(500), --Comentario que se podrá leer.\n"
                    + "latitud numeric(12,8),\n"
                    + "longitud numeric(12,8)"
                    + ")"
            );
            st.execute();
            st.close();

            //Tabla iconos
            st = conexion.prepareStatement("create table seguridad(\n"
                    + "id integer, --Id único en la bd. Aconsejable el de graphopper.\n"
                    + "fechaActualizacion date,"
                    + "idcreador integer, --Id del que introdujo el dato\n"
                    + "privado integer, -- 0 si es público, 1 si es un dato privado del creador\n"
                    + "elevar integer, -- 0 si no se desea mandar a administrador, 1 si se desea compartir en público.\n"
                    + "latitud1 numeric(12,8), --Los dos nodos que componen el tramo\n"
                    + "longitud1 numeric(12,8),\n"
                    + "latitud2 numeric(12,8),\n"
                    + "longitud2 numeric(12,8),\n"
                    + "densidadtrafico integer,\n"
                    + "acera integer,\n"
                    + "anchocalle integer,\n"
                    + "indicecriminalidad integer,\n"
                    + "ratioaccidentes integer,\n"
                    + "superficieadoquines integer, --0 si falso, 1 si verdadero\n"
                    + "superficielisa integer, -- 0 si false, 1 si verdadero\n"
                    + "pasopeatones integer, \n"
                    + "semaforos integer,\n"
                    + "residencialcomercial integer, --Indica el movimiento de personas de la calle\n"
                    + "conservacionedificios integer,\n"
                    + "niveleconomico integer,\n"
                    + "iluminacion integer,\n"
                    + "velocidadmaxima integer,\n"
                    + "callepeatonal integer, -- 0 si no es peatonal, 1 si tiene preferencia peatonal\n"
                    + "carrilbici integer, -- 0 si no es carril bici, 1 si es o tiene carril bici\n"
                    + "calidadcarrilbici integer, \n"
                    + "separacioncalzadaacera integer, -- 0 falso, 1 verdadero\n"
                    + "aparcamientoacera integer, --idem\n"
                    + "badenes integer, --idem\n"
                    + "radar integer, --idem\n"
                    + "pendiente integer, \n"
                    + "confort integer, --Indica el balance entre diversos factores como el ruido, contaminación, zonas de ocio, de paseo...\n"
                    + "policía integer -- 0 falso, 1 verdadero\n"
                    + ")"
            );
            st.execute();
            st.close();

            //Tabla propiedades
            st = conexion.prepareStatement("create table propiedades(\n"
                    + "clave varchar(50),\n"
                    + "valor varchar(5000)\n"
                    + ")"
            );
            st.execute();
            st.close();

            //Tabla rutas
            st = conexion.prepareStatement("create table rutas(\n"
                    + "id int,\n"
                    + "fecha date,\n"
                    + "kml varchar(500000),\n"
                    + "instrucciones varchar(50000)\n"
                    + ")");
            st.execute();
            st.close();

            //Tabla alertas
            st = conexion.prepareStatement("create table alertas(\n"
                    + "id int,\n"
                    + "idcreador int,\n"
                    + "titulo varchar(50),\n"
                    + "texto varchar(250),\n"
                    + "fechacreacion date,\n"
                    + "fechavalidez date\n"
                    + ");");
            st.execute();
            st.close();

            //Tabla foro
            st = conexion.prepareStatement("create table foro(\n"
                    + "id int, \n"
                    + "idpadre int, -- -1 sin padre, sino id del padre\n"
                    + "idautor int,\n"
                    + "fecha date,\n"
                    + "titulo varchar(50),\n"
                    + "mensaje varchar(5000)\n"
                    + ");");
            st.execute();
            st.close();

            //Itroducir datos en la base de datos     
            /*public int putUsuario(int idUsuario, int idCreador, int idColegio, String nombreUsuario,
             String clave, String email, String nombreReal, String nombreNino, byte sexoTutor, byte sexoNino,
             String direccion, String localidad, String provincia, int codigoPostal, double lat, double lon,
             int parentesco, Date fnacPadre, Date fnacNino, String naciolidadPadre, String nacionalidadHijo,
             String profesion, byte confirmado)*/
            this.putUsuario(VALOR_DESCONOCIDO, 0, 0, "Superadministrador", encriptarClave("super"),
                    Propiedades.getPropiedades().leerPropiedad(Propiedades.mailremitente), "Superadministrador", "", SEXO_HOMBRE, SEXO_HOMBRE,
                    "", "", "", 0, 0.0, 0.0,
                    TUTOR_LEGAL, new Date(0), new Date(0), "", "",
                    "", VERDADERO, VERDADERO);
            //En la lista de administradores
            st = conexion.prepareStatement("insert into administradores ("
                    + "id) values("
                    + "?)");
            st.setInt(1, 0);

            //Insertar
            st.execute();
            st.close();
            conexion.commit();

            //Confirmar los cambios
            conexion.commit();
        } catch (SQLException ex) {
            LogSR.mensaje("Error al crear las tablas de la BD. " + ex.getMessage(), LogSR.ERROR);

            try {
                conexion.close();
            } catch (SQLException ex2) {
                LogSR.mensaje("Error al crear las tablas de la BD. Cerrar la bd." + ex2.getMessage(), LogSR.ERROR);
            }

            new File(Propiedades.getPropiedades().leerPropiedad(Propiedades.aplicacionbd)).delete();
        }
    }

    /**
     * Comprueba si la conexión está levantada. Sino la levanta.
     */
    private void avivarConexion() {
        if (conexion != null) {
            try {
                if (conexion.isClosed()) {
                    conexion = null;
                }
            } catch (SQLException ex) {

            }
        }

        if (conexion == null) {
            conectarLaBD();
        }
    }

    /*
     USUARIOS
     */
    /**
     * Crea un usuario en la BD.
     *
     * @return El id del usuario. -1 si no lo encuentra en una actualización.
     */
    public int putUsuario(int idUsuario, int idCreador, int idColegio, String nombreUsuario,
            String clave, String email, String nombreReal, String nombreNino, byte sexoTutor, byte sexoNino,
            String direccion, String localidad, String provincia, int codigoPostal, double lat, double lon,
            int parentesco, Date fnacPadre, Date fnacNino, String naciolidadPadre, String nacionalidadHijo,
            String profesion, byte confirmado, int alertaMail) {
        byte utilizable = VERDADERO;

        //Crear o actualizar
        if (idUsuario == VALOR_DESCONOCIDO) {
            try {
                //Comprueba el número de usuarios de la BD para asignarle un id.
                Statement consulta = conexion.createStatement();
                ResultSet resultado = consulta.executeQuery("SELECT count(*) FROM usuarios");
                resultado.next();
                idUsuario = resultado.getInt(1);

                //Realizar el insert
                PreparedStatement st = conexion.prepareStatement("insert into usuarios ("
                        + "id, idcreador, idcolegio, nombreusuario, clave, email, nombrereal, "
                        + "nombrenino, sexotutor, sexonino, direccion, localidad, provincia, "
                        + "codigopostal, latitud, longitud, parentesco, fnacimientopadre, fnacimientonino, "
                        + "nacionalidadpadre, nacionalidadhijo, profesion, confirmado, activo, fechaCreacion, alertaMail) values("
                        + "?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

                st.setInt(1, idUsuario);
                st.setInt(2, idCreador);
                st.setInt(3, idColegio);
                st.setString(4, nombreUsuario);
                st.setString(5, clave);
                st.setString(6, email);
                st.setString(7, nombreReal);
                st.setString(8, nombreNino);
                st.setInt(9, sexoTutor);
                st.setInt(10, sexoNino);
                st.setString(11, direccion);
                st.setString(12, localidad);
                st.setString(13, provincia);
                st.setInt(14, codigoPostal);
                st.setDouble(15, lat);
                st.setDouble(16, lon);
                st.setInt(17, parentesco);
                st.setDate(18, fnacPadre);
                st.setDate(19, fnacNino);
                st.setString(20, naciolidadPadre);
                st.setString(21, nacionalidadHijo);
                st.setString(22, profesion);
                st.setInt(23, confirmado);
                st.setInt(24, utilizable);
                st.setDate(25, new Date(new GregorianCalendar().getTime().getTime()));
                st.setInt(26, alertaMail);

                //Insertar
                st.execute();
                st.close();
                conexion.commit();
            } catch (SQLException ex) {
                LogSR.mensaje("Error al introducir la tupla de usuario. " + ex.getMessage(), LogSR.ERROR);
            }
        } else {
            //Actualizar tupla

            try {
                //Guardar clave
                Statement consulta = conexion.createStatement();
                ResultSet resultado = consulta.executeQuery("SELECT clave FROM usuarios where id=" + idUsuario);
                resultado.next();
                String claveAntigua = resultado.getString(1);
                //Borrar la tupla antigua
                PreparedStatement stBorrar = conexion.prepareStatement("delete from usuarios where id=" + idUsuario);
                int cont = stBorrar.executeUpdate();
                if (cont == 0) {
                    idUsuario = -1;
                    throw new SQLException("No existe el usuario con id=" + idUsuario);
                } else {

                    //Realizar el insert
                    PreparedStatement st = conexion.prepareStatement("insert into usuarios ("
                            + "id, idcreador, idcolegio, nombreusuario, clave, email, nombrereal, "
                            + "nombrenino, sexotutor, sexonino, direccion, localidad, provincia, "
                            + "codigopostal, latitud, longitud, parentesco, fnacimientopadre, fnacimientonino, "
                            + "nacionalidadpadre, nacionalidadhijo, profesion, confirmado, activo, alertaMail) values("
                            + "?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

                    st.setInt(1, idUsuario);
                    st.setInt(2, idCreador);
                    st.setInt(3, idColegio);
                    st.setString(4, nombreUsuario);
                    boolean cambiarClave = false;
                    if (clave != null) {
                        if (!clave.isEmpty()) {
                            clave = encriptarClave(clave);
                            st.setString(5, clave);
                            cambiarClave = true;
                        }
                    }
                    if (!cambiarClave) {
                        st.setString(5, claveAntigua);
                    }
                    st.setString(6, email);
                    st.setString(7, nombreReal);
                    st.setString(8, nombreNino);
                    st.setInt(9, sexoTutor);
                    st.setInt(10, sexoNino);
                    st.setString(11, direccion);
                    st.setString(12, localidad);
                    st.setString(13, provincia);
                    st.setInt(14, codigoPostal);
                    st.setDouble(15, lat);
                    st.setDouble(16, lon);
                    st.setInt(17, parentesco);
                    st.setDate(18, fnacPadre);
                    st.setDate(19, fnacNino);
                    st.setString(20, naciolidadPadre);
                    st.setString(21, nacionalidadHijo);
                    st.setString(22, profesion);
                    st.setInt(23, confirmado);
                    st.setInt(24, utilizable);
                    st.setInt(25, alertaMail);

                    //Insertar
                    st.execute();
                    st.close();
                    conexion.commit();
                }
            } catch (SQLException ex) {
                LogSR.mensaje("Error al actualizar la tupla de usuario. " + ex.getMessage(), LogSR.ERROR);
            }
        }
        return idUsuario;
    }//Fin introUsuario

    /**
     * Busca un usuario por su email
     *
     * @param email El email del usuario
     * @return -1 si no existe, el id del usuario si lo encuentra.
     */
    public int buscarUsuario(String email) {
        int resultado = -1;

        try {
            //Comprueba el número de usuarios de la BD para asignarle un id.
            Statement consulta = conexion.createStatement();
            ResultSet resultadoConsulta = consulta.executeQuery("SELECT count(*) FROM usuarios where "
                    + "email='" + email + "'");
            resultadoConsulta.next();
            int contador = resultadoConsulta.getInt(1);

            //Si existe el usuario
            if (contador > 0) {
                resultadoConsulta = consulta.executeQuery("SELECT id FROM usuarios where "
                        + "email='" + email + "'");
                resultadoConsulta.next();
                resultado = resultadoConsulta.getInt(1);
            }

        } catch (SQLException ex) {
            LogSR.mensaje("Error al buscar el usuario. " + ex.getMessage(), LogSR.ERROR);
        }

        return resultado;
    }

    /**
     * Indica si un usuario está activo
     *
     * @param id Id del usuario
     * @return False si no existe el usuario o no está activo. True si está
     * activo.
     */
    public boolean esUsuarioActivo(int id) {
        boolean resultado = false;
        try {
            //Comprueba el número de usuarios de la BD para asignarle un id.
            Statement consulta = conexion.createStatement();
            ResultSet resultadoConsulta = consulta.executeQuery("SELECT count(*) FROM usuarios where "
                    + "id=" + id);
            resultadoConsulta.next();
            int contador = resultadoConsulta.getInt(1);

            //Si existe el usuario
            if (contador > 0) {
                resultadoConsulta = consulta.executeQuery("SELECT activo FROM usuarios where "
                        + "id=" + id);
                resultadoConsulta.next();
                int salida = resultadoConsulta.getInt(1);
                if (salida == VERDADERO) {
                    resultado = true;
                }
            }

        } catch (SQLException ex) {
            LogSR.mensaje("Error al comprobar el usuario. " + ex.getMessage(), LogSR.ERROR);
        }

        return resultado;
    }

    /**
     * Desactiva el usuario
     *
     * @param id
     * @return Si se ha desactivado correctamente
     */
    public boolean desactivarUsuario(int id) {
        boolean resultado = false;
        try {
            //Comprueba el número de usuarios de la BD para asignarle un id.
            Statement consulta = conexion.createStatement();
            ResultSet resultadoConsulta = consulta.executeQuery("SELECT count(*) FROM usuarios where "
                    + "id=" + id);
            resultadoConsulta.next();
            int contador = resultadoConsulta.getInt(1);

            //Si existe el usuario
            if (contador > 0) {
                //Borrar la tupla
                PreparedStatement stBorrar = conexion.prepareStatement("update usuarios set activo=" + FALSO + ", email='', clave='', nombrereal='', nombrenino='', alertamail=" + FALSO + "  where id=" + id);
                //stBorrar.executeUpdate();

                if (stBorrar.executeUpdate() >= 0) {
                    resultado = true;
                }
                conexion.commit();
            }

        } catch (SQLException ex) {
            LogSR.mensaje("Error al desactivar el usuario. " + ex.getMessage(), LogSR.ERROR);
        }

        return resultado;
    }

    /**
     * Devuelve los datos de una persona si exite.
     *
     * @param id
     * @return Datos de la persona o null.
     */
    public Persona getUsuario(int id) {
        Persona temp = null;
        try {
            Persona salida = new Persona();
            //Comprueba el número de usuarios de la BD para asignarle un id.
            Statement consulta = conexion.createStatement();
            ResultSet resultadoConsulta = consulta.executeQuery("SELECT count(*) FROM usuarios where "
                    + "id=" + id);
            resultadoConsulta.next();
            int contador = resultadoConsulta.getInt(1);

            if (contador > 0) {
                consulta = conexion.createStatement();
                resultadoConsulta = consulta.executeQuery("SELECT "
                        + "id, idcreador, idcolegio, nombreusuario, clave, email, nombrereal, "
                        + "nombrenino, sexotutor, sexonino, direccion, localidad, provincia, "
                        + "codigopostal, latitud, longitud, parentesco, fnacimientopadre, fnacimientonino, "
                        + "nacionalidadpadre, nacionalidadhijo, profesion, confirmado, activo, alertaMail"
                        + " from usuarios where id=" + id);

                resultadoConsulta.next();

                salida.idUsuario = resultadoConsulta.getInt(1);
                salida.idCreador = resultadoConsulta.getInt(2);
                salida.idColegio = resultadoConsulta.getInt(3);
                salida.nombreUsuario = resultadoConsulta.getString(4);
                salida.email = resultadoConsulta.getString(6);
                salida.nombreReal = resultadoConsulta.getString(7);
                salida.nombreNino = resultadoConsulta.getString(8);
                salida.sexoTutor = resultadoConsulta.getByte(9);
                salida.sexoNino = resultadoConsulta.getByte(10);
                salida.direccion = resultadoConsulta.getString(11);
                salida.localidad = resultadoConsulta.getString(12);
                salida.provincia = resultadoConsulta.getString(13);
                salida.codigoPostal = resultadoConsulta.getInt(14);
                salida.lat = resultadoConsulta.getDouble(15);
                salida.lon = resultadoConsulta.getDouble(16);
                salida.parentesco = resultadoConsulta.getInt(17);
                salida.fnacPadre = resultadoConsulta.getDate(18);
                salida.fnacNino = resultadoConsulta.getDate(19);
                salida.naciolidadPadre = resultadoConsulta.getString(20);
                salida.nacionalidadHijo = resultadoConsulta.getString(21);
                salida.profesion = resultadoConsulta.getString(22);
                salida.confirmado = resultadoConsulta.getByte(23);
                salida.clave = null;
                salida.alertaMail = resultadoConsulta.getInt(25);

                if (esAdministrador(id)) {
                    salida.administrador = true;
                }

                temp = salida;
            }
        } catch (SQLException ex) {
            LogSR.mensaje("Error al consultar la tupla de usuario. " + ex.getMessage(), LogSR.ERROR);
        }
        return temp;
    }

    /**
     * Devuelve los usuarios que penden de ese administrador.
     *
     * @param idCreador
     * @return
     */
    public List<Integer> getListaUsuariosDeUnAdmin(int idCreador) {
        List<Integer> salida = new ArrayList();
        try {
            //Comprueba el número de usuarios de la BD para asignarle un id.
            Statement consulta = conexion.createStatement();
            ResultSet resultadoConsulta;
            if (idCreador == 0) {
                //Si es el superadministrador devuelve todos los usuarios.
                resultadoConsulta = consulta.executeQuery("SELECT id FROM usuarios where activo=1");
            } else {
                //Si es un administrador normal, solo sus usuarios.
                resultadoConsulta = consulta.executeQuery("SELECT id FROM usuarios where "
                        + "idcreador=" + idCreador + " AND activo=1");
            }
            int temp;
            while (resultadoConsulta.next()) {
                temp = resultadoConsulta.getInt(1);
                salida.add(temp);
            }

        } catch (SQLException ex) {
            LogSR.mensaje("Error al consultar la lista de usuarios del administrador. " + ex.getMessage(), LogSR.ERROR);
        }

        return salida;
    }

    /**
     * Devuelve una cadena codificada para la clave
     *
     * @param claveSinCodificar
     * @return La clave encriptada en hexadecimal o null si hay un error.
     */
    public String encriptarClave(String claveSinCodificar) {
        String salida = null;
        try {

            MessageDigest md = MessageDigest.getInstance(MessageDigestAlgorithms.SHA_512);
            md.update(claveSinCodificar.getBytes());
            byte[] digest = md.digest();

            String temp = "";
            //Pasar los bytes codificados a hexadecimal
            for (byte b : digest) {
                temp += Integer.toHexString(0xFF & b);
            }
            salida = temp;
        } catch (NoSuchAlgorithmException ex) {
            LogSR.mensaje("Error al generar la clave. " + ex.getMessage(), LogSR.ERROR);
        }

        return salida;
    }

    /**
     * Comprueba si los datos de login de un usuario son correctos.
     *
     * @param email
     * @param clave Clave sin codificar
     * @return Si se acepta o no los datos.
     */
    public boolean validarUsuario(String email, String clave) {
        int id = buscarUsuario(email);
        //Si el usuario no existe
        if (id < 0) {
            return false;
        }

        //Si el usuario está desactivado
        if (!esUsuarioActivo(id)) {
            return false;
        }

        //Comprobar la clave si llega aquí
        String claveTempCodificada = encriptarClave(clave);

        try {
            //Comprueba el número de usuarios de la BD para asignarle un id.
            Statement consulta = conexion.createStatement();
            ResultSet resultadoConsulta = consulta.executeQuery("SELECT count(*) FROM usuarios where "
                    + "id=" + id);
            resultadoConsulta.next();
            int contador = resultadoConsulta.getInt(1);

            //Si existe el usuario
            if (contador > 0) {
                //Obtener la clave almacenada de ese usuario
                resultadoConsulta = consulta.executeQuery("SELECT clave FROM usuarios where "
                        + "id=" + id);
                resultadoConsulta.next();
                String claveBD = resultadoConsulta.getString(1);
                //Comparar las dos claves codificadas
                if (claveBD.equals(claveTempCodificada)) {
                    return true;
                }
            }
        } catch (SQLException ex) {
            LogSR.mensaje("Error al validar usuario. " + ex.getMessage(), LogSR.ERROR);
        }
        //Si llega aquí es que son distintas
        return false;
    }

    /**
     * Devuelve la lista de los emails de todos los usuarios con alertas
     * activadas.
     *
     * @return
     */
    public List<String> usuariosAlertasEmail() {
        List<String> salida = new ArrayList<>();

        try {
            //Comprueba el número de usuarios de la BD para asignarle un id.
            Statement consulta = conexion.createStatement();
            ResultSet resultadoConsulta;

            resultadoConsulta = consulta.executeQuery("SELECT email FROM usuarios WHERE alertaMail=1 AND activo=1");

            String temp;
            while (resultadoConsulta.next()) {
                temp = resultadoConsulta.getString(1);
                if (temp != null) {
                    if (!temp.isEmpty()) {
                        salida.add(temp);
                    }
                }

            }

        } catch (SQLException ex) {
            LogSR.mensaje("Error al consultar la lista de usuarios con alertas. " + ex.getMessage(), LogSR.ERROR);
        }

        return salida;
    }
    /*
     Administradores
     */

    /**
     * Crea un administrador a partir de un usuario
     *
     * @param id Id del usuario
     * @return Si se ha realizado correctamente
     */
    public boolean setAdministrador(int id) {
        boolean salida = false;

        try {
            //Comprueba el número de usuarios de la BD para asignarle un id.
            Statement consulta = conexion.createStatement();
            ResultSet resultadoConsulta = consulta.executeQuery("SELECT count(*) FROM usuarios where "
                    + "id=" + id);
            resultadoConsulta.next();
            int contador = resultadoConsulta.getInt(1);

            //Si existe el usuario
            if (contador > 0) {
                PreparedStatement st = conexion.prepareStatement("insert into administradores ("
                        + "id) values("
                        + "?)");

                st.setInt(1, id);

                //Insertar
                st.execute();
                st.close();
                conexion.commit();
                salida = true;
            }

        } catch (SQLException ex) {
            LogSR.mensaje("Error al nombrar el administrador. " + ex.getMessage(), LogSR.ERROR);
        }

        return salida;
    }

    /**
     * Borra un administrador
     *
     * @param id Id del usuario
     * @param idNuevoAdmin Nuevo administrador al que se le asignarán los
     * usuarios.
     * @return Si se ha realizado correctamente
     */
    public boolean borrarAdministrador(int id, int idNuevoAdmin) {
        boolean salida = false;
        try {
            //Asignar usuarios a otro admin
            if (!esAdministrador(idNuevoAdmin)) {
                return false;
            }
            if (id == 0) {
                return false;
            }
            PreparedStatement stActualizar = conexion.prepareStatement("update usuarios set "
                    + "idcreador=" + idNuevoAdmin + " where idcreador=" + id);
            stActualizar.executeUpdate();

            //Borrar la tupla antigua
            PreparedStatement stBorrar = conexion.prepareStatement("delete from administradores where id=" + id);
            stBorrar.executeUpdate();
            conexion.commit();
            salida = true;
        } catch (SQLException ex) {
            LogSR.mensaje("Error al borrar el administrador. " + ex.getMessage(), LogSR.ERROR);
        }

        return salida;
    }

    /**
     * Comprueba si un usuario es administrador
     *
     * @param id Id del usuario
     * @return Si es administrador
     */
    public boolean esAdministrador(int id) {
        boolean salida = false;

        try {
            //Comprueba el número de usuarios de la BD para asignarle un id.
            Statement consulta = conexion.createStatement();
            ResultSet resultadoConsulta = consulta.executeQuery("SELECT count(*) FROM administradores where "
                    + "id=" + id);
            resultadoConsulta.next();
            int contador = resultadoConsulta.getInt(1);

            //Si existe el usuario
            if (contador > 0) {
                salida = true;
            }

        } catch (SQLException ex) {
            LogSR.mensaje("Error al comprobar si es administrador. " + ex.getMessage(), LogSR.ERROR);
        }

        return salida;
    }

    /**
     * Comprueba si es superadministrador
     *
     * @param id
     * @return
     */
    public boolean esSuperAdministrador(int id) {
        return id == 0;
    }

    /*
     Lugares
     */
    /**
     * Crea un lugar en la base de datos. Punto de interés.
     *
     * @param tipo Si es un colegio, parque...
     * @param nombre Texto que se mostrará en la web.
     * @param latitud
     * @param longitud
     * @return Si se ha creado correctamente
     */
    public boolean crearLugar(byte tipo, String nombre, double latitud, double longitud) {
        boolean salida = false;
        try {
            //Comprueba el número de usuarios de la BD para asignarle un id.
            Statement consulta = conexion.createStatement();
            ResultSet resultado = consulta.executeQuery("SELECT count(*) FROM lugares "
                    + "where latitud=" + latitud + " and longitud=" + longitud);
            resultado.next();
            int contador = resultado.getInt(1);

            if (contador == 0) {
                //Si no hay nada en esa posición, insertar.
                PreparedStatement st = conexion.prepareStatement("insert into lugares ("
                        + "id, tipo, nombre, latitud, longitud) values("
                        + "?,?,?,?,?)");

                consulta = conexion.createStatement();
                resultado = consulta.executeQuery("SELECT count(*) FROM lugares ");
                resultado.next();
                contador = resultado.getInt(1);
                //Realizar el insert               

                st.setInt(1, contador);
                st.setByte(2, tipo);
                st.setString(3, nombre);
                st.setDouble(4, latitud);
                st.setDouble(5, longitud);

                st.execute();
                st.close();
                conexion.commit();
                salida = true;
            }
        } catch (SQLException ex) {
            LogSR.mensaje("Error al crear el lugar. " + ex.getMessage(), LogSR.ERROR);
        }

        return salida;
    }

    /**
     * Devuelve una lista de los lugares.
     *
     * @return Una lista de arrays (id, tipo, nombre, lat y lon)
     */
    public List<String[]> listarLugares() {
        List<String[]> salida = new ArrayList();

        try {
            //Comprueba el número de usuarios de la BD para asignarle un id.
            Statement consulta = conexion.createStatement();
            ResultSet resultado = consulta.executeQuery("SELECT id, tipo, nombre, latitud, longitud FROM lugares");

            while (resultado.next()) {
                String[] tempR = new String[5];
                tempR[0] = "" + resultado.getInt(1);
                tempR[1] = "" + resultado.getByte(2);
                tempR[2] = resultado.getString(3);
                tempR[3] = "" + resultado.getDouble(4);
                tempR[4] = "" + resultado.getDouble(5);
                salida.add(tempR);
            }
        } catch (SQLException ex) {
            LogSR.mensaje("Error al recuperar los lugares. " + ex.getMessage(), LogSR.ERROR);
        }

        return salida;
    }

    /*
     Iconos
     */
    /**
     * Crea un icono para el mapa.
     *
     * @param idCreador -1 si es público, -2 si se propone para público, o id
     * del propietario si privado
     * @param comentario
     * @param latitud
     * @param longitud
     * @param tipo
     * @return id del icono o -1 si error.
     */
    public int crearIcono(int idCreador, String comentario, double latitud, double longitud, int tipo) {
        int contador = -1;
        try {
            //Comprueba el número de usuarios de la BD para asignarle un id.
            Statement consulta = conexion.createStatement();
            ResultSet resultado = consulta.executeQuery("SELECT max(id) FROM iconos");
            resultado.next();
            contador = resultado.getInt(1);
            contador++;

            //Insertar
            PreparedStatement st = conexion.prepareStatement("insert into iconos ("
                    + "id, idcreador, comentario, latitud, longitud, tipo) values("
                    + "?,?,?,?,?,?)");
            st.setInt(1, contador);
            st.setInt(2, idCreador);
            st.setString(3, comentario);
            st.setDouble(4, latitud);
            st.setDouble(5, longitud);
            st.setInt(6, tipo);

            st.execute();
            st.close();
            conexion.commit();
        } catch (SQLException ex) {
            LogSR.mensaje("Error al crear el icono. " + ex.getMessage(), LogSR.ERROR);
        }

        return contador;
    }

    /**
     * Borra un icono.
     *
     * @param id
     * @return Si se ha ejecutado correctamente.
     */
    public boolean borrarIcono(int id) {
        boolean salida = false;
        try {
            //Borrar la tupla antigua
            PreparedStatement stBorrar = conexion.prepareStatement("delete from iconos where id=" + id);
            stBorrar.executeUpdate();
            conexion.commit();
            salida = true;
        } catch (SQLException ex) {
            LogSR.mensaje("Error al borrar el icono. " + ex.getMessage(), LogSR.ERROR);
        }

        return salida;
    }

    /**
     * Devuelve una lista de iconos
     *
     * @param idCreador Dato público o id del usuario
     * @return La lista de todos los iconos para ese usuario.
     */
    public List<String[]> listarIconos(int idCreador) {
        List<String[]> salida = new ArrayList();

        try {
            //Comprueba el número de usuarios de la BD para asignarle un id.
            Statement consulta = conexion.createStatement();
            ResultSet resultado = consulta.executeQuery("SELECT id, tipo, comentario, latitud, longitud FROM iconos "
                    + "WHERE idCreador=-1 OR idCreador=" + idCreador);

            while (resultado.next()) {
                String[] tempR = new String[5];
                tempR[0] = "" + resultado.getInt(1);
                tempR[1] = "" + resultado.getInt(2);
                tempR[2] = resultado.getString(3);
                tempR[3] = "" + resultado.getDouble(4);
                tempR[4] = "" + resultado.getDouble(5);
                salida.add(tempR);
            }
        } catch (SQLException ex) {
            LogSR.mensaje("Error al recuperar los iconos de usuario. " + ex.getMessage(), LogSR.ERROR);
        }

        return salida;
    }

    /**
     * Devuelve una lista de todos los iconos
     *
     * @return La lista de todos los iconos para ese usuario.
     */
    public List<String[]> listarIconos() {
        List<String[]> salida = new ArrayList();

        try {
            //Comprueba el número de usuarios de la BD para asignarle un id.
            Statement consulta = conexion.createStatement();
            ResultSet resultado = consulta.executeQuery("SELECT id, tipo, comentario, latitud, longitud, idcreador FROM iconos");

            while (resultado.next()) {
                String[] tempR = new String[6];
                tempR[0] = "" + resultado.getInt(1);
                tempR[1] = "" + resultado.getInt(2);
                tempR[2] = resultado.getString(3);
                tempR[3] = "" + resultado.getDouble(4);
                tempR[4] = "" + resultado.getDouble(5);
                tempR[5] = "" + resultado.getInt(6);
                salida.add(tempR);
            }
        } catch (SQLException ex) {
            LogSR.mensaje("Error al recuperar los iconos. " + ex.getMessage(), LogSR.ERROR);
        }

        return salida;
    }

    /*
     *Propiedades 
     */
    /**
     * Crea una propiedad.
     *
     * @param clave
     * @param valor
     * @return Si se ha realizado correctamente
     */
    public boolean crearPropiedad(String clave, String valor) {
        try {
            //Insertar
            PreparedStatement st = conexion.prepareStatement("insert into propiedades ("
                    + "clave, valor) values("
                    + "?,?)");
            st.setString(1, clave);
            st.setString(2, valor);

            st.execute();
            st.close();
            conexion.commit();
            return true;
        } catch (SQLException ex) {
            LogSR.mensaje("Error al crear la propiedad. " + ex.getMessage(), LogSR.ERROR);
        }
        return false;
    }

    /**
     * Lee una propiedad.
     *
     * @param clave
     * @return El valor o "" si no existe.
     */
    public String leerPropiedad(String clave) {
        String salida = "";

        try {
            Statement consulta = conexion.createStatement();
            ResultSet resultado = consulta.executeQuery("SELECT count(*) FROM propiedades WHERE clave='" + clave + "'");
            resultado.next();
            int contador = resultado.getInt(1);

            if (contador == 0) {
                return "";
            }

            //Saca el dato
            consulta = conexion.createStatement();
            resultado = consulta.executeQuery("SELECT valor FROM propiedades WHERE clave='" + clave + "'");
            resultado.next();

            return resultado.getString(1);
        } catch (SQLException ex) {
            LogSR.mensaje("Error al recuperar la propiedad. " + ex.getMessage(), LogSR.ERROR);
        }

        return salida;
    }

    /**
     * Borra una propiedad de la BD.
     *
     * @param clave
     * @return Si se ejecutó correctamente.
     */
    public boolean borrarPropiedad(String clave) {
        boolean salida = false;
        try {
            //Borrar la tupla antigua
            PreparedStatement stBorrar = conexion.prepareStatement("delete from propiedades "
                    + "WHERE clave='" + clave + "'");
            stBorrar.executeUpdate();
            conexion.commit();
            salida = true;
        } catch (SQLException ex) {
            LogSR.mensaje("Error al borrar la propiedad. " + ex.getMessage(), LogSR.ERROR);
        }

        return salida;
    }

    /*
     Seguridad
     */
    /**
     * Establece los valores de seguridad para una calle. O los actualiza si
     * existe. Se basa en el id o en la posición de los nodos.
     *
     * @param calle
     * @return
     */
    public boolean setSeguridad(Calle calle) {
        boolean salida = false;

        try {
            //Comprueba si la calle ya existe por id o por ubicación
            Statement consulta = conexion.createStatement();
            ResultSet resultado = consulta.executeQuery("SELECT count(*) FROM seguridad "
                    + "where "
                    + "id=" + calle.id + " "
                    + "OR (latitud1=" + calle.latitud1 + " AND longitud1=" + calle.longitud1 + " AND latitud2=" + calle.latitud2 + " AND longitud2=" + calle.latitud2 + ") "
                    + "OR (latitud2=" + calle.latitud1 + " AND longitud2=" + calle.longitud1 + " AND latitud1=" + calle.latitud2 + " AND longitud1=" + calle.latitud2 + ")");
            resultado.next();
            int contador = resultado.getInt(1);

            //Si ya existe la borra
            if (contador > 0) {
                //Borrar la tupla antigua
                PreparedStatement stBorrar = conexion.prepareStatement("delete from seguridad "
                        + "where "
                        + "id=" + calle.id + " "
                        + "OR (latitud1=" + calle.latitud1 + " AND longitud1=" + calle.longitud1 + " AND latitud2=" + calle.latitud2 + " AND longitud2=" + calle.latitud2 + ") "
                        + "OR (latitud2=" + calle.latitud1 + " AND longitud2=" + calle.longitud1 + " AND latitud1=" + calle.latitud2 + " AND longitud1=" + calle.latitud2 + ")");
                stBorrar.executeUpdate();
            }

            //Insertar
            PreparedStatement st = conexion.prepareStatement("insert into seguridad ("
                    + "id, --Id único en la bd. Aconsejable el de graphopper.\n"
                    + "fechaActualizacion,"
                    + "idcreador, --Id del que introdujo el dato\n"
                    + "privado, -- 0 si es público, 1 si es un dato privado del creador\n"
                    + "elevar, -- 0 si no se desea mandar a administrador, 1 si se desea compartir en público.\n"
                    + "latitud1, --Los dos nodos que componen el tramo\n"
                    + "longitud1,\n"
                    + "latitud2,\n"
                    + "longitud2,\n"
                    + "densidadtrafico,\n"
                    + "acera,\n"
                    + "anchocalle,\n"
                    + "indicecriminalidad,\n"
                    + "ratioaccidentes,\n"
                    + "superficieadoquines, --0 si falso, 1 si verdadero\n"
                    + "superficielisa, -- 0 si false, 1 si verdadero\n"
                    + "pasopeatones, \n"
                    + "semaforos,\n"
                    + "residencialcomercial, --Indica el movimiento de personas de la calle\n"
                    + "conservacionedificios,\n"
                    + "niveleconomico,\n"
                    + "iluminacion,\n"
                    + "velocidadmaxima,\n"
                    + "callepeatonal, -- 0 si no es peatonal, 1 si tiene preferencia peatonal\n"
                    + "carrilbici, -- 0 si no es carril bici, 1 si es o tiene carril bici\n"
                    + "calidadcarrilbici, \n"
                    + "separacioncalzadaacera, -- 0 falso, 1 verdadero\n"
                    + "aparcamientoacera, --idem\n"
                    + "badenes, --idem\n"
                    + "radar, --idem\n"
                    + "pendiente, \n"
                    + "confort, --Indica el balance entre diversos factores como el ruido, contaminación, zonas de ocio, de paseo...\n"
                    + "policía -- 0 falso, 1 verdadero\n"
                    + ") values ("
                    + "?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

            //Insertar datos en la consulta
            st.setInt(1, calle.id);
            st.setDate(2, new Date(new GregorianCalendar().getTimeInMillis()));
            st.setInt(3, calle.idcreador);
            st.setInt(4, calle.privado);
            st.setInt(5, calle.elevar);
            st.setDouble(6, calle.latitud1);
            st.setDouble(7, calle.longitud1);
            st.setDouble(8, calle.latitud2);
            st.setDouble(9, calle.longitud2);
            st.setInt(10, calle.densidadtrafico);
            st.setInt(11, calle.acera);
            st.setInt(12, calle.anchocalle);
            st.setInt(13, calle.indicecriminalidad);
            st.setInt(14, calle.ratioaccidentes);
            st.setInt(15, calle.superficieadoquines);
            st.setInt(16, calle.superficielisa);
            st.setInt(17, calle.pasopeatones);
            st.setInt(18, calle.semaforos);
            st.setInt(19, calle.residencialcomercial);
            st.setInt(20, calle.conservacionedificios);
            st.setInt(21, calle.niveleconomico);
            st.setInt(22, calle.iluminacion);
            st.setInt(23, calle.velocidadmaxima);
            st.setInt(24, calle.callepeatonal);
            st.setInt(25, calle.carrilbici);
            st.setInt(26, calle.calidadcarrilbici);
            st.setInt(27, calle.separacioncalzadaacera);
            st.setInt(28, calle.aparcamientoacera);
            st.setInt(29, calle.badenes);
            st.setInt(30, calle.radar);
            st.setInt(31, calle.pendiente);
            st.setInt(32, calle.confort);
            st.setInt(33, calle.policía);

            //Ejecutar consulta
            st.execute();
            st.close();
            conexion.commit();
            salida = true;
        } catch (SQLException ex) {
            LogSR.mensaje("Error al crear la calle. " + ex.getMessage(), LogSR.ERROR);
        }
        return salida;
    }

    /**
     * Borra los datos de una calle.
     *
     * @param id id de la calle a borrar o -1 para borrar todas
     * @return
     */
    public boolean borrarSeguridad(int id) {        
        boolean salida = false;
        try {
            //Borrar la tupla antigua
            PreparedStatement stBorrar;
            
            if(id<0){
                stBorrar = conexion.prepareStatement("delete from seguridad");
            }else{
                stBorrar = conexion.prepareStatement("delete from seguridad "
                    + "WHERE id=" + id);
            }
            
            stBorrar.executeUpdate();
            conexion.commit();
            salida = true;
        } catch (SQLException ex) {
            LogSR.mensaje("Error al borrar la calle. " + ex.getMessage(), LogSR.ERROR);
        }

        return salida;
    }

    /**
     * Devuelve la información de una calle.
     *
     * @param id
     * @return calle o null si no lo encuentra
     */
    public Calle getSeguridad(int id) {
        Calle salida = null;
        Calle calle = new Calle();

        try {
            //Comprueba el número de usuarios de la BD para asignarle un id.
            Statement consulta = conexion.createStatement();
            ResultSet resultado = consulta.executeQuery("SELECT count(*) FROM seguridad "
                    + "where id=" + id);
            resultado.next();
            int contador = resultado.getInt(1);

            //Si ya existe la borra
            if (contador > 0) {
                //Leer tupla
                consulta = conexion.createStatement();
                resultado = consulta.executeQuery("SELECT "
                        + "id, --Id único en la bd. Aconsejable el de graphopper.\n"
                        + "fechaActualizacion,"
                        + "idcreador, --Id del que introdujo el dato\n"
                        + "privado, -- 0 si es público, 1 si es un dato privado del creador\n"
                        + "elevar, -- 0 si no se desea mandar a administrador, 1 si se desea compartir en público.\n"
                        + "latitud1, --Los dos nodos que componen el tramo\n"
                        + "longitud1,\n"
                        + "latitud2,\n"
                        + "longitud2,\n"
                        + "densidadtrafico,\n"
                        + "acera,\n"
                        + "anchocalle,\n"
                        + "indicecriminalidad,\n"
                        + "ratioaccidentes,\n"
                        + "superficieadoquines, --0 si falso, 1 si verdadero\n"
                        + "superficielisa, -- 0 si false, 1 si verdadero\n"
                        + "pasopeatones, \n"
                        + "semaforos,\n"
                        + "residencialcomercial, --Indica el movimiento de personas de la calle\n"
                        + "conservacionedificios,\n"
                        + "niveleconomico,\n"
                        + "iluminacion,\n"
                        + "velocidadmaxima,\n"
                        + "callepeatonal, -- 0 si no es peatonal, 1 si tiene preferencia peatonal\n"
                        + "carrilbici, -- 0 si no es carril bici, 1 si es o tiene carril bici\n"
                        + "calidadcarrilbici, \n"
                        + "separacioncalzadaacera, -- 0 falso, 1 verdadero\n"
                        + "aparcamientoacera, --idem\n"
                        + "badenes, --idem\n"
                        + "radar, --idem\n"
                        + "pendiente, \n"
                        + "confort, --Indica el balance entre diversos factores como el ruido, contaminación, zonas de ocio, de paseo...\n"
                        + "policía -- 0 falso, 1 verdadero\n"
                        + " FROM seguridad "
                        + "where id=" + id);

                //Leer el resultado
                calle.id = resultado.getInt(1);
                calle.fechaActualizacion = resultado.getDate(2);
                calle.idcreador = resultado.getInt(3);
                calle.privado = resultado.getInt(4);
                calle.elevar = resultado.getInt(5);
                calle.latitud1 = resultado.getDouble(6);
                calle.longitud1 = resultado.getDouble(7);
                calle.latitud2 = resultado.getDouble(8);
                calle.longitud2 = resultado.getDouble(9);
                calle.densidadtrafico = resultado.getInt(10);
                calle.acera = resultado.getInt(11);
                calle.anchocalle = resultado.getInt(12);
                calle.indicecriminalidad = resultado.getInt(13);
                calle.ratioaccidentes = resultado.getInt(14);
                calle.superficieadoquines = resultado.getInt(15);
                calle.superficielisa = resultado.getInt(16);
                calle.pasopeatones = resultado.getInt(17);
                calle.semaforos = resultado.getInt(18);
                calle.residencialcomercial = resultado.getInt(19);
                calle.conservacionedificios = resultado.getInt(20);
                calle.niveleconomico = resultado.getInt(21);
                calle.iluminacion = resultado.getInt(22);
                calle.velocidadmaxima = resultado.getInt(23);
                calle.callepeatonal = resultado.getInt(24);
                calle.carrilbici = resultado.getInt(25);
                calle.calidadcarrilbici = resultado.getInt(26);
                calle.separacioncalzadaacera = resultado.getInt(27);
                calle.aparcamientoacera = resultado.getInt(28);
                calle.badenes = resultado.getInt(29);
                calle.radar = resultado.getInt(30);
                calle.pendiente = resultado.getInt(31);
                calle.confort = resultado.getInt(32);
                calle.policía = resultado.getInt(33);
                salida = calle;
            }

            //Ejecutar consulta
            resultado.close();
        } catch (SQLException ex) {
            LogSR.mensaje("Error al leer la calle. " + ex.getMessage(), LogSR.ERROR);
        }

        return salida;
    }

    /**
     * Devuelve los datos de todas las calles.
     *
     * @return
     */
    public List<Calle> getSeguridad() {
        List<Calle> salida = new ArrayList();

        try {
            //Comprueba el número de usuarios de la BD para asignarle un id.
            Statement consulta = conexion.createStatement();
            ResultSet resultado = consulta.executeQuery("SELECT count(*) FROM seguridad");
            resultado.next();
            int contador = resultado.getInt(1);

            //Si ya existe la borra
            if (contador > 0) {
                //Leer tupla
                consulta = conexion.createStatement();
                resultado = consulta.executeQuery("SELECT "
                        + "id"
                        + " FROM seguridad");

                while (resultado.next()) {
                    Calle temp = getSeguridad(resultado.getInt(1));
                    if (temp != null) {
                        salida.add(temp);
                    }
                }
            }

            //Ejecutar consulta
            resultado.close();
        } catch (SQLException ex) {
            LogSR.mensaje("Error al leer la calle. " + ex.getMessage(), LogSR.ERROR);
        }

        return salida;
    }

    /*
     Rutas
     */
    /**
     * Guarda la ruta en la BD
     *
     * @param kml
     * @param instrucciones
     * @return Id de la ruta en la Bd o -1 si error
     */
    public int guardarRuta(String kml, String instrucciones) {
        int salida = -1;
        try {
            //Comprueba el número de usuarios de la BD para asignarle un id.
            Statement consulta = conexion.createStatement();
            ResultSet resultado = consulta.executeQuery("SELECT count(*) FROM rutas");
            resultado.next();
            int contador = resultado.getInt(1);

            PreparedStatement st = conexion.prepareStatement("insert into rutas ("
                    + "id, fecha, kml, instrucciones) values("
                    + "?,?,?,?)");

            st.setInt(1, contador);
            st.setDate(2, new Date(new GregorianCalendar().getTimeInMillis()));
            st.setString(3, kml);
            st.setString(4, instrucciones);
            st.execute();
            st.close();
            conexion.commit();
            salida = contador;
        } catch (SQLException ex) {
            LogSR.mensaje("Error al guardar la ruta. " + ex.getMessage(), LogSR.ERROR);
        }

        return salida;
    }

    /**
     * Devuelve la información de una ruta
     *
     * @param id
     * @return Array con kml, instrucciones, y fecha en millis
     */
    public String[] recuperarRuta(int id) {
        String[] salida = new String[3];

        try {
            Statement consulta = conexion.createStatement();
            ResultSet resultado = consulta.executeQuery("SELECT count(*) FROM rutas WHERE id=" + id);
            resultado.next();
            int contador = resultado.getInt(1);

            if (contador == 0) {
                salida = null;
                return salida;
            }

            //Saca el dato
            consulta = conexion.createStatement();
            resultado = consulta.executeQuery("SELECT id, fecha, kml, instrucciones FROM rutas WHERE id=" + id);
            resultado.next();

            salida[0] = resultado.getString(3);
            salida[1] = resultado.getString(4);
            salida[2] = "" + resultado.getDate(2);
        } catch (SQLException ex) {
            LogSR.mensaje("Error al recuperar la ruta. " + ex.getMessage(), LogSR.ERROR);
        }

        return salida;
    }

    /*
     Alertas
     */
    /**
     * Crea una entrada en la base de datos para una alerta de texto.
     *
     * @param idCreador
     * @param titulo
     * @param texto
     * @return -1 en caso de error o el id de la alerta
     */
    public int crearAlerta(int idCreador, String titulo, String texto) {
        int salida = -1;
        try {
            //Comprueba el número de usuarios de la BD para asignarle un id.
            Statement consulta = conexion.createStatement();
            ResultSet resultado = consulta.executeQuery("SELECT max(id) FROM alertas");
            resultado.next();
            int contador = resultado.getInt(1);
            contador++;

            PreparedStatement st = conexion.prepareStatement("insert into alertas ("
                    + "id, idcreador, titulo, texto, fechacreacion, fechavalidez) values("
                    + "?,?,?,?,?,?)");

            st.setInt(1, contador);
            st.setInt(2, idCreador);
            st.setString(3, titulo);
            st.setString(4, texto);
            st.setDate(5, new Date(new GregorianCalendar().getTimeInMillis()));
            st.setDate(6, new Date(new GregorianCalendar().getTimeInMillis()));

            st.execute();
            st.close();
            conexion.commit();
            salida = contador;
        } catch (SQLException ex) {
            LogSR.mensaje("Error al crear la alerta. " + ex.getMessage(), LogSR.ERROR);
        }

        return salida;
    }

    /**
     * Devuelve una lista con las alertas textuales activas. id, idcreador,
     * titulo, texto, fechacreacion
     *
     * @return
     */
    public List<String[]> getAlertas() {
        List<String[]> salida = new ArrayList();

        try {
            Statement consulta = conexion.createStatement();
            ResultSet resultado = consulta.executeQuery("SELECT count(*) FROM alertas");
            resultado.next();
            int contador = resultado.getInt(1);

            if (contador == 0) {
                return salida;
            }

            //Saca el dato
            consulta = conexion.createStatement();
            resultado = consulta.executeQuery("SELECT id, idcreador, titulo, texto, fechacreacion FROM alertas order by 1 desc");

            contador = 0;
            while (resultado.next() && contador < 5) {
                String temp[] = new String[5];

                temp[0] = "" + resultado.getInt(1);
                temp[1] = "" + resultado.getInt(2);
                temp[2] = "" + resultado.getString(3);
                temp[3] = "" + resultado.getString(4);
                temp[4] = "" + resultado.getDate(5).getTime();
                salida.add(temp);
                contador++;
            }
        } catch (SQLException ex) {
            LogSR.mensaje("Error al recuperar la lista de alertas textuales. " + ex.getMessage(), LogSR.ERROR);
        }

        return salida;
    }

    /**
     * Borra una noticia.
     *
     * @param id
     * @return
     */
    public boolean borrarAlerta(int id) {
        boolean salida = false;
        try {
            //Borrar la tupla antigua
            PreparedStatement stBorrar = conexion.prepareStatement("delete from alertas where id=" + id);
            stBorrar.executeUpdate();
            conexion.commit();
            salida = true;
        } catch (SQLException ex) {
            LogSR.mensaje("Error al borrar la alerta de texto. " + ex.getMessage(), LogSR.ERROR);
        }

        return salida;
    }

    /*
     Foro
     */
    /**
     * Devuelve una lista con los mensajes del foro con la forma id, idpadre,
     * idautor, fecha, titulo, mensaje
     *
     * @return
     */
    public List<String[]> getForoMensajes() {
        List<String[]> salida = new ArrayList();

        try {
            Statement consulta = conexion.createStatement();
            ResultSet resultado = consulta.executeQuery("SELECT count(*) FROM foro");
            resultado.next();
            int contador = resultado.getInt(1);

            if (contador == 0) {
                return salida;
            }

            //Saca el dato
            consulta = conexion.createStatement();
            resultado = consulta.executeQuery("SELECT id, idpadre, idautor, fecha, titulo, mensaje FROM foro WHERE idpadre=-1 order by 1 desc");

            while (resultado.next()) {
                String temp[] = new String[6];

                temp[0] = "" + resultado.getInt(1);
                temp[1] = "" + resultado.getInt(2);
                temp[2] = "" + resultado.getInt(3);
                temp[3] = "" + resultado.getDate(4).getTime();
                temp[4] = "" + resultado.getString(5);
                temp[5] = "" + resultado.getString(6);

                salida.add(temp);
            }
        } catch (SQLException ex) {
            LogSR.mensaje("Error al recuperar la lista de mensajes del foro. " + ex.getMessage(), LogSR.ERROR);
        }

        return salida;
    }

    /**
     * Devuelve una lista con los mensajes del foro para un hilo id, idpadre,
     * idautor, fecha, titulo, mensaje
     *
     * @return
     */
    public List<String[]> getForoMensaje(int idPadre) {
        List<String[]> salida = new ArrayList();

        try {
            Statement consulta = conexion.createStatement();
            ResultSet resultado = consulta.executeQuery("SELECT count(*) FROM foro where id=" + idPadre);
            resultado.next();
            int contador = resultado.getInt(1);

            if (contador == 0) {
                return salida;
            }

            //Coger la cabecera
            consulta = conexion.createStatement();
            resultado = consulta.executeQuery("SELECT id, idpadre, idautor, fecha, titulo, mensaje FROM foro WHERE id=" + idPadre);
            String temp[] = new String[6];

            temp[0] = "" + resultado.getInt(1);
            temp[1] = "" + resultado.getInt(2);
            temp[2] = "" + resultado.getInt(3);
            temp[3] = "" + resultado.getDate(4).getTime();
            temp[4] = "" + resultado.getString(5);
            temp[5] = "" + resultado.getString(6);

            salida.add(temp);

            //Saca el dato
            consulta = conexion.createStatement();
            resultado = consulta.executeQuery("SELECT id, idpadre, idautor, fecha, titulo, mensaje FROM foro WHERE idpadre=" + idPadre + " order by 4 ");
            
            while (resultado.next()) {temp = new String[6];
                temp[0] = "" + resultado.getInt(1);
                temp[1] = "" + resultado.getInt(2);
                temp[2] = "" + resultado.getInt(3);
                temp[3] = "" + resultado.getDate(4).getTime();
                temp[4] = "" + resultado.getString(5);
                temp[5] = "" + resultado.getString(6);

                salida.add(temp);
            }
        } catch (SQLException ex) {
            LogSR.mensaje("Error al recuperar la lista de mensajes del hilo del foro. " + ex.getMessage(), LogSR.ERROR);
        }

        return salida;
    }

    /**
     * Crea un mensaje en el foro.
     *
     * @param idpadre -1 para mensajes principales o el id del mensaje padre
     * @param idautor id del autor del mensaje
     * @param titulo Título del mensaje
     * @param mensaje Texto del mensaje
     * @return
     */
    public int crearForoMensaje(int idpadre, int idautor, String titulo, String mensaje) {
        int salida = -1;
        try {
            //Comprueba el número de usuarios de la BD para asignarle un id.
            Statement consulta = conexion.createStatement();
            ResultSet resultado = consulta.executeQuery("SELECT max(id) FROM foro");
            resultado.next();
            int contador = resultado.getInt(1);
            contador++;

            PreparedStatement st = conexion.prepareStatement("insert into foro ("
                    + "id, idpadre, idautor, fecha, titulo, mensaje) values("
                    + "?,?,?,?,?,?)");

            st.setInt(1, contador);
            st.setInt(2, idpadre);
            st.setInt(3, idautor);
            st.setDate(4, new Date(new GregorianCalendar().getTimeInMillis()));
            st.setString(5, titulo);
            st.setString(6, mensaje);

            st.execute();
            st.close();
            conexion.commit();
            salida = contador;
        } catch (SQLException ex) {
            LogSR.mensaje("Error al crear el mensaje en el foro. " + ex.getMessage(), LogSR.ERROR);
        }

        return salida;
    }

    /**
     * Borra un mensaje del foro y sus hijos si los tuviera.
     *
     * @param id
     * @return
     */
    public boolean borrarForoMensaje(int id) {
        boolean salida = false;
        try {
            //Borrar la tupla antigua
            PreparedStatement stBorrar = conexion.prepareStatement("delete from foro where id=" + id);
            stBorrar.executeUpdate();

            stBorrar = conexion.prepareStatement("delete from foro where idpadre=" + id);
            stBorrar.executeUpdate();

            conexion.commit();
            salida = true;
        } catch (SQLException ex) {
            LogSR.mensaje("Error al borrar el mensaje en el foro. " + ex.getMessage(), LogSR.ERROR);
        }

        return salida;
    }
}
