package compiler;


import java.util.*;

/**
 * Created by lef on 22/6/2017.
 */
public class Optimization {
    private Graph G;
    private List<Quad> NewQuads;

    public Optimization(){
        this.NewQuads = new ArrayList<Quad>();
    }

    private Graph Split_Blocks(ArrayList<Quad> Quads, HashMap<Integer,List<Integer>> In,
                                  HashMap<Integer,List<Integer>> Out){
        List<BasicBlock> temp = new ArrayList<BasicBlock>();
        List<Quad> temp_quads = new ArrayList<Quad>();
        boolean In_Edge = false;
        boolean flag = false;
        String prev = "";
        int id = -1;
        int m = -1;
        HashMap<Integer,Integer> QuadToBlock = new HashMap<Integer, Integer>();
        int count = 0;
        for(Quad q:(List<Quad>)Quads.clone()){
            m++;
            if(flag){
                if(!In.get(q.id).isEmpty() || prev.equals(">") || prev.equals(">=") || prev.equals("<") ||
                        prev.equals("<=") || prev.equals("=") || prev.equals("#")){
                   flag = false;
                }
                else{
                    Quad t = Quads.remove(m);
                    if(t.op.equals("jump")) {
                        List<Integer> list = In.get(Integer.parseInt(t.destination));
                        list.remove((Integer) t.id);
                    }
                    m--;
                    continue;
                }
            }
            if(temp_quads.size() == 0){
                if(!In.get(q.id).isEmpty() || prev.equals(">") || prev.equals(">=") || prev.equals("<") ||
                        prev.equals("<=") || prev.equals("=") || prev.equals("#")){
                    In_Edge = true;
                    temp_quads.add(q);
                    QuadToBlock.put(q.id,count);
                }
                else if(count != 0){
                    if(prev.equals("jump") && q.id != id){
                        flag = true;
                        Quad t = Quads.remove(m);
                        if(t.op.equals("jump")) {
                            List<Integer> list = In.get(Integer.parseInt(t.destination));
                            list.remove((Integer) t.id);
                        }
                        m--;
                        continue;
                    }
                }
                else{
                    In_Edge = false;
                    temp_quads.add(q);
                    QuadToBlock.put(q.id,count);
                }
                if (!Out.get(q.id).isEmpty()) {
                    temp.add(new BasicBlock(temp_quads,In_Edge,Out.get(q.id).get(0)));
                    temp_quads = new ArrayList<Quad>();
                    count++;
                    In_Edge = false;
                }
            }
            else{
                if(In.get(q.id).isEmpty()) {
                    if (Out.get(q.id).isEmpty()) {
                        temp_quads.add(q);
                        QuadToBlock.put(q.id,count);
                    }
                    else{
                        temp_quads.add(q);
                        QuadToBlock.put(q.id,count);
                        temp.add(new BasicBlock(temp_quads,In_Edge,Out.get(q.id).get(0)));
                        temp_quads = new ArrayList<Quad>();
                        count++;
                        In_Edge = false;
                    }
                }
                else{
                    temp.add(new BasicBlock(temp_quads,In_Edge,null));
                    temp_quads = new ArrayList<Quad>();
                    count++;
                    In_Edge = true;
                    temp_quads.add(q);
                    QuadToBlock.put(q.id,count);
                    if (!Out.get(q.id).isEmpty()) {
                        temp.add(new BasicBlock(temp_quads,In_Edge,Out.get(q.id).get(0)));
                        temp_quads = new ArrayList<Quad>();
                        count++;
                        In_Edge = false;
                    }
                }
            }
            prev = q.op;
            if(q.op.equals("jump")){
                id = Integer.parseInt(q.destination);
            }
        }
        if(temp_quads.size() > 0){
            temp.add(new BasicBlock(temp_quads,In_Edge,null));
        }
        return new Graph(QuadToBlock,temp);
    }

    public List<Quad> Optimize(ArrayList<Quad> Quads,HashMap<Integer,List<Integer>> In,HashMap<Integer,List<Integer>> Out,Symbol_Table ST) {
     for(int loop =0;loop<5;loop++){
        G = Split_Blocks(Quads, In, Out);
        int counter;
        for (BasicBlock Bblock : G.Blocks) {
            for (int i = 0; i < 5; i++) {
                counter = 0;
                counter += Constant_Folding((ArrayList<Quad>) Bblock.block, ST);
                counter += Copy_Propagation((ArrayList<Quad>) Bblock.block);
                if (counter == 0)
                    break;
            }
        }
        int prev;
        while (true) {
            prev = Quads.size();
            Quads = Dead_CodeElimination(G, Quads, In, ST);
            if (prev == Quads.size()) break;
        }
        G.Blocks.clear();
        G.QuadToBlock.clear();
    }
        return Quads;
    }

    private int Constant_Folding(ArrayList<Quad> Quads,Symbol_Table ST){
        int count = 0;
        for (Quad q : Quads) {
            if (q.op.equals("+")) {
                if (((!q.r1.startsWith("$")) && (!q.r1.startsWith("[")) &&ST.Find(q.r1) == null) &&
                        ((!q.r2.startsWith("$")) && (!q.r2.startsWith("[")) &&ST.Find(q.r2) == null)) {
                    int x1 = Integer.parseInt(q.r1);
                    int x2 = Integer.parseInt(q.r2);
                    int x = x1 + x2;
                        q.op = ":=";
                        q.r1 = Integer.toString(x);
                        q.r2 = "-";
                        count++;
                }
                else if ((!q.r1.startsWith("$")) && (!q.r1.startsWith("[")) &&ST.Find(q.r1) == null){
                    int x1 = Integer.parseInt(q.r1);
                    if(x1 == 0){
                        q.op = ":=";
                        q.r1 = q.r2;
                        q.r2 = "-";
                        count++;
                    }
                }
                else if ((!q.r2.startsWith("$")) && (!q.r2.startsWith("[")) &&ST.Find(q.r2) == null){
                    int x2 = Integer.parseInt(q.r2);
                    if(x2 == 0){
                        q.op = ":=";
                        q.r2 = "-";
                        count++;
                    }
                }
            }
            else if (q.op.equals("-")) {
                if (((!q.r1.startsWith("$")) && (!q.r1.startsWith("[")) && ST.Find(q.r1) == null) &&
                     ((!q.r2.startsWith("$")) && ST.Find(q.r2) == null && (!q.r2.startsWith("[")))) {
                    int x1 = Integer.parseInt(q.r1);
                    int x2 = Integer.parseInt(q.r2);
                    if(x1 == x2){
                        q.op = ":=";
                        q.r1 = "0";
                        q.r2 = "-";
                        count++;
                    }
                    else {
                        int x = x1 - x2;
                        q.op = ":=";
                        q.r1 = Integer.toString(x);
                        q.r2 = "-";
                        count++;
                    }

                }
                else if ((!q.r2.startsWith("$")) && (!q.r2.startsWith("[")) &&ST.Find(q.r2) == null){
                    int x2 = Integer.parseInt(q.r2);
                    if(x2 == 0){
                        q.op = ":=";
                        q.r2 = "-";
                        count++;
                    }
                }
                else if(q.r2 == q.r1){
                    q.op = ":=";
                    q.r1 = "0";
                    q.r2 = "-";
                    count++;
                }
            }
            else if (q.op.equals("*")) {
                if (((!q.r1.startsWith("$")) && (!q.r1.startsWith("[")) && ST.Find(q.r1) == null) &&
                            ((!q.r2.startsWith("$")) && ST.Find(q.r2) == null && (!q.r2.startsWith("[")))) {
                    int x1 = Integer.parseInt(q.r1);
                    int x2 = Integer.parseInt(q.r2);
                    int x = x1 * x2;
                    q.op = ":=";
                    q.r1 = Integer.toString(x);
                    q.r2 = "-";
                    count++;
                }
                else if ((!q.r1.startsWith("$")) && (!q.r1.startsWith("[")) &&ST.Find(q.r1) == null) {
                    int x1 = Integer.parseInt(q.r1);
                    if (x1 == 1) {
                        q.op = ":=";
                        q.r1 = q.r2;
                        q.r2 = "-";
                        count++;
                    }
                    else if(x1 == 0){
                        q.op = ":=";
                        q.r2 = "-";
                        count++;
                    }
                }
                else if ((!q.r2.startsWith("$")) && (!q.r2.startsWith("[")) &&ST.Find(q.r2) == null){
                    int x2 = Integer.parseInt(q.r2);
                    if (x2 == 1) {
                        q.op = ":=";
                        q.r2 = "-";
                        count++;
                    }
                    else if(x2 == 0){
                        q.op = ":=";
                        q.r1 = "0";
                        q.r2 = "-";
                        count++;
                    }
                }
            }
            else if (q.op.equals("/")) {
                if (((!q.r1.startsWith("$")) && (!q.r1.startsWith("[")) && ST.Find(q.r1) == null) &&
                            ((!q.r2.startsWith("$")) && (!q.r2.startsWith("[")) &&ST.Find(q.r2) == null)) {
                    int x1 = Integer.parseInt(q.r1);
                    int x2 = Integer.parseInt(q.r2);
                    int x = x1 / x2;
                    q.op = ":=";
                    q.r1 = Integer.toString(x);
                    q.r2 = "-";
                    count++;
                }
            }
            else if (q.op.equals("%")) {
                if (((!q.r1.startsWith("$")) && (!q.r1.startsWith("[")) && ST.Find(q.r1) == null) &&
                            ((!q.r2.startsWith("$")) && (!q.r2.startsWith("[")) &&ST.Find(q.r2) == null)) {
                    int x1 = Integer.parseInt(q.r1);
                    int x2 = Integer.parseInt(q.r2);
                    int x = x1 % x2;
                    q.op = ":=";
                    q.r1 = Integer.toString(x);
                    q.r2 = "-";
                    count++;
                }
            }
        }
        return count;
    }

    private int Copy_Propagation(ArrayList<Quad> Quads){
        int counter = 0;
        HashMap<String,String> change = new HashMap<String, String>();
        for(Quad q:Quads){
            if(q.op.equals(":=")){
                if(change.containsKey(q.r1)){
                    q.r1 = change.get(q.r1);
                    counter++;
                }
                change.put(q.destination,q.r1);
                Iterator it = change.entrySet().iterator();
                while (it.hasNext())
                {
                    Map.Entry item = (Map.Entry) it.next();
                    if(item.getValue().equals(q.destination))
                        it.remove();
                }
            }
            else if (q.op.equals("call")) {
                change.clear();
            }
            else{
                if(change.containsKey(q.r1)){
                    q.r1 = change.get(q.r1);
                    counter++;
                }
                if(change.containsKey(q.r2))
                    q.r2 = change.get(q.r2);
                    counter++;
            }
        }
        return counter;
    }

    private ArrayList<Quad> Dead_CodeElimination(Graph G,ArrayList<Quad> Quads,HashMap<Integer,List<Integer>> In,Symbol_Table ST){
        HashMap<String,Integer> Dead = new HashMap<String, Integer>();
        if(Quads == null)
            return null;
        int count = 0;
        boolean rem = false;
        for(Quad q: (ArrayList<Quad>)Quads.clone()){
            if(rem == true){
                continue;
            }
            rem = false;
            if(q.op.equals(":=")){
                if(q.destination.equals(q.r1)){
                    Quads.remove(count);
                    count--;
                }
                else if(!q.destination.startsWith("$$") &&
                   (q.destination.startsWith("$") || !ST.Find_VarOpt(q.destination))) {
                    if (!G.Blocks.get(G.QuadToBlock.get(q.id)).In_Edge &&
                            (!Dead.containsKey(q.destination))) {
                        Dead.put(q.destination, new Integer(q.id));
                    }
                }
            }
            else if(q.op.equals(">")){
                if (((!q.r1.startsWith("$")) && (!q.r1.startsWith("[")) && ST.Find(q.r1) == null) &&
                        ((!q.r2.startsWith("$")) && ST.Find(q.r2) == null && (!q.r2.startsWith("[")))) {
                    if(!q.r1.startsWith("'")){
                       int x1 = Integer.parseInt(q.r1);
                       int x2 = Integer.parseInt(q.r2);
                       if(x1 > x2){
                           q.op = "jump";
                           q.r1 = "-";
                           q.r2 = "-";
                           if(Quads.get(count+1).op.equals("jump")) {
                               Quad t = Quads.remove(count + 1);
                               List<Integer> list = In.get(Integer.parseInt(t.destination));
                               list.remove((Integer)t.id);
                               count--;
                               rem = true;
                           }
                       }
                       else{
                           Quad t = Quads.remove(count);
                           List<Integer> list = In.get(Integer.parseInt(t.destination));
                           list.remove((Integer)t.id);
                           count--;
                       }
                    }
                }
            }
            else if(q.op.equals(">=")) {
                if (((!q.r1.startsWith("$")) && (!q.r1.startsWith("[")) && ST.Find(q.r1) == null) &&
                        ((!q.r2.startsWith("$")) && ST.Find(q.r2) == null && (!q.r2.startsWith("[")))) {
                    if (!q.r1.startsWith("'")) {
                        int x1 = Integer.parseInt(q.r1);
                        int x2 = Integer.parseInt(q.r2);
                        if (x1 >= x2) {
                            q.op = "jump";
                            q.r1 = "-";
                            q.r2 = "-";
                            if(Quads.get(count+1).op.equals("jump")) {
                                Quad t = Quads.remove(count + 1);
                                List<Integer> list = In.get(Integer.parseInt(t.destination));
                                list.remove((Integer)t.id);
                                count--;
                                rem = true;
                            }
                        }
                        else{
                            Quad t = Quads.remove(count);
                            List<Integer> list = In.get(Integer.parseInt(t.destination));
                            list.remove((Integer)t.id);
                            count--;
                        }
                    }
                }
            }
            else if(q.op.equals("<")) {
                if (((!q.r1.startsWith("$")) && (!q.r1.startsWith("[")) && ST.Find(q.r1) == null) &&
                        ((!q.r2.startsWith("$")) && ST.Find(q.r2) == null && (!q.r2.startsWith("[")))) {
                    if (!q.r1.startsWith("'")) {
                        int x1 = Integer.parseInt(q.r1);
                        int x2 = Integer.parseInt(q.r2);
                        if (x1 < x2) {
                            q.op = "jump";
                            q.r1 = "-";
                            q.r2 = "-";
                            if(Quads.get(count+1).op.equals("jump")) {
                                Quad t = Quads.remove(count + 1);
                                List<Integer> list = In.get(Integer.parseInt(t.destination));
                                list.remove((Integer)t.id);
                                count--;
                                rem = true;
                            }
                        }
                        else{
                            Quad t = Quads.remove(count);
                            List<Integer> list = In.get(Integer.parseInt(t.destination));
                            list.remove((Integer)t.id);
                            count--;
                        }
                    }
                }
            }
            else if(q.op.equals("<=")) {
                if (((!q.r1.startsWith("$")) && (!q.r1.startsWith("[")) && ST.Find(q.r1) == null) &&
                        ((!q.r2.startsWith("$")) && ST.Find(q.r2) == null && (!q.r2.startsWith("[")))) {
                    if (!q.r1.startsWith("'")) {
                        int x1 = Integer.parseInt(q.r1);
                        int x2 = Integer.parseInt(q.r2);
                        if (x1 <= x2) {
                            q.op = "jump";
                            q.r1 = "-";
                            q.r2 = "-";
                            if(Quads.get(count+1).op.equals("jump")) {
                                Quad t = Quads.remove(count + 1);
                                List<Integer> list = In.get(Integer.parseInt(t.destination));
                                list.remove((Integer)t.id);
                                count--;
                                rem = true;
                            }
                        }
                        else{
                            Quad t = Quads.remove(count);
                            List<Integer> list = In.get(Integer.parseInt(t.destination));
                            list.remove((Integer)t.id);
                            count--;
                        }
                    }
                }
            }
            else if(q.op.equals("=")) {
                if (((!q.r1.startsWith("$")) && (!q.r1.startsWith("[")) && ST.Find(q.r1) == null) &&
                        ((!q.r2.startsWith("$")) && ST.Find(q.r2) == null && (!q.r2.startsWith("[")))) {
                    if (!q.r1.startsWith("'")) {
                        int x1 = Integer.parseInt(q.r1);
                        int x2 = Integer.parseInt(q.r2);
                        if (x1 == x2) {
                            q.op = "jump";
                            q.r1 = "-";
                            q.r2 = "-";
                            if(Quads.get(count+1).op.equals("jump")) {
                                Quad t = Quads.remove(count + 1);
                                List<Integer> list = In.get(Integer.parseInt(t.destination));
                                list.remove((Integer)t.id);
                                count--;
                                rem = true;
                            }
                        }
                        else{
                            Quad t = Quads.remove(count);
                            List<Integer> list = In.get(Integer.parseInt(t.destination));
                            list.remove((Integer)t.id);
                            count--;
                        }
                    }
                }
            }
            else if(q.op.equals("#")) {
                if (((!q.r1.startsWith("$")) && (!q.r1.startsWith("[")) && ST.Find(q.r1) == null) &&
                        ((!q.r2.startsWith("$")) && ST.Find(q.r2) == null && (!q.r2.startsWith("[")))) {
                    if (!q.r1.startsWith("'")) {
                        int x1 = Integer.parseInt(q.r1);
                        int x2 = Integer.parseInt(q.r2);
                        if (x1 == x2) {
                            q.op = "jump";
                            q.r1 = "-";
                            q.r2 = "-";
                            if(Quads.get(count+1).op.equals("jump")) {
                                Quad t = Quads.remove(count + 1);
                                List<Integer> list = In.get(Integer.parseInt(t.destination));
                                list.remove((Integer)t.id);
                                count--;
                                rem = true;
                            }
                        }
                        else{
                            Quad t = Quads.remove(count);
                            List<Integer> list = In.get(Integer.parseInt(t.destination));
                            list.remove((Integer)t.id);
                            count--;
                        }
                    }
                }
            }
            else if(q.op.equals("jump")){
                int x = Integer.parseInt(q.destination);
                if((q.id +1) == x){
                    if(In.get(q.id).isEmpty()) {
                        List<Integer> list = In.get(x);
                        list.remove((Integer)q.id);
                        Quads.remove(count);
                        count--;
                    }
                    else{
                        q.op = "null";
                        q.r1 = "-";
                        q.r2 = "-";
                        q.destination = "-";
                    }
                }
                else if(count+1 < Quads.size() && Quads.get(count+1).id == x){
                    if(In.get(q.id).isEmpty()){
                        List<Integer> list = In.get(x);
                        list.remove((Integer)q.id);
                        Quad t = Quads.remove(count);
                        count--;
                    }
                    else{
                       q.op = "null";
                       q.r1 = "-";
                       q.r2 = "-";
                       q.destination = "-";
                    }
                }
                else {
                    Integer y =  G.QuadToBlock.get(x);
                    if(y != null) {
                        for (Quad t : G.Blocks.get(y).block) {
                            if (t.id == x) {
                                if (t.op.equals("jump")) {
                                    List<Integer> list = In.get(Integer.parseInt(q.destination));
                                    list.remove((Integer) q.id);
                                    q.destination = t.destination;
                                    list = In.get(Integer.parseInt(q.destination));
                                    list.add((Integer) q.id);
                                }
                                break;
                            }
                        }
                    }
                }
            }
            if (!q.r1.equals("-")) {
                Dead.put(q.r1, new Integer(-1));
            }
            if (!q.r2.equals("-")) {
                Dead.put(q.r2, new Integer(-1));
            }
            count++;
        }

        Iterator it = Dead.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry pair = (Map.Entry)it.next();
            if(((Integer)pair.getValue()).intValue() != -1){
                int m = 0;
                for(Quad q: (ArrayList<Quad>)Quads.clone()){
                    if(q.id == ((Integer)pair.getValue()).intValue() && In.get(q.id).size() == 0){
                        Quads.remove(m);
                        break;
                    }
                    m++;
                }
            }
            it.remove();
        }
        return Quads;
    }



}
