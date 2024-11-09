/**
 * CreateWhiteBoard.java
 * Author: Marshall Zhang
 * Student ID: 1160040
 * CreateWhiteBoard is the main class that starts the server and manager for the whiteboard application.
 */

import javax.swing.*;

public class CreateWhiteBoard {
    private static WhiteboardServer server;

    public static void main(String[] args) {
        // Check if the correct number of arguments are provided
        if (args.length != 3) {
            System.out.println("Usage: java CreateWhiteBoard <serverAddress> <serverPort> <username>");
            System.exit(1);
        }

        String serverAddress = args[0];
        int serverPort = Integer.parseInt(args[1]);
        String username = args[2];

        // Start the server in a new thread
        new Thread(() -> {
            try {
                server = new WhiteboardServer(serverPort);
                server.listenForClients();
            } catch (Exception e) {
                System.out.println("Error creating server: " + e.getMessage());
            }
        }).start();

        // Start the manager in the event dispatch thread
        SwingUtilities.invokeLater(() -> {
            WhiteboardManager manager = new WhiteboardManager(serverAddress, serverPort, username);
            server.setManager(manager);
            manager.setVisible(true);
        });
    }
}
