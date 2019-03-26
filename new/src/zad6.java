import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.*;
import java.util.Vector;

class WektoryRoznejDlugosciException extends Exception{
    WektoryRoznejDlugosciException(String msg){
        super(msg);
    }
}
public class zad6 {
    private static Vector<Integer> getNewVector(){
        Vector<Integer> vec = new Vector<>(10, 10);
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        String line = null;

        try{
            line = input.readLine();
        }catch(Exception e){
            System.out.println(e.getStackTrace());
        }
        String[] strs = line.trim().split("\\s+");

        for (int i = 0; i < strs.length; i++) {
            int temp;
            try {
                temp = Integer.parseInt(strs[i]);
            }catch(Exception e){
                continue;//ignorowanie wszystkiego co nie jest liczba
            }
            vec.add(temp);
        }
        return vec;
    }
    public static void main(String[] args){
        Boolean flag = false;
        do {
            System.out.println("input two vector of equal length: ");
            Vector<Integer> vec1 = getNewVector();
            Vector<Integer> vec2 = getNewVector();
            try {
                if (vec1.size() != vec2.size()) {
                    throw new WektoryRoznejDlugosciException("Dlugosc pierwszego wektora to "+vec1.size()+" a drugiego to "+vec2.size());
                } else {
                    flag = true;
                    //calculate sum
                    Vector<Integer> sum = new Vector(vec1.size());
                    for (int i = 0; i < sum.capacity(); i++) {
                        sum.add(vec1.get(i) + vec2.get(i));
                    }
                    System.out.println("sum = " + sum.toString());
                    //create file
                    String path = "sum.txt";
                    File f = new File(path);
                    try {
                        f.createNewFile();
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                    //save to file
                    FileOutputStream out = null;
                    PrintStream file = null;
                    try {
                        out = new FileOutputStream(f);
                        file = new PrintStream(out);
                        file.print(sum.toString());
                        if (out != null) out.close();
                        if (file != null) file.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                    System.out.println("sum saved to file: " + path);
                }
            } catch (WektoryRoznejDlugosciException e) {
                System.out.println(e.getMessage());
            }
        }while(flag == false);
    }
}
