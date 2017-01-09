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
        
        public String getId() {
            return this.id;
        }
        
        public void setInitValue( Object value ) {
            this.initValue = value;
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

}
