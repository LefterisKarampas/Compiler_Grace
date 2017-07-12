package compiler;

import compiler.node.PFunInfo;
import compiler.node.TVariable;
/**
 * Created by lef on 6/5/2017.
 */
import java.util.ArrayList;
import java.util.List;

public class Function extends Type {
    protected String return_type;
    protected int num_of_args = 0;
    protected int flag =0;
    protected int param_bytes;
    protected List<Variable> args;

    public Function(List<Variable> fun_info,String ret_type,int flag,int bytes){
        this.return_type = ret_type;
        this.args = fun_info;
        this.flag = flag;
        this.param_bytes = bytes;
        this.num_of_args = fun_info.size();
    }
}

