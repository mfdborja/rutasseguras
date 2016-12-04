package main;

import com.graphhopper.routing.Path;
import com.graphhopper.util.shapes.BBox;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import saferoute.base.Mapa;
import saferoute.bd.Propiedades;
import saferoute.comunicacion.ServidorEmailUgr;
import saferoute.log.LogSR;
import saferoute.struct.Calle;
import saferoute.struct.Instruccion;
import saferoute.struct.Pareja;
import saferoute.struct.Persona;

public class Manejador extends Thread {

    private final Socket socketConCliente;
    private final Mapa mapa;

    /**
     * Crea una instancia para atender una petición del servidor.
     *
     * @param socket Socket para comunicarse con el cliente.
     * @param mapa Mapa con la información.
     */
    public Manejador(Socket socket, Mapa mapa) {
        this.socketConCliente = socket;
        this.mapa = mapa;
    }

    /**
     * Escucha la petición y la sirve.
     */
    @Override
    public void run() {
        //Variable para escribir la respuesta a la petición.
        String respuesta;
        try {
            //Escuchar la petición del cliente
            BufferedReader entradaStream = new BufferedReader(new InputStreamReader(socketConCliente.getInputStream()));
            String peticion = entradaStream.readLine();

            //Respuesta petición
            BufferedWriter salidaStream = new BufferedWriter(new OutputStreamWriter(socketConCliente.getOutputStream()));

            //Tratar la petición - La primera palabra del protocolo define la petición            
            if (peticion.startsWith("generarruta")) {
                //Generar ruta
                respuesta = generarRuta(peticion);
                salidaStream.write(respuesta);
            } else if (peticion.startsWith("login")) {
                //Comprobar usuario
                respuesta = login(peticion);
                salidaStream.write(respuesta);
            } else if (peticion.startsWith("infousuario")) {
                //Dar la información de un usuario
                ObjectOutputStream obs = new ObjectOutputStream(socketConCliente.getOutputStream());
                obs.writeObject(infoUsuario(peticion));
                obs.writeUTF("\n");
                obs.flush();
            } else if (peticion.startsWith("actualizarusuario")) {
                //Actualizar la información de un usuario
                respuesta = actualizarUsuario(peticion);
                salidaStream.write(respuesta);
            } else if (peticion.startsWith("anadirinfocalles")) {
                //Incluir o actualizar información de un rectángulo en el mapa con calles
                respuesta = anadirInfoCallesArea(peticion);
                salidaStream.write(respuesta);
            } else if (peticion.startsWith("enviaremailmultiple")) {
                //Enviar un email a múltiples personas
                String mensaje = "";
                String tmp;
                while ((tmp = entradaStream.readLine()) != null) {
                    mensaje += tmp + "\n";
                }
                salidaStream.write(enviarEmailMultiple(peticion, mensaje));
            } else if (peticion.startsWith("nuevousuario")) {
                //Crear un nuevo usuario
                respuesta = nuevoUsuario(peticion);
                salidaStream.write(respuesta);
            } else if (peticion.startsWith("borrarusuario")) {
                //Borrar usuario
                respuesta = borrarUsuario(peticion);
                salidaStream.write(respuesta);
            } else if (peticion.startsWith("listausuarios")) {
                //Dar una lista con los usuarios
                respuesta = listarUsuarios(peticion);
                salidaStream.write(respuesta);
            } else if (peticion.startsWith("nombraradmin")) {
                //Crear un nuevo administrador
                respuesta = nombrarAdmin(peticion);
                salidaStream.write(respuesta);
            } else if (peticion.startsWith("infopunto")) {
                //Dar información de un lugar concreto
                respuesta = infoPunto(peticion);
                salidaStream.write(respuesta);
            } else if (peticion.startsWith("anadirinfopunto")) {
                //Modificar la información de un punto concreto
                respuesta = anadirInfoPunto(peticion);
                salidaStream.write(respuesta);
            } else if (peticion.startsWith("crearalerta")) {
                //Crear una alerta
                String mensaje = "";
                String tmp;
                while ((tmp = entradaStream.readLine()) != null) {
                    mensaje += (tmp + "\n");
                }
                respuesta = anadirIcono(peticion, mensaje);
                salidaStream.write(respuesta);
            } else if (peticion.startsWith("frontera")) {
                //Límites del mapa
                respuesta = getFrontera(peticion);
                salidaStream.write(respuesta);
            } else if (peticion.startsWith("iconos")) {
                //Da la lista de iconos
                respuesta = getIconos(peticion);
                salidaStream.write(respuesta);
            } else if (peticion.startsWith("ponernoticia")) {
                //Crea una noticia
                String mensaje = "";
                String tmp;
                while ((tmp = entradaStream.readLine()) != null) {
                    mensaje += tmp + "\n";
                }
                respuesta = putNoticia(peticion, mensaje);
                salidaStream.write(respuesta);
            } else if (peticion.startsWith("noticias")) {
                //Lista de noticias
                respuesta = getNoticias(peticion);
                salidaStream.write(respuesta);
            } else if (peticion.startsWith("borrarnoticia")) {
                //Borra una noticia
                respuesta = borrarNoticia(peticion);
                salidaStream.write(respuesta);
            } else if (peticion.startsWith("recuperarclave")) {
                //Genera una nueva clave y la envía por email al usuario
                respuesta = recuperarClave(peticion);
                salidaStream.write(respuesta);
            } else if (peticion.startsWith("contactoadmin")) {
                //Enviar un email al administrador de la plataforma
                String mensaje = "";
                String tmp;
                while ((tmp = entradaStream.readLine()) != null) {
                    mensaje += tmp + "\n";
                }
                respuesta = contactoAdmin(peticion, mensaje);
                salidaStream.write(respuesta);
            } else if (peticion.startsWith("listarmensajes")) {
                //Lista de mensajes generales del foro
                respuesta = listarMensajes(peticion);
                salidaStream.write(respuesta);
            } else if (peticion.startsWith("listarhilo")) {
                //Lista de mensajes de un hilo del foro
                respuesta = listarHilo(peticion);
                salidaStream.write(respuesta);
            } else if (peticion.startsWith("borrarmensaje")) {
                //Borra un mensaje del foro o un hilo
                respuesta = borrarMensaje(peticion);
                salidaStream.write(respuesta);
            } else if (peticion.startsWith("ponermensaje")) {
                //Crea un mensaje o un hilo para el foro
                String mensaje = "";
                String tmp;
                while ((tmp = entradaStream.readLine()) != null) {
                    mensaje += tmp + "\n";
                }
                respuesta = publicarMensaje(peticion, mensaje);
                salidaStream.write(respuesta);
            } else if (peticion.startsWith("borraricono")) {
                //Borra una alerta sobre el mapa
                respuesta = borrarIcono(peticion);
                salidaStream.write(respuesta);
            }

            //Fuerza la salida por el flujo y lo cierra
            salidaStream.flush();
            socketConCliente.close();
        } catch (Exception ex) {
            LogSR.mensaje("Error al escuchar la petición: "
                    + ex.getMessage(), LogSR.ERROR);
        }
    }

    /**
     * Genera la ruta y devuelve la respuesta con todo lo necesario
     *
     * @param peticion
     * @return Cadena Id de la ruta # distancia en metros # instrucciones como
     * tabla html # nodos para google js # puntos lat1,lon1;lat2,lon2;...
     */
    private String generarRuta(String peticion) {
        int usuario;
        double indSeguridad;
        byte tipo;

        //Obtener datos usuario#indSeguridad#pie o bici#lat1;lon1@...   
        peticion = peticion.substring(12); //Elimina generarruta
        String[] peticionTrozeada = peticion.split("#");

        usuario = Integer.parseInt(peticionTrozeada[0]);
        indSeguridad = Double.parseDouble(peticionTrozeada[1]);
        tipo = peticionTrozeada[2].equalsIgnoreCase("bici") ? Mapa.RUTA_A_BICI : Mapa.RUTA_A_PIE;

        //lat1;lon1 ...
        String[] listadoCoordenadas = peticionTrozeada[3].split("@");
        Pareja<Double, Double>[] listCoordenadas = new Pareja[listadoCoordenadas.length];
        for (int i = 0; i < listadoCoordenadas.length; i++) {
            String temp = listadoCoordenadas[i];
            String[] coordenada = temp.split(";");

            //Añadir en el listado de doubles
            listCoordenadas[i] = new Pareja(Double.parseDouble(coordenada[0]), Double.parseDouble(coordenada[1]));
        }

        //Ya tengo todos los datos
        Pareja<Double, Double> origen = listCoordenadas[0];
        Pareja<Double, Double> destino = listCoordenadas[1];

        //Cálculo de la ruta
        Path path = mapa.calcularRuta(origen.getPrimero(), origen.getSegundo(), destino.getPrimero(),
                destino.getSegundo(), tipo, indSeguridad, usuario);

        //Obtener instrucciones
        List<Instruccion> listInstrucciones = mapa.getInstruccionesBasico(path);
        String instrucciones = "";
        //Definicion tabla html
        instrucciones += "<tr>"
                + "<td>Instrucción</td>"
                + "<td>Distancia</td>"
                + "<td>Calle</td>"
                + "</tr>";
        for (Instruccion inst : listInstrucciones) {
            //Html - Lo que va entre <table> y </table>
            instrucciones += "<tr>"
                    + "<td>" + inst.getGiro() + "</td>"
                    + "<td>" + inst.getDistancia() + " m.</td>"
                    + "<td>" + inst.getNombre() + "</td>"
                    + "</tr>";
        }

        //Obtener kml
        String codRutaGJs = mapa.getCoordenadasGoogle(path);

        //Obtener distancia
        String distancia = "" + mapa.getDistanciaRuta(path);

        //Obtener puntos para calcular la extensión
        String puntos = mapa.getCoordenadasTexto(path);
        System.out.println(puntos);

        //Guardar ruta en la BD
        //Obtener id
        int idRuta = mapa.guardarRutaBD(puntos, instrucciones);

        //Estadística
        LogSR.mensaje("RUTA" + "\tidusuario:" + usuario + "\tidruta:" + idRuta + "\tdistancia:" + distancia + "\tpuntos:" + puntos, LogSR.ESTADISTICAS);

        //Salida - Id de la ruta # distancia en metros # instrucciones como tabla html # nodos para google js # puntos lat1,lon1;lat2,lon2;...
        return "" + idRuta + "#" + distancia + "#" + instrucciones + "#" + codRutaGJs.replaceAll("\n", "") + "#" + puntos + "\n";
    }

    /**
     * Comprueba si un usuario puede acceder con esa clave.
     *
     * @param peticion
     * @return -1 si no es posible, id del usuario si es posible. # tipo de
     * usuario
     */
    private String login(String peticion) {
        String salida = "";
        String[] trozos = peticion.split(" ");
        if (trozos.length >= 3) {
            String usuario = trozos[1];
            String clave = trozos[2];
            salida += mapa.hacerLogin(usuario, clave);
        }
        String tipo = "usuario";
        if (mapa.esAdministrador(Integer.parseInt(salida))) {
            tipo = "administrador";
        }

        //Estadística
        LogSR.mensaje("LOGIN" + "\tidusuario:" + salida, LogSR.ESTADISTICAS);

        return salida + "#" + tipo + "\n";
    }

    /**
     * Devuelve la información almacenada de una persona.
     *
     * @param peticion
     * @return La persona o null
     */
    private Persona infoUsuario(String peticion) {
        String[] trozos = peticion.split(" ");
        int usuario;
        try {
            usuario = Integer.parseInt(trozos[1]);
        } catch (NumberFormatException ex) {
            usuario = -1;
        }
        return mapa.getInfoUsuario(usuario);
    }

    /**
     * Actualiza la información de una persona si existe.
     *
     * @param peticion
     * @return El id o -1 si falla.
     */
    private String actualizarUsuario(String peticion) {
        String[] trozos = peticion.split("#");
        Persona p;
        //Id
        p = mapa.getInfoUsuario(Integer.parseInt(trozos[1]));
        if (p != null) {
            //Nombre de usuario
            p.nombreUsuario = trozos[2];
            //Email
            p.email = trozos[3];
            //Nombre real
            p.nombreReal = trozos[4];
            //Sexo tutor
            p.sexoTutor = Byte.parseByte(trozos[5]);
            //Nombre menor
            p.nombreNino = trozos[6];
            //Sexo menor
            p.sexoNino = Byte.parseByte(trozos[7]);
            //Direccion
            p.direccion = trozos[8];
            //Localidad
            p.localidad = trozos[9];
            //Provincia
            p.provincia = trozos[10];
            //CP
            p.codigoPostal = Integer.parseInt(trozos[11]);
            //Paretesco
            p.parentesco = Integer.parseInt(trozos[12]);
            //Fnacpadre
            if (!trozos[13].isEmpty()) {
                p.fnacPadre = new java.sql.Date(Long.parseLong(trozos[13]));
            }
            //FnacHijo
            if (!trozos[14].isEmpty()) {
                p.fnacNino = new java.sql.Date(Long.parseLong(trozos[14]));
            }
            //Nacionalidad padre
            p.naciolidadPadre = trozos[15];
            //N hijo
            p.nacionalidadHijo = trozos[16];
            //Profesion
            p.profesion = trozos[17];
            //Alertas mail
            p.alertaMail = Integer.parseInt(trozos[18]);
            int salida;
            //Clave si llega - Primero guarda la persona
            if ((salida = mapa.modificarPersona(p)) >= 0) {
                if (trozos.length >= 20) {
                    String clave = trozos[19];
                    p.clave = clave;
                    salida = mapa.modificarPersona(p);
                }
            }

            //Estadística
            LogSR.mensaje("ACTUALIZAUSUARIO" + "\tidusuario:" + p.idUsuario, LogSR.ESTADISTICAS);

            return "" + salida;
        }
        return "-1";

    }

    /**
     * Actualiza la información de las calles en un área dada.
     *
     * @param peticion
     * @return
     */
    private String anadirInfoCallesArea(String peticion) {
        String[] trozos = peticion.split("#");
        //Id del usuario
        int idCreador = Integer.parseInt(trozos[1]);
        //Si es un dato privado
        int privado = Integer.parseInt(trozos[2]);
        //Si es privado, si se propone para público
        int elevar = Integer.parseInt(trozos[3]);
        //Ubicación
        double sup = Double.parseDouble(trozos[4]);
        double inf = Double.parseDouble(trozos[5]);
        double izq = Double.parseDouble(trozos[6]);
        double der = Double.parseDouble(trozos[7]);
        //Variables de seguridad
        String valores = trozos[8];

        LogSR.mensaje("ACTUALIZVARIABLESZONA" + "\tsup:" + sup + "\tinf:" + inf + "\tizq:" + izq + "\tder:" + der + "\tvariables:" + valores + "\tidusuario:" + idCreador, LogSR.ESTADISTICAS);
        return mapa.anadirInfoCalles(sup, inf, izq, der, valores, idCreador, privado, elevar);
    }

    /**
     * Envía un email a los usuarios con alertas por mail activadas de una
     * administrador. O a todos con alertas si es el superadministrador.
     *
     * @param peticion
     * @param mensaje Mensaje que se desea enviar
     * @return
     */
    private String enviarEmailMultiple(String peticion, String mensaje) {
        String[] trozos = peticion.split("#");
        int idAdmin = Integer.parseInt(trozos[1]);
        String asunto = trozos[2];

        List<String> emails = mapa.enviarEmailUsuariosAdmin(idAdmin);

        //Enviar email
        ServidorEmailUgr servidorEmail = new ServidorEmailUgr();
        servidorEmail.enviarEmail(emails, asunto, mensaje);

        return "";
    }

    /**
     * Crea un usuario y le envía un mail de bienvenida.
     *
     * @param peticion
     * @return -1 en caso de error o el nuevo id de usuario
     */
    private String nuevoUsuario(String peticion) {
        String salida = "-1";

        String[] trozos = peticion.split("#");

        String email = trozos[1];
        String clave = trozos[2];
        int idCreador = Integer.parseInt(trozos[3]);

        if (!mapa.esAdministrador(idCreador)) {
            return salida;
        }

        if (mapa.buscarUsuario(email) >= 0) {
            return salida;
        }

        //Si no se pasa la clave
        if (clave == null) {
            clave = mapa.generarClaveAleatoria(8);
        } else if (clave.equalsIgnoreCase("null")) {
            clave = mapa.generarClaveAleatoria(8);
        }

        salida = "" + mapa.anadirUsuario(email, clave, idCreador);

        //Enviar mail al nuevo usuario
        try {
            ServidorEmailUgr servidorEmail = new ServidorEmailUgr();
            //Cargar cabecera
            String textoBienvenida = servidorEmail.leerFicheroTexto(Propiedades.getPropiedades().leerPropiedad(Propiedades.mensajenuevousuario));
            //CargarPie
            String textoPie = servidorEmail.leerFicheroTexto(Propiedades.getPropiedades().leerPropiedad(Propiedades.mensajepie));

            //Enviar email            
            String mensaje = textoBienvenida
                    + "<p>Usuario: <strong>" + email + "</strong></p>\n"
                    + "<p>Clave: <strong>" + clave + "</strong></p>\n"
                    + textoPie;
            if (!salida.equals("-1")) {
                servidorEmail.enviarEmail(email, "Alta en el servicio de rutas seguras", mensaje);
            }
        } catch (Exception ex) {
            LogSR.mensaje("Error al enviar email de nuevo usuario. " + ex.getMessage(), LogSR.ERROR);
        }
        return salida;
    }

    /**
     * Borra un usuario dado.
     *
     * @param peticion
     * @return
     */
    private String borrarUsuario(String peticion) {
        String salida;

        String[] trozos = peticion.split("#");
        int idBorrado = Integer.parseInt(trozos[1]);
        int idBorrador = Integer.parseInt(trozos[2]);

        //Comprobar que no intentan borrar al superadministrador
        if (idBorrado == 0) {
            return "-1";
        }

        //Comprobar que sea un administrador
        if (!mapa.esAdministrador(idBorrador)) {
            return "-1";
        }

        //Que el borrado no sea un administrador
        if (mapa.esAdministrador(idBorrado)) {
            return "-1";
        }

        //Si es un administrador borrando un usuario normal
        //Se borra
        salida = "" + mapa.borrarUsuario(idBorrado);

        return salida;
    }

    /**
     * Devuelve la lista de usuarios de un administrador o todos si es el
     * superadministrador.
     *
     * @param peticion
     * @return
     */
    private String listarUsuarios(String peticion) {
        String salida = "";

        //Obtener id admin
        String[] trozos = peticion.split("#");
        int id = Integer.parseInt(trozos[1]);

        //Obtener usuarios de ese admin
        List<Integer> listaId = mapa.listarUsuariosDeAdmin(id);

        for (int idUsuario : listaId) {
            Persona p = mapa.getInfoUsuario(idUsuario);
            if (mapa.esActivoUsuario(p.idUsuario)) {
                boolean esAdmin = mapa.esAdministrador(p.idUsuario);
                String cadAdmin = esAdmin ? "Administrador" : "Usuario";
                salida += p.idUsuario + "#" + p.email + "#" + p.nombreReal + "#"
                        + p.nombreUsuario + "#" + cadAdmin + "\n";
            }
        }
        return salida;
    }

    /**
     * Hace administrador a un usuario existente y le informa por email.
     *
     * @param peticion
     * @return
     */
    private String nombrarAdmin(String peticion) {
        String salida = "-1";

        //Obtener id admin
        String[] trozos = peticion.split("#");
        int id = Integer.parseInt(trozos[1]);

        //Comprobar que es valido
        if (mapa.esActivoUsuario(id)) {
            //Nombrar admin si no lo era
            if (!mapa.esAdministrador(id)) {
                salida = mapa.setAdministrador(id);
                String email = mapa.getInfoUsuario(id).email;

                try {
                    ServidorEmailUgr servidorEmail = new ServidorEmailUgr();
                    //Cargar cabecera
                    String textoNuevoAdmin = servidorEmail.leerFicheroTexto(Propiedades.getPropiedades().leerPropiedad(Propiedades.mensajenuevoadministrador));
                    //CargarPie
                    String textoPie = servidorEmail.leerFicheroTexto(Propiedades.getPropiedades().leerPropiedad(Propiedades.mensajepie));

                    String mensaje = textoNuevoAdmin
                            + "<p>Usuario: " + email + "</p>\n"
                            + textoPie;
                    if (!salida.equals("-1")) {
                        servidorEmail.enviarEmail(email, "Es administrador de rutas seguras", mensaje);
                    }
                } catch (Exception ex) {
                    LogSR.mensaje("Error al enviar email de nuevo administrador." + ex.getMessage(), LogSR.ERROR);
                }
            }
        }
        return salida;
    }

    /**
     * Devuelve el interior de un formulario con la información de la calle en
     * html.
     *
     * @param peticion
     * @return
     */
    private String infoPunto(String peticion) {
        String salida = "";
        //Trocear
        String[] trozos = peticion.split("#");
        double latitud = Double.parseDouble(trozos[1]);
        double longitud = Double.parseDouble(trozos[2]);

        //Obtener info de la calle
        //Calle más cercana
        int id = mapa.getArcoCercano(latitud, longitud);

        //Si ya existen valores se cargan
        Calle calle = mapa.getSeguridadValoresArco(id);

        if (calle == null) {
            //Si no existen se crean por defecto
            calle = new Calle();
        }

        //Datos públicos para la calle
        double indpeaton = mapa.getSeguridadArco(id, -1, Mapa.RUTA_A_PIE);
        double indbici = mapa.getSeguridadArco(id, -1, Mapa.RUTA_A_BICI);

        //Formatear decimales
        NumberFormat formatoNumero = NumberFormat.getNumberInstance(new Locale("es", "ES"));
        formatoNumero.setMaximumFractionDigits(2);

        //Información general
        salida += "<p>Tramo nº: " + id + "<br /> Ind. Seguridad Peatón: <strong>" + formatoNumero.format((1 - indpeaton) * 10) + "/10</strong> <br />Ind. Seguridad bici: <strong>" + formatoNumero.format((1 - indbici) * 10) + "/10</strong></p></br>";

        //Montar el formulario con la información de la calle
        String velocidad = "" + calle.velocidadmaxima;
        salida += "Velocidad máxima:\n"
                + "             <FIELDSET>\n"
                + "             <INPUT type=\"number\" name=\"velocidad\" min=\"0\" max=\"130\" step=\"10\" value=\"" + velocidad + "\" /> km/h\n"
                + "             </FIELDSET>\n"
                + "             <br />\n"
                + " Densidad de tráfico:    \n"
                + "    <FIELDSET>\n"
                + "        <label for=\"f011\"><input id=\"f011\" type=\"radio\" name=\"densidadtrafico\" value=\"10\" " + (calle.densidadtrafico == 10 ? "checked" : "") + "> Muy Baja</label>\n"
                + "        <label for=\"f012\"><input id=\"f012\" type=\"radio\" name=\"densidadtrafico\" value=\"7\" " + (calle.densidadtrafico == 7 ? "checked" : "") + "> Baja</label>\n"
                + "        <label for=\"f013\"><input id=\"f013\" type=\"radio\" name=\"densidadtrafico\" value=\"5\" " + (calle.densidadtrafico == 5 ? "checked" : "") + "> Media</label>\n"
                + "        <label for=\"f014\"><input id=\"f014\" type=\"radio\" name=\"densidadtrafico\" value=\"3\" " + (calle.densidadtrafico == 3 ? "checked" : "") + "> Alta</label>\n"
                + "        <label for=\"f015\"><input id=\"f015\" type=\"radio\" name=\"densidadtrafico\" value=\"0\" " + (calle.densidadtrafico == 0 ? "checked" : "") + "> Muy Alta</label>\n"
                + "    </FIELDSET>"
                + "             <br />\n"
                + "             Valoración de las aceras:\n"
                + "             <FIELDSET>\n"
                + "             <label for=\"f021\"><input type=\"radio\" id='f021' name=\"aceras\" value=\"0\" " + (calle.acera == 0 ? "checked" : "") + "> Ausente</label>\n"
                + "             <label for=\"f022\"><input type=\"radio\" id='f022' name=\"aceras\" value=\"3\" " + (calle.acera == 3 ? "checked" : "") + "> Estrecha</label>\n"
                + "             <label for=\"f023\"><input type=\"radio\" id='f023' name=\"aceras\" value=\"5\" " + (calle.acera == 5 ? "checked" : "") + "> Normal</label>\n"
                + "             <label for=\"f024\"><input type=\"radio\" id='f024' name=\"aceras\" value=\"7\" " + (calle.acera == 7 ? "checked" : "") + "> Ancha</label>\n"
                + "             <label for=\"f025\"><input type=\"radio\" id='f025' name=\"aceras\" value=\"10\" " + (calle.acera == 10 ? "checked" : "") + "> Muy ancha</label>\n"
                + "             </FIELDSET>\n"
                + "             <br />\n"
                + "             Ancho de la calle:\n"
                + "             <FIELDSET>\n"
                + "             <label for=\"f031\"><input type=\"radio\" id='f031' name=\"ancho\" value=\"0\" " + (calle.anchocalle == 0 ? "checked" : "") + "> Muy estrecha</label>\n"
                + "             <label for=\"f032\"><input type=\"radio\" id='f032' name=\"ancho\" value=\"3\" " + (calle.anchocalle == 3 ? "checked" : "") + "> Estrecha</label>\n"
                + "             <label for=\"f033\"><input type=\"radio\" id='f033' name=\"ancho\" value=\"5\" " + (calle.anchocalle == 5 ? "checked" : "") + "> Normal</label>\n"
                + "             <label for=\"f034\"><input type=\"radio\" id='f034' name=\"ancho\" value=\"7\" " + (calle.anchocalle == 7 ? "checked" : "") + "> Ancha</label>\n"
                + "             <label for=\"f035\"><input type=\"radio\" id='f035' name=\"ancho\" value=\"10\" " + (calle.anchocalle == 10 ? "checked" : "") + "> Muy ancha</label>\n"
                + "             </FIELDSET>\n"
                + "             <br />\n"
                + "             Índice de criminalidad:\n"
                + "             <FIELDSET>\n"
                + "             <label for=\"f041\"><input type=\"radio\" id='f041' name=\"criminalidad\" value=\"10\" " + (calle.indicecriminalidad == 10 ? "checked" : "") + "> Nulo</label>\n"
                + "             <label for=\"f042\"><input type=\"radio\" id='f042' name=\"criminalidad\" value=\"7\" " + (calle.indicecriminalidad == 7 ? "checked" : "") + "> Bajo</label>\n"
                + "             <label for=\"f043\"><input type=\"radio\" id='f043' name=\"criminalidad\" value=\"5\" " + (calle.indicecriminalidad == 5 ? "checked" : "") + "> Normal</label>\n"
                + "             <label for=\"f044\"><input type=\"radio\" id='f044' name=\"criminalidad\" value=\"3\" " + (calle.indicecriminalidad == 3 ? "checked" : "") + "> Alto</label>\n"
                + "             <label for=\"f045\"><input type=\"radio\" id='f045' name=\"criminalidad\" value=\"0\" " + (calle.indicecriminalidad == 0 ? "checked" : "") + "> Muy alto</label>\n"
                + "             </FIELDSET>\n"
                + "             <br />\n"
                + "             Ratio de accidentes:\n"
                + "             <FIELDSET>\n"
                + "             <label for=\"f051\"><input type=\"radio\" id='f051' name=\"accidentes\" value=\"10\" " + (calle.ratioaccidentes == 10 ? "checked" : "") + "> Nulo</label>\n"
                + "             <label for=\"f052\"><input type=\"radio\" id='f052' name=\"accidentes\" value=\"7\" " + (calle.ratioaccidentes == 7 ? "checked" : "") + "> Bajo</label>\n"
                + "             <label for=\"f053\"><input type=\"radio\" id='f053' name=\"accidentes\" value=\"5\" " + (calle.ratioaccidentes == 5 ? "checked" : "") + "> Normal</label>\n"
                + "             <label for=\"f054\"><input type=\"radio\" id='f054' name=\"accidentes\" value=\"3\" " + (calle.ratioaccidentes == 3 ? "checked" : "") + "> Alto</label>\n"
                + "             <label for=\"f055\"><input type=\"radio\" id='f055' name=\"accidentes\" value=\"0\" " + (calle.ratioaccidentes == 0 ? "checked" : "") + "> Muy alto</label>\n"
                + "             </FIELDSET>\n"
                + "             <br />\n"
                + "             Superficie: \n"
                + "             <FIELDSET>\n"
                + "             <label for=\"f061\"><input type=\"radio\" id='f061' name=\"superficie\" value=\"0\" " + (calle.superficieadoquines == 0 ? "checked" : "") + "> Lisa</label> \n"
                + "             <label for=\"f062\"><input type=\"radio\" id='f062' name=\"superficie\" value=\"1\" " + (calle.superficieadoquines == 1 ? "checked" : "") + "> Adoquines</label> \n"
                + "             </FIELDSET>\n"
                + "             <br />\n"
                + "             Pasos de peatones:\n"
                + "             <FIELDSET>\n"
                + "             <label for=\"f071\"><input type=\"radio\" id='f071' name=\"peatones\" value=\"0\" " + (calle.pasopeatones == 0 ? "checked" : "") + "> Inexistentes y necesarios</label> \n"
                + "             <label for=\"f072\"><input type=\"radio\" id='f072' name=\"peatones\" value=\"3\" " + (calle.pasopeatones == 3 ? "checked" : "") + "> Deficientes</label> \n"
                + "             <label for=\"f073\"><input type=\"radio\" id='f073' name=\"peatones\" value=\"5\" " + (calle.pasopeatones == 5 ? "checked" : "") + "> Suficientes</label> \n"
                + "             <label for=\"f074\"> <input type=\"radio\" id='f074' name=\"peatones\" value=\"7\" " + (calle.pasopeatones == 7 ? "checked" : "") + "> Muy buenos</label> \n"
                + "             <label for=\"f075\"><input type=\"radio\" id='f075' name=\"peatones\" value=\"10\" " + (calle.pasopeatones == 10 ? "checked" : "") + "> Excelentes</label> \n"
                + "             </FIELDSET>\n"
                + "             <br />\n"
                + "             Semáforos:\n"
                + "             <FIELDSET>\n"
                + "             <label for=\"f081\"><input type=\"radio\" id='f081' name=\"semaforos\" value=\"0\" " + (calle.semaforos == 0 ? "checked" : "") + "> Inexistentes y necesarios</label> \n"
                + "             <label for=\"f082\"><input type=\"radio\" id='f082' name=\"semaforos\" value=\"3\" " + (calle.semaforos == 3 ? "checked" : "") + "> Deficientes</label>\n"
                + "             <label for=\"f083\"><input type=\"radio\" id='f083' name=\"semaforos\" value=\"5\" " + (calle.semaforos == 5 ? "checked" : "") + "> Suficientes</label> \n"
                + "             <label for=\"f084\"><input type=\"radio\" id='f084' name=\"semaforos\" value=\"7\" " + (calle.semaforos == 7 ? "checked" : "") + "> Muy buenos</label> \n"
                + "             <label for=\"f085\"><input type=\"radio\" id='f085' name=\"semaforos\" value=\"10\" " + (calle.semaforos == 10 ? "checked" : "") + "> Excelentes</label> \n"
                + "             </FIELDSET>\n"
                + "             <br />\n"
                + "             Zona residencial y/o comercial:\n"
                + "             <FIELDSET>\n"
                + "             <label for=\"f091\"><input type=\"radio\" id='f091' name=\"residencial\" value=\"0\" " + (calle.residencialcomercial == 0 ? "checked" : "") + "> No</label> \n"
                + "             <label for=\"f092\"><input type=\"radio\" id='f092' name=\"residencial\" value=\"1\" " + (calle.residencialcomercial == 1 ? "checked" : "") + "> Si</label> \n"
                + "             </FIELDSET>\n"
                + "             <br />\n"
                + "             Estado de conservación de los edificios:\n"
                + "             <FIELDSET>\n"
                + "             <label for=\"f101\"><input type=\"radio\" id='f101' name=\"fachadas\" value=\"0\" " + (calle.conservacionedificios == 0 ? "checked" : "") + "> Nulo</label> \n"
                + "             <label for=\"f102\"><input type=\"radio\" id='f102' name=\"fachadas\" value=\"3\" " + (calle.conservacionedificios == 3 ? "checked" : "") + "> Muy bajo</label> \n"
                + "             <label for=\"f103\"><input type=\"radio\" id='f103' name=\"fachadas\" value=\"5\" " + (calle.conservacionedificios == 5 ? "checked" : "") + "> Normal</label> \n"
                + "             <label for=\"f104\"><input type=\"radio\" id='f104' name=\"fachadas\" value=\"7\" " + (calle.conservacionedificios == 7 ? "checked" : "") + "> Alto</label> \n"
                + "             <label for=\"f105\"><input type=\"radio\" id='f105' name=\"fachadas\" value=\"10\" " + (calle.conservacionedificios == 10 ? "checked" : "") + "> Muy alto</label> \n"
                + "             </FIELDSET>\n"
                + "             <br />\n"
                + "             Nivel socioeconómico:\n"
                + "             <FIELDSET>\n"
                + "             <label for=\"f111\"><input type=\"radio\" id='f111' name=\"niveleconomico\" value=\"0\" " + (calle.niveleconomico == 0 ? "checked" : "") + "> Muy bajo</label> \n"
                + "             <label for=\"f112\"><input type=\"radio\" id='f112' name=\"niveleconomico\" value=\"3\" " + (calle.niveleconomico == 3 ? "checked" : "") + "> Bajo</label> \n"
                + "             <label for=\"f113\"><input type=\"radio\" id='f113' name=\"niveleconomico\" value=\"5\" " + (calle.niveleconomico == 5 ? "checked" : "") + "> Normal</label> \n"
                + "             <label for=\"f114\"><input type=\"radio\" id='f114' name=\"niveleconomico\" value=\"7\" " + (calle.niveleconomico == 7 ? "checked" : "") + "> Alto</label> \n"
                + "             <label for=\"f115\"><input type=\"radio\" id='f115' name=\"niveleconomico\" value=\"10\" " + (calle.niveleconomico == 10 ? "checked" : "") + "> Muy alto</label> \n"
                + "             </FIELDSET>\n"
                + "             <br />\n"
                + "             Calidad de la iluminación:\n"
                + "             <FIELDSET>\n"
                + "             <label for=\"f121\"><input type=\"radio\" id='f121' name=\"iluminacion\" value=\"0\" " + (calle.iluminacion == 0 ? "checked" : "") + "> Sin iluminación</label> \n"
                + "             <label for=\"f122\"><input type=\"radio\" id='f122' name=\"iluminacion\" value=\"3\" " + (calle.iluminacion == 3 ? "checked" : "") + "> Deficiente</label> \n"
                + "             <label for=\"f123\"><input type=\"radio\" id='f123' name=\"iluminacion\" value=\"5\" " + (calle.iluminacion == 5 ? "checked" : "") + "> Normal</label> \n"
                + "             <label for=\"f124\"><input type=\"radio\" id='f124' name=\"iluminacion\" value=\"7\" " + (calle.iluminacion == 7 ? "checked" : "") + "> Buena</label> \n"
                + "             <label for=\"f125\"><input type=\"radio\" id='f125' name=\"iluminacion\" value=\"10\" " + (calle.iluminacion == 10 ? "checked" : "") + "> Excelente</label> \n"
                + "             </FIELDSET>\n"
                + "             <br />\n"
                + "             <FIELDSET>\n"
                + "             Calle peatonal: \n"
                + "             <label for=\"f141\"><input type=\"radio\" id='f141' name=\"peatonal\" value=\"0\" " + (calle.callepeatonal == 0 ? "checked" : "") + "> No</label> \n"
                + "             <label for=\"f142\"><input type=\"radio\" id='f142' name=\"peatonal\" value=\"1\" " + (calle.callepeatonal == 1 ? "checked" : "") + "> Si</label> \n"
                + "             </FIELDSET>\n"
                + "             <br />\n"
                + "             Carril bici:\n"
                + "             <FIELDSET>\n"
                + "             <label for=\"f151\"><input type=\"radio\" id='f151' name=\"bici\" value=\"0\" " + (calle.carrilbici == 0 ? "checked" : "") + "> No</label> \n"
                + "             <label for=\"f152\"><input type=\"radio\" id='f152' name=\"bici\" value=\"1\" " + (calle.carrilbici == 1 ? "checked" : "") + "> Si</label> \n"
                + "             </FIELDSET>\n"
                + "             <br />\n"
                + "             Calidad del carril bici:\n"
                + "             <FIELDSET>\n"
                + "             <label for=\"f161\"><input type=\"radio\" id='f161' name=\"carrilbici\" value=\"0\" " + (calle.calidadcarrilbici == 0 ? "checked" : "") + "> Muy bajo</label> \n"
                + "             <label for=\"f162\"><input type=\"radio\" id='f162' name=\"carrilbici\" value=\"3\" " + (calle.calidadcarrilbici == 3 ? "checked" : "") + "> Bajo</label> \n"
                + "             <label for=\"f163\"><input type=\"radio\" id='f163' name=\"carrilbici\" value=\"5\" " + (calle.calidadcarrilbici == 5 ? "checked" : "") + "> Normal</label> \n"
                + "             <label for=\"f164\"><input type=\"radio\" id='f164' name=\"carrilbici\" value=\"7\" " + (calle.calidadcarrilbici == 7 ? "checked" : "") + "> Alto</label> \n"
                + "             <label for=\"f165\"><input type=\"radio\" id='f165' name=\"carrilbici\" value=\"10\" " + (calle.calidadcarrilbici == 10 ? "checked" : "") + "> Muy alto</label> \n"
                + "             </FIELDSET>\n"
                + "             <br />\n"
                + "             Separación entre la calzada y la acera:\n"
                + "             <FIELDSET>\n"
                + "             <label for=\"f171\"><input type=\"radio\" id='f171' name=\"separacion\" value=\"0\" " + (calle.separacioncalzadaacera == 0 ? "checked" : "") + "> No</label> \n"
                + "             <label for=\"f172\"><input type=\"radio\" id='f172' name=\"separacion\" value=\"1\" " + (calle.separacioncalzadaacera == 1 ? "checked" : "") + "> Si</label> \n"
                + "             </FIELDSET>\n"
                + "             <br />\n"
                + "             Aparcamiento junto a la acera: \n"
                + "             <FIELDSET>\n"
                + "             <label for=\"f181\"><input type=\"radio\" id='f181' name=\"aparcamiento\" value=\"1\" " + (calle.aparcamientoacera == 1 ? "checked" : "") + "> No</label> \n"
                + "             <label for=\"f182\"><input type=\"radio\" id='f182' name=\"aparcamiento\" value=\"0\" " + (calle.aparcamientoacera == 0 ? "checked" : "") + "> Si</label> \n"
                + "             </FIELDSET>\n"
                + "             <br />\n"
                + "             Badenes: \n"
                + "             <FIELDSET>\n"
                + "             <label for=\"f191\"><input type=\"radio\" id='f191' name=\"badenes\" value=\"0\" " + (calle.badenes == 0 ? "checked" : "") + "> No</label> \n"
                + "             <label for=\"f192\"><input type=\"radio\" id='f192' name=\"badenes\" value=\"1\" " + (calle.badenes == 1 ? "checked" : "") + "> Si o no necesarios</label> \n"
                + "             </FIELDSET>\n"
                + "             <br />\n"
                + "             Radar:\n"
                + "             <FIELDSET>\n"
                + "             <label for=\"f201\"><input type=\"radio\" id='f201' name=\"radar\" value=\"0\" " + (calle.radar == 0 ? "checked" : "") + "> No</label> \n"
                + "             <label for=\"f202\"><input type=\"radio\" id='f202' name=\"radar\" value=\"1\" " + (calle.radar == 1 ? "checked" : "") + "> Si o no necesarios</label> \n"
                + "             </FIELDSET>\n"
                + "             <br />\n"
                + "             Nivel de pendiente:\n"
                + "             <FIELDSET>\n"
                + "             <label for=\"f211\"><input type=\"radio\" id='f211' name=\"pendiente\" value=\"10\" " + (calle.pendiente == 10 ? "checked" : "") + "> Llano</label> \n"
                + "             <label for=\"f212\"><input type=\"radio\" id='f212' name=\"pendiente\" value=\"7\" " + (calle.pendiente == 7 ? "checked" : "") + "> Leve inclinación</label> \n"
                + "             <label for=\"f213\"><input type=\"radio\" id='f213' name=\"pendiente\" value=\"5\" " + (calle.pendiente == 5 ? "checked" : "") + "> Pendiente apreciable</label> \n"
                + "             <label for=\"f214\"><input type=\"radio\" id='f214' name=\"pendiente\" value=\"3\" " + (calle.pendiente == 3 ? "checked" : "") + "> Desnivel alto</label> \n"
                + "             <label for=\"f215\"><input type=\"radio\" id='f215' name=\"pendiente\" value=\"0\" " + (calle.pendiente == 0 ? "checked" : "") + "> Gran pendiente</label> \n"
                + "             </FIELDSET>\n"
                + "             <br />\n"
                + "             Nivel de confort:\n"
                + "             <FIELDSET>\n"
                + "             <label for=\"f221\"><input type=\"radio\" id='f221' name=\"confort\" value=\"0\" " + (calle.confort == 0 ? "checked" : "") + "> Muy bajo</label> \n"
                + "             <label for=\"f221\"><input type=\"radio\" id='f222' name=\"confort\" value=\"3\" " + (calle.confort == 3 ? "checked" : "") + "> Bajo</label> \n"
                + "             <label for=\"f223\"><input type=\"radio\" id='f223' name=\"confort\" value=\"5\" " + (calle.confort == 5 ? "checked" : "") + "> Normal</label> \n"
                + "             <label for=\"f224\"><input type=\"radio\" id='f224' name=\"confort\" value=\"7\" " + (calle.confort == 7 ? "checked" : "") + "> Alto</label> \n"
                + "             <label for=\"f225\"><input type=\"radio\" id='f225' name=\"confort\" value=\"10\" " + (calle.confort == 10 ? "checked" : "") + "> Muy alto</label> \n"
                + "             </FIELDSET>\n"
                + "             <br />\n"
                + "             Control de policía: \n"
                + "             <FIELDSET>\n"
                + "             <label for=\"f231\"><input type=\"radio\" id='f231' name=\"policia\" value=\"0\" " + (calle.policía == 0 ? "checked" : "") + "> No</label> \n"
                + "             <label for=\"f232\"><input type=\"radio\" id='f232' name=\"policia\" value=\"1\" " + (calle.policía == 1 ? "checked" : "") + "> Si o no necesarios</label> \n"
                + "             </FIELDSET>\n"
                + "             <br />";

        //Estadística
        LogSR.mensaje("INFOPUNTO" + "\tlatitud:" + latitud + "\tlongitud:" + longitud, LogSR.ESTADISTICAS);

        return salida;
    }

    /**
     * Guarda o actualiza la información de seguridad para un punto concreto.
     *
     * @param peticion
     * @return
     */
    private String anadirInfoPunto(String peticion) {
        String[] trozos = peticion.split("#");
        int idCreador = Integer.parseInt(trozos[1]);
        int privado = Integer.parseInt(trozos[2]);
        int elevar = Integer.parseInt(trozos[3]);
        double lat = Double.parseDouble(trozos[4]);
        double lon = Double.parseDouble(trozos[5]);
        String valores = trozos[6];

        //Estadística
        LogSR.mensaje("INFOPUNTOPONER" + "\tlatitud:" + lat + "\tlongitud:" + lon, LogSR.ESTADISTICAS);

        return mapa.anadirInfoPunto(lat, lon, valores, idCreador, privado, elevar);
    }

    /**
     * Guarda información sobre una alerta y cambia los parámetros de seguridad.
     *
     * @param peticion
     * @param comentario
     * @return
     */
    private String anadirIcono(String peticion, String comentario) {
        //Obtener info de la petición 
        //os.write("crearalerta#" + idCreador + "#" + lat + "#" + lon + "#" + tipo + "\n");
        String[] trozos = peticion.split("#");
        int idCreador = Integer.parseInt(trozos[1]);
        double lat = Double.parseDouble(trozos[2]);
        double lon = Double.parseDouble(trozos[3]);
        int tipo = Integer.parseInt(trozos[4]);

        //Limpiar mensaje de caracteres reservados
        comentario = comentario.replaceAll("#", "·");

        //Incluir información en la bd y memoria
        String id = mapa.putAlertaIcono(idCreador, tipo, lat, lon, comentario, true);

        //Estadística
        LogSR.mensaje("ICONO" + "\tid:" + id + "\tlatitud:" + lat + "\tlongitud:" + lon, LogSR.ESTADISTICAS);

        return "";
    }

    /**
     * Informa de los límites que tiene el mapa sobre el que trabajamos.
     *
     * @param peticion
     * @return Lat sup, lat inf, lon izq, lon der separados por #
     */
    private String getFrontera(String peticion) {
        //Obtener límites del mapa
        BBox limites = mapa.getGraphHopper().getGraph().getBounds();

        double sup = limites.maxLat;
        double inf = limites.minLat;
        double izq = limites.minLon;
        double der = limites.maxLon;

        //Devolver límites del mapa
        return "" + sup + "#" + inf + "#" + izq + "#" + der + "\n";
    }

    /**
     * Informa de las alertas con icono que existen.
     *
     * @param peticion
     * @return Lat sup, lat inf, lon izq, lon der separados por # y cada
     * elemento separado por #-#
     */
    private String getIconos(String peticion) {
        String salida = "";
        //Obtener lista de iconos
        List<String[]> lista = mapa.getAlertasIcono();

        for (String[] elemento : lista) {
            //Para cada alerta -id, tipo, comentario, latitud, longitud, idCreador
            salida += elemento[0] + "#" + elemento[1] + "#" + elemento[2] + "#" + elemento[3] + "#" + elemento[4] + "#" + elemento[5] + "#-#";

        }

        //Devolver
        return salida + "\n";
    }

    /**
     * Crea una alerta para mostrar en la página principal
     */
    private String putNoticia(String peticion, String mensaje) {
        //Obtener info de la petición 
        //os.write("poneralerta#" + idCreador + "#" + titulo + "#" + texto + "#" + "\n");
        String[] trozos = peticion.split("#");
        int idCreador = Integer.parseInt(trozos[1]);

        //Incluir información en la bd
        return mapa.putAlertaTexto(idCreador, trozos[2], mensaje);
    }

    /**
     * Devuelve una lista con las alertas o "" si no existe ninguna.
     * id#idcreador#titulo#texto#fechacreacion#-#
     *
     * @param peticion
     * @return
     */
    private String getNoticias(String peticion) {
        String salida = "";
        List<String[]> lista;
        //Obtener lista de alertas
        lista = mapa.getAlertasTexto();

        for (String[] elemento : lista) {
            salida += elemento[0] + "#" + elemento[1] + "#" + elemento[2] + "#" + elemento[3] + "#" + elemento[4] + "#-#";
        }

        //Devolver
        return salida;
    }

    /**
     * Borra una noticia de la portada
     *
     * @param peticion
     * @return
     */
    private String borrarNoticia(String peticion) {
        //Obtener info de la petición 
        String[] trozos = peticion.split("#");
        int id = Integer.parseInt(trozos[1]);
        int idBorrador = Integer.parseInt(trozos[2]);

        //Comprobar si el usuario tiene derecho
        if (mapa.esAdministrador(idBorrador)) {
            //Borrar de la base de datos
            mapa.borrarAlerta(id);
        }

        return "-1";

    }

    /**
     * Genera una nueva clave aleatoria y la envía al usuario por email.
     *
     * @param peticion
     * @return
     */
    private String recuperarClave(String peticion) {
        String salida;
        String nuevaClave = mapa.generarClaveAleatoria(8);

        //Obtener info de la petición 
        String[] trozos = peticion.split("#");
        String email = trozos[1];

        //Nueva clave
        if (!(salida = mapa.cambiarClave(email, nuevaClave)).equals("-1")) {
            //Enviar email
            try {
                ServidorEmailUgr servidorEmail = new ServidorEmailUgr();
                //Cargar cabecera
                String textoClaveRecuperada = servidorEmail.leerFicheroTexto(Propiedades.getPropiedades().leerPropiedad(Propiedades.mensajecambioclave));
                //CargarPie
                String textoPie = servidorEmail.leerFicheroTexto(Propiedades.getPropiedades().leerPropiedad(Propiedades.mensajepie));

                String mensaje = textoClaveRecuperada
                        + "<p>Usuario: <strong>" + email + "</strong></p>\n"
                        + "<p>Clave: <strong>" + nuevaClave + "</strong></p>\n"
                        + textoPie;
                if (!salida.equals("-1")) {
                    servidorEmail.enviarEmail(email, "Nueva contraseña - Servicio rutas seguras", mensaje);
                }
            } catch (Exception ex) {
                LogSR.mensaje("Error al enviar email de nueva clave." + ex.getMessage(), LogSR.ERROR);
            }
            //Estadística
            LogSR.mensaje("RECUPERARCLAVE" + "\tusuario:" + email, LogSR.ESTADISTICAS);
        }

        return salida;
    }

    /**
     * Permite enviar un mensaje al administrador de la plataforma.
     *
     * @param peticion
     * @param mensaje
     * @return
     */
    private String contactoAdmin(String peticion, String mensaje) {
        String[] trozos = peticion.split("#");
        String asunto = trozos[1];
        String email = trozos[2];
        mensaje = "Mensaje enviado por: " + email + "\n<br/>" + mensaje;

        //Enviar email
        ServidorEmailUgr servidorEmail = new ServidorEmailUgr();
        servidorEmail.enviarEmail(Propiedades.getPropiedades().leerPropiedad(Propiedades.mailremitente), asunto, mensaje);

        return "";
    }

    /**
     * Devuelve la lista de mensajes principales del foro.
     *
     * @return listado id\uffff idpadre\uffffidautor\uffff fecha\uffff
     * titulo\uffff mensaje separado por \fffe\uffff
     */
    private String listarMensajes(String peticion) {
        List<String[]> mensajes = mapa.getMensajes();
        String salida = "";

        String autor;

        for (String[] mensaje : mensajes) {
            //Autor del mensaje
            int idautor = Integer.parseInt(mensaje[2]);
            autor = mapa.getInfoUsuario(idautor).nombreUsuario;
            salida += mensaje[0] + "\u0001" + mensaje[1] + "\u0001" + autor + "\u0001" + mensaje[3] + "\u0001" + mensaje[4] + "\u0001" + mensaje[5] + "\ufffe\uffff";
        }

        return salida + "\n";
    }

    /**
     * Devuelve la lista de mensajes de un hilo dado.
     *
     * @param peticion
     * @return
     */
    private String listarHilo(String peticion) {
        String[] trozos = peticion.split("#");
        int idPadre = Integer.parseInt(trozos[1]);

        List<String[]> mensajes = mapa.getMensaje(idPadre);
        String salida = "";

        String autor;
        

        for (int i=0;i<mensajes.size();i++) {
            String[] mensaje=null;
            mensaje=mensajes.get(i);
            //Autor del mensaje
            int idautor = Integer.parseInt(mensaje[2]);
            autor = mapa.getInfoUsuario(idautor).nombreUsuario;
            
            salida += mensaje[0] + "\u0001" + mensaje[1] + "\u0001" + autor + "\u0001" + mensaje[3] + "\u0001" + mensaje[4] + "\u0001" + mensaje[5] + "\ufffe\uffff";
        }

        return salida;
    }

    /**
     * Publica un mensaje en el foro
     *
     * @param peticion idpadre (-1 para principal) # idautor # titulo # mensaje
     * @return
     */
    private String publicarMensaje(String peticion, String mensaje) {
        String salida;
        String[] trozos = peticion.split("#");

        int idpadre = Integer.parseInt(trozos[1]);
        int idautor = Integer.parseInt(trozos[2]);
        String titulo = trozos[3];

        //Publicar mensaje
        salida = "" + mapa.putMensaje(idpadre, idautor, titulo, mensaje);

        return salida;
    }

    /**
     * Borra un mensaje de un hilo o todo el hilo.
     *
     * @param peticion
     * @return
     */
    private String borrarMensaje(String peticion) {
        String salida = "-1";
        String[] trozos = peticion.split("#");

        int id = Integer.parseInt(trozos[1]);
        int idBorrador = Integer.parseInt(trozos[2]);

        //Comprobar el usuario
        if (!mapa.esAdministrador(idBorrador)) {
            return salida;
        }

        if (mapa.borrarMensaje(id)) {
            salida = "1";
        }

        return salida;
    }

    /**
     * Borra un icono del mapa.
     *
     * @param peticion
     * @return
     */
    private String borrarIcono(String peticion) {
        String salida = "-1";
        String[] trozos = peticion.split("#");

        int id = Integer.parseInt(trozos[1]);
        int idBorrador = Integer.parseInt(trozos[2]);

        if (mapa.borrarIcono(id)) {
            salida = "1";
        }

        return salida;
    }
}
