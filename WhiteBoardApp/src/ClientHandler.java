/**
 * ClientHandler.java
 * Author: Marshall Zhang
 * Student ID: 1160040
 * This file contains the ClientHandler class, which is responsible for handling
 * communication between the server and a single client.
 */

import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class ClientHandler implements Runnable {
    private Socket socket;
    private WhiteboardServer server;
    private ObjectOutputStream out;
    private String username;
    private boolean isFirstJoin;

    /**
     * Constructor for ClientHandler
     *
     * @param socket the socket to communicate with the client
     * @param server the server that the client is connected to
     */
    public ClientHandler(Socket socket, WhiteboardServer server) {
        this.socket = socket;
        this.server = server;
        this.isFirstJoin = true;
    }

    /**
     * Main method for handling communication between the server and the client
     */
    @Override
    public void run() {
        try {
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());

            this.username = (String) in.readObject();
            // Check if username is taken
            if (server.isUsernameTaken(username, this.socket)) {
                out.writeObject(new UsernameTakenCommand());
                out.reset();
                socket.close();
            }
            server.updateUserList();

            // Send current shapes to new client
            if (this.isFirstJoin && !server.getShapes().isEmpty()) {
                for (Shape shape : server.getShapes()) {
                    sendShape(shape);
                }
                this.isFirstJoin = false;
            }

            Object inputObject;
            while ((inputObject = in.readObject()) != null) {
                if (inputObject instanceof String) {
                    // Broadcast message to all clients
                    server.broadcastMessage(username + ": " + inputObject);
                } else if (inputObject instanceof Shape) {
                    // Broadcast shape to all clients
                    server.addShape((Shape) inputObject, out);
                } else if (inputObject instanceof ClearCommand) {
                    // Clear all shapes
                    server.clearAllShapes(out);
                } else if (inputObject instanceof OpenCommand openCommand) {
                    // Clear all shapes and open new board
                    server.clearAllShapes(out);
                    server.getShapes().addAll(openCommand.getShapes());
                    server.openNewBoard(out);
                } else if (inputObject instanceof JoinRequest joinRequest) {
                    // Notify manager of join request
                    boolean isApproved = server.notifyManager(joinRequest);
                    JoinResponse joinResponse = new JoinResponse(isApproved);
                    out.writeObject(joinResponse);
                    out.reset();
                } else if (inputObject instanceof ServerQuitCommand) {
                    // Shutdown server
                    server.shutdown();
                } else if (inputObject instanceof KickCommand kickCommand) {
                    // Kick user
                    server.kickUser(kickCommand.getUsername());
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error handling client: " + e.getMessage());
        } finally {
            // Remove client from server
            server.getClients().remove(this);
            server.updateUserList();
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Error closing socket: " + e.getMessage());
            }
        }
    }

    public String getUsername() {
            return username;
    }

    /**
     * Sends a message to the client
     *
     * @param message the message to send
     */
    public void sendMessage(String message) {
        try {
            out.writeObject(message);
            out.reset();
        } catch (IOException e) {
            System.out.println("Error sending message to client: " + e.getMessage());
        }
    }

    /**
     * Sends a shape to the client
     *
     * @param shape the shape to send
     */
    public void sendShape(Shape shape) {
        try {
            out.writeObject(shape);
            out.reset();
        } catch (IOException e) {
            System.out.println("Error sending shape to client: " + e.getMessage());
        }
    }

    public ObjectOutputStream getOut() {
        return out;
    }

    /**
     * Sends a clear command to the client
     */
    public void sendClearCommand() {
        try {
            out.writeObject(new ClearCommand());
            out.reset();
        } catch (IOException e) {
            System.out.println("Error sending clear command to client: " + e.getMessage());
        }
    }

    /**
     * Sends an open command to the client
     *
     * @param shapes the shapes to send
     */
    public void sendOpenCommand(ArrayList<Shape> shapes) {
        try {
            out.writeObject(new OpenCommand(shapes));
            out.reset();
        } catch (IOException e) {
            System.out.println("Error sending open command to client: " + e.getMessage());
        }
    }

    public Socket getSocket() {
        return socket;
    }

    /**
     * Closes the connection with the client
     */
    public void closeConnection() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing socket: " + e.getMessage());
        }
    }
}
