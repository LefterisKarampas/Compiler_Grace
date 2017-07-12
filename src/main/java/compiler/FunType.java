package compiler;

/**
 * Created by lef on 12/5/2017.
 */
public class FunType {
    protected String type;
    protected String function_name;
    protected boolean flag;

    public FunType(String funname,String type){
        this.function_name = funname;
        this.type = type;
        this.flag = false;
    }
}
