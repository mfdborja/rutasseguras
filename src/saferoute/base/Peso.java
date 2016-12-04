package saferoute.base;

import com.graphhopper.routing.util.Weighting;
import com.graphhopper.util.EdgeIteratorState;

/**
 * Calcula el peso de los tramos de forma personalizada.
 */
public class Peso implements Weighting {
    /**
     * Indica el multiplicador de la estimación para A*
     */
    private double indExpansion = 1.50;
    /**
     * Indica cuanto pesa la distancia vs seguridad. Mide el peso con la
     * seguridad. Entre 0 y 1.
     */
    private double importanciaSeguridad;

    /**
     * Tipo de ruta para el que se ejecuta el peso
     */
    private final byte tipoDeRuta;
    /**
     * Usuario para comprobar ruta
     */
    private final int usuario;
    /**
     * Representa al mapa.
     */
    private final Mapa miMapa;

    /**
     * Inicializa una instancia del medidor de pesos.
     *
     * @param tipoDeRuta Indica si la ruta es a pie o en bici.
     * @param usuario Usuario que consulta 
     * @param miMapa Mapa con la información de seguridad personalizada.
     */
    public Peso(byte tipoDeRuta, int usuario, Mapa miMapa) {
        this.importanciaSeguridad = 0.5;
        this.miMapa = miMapa;
        this.tipoDeRuta = tipoDeRuta;
        this.usuario=usuario;
    }

    /**
     * Used only for the heuristical estimation in A
     * <p/>
     * @return minimal weight. E.g. if you calculate the fastest way it is
     * distance/maxVelocity
     */
    @Override
    public double getMinWeight(double distance) {
        return distance * indExpansion;
    }

    /**
     * @param edge the edge for which the weight should be calculated
     * @param reverse if the specified edge is specified in reverse direction
     * e.g. from the reverse case of a bidirectional search.
     * @return the calculated weight with the specified velocity has to be in
     * the range of 0 and +Infinity. Make sure your method does not return NaN
     * which can e.g. occur for 0/0.
     */
    @Override
    public double calcWeight(EdgeIteratorState edgeState, boolean reverse, int prevOrNextEdgeId) {
        //Id del arco
        int idArco = edgeState.getEdge();
        //Tamaño del arco
        double distancia = edgeState.getDistance();

        //Cálculo de seguridad
        double indiceSeguridad;
        if (tipoDeRuta == Mapa.RUTA_A_PIE) {
            indiceSeguridad = miMapa.getSeguridadArco(idArco, usuario, Mapa.RUTA_A_PIE);
        } else {
            indiceSeguridad = miMapa.getSeguridadArco(idArco, usuario, Mapa.RUTA_A_BICI);
        }

        //Si no hay información aún en la base de datos se le da un valor intermedio
        if (Double.isNaN(indiceSeguridad)) {
            indiceSeguridad = 0.5;
        }
        
        //Cálculo
        double calculo = (distancia * (1 - importanciaSeguridad))
                + ((indiceSeguridad * distancia) * importanciaSeguridad);
        return calculo;
    }

    /**
     * Cambia el valor para la estimación de la distancia por calcular en A*. La
     * distancia euclidea entre este punto y el final se multiplica con este
     * valor. Debe ser mayor o igual que 1.
     *
     * @param nuevoIndice
     */
    public void cambiarIndiceExpasion(double nuevoIndice) {
        if (nuevoIndice >= 1) {
            indExpansion = nuevoIndice;
        }
    }

    /**
     * Indica el peso de la seguridad en un valor entre 0 y 1. 0 indica que solo
     * importa la distancia (camino mínimo), 1 que solo importa la seguridad.
     *
     * @param impSeguridad
     */
    public void cambiarImportanciaSeguridad(double impSeguridad) {
        if (importanciaSeguridad >= 0 && importanciaSeguridad <= 1) {
            this.importanciaSeguridad = impSeguridad;
        }
    }

}
