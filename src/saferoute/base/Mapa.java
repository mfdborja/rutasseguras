package saferoute.base;

import com.graphhopper.GraphHopper;
import com.graphhopper.routing.AbstractBidirAlgo;
import com.graphhopper.routing.DijkstraBidirectionRef;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.util.AllEdgesIterator;
import com.graphhopper.routing.util.CarFlagEncoder;
import com.graphhopper.routing.util.DefaultEdgeFilter;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.FootFlagEncoder;
import com.graphhopper.routing.util.MountainBikeFlagEncoder;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.storage.GraphStorage;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.InstructionList;
import com.graphhopper.util.PointList;
import com.graphhopper.util.Translation;
import com.graphhopper.util.TranslationMap;
import com.graphhopper.util.shapes.BBox;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Random;
import saferoute.bd.BaseDatos;
import saferoute.bd.Propiedades;
import saferoute.comunicacion.AlertasMeteo;
import saferoute.log.LogSR;
import saferoute.struct.Calle;
import saferoute.struct.Instruccion;
import saferoute.struct.Pareja;
import saferoute.struct.Persona;
import saferoute.struct.ValoracionTramo;

public class Mapa {

    //Variables estáticas
    /**
     * Indicativo de una ruta peatonal
     */
    public static final byte RUTA_A_PIE = 1;
    /**
     * Indicativo de una ruta ciclista
     */
    public static final byte RUTA_A_BICI = 2;

    /**
     * Instancia del mapa
     */
    private static Mapa mapa = null;

    /**
     * Instanncia de la BD.
     */
    private BaseDatos bd = null;

    /**
     * Instancia del mapa graphhopper
     */
    private final GraphHopper grafo;
    
    ValoracionTramo valoresSeguridad = new ValoracionTramo();

    /**
     * Información de la seguridad de los arcos. En la primera posición el
     * índice a pie y en la segunda en bici.
     */
    private final TIntObjectHashMap<Pareja<Double, Double>> arcosIndSeguridad;
    /**
     * Información de arcos con una alerta de icono y la valoración
     */
    private final TIntDoubleHashMap arcosAlertasIcono;

    /**
     * Codificadores de rutas
     */
    EncodingManager emAPie = new EncodingManager(EncodingManager.FOOT);
    EncodingManager emBici = new EncodingManager(EncodingManager.BIKE);

    /**
     * Alertas del tiempo
     */
    AlertasMeteo alertaTiempo;

    /**
     * Operaciones referentes al mapa
     */
    /**
     * Crea una instancia del mapa. Lee de disco la información.
     *
     * @param rutaMapa
     */
    private Mapa() {
        //BD
        bd = new BaseDatos();
        bd.conectarLaBD();

        //Arcos Seguridad
        this.arcosIndSeguridad = new TIntObjectHashMap();
        this.arcosAlertasIcono = new TIntDoubleHashMap();

        //Alerta tiempo
        alertaTiempo = new AlertasMeteo(bd);

        //Iniciar la librería del grafo
        //Grafo
        grafo = new GraphHopper();
        //Versión para servidor        
        grafo.setElevation(true);
        grafo.forServer();
        //En memoria el mapa
        grafo.setInMemory();
        //Eliminar preprocesado de pesos
        grafo.setCHEnable(false);
        //Ruta del archivo
        grafo.setOSMFile(Propiedades.getPropiedades().leerPropiedad(Propiedades.aplicacionmapa));
        //Ruta para el archivo expandido
        grafo.setGraphHopperLocation(Propiedades.getPropiedades().leerPropiedad(Propiedades.aplicacionmapa) + "_Expand");
        //Tipo de rutas
        grafo.setEncodingManager(emAPie);
        //Inicia el grafo
        grafo.importOrLoad();

        //Cargar valores de los pesos
        cargarConfSeguridad();

        //Obtener info implicita mapa de las calles
        //leerInfoCallesImplicita();
        //Cargar datos de la BD
        System.out.println("Reconstruyendo base de datos. Espere por favor.");
        LogSR.mensaje("Reconstruyendo base de datos.", LogSR.INFO);
        reconstruirInfoSeguridad();
        System.out.println("Base de datos reconstruida.");
        LogSR.mensaje("Base de datos reconstruida.", LogSR.INFO);

        //Info del grafo
        LogSR.mensaje("\nInformación del mapa:" + grafo.getGraph().toDetailsString(), LogSR.INFO);
    }
    
    private void cargarConfSeguridad() {
        
        File archivoPesos = new File(Propiedades.getPropiedades().leerPropiedad(Propiedades.aplicacionvariables));
        Properties propPesos = new Properties();
        
        if (!archivoPesos.exists()) {
            propPesos.put("densidadtraficoA", "8");
            propPesos.put("densidadtraficoB", "8");
            propPesos.put("aceraA", "76");
            propPesos.put("aceraB", "0");
            propPesos.put("anchocalleA", "24");
            propPesos.put("anchocalleB", "18");
            propPesos.put("criminalidadA", "8");
            propPesos.put("criminalidadB", "8");
            propPesos.put("ratioaccidentesA", "6");
            propPesos.put("ratioaccidentesB", "6");
            propPesos.put("superficieadoquinesA", "2");
            propPesos.put("superficieadoquinesB", "0");
            propPesos.put("superficielisaA", "0");
            propPesos.put("superficielisaB", "8");
            propPesos.put("pasopeatonesA", "30");
            propPesos.put("pasopeatonesB", "0");
            propPesos.put("semaforosA", "32");
            propPesos.put("semaforosB", "28");
            propPesos.put("residencialA", "12");
            propPesos.put("residencialB", "12");
            propPesos.put("conservacionedificiosA", "6");
            propPesos.put("conservacionedificiosB", "6");
            propPesos.put("niveleconomicoA", "6");
            propPesos.put("niveleconomicoB", "6");
            propPesos.put("iluminacionA", "8");
            propPesos.put("iluminacionB", "8");
            propPesos.put("callepeatonalA", "18");
            propPesos.put("callepeatonalB", "0");
            propPesos.put("carrilbiciA", "0");
            propPesos.put("carrilbiciB", "18");
            propPesos.put("separacionaceraA", "0");
            propPesos.put("separacionaceraB", "74");
            propPesos.put("aparcamientoaceraA", "12");
            propPesos.put("aparcamientoaceraB", "0");
            propPesos.put("badenesA", "4");
            propPesos.put("badenesB", "4");
            propPesos.put("radarA", "6");
            propPesos.put("radarB", "6");
            propPesos.put("pendienteA", "4");
            propPesos.put("pendienteB", "8");
            propPesos.put("confortA", "6");
            propPesos.put("confortB", "4");
            propPesos.put("policiaA", "8");
            propPesos.put("policiaB", "8");
            propPesos.put("velocidadA", "8");
            propPesos.put("velocidadB", "10");
            propPesos.put("totalpuntosA", "288");
            propPesos.put("totalpuntosB", "248");
            //Se guarda
            try {
                FileOutputStream flujoSalida = new FileOutputStream(archivoPesos);
                propPesos.storeToXML(flujoSalida, "Archivo de configuración de índice de seguridad.");
                LogSR.mensaje("Se ha creado un archivo de configuración de seguridad. ", LogSR.INFO);
            } catch (IOException ex) {
                LogSR.mensaje("Error al crear el archivo de configuración de seguridad. " + ex.getMessage(), LogSR.ERROR);
            }
        }

        //Cargar datos
        propPesos = new Properties();
        try {
            FileInputStream flujoEntrada = new FileInputStream(archivoPesos);
            propPesos.loadFromXML(flujoEntrada);
            
            valoresSeguridad.densidadtraficoA = Integer.parseInt(propPesos.getProperty("densidadtraficoA", "0"));
            valoresSeguridad.densidadtraficoB = Integer.parseInt(propPesos.getProperty("densidadtraficoB", "0"));
            valoresSeguridad.aceraA = Integer.parseInt(propPesos.getProperty("aceraA", "0"));
            valoresSeguridad.aceraB = Integer.parseInt(propPesos.getProperty("aceraB", "0"));
            valoresSeguridad.anchocalleA = Integer.parseInt(propPesos.getProperty("anchocalleA", "0"));
            valoresSeguridad.anchocalleB = Integer.parseInt(propPesos.getProperty("anchocalleB", "0"));
            valoresSeguridad.criminalidadA = Integer.parseInt(propPesos.getProperty("criminalidadA", "0"));
            valoresSeguridad.criminalidadB = Integer.parseInt(propPesos.getProperty("criminalidadB", "0"));
            valoresSeguridad.ratioaccidentesA = Integer.parseInt(propPesos.getProperty("ratioaccidentesA", "0"));
            valoresSeguridad.ratioaccidentesB = Integer.parseInt(propPesos.getProperty("ratioaccidentesB", "0"));
            valoresSeguridad.superficieadoquinesA = Integer.parseInt(propPesos.getProperty("superficieadoquinesA", "0"));
            valoresSeguridad.superficieadoquinesB = Integer.parseInt(propPesos.getProperty("superficieadoquinesB", "0"));
            valoresSeguridad.superficielisaA = Integer.parseInt(propPesos.getProperty("superficielisaA", "0"));
            valoresSeguridad.superficielisaB = Integer.parseInt(propPesos.getProperty("superficielisaB", "0"));
            valoresSeguridad.pasopeatonesA = Integer.parseInt(propPesos.getProperty("pasopeatonesA", "0"));
            valoresSeguridad.pasopeatonesB = Integer.parseInt(propPesos.getProperty("pasopeatonesB", "0"));
            valoresSeguridad.semaforosA = Integer.parseInt(propPesos.getProperty("semaforosA", "0"));
            valoresSeguridad.semaforosB = Integer.parseInt(propPesos.getProperty("semaforosB", "0"));
            valoresSeguridad.residencialA = Integer.parseInt(propPesos.getProperty("residencialA", "0"));
            valoresSeguridad.residencialB = Integer.parseInt(propPesos.getProperty("residencialB", "0"));
            valoresSeguridad.conservacionedificiosA = Integer.parseInt(propPesos.getProperty("conservacionedificiosA", "0"));
            valoresSeguridad.conservacionedificiosB = Integer.parseInt(propPesos.getProperty("conservacionedificiosB", "0"));
            valoresSeguridad.niveleconomicoA = Integer.parseInt(propPesos.getProperty("niveleconomicoA", "0"));
            valoresSeguridad.niveleconomicoB = Integer.parseInt(propPesos.getProperty("niveleconomicoB", "0"));
            valoresSeguridad.iluminacionA = Integer.parseInt(propPesos.getProperty("iluminacionA", "0"));
            valoresSeguridad.iluminacionB = Integer.parseInt(propPesos.getProperty("iluminacionB", "0"));
            valoresSeguridad.callepeatonalA = Integer.parseInt(propPesos.getProperty("callepeatonalA", "0"));
            valoresSeguridad.callepeatonalB = Integer.parseInt(propPesos.getProperty("callepeatonalB", "0"));
            valoresSeguridad.carrilbiciA = Integer.parseInt(propPesos.getProperty("carrilbiciB", "0"));
            valoresSeguridad.separacionaceraA = Integer.parseInt(propPesos.getProperty("separacionaceraA", "0"));
            valoresSeguridad.separacionaceraB = Integer.parseInt(propPesos.getProperty("separacionaceraB", "0"));
            valoresSeguridad.aparcamientoaceraA = Integer.parseInt(propPesos.getProperty("aparcamientoaceraA", "0"));
            valoresSeguridad.aparcamientoaceraB = Integer.parseInt(propPesos.getProperty("aparcamientoaceraB", "0"));
            valoresSeguridad.badenesA = Integer.parseInt(propPesos.getProperty("badenesA", "0"));
            valoresSeguridad.badenesB = Integer.parseInt(propPesos.getProperty("badenesB", "0"));
            valoresSeguridad.radarA = Integer.parseInt(propPesos.getProperty("radarA", "0"));
            valoresSeguridad.radarB = Integer.parseInt(propPesos.getProperty("radarB", "0"));
            valoresSeguridad.pendienteA = Integer.parseInt(propPesos.getProperty("pendienteA", "0"));
            valoresSeguridad.pendienteB = Integer.parseInt(propPesos.getProperty("pendienteB", "0"));
            valoresSeguridad.confortA = Integer.parseInt(propPesos.getProperty("confortA", "0"));
            valoresSeguridad.confortB = Integer.parseInt(propPesos.getProperty("confortB", "0"));
            valoresSeguridad.policiaA = Integer.parseInt(propPesos.getProperty("policiaA", "0"));
            valoresSeguridad.policiaB = Integer.parseInt(propPesos.getProperty("policiaB", "0"));
            valoresSeguridad.velocidadA = Integer.parseInt(propPesos.getProperty("velocidadA", "0"));
            valoresSeguridad.velocidadB = Integer.parseInt(propPesos.getProperty("velocidadB", "0"));
            valoresSeguridad.totalpuntosA = Integer.parseInt(propPesos.getProperty("totalpuntosA", "100"));
            valoresSeguridad.totalpuntosB = Integer.parseInt(propPesos.getProperty("totalpuntosB", "100"));
        } catch (IOException | NumberFormatException ex) {
            LogSR.mensaje("Error al leer el archivo de configuración de variables de seguridad. " + ex.getMessage(), LogSR.ERROR);
            System.err.println("Error al leer el archivo de configuración de variables de seguridad.\n "
                    + "El servidor de rutas seguras se ha cerrado.");
            System.exit(1);
        }
    }

    /**
     * Devuelve una instancia de mapa.
     *
     * @return Un mapa
     */
    public static Mapa getMapa() {
        if (mapa == null) {
            mapa = new Mapa();
        }
        return mapa;
    }

    /**
     * Activa el cron para alertas atmosféricas
     */
    public void activarAlertasTiempo() {
        //Preparar cron
        alertaTiempo.start();
    }

    /**
     * Devuelve el grafo que representa el mapa.
     *
     * @return
     */
    public GraphHopper getGraphHopper() {
        return grafo;
    }

    /*
     Recuperar información ante errores.
     */
    /**
     * Lee la base de datos y carga la información de seguridad en memoria.
     */
    private void reconstruirInfoSeguridad() {
        //Para cada calle, introducir info en la memoria
        List<Calle> listaCalles = bd.getSeguridad();

        //Punto medio del tramo
        double latitudmedia;
        double longitudmedia;
        //Id de la calle en el mapa actual
        int idnuevocalle;

        /*V2*/
        //Borrar calles viejas
        bd.borrarSeguridad(-1);

        //Crear todas las calles nuevas
        for (Calle calle : listaCalles) {
            //Punto medio de la calle
            latitudmedia = (calle.latitud1 + calle.latitud2) / 2.0;
            longitudmedia = (calle.longitud1 + calle.longitud2) / 2.0;

            //Nuevo id - Si el mapa se actualiza
            idnuevocalle = getArcoCercano(latitudmedia, longitudmedia);
            calle.id = idnuevocalle;
            bd.setSeguridad(calle);

            //Ponerla en memoria
            nuevosValoresSeguridad(calle);
        }
        listaCalles.clear();
        listaCalles = null;

        /*V2*******/
        /*for (Calle calle : listaCalles) {
         //Punto medio de la calle
         latitudmedia = (calle.latitud1 + calle.latitud2) / 2.0;
         longitudmedia = (calle.longitud1 + calle.longitud2) / 2.0;
            
            
         //Nuevo id - Si el mapa se actualiza
         idnuevocalle = getArcoCercano(latitudmedia, longitudmedia);
         if (calle.id != idnuevocalle) {
         //Borrar la calle antigua y crearla de nuevo - Sirve si el mapa es nuevo
         bd.borrarSeguridad(calle.id);

         //Darle los nuevos valores
         calle.id = idnuevocalle;
         bd.setSeguridad(calle);
         }

         //Ponerla en memoria
         nuevosValoresSeguridad(calle);
         }*/
        //Alertas con iconos
        List<String[]> listaIconos = bd.listarIconos();
        
        for (String[] icono : listaIconos) {
            //Obtener datos del icono
            //Guardarlo en memoria
            putAlertaIcono(-1, Integer.parseInt(icono[1]), Double.parseDouble(icono[3]), Double.parseDouble(icono[4]), icono[2], false);
        }
    }

    /**
     * Incluye información de una calle si no existe de la información del
     * grafo.
     */
    private void leerInfoCallesImplicita() {
        EncodingManager em; //No borrar
        FlagEncoder encoder;
        encoder = new CarFlagEncoder();
        em = new EncodingManager(encoder);
        
        GraphStorage graph = grafo.getGraph();

        //Cargar índice
        LocationIndex index = grafo.getLocationIndex();
        
        AllEdgesIterator listaArcos = grafo.getGraph().getAllEdges();
        long banderas;
        int idArco;
        Calle tmpCalle;
        double aLt;
        double aLn;
        double bLt;
        double bLn;
        int nodoA;
        int nodoB;

        //Para cada arco
        while (listaArcos.next()) {

            //Ids
            idArco = listaArcos.getEdge();
            Calle id = bd.getSeguridad(idArco);
            
            if (id == null) {
                nodoA = listaArcos.getBaseNode();
                nodoB = listaArcos.getAdjNode();

                //Lats y lons
                aLt = grafo.getGraph().getNodeAccess().getLat(nodoA);
                aLn = grafo.getGraph().getNodeAccess().getLon(nodoA);
                bLt = grafo.getGraph().getNodeAccess().getLat(nodoB);
                bLn = grafo.getGraph().getNodeAccess().getLon(nodoB);

                //Banderas
                banderas = listaArcos.getFlags();

                //Velocidad de los coches
                tmpCalle = new Calle();
                tmpCalle.id = idArco;
                tmpCalle.velocidadmaxima = ((int) Math.max(encoder.getSpeed(banderas), encoder.getReverseSpeed(banderas))) - 5;
                //Id creador                
                tmpCalle.idcreador = -1;

                //Privado
                tmpCalle.privado = 0;

                //Elevar
                tmpCalle.elevar = 0;

                //Posición
                tmpCalle.latitud1 = aLt;
                tmpCalle.longitud1 = aLn;
                tmpCalle.latitud2 = bLt;
                tmpCalle.longitud2 = bLn;

                //Añadir calle a bd
                bd.setSeguridad(tmpCalle);

                //Ponerla en memoria
                nuevosValoresSeguridad(tmpCalle);
            }
            
        }
    }

    /*
     Rutas
     */
    /**
     * Genera una ruta entre dos puntos
     *
     * @param deLat Latitud del punto de partida
     * @param deLon Longitud del punto de partida
     * @param hastaLat Lat del punto de llegada
     * @param hastaLon Lon del punto de llegada
     * @param tipoRuta Si la ruta es a pie o en bici
     * @param indiceSeguridad Seguridad vs distancia 1-0
     * @param usuario
     * @return Un camino con la ruta.
     */
    public Path calcularRuta(double deLat, double deLon, double hastaLat, double hastaLon, byte tipoRuta,
            double indiceSeguridad, int usuario) {
        EncodingManager em; //No borrar, sino no funciona.
        FlagEncoder encoder;
        /*if (tipoRuta == RUTA_A_BICI) {
         encoder = new MountainBikeFlagEncoder();
         em = new EncodingManager(encoder);
         } else {*/
        encoder = new FootFlagEncoder();
        em = new EncodingManager(encoder);
        /*}*/
        GraphStorage graph = grafo.getGraph();

        //Cargar índice
        LocationIndex index = grafo.getLocationIndex();

        //Busqueda con Indice , 
        QueryResult desde = index.findClosest(deLat, deLon, EdgeFilter.ALL_EDGES);
        QueryResult hasta = index.findClosest(hastaLat, hastaLon, EdgeFilter.ALL_EDGES);

        //Algoritmo
        AbstractBidirAlgo algoritmo;

        //Cálculo de pesos de tramos
        Peso peso = new Peso(tipoRuta, usuario, this);
        peso.cambiarImportanciaSeguridad(indiceSeguridad);
        
        algoritmo = new DijkstraBidirectionRef(graph, encoder, peso, TraversalMode.NODE_BASED);
        //algoritmo=new AStarBidirection(graph, encoder, peso, TraversalMode.NODE_BASED);
        Path path = algoritmo.calcPath(desde.getClosestNode(), hasta.getClosestNode());
        return path;
    }

    /**
     * Da una lista de instrucciones a partir de un path.
     *
     * @param path
     * @return Lista de instrucciones
     */
    public List<Instruccion> getInstruccionesBasico(Path path) {
        List<Instruccion> salida = new ArrayList();
        
        TranslationMap tm = new TranslationMap();
        Translation t = tm.getWithFallBack(new Locale("es_ES"));
        InstructionList il = path.calcInstructions(t);
        //Lista de instrucciones
        for (int i = 0; i < il.size(); i++) {
            Instruccion instruccion = new Instruccion(il.get(i));
            salida.add(instruccion);
        }
        
        return salida;
    }

    /**
     * Distancia total de la ruta en metros.
     *
     * @param path
     * @return
     */
    public int getDistanciaRuta(Path path) {
        return (int) path.getDistance();
    }

    /**
     * Devuelve una cadena con las coordenadas de una ruta
     *
     * @param path
     * @return lat,lon;lat,lon...
     */
    public String getCoordenadasTexto(Path path) {
        String salida = "";
        
        PointList lp = path.calcPoints();
        for (int i = 0; i < lp.size(); i++) {
            salida += lp.getLat(i) + "," + lp.getLon(i) + ";";
        }
        
        return salida;
    }

    /**
     * Genera puntos listos para Google maps.
     *
     * @param path
     * @return new google.maps.LatLng(...),[...]new google.maps.LatLng()
     */
    public String getCoordenadasGoogle(Path path) {
        String salida = "";
        
        PointList lp = path.calcPoints();
        for (int i = 0; i < lp.size(); i++) {
            salida += "new google.maps.LatLng(" + lp.getLat(i) + ", " + lp.getLon(i) + ")";
            if (i < (lp.size() - 1)) {
                salida += ",\n";
            }
        }
        
        return salida;
    }

    /**
     * Genera un documento kml como String a partir de una ruta
     *
     * @param path
     * @return String con el xml
     */
    public String getKML(Path path) {
        PointList listaPuntos = path.calcPoints();
        String kml = "";
        //Declaración
        kml += "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                + "<kml xmlns=\"http://earth.google.com/kml/2.2\" xmlns:gx=\"http://www.google.com/kml/ext/2.2\">\n"
                + "<Document>\n"
                + "<name>Ruta segura</name>\n"
                //+ "<description>Ruta segura descripcion</description>\n"
                + "<StyleMap id=\"multiTrack\">"
                + "<Pair><key>normal</key><styleUrl>#multiTrack_n</styleUrl></Pair>"
                + "<Pair><key>highlight</key><styleUrl>#multiTrack_h</styleUrl></Pair>"
                + "</StyleMap>"
                + "<Style id=\"multiTrack_n\">"
                + "<IconStyle><Icon><href>http://earth.google.com/images/kml-icons/track-directional/track-0.png</href></Icon></IconStyle>"
                + "<LineStyle><color>99ffac59</color><width>6</width></LineStyle>"
                + "</Style>"
                + "<Style id=\"multiTrack_h\"><IconStyle><scale>1.2</scale>"
                + "<Icon><href>http://earth.google.com/images/kml-icons/track-directional/track-0.png</href></Icon></IconStyle>"
                + "<LineStyle><color>99ffac59</color><width>8</width></LineStyle></Style>"
                + "<Folder>\n"
                + "<name>Waypoints</name>\n"
                + "<visibility>1</visibility>\n"
                + "</Folder>n";
        
        kml += "<Folder>\n"
                + "<name>Tracks</name>\n"
                + "<Placemark>\n"
                + "<name>Ruta segura</name>\n"
                //+ "<description><![CDATA[]]></description>\n"
                + "<styleUrl>#multiTrack</styleUrl>\n"
                + "<gx:Track>\n"; //+ "<when>2015-02-04T13:23:07Z</when>\n"

        kml += "</gx:Track>\n"
                + "</Placemark>\n"
                + "<open>0</open>\n"
                + "<visibility>1</visibility>\n"
                + "</Folder>";
        
        kml += "<Folder>\n"
                + "<name>Paths</name>\n"
                + "<Placemark>\n"
                + "<name>Ruta segura</name>\n"
                //+ "<description><![CDATA[]]></description>\n"
                + "<MultiGeometry>\n"
                + "<LineString>\n"
                + "<altitudeMode>clampToGround</altitudeMode>\n"
                + "<coordinates>\n";
        for (int i = 0; i < listaPuntos.size(); i++) {
            kml += "" + listaPuntos.getLon(i) + "," + listaPuntos.getLat(i) + ",0\n";
        }
        
        kml += "</coordinates>\n"
                + "<tessellate>1</tessellate>\n"
                + "</LineString>\n"
                + "</MultiGeometry>\n"
                //Estilo de la línea de la ruta
                + "<Style><LineStyle><color>FF00FF00</color><width>4</width></LineStyle></Style>\n"
                + "</Placemark>\n"
                + "<open>0</open>\n"
                + "<visibility>1</visibility>\n"
                + "</Folder>\n"
                + "<open>1</open>\n"
                + "<visibility>1</visibility>\n"
                + " </Document>\n"
                + "</kml>";
        
        return kml;
    }

    /**
     * Guarda una ruta en la bd y devuelve el id para recuperarla.
     *
     * @param rutaTexto
     * @param instrucciones
     * @return
     */
    public int guardarRutaBD(String rutaTexto, String instrucciones) {
        return bd.guardarRuta(rutaTexto, instrucciones);
    }

    /**
     * Devuelve la información de una ruta
     *
     * @param id
     * @return Array con ruta, instrucciones, y fecha en millis
     */
    public String[] recuperarInfoRuta(int id) {
        return bd.recuperarRuta(id);
    }

    /*
     Arcos
     */
    /**
     * Devuelve el índice de seguridad para un arco.
     *
     * @param nArco Número de arco en GH.
     * @param usuario
     * @param tipoDeRuta Si es el índice para ruta a pie o en bici
     * @return Un número entre 0 y 1 representando el índice. Donde 0 es calle
     * segura.
     */
    public double getSeguridadArco(int nArco, int usuario, Byte tipoDeRuta) {
        double salida;
        //Si no hay información responde 1
        if (!arcosIndSeguridad.containsKey(nArco)) {
            salida = 1;
        } else {
            //Si está en la lista
            if (tipoDeRuta == RUTA_A_PIE) {
                salida = arcosIndSeguridad.get(nArco).getPrimero();
            } else {
                salida = arcosIndSeguridad.get(nArco).getSegundo();
            }
        }

        //Si está en las alertas con iconos
        if (arcosAlertasIcono.containsKey(nArco)) {
            salida = arcosAlertasIcono.get(nArco);
        }
        
        return salida;
    }

    /**
     * Actualiza los valores para las calles en el área dada.
     *
     * @param sup lat superior
     * @param inf
     * @param izq lon izquierda
     * @param der
     * @param valores Cadena con los valores separados por ;
     * @param idCreador
     * @param privado
     * @param elevar
     * @return
     */
    public String anadirInfoCalles(double sup, double inf, double izq, double der, String valores, int idCreador,
            int privado, int elevar) {
        //Números con los valores
        String[] trozos = valores.split(";");
        
        BBox caja = new BBox(izq, der, inf, sup);
        
        AllEdgesIterator listaArcos = grafo.getGraph().getAllEdges();
        
        while (listaArcos.next()) {
            //Ids
            int idArco = listaArcos.getEdge();
            int nodoA = listaArcos.getBaseNode();
            int nodoB = listaArcos.getAdjNode();

            //Lats y lons
            double aLt = grafo.getGraph().getNodeAccess().getLat(nodoA);
            double aLn = grafo.getGraph().getNodeAccess().getLon(nodoA);
            double bLt = grafo.getGraph().getNodeAccess().getLat(nodoB);
            double bLn = grafo.getGraph().getNodeAccess().getLon(nodoB);
            
            if (caja.contains(aLt, aLn) || caja.contains(bLt, bLn)) {
                Calle tempCalle;
                //Obtener calle antigua si la hubiera
                tempCalle = bd.getSeguridad(idArco);
                //Sino crear una calle desde cero
                if (tempCalle == null) {
                    tempCalle = new Calle();
                }
                tempCalle.id = idArco;
                tempCalle.fechaActualizacion = new Date(new GregorianCalendar().getTimeInMillis());
                //Actualizar los valores a la calle
                //Valor por valor
                int valor;

                //Id creador                
                tempCalle.idcreador = idCreador;

                //Privado
                tempCalle.privado = privado;

                //Elevar
                tempCalle.elevar = elevar;

                //Posición
                tempCalle.latitud1 = aLt;
                tempCalle.longitud1 = aLn;
                tempCalle.latitud2 = bLt;
                tempCalle.longitud2 = bLn;

                //Densidad tráfico
                valor = Integer.parseInt(trozos[0]);
                if (valor >= 0 && valor <= 10) {
                    tempCalle.densidadtrafico = valor;
                }

                //aceras
                valor = Integer.parseInt(trozos[1]);
                if (valor >= 0 && valor <= 10) {
                    tempCalle.acera = valor;
                }

                //ancho
                valor = Integer.parseInt(trozos[2]);
                if (valor >= 0 && valor <= 10) {
                    tempCalle.anchocalle = valor;
                }

                //criminalidad
                valor = Integer.parseInt(trozos[3]);
                if (valor >= 0 && valor <= 10) {
                    tempCalle.indicecriminalidad = valor;
                }

                //accidentes
                valor = Integer.parseInt(trozos[4]);
                if (valor >= 0 && valor <= 10) {
                    tempCalle.ratioaccidentes = valor;
                }

                //superficie
                valor = Integer.parseInt(trozos[5]);
                if (valor >= 0 && valor <= 1) {
                    tempCalle.superficieadoquines = valor;
                    tempCalle.superficielisa = (valor + 1) % 2;
                }

                //peatones
                valor = Integer.parseInt(trozos[6]);
                if (valor >= 0 && valor <= 10) {
                    tempCalle.ratioaccidentes = valor;
                }

                //semaforos
                valor = Integer.parseInt(trozos[7]);
                if (valor >= 0 && valor <= 10) {
                    tempCalle.semaforos = valor;
                }

                //residencial
                valor = Integer.parseInt(trozos[8]);
                if (valor >= 0 && valor <= 10) {
                    tempCalle.residencialcomercial = valor;
                }

                //fachadas
                valor = Integer.parseInt(trozos[9]);
                if (valor >= 0 && valor <= 10) {
                    tempCalle.conservacionedificios = valor;
                }

                //nivelsocioeconomico
                valor = Integer.parseInt(trozos[10]);
                if (valor >= 0 && valor <= 10) {
                    tempCalle.niveleconomico = valor;
                }

                //iluminación
                valor = Integer.parseInt(trozos[11]);
                if (valor >= 0 && valor <= 10) {
                    tempCalle.iluminacion = valor;
                }

                //velocidad
                valor = Integer.parseInt(trozos[12]);
                if (valor >= 0 && valor <= 130) {
                    tempCalle.velocidadmaxima = valor;
                }

                //peatonal
                valor = Integer.parseInt(trozos[13]);
                if (valor >= 0 && valor <= 1) {
                    tempCalle.callepeatonal = valor;
                }

                //bici
                valor = Integer.parseInt(trozos[14]);
                if (valor >= 0 && valor <= 1) {
                    tempCalle.carrilbici = valor;
                }

                //calidad carril
                valor = Integer.parseInt(trozos[15]);
                if (valor >= 0 && valor <= 10) {
                    tempCalle.calidadcarrilbici = valor;
                }

                //separación
                valor = Integer.parseInt(trozos[16]);
                if (valor >= 0 && valor <= 1) {
                    tempCalle.separacioncalzadaacera = valor;
                }

                //aparcamiento
                valor = Integer.parseInt(trozos[17]);
                if (valor >= 0 && valor <= 1) {
                    tempCalle.aparcamientoacera = valor;
                }

                //badenes
                valor = Integer.parseInt(trozos[18]);
                if (valor >= 0 && valor <= 1) {
                    tempCalle.badenes = valor;
                }

                //radar
                valor = Integer.parseInt(trozos[19]);
                if (valor >= 0 && valor <= 1) {
                    tempCalle.radar = valor;
                }

                //pendiente
                valor = Integer.parseInt(trozos[20]);
                if (valor >= 0 && valor <= 10) {
                    tempCalle.pendiente = valor;
                }

                //confort
                valor = Integer.parseInt(trozos[21]);
                if (valor >= 0 && valor <= 10) {
                    tempCalle.confort = valor;
                }

                //policia
                valor = Integer.parseInt(trozos[22]);
                if (valor >= 0 && valor <= 1) {
                    tempCalle.policía = valor;
                }

                //Calcular nuevos índices de seguridad
                nuevosValoresSeguridad(tempCalle);

                //Guardar calle en bd
                bd.setSeguridad(tempCalle);
            }//Fin si la calla está en la caja
        }//Fin bucle calles

        return "";
    }
    
    public String anadirInfoPunto(double lat, double lon, String valores, int idCreador, int privado, int elevar) {
        //Números con los valores
        String[] trozos = valores.split(";");

        //Calle más cercana
        int id = getArcoCercano(lat, lon);

        //Info de la calle
        AllEdgesIterator listaArcos = grafo.getGraph().getAllEdges();
        
        for (int i = 0; i < listaArcos.getCount() && (listaArcos.getEdge() != id); i++) {
            listaArcos.next();
        }
        
        int nodoA = listaArcos.getBaseNode();
        int nodoB = listaArcos.getAdjNode();

        //Lats y lons
        double aLt = grafo.getGraph().getNodeAccess().getLat(nodoA);
        double aLn = grafo.getGraph().getNodeAccess().getLon(nodoA);
        double bLt = grafo.getGraph().getNodeAccess().getLat(nodoB);
        double bLn = grafo.getGraph().getNodeAccess().getLon(nodoB);
        
        Calle tempCalle;
        //Obtener calle antigua si la hubiera
        tempCalle = bd.getSeguridad(id);
        //Sino crear una calle desde cero
        if (tempCalle == null) {
            tempCalle = new Calle();
        }

        //Desglosar los valores
        tempCalle.id = id;
        tempCalle.fechaActualizacion = new Date(new GregorianCalendar().getTimeInMillis());
        //Actualizar los valores a la calle
        //Valor por valor
        int valor;

        //Id creador                
        tempCalle.idcreador = idCreador;

        //Privado
        tempCalle.privado = privado;

        //Elevar
        tempCalle.elevar = elevar;

        //Posición
        tempCalle.latitud1 = aLt;
        tempCalle.longitud1 = aLn;
        tempCalle.latitud2 = bLt;
        tempCalle.longitud2 = bLn;

        //Densidad tráfico
        valor = Integer.parseInt(trozos[0]);
        if (valor >= 0 && valor <= 10) {
            tempCalle.densidadtrafico = valor;
        }

        //aceras
        valor = Integer.parseInt(trozos[1]);
        if (valor >= 0 && valor <= 10) {
            tempCalle.acera = valor;
        }

        //ancho
        valor = Integer.parseInt(trozos[2]);
        if (valor >= 0 && valor <= 10) {
            tempCalle.anchocalle = valor;
        }

        //criminalidad
        valor = Integer.parseInt(trozos[3]);
        if (valor >= 0 && valor <= 10) {
            tempCalle.indicecriminalidad = valor;
        }

        //accidentes
        valor = Integer.parseInt(trozos[4]);
        if (valor >= 0 && valor <= 10) {
            tempCalle.ratioaccidentes = valor;
        }

        //superficie
        valor = Integer.parseInt(trozos[5]);
        if (valor >= 0 && valor <= 1) {
            tempCalle.superficieadoquines = valor;
            tempCalle.superficielisa = (valor + 1) % 2;
        }

        //peatones
        valor = Integer.parseInt(trozos[6]);
        if (valor >= 0 && valor <= 10) {
            tempCalle.ratioaccidentes = valor;
        }

        //semaforos
        valor = Integer.parseInt(trozos[7]);
        if (valor >= 0 && valor <= 10) {
            tempCalle.semaforos = valor;
        }

        //residencial
        valor = Integer.parseInt(trozos[8]);
        if (valor >= 0 && valor <= 10) {
            tempCalle.residencialcomercial = valor;
        }

        //fachadas
        valor = Integer.parseInt(trozos[9]);
        if (valor >= 0 && valor <= 10) {
            tempCalle.conservacionedificios = valor;
        }

        //nivelsocioeconomico
        valor = Integer.parseInt(trozos[10]);
        if (valor >= 0 && valor <= 10) {
            tempCalle.niveleconomico = valor;
        }

        //iluminación
        valor = Integer.parseInt(trozos[11]);
        if (valor >= 0 && valor <= 10) {
            tempCalle.iluminacion = valor;
        }

        //velocidad
        valor = Integer.parseInt(trozos[12]);
        if (valor >= 0 && valor <= 130) {
            tempCalle.velocidadmaxima = valor;
        }

        //peatonal
        valor = Integer.parseInt(trozos[13]);
        if (valor >= 0 && valor <= 1) {
            tempCalle.callepeatonal = valor;
        }

        //bici
        valor = Integer.parseInt(trozos[14]);
        if (valor >= 0 && valor <= 1) {
            tempCalle.carrilbici = valor;
        }

        //calidad carril
        valor = Integer.parseInt(trozos[15]);
        if (valor >= 0 && valor <= 10) {
            tempCalle.calidadcarrilbici = valor;
        }

        //separación
        valor = Integer.parseInt(trozos[16]);
        if (valor >= 0 && valor <= 1) {
            tempCalle.separacioncalzadaacera = valor;
        }

        //aparcamiento
        valor = Integer.parseInt(trozos[17]);
        if (valor >= 0 && valor <= 1) {
            tempCalle.aparcamientoacera = valor;
        }

        //badenes
        valor = Integer.parseInt(trozos[18]);
        if (valor >= 0 && valor <= 1) {
            tempCalle.badenes = valor;
        }

        //radar
        valor = Integer.parseInt(trozos[19]);
        if (valor >= 0 && valor <= 1) {
            tempCalle.radar = valor;
        }

        //pendiente
        valor = Integer.parseInt(trozos[20]);
        if (valor >= 0 && valor <= 10) {
            tempCalle.pendiente = valor;
        }

        //confort
        valor = Integer.parseInt(trozos[21]);
        if (valor >= 0 && valor <= 10) {
            tempCalle.confort = valor;
        }

        //policia
        valor = Integer.parseInt(trozos[22]);
        if (valor >= 0 && valor <= 1) {
            tempCalle.policía = valor;
        }

        //Calcular nuevos índices de seguridad
        nuevosValoresSeguridad(tempCalle);

        //Guardar calle en bd
        bd.setSeguridad(tempCalle);
        
        return "";
    }

    /**
     * Calcula y guarda en memoria los valores de seguridad de una calle.
     *
     * @param calle
     */
    private void nuevosValoresSeguridad(Calle calle) {
        final double totalCaminar = valoresSeguridad.totalpuntosA;
        final double totalBici = valoresSeguridad.totalpuntosB;
        double valoracionCaminar = totalCaminar;
        double valoracionBici = totalBici;
        double valorPtoCaminar = 1.0 /*/ valoracionCaminar*/;
        double valorPtoBici = 1.0 /*/ valoracionBici*/;
        double puntosCaminar;
        double puntosBici;
        int criterio;
        //Valor por valor restando a las valoraciones
        //trafico
        criterio = calle.densidadtrafico;
        puntosCaminar = valoresSeguridad.densidadtraficoA;
        puntosBici = valoresSeguridad.densidadtraficoB;
        
        valoracionCaminar -= criterio * (puntosCaminar * valorPtoCaminar * 0.1);
        valoracionBici -= criterio * (puntosBici * valorPtoBici * 0.1);

        //acera
        criterio = calle.acera;
        puntosCaminar = valoresSeguridad.aceraA;
        puntosBici = valoresSeguridad.aceraB;
        
        valoracionCaminar -= criterio * (puntosCaminar * valorPtoCaminar * 0.1);
        valoracionBici -= criterio * (puntosBici * valorPtoBici * 0.1);

        //ancho calle
        criterio = calle.anchocalle;
        puntosCaminar = valoresSeguridad.anchocalleA;
        puntosBici = valoresSeguridad.anchocalleB;
        
        valoracionCaminar -= criterio * (puntosCaminar * valorPtoCaminar * 0.1);
        valoracionBici -= criterio * (puntosBici * valorPtoBici * 0.1);

        //criminalidad
        criterio = calle.indicecriminalidad;
        puntosCaminar = valoresSeguridad.criminalidadA;
        puntosBici = valoresSeguridad.criminalidadB;
        
        valoracionCaminar -= criterio * (puntosCaminar * valorPtoCaminar * 0.1);
        valoracionBici -= criterio * (puntosBici * valorPtoBici * 0.1);

        //ratio accidentes
        criterio = calle.ratioaccidentes;
        puntosCaminar = valoresSeguridad.ratioaccidentesA;
        puntosBici = valoresSeguridad.ratioaccidentesB;
        
        valoracionCaminar -= criterio * (puntosCaminar * valorPtoCaminar * 0.1);
        valoracionBici -= criterio * (puntosBici * valorPtoBici * 0.1);

        //superficie adoquines 0 1 
        criterio = calle.superficieadoquines;
        puntosCaminar = valoresSeguridad.superficieadoquinesA;
        puntosBici = valoresSeguridad.superficieadoquinesB;
        
        valoracionCaminar -= criterio * (puntosCaminar * valorPtoCaminar);
        valoracionBici -= criterio * (puntosBici * valorPtoBici * 0.1);

        //superficie lisa
        criterio = calle.superficielisa;
        puntosCaminar = valoresSeguridad.superficielisaA;
        puntosBici = valoresSeguridad.superficielisaB;
        
        valoracionCaminar -= criterio * (puntosCaminar * valorPtoCaminar * 0.1);
        valoracionBici -= criterio * (puntosBici * valorPtoBici * 0.1);

        //peatones
        criterio = calle.pasopeatones;
        puntosCaminar = valoresSeguridad.pasopeatonesA;
        puntosBici = valoresSeguridad.pasopeatonesB;
        
        valoracionCaminar -= criterio * (puntosCaminar * valorPtoCaminar * 0.1);
        valoracionBici -= criterio * (puntosBici * valorPtoBici * 0.1);

        //semaforos
        criterio = calle.semaforos;
        puntosCaminar = valoresSeguridad.semaforosA;
        puntosBici = valoresSeguridad.semaforosB;
        
        valoracionCaminar -= criterio * (puntosCaminar * valorPtoCaminar * 0.1);
        valoracionBici -= criterio * (puntosBici * valorPtoBici * 0.1);

        //residencial
        criterio = calle.residencialcomercial;
        puntosCaminar = valoresSeguridad.residencialA;
        puntosBici = valoresSeguridad.residencialB;
        
        valoracionCaminar -= criterio * (puntosCaminar * valorPtoCaminar * 0.1);
        valoracionBici -= criterio * (puntosBici * valorPtoBici * 0.1);

        //ancho calle
        criterio = calle.conservacionedificios;
        puntosCaminar = valoresSeguridad.conservacionedificiosA;
        puntosBici = valoresSeguridad.conservacionedificiosB;
        
        valoracionCaminar -= criterio * (puntosCaminar * valorPtoCaminar * 0.1);
        valoracionBici -= criterio * (puntosBici * valorPtoBici * 0.1);

        //Nivel barrio
        criterio = calle.niveleconomico;
        puntosCaminar = valoresSeguridad.niveleconomicoA;
        puntosBici = valoresSeguridad.niveleconomicoB;
        
        valoracionCaminar -= criterio * (puntosCaminar * valorPtoCaminar * 0.1);
        valoracionBici -= criterio * (puntosBici * valorPtoBici * 0.1);

        //iluminación
        criterio = calle.iluminacion;
        puntosCaminar = valoresSeguridad.iluminacionA;
        puntosBici = valoresSeguridad.iluminacionB;
        
        valoracionCaminar -= criterio * (puntosCaminar * valorPtoCaminar * 0.1);
        valoracionBici -= criterio * (puntosBici * valorPtoBici * 0.1);

        //callel peatonal
        criterio = calle.callepeatonal;
        puntosCaminar = valoresSeguridad.callepeatonalA;
        puntosBici = valoresSeguridad.callepeatonalB;
        
        valoracionCaminar -= criterio * (puntosCaminar * valorPtoCaminar);
        valoracionBici -= criterio * (puntosBici * valorPtoBici);

        //Carril bici
        criterio = calle.carrilbici;
        puntosCaminar = valoresSeguridad.carrilbiciA;
        puntosBici = valoresSeguridad.carrilbiciB;
        
        valoracionCaminar -= criterio * (puntosCaminar * valorPtoCaminar);
        valoracionBici -= criterio * (puntosBici * valorPtoBici);

        //calidad carril bici
        criterio = calle.calidadcarrilbici;
        puntosCaminar = valoresSeguridad.calidadcarrilA;
        puntosBici = valoresSeguridad.calidadcarrilB;
        
        if (calle.carrilbici == 0.0) {
            puntosBici = 0.0;
        }
        valoracionCaminar -= criterio * (puntosCaminar * valorPtoCaminar * 0.1);
        valoracionBici -= criterio * (puntosBici * valorPtoBici * 0.1);

        //separación
        criterio = calle.separacioncalzadaacera;
        puntosCaminar = valoresSeguridad.separacionaceraA;
        puntosBici = valoresSeguridad.separacionaceraB;
        
        valoracionCaminar -= criterio * (puntosCaminar * valorPtoCaminar);
        valoracionBici -= criterio * (puntosBici * valorPtoBici * 0.1);

        //aparcamiento acera
        criterio = calle.aparcamientoacera;
        puntosCaminar = valoresSeguridad.aparcamientoaceraA;
        puntosBici = valoresSeguridad.aparcamientoaceraB;
        
        valoracionCaminar -= criterio * (puntosCaminar * valorPtoCaminar);
        valoracionBici -= criterio * (puntosBici * valorPtoBici);

        //badenes
        criterio = calle.badenes;
        puntosCaminar = valoresSeguridad.badenesA;
        puntosBici = valoresSeguridad.badenesB;
        
        valoracionCaminar -= criterio * (puntosCaminar * valorPtoCaminar);
        valoracionBici -= criterio * (puntosBici * valorPtoBici);

        //radar
        criterio = calle.radar;
        puntosCaminar = valoresSeguridad.radarA;
        puntosBici = valoresSeguridad.radarB;
        
        valoracionCaminar -= criterio * (puntosCaminar * valorPtoCaminar);
        valoracionBici -= criterio * (puntosBici * valorPtoBici);

        //pendiente
        criterio = calle.pendiente;
        puntosCaminar = valoresSeguridad.pendienteA;
        puntosBici = valoresSeguridad.pendienteB;
        
        valoracionCaminar -= criterio * (puntosCaminar * valorPtoCaminar * 0.1);
        valoracionBici -= criterio * (puntosBici * valorPtoBici * 0.1);

        //confort
        criterio = calle.confort;
        puntosCaminar = valoresSeguridad.confortA;
        puntosBici = valoresSeguridad.confortB;
        
        valoracionCaminar -= criterio * (puntosCaminar * valorPtoCaminar * 0.1);
        valoracionBici -= criterio * (puntosBici * valorPtoBici * 0.1);

        //policia
        criterio = calle.policía;
        puntosCaminar = valoresSeguridad.policiaA;
        puntosBici = valoresSeguridad.policiaB;
        
        valoracionCaminar -= criterio * (puntosCaminar * valorPtoCaminar);
        valoracionBici -= criterio * (puntosBici * valorPtoBici);

        //Velocidad
        criterio = calle.velocidadmaxima;
        puntosCaminar = valoresSeguridad.velocidadA;
        puntosBici = valoresSeguridad.velocidadB;
        if (criterio >= 0 && criterio <= 10) {
            criterio = 10;
        } else if (criterio >= 11 && criterio <= 30) {
            criterio = 7;
        } else if (criterio >= 31 && criterio <= 40) {
            criterio = 5;
        } else if (criterio >= 41 && criterio <= 50) {
            criterio = 3;
        }
        if (criterio > 50) {
            valoracionCaminar = totalCaminar;
            valoracionBici = totalBici;
            criterio = 0;
        }
        
        valoracionCaminar -= criterio * (puntosCaminar * valorPtoCaminar * 0.1);
        valoracionBici -= criterio * (puntosBici * valorPtoBici * 0.1);
        if (criterio > 50) {
            valoracionCaminar = totalCaminar;
            valoracionBici = totalBici;
        }

        //Actualizar valor en memoria        
        setSeguridadMemoria(calle.id, valoracionCaminar / totalCaminar, valoracionBici / totalBici);
    }

    /**
     * Establece o modifica el valor de seguridad para un arco.
     *
     * @param idArco Id de GH del arco
     * @param indSegAPie Índice de seguridad a pie. Entre 0 y 1. 0 es el valor
     * más seguro.
     * @param indSegEnBici Índice de seguridad en bici.
     * @return Si se ha incluido o había un error en los datos.
     */
    public boolean setSeguridadMemoria(int idArco, double indSegAPie, double indSegEnBici) {
        //Comprobar los datos
        if (indSegAPie < 0 || indSegAPie > 1) {
            return false;
        }
        if (indSegEnBici < 0 || indSegEnBici > 1) {
            return false;
        }

        //Comprobar que el arco existe
        AllEdgesIterator iterador = grafo.getGraph().getAllEdges();
        boolean coincidencia = false;
        while (!coincidencia && iterador.next()) {
            if (idArco == iterador.getEdge()) {
                coincidencia = true;
            }
        }
        
        if (!coincidencia) {
            return false;
        }

        //Si está todo correcto
        arcosIndSeguridad.put(idArco, new Pareja(indSegAPie, indSegEnBici));
        return true;
    }

    /**
     * Devuelve el id del arco más cercano accesible para el tipo de ruta dado.
     *
     * @param lat1 Latitud del punto
     * @param lon1 Longitud del punto
     * @param tipoDeRuta Si es para una ruta a pie o en bici
     * @return
     */
    public int getArcoCercano(double lat1, double lon1, byte tipoDeRuta) {
        //Cargar índice
        LocationIndex index = grafo.getLocationIndex();

        //Busqueda con Indice
        QueryResult fromQR;
        if (tipoDeRuta == RUTA_A_BICI) {
            fromQR = index.findClosest(lat1, lon1, new DefaultEdgeFilter(new MountainBikeFlagEncoder()));
        } else {
            fromQR = index.findClosest(lat1, lon1, new DefaultEdgeFilter(new FootFlagEncoder()));
        }

        //Obtener id
        return fromQR.getClosestEdge().getEdge();
    }

    /**
     * Devuelve el arco más cercano a un punto.
     *
     * @param lat1
     * @param lon1
     * @return
     */
    public int getArcoCercano(double lat1, double lon1) {
        //Cargar índice
        LocationIndex index = grafo.getLocationIndex();

        //Busqueda con Indice
        QueryResult fromQR;
        
        fromQR = index.findClosest(lat1, lon1, EdgeFilter.ALL_EDGES);

        //Obtener id
        return fromQR.getClosestEdge().getEdge();
    }

    /**
     * Devuelve la información detallada de seguridad.
     *
     * @param id
     * @return
     */
    public Calle getSeguridadValoresArco(int id) {
        return bd.getSeguridad(id);
    }

    /**
     * Crear un icono y actualiza los valores de seguridad.
     *
     * @param idCreador
     * @param tipo
     * @param lat
     * @param lon
     * @param mensaje
     * @param guardarEnBd
     * @return
     */
    public String putAlertaIcono(int idCreador, int tipo, double lat, double lon, String mensaje, boolean guardarEnBd) {
        if (guardarEnBd) {
            //Incluir dato en bd
            bd.crearIcono(idCreador, mensaje, lat, lon, tipo);
        }

        //Obtener tramo más cercano sobre el que operar
        int idArco = getArcoCercano(lat, lon);

        //Incluir en lista de arcos con icono
        //Obtener valoración de seguridad
        if (tipo >= 0 && tipo <= 20) {
            //Alertas con poca seguridad
            arcosAlertasIcono.put(idArco, 1.0);
        } else if (tipo > 20 && tipo <= 100) {
            //Alertas que aumentan seguridad
            arcosAlertasIcono.put(idArco, 0.0);
        }
        
        return "" + idArco;
    }

    /**
     * Devuelve una lista con los iconos con alerta del mapa. id, tipo,
     * comentario, latitud, longitud
     *
     * @return
     */
    public List<String[]> getAlertasIcono() {
        return bd.listarIconos();
    }
    
    public boolean borrarIcono(int id) {
        //Borrar la alerta de la memoria
        List<String[]> listaIconos = getAlertasIcono();
        
        for (int i = 0; i < listaIconos.size(); i++) {
            if (Integer.parseInt(listaIconos.get(i)[0]) == id) {
                //Este es el icono y se borra                
                double lat = Double.parseDouble(listaIconos.get(i)[3]);
                double lon = Double.parseDouble(listaIconos.get(i)[4]);

                //Obtener el tramo
                int idArco = getArcoCercano(lat, lon);
                arcosAlertasIcono.remove(idArco);
                //Se acaba el bucle
                i=Integer.MAX_VALUE-1;
            }
        }

        //Borrar la alerta de la BD
        return bd.borrarIcono(id);
    }
    /*
     Usuarios
     */

    /**
     * Comprueba si el usuario y contraseña de un usuario es válida para
     * acceder.
     *
     * @param usuario
     * @param clave
     * @return -1 si no lo es, o el id de usuario
     */
    public String hacerLogin(String usuario, String clave) {
        if (bd.validarUsuario(usuario, clave)) {
            return "" + bd.buscarUsuario(usuario);
        }
        return "-1";
    }

    /**
     * Devuelve la información de una persona.
     *
     * @param id
     * @return
     */
    public Persona getInfoUsuario(int id) {
        return bd.getUsuario(id);
    }

    /**
     * Modifica la información de una persona
     *
     * @param p
     * @return -1 si no se realiza o el id del usuario.
     */
    public int modificarPersona(Persona p) {
        String clave = null;
        //Evita que se borre la clave
        if (p.clave != null) {
            clave = p.clave;
        }
        return bd.putUsuario(p.idUsuario, p.idCreador, p.idColegio, p.nombreUsuario,
                clave, p.email, p.nombreReal, p.nombreNino, p.sexoTutor, p.sexoNino,
                p.direccion, p.localidad, p.provincia, p.codigoPostal, p.lat, p.lon,
                p.parentesco, p.fnacPadre, p.fnacNino, p.naciolidadPadre, p.nacionalidadHijo,
                p.profesion, p.confirmado, p.alertaMail);
    }

    /**
     * Crea un usuario.
     *
     * @param email
     * @param clave
     * @param idCreador
     * @return
     */
    public int anadirUsuario(String email, String clave, int idCreador) {
        //Nombre usuario
        String nombreUsuario = email.substring(0, email.indexOf("@"));
        
        if (bd.buscarUsuario(email) >= 0) {
            return -1;
        }
        clave = bd.encriptarClave(clave);
        //Si el usuario no existe
        return bd.putUsuario(BaseDatos.VALOR_DESCONOCIDO, idCreador, BaseDatos.VALOR_DESCONOCIDO, nombreUsuario, clave,
                email, email, "", BaseDatos.VALOR_DESCONOCIDO, BaseDatos.VALOR_DESCONOCIDO,
                "", "", "", BaseDatos.VALOR_DESCONOCIDO, BaseDatos.VALOR_DESCONOCIDO,
                BaseDatos.VALOR_DESCONOCIDO, BaseDatos.VALOR_DESCONOCIDO, null, null, "",
                "", "", BaseDatos.VERDADERO, BaseDatos.VERDADERO);
        
    }

    /**
     * Devuelve una cadena de texto aleatorio del tamaño especificado.
     *
     * @param tamano
     * @return
     */
    public String generarClaveAleatoria(int tamano) {
        String cadenaAleatoria = "";
        Random r = new Random(new java.util.GregorianCalendar().getTimeInMillis());
        int i = 0;
        while (i < tamano) {
            char c = (char) r.nextInt(255);
            if ((c >= '!' && c <= '@') || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
                cadenaAleatoria += c;
                i++;
            }
        }
        return cadenaAleatoria;
    }

    /**
     * Para un usuario dado por su email, le cambia la contraseña.
     *
     * @param email
     * @param nuevaClave
     * @return
     */
    public String cambiarClave(String email, String nuevaClave) {
        int id = bd.buscarUsuario(email);
        if (id < 0) {
            return "-1";
        }
        
        if (!bd.esUsuarioActivo(id)) {
            return "-1";
        }
        
        Persona p = bd.getUsuario(id);
        
        return "" + bd.putUsuario(id, p.idCreador, p.idColegio, p.nombreUsuario, nuevaClave,
                p.email, p.nombreReal, p.nombreNino, p.sexoTutor, p.sexoNino, p.direccion, p.localidad, p.provincia,
                p.codigoPostal, p.lat, p.lon, p.parentesco, p.fnacPadre, p.fnacNino, p.naciolidadPadre, p.nacionalidadHijo,
                p.profesion, p.confirmado, p.alertaMail);
    }

    /**
     * Para un administrador, indica los email de los usuarios que penden de él.
     *
     * @param idAdmin
     * @return Lista de emails.
     */
    public List<String> enviarEmailUsuariosAdmin(int idAdmin) {
        List<Integer> listaIds = bd.getListaUsuariosDeUnAdmin(idAdmin);
        List<String> emails = new ArrayList();
        for (int temp : listaIds) {
            String email = bd.getUsuario(temp).email;
            if (email != null) {
                emails.add(email);
            }
        }
        return emails;
    }

    /**
     * Devuelve una lista con los usuarios de un administrador.
     *
     * @param id
     * @return
     */
    public List<Integer> listarUsuariosDeAdmin(int id) {
        return bd.getListaUsuariosDeUnAdmin(id);
    }

    /**
     * Indica si un usuario está activado.
     *
     * @param id
     * @return
     */
    public boolean esActivoUsuario(int id) {
        return bd.esUsuarioActivo(id);
    }

    /**
     * Elimina los datos personales de un usuario del sistema.
     *
     * @param id
     * @return
     */
    public int borrarUsuario(int id) {
        int salida;

        //Borrar
        salida = bd.desactivarUsuario(id) ? 1 : -1;
        
        return salida;
    }

    /**
     * Busca el usuario en la base de datos
     *
     * @param email
     * @return -1 si no lo encuentra, id del usuario si existe.
     */
    public int buscarUsuario(String email) {
        return bd.buscarUsuario(email);
    }

    /*
     Administradores
     */
    /**
     * Indica si un usuario es administrador.
     *
     * @param id
     * @return
     */
    public boolean esAdministrador(int id) {
        return bd.esAdministrador(id);
    }

    /**
     * Nombra a un usuario administrador
     *
     * @param id
     * @return
     */
    public String setAdministrador(int id) {
        return bd.setAdministrador(id) ? "" + id : "-1";
    }

    /*
     Alertas en anuncio
     */
    /**
     * Incluye la alerta en la base de datos.
     *
     * @param idCreador
     * @param titulo
     * @param texto
     * @return
     */
    public String putAlertaTexto(int idCreador, String titulo, String texto) {
        if (mapa.esAdministrador(idCreador)) {
            //Tiene permiso
            return "" + bd.crearAlerta(idCreador, titulo, texto);
        } else {
            return "-1";
        }
    }

    /**
     * Devuelve la lista de alertas de texto activas. id, idcreador, titulo,
     * texto, fechacreacion
     *
     * @return
     */
    public List<String[]> getAlertasTexto() {
        return bd.getAlertas();
    }

    /**
     * Borra una noticia.
     *
     * @param id
     * @return
     */
    public boolean borrarAlerta(int id) {
        return bd.borrarAlerta(id);
    }

    /*
     Foro
     */
    /**
     * Devuelve una lista con los mensajes principales del foro.
     *
     * @return
     */
    public List<String[]> getMensajes() {
        return bd.getForoMensajes();
    }

    /**
     * Devuelve la lista de mensajes para un hilo determinado.
     *
     * @param idPadre
     * @return
     */
    public List<String[]> getMensaje(int idPadre) {
        return bd.getForoMensaje(idPadre);
    }

    /**
     * Crea un hilo o le agrega un mensaje
     *
     * @param idPadre -1 para un mensaje principal o el id del padre
     * @param idAutor
     * @param titulo
     * @param mensaje
     * @return
     */
    public int putMensaje(int idPadre, int idAutor, String titulo, String mensaje) {
        return bd.crearForoMensaje(idPadre, idAutor, titulo, mensaje);
    }

    /**
     * Borra un mensaje de un hilo o el propio hilo.
     *
     * @param idMensaje
     * @return
     */
    public boolean borrarMensaje(int idMensaje) {
        return bd.borrarForoMensaje(idMensaje);
    }
}
