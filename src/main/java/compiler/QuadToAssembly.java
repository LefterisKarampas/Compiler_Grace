package compiler;

import org.testng.annotations.Test;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


/**
 * Created by lef on 19/5/2017.
 */
public class QuadToAssembly {
    protected int flag;
    protected String size;
    protected int string_count = 0;
    protected HashMap<String,String> labels;

    public QuadToAssembly(){
        PrintWriter out = null;
        labels = new HashMap<String, String>();
        this.flag = 1;
        try {

            out = new PrintWriter(new BufferedWriter(new FileWriter(
                    "../../file.s",false)));
        }catch (IOException e) {
            System.err.println(e);
        }
        finally {
            out.println(".intel_syntax noprefix # Use Intel syntax instead of AT&T\n" +
                    "\t.text\n" +
                    "\t.global main\n");
            if (out != null) {
                out.close();
            }
        }
    }

    public void Generator(QuadRuple Quads,Symbol_Table ST,int vars_byte,int flag){
        PrintWriter out = null;
        this.flag +=1;
        String endof = null;
        try {
            out = new PrintWriter(new BufferedWriter(new FileWriter(
                    "../../file.s",true)));
            int counter = 0;
            if(flag == 1){
                out.println("\ngrace_main:");
            }
            while(Quads.list.size()>0){
                Quad x = Quads.list.remove(0);
                out.print(".".concat(Integer.toString(this.flag).concat(Integer.toString(x.id)).concat(":\n")));
                if(x.op.equals("unit")){
                    endof = ".endof_".concat(x.r1);
                    out.print("\n"+this.name(x.r1,this.flag,ST)+":\n");
                    out.println("\tpush ebp");
                    out.println("\tmov ebp, esp");
                    out.printf("\tsub esp,%d\n",vars_byte);
                    out.println("\tand esp,-4");
                    out.println("\tpush esi");
                    out.println("\tpush edi");
                    out.println("\tpush ebx");
                }
                else if(x.op.equals("endu")){
                    out.println("\tpop ebx");
                    out.println("\tpop edi");
                    out.println("\tpop esi");
                    out.println(endof+":\tmov esp, ebp");
                    out.println("\tpop ebp");
                    out.println("\tret");
                    //out.print("\t"+this.name(x.r1,this.flag,counter)+" endp\n");
                }
                else if(x.op.equals("+")){
                    out.println("\t"+load("eax",x.r1,Quads.TmpVariable,ST,Quads.String_Labels));
                    out.println("\t"+load("edx",x.r2,Quads.TmpVariable,ST,Quads.String_Labels));
                    out.println("\tadd eax, edx");
                    out.println("\t"+store("eax",x.destination,Quads.TmpVariable,ST,Quads.String_Labels,2));
                }
                else if(x.op.equals("-")){
                    out.println("\t"+load("eax",x.r1,Quads.TmpVariable,ST,Quads.String_Labels));
                    out.println("\t"+load("edx",x.r2,Quads.TmpVariable,ST,Quads.String_Labels));
                    out.println("\tsub eax, edx");
                    out.println("\t"+store("eax",x.destination,Quads.TmpVariable,ST,Quads.String_Labels,2));
                }
                else if(x.op.equals("*")){
                    out.println("\t"+load("eax",x.r1,Quads.TmpVariable,ST,Quads.String_Labels));
                    out.println("\t"+load("ecx",x.r2,Quads.TmpVariable,ST,Quads.String_Labels));
                    out.println("\timul ecx");
                    out.println("\t"+store("eax",x.destination,Quads.TmpVariable,ST,Quads.String_Labels,2));
                }
                else if(x.op.equals("/")){
                    out.println("\t"+load("eax",x.r1,Quads.TmpVariable,ST,Quads.String_Labels));
                    out.println("\tcdq");
                    out.println("\t"+load("ecx",x.r2,Quads.TmpVariable,ST,Quads.String_Labels));
                    out.println("\tidiv ecx");
                    out.println("\t"+store("eax",x.destination,Quads.TmpVariable,ST,Quads.String_Labels,2));
                }
                else if(x.op.equals("%")){
                    out.println("\t"+load("eax",x.r1,Quads.TmpVariable,ST,Quads.String_Labels));
                    out.println("\tcdq");
                    out.println("\t"+load("ecx",x.r2,Quads.TmpVariable,ST,Quads.String_Labels));
                    out.println("\tidiv ecx");
                    out.println("\t"+store("edx",x.destination,Quads.TmpVariable,ST,Quads.String_Labels,2));
                }
                else if(x.op.equals("ret")){
                    out.println("\tjmp "+endof);
                }
                else if(x.op.equals("array")){
                    String temp = "";
                    out.println("\t"+load("eax",x.r2,Quads.TmpVariable,ST,Quads.String_Labels));
                    temp = "\t"+loadAddr("ecx",x.r1,Quads.TmpVariable,ST,Quads.String_Labels);
                    if(this.size.equals("dword")) {
                        out.println("\tmov ecx,4");
                    }
                    else {
                        out.println("\tmov ecx,1");
                    }
                    out.println("\timul ecx");
                    out.println(temp);
                    out.println("\tadd eax, ecx");
                    out.println("\t"+store("eax",x.destination,Quads.TmpVariable,ST,Quads.String_Labels,0));
                }
                else if(x.op.equals("jump")){
                    out.print("\tjmp ");
                    out.println(".".concat(Integer.toString(this.flag).concat(x.destination)));
                }
                else if(x.op.equals("call")){
                    DataType y = ST.Find(x.destination);
                    if(y.type.equals("nothing")) {
                        out.println("\tsub esp, 4");
                    }
                    out.print(updateAL(ST.Get_Nest(),x.destination,ST));
                    out.println("\tcall near ptr "+name(x.destination,this.flag,ST));
                    out.println("\tadd esp, "+ST.Get_ParamBytes(x.destination)+" + 8");
                }
                else if(x.op.equals("par")){
                    if(x.r2.equals("V")){
                        out.println("\t"+load("eax",x.r1,Quads.TmpVariable,ST,Quads.String_Labels));
                        out.println("\tpush eax");
                    }
                    else{
                        out.println("\t"+loadAddr("esi",x.r1,Quads.TmpVariable,ST,Quads.String_Labels));
                        out.println("\tpush esi");
                    }
                }
                else if(x.op.equals(":=")){
                   out.println("\t"+load("eax",x.r1,Quads.TmpVariable,ST,Quads.String_Labels));
                   out.println("\t"+store("eax",x.destination,Quads.TmpVariable,ST,Quads.String_Labels,1));
                }
                else if(x.op.equals("#")){
                    out.println("\t"+load("eax",x.r1,Quads.TmpVariable,ST,Quads.String_Labels));
                    out.println("\t"+load("edx",x.r2,Quads.TmpVariable,ST,Quads.String_Labels));
                    out.println("\tcmp eax, edx");
                    out.print("\tjnz ");
                    out.println(".".concat(Integer.toString(this.flag).concat(x.destination)));
                }
                else if(x.op.equals(">=")){
                    out.println("\t"+load("eax",x.r1,Quads.TmpVariable,ST,Quads.String_Labels));
                    out.println("\t"+load("edx",x.r2,Quads.TmpVariable,ST,Quads.String_Labels));
                    out.println("\tcmp eax, edx");
                    out.print("\tjge ");
                    out.println(".".concat(Integer.toString(this.flag).concat(x.destination)));
                }
                else if(x.op.equals("<=")){
                    out.println("\t"+load("eax",x.r1,Quads.TmpVariable,ST,Quads.String_Labels));
                    out.println("\t"+load("edx",x.r2,Quads.TmpVariable,ST,Quads.String_Labels));
                    out.println("\tcmp eax, edx");
                    out.print("\tjle ");
                    out.println(".".concat(Integer.toString(this.flag).concat(x.destination)));
                }
                else if(x.op.equals("=")){
                    out.println("\t"+load("eax",x.r1,Quads.TmpVariable,ST,Quads.String_Labels));
                    out.println("\t"+load("edx",x.r2,Quads.TmpVariable,ST,Quads.String_Labels));
                    out.println("\tcmp eax, edx");
                    out.print("\tjz ");
                    out.println(".".concat(Integer.toString(this.flag).concat(x.destination)));
                }
                else if(x.op.equals(">")){
                    out.println("\t"+load("eax",x.r1,Quads.TmpVariable,ST,Quads.String_Labels));
                    out.println("\t"+load("edx",x.r2,Quads.TmpVariable,ST,Quads.String_Labels));
                    out.println("\tcmp eax, edx");
                    out.print("\tjg ");
                    out.println(".".concat(Integer.toString(this.flag).concat(x.destination)));
                }
                else if(x.op.equals("<")){
                    out.println("\t"+load("eax",x.r1,Quads.TmpVariable,ST,Quads.String_Labels));
                    out.println("\t"+load("edx",x.r2,Quads.TmpVariable,ST,Quads.String_Labels));
                    out.println("\tcmp eax, edx");
                    out.print("\tjl ");
                    out.println(".".concat(Integer.toString(this.flag).concat(x.destination)));
                }
                counter++;
            }
            if(flag == 1){
                out.println("\nmain:\tpush ebp");
                out.println("\tmov ebp,esp");
                out.println("\tcall grace_main");
                out.println("\tmov eax,0");
                out.println("\tmov esp,ebp");
                out.println("\tpop ebp");
                out.println("\tret");
            }
        }catch (IOException e) {
            System.err.println(e);
        }finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private String name(String x,int flag,Symbol_Table ST){
        int nest = ST.Get_FunNest(x);
        if(nest == 1){
            return x+"1";
        }
        String y = this.labels.get(x);
        if(y == null){
            String label = x.concat(Integer.toString(flag));
            this.labels.put(x,label);
            return label;
        }
        return y;
    }

    private String load(String register, String var, HashMap<String,TempVariable> TmpVar, Symbol_Table ST,Queue<StringLabel> String_Labels){
        String code="";
        if(var.startsWith("$")){                //Temp variable
            TempVariable x = TmpVar.get(var);
            if(x.type.equals("String")){
                this.size = "byte";
                return "mov esi, OFFSET FLAT:"+((LinkedList<StringLabel>)String_Labels).get(this.string_count++).label;
            }
            if(x.type.equals("int")){
                this.size = "dword";
            }
            else{
                this.size = "byte";
                if(register.equals("eax"))
                    register = "al";
            }
            if(x.ref == true && x.type.equals("char")){
                code = "mov "+register+",dword ptr [ebp " + x.position+"]";
            }
            else {
                code = "mov " + register + ", " + this.size + " ptr [ebp " + x.position + "]";
            }
        }
        else if(var.startsWith("[")){           //[x] repair size
            String temp = this.size;

            code = load("edi",var.substring(1).replaceAll("]",""),TmpVar,ST,String_Labels) +
                    "\n\tmov " +register +", "+temp+" ptr [edi]";
            this.size = temp;
        }
        else {                              // Local variable or parameters
            DataType x = ST.Find(var);
            if (x != null) {
                if(x.type.equals("int")){
                    this.size = "dword";
                }
                else{
                    this.size = "byte";
                    if(register.equals("eax"))
                        register = "al";
                }
                if (x.nesting == ST.Get_Nest()) {   //Local
                    if(x.ref == false) {
                        if (x.position > 0) {       //Parameter by value
                            code = "mov " + register + ", "+this.size+" ptr [ebp +" + x.position + "]";
                        } else {                    //Local variable
                            if(x.dimension > 0){
                                code = "mov " + register + ","+this.size +" ptr [ebp " + x.position + "]";
                            }
                            else {
                                code = "mov " + register + ", " + this.size + " ptr [ebp " + x.position + "]";
                            }
                        }
                    }
                    else{                 //Parameter by reference
                        code = "mov esi, dword ptr [ebp +" + x.position + "]\n";
                        code += "\tmov " + register + ", "+this.size+" ptr [esi]"; //Parameter by ref
                    }
                }
                else{                               //Not local
                    code = getAR(x.nesting,ST.Get_Nest());
                    if(x.ref == false){
                        if (x.position > 0) {       //Parameter
                            code += "\tmov " + register + ", "+this.size +" ptr [esi+" + x.position + "]";
                        } else {                    //temporary or local in other scope
                            code += "\tmov " + register + ", "+this.size+" ptr [esi" + x.position + "]";
                        }
                    }
                    else{
                        code += "\tmov esi, "+this.size+" ptr [esi +" + x.position + "]\n";
                        code += "\tmov "+register+", "+this.size+" ptr [esi]";
                    }
                }
            }
            else{                                   //Const_char
                if(var.startsWith("'")){
                    if(var.equals("'\\0'")){
                        code = "mov "+register+", 0";
                    }
                    else {
                        this.size = "byte";
                        register = "al";
                        code = "mov " + register + ", " + var;
                    }
                }
                else{                               //Arithmetic
                    code = "mov " + register + ", "+var;
                    this.size = "dword";
                }
            }

        }
        return code;
    }

    private String updateAL(int nesting,String name,Symbol_Table ST){
        int x = ST.Get_FunNest(name);
        if((x == 1) && ( name.equals("puti")|| name.equals("puts") || name.equals("putc") || name.equals("geti")
        || name.equals("getc") || name.equals("gets") || name.equals("strcpy") || name.equals("strcmp")
        || name.equals("strcpy") || name.equals("strlen"))){
            return "\tpush ebp\n";
        }
        if(nesting < x){
            return "\tpush ebp\n";
        }
        else if(nesting == x){
            return "\tpush dword ptr [ebp+8]\n";
        }
        else{
            String code ="";
            code +="\tmov esi, dword ptr [ebp+8]\n";
            for(int i=0;i<nesting-x-1;i++){
                code += "\tmov esi, dword ptr [esi+8]\n";
            }
            return code+"\tpush dword ptr [esi+8]\n";
        }
    }

    public String getAR(int nesting,int x){
        String code ="";
        code ="mov esi, dword ptr [ebp+8]\n";
        for(int i=0;i<x-nesting-1;i++){
            code += "\tmov esi, dword ptr [esi+8]\n";
        }
        return code;
    }

    public String store(String register, String var, HashMap<String,TempVariable> TmpVar, Symbol_Table ST,Queue<StringLabel> String_Labels,int flag){
        if(var.equals("$$")){
            return  "mov esi, dword ptr [ebp+12]\n"+
            "\tmov dword ptr [esi], "+register;
            //return "mov "+this.size +" ptr [ebp +12], "+register;
        }
        if(var.startsWith("$")){                //Temp variable
            TempVariable x = TmpVar.get(var);

            if(x.type.equals("int")){
                this.size = "dword";
            }
            else{
                this.size = "byte";
                if(flag == 1 && register.equals("eax")){
                    register = "al";
                }
                else if(flag == 0){
                    this.size = "dword";  //array's store (pointer)
                }
            }
            return "mov "+this.size+" ptr [ebp "+x.position+"], "+register;
        }
        DataType x = ST.Find(var);
        if (x != null) {
            if(x.type.equals("int")){
                this.size = "dword";
            }
            else{
                if(flag == 1 && register.equals("eax")){
                    register = "al";
                }
                if(flag == 0){
                    this.size = "dword";
                }
                this.size = "byte";
            }
            if (x.nesting == ST.Get_Nest()) {   //Local variable
                if(x.ref == false) {
                    if (x.position > 0) {
                        return "mov " + this.size + " ptr [ebp +" + x.position + "], " + register;
                    } else {
                        return "mov " + this.size + " ptr [ebp " + x.position + "], " + register;
                    }
                }
                else{
                    return "mov esi, dword ptr [ebp + "+x.position+"]\n"+
                    "\tmov "+this.size+" ptr [esi], "+register;
                }
            }
            else{                               //Non local variable
                if(x.ref == true){              //by ref
                    return getAR(x.nesting,ST.Get_Nest()) +
                            "\tmov esi, dword  ptr [esi + "+x.position+"]" +
                            "\t\nmov "+this.size+" ptr [esi], "+register;
                }
                else {
                    if (x.position > 0) {
                        return getAR(x.nesting, ST.Get_Nest()) +
                                "\tmov " + this.size + " ptr [esi + " + x.position + "], " + register;
                    } else {
                        return getAR(x.nesting, ST.Get_Nest()) +
                                "\tmov " + this.size + " ptr [esi " + x.position + "], " + register;
                    }
                }
            }
        }
        if(var.startsWith("[")){           //[x] size
            String code = load("edi",var.substring(1).replaceAll("]",""),TmpVar,ST,String_Labels);
            if(flag == 1 && this.size.equals("byte") && register.equals("eax")){
                register = "al";
            }
            code+="\n\tmov "+this.size+" ptr [edi], " + register;
            return code;
        }
        return "";
    }

    private String loadAddr(String register, String var, HashMap<String,TempVariable> TmpVar, Symbol_Table ST,Queue<StringLabel> String_Labels) {
        if (var.startsWith("[")) {           //[x] repair size
            return load(register, var.substring(1).replaceAll("]", ""), TmpVar, ST, String_Labels);
        }
        if(var.startsWith("$")){
            TempVariable x = TmpVar.get(var);
            if(x.type.equals("String")){
                return "mov "+register+", OFFSET FLAT: "+ ((LinkedList<StringLabel>)String_Labels).get(this.string_count++).label;
            }
            else{
                return "lea " + register + ", dword ptr [ebp " + x.position + "]";
            }
        }
        DataType x = ST.Find(var);
        if (x != null) {
            if (x.type.equals("int")) {
                this.size = "dword";
            }
            else {
                this.size = "byte";
            }
            if (x.nesting == ST.Get_Nest()) {   //Local variable
                if(x.ref == true){              //Local by ref
                    return "mov " + register + ", dword ptr [ebp + " + x.position + "]";
                }
                else {              //Local by value
                    if(x.position > 0)
                        return "lea " + register + ", " + this.size + " ptr [ebp + " + x.position + "]";
                    else
                        return "lea " + register + ", " + this.size + " ptr [ebp " + x.position + "]";
                }
            } else {                        //Not local
                if (x.ref == true){         //Parameter by ref
                    return getAR(x.nesting,ST.Get_Nest()) +
                            "\tmov " + register + ", dword ptr [esi + " + x.position + "]";
                } else {                    //Parameter by value
                    if(x.position > 0){
                        return getAR(x.nesting,ST.Get_Nest()) +
                                "\tmov " + register + ", dword ptr [esi + " + x.position + "]";
                    }
                    else {
                        if(x.dimension > 0){
                            return getAR(x.nesting, ST.Get_Nest()) +
                                    "\tlea " + register + ",dword ptr [esi " + x.position + "]";
                        }
                        else {
                            return getAR(x.nesting, ST.Get_Nest()) +
                                    "\tlea " + register + ", dword ptr [esi " + x.position + "]";
                        }
                    }
                }
            }
        }
        return "";
    }

    public void Data(Queue<StringLabel> x) {
        PrintWriter out = null;
        try {
            out = new PrintWriter(new BufferedWriter(new FileWriter(
                    "../../file.s", true)));
            if(x.size()>0)
                out.println(".data");
            while (!x.isEmpty()) {
                StringLabel temp = x.remove();
                out.println("\t"+temp.label+":\t.asciz "+temp.name);
            }
        }catch (IOException e) {
            System.err.println(e);
        }finally {
            if (out != null) {
                out.close();
            }
        }
    }
}
