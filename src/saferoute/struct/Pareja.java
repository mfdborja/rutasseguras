package saferoute.struct;

public class Pareja<PrimerObjeto, SegundoObjeto> {
    /**
     * Primero de la pareja de objetos.
     */
    private PrimerObjeto primero;
    
    /**
     * Segundo de la pareja de objetos.
     */
    private SegundoObjeto segundo;

    /**
     * Crea el par con los objetos interiores vacíos.
     */
    public Pareja() {
    }

    /**
     * Crea el par con los objetos que se le pasen.
     * @param primero Objeto para la primera posición
     * @param segundo Objeto para la segunda posición
     */
    public Pareja(PrimerObjeto primero, SegundoObjeto segundo) {
        this.primero = primero;
        this.segundo = segundo;
    }

    /**
     * Modifica el primer objeto de la pareja.
     * @param primero Objeto que se quiere incluir
     */
    public void setPrimero(PrimerObjeto primero) {
        this.primero = primero;
    }

    /**
     * modifica el segundo objeto de la pareja.
     * @param segundo Objeto que se quiere incluir
     */
    public void setSegundo(SegundoObjeto segundo) {
        this.segundo = segundo;
    }

    /**
     * Devuelve el primer objeto de la pareja.
     * @return Objeto en la primera posición
     */
    public PrimerObjeto getPrimero() {
        return this.primero;
    }
    
    /**
     * Devuelve el segundo objeto de la pareja.
     * @return Objeto en la segunda posición
     */
    public SegundoObjeto getSegundo() {
        return this.segundo;
    }
}
