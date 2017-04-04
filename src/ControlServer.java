import java.io.*;
import java.net.*;

/**
 * ControlServer
 *
 * @author :   Andrew Dichabeng
 * @version :   1.5 [Final Iteration]
 */

public class ControlServer extends Thread {

    /* instance variables */
    DatagramSocket sendSocket;
    DatagramPacket sendPacket;

    /* constants */
    private final int SEND_PORT = 69;

    /**
     * ControlServer constructor
     */
    public ControlServer() throws SocketException {
        sendSocket = new DatagramSocket();
    }

    /**
     * Method to run the ControlServer.
     */
    public void run() {

        try {
            userOrders();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to prompt the user to shut down the Server.
     *
     * @throws IOException -   Throws an IOException.
     */
    public void userOrders() throws IOException {

        String popUpMessage = "Type 'Shut' to shutdown server";
        String userChoice = "";

        byte[] data = {0, 0};      // data sent to Server to shut it down.

        for (; ; ) {

            System.out.println(popUpMessage);
            BufferedReader a = new BufferedReader(new InputStreamReader(System.in));

            try {
                userChoice = a.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (userChoice.equalsIgnoreCase("shut")) {
                // prepare and send shutdown packet to the server
                sendPacket = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), SEND_PORT);
                sendSocket.send(sendPacket);
                break;
            }
        }
    }

    /**
     * Main method.
     */
    public static void main(String[] args) throws SocketException {
        Thread c = new ControlServer();
        c.start();
    }

}
