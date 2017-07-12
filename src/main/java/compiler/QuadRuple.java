package compiler;

import javax.swing.plaf.ListUI;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created by lef on 14/5/2017.
 */
public class QuadRuple {
    protected  List<Quad> list;
    protected HashMap<String,TempVariable> TmpVariable;
    protected int count_var;
    protected Queue<StringLabel> String_Labels;
    protected int String_count;
    protected HashMap<Integer,List<Integer>> In;
    protected HashMap<Integer,List<Integer>> Out;

    public QuadRuple(){
        this.list = new ArrayList<Quad>();
        this.TmpVariable = new HashMap<String, TempVariable>();
        this.count_var = 0;
        this.String_count = 0;
        this.String_Labels = new LinkedList<StringLabel>();
        this.In = new HashMap<Integer, List<Integer>>();
        this.Out = new HashMap<Integer, List<Integer>>();
        PrintWriter out = null;
        try {
            out = new PrintWriter(new BufferedWriter(new FileWriter(
                    "../../Quad",false)));
        }catch (IOException e) {
            System.err.println(e);
        }
        finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public int NextQuad(){
        return this.list.size();
    }

    public void GenQuad(String op,String r1,String r2,String dest){
        int id = this.list.size();
        this.list.add(new Quad(id,op,r1,r2,dest));
        List<Integer> temp_in;
        if(this.In.containsKey(id))
            temp_in = this.In.get(id);
        else
            temp_in = new ArrayList<Integer>();
        List<Integer> temp_out ;
        if(this.Out.containsKey(id))
            temp_out = this.Out.get(id);
        else
            temp_out = new ArrayList<Integer>();
        if((op.equals("jump") || op.equals("<=") || op.equals(">=") || op.equals(">") || op.equals("<") || op.equals("#") || op.equals("=") ) && (!dest.equals("*"))){
            temp_out.add(Integer.parseInt(dest));
            List<Integer> x = this.In.get(Integer.parseInt(dest));
            if(x == null) {
                x = new ArrayList<Integer>();
            }
            x.add(id);
            this.In.put(Integer.parseInt(dest),x);
        }
        this.In.put(id,temp_in);
        this.Out.put(id,temp_out);

    }

    public String NewTemp(String type,int pos,boolean ref){
        this.count_var++;
        String x = "$".concat(Integer.toString(this.count_var));
        TmpVariable.put(x,new TempVariable(type,pos,ref));
        return x;
    }
    public String NewTempString(String type,int pos,String data){
        this.count_var++;
        String x = "$".concat(Integer.toString(this.count_var));
        TmpVariable.put(x,new TempVariable(type,pos,true));
        if(type.equals("String")){
            this.String_count++;
            this.String_Labels.add(new StringLabel(data,".String"+Integer.toString(this.String_count)));
        }
        return x;
    }

    public void Print(int nesting){
        PrintWriter out = null;
        try {
            out = new PrintWriter(new BufferedWriter(new FileWriter(
                    "../../Quad",true)));
        }catch (IOException e) {
            System.err.println(e);
        }
        finally {
            for(int i=0;i<this.list.size();i++){
               Quad x = this.list.get(i);
               if(x.op.equals("unit")){
                   out.println("#"+x.r1+nesting);
               }
               out.println(x.id+": "+x.op +","+x.r1+","+x.r2+","+x.destination);
            }
            out.print("\n\n");
            if (out != null) {
                out.close();
            }
        }
    }

    public List<Integer> EmptyList(){
        return (new ArrayList<Integer>());
    }

    public List<Integer> MakeList(int i){
        List<Integer> x = new ArrayList<Integer>();
        x.add(i);
        return x;
    }

    public List<Integer> Merge(List<Integer> l,List<Integer> m){
       if(l.size() >= m.size()){
           for(int i=0;i<m.size();i++){
               int x = m.remove(0);
               if(!l.contains(x)){
                    l.add(x);
               }
           }
           return l;
       }
       else{
           for(int i=0;i<l.size();i++){
               int x = l.remove(0);
               if(!m.contains(x)){
                   m.add(x);
               }
           }
           return m;
       }
    }

    public void Backpatch(List<Integer> l,int x){
        while(l.size()>0){
            Quad y = this.list.get(l.remove(0));
            y.destination = Integer.toString(x);
            List<Integer> temp;
            temp = this.Out.get((Integer) y.id);
            if(temp != null) {
                temp.add(x);
                this.Out.put((Integer)y.id,temp);
            }
            else{
                temp = new ArrayList<Integer>();
                temp.add(x);
                this.Out.put((Integer)y.id,temp);
            }
            temp = this.In.get((Integer) x);
            if(temp != null){
                temp.add(y.id);
                this.In.put((Integer)x,temp);
            }
            else{
                temp = new ArrayList<Integer>();
                temp.add(y.id);
                this.In.put((Integer)x,temp);
            }
        }
    }

    public void Clear(){
        this.list.clear();
        this.TmpVariable.clear();
        this.In.clear();
        this.Out.clear();
    }
}
