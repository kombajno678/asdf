package server;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * used to distribute hdd operations evenly
 * when new file is sent to server, Connection  will 'ask' HddController for hdd number
 * on which number of active operations is the lowest, and Connection will save file on this hdd
 */
class HddController{

    private List<Integer> hddNumberOfOperations = Arrays.asList(0, 0, 0, 0, 0);
    private Controller gui;

    /**
     *
     * @param c reference to gui controller
     */
    public HddController(Controller c) {
        gui = c;
    }

    /**
     * used when server is downloading new file and wants to know oh which hdd file should be stored
     * adds 1 to corresponding hdd on hddNumberOfOperations list
     * @return hdd number with the lowest number of active operations
     */
    public synchronized int addOperation(){
        int hddToReturn;
        //find hdd with min operations
        int minOperations = Collections.min(hddNumberOfOperations);
        for(hddToReturn = 0; hddToReturn < hddNumberOfOperations.size(); hddToReturn++){
            if(hddNumberOfOperations.get(hddToReturn) == minOperations){
                hddNumberOfOperations.set(hddToReturn,hddNumberOfOperations.get(hddToReturn) + 1);
                gui.updateOperations(hddNumberOfOperations);
                printStatus();
                return hddToReturn;
            }
        }
        return -1;
    }

    /**
     * used when server is uploading file from specified hdd
     * adds 1 to corresponding hdd on hddNumberOfOperations list
     * @param hddNo on which hdd operation has started
     */
    public synchronized void addOperation(int hddNo) {
        hddNumberOfOperations.set(hddNo,hddNumberOfOperations.get(hddNo) + 1);
        gui.updateOperations(hddNumberOfOperations);
        printStatus();
    }

    /**
     * used when operation on hss is finished
     * subtracts 1 to corresponding hdd on hddNumberOfOperations list
     * @param hddNo on which hdd operation has ended
     */
    public synchronized void endOperation(int hddNo){
        hddNumberOfOperations.set(hddNo,hddNumberOfOperations.get(hddNo) - 1);
        gui.updateOperations(hddNumberOfOperations);
        printStatus();
    }

    /**
     * prints status of all hdds on console
     */
    private void printStatus(){
        System.out.print("HDD STATUS:");
        for(int i = 0; i < hddNumberOfOperations.size(); i++)
            System.out.print(" ["+(i+1)+"]:"+hddNumberOfOperations.get(i));
        System.out.println();
    }
}
