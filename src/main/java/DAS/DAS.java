package DAS;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

/**
 * The DAS class implements a simple distributed application system that operates
 * in either MASTER or SLAVE mode depending on the availability of the specified port.
 * - MASTER mode listens on the specified port for incoming messages, processes them,
 *   and broadcasts calculated values like the average of received numbers.
 * - SLAVE mode sends a specified number to the MASTER mode application.
 * Usage: `java DAS port number`
 */
public class DAS {

    /**
     * The entry point of the application.
     * Validates input arguments, parses them, and initializes the application.
     *
     * @param args Command-line arguments where:
     *             args[0] specifies the port number (integer between 0 and 65535),
     *             args[1] specifies a number to process.
     */
    public static void main(String[] args) {
        //checking for appropriate input
        if (args.length != 2) {
            System.err.println("Amount of arguments is not correct!");
            System.err.println("Correct usage: java DAS <port> <number>");
            System.exit(1);
        }

        int port = 0;
        int number = 0;

        try {
            port = Integer.parseInt(args[0]);
            if (port < 0 || port > 65535) {
                throw new Exception();
            }
        }
        catch (Exception e) {
            System.err.println("Port input is incorrect! Must be an integer value between 0 and 65535");
            System.exit(1);
        }

        try {
            number = Integer.parseInt(args[1]);
        } catch (Exception e) {
            System.err.println("Number input is incorrect! Should be an integer value");
            System.exit(1);
        }

        new DAS(port, number);
    }

    /**
     * Initializes the DAS application and determines the mode of operation (MASTER or SLAVE).
     *
     * @param port   The port to be used for communication.
     * @param number The number to be sent or processed.
     */
    public DAS(int port, int number) {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("Application entered MASTER mode");
            masterMode(socket, number);
        }
        catch (SocketException e) {
            System.out.println("Application entered SLAVE mode");
            slaveMode(port, number);
        }
    }

    /**
     * Operates the application in MASTER mode, listening for messages,
     * processing numbers, and broadcasting results.
     *
     * @param socket The DatagramSocket for communication.
     * @param number The initial number to process.
     */
    private void masterMode(DatagramSocket socket, int number) {
        ArrayList<Integer> receivedNumbers = new ArrayList<>();
        receivedNumbers.add(number);
        boolean shouldIgnoreAverage = false;

        try {
            while (true) {
                //opening buffer to read information
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                //reading information
                socket.receive(packet);
                String receivedMessage = new String(packet.getData(), 0, packet.getLength()).trim();
                int integerMessage;
                try {
                    integerMessage = Integer.parseInt(receivedMessage);
                }
                catch (Exception e) {
                    System.out.println("Incorrect format of input value!");
                    continue;
                }


                //processing received information
                if (integerMessage == 0) {
                    System.out.println("Number " + integerMessage + " received. Counting average value");
                    //counting average number

                    int avg = countAverage(receivedNumbers);

                    //sending computed number to everyone else
                    System.out.println("Broadcasting computed average number: " + avg);
                    shouldIgnoreAverage = true;
                    broadcastMessage(socket, avg);
                }
                else if (integerMessage == -1) {
                    //sending -1 to everyone else
                    System.out.println("Number -1 was received. Broadcasting number: " + integerMessage);
                    broadcastMessage(socket, integerMessage);

                    //terminating the application
                    System.out.println("Terminating the application");
                    socket.close();
                    System.exit(0);
                }
                else {
                    if (integerMessage == countAverage(receivedNumbers) && shouldIgnoreAverage) {
                        shouldIgnoreAverage = false;
                        continue;
                    }
                    System.out.println("Received number: " + integerMessage);
                    receivedNumbers.add(integerMessage);
                    System.out.println("Current numbers: " + receivedNumbers);
                }
            }
        }
        catch (IOException e) {
            System.out.println("Termination the application due to mistake during runtime");
            System.exit(1);
        }
    }

    /**
     * Calculates the average of the numbers received.
     *
     * @param receivedNumbers The list of received numbers.
     * @return The calculated average.
     */
    private int countAverage(ArrayList<Integer> receivedNumbers) {
        int avg = 0;
        for (int num : receivedNumbers) {
            avg += num;
        }
        avg /= receivedNumbers.size();
        return avg;
    }

    /**
     * Broadcasts a message to all devices in the local network.
     *
     * @param socket The DatagramSocket used for communication.
     * @param number The number to broadcast.
     */
    private void broadcastMessage(DatagramSocket socket, int number) {
        try {
            byte[] buffer = String.valueOf(number).getBytes();

            //getting broadcast address and handling failure
            InetAddress broadcastAddress = calculateBroadcastAddress();
            if (broadcastAddress == null) {
                System.out.println("Unable to calculate broadcast address");
                return;
            }

            //broadcasting message to all user in local network
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, socket.getLocalPort());
            socket.setBroadcast(true);
            socket.send(packet);
        }
        catch (IOException e) {
            System.out.println("Broadcasting message went wrong");
        }
    }

    /**
     * Calculates the broadcast address based on the local network configuration.
     *
     * @return The broadcast address or null if it cannot be determined.
     */
    private InetAddress calculateBroadcastAddress() {
        //finding IP address
        InetAddress localIPAddress;
        try {
            localIPAddress = Inet4Address.getLocalHost();
        } catch (UnknownHostException e) {
            System.out.println("Unable to find local IP address!");
            return null;
        }

        //taking network interface according to the IP address we retrieved
        NetworkInterface networkInterface;
        try {
            networkInterface = NetworkInterface.getByInetAddress(localIPAddress);
        } catch (SocketException e) {
            System.out.println("Unable to find corresponding network interface!");
            return null;
        }

        //finding proper prefix length for IPv4
        short prefixLength = -1;
        for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
            if (address.getAddress() instanceof Inet4Address) {
                prefixLength = address.getNetworkPrefixLength();
            }
        }
        if (prefixLength == -1) {
            System.out.println("Couldn't find proper prefix length!");
            return null;
        }

        return bitwiseCalculation(localIPAddress, prefixLength);
    }

    /**
     * Performs bitwise calculation to determine the broadcast address.
     *
     * @param IPAddress    The local IP address.
     * @param prefixLength The subnet mask prefix length.
     * @return The calculated broadcast address.
     */
    private InetAddress bitwiseCalculation(InetAddress IPAddress, short prefixLength) {
        //taking byte representation of the ip and subnet mask
        byte[] IPBytes = IPAddress.getAddress();
        int subnetMask = -(1 << (32 - prefixLength));
        byte[] maskBytes = {
                (byte) ((subnetMask >> 24) & 0xFF),
                (byte) ((subnetMask >> 16) & 0xFF),
                (byte) ((subnetMask >> 8) & 0xFF),
                (byte) (subnetMask & 0xFF)
        };

        //computing broadcast address
        byte[] broadcastAddress = new byte[4];
        for (int i = 0; i < 4; i++) {
            broadcastAddress[i] = (byte) (IPBytes[i] | ~maskBytes[i]);
        }

        try {
            return InetAddress.getByAddress(broadcastAddress);
        } catch (UnknownHostException e) {
            System.out.println("Couldn't calculate broadcast address!");
            return null;
        }
    }

    /**
     * Operates the application in SLAVE mode, sending a number to the MASTER mode application.
     *
     * @param port   The port to which the number will be sent.
     * @param number The number to send.
     */
    private void slaveMode(int port, int number) {
        String hostname = "localhost";
        try {
            //making socket with random number
            DatagramSocket socket = new DatagramSocket();

            //wrapping sent number into buffer
            byte[] buffer = String.valueOf(number).getBytes();

            //sending the number
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(hostname), port);
            socket.send(packet);

            //terminating the process
            System.out.println("Number " + number + " sent to port " + port + ". Terminating the application");
            socket.close();
            System.exit(0);
        }
        catch (IOException e) {
            System.out.println("Termination the application due to mistake during runtime");
            System.exit(1);
        }
    }
}