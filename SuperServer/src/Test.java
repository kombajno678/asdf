public class Test {
    Test(){
        int[] a = {5, 10, 20, 30};
        int sum = 0;
        for(int i = 0; i < a.length; i++){
            sum += a[i];
        }
        System.out.print("suma "+sum+"\n");
    }
    public static void main(String[] args){
        new Test();
    }
}
