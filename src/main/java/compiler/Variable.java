package compiler;

import compiler.node.TNumber;

import java.util.ArrayList;
import java.util.List;
import  compiler.Type.*;
/**
 * Created by lef on 6/5/2017.
 */

public class Variable extends Type{
    protected String type;
    protected boolean ref;
    protected int Dimension;
    protected int position;
    protected List<Integer> dim;

    public Variable(String type, Boolean ref, int x,int pos,List<Integer> dim){
        super();
        this.type = type;
        this.ref = ref;
        this.dim = dim;
        this.Dimension = x;
        this.position = pos;
    }
}
