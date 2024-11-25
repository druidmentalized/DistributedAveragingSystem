package DAS;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;

public class DAS {
    private static final String BROADCAST_ADDRESS = "255.255.255.255";

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
            e.printStackTrace();
        }
    }

    private void broadcastMessage(DatagramSocket socket, int number) {
        try {
            byte[] buffer = String.valueOf(number).getBytes();

            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(BROADCAST_ADDRESS), socket.getLocalPort());
            socket.setBroadcast(true);
            socket.send(packet);
        }
        catch (IOException e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }
}