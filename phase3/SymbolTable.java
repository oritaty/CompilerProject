import java.util.Vector; 

class SymbolTable
{
    private static class Symbol {
        String id;
        int type;
        Object initValue; // null if not initialized in declaration
        public Symbol( String id, int type ) {
            this.id = id;
            this.type = type;
            this.initValue = null;
        }
            
       /* public Symbol( String id, int type, Object value ) {
            this.id = id;
            this.type = type;
            this.initValue = value;
        }*/
        public String toString() {
         String rtn = "id=" + this.id + ", type=" + this.type;
            if (this.initValue != null) {
                rtn += ", init=" + this.initValue;
            }
            //rtn += "\n";
            return rtn;
        }
        
        public String getId() {
            return this.id;
        }
        
        public void setInitValue( Object value ) {
            this.initValue = value;
        }
        
        public Object getInitValue() {
        	return this.initValue;
        }
        
        public int getType() {
        	return this.type;
        }
    }
    
    private Vector<Symbol> st;
    
    public SymbolTable()
    {
        st = new Vector<>();
    }
    
    public void addItem( Token token, int type )
    {
        st.add( new Symbol( token.getId(), type) );
        //printAll();
    }
    
    public boolean checkSTforItem( String id )
    {
       for (Symbol s : st) {
           if (s.getId().equals(id))
               return true;
       }
       return false;
    }
    
    // Add an initial value to the symbol table entry for a certain id
    public void initVariable( String id, Object value )
    {
        for (Symbol s : st) {
           if (s.getId().equals(id)) {
               s.setInitValue(value);
           }
        }
    }
    
    public Object getInitValue( String id )
    {
        for (Symbol s : st) {
           if (s.getId().equals(id)) {
               return s.getInitValue();
           }
        }
        return null;
    }
    
    //For test purpose.
    public void printAll() {
        for (Symbol s : st) {
            System.out.println(s.toString());
        }
        System.out.println();
    }
    
    // Get the type (Token.INT, Token.STRING, or Token.BOOLEAN) of a variable
    public int getType( String id ) {
    	for (Symbol s : st) {
            if (s.getId().equals(id)) {
                return s.getType();
            }
        }
    	return 0;
    }

}