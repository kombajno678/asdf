public class main {

    public static void main(String[] args) {

        try {
        	String txt = args[0];
        	int start = Integer.parseInt(args[1]);
        	int end = Integer.parseInt(args[2]);
        	try {
            	System.out.println(txt.substring(start, end+1));
            }catch(Exception ex) {
            	System.out.println("Error while creating substring: " + ex.getMessage() + " (" + ex.getClass() + ")" );
            }
        }catch(Exception ex) {
        	System.out.println("Error while getting args: " + ex.getMessage() + " (" + ex.getClass() + ")" );
        }
        
        
    }
}