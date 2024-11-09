/**
 * JoinWhiteBoard.java
 * Author: Marshall Zhang
 * Student ID: 1160040
 * This is the main class for the client side of the whiteboard application.
 */

import javax.swing.*;

public class JoinWhiteBoard {
    public static void main(String[] args) {
        // Check if the number of arguments is correct
        if (args.length != 3) {
            System.out.println("Usage: java JoinWhiteBoard <serverAddress> <serverPort> <username>");
            System.exit(1);
        }

        String serverAddress = args[0];
        int serverPort = Integer.parseInt(args[1]);
        String username = args[2];

        // Create a new client and make it visible
        SwingUtilities.invokeLater(() -> {
            WhiteboardClient client = new WhiteboardClient(serverAddress, serverPort, username);
            client.sendJoinRequest(username);
            client.setVisible(true);
        });
    }
}
