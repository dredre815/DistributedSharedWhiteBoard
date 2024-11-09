# Collaborative Whiteboard Application

A Java-based collaborative whiteboard application that allows multiple users to draw and interact in real-time. The application follows a client-server architecture with support for multiple drawing tools and user management features.

## Features

### Drawing Tools
- Line drawing
- Rectangle shapes
- Oval shapes
- Circle shapes
- Free-hand drawing
- Text insertion
- Eraser tool

### Collaboration Features
- Real-time multi-user collaboration
- Manager/Client role system
- User join request approval system
- User kick functionality
- Shared canvas state synchronization

### File Operations
- New whiteboard creation
- Save whiteboard state
- Save As functionality
- Open saved whiteboards
- File persistence support

## Technical Details

### Core Components

1. **Shape System**
   - Abstract Shape base class
   - Multiple shape implementations (Line, Rectangle, Oval, Circle, etc.)
   - Serializable shape objects for persistence
   - Color and coordinate management

2. **Whiteboard Manager**
   - User management system
   - Join request handling
   - Client kick functionality
   - Menu bar with file operations
   - Canvas state management

### Architecture
- Client-Server model
- Java Swing-based GUI
- Serializable objects for network transmission
- Event-driven drawing system

## Requirements
- Java Runtime Environment (JRE)
- Java Development Kit (JDK)
- Network connectivity for multi-user functionality

## Usage

### Starting the Application

1. Launch the server:
```bash
java WhiteboardServer <port>
```

2. Start the manager instance:
```bash
java WhiteboardManager <serverAddress> <port> <username>
```

3. Connect clients:
```bash
java WhiteboardClient <serverAddress> <port> <username>
```

### Manager Controls
- Use the File menu for whiteboard operations
- Approve/deny join requests
- Kick users using the username field and kick button
- Access all drawing tools

### Client Controls
- Request join permission from manager
- Use available drawing tools
- View real-time updates from other users

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.