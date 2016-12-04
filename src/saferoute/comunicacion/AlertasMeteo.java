package saferoute.comunicacion;

import java.io.InputStream;
import java.net.URL;
import java.util.GregorianCalendar;
import java.util.List;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import saferoute.bd.BaseDatos;
import saferoute.bd.Propiedades;
import saferoute.log.LogSR;

/**
 * Servicio automático de alertas meteorológicas.
 * @author usuario
 */
public class AlertasMeteo extends Thread {

    private final BaseDatos miBD;

    public AlertasMeteo(BaseDatos bd) {
        this.miBD = bd;
    }

    @Override
    public void run() {
        GregorianCalendar ultimaActualizacion = new GregorianCalendar();
        //Ayer
        ultimaActualizacion.add(GregorianCalendar.HOUR_OF_DAY, -24);
        //A las dos de la tarde
        ultimaActualizacion.set(GregorianCalendar.HOUR_OF_DAY, 14);

        //Para almacenar el momento actual
        GregorianCalendar ahora;
        //Para hacer cálculos
        GregorianCalendar temp;

        while (true) {
            ahora = new GregorianCalendar();
            temp = new GregorianCalendar();
            temp.setTime(ultimaActualizacion.getTime());
            temp.add(GregorianCalendar.HOUR_OF_DAY, 24);
            //Temp = Ultima actualización más un día

            /*LogSR.mensaje("Cron tiempo: \n"
             + "AlertasMeteo actual:"
             +ahora.toString()+"\n"
             + "AlertasMeteo últ. a:"
             + ultimaActualizacion.toString()+"\n"
             + "T. ult. +1 d.:"
             + temp.toString()+"\n", LogSR.INFO);*/
            //Si la última actualización más un día ha pasado...
            if (ahora.after(temp)) {
                //Si este momento es posterior a ayer mas 24 horas
                try {
                    LogSR.mensaje("Descargando y analizando el tiempo.", LogSR.INFO);
                    boolean lanzarAlerta = descargarXML(Integer.parseInt(Propiedades.getPropiedades().leerPropiedad(Propiedades.tiempodesfaseprevision)), Integer.parseInt(Propiedades.getPropiedades().leerPropiedad(Propiedades.tiempoprobabilidadminima)));
                    //Actualiza la hora de última actualización y envia las alertas
                    ultimaActualizacion.setTime(temp.getTime());

                    /*Comprobar si es fiesta*/
                    //Comprobar que es mañana
                    temp.add(GregorianCalendar.HOUR_OF_DAY, 24);
                    //No se envían los viernes o los sábados
                    if (temp.get(GregorianCalendar.DAY_OF_WEEK) == GregorianCalendar.FRIDAY || temp.get(GregorianCalendar.DAY_OF_WEEK) == GregorianCalendar.SATURDAY) {
                        lanzarAlerta = false;
                    }

                    //Fiestas por meses
                    switch (temp.get(GregorianCalendar.MONTH)) {
                        case GregorianCalendar.JANUARY:{
                            //Navidad
                            if(temp.get(GregorianCalendar.DAY_OF_MONTH)<7){
                                lanzarAlerta=false;
                            }
                        }break;
                        case GregorianCalendar.FEBRUARY:{
                            //Día de andalucía
                            if(temp.get(GregorianCalendar.DAY_OF_MONTH)==28){
                                lanzarAlerta=false;
                            }
                        }break;
                        case GregorianCalendar.MARCH:{
                            
                        }break;
                        case GregorianCalendar.APRIL:{
                            //Día del trabajo
                            
                        }break;
                        case GregorianCalendar.MAY:{
                            if(temp.get(GregorianCalendar.DAY_OF_MONTH)==1){
                                lanzarAlerta=false;
                            }
                        }break;
                        case GregorianCalendar.JUNE:{
                            if(temp.get(GregorianCalendar.DAY_OF_MONTH)>=15){
                                lanzarAlerta=false;
                            }
                        }break;
                        case GregorianCalendar.JULY:{
                            lanzarAlerta=false;
                        }break;
                        case GregorianCalendar.AUGUST:{
                            lanzarAlerta=false;
                        }break;
                        case GregorianCalendar.SEPTEMBER:{
                            if(temp.get(GregorianCalendar.DAY_OF_MONTH)<=15){
                                lanzarAlerta=false;
                            }
                        }break;
                        case GregorianCalendar.OCTOBER:{
                            
                        }break;
                        case GregorianCalendar.NOVEMBER:{
                            
                        }break;
                        case GregorianCalendar.DECEMBER:{
                            if(temp.get(GregorianCalendar.DAY_OF_MONTH)==6){
                                lanzarAlerta=false;
                            }else if(temp.get(GregorianCalendar.DAY_OF_MONTH)==8){
                                lanzarAlerta=false;
                            }else if(temp.get(GregorianCalendar.DAY_OF_MONTH)>=22){
                                lanzarAlerta=false;
                            }
                        }break;
                    }

                    if (lanzarAlerta) {

                        //Sacar lista destinatarios
                        List<String> listaUsuarios = miBD.usuariosAlertasEmail();

                        ServidorEmailUgr servidorEmail = new ServidorEmailUgr();
                        //Cargar cabecera
                        String textoAlertaMeteo = servidorEmail.leerFicheroTexto(Propiedades.getPropiedades().leerPropiedad(Propiedades.mensajetiempo));
                        //CargarPie
                        String textoPie = servidorEmail.leerFicheroTexto(Propiedades.getPropiedades().leerPropiedad(Propiedades.mensajepie));

                        String mensaje = textoAlertaMeteo + textoPie;

                        //Mandar email
                        LogSR.mensaje("Enviando mails con alertas de tiempo.", LogSR.INFO);
                        servidorEmail.enviarEmail(listaUsuarios, "Alerta meteorológica - Rutas seguras", mensaje);
                        LogSR.mensaje("Se han enviado los emails de alerta meteorológica.", LogSR.INFO);
                    }

                } catch (Exception ex) {
                    LogSR.mensaje("Error en el envío del cron de emails. " + ex.getMessage(), LogSR.ERROR);
                }
            }
            try {
                this.sleep(1000 * 60 * 60);
            } catch (InterruptedException ex) {
                LogSR.mensaje("Error en el cron de emails. " + ex.getMessage(), LogSR.ERROR);
            }
        }
    }

    /**
     * Lee el archivo xml online y obtiene si hay alertas para un día posterior.
     *
     * @param direccionXML Url del archivo aemet
     * @param dentroDeXDias Días contando desde hoy para comprobar 0 es hoy, 1
     * es mañana, 2 es pasado
     * @param probPrecipitacionMin Probabilidad de precipitación mínima para que
     * salte la alerta a lo largo del día.
     * @return true si se cumple la condición, 0 si no hay alerta.
     */
    public boolean descargarXML(int dentroDeXDias, int probPrecipitacionMin) {
        boolean correcto = false;
        int maxPrecipitacion = 0;
        try {
            URL url = new URL(Propiedades.getPropiedades().leerPropiedad(Propiedades.tiempourlxml));
            InputStream flujoEntrada = url.openStream();
            SAXBuilder flujoEntradaXML = new SAXBuilder();
            Document xmlLeido = flujoEntradaXML.build(flujoEntrada);

            //Leer el xml
            //Nodo raÃ­z
            Element nodoRaiz = xmlLeido.getRootElement();

            //Veo origen y predicción
            //Accedo a petición
            Element prediccion = nodoRaiz.getChild("prediccion");

            //Ahora veo los días con la predicción
            List<Element> dias = prediccion.getChildren();

            if (dias.size() >= (dentroDeXDias + 1)) {
                //Comprobar que el día está en la lista
                //La lista comienza por el día de hoy

                //Posicionarse en el día seleccionado
                Element diaInteresante = dias.get(dentroDeXDias);

                //Obtener la probabilidad de precipitación para 
                //todos los tramos horarios del día y ver si supera el minimo
                List<Element> probPrecipitaciones = diaInteresante.getChildren("prob_precipitacion");

                for (Element temp : probPrecipitaciones) {
                    if (maxPrecipitacion < (Integer.parseInt(temp.getValue()))) {
                        maxPrecipitacion = Integer.parseInt(temp.getValue());
                    }
                }
            }
            correcto = true;
        } catch (Exception ex) {
            LogSR.mensaje("Se ha producido un error al leer la información del tiempo. " + ex.getMessage(), LogSR.ERROR);
        }

        //Si se cumple la condición, salta la alerta.
        return maxPrecipitacion >= probPrecipitacionMin && correcto;
    }

}
