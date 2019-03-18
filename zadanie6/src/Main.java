
import java.util.Comparator;
import java.util.Arrays;

class NrTelefoniczny implements Comparable<NrTelefoniczny> {
    public NrTelefoniczny(){
        setNrKierunkowy("0");
        setNrTelefonu("0");
    }
    public NrTelefoniczny(String kierunkowy, String telefoniczny){
        setNrKierunkowy(kierunkowy);
        setNrTelefonu(telefoniczny);
    }
    String nrKierunkowy;
    String nrTelefonu;
    public void setNrTelefonu(String n){
        nrTelefonu = n;
    }
    public void setNrKierunkowy(String n){
        nrKierunkowy = n;
    }
    public String getNrKierunkowy(){
        return nrKierunkowy;
    }
    public String getNrTelefonu(){
        return nrTelefonu;
    }
    public String ToString(){
        return String.join(" - ", this.nrKierunkowy,this.nrTelefonu);
        //return (this.getNrKierunkowy() + " - " + this.getNrTelefonu());
    }
    //Comparable interfejs
    public int compareTo(NrTelefoniczny a){
        return(Integer.parseInt(this.getNrTelefonu()) - Integer.parseInt(a.getNrTelefonu()));
    }
}
abstract class Wpis{
    abstract public void opis();
    //... inne metody abstrakcyjne, moze
}
class Osoba extends Wpis{
    String imie, nazwisko, adres;
    NrTelefoniczny telefon = new NrTelefoniczny();
    public void opis(){

    }
}
class Firma extends Wpis{
    String nazwa, adres;
    NrTelefoniczny telefon = new NrTelefoniczny();
    public void opis(){

    }
}
public class Main {

    public static void main(String[] args) {
        System.out.println("Hello World!");
        NrTelefoniczny[] a = new NrTelefoniczny[4];
        a[0] = new NrTelefoniczny("0", "123");
        a[1] = new NrTelefoniczny("0", "321");
        a[2] = new NrTelefoniczny("0", "222");
        a[3] = new NrTelefoniczny("0", "1");
        /*
        for(int i = 0; i < 4; i++){
            System.out.println(a[i].getNrTelefonu());
        }
        */
        Arrays.sort(a);


        for(int i = 0; i < 4; i++){
            System.out.println(a[i].ToString());
        }
    }
}
