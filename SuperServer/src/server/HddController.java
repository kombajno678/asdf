package server;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * <pre>
 * used to distribute operations on hdds evenly
 * when new file is sent to server, Connection  will 'ask' HddController for hdd number
 * on which number of active operations is the lowest, and Connection will save file on this hdd
 * </pre>
 */
class HddController{
    /**
     * list containing five interegs, each corresponds to a different hdd and contains number of active operations on this hdd
     */
    private List<Integer> hddNumberOfOperations = Arrays.asList(0, 0, 0, 0, 0);
    /**
     * reference to gui controller,
     * used to update file operations numbers in gui
     */
    private Controller gui;

    /**
     * @param c reference to gui controller
     */
    public HddController(Controller c) {
        gui = c;
    }

    /**
     * <pre>
     * used when server is downloading new file and wants to know oh which hdd file should be stored
     * adds 1 to corresponding hdd on hddNumberOfOperations list
     * </pre>
     * @return hdd number with the lowest number of active operations
     */
    public synchronized int addOperation(){
        int hddToReturn;
        int minOperations = Collections.min(hddNumberOfOperations);
        for(hddToReturn = 0; hddToReturn < hddNumberOfOperations.size(); hddToReturn++){
            if(hddNumberOfOperations.get(hddToReturn) == minOperations){
                hddNumberOfOperations.set(hddToReturn,hddNumberOfOperations.get(hddToReturn) + 1);
                gui.updateOperations(hddNumberOfOperations);
                //printStatus();
                return hddToReturn;
            }
        }
        return -1;
    }

    /**
     * <pre>
     * used when server is uploading file from specified hdd
     * adds 1 to corresponding hdd on hddNumberOfOperations list
     * </pre>
     * @param hddNo on which hdd operation has started
     */
    public synchronized void addOperation(int hddNo) {
        hddNumberOfOperations.set(hddNo,hddNumberOfOperations.get(hddNo) + 1);
        gui.updateOperations(hddNumberOfOperations);
        //printStatus();
    }

    /**
     * <pre>
     * used when operation on hss is finished;
     * subtracts 1 to corresponding hdd on hddNumberOfOperations list
     * </pre>
     * @param hddNo on which hdd operation has ended
     */
    public synchronized void endOperation(int hddNo){
        hddNumberOfOperations.set(hddNo,hddNumberOfOperations.get(hddNo) - 1);
        gui.updateOperations(hddNumberOfOperations);
        //printStatus();
    }

    /**
     * prints status of all hdds on system console
     */
    private void printStatus(){
        System.out.print("HDD STATUS:");
        for(int i = 0; i < hddNumberOfOperations.size(); i++)
            System.out.print(" ["+(i+1)+"]:"+hddNumberOfOperations.get(i));
        System.out.println();
    }
}
