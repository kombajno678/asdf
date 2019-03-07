import java.util.Random;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Random g = new Random();
        Scanner in = new Scanner(System.in);
        String again;
        do{

            int r = g.nextInt(100);
            System.out.println(r);
            System.out.println("zgadnij liczbe 0-100: ");
            int tries = 0, guess = 0;
            do {
                guess = in.nextInt();
                tries += 1;
                if(guess == r){
                    break;
                }else{
                    if(r > guess)
                        System.out.println("za malo");
                    else
                        System.out.println("za duzo");
                }
            }while(true);

            System.out.println("Brawo!, ilosc prob: " + tries);
            System.out.println("jeszcze raz? Y/N");

            again = in.next();

        }while(again.equals("Y") || again.equals("y"));

    }
}
