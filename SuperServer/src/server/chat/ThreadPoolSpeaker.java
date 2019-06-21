package server.chat;

import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * accepts new connections on speaker port
 * (client's listener connects here)
 */
class ThreadPoolSpeaker implements Runnable{
    /**
     * thread
     */
    protected Thread t;
    /**
     * user in main thread loop
     */
    private boolean flag = true;
    /**
     * chat's speaker port
     */
    private int port;
    /**
     * maximum number of threads
     */
    private int nThreads;
    /**
     * chat's speaker socket
     */
    private ServerSocket socket;
    /**
     * chat speakers thread pool
     */
    private ExecutorService pool;
    /**
     * reference to ChatServer
     */
    private ChatServer cm;

    /**
     *
     * @param port server's listener port
     * @param nThreads maximum number of listener threads
     * @param cm reference to ChatServer thread
     */
    ThreadPoolSpeaker(int port, int nThreads, ChatServer cm) {
        this.port = port;
        this.nThreads = nThreads;
        this.cm = cm;
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
        while (flag) {
            try {
                pool.execute(new ConnectionSpeaker(socket.accept(), cm));
            } catch (Exception e) {
                if (!flag) break;
                System.out.println("Failed to add new ConnectionSpeaker");
            }
        }
    }
    public void start(){
        if (t == null) {
            t = new Thread (this, "ThreadPoolSpeaker_Thread");
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

