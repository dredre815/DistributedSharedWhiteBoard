/**
 * JoinRequest.java
 * Author: Marshall Zhang
 * Student ID: 1160040
 * These are the classes that are used to send and receive join requests from the server.
 */

import java.io.Serializable;

public class JoinRequest implements Serializable {
    private String username;

    public JoinRequest(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}

record JoinResponse(boolean approved) implements Serializable {
}