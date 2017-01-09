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
            
        public Symbol( String id, int type, Object value ) {
            this.id = id;
            this.type = type;
            this.initValue = value;
        }
    }
    
    private Vector<Symbol> st;
    
    public SymbolTable()
    {
        st = new Vector<>();
    }
    
    public void addItem( Token token )
    {
        st.add( token.getId() );
    }
    
    public boolean checkSTforItem( String id )
    {
       return st.contains( id );
    }

}
