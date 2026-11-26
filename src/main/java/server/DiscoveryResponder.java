package server;

import commonInfo.ServerFinder;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class DiscoveryResponder implements Runnable {

    private DatagramSocket socket;
    private volatile boolean running = true;

    public DiscoveryResponder() throws SocketException {
        socket = new DatagramSocket(ServerFinder.DISCOVERY_PORT);
    }

    @Override
    public void run() {
        byte[] buffer = new byte[100];
        byte[] answer = ServerFinder.ANSWER.getBytes(StandardCharsets.UTF_8);
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String text = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                if (text.equals(ServerFinder.REQUEST)) {
                    socket.send(new DatagramPacket(answer, answer.length, packet.getAddress(), packet.getPort()));
                }
            } catch (IOException e) {
                if (running) {
                    System.out.println("Discovery error: " + e.getMessage());
                }
            }
        }
    }

    public void stop() {
        running = false;
        socket.close();
    }
}
