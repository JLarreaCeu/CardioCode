package patient;

import java.io.IOException;

public class RecordingSession implements Runnable{

    /**
     * Receives what happens during the recording.
     */
    public interface Listener {
        /**
         * Called every time a block of samples is read.
         *
         * @param samples samples ecg values of the block
         * @throws IOException if block cannot be sent.
         */
        void onBlock(int[] samples) throws IOException;

        /**
         * Called once when recording ends.
         *
         * @param totalSamples number of samples recorded
         * @param error
         */
        void onFinished(int totalSamples, Exception error);
    }

    //Number of samples read each time: 0.1s.
    private static final int BLOCK_SIZE=BitalinoRecorder.SAMPLING_RATE/10;

    //Recorder used to get ECG
    private final BitalinoRecorder recorder;

    //Maximum duration in seconds
    private final int durationSeconds;

    //Receives each block and the end of the recording.
    private final Listener listener;


    private volatile boolean stopRequested=false;
    private volatile boolean running=false;
    private volatile int sampleCount=0;

    /**
     * Creates a recording session. It doesn't start until it is run in a thread
     * @param recorder
     * @param durationSeconds duration in seconds
     * @param listener receives each blocks of samples and end of recording.
     */
    public RecordingSession(BitalinoRecorder recorder, int durationSeconds,Listener listener){
        this.recorder=recorder;
        this.durationSeconds=durationSeconds;
        this.listener=listener;
    }

    /**
     * Body of thread: starts recorder, reads and gives over blocks until end condition reached.
     */
    @Override
    public void run(){
        running=true;
        Exception error=null;
        long maxSamples=(long) durationSeconds*BitalinoRecorder.SAMPLING_RATE;
        try{
            recorder.start();
            while(!stopRequested&&!isDurationReached(maxSamples)){
                int[]block=recorder.readBlock(BLOCK_SIZE);
                block=cutToDuration(block,maxSamples);
                listener.onBlock(block);
                sampleCount=sampleCount+ block.length;
            }
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
            error=e;
        }catch(Exception e){
            error=e;
        }finally{
            try{
                recorder.stop();
            }catch(Exception e){
                if(error==null){
                    error=e;
                }
            }
            running=false;
            listener.onFinished(sampleCount,error);
        }
    }

    public void stop(){
        stopRequested=true;
    }

    public boolean isRunning(){
        return running;
    }

    public int getSampleCount(){
        return sampleCount;
    }

    public double getRecorderSeconds(){
        return sampleCount/(double)BitalinoRecorder.SAMPLING_RATE;
    }

    private boolean isDurationReached(long maxSamples){
        if(durationSeconds<=0){
            return false;
        }
        return sampleCount>= maxSamples;
    }


    private int[] cutToDuration(int[]block, long maxSamples){
        if(durationSeconds<=0){
            return block;
        }
        long remaining=maxSamples-sampleCount;
        if(block.length<=remaining){
            return block;
        }
        int[]cut=new int[(int)remaining];;
        System.arraycopy(block,0,cut,0,cut.length);
        return cut;
    }
}
