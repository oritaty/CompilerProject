import java.util.ArrayList;

class CodeFactory {
	private static int tempCount;
	private static ArrayList<String> intVariablesList;
	private static ArrayList<String> stringVariablesList;
	private static int labelCount = 0;
	private static boolean firstWrite = true;
	
	private SymbolTable symbolTable; // Reference to the symbol table in Parser
	private boolean usesStrCpy, usesWriteStr, usesConcat;

	public CodeFactory(SymbolTable symbolTable) {
		tempCount = 0;
		intVariablesList = new ArrayList<String>();
		stringVariablesList = new ArrayList<String>();
		this.symbolTable = symbolTable;
	}

	void generateIntDeclaration(Token token) {
		intVariablesList.add(token.getId());
	}
	
	void generateStringDeclaration(Token token) {
		stringVariablesList.add(token.getId());
	}

	Expression generateArithExpr(Expression left, Expression right, Operation op) {
		Expression tempExpr = new Expression(Expression.TEMPEXPR, createTempName());
		if (right.expressionType == Expression.LITERALEXPR) {
			System.out.println("\tMOVL " + "$" + right.expressionName + ", %ebx");
		} else {
			System.out.println("\tMOVL " + right.expressionName + ", %ebx");
		}
		if (left.expressionType == Expression.LITERALEXPR) {
			System.out.println("\tMOVL " + "$" + left.expressionName + ", %eax");
		} else {
			System.out.println("\tMOVL " + left.expressionName + ", %eax");
		}
		if (op.opType == Token.PLUS) {
			System.out.println("\tADD %ebx, %eax");
			
		} else if (op.opType == Token.MINUS) {
			System.out.println("\tSUB %ebx, %eax");
		}
		System.out.println("\tMOVL " + "%eax, " + tempExpr.expressionName);
		return tempExpr;
	}
	
	// Call a helper assembly method to concatenate two strings into a temporary string
	StringExpression generateConcatExpr(StringExpression left, StringExpression right) {
		String resultName = createStringTemp();
		StringExpression result = new StringExpression(StringExpression.IDEXPR, resultName);
		System.out.println("\n\tPUSHL $" + left.expressionName);	// Push left operand address
		System.out.println("\tPUSHL $" + right.expressionName);	// Push right operand address
		System.out.println("\tPUSHL $" + resultName);	// Push destination address
		System.out.println("\tCALL __concat");
		usesConcat = true;
		return result;
	}

	void generateWrite(Expression expr) {
		switch (expr.expressionType) {
		case Expression.IDEXPR:
		case Expression.TEMPEXPR: {
			generateAssemblyCodeForWriting(expr.expressionName);
			break;
		}
		case Expression.LITERALEXPR: {
			System.out.println("write " + expr.expressionIntValue);
		}
		}
	}
	
	void generateStringWrite(StringExpression expr) {
		System.out.println("\n\tPUSHL $" + expr.expressionName); // Push string source onto stack
		System.out.println("\tCALL __writeStr"); // Use helper method to write string, using its corresponding length variable
		usesWriteStr = true;
	}

	private void generateAssemblyCodeForWriting(String idName) {
		if (!firstWrite) {
			
			System.out.println("\tmovl " + idName + ",%eax");
			System.out.println("\tpushl %eax");
			System.out.println("\tcall __reversePrint    /* The return address is at top of stack! */");
			System.out.println("\tpopl  %eax    /* Remove value pushed onto the stack */");
			
		} else
		// String reverseLoopLabel = generateLabel("reverseLoop");
		{
			firstWrite = false;
			
			System.out.println("\tmovl " + idName + ",%eax");
			System.out.println("\tpushl %eax");
			System.out.println("\tcall __reversePrint    /* The return address is at top of stack! */");
			System.out.println("\tpopl  %eax    /* Remove value pushed onto the stack */");
			System.out.println("\tjmp __writeExit");  /* Needed to jump over the reversePrint code since it was called */

			System.out.println("__reversePrint: ");
			System.out.println("\t/* Save registers this method modifies */");
			System.out.println("\tpushl %eax");
			System.out.println("\tpushl %edx");
			System.out.println("\tpushl %ecx");
			System.out.println("\tpushl %ebx");

			System.out.println("\tcmpw $0, 20(%esp)");
			System.out.println("\tjge __positive");
			System.out.println("\t/* Display minus on console */");
			System.out.println("\tmovl $4, %eax       /* The system call for write (sys_write) */");
			System.out.println("\tmovl $1, %ebx       /* File descriptor 1 - standard output */");
			System.out.println("\tmovl $1, %edx     /* Place number of characters to display */");
			System.out.println("\tmovl $__minus, %ecx   /* Put effective address of stack into ecx */");
			System.out.println("\tint $0x80	    /* Call to the Linux OS */");
			
			System.out.println("\t__positive:");
			System.out.println("\txorl %eax, %eax       /* eax = 0 */");
			System.out.println("\txorl %ecx, %ecx       /* ecx = 0, to track characters printed */");

			System.out.println("\t/** Skip 16-bytes of register data stored on stack and 4 bytes");
			System.out.println("\tof return address to get to first parameter on stack ");
			System.out.println("\t*/   ");
			System.out.println("\tmovw 20(%esp), %ax     /* ax = parameter on stack */");

			System.out.println("\tcmpw $0, %ax");
			System.out.println("\tjge __reverseLoop");
			System.out.println("\tmulw __negOne\n");
			
			System.out.println("__reverseLoop:");

			System.out.println("\tcmpw $0, %ax");
			System.out.println("\tje   __reverseExit");
			System.out.println("\t/* Do div and mod operations */");
			System.out.println("\tmovl $10, %ebx         /* ebx = 10 as divisor  */");
			System.out.println("\txorl %edx, %edx        /* edx = 0 to get remainder */");
			System.out.println("\tidivl %ebx             /* edx = eax % 10, eax /= 10 */");
			System.out.println("\taddb $'0', %dl         /* convert 0..9 to '0'..'9'  */");

			System.out.println("\tdecl %esp              /* use stack to store digit  */");
			System.out.println("\tmovb %dl, (%esp)       /* Save character on stack.  */");
			System.out.println("\tincl %ecx              /* track number of digits.   */");

			System.out.println("\tjmp __reverseLoop");

			System.out.println("__reverseExit:");

			System.out.println("__printReverse:");

			System.out.println("\t/* Display characters on _stack_ on console */");

			System.out.println("\tmovl $4, %eax       /* The system call for write (sys_write) */");
			System.out.println("\tmovl $1, %ebx       /* File descriptor 1 - standard output */");
			System.out.println("\tmovl %ecx, %edx     /* Place number of characters to display */");
			System.out.println("\tleal (%esp), %ecx   /* Put effective address of stack into ecx */");
			System.out.println("\tint $0x80	    /* Call to the Linux OS */");

			System.out.println("\t /* Clean up data and registers on the stack */");
			System.out.println("\taddl %edx, %esp");
			System.out.println("\tpopl %ebx");
			System.out.println("\tpopl %ecx");
			System.out.println("\tpopl %edx");
			System.out.println("\t popl %eax");

			System.out.println("\tret");
			System.out.println("__writeExit:");
		}
	}

	void generateRead(Expression expr) {
		switch (expr.expressionType) {
		case Expression.IDEXPR:
		case Expression.TEMPEXPR: {
			generateAssemblyCodeForReading(expr.expressionName);
			break;
		}
		case Expression.LITERALEXPR: {
			// not possible since you cannot read into a literal. An error
			// should be generated
		}
		}
	}

	private void generateAssemblyCodeForReading(String idName) {
		
		String readLoopLabel = generateLabel("__readLoop");
		String readLoopEndLabel = generateLabel("__readLoopEnd");
		String readEndLabel = generateLabel("__readEnd");
		String readPositiveLabel = generateLabel("__readPositive");
		
		System.out.println("\tmovl $0, " + idName);
		
		System.out.println("\tmovl %esp, %ebp");
		System.out.println("\t/* read first character to check for negative */");
		System.out.println("\tmovl $3, %eax        /* The system call for read (sys_read) */");
		System.out.println("\tmovl $0, %ebx        /* File descriptor 0 - standard input */");
		System.out.println("\tlea 4(%ebp), %ecx      /* Put the address of character in a buffer */");
		System.out.println("\tmovl $1, %edx        /* Place number of characters to read in edx */");
		System.out.println("\tint $0x80	     /* Call to the Linux OS */ ");
		System.out.println("\tmovb 4(%ebp), %al");
		System.out.println("\tcmpb $'\\n', %al      /* Is the newline character? */");
		System.out.println("\tje  " + readEndLabel);
		System.out.println("\tcmpb $'-', %al		/* Is the character '-'? */");
		System.out.println("\tjne " + readPositiveLabel);
		
		System.out.println("\tmovb $'-', __negFlag	");
		System.out.println("\tjmp " + readLoopLabel);
		
		
		System.out.println(readPositiveLabel + ":");
		System.out.println("\tcmpb $'+', %al");
		System.out.println("\tje " + readLoopLabel);
		System.out.println("\t/*Process the first digit that is not a minnus or newline.*/");
		System.out.println("\tsubb $'0', 4(%ebp)      /* Convert '0'..'9' to 0..9 */ \n");

		System.out.println("\t/* result  = (result * 10) + (idName  - '0') */");
		System.out.println("\tmovl $10, %eax");
		System.out.println("\txorl %edx, %edx");
		System.out.println("\tmull " + idName + "        /* result  *= 10 */");
		System.out.println("\txorl %ebx, %ebx    /* ebx = (int) idName */");
		System.out.println("\tmovb 4(%ebp), %bl");
		System.out.println("\taddl %ebx, %eax    /* eax += idName */");
		System.out.println("\tmovl %eax, " + idName);
		
		
		System.out.println(readLoopLabel + ":");
		System.out.println("\tmovl $3, %eax        /* The system call for read (sys_read) */");
		System.out.println("\tmovl $0, %ebx        /* File descriptor 0 - standard input */");
		System.out.println("\tlea 4(%ebp), %ecx      /* Put the address of character in a buffer */");
		System.out.println("\tmovl $1, %edx        /* Place number of characters to read in edx */");
		System.out.println("\tint $0x80	     /* Call to the Linux OS */ \n");

		System.out.println("\tmovb 4(%ebp), %al");
		System.out.println("\tcmpb $'\\n', %al      /* Is the character '\\n'? */");

		
		System.out.println("\tje  " + readLoopEndLabel);
		System.out.println("\tsubb $'0', 4(%ebp)      /* Convert '0'..'9' to 0..9 */ \n");

		System.out.println("\t/* result  = (result * 10) + (idName  - '0') */");
		System.out.println("\tmovl $10, %eax");
		System.out.println("\txorl %edx, %edx");
		System.out.println("\tmull " + idName + "        /* result  *= 10 */");
		System.out.println("\txorl %ebx, %ebx    /* ebx = (int) idName */");
		System.out.println("\tmovb 4(%ebp), %bl");
		System.out.println("\taddl %ebx, %eax    /* eax += idName */");
		System.out.println("\tmovl %eax, " + idName);
		System.out.println("\t/* Read the next character */");
		System.out.println("\tjmp " + readLoopLabel);
		System.out.println(readLoopEndLabel + ":\n");
		System.out.println("\tcmpb $'-', __negFlag");
		System.out.println("\tjne " + readEndLabel);
		System.out.println("\tmovl " + idName + ", %eax");
		System.out.println("\tmull __negOne");
		System.out.println("\tmovl %eax, " + idName);
		System.out.println("\tmovb $'+', __negFlag");
		System.out.println(readEndLabel + ":\n");

	}

	private String generateLabel(String start) {
		String label = start + labelCount++;
		return label;

	}

	void generateAssignment(Expression lValue, Expression expr) {
		if (expr.expressionType == Expression.LITERALEXPR) {
			System.out.println("\tMOVL " + "$" + expr.expressionIntValue + ", %eax");
			System.out.println("\tMOVL %eax, " + lValue.expressionName);
		} else {
			System.out.println("\tMOVL " + expr.expressionName + ", %eax");
			System.out.println("\tMOVL %eax, " + lValue.expressionName);
		}
	}
	
	void generateStringAssignment( StringExpression lValue, StringExpression expr) {
		System.out.println("\n\tPUSHL $" + expr.expressionName); // Push source address
		System.out.println("\tPUSHL $" + lValue.expressionName); // Push destination address
		System.out.println("\tCALL __strcpy");	// Use helper method to copy string from source to destination
		usesStrCpy = true;
	}

	void generateStart() {
		System.out.println(".text\n.global _start\n\n_start:\n");
	}

	void generateExit() {
		System.out.println("exit:");
		System.out.println("\tmov $1, %eax");
		System.out.println("\tmov $1, %ebx");
		System.out.println("\tint $0x80");
		
		// Add __strcpy method if needed
		if (usesStrCpy) {
			System.out.println("\n/* Method to copy string from a source to a destination */");
			System.out.println("__strcpy:");
			System.out.println("\tpopl %ecx	/* Pop return address */");
			System.out.println("\tpopl %ebx	/* Pop destination address */");
			System.out.println("\tpopl %eax	/* Pop source address */");
			System.out.println("\tpushl %ecx	/* Replace return address */");
			System.out.println("\tpushl %ebx	/* Put destination start address on top of stack */");
			System.out.println("__strcpyloop:");
			System.out.println("\tmovb (%eax), %cl");
			System.out.println("\tmovb %cl, (%ebx)	/* Move character */");
			System.out.println("\tcmpb $0, (%eax)");
			System.out.println("\tjz __strcpyend		/* Stop copying at zero character */");
			System.out.println("\tincl %eax");
			System.out.println("\tincl %ebx");
			System.out.println("\tjmp __strcpyloop");
			System.out.println("__strcpyend:");
			System.out.println("\tincl %ebx");
			System.out.println("\tpopl %ecx");
			System.out.println("\tsubl %ecx, %ebx		/* Subtract destination start address to get new length */");
			System.out.println("\tmovl %ebx, 256(%ecx)");
			System.out.println("\tret");
		}
		
		// Add __writeStr method if needed
		if (usesWriteStr) {
			System.out.println("\n/* Method to write string to output */");
			System.out.println("__writeStr:");
			System.out.println("\tpopl %ebx");
			System.out.println("\tpopl %ecx	/* Extract source address */");
			System.out.println("\tpushl %ebx\n");
			System.out.println("\tmovl $4, %eax");
			System.out.println("\tmovl $1, %ebx");
			System.out.println("\tmovl 256(%ecx), %edx	/* Use corresponding length variable */");
			System.out.println("\tdecl %edx");
			System.out.println("\tint $0x80");
			System.out.println("\tret");
		}
		
		// Add __concat method if needed
		if (usesConcat) {
			System.out.println("\n/* Method to concatenate 2 strings */");
			System.out.println("__concat:");
			System.out.println("\tpopl %edx	/* Pop return address */");
			System.out.println("\tpopl %ecx	/* Pop destination address */");
			System.out.println("\tpopl %ebx	/* Pop right operand address */");
			System.out.println("\tpopl %eax	/* Pop left operand address */");
			System.out.println("\tpushl %edx	/* Replace return address */");
			System.out.println("\n\t/* Update string length for destination here */");
			System.out.println("\tmovl 256(%eax), %edx");
			System.out.println("\taddl 256(%ebx), %edx");
			System.out.println("\tdecl %edx");
			System.out.println("\tmovl %edx, 256(%ecx)");
			System.out.println("\n__concatloop1:");
			System.out.println("\tcmpb $0, (%eax)	/* Move on to second operand at zero character */");
			System.out.println("\tjz __concatloop2");
			System.out.println("\tmovb (%eax), %dl");
			System.out.println("\tmovb %dl, (%ecx)	/* Move character */");
			System.out.println("\tincl %eax");
			System.out.println("\tincl %ecx");
			System.out.println("\tjmp __concatloop1");
			System.out.println("\n__concatloop2:");
			System.out.println("\tmovb (%ebx), %dl");
			System.out.println("\tmovb %dl, (%ecx)	/* Move character */");
			System.out.println("\tcmpb $0, (%ebx)");
			System.out.println("\tjz __concatend	/* Stop concatenating at zero character */");
			System.out.println("\tincl %ebx");
			System.out.println("\tincl %ecx");
			System.out.println("\tjmp __concatloop2");
			System.out.println("\n__concatend:");
			System.out.println("\tret	/* Operation finished, so return */");
		}
	}

	public void generateData() {
		System.out.println("\n\n.data");
		System.out.println("/* Int variables */");
		for (String var : intVariablesList) {
			System.out.print(var + ":\t.int ");
			Object initValue = symbolTable.getInitValue(var);
			// Initialize as zero if no initValue is given in symbol table
			System.out.println(initValue == null ? "0" : initValue);
		}
		
		System.out.println("\n/* String variables */");
		for (String var : stringVariablesList) {
			Object initValue = symbolTable.getInitValue(var);
			if (initValue == null) {
				// Initialize empty string, with max length 256
				System.out.println(var + ":\t.zero 256");
				System.out.println("__" + var + "Len:\t.int 1"); // Add length count
			} else {
				if (!(initValue instanceof String))
					continue; // in case of previous compiler error
				// Initialize with literal, and allocate more zeros to make max length 256
				String literal = (String)initValue;
				System.out.println(var + ":\t.string \"" + literal + "\"");
				System.out.println("\t.zero " + (255 - literal.length()));
				System.out.println("__" + var + "Len:\t.int " + (literal.length() + 1));
			}
		}
		
		System.out.println("\n__minus:  .byte '-'");
		System.out.println("__negOne: .int -1");
		System.out.println("__negFlag: .byte '+'");
	}

	private String createTempName() {
		String tempVar = new String("__temp" + tempCount++);
		intVariablesList.add(tempVar);
		return tempVar;
	}
	
	// Public method, since this is also used to create variables for literal values
	public String createStringTemp() {
		String tempVar = new String("__temp" + tempCount++);
		stringVariablesList.add(tempVar);
		return tempVar;
	}

}
