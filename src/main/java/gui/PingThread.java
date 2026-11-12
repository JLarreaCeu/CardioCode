package gui;

import commonInfo.Pings;
import javafx.application.Platform;
import javafx.scene.control.Label;

import java.io.IOException;
import java.rmi.ServerError;

public class PingThread implements Runnable {
    private Pings ping;
    private GUI.App mainFrame;
    private static int INTERVAL= 5000; // esta en mili
    private volatile boolean running; //instantaneo

    public PingThread(Pings pings, GUI.App mainFrame) {
        this.ping = pings;
        this.mainFrame = mainFrame;
        this.running = true;
    }

    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(INTERVAL);
                if(running) {
                    ping.ping();
                }
            } catch (IOException e) {
                if(running) {

                    Platform.runLater(() -> {
                        mainFrame.showLogin();
                        mainFrame.showMessage("Server connection lost. Please log in again.");
                    });

                }
                stop();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                stop();
            }
        }
    }

    public void stop() {
        running = false;
    }
}
