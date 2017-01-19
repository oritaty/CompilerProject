import java.util.Vector; 

// Created to store function identifiers separately from separately from variables
class FunctionTable
{
    
    private Vector<String> st;
    
    public FunctionTable()
    {
        st = new Vector<>();
    }
    
    public void addItem( String functionName )
    {
        st.add( functionName );
        //printAll();
    }
    
    public boolean checkSTforItem( String id )
    {
       for (String s : st) {
           if (s.equals(id))
               return true;
       }
       return false;
    }
    
    //For test purpose.
    public void printAll() {
        for (String s : st) {
            System.out.println(s);
        }
        System.out.println();
    }

}