/**
 * ClearCommand.java
 * Author: Marshall Zhang
 * Student ID: 1160040
 * These are the commands that allow the server to communicate with the client.
 */

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;

public class ClearCommand implements Serializable {
}

class KickCommand implements Serializable {
    private String username;

    public KickCommand(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}

class OpenCommand implements Serializable {
    private ArrayList<Shape> shapes;

    public OpenCommand(ArrayList<Shape> shapes) {
        this.shapes = shapes;
    }

    public ArrayList<Shape> getShapes() {
        return shapes;
    }
}

class ServerQuitCommand implements Serializable {
    @Serial
    private static final long serialVersionUID = 8318430527895940854L;
}

class UsernameTakenCommand implements Serializable {
}