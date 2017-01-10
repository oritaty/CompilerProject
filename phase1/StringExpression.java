// Modified version of Expression, for representing string expressions instead of int ones
public class StringExpression    
{
    public final static int IDEXPR = 0;
    public final static int LITERALEXPR = 1;
    public final static int TEMPEXPR = 2;
    
    public int expressionType;
    public String expressionName;
    public String expressionValue; // for literals
        
    public StringExpression( )
    {
        expressionType = 0;
        expressionName = "";
    }
        
    // Constructor for literals
    public StringExpression( String value )
    {
        expressionType = StringExpression.LITERALEXPR;
        expressionValue = value;
    }
    
    // Constructor for ids or temps
    public StringExpression( int type, String name)
    {
        expressionType = type;
        expressionName = name;
    }
}
