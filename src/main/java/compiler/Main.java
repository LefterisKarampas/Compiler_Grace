
package compiler;

import compiler.parser.*;
import compiler.lexer.Lexer;
import compiler.node.*;
import compiler.lexer.LexerException;
import compiler.parser.ParserException;
import java.io.*;
import java.util.*;

public class Main {
        public static void main(String args[]) {
			FileInputStream inputStream = null;
			if(args.length < 1) {
				System.out.printf("Error: No argument-file to compile!\n");
				System.exit(6);
			}
			try {
				inputStream = new FileInputStream(args[0]);
			}
			catch (IOException e){
				e.printStackTrace();
				System.exit(4);
			}
			Start tree = null;
                Parser p =
	    new Parser(
	    new Lexer(
	    new PushbackReader(
	    new InputStreamReader(inputStream), 1024)));


	   // Parse the input
		try{
		tree = p.parse();
		}
		catch (LexerException k) {
			System.err.printf("Lexing error: %s\n", k.getMessage());
			System.exit(1);
		}
		catch (ParserException q) {
			System.err.printf("Parsing error: %s\n", q.getMessage());
			System.exit(2);
		}
		catch(Exception e) {
			e.printStackTrace();
			System.exit(3);
		}
		if(args.length == 2){
			tree.apply(new Visitor(false));
		}
		else{
			tree.apply(new Visitor(true));
		}
		System.exit(0);
	}
}
