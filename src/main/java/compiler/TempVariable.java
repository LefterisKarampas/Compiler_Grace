package compiler;

/**
 * Created by lef on 19/5/2017.
 */
public class TempVariable {
    protected int position;
    protected String type;
    protected boolean ref;

    public TempVariable(String type,int pos,boolean ref){
        this.type = type;
        this.position = pos;
        this.ref = ref;
    }
}
