package compiler;

import javax.sound.sampled.Line;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

/**
 * Created by lef on 7/5/2017.
 */
public class Symbol_Table {
    protected Stack<Info_Table> ST;
    private int nesting;

    public Symbol_Table()
    {
        this.nesting = -1;
        this.ST = new Stack<Info_Table>();
    }

    public void Enter()
    {
        this.nesting +=1;
        this.ST.push(new Info_Table(this.nesting));
    }

    public void Enter_After(Info_Table x){
        this.nesting+=1;
        x.nesting = this.nesting;
        this.ST.push(x);
    }

    public void Exit()
    {
        this.nesting -=1;
        Info_Table x = this.ST.pop();
    }

    public void Insert(String name,Type type,int line,int bytes)
    {
       if(type instanceof Function){
           this.Exists_Function(name,(Function) type,line);
       }
       else{
           this.Exists_Variable(name,(Variable) type,line,bytes);
       }
    }

    public DataType Lookup(String name,List<DataType> list,int line){
        Type temp;
        for(int i =this.ST.size()-1;i>=0;i--) {
            temp = this.ST.get(i).Hash.get(name);
            if(temp != null){
                if (temp instanceof Variable){
                    if(list != null){
                        System.out.println("Error:"+line+": Variable: "+name+" can't use it as function");
                        System.exit(4);
                    }
                    return (new DataType(((Variable)temp).type,((Variable)temp).Dimension,
                            this.ST.get(i).nesting,((Variable)temp).position,((Variable)temp).ref,((Variable)temp).dim));
                }
                else{
                    if(list == null){
                        System.out.println("Error:"+line+": Function: "+name+" can't use it as variable");
                        System.exit(4);
                    }
                    List<Variable> list_Var = ((Function)temp).args;
                    if(list_Var.size() != list.size()){
                        System.out.print("Error:"+line+": Wrong call function ");
                        System.out.print(name+"(");
                        for(i = 0;i<list_Var.size();i++){
                            Variable x = list_Var.get(i);
                            if(x.ref)
                                System.out.print("ref ");
                            System.out.print(x.type+" ");
                            for(int j =0;j<x.Dimension;j++){
                                System.out.print("[]");
                            }
                            if(i+1 != list_Var.size())
                                System.out.print(",");
                        }
                        System.out.print(")\n");
                        System.exit(6);
                    }
                    for(int j=0;j<list_Var.size();j++){
                        if(list.get(j).ref == false && list_Var.get(j).ref == true){
                            System.out.println("Error:"+line+": In function: "+name+" wrong call "+(j+1)+" argument " +
                                    "expecting "+ list_Var.get(j).type+ " by reference with "
                                    +list_Var.get(j).Dimension + " dimensions and get not refernce type");
                            System.exit(6);
                        }
                        if(list_Var.get(j).Dimension != list.get(j).dimension ||
                                !(list_Var.get(j).type.equals(list.get(j).type))){
                            System.out.println("Error:"+line+": In function: "+name+" wrong call "+(j+1)+" argument "
                                    +"expecting "+ list_Var.get(j).type+ " with "+list_Var.get(j).Dimension + " " +
                                    "dimensions " + "and get "+ list.get(j).type +" with "+list.get(j).dimension +" " +
                                    "dimensions");
                            System.exit(6);
                        }
                        if(list.get(j).ref == true && list.get(j).dim != null){
                            int y = list_Var.get(j).dim.size();
                            int m = list.get(j).dim.size() -1;
                            for(int k = y-1;k>0;k--){
                                if(list.get(j).dim.get(m) != list_Var.get(j).dim.get(k)){
                                    System.out.print("Error:"+line+": Wrong pass array in function "
                                       +name+" in "
                                        +(j+1)+" parameter in "+(k+2)+" dimension,");
                                    System.out.println("expecting ["+list_Var.get(j).dim.get(k)+"] and get ["
                                            +list.get(j).dim.get(m)+"]");
                                    System.exit(2);
                                }
                                m--;
                            }
                        }
                    }
                    return (new DataType(((Function)temp).return_type,0,list_Var,this.ST.get(i).nesting,0));
                }
            }
        }

        System.out.println("Error:"+line+": '" + name + "' undeclared");
        System.exit(4);
        return null;
    }

    private void Exists_Function(String name,Function fun,int line){
        Type x = this.ST.peek().Hash.get(name);
        if(x != null){
            if(x instanceof Variable){
                System.out.println("Error:"+line+": Variable "+name+" exists already as variable");
                System.exit(3);
            }
            Function temp = (Function) x;
            if(temp.flag == 1 && fun.flag == 2){
                switch (this.Equal_Function(temp,fun)){
                    case -1:
                        System.out.println("Error:"+line+": Duplicate function definition, not matched return_type: "+name);
                        System.exit(3);
                        break;
                    case -2:
                        System.out.println("Error:"+line+": Duplicate function definition, not correct number of args: "+name);
                        System.exit(3);
                        break;
                    case -3:
                        System.out.println("Error:"+line+": Duplicate function definition, not matched args: "+name);
                        System.exit(3);
                        break;
                    default:
                        ;
                }
                temp.flag = 2;
                this.ST.peek().undef_functions.remove(name);
            }
            else{
                System.out.println("Error:"+line+": Duplicate function definition: "+name);
                System.exit(2);
            }
        }
        else{
            this.ST.peek().Hash.put(name,fun);
            if(fun.flag == 1){
                this.ST.peek().undef_functions.add(name);
            }
        }
    }

    private int Equal_Function(Function x,Function y){
        if(!x.return_type.equals(y.return_type)){
            return -1;
        }
        if(x.num_of_args != y.num_of_args){
            return -2;
        }
        for(int i=0;i<x.num_of_args;i++){
            Variable temp1,temp2;
            temp1 = x.args.get(i);
            temp2 = y.args.get(i);
            if(temp1.Dimension != temp2.Dimension || (!temp1.type.equals(temp2.type)) || temp1.ref != temp2.ref){
                return -3;
            }
        }
        return 0;
    }

    private void Exists_Variable(String name,Variable var,int line,int bytes){
        Type x = this.ST.peek().Hash.get(name);
        if(x instanceof Function){
            System.out.println("Error:"+line+": Variable "+name+" exists already as function");
            System.exit(3);
        }
        if(x != null){
            System.out.println("Error:"+line+": Duplicate variable definition: "+name);
            System.exit(3);
        }
        else{
            if(var.type.equals("int") && this.ST.peek().var_bytes % 4 != 0){
                bytes += 4 - (this.ST.peek().var_bytes % 4);
            }
            this.ST.peek().bytes+=bytes;
            this.ST.peek().var_bytes+=bytes;
            var.position = -this.ST.peek().var_bytes;
            this.ST.peek().Hash.put(name,var);
        }
    }

    public int Get_TempVarPos(){
        return this.ST.peek().var_bytes;
    }

    public List<String> Get_UndefFun(){
        return this.ST.peek().undef_functions;
    }

    public int Get_Nest(){
        return this.nesting;
    }

    public DataType Find(String name){
        Type temp;
        for(int i =this.ST.size()-1;i>=0;i--) {
            temp = this.ST.get(i).Hash.get(name);
            if (temp != null) {
                if(temp instanceof Variable) {
                    return (new DataType(((Variable) temp).type, ((Variable) temp).Dimension,
                            this.ST.get(i).nesting, ((Variable) temp).position, ((Variable) temp).ref));
                }
                else{
                    return (new DataType(((Function) temp).return_type));
                }
            }
        }
        return null;
    }

    public int Get_FunNest(String name){
        Type temp;
        for(int i =this.ST.size()-1;i>=0;i--) {
            temp = this.ST.get(i).Hash.get(name);
            if (temp != null) {
               return this.ST.get(i).nesting+1;
            }
        }
        return 1;
    }

    public int Get_ParamBytes(String name){
        Type temp;
        for(int i =this.ST.size()-1;i>=0;i--) {
            temp = this.ST.get(i).Hash.get(name);
            if (temp != null) {
                return ((Function)temp).param_bytes;
            }
        }
        return 0;
    }

    public boolean Find_VarOpt(String name){
        Type x = this.ST.peek().Hash.get(name);
        if(x != null && ((Variable)x).ref == false ){
            return false;
        }
        return true;
    }
}
