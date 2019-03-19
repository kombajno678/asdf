

class NrTelefoniczny{
    String nrkierunkowy = null;
    String nrTelefonu = null;
    public void setNrkierunkowy(String in){
        if(in.matches("[0-9]{9}")){
            nrkierunkowy = in;
        }
    }
    public void setNrTelefonu(String in){
        if(in.matches("[0-9]{9}")){
            nrTelefonu = in;
        }
    }
}
public class Main {

    public static void main(String[] args) {
        System.out.println("Hello World!");
    }
}
