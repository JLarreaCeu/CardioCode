package patient;
import bitalino.BITalino;
import bitalino.BITalinoErrorTypes;
import bitalino.BITalinoException;
import bitalino.Frame;

import java.util.Random;

/**
 * Reads ECG samples form a real BITalino device or a simulated signal (test mode).
 */
public class BitalinoRecorder {

    //Sampling rate for all recordings, samples per second.
    public static final int SAMPLING_RATE=100;

    //analog channel where ECG snesor is connected (0=A1, 1=A2,..)
    private static final int ECG_CHANNEL=1;

    //name of ECG channel as stored in recording (channel field).
    public static final String ECG_CHANNEL_NAME="A2";

    //Supply voltage of BITalino, in volts (to convert to mV)
    private static final double VCC=3.3;

    //Gain of the BITalino ECG sensor (to convert to mV)
    private static final double ECG_GAIN=1100;

    //Duration of one simulated heartbeat in normal rhythm in seconds (75 beats per minute)
    private static final double FAKE_BEAT_LENGTH=0.8;

    //Shortest and longest simulated hertbeat in AF, in seconds.
    private static final double AF_MIN_BEAT_LENGTH=0.45;
    private static final double AF_MAX_BEAT_LENGTH=1.1;

    //True if the recorder generate a simulted signal instead of using the device
    private final boolean testMode;

    //Treu if the simulated signal is atrial fibrilation instead od a normal rhythm.
    private final boolean simulateAF;

    //MAC address of the BITalino device. Null in test mode.
    private final String macAdress;

    //Connection to real devide. Null in tst mode or when not recording.
    private BITalino bitalino;

    //Number of simulated samples generated so far, used to biuld fake signal
    private long sampleCounter=0;

    //Sample where the current simulated heartbeat started.
    private long beatStart=0;

    //Length of simulated heartbeat, in seconds.
    private double beatLength=FAKE_BEAT_LENGTH;

    //Random generator used for noise and irregualr beats in AF
    private final Random random=new Random();

    /**
     * Creates a test recorder in test mode that simulates a normal ECG.
     */
    public BitalinoRecorder(){
        this(false);
    }

    /**
     * Creates a recorder in test mode, which generates a simulated ECG instead
     * of connectign to a device.
     * @param simulateAF, true to simulate atrial fibrillation. False, to simulate a normal rhythm.
     */
    public BitalinoRecorder(boolean simulateAF){
        this.testMode=true;
        this.simulateAF=simulateAF;
        this.macAdress=null;
    }

    /**
     * Creates a recorder that reads from a real BITalino device.
     * @param macAdress, MAC adress of device.
     */
    public BitalinoRecorder(String macAdress){
        this.testMode=false;
        this.simulateAF=false;
        this.macAdress=macAdress;
    }

    /**
     * Shows if the recorder is using a simualted signal or not.
     * @return true, if the recorder is in test mode.
     */
    public boolean isTestMode(){
        return testMode;
    }

    /**
     * Connect to the device and starts the acquisition ont he ECG channel. In test mode
     * it only resets the simulated singal.
     * @throws BITalinoException if the connection with the device fails.
     */
    public void start()throws BITalinoException{
        if(testMode){
            sampleCounter=0;
            beatStart=0;
            beatLength=nextBeatLength();
            return;
        }
        bitalino=new BITalino();
        bitalino.open(macAdress,SAMPLING_RATE);
        try{
            bitalino.start(new int[]{ECG_CHANNEL});
        }catch (BITalinoException e){
            throw e;
        }catch(Throwable t){
            throw new BITalinoException(BITalinoErrorTypes.BT_DEVICE_NOT_CONNECTED);
        }
    }

    /**
     * Reads next block of ECG samples.
     * If it is test mode, it waits the same time as the real device does.
     * @param blockSize number of samples to read.
     * @return samples, array with ECG samples.
     * @throws BITalinoException if communication fails.
     * @throws InterruptedException if thread is interrupted in waiting mode.
     */
    public int[] readBlock(int blockSize) throws BITalinoException,InterruptedException{
        int[] samples=new int[blockSize];
        if(testMode){
            Thread.sleep(blockSize*1000L/SAMPLING_RATE);
            for(int i=0; i<blockSize;i++){
                samples[i]=fakeEcgSample();
            }
        }else{
            Frame[] frames=bitalino.read(blockSize);
            for (int i=0; i<frames.length;i++){
                samples[i]=frames[i].analog[0];
            }
        }
        return samples;
    }

    /**
     * Stops acquistion and closes the connection with the devide.
     * It isn't working in test mode.
     * @throws BITalinoException if the device cannot be stopped or closed.
     */
    public void stop()throws BITalinoException{
        if(testMode||bitalino==null){
            return;
        }
        try{
            bitalino.stop();
        }finally{
            bitalino.close();
            bitalino=null;
        }
    }

    /**
     * Converts a raw ECG value from the BITalino (0-1023) to mv.
     * @param adcValue raw value read from device
     * @return adcValue in mv.
     */
    public static double toMillivolts(int adcValue){
        return(adcValue/1024.0-0.5)*VCC/ECG_GAIN*1000;
    }

    /**
     * Generates the next sample of simulated ECG.
     * @return Simuated ECG value, between 0-1023.
     */
    private int fakeEcgSample() {
        double t=(sampleCounter-beatStart)/(double)SAMPLING_RATE;
        if(t>= beatLength){
            beatStart=sampleCounter;
            beatLength=nextBeatLength();
            t=0;
        }
        double value=512
                +wave(t,0.26,-30,0.010)    //Q WAVE
                +wave(t,0.25,300,0.012)    //R WAVE
                +wave(t,0.30,-60,0.010)   //S WAVE
                +wave(t,0.50,80,0.040)    //T WAVE
                +random.nextGaussian()*4; //noise
        if(simulateAF){
            double seconds=sampleCounter/(double)SAMPLING_RATE;
            value= value+8*Math.sin((2*Math.PI*6 *seconds));
        }else{
            value=value+wave(t,0.16,40,0.025); //P wave
        }
        sampleCounter++;
        return(int)Math.max(0,Math.min(1023,Math.round(value)));

    }

    /**
     * Chooses the length of the next simulated heartbeat.
     * @return length of heartbeat in seconds.
     */
    private double nextBeatLength() {
        if(simulateAF){
            return AF_MIN_BEAT_LENGTH+random.nextDouble()*(AF_MAX_BEAT_LENGTH-AF_MIN_BEAT_LENGTH);
        }
        return FAKE_BEAT_LENGTH;
    }

    /**
     * Calculates the value of bell-shaped (Gaussian) wave at given time
     * @param t time within the heartbeat, in seconds
     * @param center time where the wave reaches its peak, in seocnds
     * @param amplitude height of the peak (negative for a downward value)
     * @param width how wide the wave is, in seconds
     * @return value of wave at time t
     */

    private double wave(double t, double center,double amplitude, double width) {
        double x=(t-center)/width;
        return amplitude*Math.exp(-x*x/2);
    }

}

