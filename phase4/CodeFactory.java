import java.util.ArrayList;

class CodeFactory {
	private static int tempCount;
	private static ArrayList<String> intVariablesList;
	private static ArrayList<String> stringVariablesList;
	private static ArrayList<String> booleanVariablesList;
	private static int labelCount = 0;
	private static int controlCount = 0;
	private static int compareLabelCount = 0;
	private static boolean firstWrite = true;
	
	private SymbolTable symbolTable; // Reference to the symbol table in Parser
	private boolean usesStrCpy, usesWriteStr, usesConcat, usesReadStr, usesWriteBool;

	public CodeFactory(SymbolTable symbolTable) {
		tempCount = 0;
		intVariablesList = new ArrayList<String>();
		stringVariablesList = new ArrayList<String>();
		booleanVariablesList = new ArrayList<String>();
		this.symbolTable = symbolTable;
	}

	void generateIntDeclaration(Token token) {
		intVariablesList.add(token.getId());
	}
	
	void generateStringDeclaration(Token token) {
		stringVariablesList.add(token.getId());
	}
	
	void generateBooleanDeclaration(Token token) {
		booleanVariablesList.add(token.getId());
	}
	
	// Added phase 4: function call generation
	void generateCall(String functionName) {
		System.out.println("\tCALL " + functionName);
	}
	
	// Added phase 4: instructions at start of function definition
	void generateFunctionStart(String functionName) {
		System.out.println("\tjmp " + functionName + "__end");
		System.out.println(functionName + ":");
		System.out.println("\tpushl %eax");
		System.out.println("\tpushl %ebx");
		System.out.println("\tpushl %ecx");
		System.out.println("\tpushl %edx");
	}
	
	// Added phase 4: instructions at start of function definition
	void generateFunctionEnd(String functionName) {
		System.out.println("\tpopl %edx");
		System.out.println("\tpopl %ecx");
		System.out.println("\tpopl %ebx");
		System.out.println("\tpopl %eax");
		System.out.println("\tret");
		System.out.println(functionName + "__end:");
	}
	
	// Added phase 3: if statement generation
	int generateIf() {
		int id = controlCount++;
		System.out.println("\tcmpb $0, _condition");
		System.out.println("\tje __else" + id);
		return id;
	}
	void generateElse(int id) {
		System.out.println("\tjmp __endif" + id);
		System.out.println("__else" + id + ":");
	}
	void generateEndIf(int id) {
		System.out.println("__endif" + id + ":");
	}
	
	// Added phase 3: while statement generation
	int generateWhile() {
		int id = controlCount++;
		System.out.println("__while" + id + ":");
		return id;
	}
	void generateWhileBody(int id) {
		System.out.println("\tcmpb $1, _condition");
		System.out.println("\tjne __endwhile" + id);
	}
	void generateEndWhile(int id) {
		System.out.println("\tjmp __while" + id);
		System.out.println("__endwhile" + id + ":");
	}
	
	
	Expression generateArithExpr(Expression left, Expression right, Operation op) {
		Expression tempExpr = null;
		boolean isComparison = false;	// To detect whether tempExpr should be int or bool
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
		String targetReg = "%eax, ";
		if (op.opType == Token.PLUS) {
			System.out.println("\tADD %ebx, %eax");	
		} else if (op.opType == Token.MINUS) {
			System.out.println("\tSUB %ebx, %eax");
		} else if (op.opType == Token.MULT) {
			System.out.println("\tIMULL %ebx");
		} else if (op.opType == Token.DIV) {
			System.out.println("\tXORL %edx, %edx");
                        System.out.println("\tIDIVL %ebx");
		} else if (op.opType == Token.MOD) {
			System.out.println("\tXORL %edx, %edx");
                        System.out.println("\tIDIVL %ebx");
                        targetReg = "%edx, ";
        } else if (op.opType >= Token.EQUAL && op.opType <= Token.SMALLER_OR_EQUAL) {
        	// All int comparisons handled here
        	isComparison = true;
        	tempExpr = new Expression(Expression.BOOLTEMPEXPR, createBoolTemp());
        	System.out.println("\tmovb $1, " + tempExpr.expressionName);
        	System.out.println("\tcmpl %eax, %ebx");
        	
        	String jumpType = "";
        	switch (op.opType) {
        	case Token.EQUAL:				jumpType = "je "; break;
        	case Token.NOT_EQUAL:			jumpType = "jne "; break;
        	case Token.GREATER:				jumpType = "jl "; break;
        	case Token.GREATER_OR_EQUAL:	jumpType = "jle "; break;
        	case Token.SMALLER:				jumpType = "jg "; break;
        	case Token.SMALLER_OR_EQUAL:	jumpType = "jge "; break;
        	}
        	
        	String jumpLabel = createCompareLabel();
        	System.out.println("\t" + jumpType + jumpLabel);
        	System.out.println("\tmovb $0, " + tempExpr.expressionName);
        	System.out.println(jumpLabel + ":\n");
        }
		
		if (!isComparison) {
			// This only applied to expressions evaluating to an int
			tempExpr = new Expression(Expression.TEMPEXPR, createTempName());
			System.out.println("\tMOVL " + targetReg + tempExpr.expressionName);
		}
		return tempExpr;
	}
	
	Expression generateBoolExpr(Expression left, Expression right, Operation op) {
		Expression tempExpr = new Expression(Expression.BOOLTEMPEXPR, createBoolTemp());
		if (right.expressionType == Expression.BOOLLITERALEXPR) {
			System.out.println("\tMOVB " + "$" + right.expressionIntValue + ", %bl");
		} else {
			System.out.println("\tMOVB " + right.expressionName + ", %bl");
		}
		if (left.expressionType == Expression.BOOLLITERALEXPR) {
			System.out.println("\tMOVB " + "$" + left.expressionIntValue + ", %al");
		} else {
			System.out.println("\tMOVB " + left.expressionName + ", %al");
		}
		String targetReg = "%al, ";
		if (op.opType == Token.OR) {
			System.out.println("\tORB %bl, %al");
		} else if (op.opType == Token.AND) {
			System.out.println("\tANDB %bl, %al");
		} else if (op.opType == Token.EQUAL) {
			System.out.println("\tMOVB %al, %cl");
			System.out.println("\tXORB %cl, %bl");
			System.out.println("\tMOVB $1, %al");
			System.out.println("\tSUBB %bl, %al");
		} else if (op.opType == Token.NOT_EQUAL) {
			System.out.println("\tXORB %bl, %al");
		}
		System.out.println("\tMOVB " + targetReg + tempExpr.expressionName);
		return tempExpr;
	}
	
	Expression generateNegation( Expression boolExpr ) {
		Expression tempExpr = new Expression(Expression.BOOLTEMPEXPR, createBoolTemp());
		System.out.println("\tMOVB $1, %al");
		if (boolExpr.expressionType == Expression.BOOLLITERALEXPR) {
			System.out.println("\tSUBB " + "$" + boolExpr.expressionIntValue + ", %al");
		} else {
			System.out.println("\tSUBB " + boolExpr.expressionName + ", %al");
		}
		System.out.println("\tMOVB %al, " + tempExpr.expressionName);
		
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
			generateAssemblyCodeForWriting("$" + expr.expressionName);
		}
		}
	}
	
	// Use helper method to write "True" or "False" to output
	void generateBoolWrite(Expression expr) {
		System.out.println("\t/* Write bool */");
		System.out.println("\txorl %eax, %eax");
		switch (expr.expressionType) {
		case Expression.BOOLIDEXPR:
		case Expression.BOOLTEMPEXPR: {
			System.out.println("\tmovb " + expr.expressionName + ", %al");
			break;
		}
		case Expression.BOOLLITERALEXPR: {
			System.out.println("\tmovb $" + expr.expressionIntValue + ", %al");
		}
		}
		System.out.println("\tpushl %eax");
		System.out.println("\tcall __writeBool");
		
		usesWriteBool = true;
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
	
	void generateStringRead(String identifier) {
		System.out.println("\n\tPUSHL $" + identifier);// Push string destination onto stack
		System.out.println("\tCALL __readStr"); // Use helper method to read string into destination
		usesReadStr = true;
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
	
	void generateBoolAssignment( Expression lValue, Expression expr ) {
		if (expr.expressionType == Expression.BOOLLITERALEXPR) {
			System.out.println("\tMOVB " + "$" + expr.expressionIntValue + ", %al");
			System.out.println("\tMOVB %al, " + lValue.expressionName);
		} else {
			System.out.println("\tMOVB " + expr.expressionName + ", %al");
			System.out.println("\tMOVB %al, " + lValue.expressionName);
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
		
		// Add __readStr method if needed
		if (usesReadStr) {
			System.out.println("\n/* Method to read a string from input, terminated by newline */");
			System.out.println("__readStr:");
			System.out.println("\tpopl %ebx");
			System.out.println("\tpopl %ecx	/* Extract destination address */");
			System.out.println("\tpushl %ebx");
			System.out.println("\tpushl %ecx	/* Push start address back for later */");
			System.out.println("\n__readStrLoop:");
			System.out.println("\tmovl $3, %eax");
			System.out.println("\tmovl $0, %ebx");
			System.out.println("\tmovl $1, %edx	/* Read one character, to address passed from stack */");
			System.out.println("\tint $0x80");
			System.out.println("\n\tcmpb $'\\n', (%ecx)");
			System.out.println("\tjz __readStrEnd		/* Stop reading at a newline character */");
			System.out.println("\tincl %ecx");
			System.out.println("\tjmp __readStrLoop");
			System.out.println("\n__readStrEnd:");
			System.out.println("\tmovb $0, (%ecx)");
			System.out.println("\tincl %ecx");
			System.out.println("\tpopl %eax");
			System.out.println("\tsubl %eax, %ecx		/* Subtract start address to get string length */");
			System.out.println("\tmovl %ecx, 256(%eax)	/* Store string length in corresponding length variable */");
			System.out.println("\tret");
		}
		
		// Add __writeBool method if needed
		if (usesWriteBool) {
			System.out.println("\n/* Method to write \"true\" or \"false\" for a boolean value */");
			System.out.println("__writeBool:");
			System.out.println("\tpopl %ecx");
			System.out.println("\tpopl %eax");
			System.out.println("\tpushl %ecx");
			System.out.println("\tcmpb $0, %al");
			System.out.println("\tje __writeFalse		/* Write \"false\" if x == 0, \"true\" otherwise */");
			System.out.println("\n\tmovl $4, %eax");
			System.out.println("\tmovl $1, %ebx");
			System.out.println("\tmovl $_true, %ecx");
			System.out.println("\tmovl $4, %edx");
			System.out.println("\tint $0x80");
			System.out.println("\tret");
			System.out.println("\n__writeFalse:");
			System.out.println("\tmovl $4, %eax");
			System.out.println("\tmovl $1, %ebx");
			System.out.println("\tmovl $_false, %ecx");
			System.out.println("\tmovl $5, %edx");
			System.out.println("\tint $0x80");
			System.out.println("\tret");
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
		
		System.out.println("\n/* Boolean variables */");
		for (String var : booleanVariablesList) {
			System.out.print(var + ":\t.byte ");
			Object initValue = symbolTable.getInitValue(var);
			// Initialize as 0 or 1, depending on initialization (or just 0 if no initialization)
			System.out.println(initValue == null ? "0" :
				(initValue.equals(Boolean.TRUE) ? "1" : "0") );
		}
		
		System.out.println("\n__minus:  .byte '-'");
		System.out.println("__negOne: .int -1");
		System.out.println("__negFlag: .byte '+'");
		System.out.println("_condition:\t.byte 0");	// Added _condition temporary variable for all control conditions
		if (usesWriteBool) {
			System.out.println("_true:\t.string \"True\"");
			System.out.println("_false:\t.string \"False\"");
		}
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
	
	// Modification of createTempName
	private String createBoolTemp() {
		String tempVar = new String("__temp" + tempCount++);
		booleanVariablesList.add(tempVar);
		return tempVar;
	}
	
	// Create a name for a temporary label used in comparisons
	private String createCompareLabel() {
		return "__compareTrue" + (compareLabelCount++);
	}

}
