package saferoute.struct;

import com.graphhopper.util.Instruction;

/**
 * Soporte léxico para usuarios sobre una instrucción del camino.
 */
public class Instruccion {

    private final String nombreCalle;
    private final int giro;
    private final double distancia;

    /**
     * Crea una instancia de la instrucción.
     *
     * @param instruction
     */
    public Instruccion(Instruction instruction) {
        this.nombreCalle = instruction.getName();
        this.giro = instruction.getSign();
        this.distancia = instruction.getDistance();
    }

    /**
     * Nombre de la calle.
     *
     * @return
     */
    public String getNombre() {
        return nombreCalle;
    }

    /**
     * Texto con la información de giro. 'Gire a la derecha y continúe' por calle ...
     *
     * @return
     */
    public String getGiro() {
        switch (giro) {
            //Recto
            case Instruction.CONTINUE_ON_STREET: {
                return "Continúe recto";
            }

            //Giros izquierda
            case Instruction.TURN_SLIGHT_LEFT: {

            }
            case Instruction.TURN_SHARP_LEFT: {

            }
            case Instruction.TURN_LEFT: {
                return "Gire a la izquierda y continúe";
            }

            //Giros derecha
            case Instruction.TURN_SLIGHT_RIGHT: {

            }
            case Instruction.TURN_SHARP_RIGHT: {

            }
            case Instruction.TURN_RIGHT: {
                return "Gire a la derecha y continúe";
            }

            //Final del trayecto
            case Instruction.FINISH: {
                return "Llegada a destino";
            }

            //Otros
            case Instruction.REACHED_VIA: {
                return "Cruce"; //A través de.
            }
        }

        return "";
    }

    /**
     * Distancia en metros de la instrucción.
     *
     * @return
     */
    public int getDistancia() {
        return (int)distancia;
    }
    
    @Override
    public String toString(){
        //Fin de ruta
        if(giro==Instruction.FINISH){
            return "Final de la ruta";
        }
        
        //Instrucción
        String salida="";
       salida+= getGiro();
       
       //Si tiene el nombre de la calle
       if(!getNombre().isEmpty())
       salida+=" por "+getNombre();
       
       salida+=" durante "+getDistancia()+" metros.";
       
       return salida;
    }
}
