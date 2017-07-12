package compiler;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lef on 17/5/2017.
 */
public class QuadInfo {
    protected String place;
    protected List<Integer> True;
    protected List<Integer> False;
    protected List<Integer> Next;

    public QuadInfo(){
        True = new ArrayList<Integer>();
        False = new ArrayList<Integer>();
        Next = new ArrayList<Integer>();
        place = "";
    }
}
