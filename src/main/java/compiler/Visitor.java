package compiler;

/**
 * Created by lef on 6/5/2017.
 */
import compiler.analysis.DepthFirstAdapter;
import compiler.node.*;


import java.util.*;

public class Visitor extends DepthFirstAdapter {
    int tab = 0;
    protected Symbol_Table ST;
    protected String type;              // Hold here the type for semantics analysis
    protected String result;            // Hold here the result for quadruples
    protected int line;                 // Hold line for errors
    protected int TempPos = 0;          // Space for activation record
    protected QuadInfo QInfo;           // Detais for quads
    protected int dimension;            // Arrays dimensions for semantics analysis
    protected QuadRuple Quads;          // Save quads and temp variables here
    protected QuadToAssembly q;         // Assembly code generate
    protected Stack<FunType> ret;       // Returns of functions
    protected List<Integer> dim;        // Dimensions for addressing an array's item
    protected Optimization opt;
    protected boolean opt_flag;

    public Visitor(boolean opt_flag){
        if(opt_flag){
            this.opt_flag = true;
            this.opt = new Optimization();
        }
        this.ST = new Symbol_Table();
        this.ret = new Stack<FunType>();
        this.Quads = new QuadRuple();
        this.QInfo = new QuadInfo();
        this.q = new QuadToAssembly();
    }

    public void caseAProgram(AProgram node)
    {
        node.getReturnType().apply(this);
        inAProgram(node);
        {
            List<PFunInfo> copy = new ArrayList<PFunInfo>(node.getParamInfo());
            for(PFunInfo e : copy)
            {
                e.apply(this);
            }
        }
        {
            List<PLocalDef> copy = new ArrayList<PLocalDef>(node.getLocalDef());
            for(PLocalDef e : copy)
            {
                e.apply(this);
            }
        }
        this.TempPos = this.ST.Get_TempVarPos();                             //Get the space in activation record
        this.Quads.GenQuad("unit",node.getFunName().getText(),"-","-");
        {
            List<PStmt> copy = new ArrayList<PStmt>(node.getStmt());
            for(PStmt e : copy)
            {
                e.apply(this);
            }
        }
        this.Quads.GenQuad("endu",node.getFunName().getText(),"-","-");
        outAProgram(node);
        if(this.opt_flag) {
            this.Quads.list = this.opt.Optimize((ArrayList<Quad>) this.Quads.list, this.Quads.In, this.Quads.Out, this.ST);
        }
        this.Quads.Print(this.ST.Get_Nest());
        this.q.Generator(this.Quads,this.ST,this.TempPos,this.ST.Get_Nest());
        this.q.Data(this.Quads.String_Labels);
        this.ST.Exit();
    }

    @Override
    public void inAProgram(AProgram node){
        this.ST.Enter();
        List<Variable> vars = new ArrayList<Variable>();
        if(node.getParamInfo().size() > 0){
            System.out.println("Main fucntion: "+node.getFunName().toString().trim() +" must have not arguments");
            System.exit(5);
        }
        if(!node.getReturnType().toString().trim().equals("nothing")){
            System.out.println("Main fucntion: "+node.getFunName().toString().trim() +" must not return anything");
            System.exit(5);
        }
        this.ST.Insert(node.getFunName().getText(),new Function(vars,node.getReturnType().toString().trim(),2,0),this.line,0);
        //Push the return in stack for semantics analysis
        this.ret.push(new FunType(node.getFunName().getText(),node.getReturnType().toString().trim()));

        //I/0 and other Functions on Grace hardcoded
        List<Variable> vars1 = new ArrayList<Variable>();
        vars1.add(new Variable("int",false,0,16,null));
        ST.Insert("puti",new Function(vars1,"nothing",2,4),0,0);
        List<Variable> vars2 = new ArrayList<Variable>();
        vars2.add(new Variable("char",false,0,16,null));
        ST.Insert("putc",new Function(vars2,"nothing",2,1),0,0);
        List<Variable> vars3 = new ArrayList<Variable>();
        vars3.add(new Variable("char",true,1,16,new ArrayList<Integer>()));
        ST.Insert("puts",new Function(vars3,"nothing",2,4),0,0);
        ST.Insert("geti",new Function(new ArrayList<Variable>(),"int",2,0),0,0);
        ST.Insert("getc",new Function(new ArrayList<Variable>(),"char",2,0),0,0);
        List<Variable> vars4 = new ArrayList<Variable>();
        vars4.add(new Variable("int",false,0,16,null));
        vars4.add(new Variable("char",true,1,20,new ArrayList<Integer>()));
        ST.Insert("gets",new Function(vars4,"nothing",2,8),0,0);
        List<Variable> vars5 = new ArrayList<Variable>();
        vars5.add(new Variable("int",false,0,16,null));
        ST.Insert("abs",new Function(vars5,"int",2,4),0,0);
        List<Variable> vars6 = new ArrayList<Variable>();
        vars6.add(new Variable("char",false,0,16,null));
        ST.Insert("ord",new Function(vars6,"int",2,1),0,0);
        List<Variable> vars7 = new ArrayList<Variable>();
        vars7.add(new Variable("int",false,0,16,null));
        ST.Insert("chr",new Function(vars7,"char",2,4),0,0);

        List<Variable> vars8 = new ArrayList<Variable>();
        vars8.add(new Variable("char",true,1,16,new ArrayList<Integer>()));
        ST.Insert("strlen",new Function(vars8,"int",2,4),0,0);

        List<Variable> vars9 = new ArrayList<Variable>();
        vars9.add(new Variable("char",true,1,16,new ArrayList<Integer>()));
        vars9.add(new Variable("char",true,1,17,new ArrayList<Integer>()));
        ST.Insert("strcmp",new Function(vars9,"int",2,8),0,0);

        ST.Insert("strcpy",new Function(vars9,"nothing",2,7),0,0);
        ST.Insert("strcat",new Function(vars9,"nothing",2,8),0,0);
        this.ST.Enter();
    }

    @Override
    public void outAProgram(AProgram node) {
        FunType x = this.ret.pop();
        //Check if a functions return the correct type
        if(x.function_name.equals(node.getFunName().getText())){
            if(x.flag == false) {
                if (!x.type.equals("nothing")) {
                    System.out.println(" Expecting function "+ node.getFunName().toString().trim() + " return " + x.type);
                    System.exit(1);
                }
            }
        }
        else{
            this.ret.push(x);
        }
        //If there are undefined functions, throw an error and exit
        List<String> undef = this.ST.Get_UndefFun();
        if(undef.size()>0){
            System.out.println("There is a header for function "+undef.get(0) +" but not definition");
            System.exit(4);
        }
    }

    @Override
    public void inAFuncDefLocalDef(AFuncDefLocalDef node)
    {
        int count_bytes = 0;
        HashMap<String,Type> temp = new HashMap<String, Type>();
        List<Variable> vars = new ArrayList<Variable>();
        int c_var =0;
        //Create List of arguments for functions
        for(int i = 0;i<node.getParamInfo().size();i++){
            AFunInfo x =  (AFunInfo) node.getParamInfo().get(i);
            List<Integer> dim = null;
            for(int j=0;j<x.getVariable().size();j++){
                c_var++;
                if(j == 0){
                    int k;
                    if(x.getLpin()!=null){          //First dimension missing
                        k=0;
                    }
                    else{
                        k=1;
                    }
                    dim = new ArrayList<Integer>();
                    for(;k<x.getNumber().size();k++){                               //Hold the space of each dimension
                        dim.add(Integer.parseInt(x.getNumber().get(k).getText()));
                    }
                }
                int y = x.getNumber().size();
                if(x.getLpin() != null){
                    y +=1;
                }
                // Create info for each variable (name,ref,position in activation record,dimensions)
                if (x.getRef() == null) {   //Not by ref variable
                    Variable var = new Variable(x.getDataType().toString().trim(),false,y,count_bytes+16,dim);
                    if(y > 0){      //Arrays pass by ref
                        System.out.println("Error:"+this.line+": In fuction: "
                                +node.getFunName().getText()+" in parameter "+Integer.toString(c_var)+
                                " try pass by value an array of "+x.getDataType().toString().trim());
                        System.exit(4);
                        if(count_bytes % 4 != 0){
                            count_bytes += 4 - (count_bytes % 4);
                        }
                        count_bytes +=4;
                    }
                    else if(x.getDataType().toString().trim().equals("int")){
                        if(count_bytes % 4 != 0){
                            count_bytes += 4 - (count_bytes % 4);
                        }
                        count_bytes+=4;
                    }
                    else{
                        count_bytes+=1;
                    }
                    vars.add(var);
                    temp.put(x.getVariable().get(j).getText(),var);     //Insert a variable in Symbol Table
                }
                else {  //By ref variable
                    Variable var = new Variable(x.getDataType().toString().trim(),true,y,count_bytes+16,dim);
                    if(count_bytes % 4 != 0){
                        count_bytes += 4 - (count_bytes % 4);
                    }
                    count_bytes+=4;
                    vars.add(var);
                    temp.put(x.getVariable().get(j).getText(),var);
                }
            }
        }
        Function function = new Function(vars,node.getReturnType().toString().trim(),2,count_bytes);
        this.ST.Insert(node.getFunName().getText(),function,this.line,0);
        this.ret.push(new FunType(node.getFunName().getText(),node.getReturnType().toString().trim()));
        Info_Table info = new Info_Table(count_bytes,temp);
        this.ST.Enter_After(info);
    }

    @Override
    public void outAFuncDefLocalDef(AFuncDefLocalDef node)
    {
        FunType x = this.ret.pop();
        if(!x.type.equals("nothing")) {
            if (x.flag == false) {
                System.out.println("Error:" + this.line + " Expecting return " + x.type);
                System.exit(1);
            }
        }

        List<String> undef = this.ST.Get_UndefFun();
        if(undef.size()>0){
            System.out.println("Error:"+this.line+": There is a header for function "+undef.get(0) +" but not " +
                    "definition in the same scope");
            System.exit(4);
        }
    }

    @Override
    public void caseAFuncDefLocalDef(AFuncDefLocalDef node)
    {
        node.getReturnType().apply(this);
        inAFuncDefLocalDef(node);
        {
            List<PFunInfo> copy = new ArrayList<PFunInfo>(node.getParamInfo());
            for(PFunInfo e : copy)
            {
                e.apply(this);
            }
        }
        {
            List<PLocalDef> copy = new ArrayList<PLocalDef>(node.getLocalDef());
            for(PLocalDef e : copy)
            {
                e.apply(this);
            }
        }
        this.TempPos = this.ST.Get_TempVarPos();
        this.Quads.GenQuad("unit",node.getFunName().getText(),"-","-");
        {
            List<PStmt> copy = new ArrayList<PStmt>(node.getStmt());
            for(PStmt e : copy)
            {
                e.apply(this);
            }
        }
        outAFuncDefLocalDef(node);
        this.Quads.GenQuad("endu",node.getFunName().getText(),"-","-");
        if(this.opt_flag) {
            this.Quads.list = this.opt.Optimize((ArrayList<Quad>) this.Quads.list, this.Quads.In, this.Quads.Out, this.ST);
        }
        this.Quads.Print(this.ST.Get_Nest());                                   //Write quad into Quad file
        this.q.Generator(this.Quads,this.ST,this.TempPos,this.ST.Get_Nest());   //Generate assembly x86 code
        this.Quads.Clear();                                                     //Clear the trash
        this.ST.Exit();
    }

    @Override
    public void inAHeaderLocalDef(AHeaderLocalDef node)
    {
        int count_bytes = 0;
        List<Variable> vars = new ArrayList<Variable>();
        int c_var =0;
        this.line = node.getFunName().getLine();
        for(int i = 0;i<node.getParamInfo().size();i++) {
            List<Integer> dim = null;
            AFunInfo x = (AFunInfo) node.getParamInfo().get(i);
            for (int j = 0; j < x.getVariable().size(); j++) {
                if(j == 0){
                    int k;
                    if(x.getLpin()!=null){
                        k=0;
                    }
                    else{
                        k=1;
                    }
                    dim = new ArrayList<Integer>();
                    for(;k<x.getNumber().size();k++){
                        dim.add(Integer.parseInt(x.getNumber().get(k).getText()));
                    }
                }
                c_var+=1;
                int y = x.getNumber().size();
                if (x.getLpin() != null) {
                    y += 1;
                }
                if (x.getRef() == null) {
                    vars.add(new Variable(x.getDataType().toString().trim(), false, y,count_bytes+16,dim));
                    if(y > 0){
                        System.out.println("Error:"+this.line+": In fuction: "
                                +node.getFunName().getText()+" in parameter "+ Integer.toString(c_var)+
                                " try pass by value an array of "+x.getDataType().toString().trim());
                        System.exit(4);
                    }
                    else if(x.getDataType().toString().trim().equals("int")){
                        if(count_bytes % 4 != 0){
                            count_bytes += 4 - (count_bytes % 4);
                        }
                        count_bytes+=4;
                    }
                    else{
                        count_bytes+=1;
                    }
                }
                else {
                    vars.add(new Variable(x.getDataType().toString().trim(), true, y,count_bytes+16,dim));
                    if(count_bytes % 4 != 0){
                        count_bytes += 4 - (count_bytes % 4);
                    }
                    count_bytes+=4;
                }
            }
        }
        this.ST.Insert(node.getFunName().getText(),new Function(vars,node.getReturnType().toString().trim(),1,count_bytes),this.line,0);
    }


    public void inAVarDeclLocalDef(AVarDeclLocalDef node) {
        int size = 0;
        List<Integer> dim = null;
        for (int i = 0; i < node.getVariable().size(); i++) {
            String name = node.getVariable().get(i).getText();
            if (i == 0){
                this.line =node.getVariable().get(i).getLine();
                if (node.getNumber().size() > 0) {
                    if (node.getDataType().toString().trim().equals("int")) {
                        size = 4;
                    } else {
                        size = 1;
                    }
                    for (int j = 0; j < node.getNumber().size(); j++) {
                        if(j == 0){
                            dim = new ArrayList<Integer>();
                        }
                        else{
                            dim.add(Integer.parseInt(node.getNumber().get(j).getText().trim()));
                        }
                        size *= Integer.parseInt(node.getNumber().get(j).getText().trim());
                    }
                }
                else {
                    if (node.getDataType().toString().trim().equals("int")) {
                        size = 4;
                    } else {
                        size = 1;
                    }
                }
            }
            this.ST.Insert(name, new Variable(node.getDataType().toString().trim(), false,
                            node.getNumber().size(),0,dim), this.line,size);
        }
    }

    public void inAVariableValue(AVariableValue node)
    {
        this.line = node.getVariable().getLine();
        DataType x = this.ST.Lookup(node.getVariable().getText(),null,this.line);
        this.dim = x.dim;
        this.QInfo = new QuadInfo();
        QInfo.place = node.getVariable().getText();
    }

    @Override
    public void caseAPlusExpr(APlusExpr node)
    {
        node.getLeft().apply(this);
        if(!type.equals("int")){
            System.out.println("Error:"+this.line+": Expecting int: Addition only with int");
            System.exit(5);
        }
        if(this.dimension != 0){
            System.out.println("Error:"+this.line+": Expecting int: Not an array of int");
            System.exit(5);
        }
        String lresult = QInfo.place;
        node.getRight().apply(this);
        if(!type.equals("int")){
            System.out.println("Error:"+this.line+": Expecting int: Addition only with int");
            System.exit(5);
        }
        if(this.dimension != 0){
            System.out.println("Error:"+this.line+": Expecting int: Not an array of int");
            System.exit(5);
        }
        String rresult = QInfo.place;
        if (this.TempPos % 4 != 0) {
            this.TempPos += 4 - (this.TempPos % 4);
        }
        this.TempPos +=4;
        QInfo.place = this.Quads.NewTemp("int",-this.TempPos,false);
        this.Quads.GenQuad("+",lresult,rresult,QInfo.place);
    }

    @Override
    public void caseAMinusExpr(AMinusExpr node)
    {
        node.getLeft().apply(this);
        if(!type.equals("int")){
            System.out.println("Error:"+this.line+": Expecting int: Subtraction only with int");
            System.exit(5);
        }
        if(this.dimension != 0){
            System.out.println("Error:"+this.line+": Expecting int: Not an array of int");
            System.exit(5);
        }
        String lresult = QInfo.place;
        node.getRight().apply(this);
        if(!type.equals("int")){
            System.out.println("Error:"+this.line+": Expecting int: Subtraction only with int");
            System.exit(5);
        }
        if(this.dimension != 0){
            System.out.println("Error:"+this.line+": Expecting int: Not an array of int");
            System.exit(5);
        }
        String rresult = QInfo.place;
        if (this.TempPos % 4 != 0) {
            this.TempPos += 4 - (this.TempPos % 4);
        }
        this.TempPos +=4;
        QInfo.place = this.Quads.NewTemp("int",-this.TempPos,false);
        this.Quads.GenQuad("-",lresult,rresult,QInfo.place);
    }

    @Override
    public void caseAMultExpr(AMultExpr node)
    {
        node.getLeft().apply(this);
        if(!type.equals("int")){
            System.out.println("Error:"+this.line+": Expecting int: Multiplication only with int");
            System.exit(5);
        }
        if(this.dimension != 0){
            System.out.println("Error:"+this.line+": Expecting int: Not an array of int");
            System.exit(5);
        }
        String lresult = QInfo.place;
        node.getRight().apply(this);
        if(!type.equals("int")){
            System.out.println("Error:"+this.line+": Expecting int: Multiplication only with int");
            System.exit(5);
        }
        if(this.dimension != 0){
            System.out.println("Error:"+this.line+": Expecting int: Not an array of int");
            System.exit(5);
        }
        String rresult = QInfo.place;
        if (this.TempPos % 4 != 0) {
            this.TempPos += 4 - (this.TempPos % 4);
        }
        this.TempPos +=4;
        QInfo.place = this.Quads.NewTemp("int",-this.TempPos,false);
        this.Quads.GenQuad("*",lresult,rresult,QInfo.place);
    }

    @Override
    public void caseADivExpr(ADivExpr node)
    {
        node.getLeft().apply(this);
        if(!type.equals("int")){
            System.out.println("Error:"+this.line+": Expecting int: Division only with int");
            System.exit(5);
        }
        if(this.dimension != 0){
            System.out.println("Error:"+this.line+": Expecting int: Not an array of int");
            System.exit(5);
        }
        String lresult = QInfo.place;


        node.getRight().apply(this);
        if(!type.equals("int")){
            System.out.println("Error:"+this.line+": Expecting int: Division only with int");
            System.exit(5);
        }
        if(this.dimension != 0){
            System.out.println("Error:"+this.line+": Expecting int: Not an array of int");
            System.exit(5);
        }

        String rresult = QInfo.place;
        if (this.TempPos % 4 != 0) {
            this.TempPos += 4 - (this.TempPos % 4);
        }
        this.TempPos +=4;
        QInfo.place = this.Quads.NewTemp("int",-this.TempPos,false);
        this.Quads.GenQuad("/",lresult,rresult,QInfo.place);
    }

    @Override
    public void caseAModExpr(AModExpr node)
    {
        node.getLeft().apply(this);
        if(!this.type.equals("int")){
            System.out.println("Error:"+this.line+": Expecting int: Modulation only with int");
            System.exit(5);
        }
        if(this.dimension != 0){
            System.out.println("Error:"+this.line+": Expecting int: Not an array of int");
            System.exit(5);
        }
        String lresult = QInfo.place;

        node.getRight().apply(this);
        if(!this.type.equals("int")){
            System.out.println("Error:"+this.line+": Expecting int: Modulation only with int");
            System.exit(5);
        }
        if(this.dimension != 0){
            System.out.println("Error:"+this.line+": Expecting int: Not an array of int");
            System.exit(5);
        }
        String rresult = QInfo.place;
        if (this.TempPos % 4 != 0) {
            this.TempPos += 4 - (this.TempPos % 4);
        }
        this.TempPos +=4;
        QInfo.place = this.Quads.NewTemp("int",-this.TempPos,false);
        this.Quads.GenQuad("%",lresult,rresult,QInfo.place);
    }

    @Override
    public void caseASignExpr(ASignExpr node)
    {

        node.getExpr().apply(this);
        if(this.dimension != 0){
            System.out.println("Error:"+this.line+": Expecting int: Not an array of int");
            System.exit(5);
        }
        if(!this.type.equals("int")){
            System.out.println("Error:"+this.line+": Expecting int: Put sign only to int");
            System.exit(5);
        }
        if(node.getSign().toString().trim().equals("-")){
            String temp = this.QInfo.place;
            if (this.TempPos % 4 != 0) {
                this.TempPos += 4 - (this.TempPos % 4);
            }
            this.TempPos +=4;
            QInfo.place = this.Quads.NewTemp("int",-this.TempPos,false);
            this.Quads.GenQuad("-","0",temp,QInfo.place);
        }

    }

    @Override
    public void caseANumberExpr(ANumberExpr node)
    {
        this.type = "int";
        this.line = node.getNumber().getLine();
        this.dimension = 0;
        QInfo.place = node.getNumber().getText();

    }

    @Override
    public void caseAConsCharExpr(AConsCharExpr node)
    {

        this.type = "char";
        this.line = node.getConsChar().getLine();
        this.dimension = 0;
        QInfo.place = node.getConsChar().getText();
    }

    @Override
    public void caseAStringValue(AStringValue node)
    {
        this.type = "char";
        this.line = node.getString().getLine();
        this.dimension = 1;
        this.dim = null;
        QInfo.place = "["+this.Quads.NewTempString("String",0,node.getString().getText())+"]";

    }

    @Override
    public void caseAVariableValue(AVariableValue node)
    {
        this.line = node.getVariable().getLine();
        DataType x = this.ST.Lookup(node.getVariable().getText(),null,this.line);
        if(x != null) {
            this.type = x.type;
            this.dimension = x.dimension;
            this.dim = x.dim;
        }
        QInfo.place = node.getVariable().getText();
    }

    @Override
    public void caseALValueExpr(ALValueExpr node)
    {
        String value_type;
        List <Integer> dim;
        int value_dimension;
        node.getValue().apply(this);
        dim = this.dim;
        String value_place = QInfo.place;
        value_dimension = this.dimension;
        value_type = this.type;
        String dest = "";
        int flag = 0;
        {
            List<PExpr> copy = new ArrayList<PExpr>(node.getExpr());
            String temp = "";
            int count = 0;
            if(dim != null && dim.size() > 0)
                flag = 1;
            for(PExpr e : copy)
            {
                count++;
                e.apply(this);
                if(!this.type.equals("int")){
                    System.out.println("Error:"+this.line+": Expecting int: get "+this.type+", wrong indexing the array " +
                            "in dimension "+count);
                    System.exit(5);
                }
                if(this.dimension != 0){
                    System.out.println("Error:"+this.line+": Expecting int: get array of int, wrong indexing the array " +
                            "in dimension "+count);
                    System.exit(5);
                }
                if(flag == 1){
                    if(count == 1){
                        if(this.TempPos % 4 != 0){
                            this.TempPos += 4 -(this.TempPos % 4);
                        }
                        this.TempPos += 4;
                        temp = this.Quads.NewTemp("int", -this.TempPos,false);
                        this.Quads.GenQuad("*",QInfo.place ,dim.get(count-1).toString(),temp);
                        dest = temp;
                    }
                    else{
                        if(this.TempPos % 4 != 0){
                            this.TempPos += 4 -(this.TempPos % 4);
                        }
                        this.TempPos += 4;
                        dest = this.Quads.NewTemp("int", -this.TempPos,false);
                        this.Quads.GenQuad("+",temp,QInfo.place,dest);
                        if(count <= dim.size()) {
                            if(this.TempPos % 4 != 0){
                                this.TempPos += 4 -(this.TempPos % 4);
                            }
                            this.TempPos += 4;
                            temp = this.Quads.NewTemp("int", -this.TempPos,false);
                            this.Quads.GenQuad("*", dest, dim.get(count-1).toString(), temp);
                            dest = temp;
                        }
                    }
                }
                else{
                    if (this.TempPos % 4 != 0) {
                        this.TempPos += 4 - (this.TempPos % 4);
                    }
                    this.TempPos += 4;
                    dest = this.Quads.NewTemp(value_type, -this.TempPos,true);
                    this.Quads.GenQuad("array",value_place,QInfo.place,dest);
                }
            }
            if(!dest.equals("")){
                if(flag == 1){
                    while(count<dim.size()){
                        if (this.TempPos % 4 != 0) {
                            this.TempPos += 4 - (this.TempPos % 4);
                        }
                        this.TempPos += 4;
                        temp = this.Quads.NewTemp("int", -this.TempPos,true);
                        this.Quads.GenQuad("*",dim.get(count).toString(),dest,temp);
                        count++;
                        dest = temp;
                    }
                    if (this.TempPos % 4 != 0) {
                        this.TempPos += 4 - (this.TempPos % 4);
                    }
                    this.TempPos += 4;
                    temp = this.Quads.NewTemp(value_type, -this.TempPos,true);
                    this.Quads.GenQuad("array",value_place,dest,temp);
                    dest = temp;
                }
                QInfo.place = "["+dest+"]";
            }
        }
        this.type = value_type;
        this.dimension = value_dimension - node.getExpr().size();
    }

    public void caseAFuncCallExpr(AFuncCallExpr node)
    {
        this.line = node.getFunName().getLine();
        List<DataType> list = new ArrayList<DataType>();
        List <String> place = new ArrayList<String>();
        {
            List<PExpr> copy = new ArrayList<PExpr>(node.getExpr());
            for(PExpr e : copy)
            {
                e.apply(this);
                if(e instanceof ALValueExpr){
                    if(this.dimension > 1){
                        list.add(new DataType(this.type, this.dimension, true,this.dim));
                    }
                    else{
                        list.add(new DataType(this.type, this.dimension, true,null));
                    }
                }
                else
                    list.add(new DataType(this.type,this.dimension,false));
                place.add(QInfo.place);
            }
        }
        DataType x = this.ST.Lookup(node.getFunName().getText(),list,this.line);
        for(int i=place.size()-1;i>=0;i--){
            if(list.get(i).dimension > 0){
                this.Quads.GenQuad("par",place.get(i),"R","-");
            }
            else {
                if(x.k.get(i).ref)
                    this.Quads.GenQuad("par", place.get(i),"R","-");
                else
                    this.Quads.GenQuad("par", place.get(i),"V","-");
            }
        }
        if(!x.type.equals("nothing")){
            if (this.TempPos % 4 != 0) {
                this.TempPos += 4 - (this.TempPos % 4);
            }
            this.TempPos +=4;
            QInfo.place = this.Quads.NewTemp(x.type,-this.TempPos,false);
            this.Quads.GenQuad("par",QInfo.place,"RET","-");
        }
        this.Quads.GenQuad("call","-","-",node.getFunName().getText());
        this.type = x.type;
        this.dimension = x.dimension;
    }


    @Override
    public void caseAAssignStmt(AAssignStmt node)
    {
        node.getValue().apply(this);
        String value_type = this.type;
        List <Integer> dim = this.dim;
        String value_place = QInfo.place;
        int value_dimension = this.dimension;
        String dest ="";
        String temp = "";
        int count = 0;
        int flag = 0;
        {
            List<PExpr> copy = new ArrayList<PExpr>(node.getArrayIndex());
            if(dim != null && dim.size() > 0)
                flag =1;
            for(PExpr e : copy)
            {
                count++;
                e.apply(this);
                if(!this.type.equals("int")){
                    System.out.println("Error:"+this.line+": Expecting int, get char for indexing an array in assignment"
                    + "in dimension "+count);
                    System.exit(4);
                }
                if(this.dimension != 0){
                    System.out.println("Error:"+this.line+": Expecting int, get array of int for indexing an array " +
                            "in assignment in dimension "+count);
                    System.exit(4);
                }
                if(flag == 1 ){
                    if(count == 1){
                        if (this.TempPos % 4 != 0) {
                            this.TempPos += 4 - (this.TempPos % 4);
                        }
                        this.TempPos += 4;
                        temp = this.Quads.NewTemp("int", -this.TempPos,false);
                        this.Quads.GenQuad("*",QInfo.place ,dim.get(count-1).toString(),temp);
                        dest = temp;
                    }
                    else if(count >=2){
                        if (this.TempPos % 4 != 0) {
                            this.TempPos += 4 - (this.TempPos % 4);
                        }
                        this.TempPos += 4;
                        dest = this.Quads.NewTemp("int", -this.TempPos,false);
                        this.Quads.GenQuad("+",temp,QInfo.place,dest);
                        if(count <= dim.size()) {
                            if (this.TempPos % 4 != 0) {
                                this.TempPos += 4 - (this.TempPos % 4);
                            }
                            this.TempPos += 4;
                            temp = this.Quads.NewTemp("int", -this.TempPos,false);
                            this.Quads.GenQuad("*", dest, dim.get(count - 1).toString(), temp);
                            dest = temp;
                        }
                    }
                }
                else{
                    if (this.TempPos % 4 != 0) {
                        this.TempPos += 4 - (this.TempPos % 4);
                    }
                    this.TempPos += 4;
                    dest = this.Quads.NewTemp(value_type, -this.TempPos,true);
                    this.Quads.GenQuad("array",value_place,QInfo.place,dest);
                }
            }
            if(flag == 1){
                while(count<dim.size()){
                    if (this.TempPos % 4 != 0) {
                        this.TempPos += 4 - (this.TempPos % 4);
                    }
                    this.TempPos += 4;
                    temp = this.Quads.NewTemp("int", -this.TempPos,true);
                    this.Quads.GenQuad("*",dim.get(count).toString(),dest,temp);
                    count++;
                    dest = temp;
                }
                if (this.TempPos % 4 != 0) {
                    this.TempPos += 4 - (this.TempPos % 4);
                }
                this.TempPos += 4;
                temp = this.Quads.NewTemp(value_type, -this.TempPos,true);
                this.Quads.GenQuad("array",value_place,dest,temp);
                dest = temp;
            }
        }
        if((value_dimension - node.getArrayIndex().size()) > 0){
            System.out.println("Error:"+this.line+": Can't assignment arrays!");
            System.exit(4);
        }
        else if((value_dimension - node.getArrayIndex().size()) < 0){
            System.out.println("Error:"+this.line+": Expecting "+value_type+" with "+value_dimension+" dimensions and " +
                    "get " + value_type+" with "+ node.getArrayIndex().size()+" dimensions");
            System.exit(4);
        }

        if(dest.equals("")){
            dest = this.QInfo.place;
        }
        else{
            dest = "[" + dest + "]";
        }
        node.getAssignValue().apply(this);
        if(!this.type.equals(value_type)){
            System.out.println("Error:"+this.line+": Expecting "+value_type +" and get "+this.type+", " +
                    "assignment a different type of expected!");
            System.exit(6);
        }
        if(this.dimension!=0){
            System.out.println("Error:"+this.line+": Can't assignment arrays!");
            System.exit(4);
        }
        this.Quads.GenQuad(":=",QInfo.place,"-",dest);
    }

    @Override
    public void caseAFuncCallStmt(AFuncCallStmt node)
    {
        List<DataType> list = new ArrayList<DataType>();
        List <String> place = new ArrayList<String>();
        {
            List<PExpr> copy = new ArrayList<PExpr>(node.getExpr());
            for(PExpr e : copy)
            {
                e.apply(this);
                if(e instanceof ANumberExpr || e instanceof AConsCharExpr)
                    list.add(new DataType(this.type,this.dimension,false));
                else {
                    if(this.dimension > 1){
                        list.add(new DataType(this.type, this.dimension, true,this.dim));
                    }
                    else{
                        list.add(new DataType(this.type, this.dimension, true,null));
                    }
                }
                place.add(QInfo.place);
            }
        }
        DataType x = this.ST.Lookup(node.getVariable().getText(),list,this.line);
        for(int i=place.size()-1;i>=0;i--){
            if(list.get(i).dimension > 0){
                this.Quads.GenQuad("par",place.get(i),"R","-");
            }
            else {
                if(x.k.get(i).ref)
                    this.Quads.GenQuad("par", place.get(i),"R","-");
                else
                    this.Quads.GenQuad("par", place.get(i),"V","-");
            }
        }
        if(!x.type.equals("nothing")){
            if (this.TempPos % 4 != 0) {
                this.TempPos += 4 - (this.TempPos % 4);
            }
            this.TempPos +=4;
            QInfo.place = this.Quads.NewTemp(x.type,-this.TempPos,false);
            this.Quads.GenQuad("par",QInfo.place,"RET","-");
        }
        this.Quads.GenQuad("call","-","-",node.getVariable().getText());
    }

    @Override
    public void caseAReturnStmt(AReturnStmt node)
    {
        FunType x = this.ret.pop();
        if(node.getExpr() != null) {
            node.getExpr().apply(this);
            if (!x.type.equals(this.type)) {
                if(x.type.equals("nothing")) {
                    System.out.println("Error:"+this.line+": Not expecting return something in fuction " + x.function_name +
                            " and get return " + this.type);
                    System.exit(7);
                }
                else{
                    System.out.println("Error:"+this.line+": Expecting return "+ x.type +" in fuction " + x.function_name +
                            " and get return " + this.type);
                    System.exit(7);
                }
            }
            if(this.dimension != 0){
                System.out.println("Error:"+this.line+": Expecting return "+ x.type +" in fuction " + x.function_name +
                        " and get return array of " + this.type);
                System.exit(7);
            }
            this.Quads.GenQuad(":=",this.QInfo.place,"-","$$");
        }
        else{
            if(!x.type.equals("nothing")){
                System.out.println(" Expecting return "+ x.type +" in fuction " + x.function_name +
                        " and not return anything");
                System.exit(7);
            }
        }

        this.Quads.GenQuad("ret", "-", "-", "-");
        x.flag = true;
        this.ret.push(x);
    }

    @Override
    public void caseAIfStmt(AIfStmt node)
    {
        node.getCondition().apply(this);
        List<Integer> L1 = this.QInfo.False;
        List<Integer> cond_False = L1;
        List<Integer> stmt1_Next = null;
        List<Integer> L2 = this.Quads.EmptyList();
        this.Quads.Backpatch(this.QInfo.True,this.Quads.NextQuad());
        {
            List<PStmt> copy = new ArrayList<PStmt>(node.getThen());
            for(PStmt e : copy)
            {
                e.apply(this);
            }
            stmt1_Next = this.QInfo.Next;
        }
        {
            List<PStmt> copy = new ArrayList<PStmt>(node.getElse());
            if(node.getElse().size() >0){
                L1 = this.Quads.MakeList(this.Quads.NextQuad());
                this.Quads.GenQuad("jump","-","-","*");
                this.Quads.Backpatch(cond_False,this.Quads.NextQuad());
            }
            for(PStmt e : copy)
            {
                e.apply(this);
            }
            if(node.getElse().size()>0){
                L2 = this.QInfo.Next;
            }
        }
        this.QInfo.Next = Quads.Merge(L1, L2);
        this.QInfo.Next = Quads.Merge(this.QInfo.Next,stmt1_Next);
        this.Quads.Backpatch(this.QInfo.Next,this.Quads.NextQuad());
    }

    public void caseABlockStmt(ABlockStmt node)
    {
        {
            List<PStmt> copy = new ArrayList<PStmt>(node.getBody());
            int count = 0;
            for(PStmt e : copy)
            {
                count++;
                e.apply(this);
                if(count == 1){
                    this.Quads.Backpatch(this.QInfo.Next,this.Quads.NextQuad());
                }
            }
        }
    }

    @Override
    public void caseAWhileStmt(AWhileStmt node)
    {
        int Q = this.Quads.NextQuad();
        node.getCondition().apply(this);
        List<Integer> cond_False = this.QInfo.False;
        this.Quads.Backpatch(this.QInfo.True,this.Quads.NextQuad());
        {
            List<PStmt> copy = new ArrayList<PStmt>(node.getBody());
            for(PStmt e : copy)
            {
                e.apply(this);
            }
        }
        this.Quads.Backpatch(this.QInfo.Next,Q);
        this.Quads.GenQuad("jump","-","-",Integer.toString(Q));
        this.QInfo.Next = cond_False;
        this.Quads.Backpatch(this.QInfo.Next,this.Quads.NextQuad());
    }

    @Override
    public void caseAExprCond(AExprCond node)
    {
        node.getLeft().apply(this);
        String ltype = this.type;
        int ldimenstion = this.dimension;

        if(ldimenstion > 0){
            System.out.printf("Error:%d Expecting %s to compare and get array type of %s\n",
                    this.line,this.type,this.type);
            System.exit(8);
        }
        String lresult = this.QInfo.place;


        node.getRight().apply(this);
        if(!this.type.equals(ltype)){
            System.out.printf("Error:%d Expecting %s to compare and get %s,comparison only with same types!\n",
                    this.line,ltype,this.type);
            System.exit(8);
        }
        if(this.dimension > 0){
            System.out.printf("Error:%d Expecting %s to compare and get array type of %s\n",
                    this.line,ltype,this.type);
            System.exit(8);
        }
        String rresult = this.QInfo.place;
        this.QInfo.True = this.Quads.MakeList(this.Quads.NextQuad());
        this.Quads.GenQuad(node.getComp().toString().trim(),lresult,rresult,"*");
        this.QInfo.False = this.Quads.MakeList(this.Quads.NextQuad());
        this.Quads.GenQuad("jump","-","-","*");

    }

    @Override
    public void caseAOrCond(AOrCond node)
    {
        node.getLeft().apply(this);
        this.Quads.Backpatch(this.QInfo.False,this.Quads.NextQuad());
        List<Integer> cond1_true = this.QInfo.True;

        node.getRight().apply(this);
        this.QInfo.True = this.Quads.Merge(cond1_true,this.QInfo.True);
    }

    @Override
    public void caseAAndCond(AAndCond node)
    {
        node.getLeft().apply(this);
        this.Quads.Backpatch(this.QInfo.True,this.Quads.NextQuad());
        List<Integer> cond1_false = this.QInfo.False;

        node.getRight().apply(this);
        this.QInfo.False = this.Quads.Merge(cond1_false,this.QInfo.False);
    }

    @Override
    public void caseANotCond(ANotCond node)
    {
        node.getCond().apply(this);
        List<Integer> temp = this.QInfo.True;
        this.QInfo.True = this.QInfo.False;
        this.QInfo.False = temp;

    }

    @Override
    public void caseAIntRetType(AIntRetType node)
    {
       this.line = node.getInt().getLine();
    }

    @Override
    public void caseACharRetType(ACharRetType node)
    {
        this.line = node.getChar().getLine();
    }

    @Override
    public void caseANothRetType(ANothRetType node)
    {
        this.line = node.getNothing().getLine();
    }

   }