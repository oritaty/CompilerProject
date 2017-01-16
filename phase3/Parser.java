/* PROGRAM Micro */

/* 	Java version of the Micro compiler from Chapter 2 of "Crafting a Compiler" --
*	for distribution to instructors 
*	Converted to Java by James Kiper in July 2003.
*
*/

/* Micro grammar
   <program>	    -> #Start BEGIN <statement list> END
   <statement list> -> <statement> {<statement>}
   <statement>	    -> <ident> := <expression> #Assign ;
   <statement>	    -> READ ( <id list> ) ;
   <statement>	    -> WRITE ( <expr list> ) ;
   <id list>	    -> <ident> #ReadId {, <ident> #ReadId }
   <expr list>	    -> <expression> #WriteExpr {, <expression> #WriteExpr}
   <expression>	    -> <primary> {<add op> <primary> #GenInfix}
   <primary>	    -> ( <expression> )
   <primary>	    -> <ident>
   <primary>	    -> IntLiteral #ProcessLiteral
   <primary>	    -> MinusOp #ProcessNegative IntLiteral #ProcessLiteral
   <add op>	    	-> PlusOp #ProcessOp
   <add op>	    	-> MinusOp #ProcessOp
   <ident>	    	-> Id #ProcessId
   <system goal>    -> <program> EofSym #Finish
 */


public class Parser
{
    private static Scanner scanner;
    private static SymbolTable symbolTable;
    private static CodeFactory codeFactory;
    private Token currentToken;
    private Token previousToken;
    private static boolean signSet = false;
    private static String signFlag = "+";
   
    //private static int previousType = 0; // The most recent type keyword (Token.INT or Token.STRING)

    public Parser()
    {
        
    }

    static public void main (String args[])
    {
        Parser parser = new Parser();
      //  scanner = new Scanner( args[0]);
        scanner = new Scanner( "test.txt");
        symbolTable = new SymbolTable();
        codeFactory = new CodeFactory(symbolTable); // Pass reference to the symbol table
        parser.parse();
    }
    
    public void parse()
    {
        currentToken = scanner.findNextToken();
        systemGoal();
    }
    
    private void systemGoal()
    {
        program();
        codeFactory.generateData();
    }
    
    private void program()
    {
        match( Token.BEGIN );
        codeFactory.generateStart();
        statementList();
        match( Token.END );
        codeFactory.generateExit();
    }
    
    private void statementList()
    {
        while ( currentToken.getType() == Token.ID || currentToken.getType() == Token.READ || 
                    currentToken.getType() == Token.WRITE || currentToken.getType() == Token.DECLARE)
        {
            statement();
        }
    }
    
    private void statement()
    {
        Expression lValue;
        Expression expr;
        StringExpression stringExpr;
        
        switch ( currentToken.getType() )
        {
            case Token.ID:
            {
                lValue = identifier(false);
                match( Token.ASSIGNOP );
                // Added conditional here to determine whether to match int, string, or boolean expression
                if (symbolTable.getType(lValue.expressionName) == Token.STRING) {
                	stringExpr = stringExpression();
                	codeFactory.generateStringAssignment( new StringExpression(0, lValue.expressionName), stringExpr );
                } else if (symbolTable.getType(lValue.expressionName) == Token.BOOLEAN) {
                	expr = boolExpression();
                	codeFactory.generateBoolAssignment( lValue, expr );
                } else {
                	expr = expression(true);
                	if (expr.expressionType > 2)
                		System.out.println("Cannot assign non-int expression to int, at line " + scanner.getLineNumber());
                	else
                		codeFactory.generateAssignment( lValue, expr );
                }
                match( Token.SEMICOLON );
                break;
            }
            case Token.READ :
            {
                match( Token.READ );
                match( Token.LPAREN );
                idList();
                match( Token.RPAREN );
                match( Token.SEMICOLON );
                break;
            }
            case Token.WRITE :
            {
                match( Token.WRITE );
                match( Token.LPAREN );
                expressionList();
                match( Token.RPAREN );
                match( Token.SEMICOLON );
                break;
            }
            case Token.DECLARE :	// Added, to process the new declaration syntax
            {
                match( Token.DECLARE);
                match( Token.LPAREN );
                type();
                int declareType = previousToken.getType(); // Token.INT, Token.STRING, or Token.BOOLEAN
                lValue = identifier(true); // The variable being declared
                Token varToken = previousToken; // Its identifier token
                
                symbolTable.addItem(varToken, declareType);
                
                // Process an initialization, if there is one
                if (currentToken.getType() == Token.ASSIGNOP) {
                   match( Token.ASSIGNOP );
                   if (currentToken.getType() == Token.STRINGLITERAL) {
                	   // Initializing with string literal
                	   if (declareType != Token.STRING) {
                		   System.out.println("Type error! Cannot initialize non-string variable to a string at line "
                				   + scanner.getLineNumber());
                	   }
                	   // Add initial value to symbol table entry
                	   match (Token.STRINGLITERAL);
                	   StringExpression literal = processStringLiteral(true);
                	   symbolTable.initVariable(varToken.getId(), literal.expressionValue);
                	   
                   } else if (currentToken.getType() == Token.BOOLEANLITERAL) {	// Added, to support new type (boolean)
                	// Initializing with boolean literal
                	   if (declareType != Token.BOOLEAN) {
                		   System.out.println("Type error! Cannot initialize non-boolean variable to a boolean at line "
                				   + scanner.getLineNumber());
                	   }
                	   // Add initial value to symbol table entry
                	   match (Token.BOOLEANLITERAL);
                	   symbolTable.initVariable(varToken.getId(),
                			   previousToken.getId().toLowerCase().equals("true") ? Boolean.TRUE : Boolean.FALSE);
                   
                   } else if (currentToken.getType() == Token.INTLITERAL || currentToken.getType() == Token.MINUS) {
                	   // Initializing with int literal
                	   if (declareType != Token.INT) {
                		   System.out.println("Type error! Cannot initialize non-int variable to an int at line "
                				   + scanner.getLineNumber());
                	   }
                	   // Handle a possible minus sign
                	   if (currentToken.getType() == Token.MINUS) {
                		   match( Token.MINUS );
                		   processSign();
                	   }
                	   // Add an initial value to symbol table entry
                	   match( Token.INTLITERAL );
                	   Expression literal = processLiteral();
                	   symbolTable.initVariable(varToken.getId(), literal.expressionIntValue);
                   } else {
                	   // Trying to initialize to something that isn't a literal
                	   System.out.println("Initialization error! Variables can only be initialized to a literal, at line "
                			   + scanner.getLineNumber());
                   }
                	   
                }
                
                // Add to the proper variable list in CodeFactory
                if (declareType == Token.STRING)
                    codeFactory.generateStringDeclaration( varToken );
                else if (declareType == Token.BOOLEAN)
                	codeFactory.generateBooleanDeclaration( varToken );
                else
                	codeFactory.generateIntDeclaration( varToken );
                   
               
                match( Token.RPAREN );
                match( Token.SEMICOLON );
                break;
            }
            default: error(currentToken);
        }
    }
    
	// New method added to match "int" or "string" in type declarations
	private void type() {
		int tokenType = currentToken.getType();
		if (tokenType == Token.INT) {
			//previousType = Token.INT;
			match(Token.INT);
		} else if (tokenType == Token.STRING) {
			//previousType = Token.STRING;
			match(Token.STRING);
		} else if (tokenType == Token.BOOLEAN) {
			//previousType = Token.BOOLEAN;
			match(Token.BOOLEAN);
		} else
			error(tokenType); // Invalid type keyword
	}
   
    private void idList()
    {
        readId();
        while ( currentToken.getType() == Token.COMMA )
        {
            match(Token.COMMA);
            readId();
        }
    }
    
    // Added method, reads an individual string or int from std in
    private void readId() {
    	Expression idExpr = identifier(false);
    	if (symbolTable.getType(idExpr.expressionName) == Token.STRING) {
    		// Read string
    		codeFactory.generateStringRead(idExpr.expressionName);
    	} else {
    		// Read int
    		codeFactory.generateRead(idExpr);
    	}
    }
    
    private void expressionList() // Modified to check whether each expression is int or string
    {
        writeExpression();
        while ( currentToken.getType() == Token.COMMA )
        {
            match( Token.COMMA );
            writeExpression();
        }
    }
    
    // Added method, to write an individual expression in an expression list
    private void writeExpression() {
    	if (currentToken.getType() == Token.STRINGLITERAL
    			|| (currentToken.getType() == Token.ID && symbolTable.getType(currentToken.getId()) == Token.STRING)) {
    		// String expression
    		StringExpression expr = stringExpression();
    		codeFactory.generateStringWrite(expr);
    	} else if (currentToken.getType() == Token.BOOLEANLITERAL || currentToken.getType() == Token.NOT
    			|| (currentToken.getType() == Token.ID && symbolTable.getType(currentToken.getId()) == Token.BOOLEAN)) {
    		Expression expr = boolExpression();
    		codeFactory.generateBoolWrite(expr);
    	} else {
    		// Int or boolean expression
    		Expression expr = expression(true);
    		if (expr.expressionType >= Expression.BOOLIDEXPR) {
    			codeFactory.generateBoolWrite(expr);
    		} else {
    			// write int expression
    			codeFactory.generateWrite(expr);
    		}
    	}
    }
    
    private Expression expression(boolean isTopLevel)
    {
        Expression result;
        Expression leftOperand;
        Expression rightOperand;
        Operation op;
        
        // Store the current state in case this is the wrong parse
        int oldScannerLocation = scanner.getCurrentLocation();
        Token oldPreviousToken = previousToken;
        Token oldCurrentToken = currentToken;
        
        result = factor();
        
        // If the expression should be a boolean expression instead of an integer one
        if (result.expressionType == Expression.SHOULD_BE_BOOL) {
        	if (isTopLevel) {
	        	// Back up to state before this method's matching, to do again in boolExpression
	        	scanner.setCurrentLocation(oldScannerLocation);
	        	previousToken = oldPreviousToken;
	        	currentToken = oldCurrentToken;
	        	return boolExpression();
        	} else
        		return result;
        }
        
        while ( currentToken.getType() == Token.PLUS || currentToken.getType() == Token.MINUS)
        {
            leftOperand = result;
            op = addOperation();
            rightOperand = factor();
            result = codeFactory.generateArithExpr( leftOperand, rightOperand, op );
        }
        
        
        return result;
    }
    
    // Modified version of expression(), to handle boolean expressions instead of int expressions
    private Expression boolExpression() {
    	Expression result;
        Expression leftOperand;
        Expression rightOperand;
        Operation op;
        
        result = boolTerm();
        
        while ( currentToken.getType() == Token.OR ) {
        	leftOperand = result;
        	op = addOperation();
        	rightOperand = boolTerm();
        	result = codeFactory.generateBoolExpr( leftOperand, rightOperand, op );
        }
        
    	return result;
    }
    
    private Expression boolTerm() {
    	Expression result;
        Expression leftOperand;
        Expression rightOperand;
        Operation op;
        
        result = boolPrimary();
        
        while ( currentToken.getType() == Token.AND ) {
        	leftOperand = result;
        	op = addOperation();
        	rightOperand = boolPrimary();
        	result = codeFactory.generateBoolExpr( leftOperand, rightOperand, op );
        }
        
        return result;
    }
    
    private Expression boolPrimary() {
    	Expression result = new Expression();
    	switch (currentToken.getType()) {
    	case Token.ID:
    	{
    		Expression idExpr = identifier(false);
    		if (symbolTable.getType(previousToken.getId()) != Token.BOOLEAN)
    			System.out.println("Type error! Variable '" + previousToken.getId() + "' is not a boolean at line "
    					+ scanner.getLineNumber());
    		result = idExpr;
    		result.expressionType = Expression.BOOLIDEXPR;
    		break;
    	}
    	case Token.LPAREN:
    	{
    		match( Token.LPAREN );
            result = boolExpression();
            match( Token.RPAREN );
            break;
    	}
    	case Token.BOOLEANLITERAL:
    	{
    		match(Token.BOOLEANLITERAL);
    		result = processBoolLiteral();
    		break;
    	}
    	case Token.NOT:
    	{
    		match(Token.NOT);
    		result = codeFactory.generateNegation( boolPrimary() );
    		break;
    	}
    	default:	error(currentToken);
    	}
    	return result;
    }
    
    private Expression factor()
    {
        Expression result;
        Expression leftOperand;
        Expression rightOperand;
        Operation op;
        
        result = primary();
        
        if (result.expressionType == Expression.SHOULD_BE_BOOL)
        	return result;
        
        while ( currentToken.getType() == Token.MULT || currentToken.getType() == Token.DIV || 
                currentToken.getType() == Token.MOD)
        {
            leftOperand = result;
            op = addOperation();
            rightOperand = primary();
            result = codeFactory.generateArithExpr( leftOperand, rightOperand, op );
        }
        return result;
    }
    
    // Modification of Expression to handle string literals, ids, or concat operations
    private StringExpression stringExpression()
    {
    	StringExpression result;
    	StringExpression leftOperand;
    	StringExpression rightOperand;
    	
    	result = stringPrimary();
    	while ( currentToken.getType() == Token.CONCAT )
    	{
    		leftOperand = result;
    		match(Token.CONCAT);
    		rightOperand = stringPrimary();
    		result = codeFactory.generateConcatExpr( leftOperand, rightOperand );
    	}
    	return result;
    }
    
    private Expression primary()
    {
        Expression result = new Expression();
        switch ( currentToken.getType() )
        {
            case Token.LPAREN :
            {
                match( Token.LPAREN );
                result = expression(false);
                if (result.expressionType == Expression.SHOULD_BE_BOOL)
                	return result;
                match( Token.RPAREN );
                break;
            }
            case Token.ID:
            {
            	if (symbolTable.getType(currentToken.getId()) == Token.BOOLEAN)
            		result.expressionType = Expression.SHOULD_BE_BOOL;
            	else {
            		if (symbolTable.getType(currentToken.getId()) != Token.INT)
            			System.out.println("Type error! Variable '" + currentToken.getId() + "' is not an int at line "
            					+ scanner.getLineNumber());
            		result = identifier(false);
            	}
                break;
            }
            case Token.INTLITERAL:
            {
                match(Token.INTLITERAL);
                result = processLiteral();
                break;
            }
            case Token.MINUS:
            {
                match(Token.MINUS);
                processSign();
                match(Token.INTLITERAL);
                result = processLiteral();
                break;
            }
            case Token.PLUS:
            {
                match(Token.PLUS);
                processSign();
                match(Token.INTLITERAL);
                result = processLiteral();
                break;
            }
            case Token.MULT:
            {
                match(Token.MULT);
                processSign();
                match(Token.INTLITERAL);
                result = processLiteral();
                break;
            }
            case Token.DIV:
            {
                match(Token.DIV);
                processSign();
                match(Token.INTLITERAL);
                result = processLiteral();
                break;
            }
            case Token.MOD:
            {
                match(Token.MOD);
                processSign();
                match(Token.INTLITERAL);
                result = processLiteral();
                break;
            }
            case Token.BOOLEANLITERAL:	case Token.NOT:
            {
            	result.expressionType = Expression.SHOULD_BE_BOOL;
            	break;
            }
            default: error( currentToken );
        }
        return result;
    }
    
    // Matches a string variable or literal
    private StringExpression stringPrimary() {
    	if (currentToken.getType() == Token.ID) {
    		identifier(false); // Match the ID token, and check it has been declared
    		return new StringExpression(StringExpression.IDEXPR, previousToken.getId());
    	} else if (currentToken.getType() == Token.STRINGLITERAL) {
    		match(Token.STRINGLITERAL);
    		return processStringLiteral(false);
    	} else {
    		error( currentToken );
        	return new StringExpression();
    	}
    }
    
    private Operation addOperation()
    {
        Operation op = new Operation();
        switch ( currentToken.getType() )
        {
            case Token.PLUS:
            {
                match( Token.PLUS ); 
                op = processOperation();
                break;
            }
            case Token.MINUS:
            {
                match( Token.MINUS ); 
                op = processOperation();
                break;
            }
            case Token.MULT:
            {
                match( Token.MULT ); 
                op = processOperation();
                break;
            }
            case Token.DIV:
            {
                match( Token.DIV ); 
                op = processOperation();
                break;
            }
            case Token.MOD:
            {
                match( Token.MOD ); 
                op = processOperation();
                break;
            }
	    case Token.AND:
            {
                match( Token.AND ); 
                op = processOperation();
                break;
            }
            case Token.OR:
            {
                match( Token.OR ); 
                op = processOperation();
                break;
            }
            case Token.NOT:
            {
                match( Token.NOT ); 
                op = processOperation();
                break;
            }
	    //Added for phase 3
            case Token.EQUAL:
            {
                match( Token.EQUAL );
                op = processOperation();
                break;
            }
            case Token.NOT_EQUAL:
            {
                match( Token.NOT_EQUAL );
                op = processOperation();
                break;
            }
            case Token.GREATER_OR_EQUAL:
            {
                match( Token.GREATER_OR_EQUAL );
                op = processOperation();
                break;
            }
            case Token.GREATER:
            {
                match( Token.GREATER );
                op = processOperation();
                break;
            }
            case Token.SMALLER_OR_EQUAL:
            {
                match( Token.SMALLER_OR_EQUAL );
                op = processOperation();
                break;
            }
            case Token.SMALLER:
            {
                match( Token.SMALLER );
                op = processOperation();
                break;
            }
            default: error( currentToken );
        }
        return op;
    }
    
    private Expression identifier(boolean declaring)
    {
        Expression expr;
        match( Token.ID );
        expr = processIdentifier(declaring);
        return expr;
    }
    
    private void match( int tokenType)
    {
        previousToken = currentToken;
        if ( currentToken.getType() == tokenType )
            currentToken = scanner.findNextToken();
        else 
        {
            error( tokenType );
            currentToken = scanner.findNextToken();
        }
    }

    private void processSign()
    {
	/* changed
    	Parser.signSet = true;
    	if ( previousToken.getType() == Token.PLUS ) 
    	{
    		Parser.signFlag = "+";
    	} else
    	{
    		Parser.signFlag = "-";
    	}*/
	    
	Parser.signSet = true;
    	if ( previousToken.getType() == Token.PLUS ) {
    		Parser.signFlag = "+";
    	} else if (previousToken.getType() == Token.MINUS ) {
    		Parser.signFlag = "-";
    	}
    }
    private Expression processLiteral()
    {
    	if (previousToken.getType() != Token.INTLITERAL) {
    		Parser.signSet = false;
    		return new Expression(); // No valid literal to process
    	}
    	Expression expr;
        int value = ( new Integer( previousToken.getId() )).intValue();
        if (Parser.signSet && Parser.signFlag.equals("-"))
        {
        	 expr = new Expression( Expression.LITERALEXPR, "-"+previousToken.getId(), value*-1 );
        } else
        {
        	 expr = new Expression( Expression.LITERALEXPR, previousToken.getId(), value ); 
        }
        Parser.signSet = false;
        return expr;
    }
    
    private StringExpression processStringLiteral(boolean isDeclaration)
    {
    	if (isDeclaration) {
    		return new StringExpression(previousToken.getId());
    	} else {
    		String literalVar = codeFactory.createStringTemp();
        	symbolTable.addItem(new Token(literalVar, Token.ID), Token.STRING);
        	symbolTable.initVariable(literalVar, previousToken.getId());
        	return new StringExpression(StringExpression.IDEXPR, literalVar);
    	}
    }
	
    private Expression processBoolLiteral() {
        if (previousToken.getType() != Token.BOOLEANLITERAL) {
    		Parser.signSet = false;
    		return new Expression(); // No valid literal to process
	}
        int value;
        if (previousToken.getId().toLowerCase().equals("true")) {
            value = 1;
        } else {
            value = 0;
        }
        return new Expression( Expression.BOOLLITERALEXPR, previousToken.getId(), value);
    }
    
    private Operation processOperation()
    {
        Operation op = new Operation();
        if ( previousToken.getType() == Token.PLUS ) op.opType = Token.PLUS;
        else if ( previousToken.getType() == Token.MINUS ) op.opType = Token.MINUS;
	else if ( previousToken.getType() == Token.MULT ) op.opType = Token.MULT;
        else if ( previousToken.getType() == Token.DIV ) op.opType = Token.DIV;
        else if ( previousToken.getType() == Token.MOD ) op.opType = Token.MOD;
	else if ( previousToken.getType() == Token.OR ) op.opType = Token.OR;//add
        else if ( previousToken.getType() == Token.AND ) op.opType = Token.AND;//add
        else if ( previousToken.getType() == Token.NOT ) op.opType = Token.NOT;//add
	else if ( previousToken.getType() == Token.EQUAL ) op.opType = Token.EQUAL;//add
        else if ( previousToken.getType() == Token.NOT_EQUAL ) op.opType = Token.NOT_EQUAL;//add
        else if ( previousToken.getType() == Token.GREATER_OR_EQUAL ) op.opType = Token.GREATER_OR_EQUAL;//add
        else if ( previousToken.getType() == Token.GREATER ) op.opType = Token.GREATER;//add
        else if ( previousToken.getType() == Token.SMALLER_OR_EQUAL ) op.opType = Token.SMALLER_OR_EQUAL;//add
        else error( previousToken );
        return op;
    }
    
    private Expression processIdentifier(boolean declaring)
    {
        Expression expr = new Expression( Expression.IDEXPR, previousToken.getId());
        
        if ( ! symbolTable.checkSTforItem( previousToken.getId() ) && !declaring )
        {
            /*symbolTable.addItem( previousToken );
            codeFactory.generateDeclaration( previousToken );*/ // This code previously declared variables automatically
           
           // Now, give an error if this isn't a declaration statement and the variable hasn't been declared yet.
           System.out.println("Identifier error! " + previousToken.getId() + " has not been declared at line number " +
                              scanner.getLineNumber() );
        } else if (declaring && symbolTable.checkSTforItem( previousToken.getId() )) {
           if (symbolTable.checkSTforItem( previousToken.getId() ))
              // Give the reverse error: the variable name is already used.
              System.out.println("Identifier error! Variable name '" + previousToken.getId() + "' is already declared at line number " +
                              scanner.getLineNumber() );

        }
        return expr;
    }
    private void error( Token token )
    {
        System.out.println( "Syntax error! Parsing token type " + token.toString() + " at line number " + 
                scanner.getLineNumber() );
        if (token.getType() == Token.ID )
            System.out.println( "ID name: " + token.getId() );
    }
    private void error( int tokenType )
    {
        System.out.println( "Syntax error! Parsing token type " +tokenType + " at line number " + 
                scanner.getLineNumber() );
    }
}
