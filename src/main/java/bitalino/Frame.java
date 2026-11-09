package bitalino;

/**
 * Represent a single sample, a frame received from BITalino device.
 * Each frame is returned by BITalino.read().
 */
public class Frame {
    /**
     *CRC: CRC4 result checks for frame
     * It is used to verify that the frame was trasnmitted without errors.
     */
    public int CRC;

    /**
     * seq: frame sequence number (0-15)
     * This number is incremented by 1 on each consecutive frame, and it overflows to 0 after 15 (it is a 4-bit number).
     * This number can be used to detect if frames were dropped while transmitting data.
     */
    public int seq;

    /**
     * Values of the analog input channels.
     * The first 4 channels range from 0 to 1023 (10-bit resolution) and
     * the remaining channels range from 0 to 63 (6-bit resolution).
     * Values are stored in the order of the channels activated when
     * starting the acquisition.
     */
    public int []analog=new int[6];

    /**
     * Array of digital ports (0 for low level, 1 for high level)
     * On the original BITalino the array contains: I1 I2 I3 I4.
     * On BITalino (r)evolution it contains: I1 I2 O1 O2.
     */
    public int[]digital=new int[4];
}
