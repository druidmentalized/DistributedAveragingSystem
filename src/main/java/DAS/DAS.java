package DAS;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

public class DAS {

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

    public DAS(int port, int number) {
        //deciding mode of the application
        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("Application entered MASTER mode");
            masterMode(socket, number);
        }
        catch (SocketException e) {
            System.out.println("Application entered SLAVE mode");
            slaveMode(port, number);
        }
    }

    private void masterMode(DatagramSocket socket, int number) {
        ArrayList<Integer> receivedNumbers = new ArrayList<>();
        receivedNumbers.add(number);

        try {
            while (true) {
                //opening buffer to read information
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                //reading information
                socket.receive(packet);
                String receivedMessage = new String(packet.getData(), 0, packet.getLength()).trim();
                int integerMessage = Integer.parseInt(receivedMessage);

                //processing received information
                if (integerMessage == 0) {
                    System.out.println("Number " + integerMessage + " received. Counting average value");
                    //counting average number
                    int avg = 0;
                    for (int num : receivedNumbers) {
                        avg += num;
                    }
                    avg /= receivedNumbers.size();

                    //sending computed number to everyone else
                    System.out.println("Broadcasting computed average number: " + avg);
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
                    System.out.println("Received number: " + integerMessage);
                    receivedNumbers.add(Integer.parseInt(receivedMessage));
                    System.out.println("Current numbers: " + receivedNumbers);
                }
            }
        }
        catch (IOException e) {
            System.out.println("Termination the application due to mistake during runtime");
            System.exit(1);
        }
    }

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