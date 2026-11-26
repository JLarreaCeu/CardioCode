package commonInfo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ServerFinder {

    public static final int DISCOVERY_PORT = 6768;
    public static final String REQUEST = "CARDIOCODE_DISCOVER";
    public static final String ANSWER = "CARDIOCODE_SERVER";
    private static final int TIMEOUT = 1500;

    public static String findServer() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            socket.setSoTimeout(TIMEOUT);

            byte[] request = REQUEST.getBytes(StandardCharsets.UTF_8);
            for (InetAddress target : getTargets()) {
                try {
                    socket.send(new DatagramPacket(request, request.length, target, DISCOVERY_PORT));
                } catch (IOException e) {
                    System.out.println("Could not search the server in " + target.getHostAddress());
                }
            }

            byte[] buffer = new byte[100];
            String serverAddress = null;
            while (serverAddress == null) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String text = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                if (text.equals(ANSWER)) {
                    serverAddress = packet.getAddress().getHostAddress();
                }
            }
            return serverAddress;
        } catch (SocketTimeoutException e) {
            return null;
        } catch (IOException e) {
            System.out.println("Could not search the server: " + e.getMessage());
            return null;
        }
    }

    private static List<InetAddress> getTargets() throws IOException {
        List<InetAddress> targets = new ArrayList<>();
        targets.add(InetAddress.getLoopbackAddress());
        targets.add(InetAddress.getByName("255.255.255.255"));

        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            if (networkInterface.isUp() && !networkInterface.isLoopback()) {
                for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
                    if (address.getBroadcast() != null) {
                        targets.add(address.getBroadcast());
                    }
                }
            }
        }
        return targets;
    }
}
