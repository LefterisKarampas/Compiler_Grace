package compiler;

import java.util.List;

/**
 * Created by lef on 12/5/2017.
 */
public class DataType {
    protected int dimension;
    protected String type;
    protected List<Variable> k;
    protected int nesting;
    protected int position;
    protected List<Integer> dim;
    protected boolean ref;

    public DataType(String x,int y,int nest,int position,boolean ref){
        this.type = x;
        this.dimension = y;
        this.nesting = nest;
        this.position = position;
        this.ref = ref;
    }

    public DataType(String x,int y,int nest,int position,boolean ref,List<Integer> dim){
        this.type = x;
        this.dimension = y;
        this.nesting = nest;
        this.dim = dim;
        this.position = position;
        this.ref = ref;
    }

    public DataType(String x,int y,boolean ref){
        this.type = x;
        this.dimension = y;
        this.ref = ref;
    }

    public DataType(String x,int y,boolean ref,List<Integer> dim){
        this.type = x;
        this.dimension = y;
        this.ref = ref;
        this.dim = dim;
    }

    public DataType(String ret_type){
        this.type = ret_type;
    }

    public DataType(String x,int y,List<Variable> k,int nest,int position){
        this.type = x;
        this.dimension = y;
        this.k = k;
        this.nesting = nest;
        this.position = position;
    }
}
