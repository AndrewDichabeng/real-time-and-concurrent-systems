import java.io.*;
import java.net.*;

/**
 * Assistant
 *
 * @author :   Andrew Dichabeng
 * @version :   1.5 [Final Iteration]
 */

public class Assistant {

    /* constants */
    public static byte RRQ_OPCODE = 1;      // Read Request OpCode
    public static byte WRQ_OPCODE = 2;      // Write Request OpCode
    public static byte DATA_OPCODE = 3;     // Data Transfer OpCode
    public static byte ACK_OPCODE = 4;      // Acknowledgement OpCode
    public static byte ERROR_OPCODE = 5;    // Error OpCode
    private static byte ACK_SIZE = 4;       // Size of Acknowledgement Array
    private static byte ERROR_SIZE = 50;    // Size of Error Message

    /**
     * Method to resize a byte array to specific size.
     *
     * @param elongatedData -   The byte array to be resized.
     * @param newSize       -   The new size of the byte array to be resized to.
     * @return -   Returns the resized byte array.
     */
    private static byte[] trimArray(byte[] elongatedData, int newSize) {
        byte[] trimmedData = new byte[newSize];
        System.arraycopy(elongatedData, 0, trimmedData, 0, trimmedData.length);
        return trimmedData;
    }

    /**
     * Method to trim an array to ACK_SIZE of the DatagramPacket.
     *
     * @param packet -   The DatagramPacket with data packet.
     * @return -   Returns the trimmed array.
     */
    public static byte[] trimArrayToAck(DatagramPacket packet) {
        byte[] elongatedData = packet.getData();
        return trimArray(elongatedData, ACK_SIZE);
    }

    /**
     * Method to trim and array to  ERROR_SIZE of the DatagramPacket.
     *
     * @param packet -   The DatagramPacket with data packet.
     * @return -   Returns the trimmed array.
     */
    public static byte[] trimArrayToError(DatagramPacket packet) {
        byte[] elongatedData = packet.getData();
        return trimArray(elongatedData, ERROR_SIZE);
    }

    /**
     * Method to check if a packet is an error packet.
     *
     * @param packet -   The DatagramPacket with data packet.
     * @return -   Returns true if the packet is an error packet.
     */
    public static boolean isErrorPacket(DatagramPacket packet) {
        if (packet.getData()[1] == ERROR_OPCODE) {
            return true;
        }
        return false;
    }

    /**
     * Method to check if the data in the Datagram packet occurs as expected.
     *
     * @param packet -   The DatagramPacket with data packet.
     * @param opCode -   The expected opcode value.
     * @param b1     -   The start byte of the block to be verified. (MSB)
     * @param b0     -   The stop byte of the block to be verified.  (LSB)
     * @return -   Returns an int corresponding to a message :
     * -   1 : If an unexpected opcode was found.
     * -   2 : If an unexpected block number was found.
     * -   3 : If an unexpected opcode and block number was found.
     * -   0 : If the data was found to be as expected.
     */
    public static int asExpected(DatagramPacket packet, byte opCode, byte b1, byte b0) {

        byte[] data = packet.getData();
        int returnValue = 0;            // default value = data is as expected.
        int block = printBlock(b1, b0);

        if (block - 2 > 0) {
            if (data[1] != opCode) {
                returnValue = 1;
            }
            if ((printBlock(data[2], data[3]) > (block + 2) || printBlock(data[2], data[3]) < (block - 2)) && data[1] != 5) {
                returnValue = 2;
            }
            if (data[1] != opCode && returnValue == 2) {
                returnValue = 3;
            }
        }
        return returnValue;
    }

    /**
     * Method print a byte array block given the MSB and LSB.
     *
     * @param b1 -   The start byte of the block to be verified. (MSB)
     * @param b0 -   The stop byte of the block to be verified.  (LSB)
     * @return -   Returns the equivalent int value.
     */
    public static int printBlock(byte b1, byte b0) {
        return (int) b1 * 128 + b0;
    }

    /**
     * Method to send an error packet.
     *
     * @param errorOpcode -   The error opcode.
     * @param sendSocket  -   The DatagramSocket sendSocket with port number.
     * @param packet      -   The DatagramPacket with the IP address.
     */
    public static void sendErrorPacket(byte errorOpcode, DatagramSocket sendSocket, DatagramPacket packet) {
        sendErrorPacket(errorOpcode, sendSocket, packet.getPort(), packet.getAddress());
    }

    /**
     * Method to send an error packet.
     *
     * @param errorOpcode     -   The error opcode.
     * @param sendSocket      -   The DatagramSocket sendSocket with port number.
     * @param destinationPort -   The destination port number.
     * @param address         -   The InetAddress IP address.
     */
    public static void sendErrorPacket(byte errorOpcode, DatagramSocket sendSocket, int destinationPort, InetAddress address) {

        byte[] messageArray;
        String message = "";
        int cursor = 0;         // a cursor place holder.

        //prepare the error message depending on the errorOpcode
        if (errorOpcode == (byte) 1) {
            message = "Error 1: File not found";
        } else if (errorOpcode == (byte) 2) {
            message = "Error 2: File can not be accessed";
        } else if (errorOpcode == (byte) 3) {
            message = "Error 3: Disk full";
        } else if (errorOpcode == (byte) 4) {
            message = "Error 4: Packet corrupted";
        } else if (errorOpcode == (byte) 5) {
            message = "Error 5: Duplicated request";
        } else if (errorOpcode == (byte) 6) {
            message = "Error 6: File already exists";
        }

        messageArray = message.getBytes();

        // 2 bytes for the error opcode {0,5} then 2 bytes for the error type opcode {0,#} the msg and a byte for 0
        int length = 5 + messageArray.length;

        //Constructing the array for the error packet
        byte[] errorArray = new byte[length];
        errorArray[0] = 0;
        errorArray[1] = 5;
        errorArray[2] = 0;
        errorArray[3] = errorOpcode;
        cursor = 4;     // current position in array

        System.arraycopy(messageArray, 0, errorArray, cursor, messageArray.length);     // add an error message
        cursor += messageArray.length;      // update the cursor position
        errorArray[cursor] = 0;             //  add a zero(0) to the end of the array.

        try {   // try to create the packet to be sent
            DatagramPacket errorPacket = new DatagramPacket(errorArray, errorArray.length, address, destinationPort);
            try {
                sendSocket.send(errorPacket);
            } catch (SocketException e) {
                System.out.println("Could not create socket to send the error packet.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Method to extract the file name from the RRQ and WRQ packet.
     *
     * @param data -   The byte array containing data.
     * @return -   Returns the String value of the file to be read from or written from.
     */
    public static String extractFileName(byte[] data) {

        byte[] fileNameInBytes = new byte[100];

        for (int i = 2; data[i] != 0b0; i++) {
            fileNameInBytes[i - 2] = data[i];
        }

        System.out.println("The file name is " + new String(fileNameInBytes).trim());
        return new String(fileNameInBytes).trim();

    }

    /**
     * Method to extract the error message from the error packet.
     *
     * @param data -   The byte array that contains the data.
     * @return -   Returns the value of the error message.
     */
    public static String extractErrorMessage(byte[] data) {

        byte[] errorMessageInBytes = new byte[100];

        for (int i = 4; data[i] != 0b0; i++) {
            errorMessageInBytes[i - 4] = data[i];
        }

        System.out.println(new String(errorMessageInBytes).trim());
        return new String(errorMessageInBytes).trim();
    }

    /**
     * Method to extract the mode name from the Read/Write request packet.
     *
     * @param data -   The byte array containing the data.
     * @return -   Returns the String value of the mode name.
     */
    public static String extractModeName(byte[] data) {

        byte[] modeNameInBytes = new byte[20];

        int mode = -1;
        for (int i = 2; data[i] != 0b0; i++) {
            mode = i + 2;
        }

        if (mode != -1) {
            for (int i = 0; data[mode] != 0b0; i++) {
                modeNameInBytes[i] = data[mode];
                mode++;
            }
        }

        return new String(modeNameInBytes).trim();
    }

    /**
     * Method to fill the rest of an byte array with zeros(0)
     *
     * @param data -   The byte array to be filled.
     * @return -   Returns the filled length of the array.
     */
    public static int lengthFilled(byte[] data) {

        for (int i = 4; i < data.length; i++) {
            if (data[i] == 0) {
                return i;
            }
        }

        return data.length;
    }

}
