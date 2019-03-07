public class Main {

    public static void main(String[] args) {

        String txt = args[0];
        int start = Integer.parseInt(args[1]);
        int end = Integer.parseInt(args[2]);
        String new_txt = txt.substring(start, end+1);
        System.out.println(new_txt);
    }
}
