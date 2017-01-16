

public class Token {
    private String id;
    private int type;
    public final static int LexERROR = 0;
    public final static int BEGIN = 1;
    public final static int END = 2;
    public final static int READ = 3;
    public final static int WRITE = 4;
    public final static int ID = 5;
    public final static int LPAREN = 6;
    public final static int RPAREN = 7;
    public final static int COMMA = 8;
    public final static int SEMICOLON = 9;
    public final static int ASSIGNOP = 10;
    public final static int PLUS = 11;
    public final static int MINUS = 12;
    public final static int INTLITERAL = 13;
    public final static int EOF = 14;
    public final static int INT = 15; //Added
    public final static int STRING = 16; //Added
    public final static int CONCAT = 17; //Added
    public final static int STRINGLITERAL = 18; //Added
    public final static int DECLARE = 19; //Added
    
    //Added for phase 2
    public final static int BOOLEAN = 20; 
    public final static int BOOLEANLITERAL = 21; 
    public final static int MULT = 22;
    public final static int DIV = 23;
    public final static int MOD = 24;
    public final static int NOT = 25;
    public final static int AND = 26;
    public final static int OR = 27;
    
    //Added for phase 3
    public static final int EQUAL = 28;
    public static final int NOT_EQUAL = 29;
    public static final int GREATER = 30;
    public static final int GREATER_OR_EQUAL = 31;
    public static final int SMALLER = 32;
    public static final int SMALLER_OR_EQUAL = 33;
    public static final int WHILE = 34;
    
    public Token( String tokenString, int tokenType)
    {
        id = tokenString;
        type = tokenType;
        if (tokenType == ID)
        {
            String temp = tokenString.toLowerCase();
            if ( temp.compareTo( "begin") == 0) type = BEGIN;
            else if ( temp.compareTo( "end") == 0) type = END;
            else if ( temp.compareTo("read") == 0) type = READ;
            else if ( temp.compareTo("write") == 0) type = WRITE;
            else if ( temp.compareTo("declare") == 0) type = DECLARE;
            else if ( temp.compareTo("int") == 0) type = INT;
            else if ( temp.compareTo("string") == 0) type = STRING;
            else if ( temp.compareTo("boolean") == 0) type = BOOLEAN;
            else if ( temp.compareTo("true") == 0) type = BOOLEANLITERAL;
            else if ( temp.compareTo("false") == 0) type = BOOLEANLITERAL;
        }
    }
    public String getId()
    {
        return id;
    }
    public int getType()
    {
        return type;
    }
    public String toString()
    {
        String str;
        switch (type)
        {
            case LexERROR : str = "Lexical Error"; break;
            case BEGIN : str = "BEGIN"; break;
            case END : str = "END"; break;
            case READ : str = "READ"; break;
            case WRITE : str = "WRITE"; break;
            case ID: str = "ID"; break;
            case LPAREN : str = "LPAREN"; break;
            case RPAREN : str = "RPAREN"; break;
            case COMMA : str = "COMMA"; break;
            case SEMICOLON : str = "SEMICOLON"; break;
            case ASSIGNOP : str = "ASSIGNOP"; break;
            case PLUS : str = "PLUS"; break;
            case MINUS : str = "MINUS"; break;
            case INTLITERAL : str = "INTLITERAL"; break;
            case EOF : str = "EOF"; break;
            case INT : str = "INT"; break; //Added
            case STRING : str = "STRING"; break; //Added
            case CONCAT : str = "CONCAT"; break; //Added
            case STRINGLITERAL : str = "STRINGLITERAL"; break; //Added
            case DECLARE : str = "DECLARE"; break; //Added
                
            //Added for phase 2
            case BOOLEAN : str = "BOOLEAN"; break; 
            case BOOLEANLITERAL : str = "BOOLEANLITERAL"; break; 
            case MULT : str = "MULT"; break;
            case DIV : str = "DIV"; break;
            case MOD : str = "MOD"; break;
            case NOT : str = "NOT"; break;
            case AND : str = "AND"; break;
            case OR  : str = "OR"; break;
                
            //Added for phase 3
            case EQUAL : str = "EQUAL"; break;
            case NOT_EQUAL : str = "EQUAL"; break;
            case GREATER : str = "EQUAL"; break;
            case GREATER_OR_EQUAL : str = "EQUAL"; break;
            case SMALLER : str = "EQUAL"; break;
            case SMALLER_OR_EQUAL : str = "EQUAL"; break;
            case WHILE : str = "EQUAL"; break;
            
            default: str = "Lexical Error";
        }
        return str;
    }
}
