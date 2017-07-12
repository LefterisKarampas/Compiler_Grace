package compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by lef on 18/5/2017.
 */
public class Info_Table {
    protected int bytes;
    protected int var_bytes;
    protected int par_bytes;
    HashMap<String,Type> Hash;
    protected int nesting;
    List<String> undef_functions;

    public Info_Table(int nest){
        this.bytes = 0;
        this.var_bytes = 0;
        this.par_bytes = 0;
        Hash = new HashMap<String, Type>();
        this.nesting = nest;
        undef_functions = new ArrayList<String>();
    }

    public Info_Table(int bytes,HashMap<String,Type> hash){
        this.bytes = bytes;
        this.par_bytes = bytes;
        this.var_bytes = 0;
        Hash = hash;
        undef_functions = new ArrayList<String>();
    }
}
