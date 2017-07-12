package compiler;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lef on 22/6/2017.
 */
public class BasicBlock {
    protected List<Quad> block;
    protected boolean In_Edge;
    protected Integer Out_Edge;

    public BasicBlock(List<Quad> q,boolean in,Integer out){
        this.block = q;
        this.In_Edge = in;
        this.Out_Edge = out;
    }
}
