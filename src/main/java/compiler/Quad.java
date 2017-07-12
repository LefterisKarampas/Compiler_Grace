package compiler;

/**
 * Created by lef on 14/5/2017.
 */
public class Quad {
    protected int id;
    protected String op;
    protected String r1;
    protected String r2;
    protected String destination;

    public Quad(int id,String op,String r1,String r2,String destination){
        this.id = id;
        this.op = op;
        this.r1 = r1;
        this.r2= r2;
        this.destination = destination;
    }
}
