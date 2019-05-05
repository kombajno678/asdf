import java.util.TreeMap;
import java.util.Set;
import java.util.Iterator;
import java.util.Map;

class NrTelefoniczny implements Comparable<NrTelefoniczny> {
    protected String nrKierunkowy;
    protected String nrTelefonu;
    //constructors
    public NrTelefoniczny(){
        setNrKierunkowy("0");
        setNrTelefonu("0");
    }
    public NrTelefoniczny(String kierunkowy, String telefoniczny){
        setNrKierunkowy(kierunkowy);
        setNrTelefonu(telefoniczny);
    }
    //getters setters
    public String getNrKierunkowy() {
        return nrKierunkowy;
    }
    public void setNrKierunkowy(String nrKierunkowy) {
        this.nrKierunkowy = nrKierunkowy;
    }
    public String getNrTelefonu() {
        return nrTelefonu;
    }
    public void setNrTelefonu(String nrTelefonu) {
        this.nrTelefonu = nrTelefonu;
    }
    //methods
    public String ToString(){
        return String.join(" - ", this.getNrKierunkowy(), this.getNrTelefonu());
    }
    //Comparable interface
    public int compareTo(NrTelefoniczny a){
        if(Integer.parseInt(this.getNrTelefonu()) - Integer.parseInt(a.getNrTelefonu()) == 0){
            return( Integer.parseInt(this.getNrKierunkowy()) - Integer.parseInt(a.getNrKierunkowy()) );
        }else return(Integer.parseInt(this.getNrTelefonu()) - Integer.parseInt(a.getNrTelefonu()));
    }
}
abstract class Wpis{
    abstract public String opis();
    @Override abstract public String toString();
    NrTelefoniczny telefon = new NrTelefoniczny();
}
class Osoba extends Wpis{
    String imie, nazwisko, adres;
    public Osoba(String imie_, String nazwisko_, String adres_, String nrTelefoniczny_, String nrKierunkowy_){
        imie = imie_;
        nazwisko = nazwisko_;
        adres = adres_;
        telefon.setNrKierunkowy(nrKierunkowy_);
        telefon.setNrTelefonu(nrTelefoniczny_);
    }
    @Override public String toString(){
        return this.opis();
    }
    public String opis(){
        return  String.format("%16s%16s%16s",imie+" "+nazwisko, adres, telefon.ToString());
    }
}
class Firma extends Wpis{
    String nazwa, adres;
    public Firma(String nazwa_ , String adres_, String nrTelefoniczny_, String nrKierunkowy_){
        nazwa = nazwa_;
        adres = adres_;
        telefon.setNrKierunkowy(nrKierunkowy_);
        telefon.setNrTelefonu(nrTelefoniczny_);
    }
    @Override public String toString(){
        return this.opis();
    }
    public String opis(){
        return String.format("%16s%16s%16s", nazwa, adres, telefon.ToString());
    }
}
public class zad7 {

    public static void main(String[] args) {

        Osoba[] osoby = new Osoba[5];
        Firma[] firmy = new Firma[5];

        osoby[0] = new Osoba("adam", "kowalski", "Warszawa", "600200300", "42");
        osoby[1] = new Osoba("Stacey ", "Stadler", "Chantilly", "600999300", "42");
        osoby[2] = new Osoba("Daniel ", "Alex", "Washington", "317595164", "90");
        osoby[3] = new Osoba("Miriam ", "Landy", "New York", "727365873", "08");
        osoby[4] = new Osoba("Steven ", "Leisure", "Chantilly", "240272176", "50");

        firmy[0] = new Firma("poltex", "lodz", "700400300", "48");
        firmy[1] = new Firma("apple", "warszawa", "800400300", "44");
        firmy[2] = new Firma("google", "krakow", "600200300", "40");
        firmy[3] = new Firma("lenovo", "lodz", "500400300", "22");
        firmy[4] = new Firma("chrome", "poznan", "700800300", "48");

        TreeMap<NrTelefoniczny, Wpis> ksiazkaTel = new TreeMap<NrTelefoniczny, Wpis>();
        for(int i = 0; i < 5 ; i++){
            ksiazkaTel.put(osoby[i].telefon, osoby[i]);
            ksiazkaTel.put(firmy[i].telefon, firmy[i]);
        }

        Set set = ksiazkaTel.entrySet();
        Iterator iterator = set.iterator();
        while(iterator.hasNext()) {
            Map.Entry mentry = (Map.Entry)iterator.next();
            System.out.println(mentry.getValue());
        }
    }
}