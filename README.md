## Distributed Averaging System
The Distributed Averaging System (DAS) is a Java-based application that facilitates communication between processes running in master and slave modes using UDP sockets. The application allows users to send and receive numeric values, compute averages, and broadcast these results across a local network. The DAS application is structured to handle both master and slave operations and provides automatic mode selection based on the availability of the UDP port.

## Features
- Master Mode
- Slave Mode
- UDP Communication

## Usage
- Application after compilation is started through command line with two additional arguments in a way: `java DAS <port> <number>` Where:
    - `<port>` - UDP port number
    - `<number>` - Initial number for the application
- Example output:
    - `Master` Mode:
    ```
    Application entered MASTER mode
    Received number: 100
    Current numbers: [100, 100]
    Received number: 100
    Current numbers: [100, 100, 100]
    Number 0 received. Counting average value
    Broadcasting computed average number: 100
    Number -1 was received. Broadcasting number: -1
    Terminating the application
    ```
    - `Slave` mode:
    ```
    Application entered SLAVE mode
    Number -1 sent to port 5058. 
    Terminating the application
    ```

## Built with
- Java & Java.net
- UDP protocol

## Project Structure
- `DAS.java` - Entry point of the program
- `src/maim/java/DAS` - Directory of the class
- `README.md` - Project documentation
- `LICENSE` - Project license

## Licence
This project is licensed under the MIT License
