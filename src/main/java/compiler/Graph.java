package compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by lef on 24/6/2017.
 */
public class Graph {
    protected HashMap<Integer,Integer> QuadToBlock;
    protected List<BasicBlock> Blocks;

    public Graph(HashMap<Integer,Integer> x, List<BasicBlock> y){
        this.QuadToBlock = x;
        this.Blocks = y;
    }
}
