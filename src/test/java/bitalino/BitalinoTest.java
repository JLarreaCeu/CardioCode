package bitalino;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Test: connects bitalino and shows 100 ECG samples
 */
public class BitalinoTest {
    public static void main(String[]args){
        BITalino bitalino= new BITalino();
        try{
            BufferedReader reader=new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Type the BITalino MAC address: ");
            String mac=reader.readLine();
            if(mac==null){
                return;
            }
            bitalino.open(mac.trim(),100); //100 samples per second
            System.out.print("Connected");

            int[]channels={1}; //A2
            bitalino.start(channels);

            for(int block=0;block<10;block++){
                Frame[] frames=bitalino.read(10); // blocks of 10 samples
                for(int i=0;i<frames.length;i++){
                    System.out.println("seq: "+frames[i].seq+ " ECG: "+frames[i].analog[0]);
                }
            }
            bitalino.stop();
        }catch (Throwable e){
            System.out.println("Error: "+e.getMessage());
        }
        finally{
            try{
                bitalino.close();
            } catch (BITalinoException e) {
                System.out.println("Error closing BITalino: "+e.getMessage());
            }
        }
    }
}
