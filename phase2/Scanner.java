//import java.io.FileInputStream;          
import java.io.IOException;         
import java.io.FileReader;       
import java.io.BufferedReader;
//import java.util.*;

public class Scanner
{    
    //public List<Token> list = new LinkedList(); //Test
    public static FileReader fileIn;
    public static BufferedReader bufReader;
    public String fileName;
    
    private int currentLineNumber;
    private String currentLine;
    private String nextLine;
  //  private boolean done;
    private int currentLocation;
    
    public int getCurrentLocation() {
    	return currentLocation;
    }
    
    public void setCurrentLocation(int revertLocation) {
    	currentLocation = revertLocation;
    }
    
    public Scanner(String fname)
    {
        currentLineNumber = 1;
        fileName = fname;
        try
        {
            fileIn = new FileReader(fileName);
            bufReader = new BufferedReader(fileIn);
            
            currentLine = bufReader.readLine();
            currentLocation = 0;
            if (currentLine == null )
            {
   //             done = true;
                nextLine = null;
            } else
            {
   //             done = false;
                nextLine = bufReader.readLine();
            }
        }
        catch (IOException e)
        {
            System.out.println(e);
            return;
        }
    }        
    
    public int getLineNumber()
    {
        return currentLineNumber;
    }
    
    public Token findNextToken()
    {
    	if (currentLine == null)
    	{
    		Token token = new Token( "", Token.EOF );
            return token;
    	}
    	
        int len = currentLine.length();
        String tokenStr = new String();
        int tokenType;
        if ( currentLocation >= len && nextLine == null) 
        {
            Token token = new Token( "", Token.EOF );
            return token;
        }
        if ( currentLocation >= len ) // all characters of currentLine used
        {
            currentLine = nextLine;
            currentLineNumber++;
            try
            {
                nextLine = bufReader.readLine();
            }
            catch (IOException e)
            {
                System.out.println(e);
                Token token = new Token( "", Token.EOF );
                return token;
            } 
            currentLocation = 0;
        }
        while ( Character.isWhitespace( currentLine.charAt(currentLocation)))
            currentLocation++;
        int i = currentLocation;
        if (currentLine.charAt(i) == ';')
        {
            tokenStr = ";";
            tokenType = Token.SEMICOLON;
            i++;
        } else if (currentLine.charAt(i) == '(')
        {
            tokenStr = "(";
            tokenType = Token.LPAREN;
            i++;
        } else if (currentLine.charAt(i) == ')')
        {
            tokenStr = ")";
            tokenType = Token.RPAREN;
            i++;
        } else if(currentLine.charAt(i) == '+')
        {
            tokenStr = "+";
            tokenType = Token.PLUS;
            i++;
        } else if(currentLine.charAt(i) == '-')
        {
            tokenStr = "-";
            tokenType = Token.MINUS;
            i++;
        } else if(currentLine.charAt(i) == ',')
        {
            tokenStr = ",";
            tokenType = Token.COMMA;
            i++;
        } else if (currentLine.charAt(i) == ':'  && i+1 < len && currentLine.charAt(i+1) == '=')
        {
            tokenStr = ":=";
            tokenType = Token.ASSIGNOP;
            i+=2;
        } else if(currentLine.charAt(i) == '*')
        {
            tokenStr = "*";
            tokenType = Token.MULT;
            i++;
        } else if(currentLine.charAt(i) == '/')
        {
            tokenStr = "/";
            tokenType = Token.DIV;
            i++;
        } else if(currentLine.charAt(i) == '%')
        {
            tokenStr = "%";
            tokenType = Token.MOD;
            i++;
        } else if(currentLine.charAt(i) == '~')
        {
        	tokenStr = "~";
        	tokenType = Token.NOT;
        	i++;
        } else if(currentLine.charAt(i) == '&')
        {
        	tokenStr = "&";
        	tokenType = Token.AND;
        	i++;
        }
        
        //Added from here
        else if (currentLine.charAt(i) == '|')
        {
        	// Modified (phase 2) to differentiate between concat (||) and OR (|) operators
        	if (i+1 < len && currentLine.charAt(i+1) == '|') {
                tokenStr = "||";
                tokenType = Token.CONCAT;
                i+=2;
        	} else {
        		tokenStr = "|";
        		tokenType = Token.OR;
        		i++;
        	}
        } 
        
        /*
        else if (i+2 < len && currentLine.substring(0, 3).equals("int")) {
            tokenStr = "int";
            tokenType = Token.INT;
            i+=3;
        }
        
        else if (i+5 < len && currentLine.substring(0, 6).equals("string")) {
            tokenStr = "string";
            tokenType = Token.STRING;
            i+=6;
        }*/
        
        else if (currentLine.charAt(i) == '"' && i+1 < len) {
            //tokenStr = "STRINGLITERAL";
            tokenType = Token.STRINGLITERAL;
            tokenStr = currentLine.substring(i + 1, nextQuot(currentLine, i));
            i = nextQuot(currentLine, i) + 1;
        }
        //to here
        
        else  if ( Character.isDigit((currentLine.charAt(i))) )// find literals
        {
            while ( i < len && Character.isDigit(currentLine.charAt(i)) )
            {
                i++;
            }
            tokenStr = currentLine.substring(currentLocation, i);
            tokenType = Token.INTLITERAL;
        } else // find identifiers and reserved words
        {
            while ( i < len && ! isReservedSymbol(currentLine.charAt(i)) )
            {
                i++;
            }
            tokenStr = currentLine.substring(currentLocation, i);
            tokenType = Token.ID;
        }
       
        currentLocation = i;
        Token token = new Token(tokenStr, tokenType);
        if ( i == len )// characters on currentLine used up
        {
            currentLine = nextLine;
            currentLineNumber++;
            try
            {
                nextLine = bufReader.readLine();
            }
            catch (IOException e)
            {
                System.out.println(e);
                return null;
            }
            currentLocation = 0;
        }
//        if (currentLine == null) done = true;  // reached EOF
        return token;
    }
    
    //Jump to the index of next quotation mark;
    int nextQuot(String str, int currentIndex) {
        currentIndex++; //Start from one character behind.
        while (currentIndex < str.length()) {
            if (str.charAt(currentIndex) == '"') {
                break;
            }
            currentIndex++;
        }
        return currentIndex;
    }
 
    boolean isReservedSymbol( char ch)
    {
        return( ch == ' ' || ch == '\n' || ch == '\t' || ch == ';' | ch == '+' ||
                ch == '-' || ch == '(' || ch == ')' || ch == ','  || ch == ':' ||
                ch == '|' || ch == '*' || ch == '/' || ch == '%');
    }

    //Test
    /* void printAll() {
        for (Token t : list) {
            System.out.println(t.toString() + " " + t.getId() + " " + t.getType());
        }
    } */
}
