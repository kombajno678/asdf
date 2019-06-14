package server.chat;


//thread with pool listeners

import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * accepts new connections on listener port
 * (client's speaker connects here)
 */
class ThreadPoolListener implements Runnable{
    protected Thread t;
    private boolean flag = true;
    private int port, nThreads;
    private ServerSocket socket;
    private ExecutorService pool;
    private ChatServer cm;

    /**
     *
     * @param port server's listener port
     * @param nThreads maximum number of listener threads
     * @param cm reference to ChatServer thread
     */
    ThreadPoolListener(int port, int nThreads, ChatServer cm) {
        this.port = port;
        this.nThreads = nThreads;
        this.cm = cm;
        //System.out.println("ThreadPoolListener started.");
    }

    @Override
    public void run() {
        pool = null;
        socket = null;
        while (socket == null) {
            try {
                socket = new ServerSocket(port);
                pool = Executors.newFixedThreadPool(nThreads);
                break;
            } catch (Exception e) {
                System.out.println("Failed to create server socket " + e.getMessage() + "\nTrying again in 10s ...\n");
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ee) {
                }
            }
        }
        //System.out.println("ThreadPoolListener is running.");
        while (flag) {
            try {
                pool.execute(new ConnectionListener(socket.accept(), cm));
            } catch (Exception e) {
                if (!flag) break;
                System.out.println("Failed to add new ConnectionListener");
            }
        }
        //System.out.println("ThreadPoolListener end");

    }
    public void start(){
        if (t == null) {
            t = new Thread (this, "ThreadPoolListener_Thread");
            t.start ();
        }
    }
    public void stop() {
        flag = false;
        try{
            socket.close();
        }catch(Exception e){}
        pool.shutdown();
        t.interrupt();
    }
}
