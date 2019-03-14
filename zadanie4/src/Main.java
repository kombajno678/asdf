//zadanie 4
import java.util.Random;
import java.io.*;

class Fileop{
    public void Read(){
        String line = null;
        try {
            FileReader fileReader = new FileReader("out_new.txt");
            BufferedReader buffer = new BufferedReader(fileReader);
            while((line = buffer.readLine())!=null){
                System.out.println(line);
            }
            buffer.close();

        }catch(Exception e){

        }

    }
    public void Write(String in){
        //String in = "test123";
        File f = new File("out_new.txt");
        try{
            f.createNewFile();
        }catch(Exception e){
            System.out.println(e.getStackTrace());
        }

        FileOutputStream out = null;
        try{
            out = new FileOutputStream(f);
            PrintStream file = new PrintStream(out);
            file.print(in);
        } catch (Exception e) {
            e.printStackTrace();
        } finally{
            //if(out != null)out.close();
            //if(file!= null)file.close();
        }
    }
}

public class Main {


    public static void main(String[] args) {


        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 1000;
        Random random = new Random();
        StringBuilder buffer = new StringBuilder(targetStringLength);
        for (int i = 0; i < targetStringLength; i++) {
            int randomLimitedInt = leftLimit + (int)
                    (random.nextFloat() * (rightLimit - leftLimit + 1));
            buffer.append((char) randomLimitedInt);
        }
        String generatedString = buffer.toString();

        //System.out.println(generatedString);
        Fileop a = new Fileop();
        a.Write(generatedString);
        a.Read();

    }
}
