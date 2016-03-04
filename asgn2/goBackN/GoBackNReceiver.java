/**
 * Imports
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
/**
 * The go back n receiver. It has a buffer of 128 bytes.
 * It will acknowledge each packet by returning one byte with a value of the sequence number.
 * It follows the standard GBN transfer protocol as described in Computer Networking: A Top-Down Approach.
 * The receiver will acknowledge the packet sequence number processed.
 *
 * @author Dallas Fraser - 110242560
 * @author George Lifchits - 100691350
 * @version 1.0
 * @see Class#`UnreliableDatagramSocket`
 */

public class GoBackNReceiver {
	/**
	*
	* {@link socket}: the UPD socket
	* @see Class#UnreliableDatagramSocket
	* {@link logger}: the logger for the class
	* @see Class#Logger
	* {@link fs}: the file output stream when transfering binary files
	* {@link fw}: the file writer for text files
	* {@link out_packet}: the datagram packet for responding
	* {@link in_packet}: the datagram packet for receiving
	* {@link ia}: the internet address of the sender
	* @see Class#
	* {@link sequence}: the sequence number of packet (0 or 1)
	*/
	private UnreliableDatagramSocket socket;
	private Logger logger;
	private PrintWriter out;
	private FileOutputStream fs;
	private FileWriter fw;
	private DatagramPacket out_packet;
	private DatagramPacket in_packet;
	private InetAddress ia;
	private int sequence;
	private boolean binaryFile;

	private int DATA_BUF = 124;
	private int PACKET_SIZE = DATA_BUF+2;
	/**
	* The public constructor
	* @param hostAddress: a String of the host address
	* @param senderPort: the port number of the sender
	* @param receiverPort: the port number of this receiver
	* @param reliabilityNumber: the reliability number of the server @see Class#UnreliableDatagramSocket
	* @param fileName: the name of the file to output
	* @param logger: the logger of the class
	*
	* @throws IOException if unable to access file, create socket
	* @throws UnknownHostException if unable to find address for host
	* @throws SocketException if unable to create UDP socket
	*/
	public GoBackNReceiver(String hostAddress,
							int senderPort,
							int receiverPort,
							int reliabilityNumber,
							String fileName,
							Logger logger) throws IOException {
		this.socket = new UnreliableDatagramSocket(receiverPort, reliabilityNumber, logger);
		this.logger = logger;
		InetAddress ia = InetAddress.getByName(hostAddress);
		byte[] in_data = new byte[PACKET_SIZE];
		byte[] out_data = new byte[1];
		out_data[0] = (byte) 1;
		this.in_packet = new DatagramPacket(in_data, in_data.length, ia, senderPort);
		this.out_packet = new DatagramPacket(out_data, out_data.length,ia, senderPort);
		this.fw = new FileWriter(new File(fileName));
		this.fs = new FileOutputStream(new File(fileName));
		this.logger.debug("Created receiver");
		this.sequence = 0;
		this.binaryFile = true;
	}

	/**
	* a method that receives a packet.
	* It will call itself recursively until the packet is received
	* @throws IOException: this occurs when the socket is unable to receive a packet
	*/
	public int receivePacket() throws IOException {
		int result = -1;
		try {
			this.socket.receive(this.in_packet);
			byte[] data = this.in_packet.getData();
			this.logger.debug(data);
			this.logger.debug(this.sequence + " vs " + data[0]);
			if (data[0] == this.sequence && data[1] != 0) {
				this.logger.debug("Packet was right sequence");
				if(data[1] == DATA_BUF+1){
					// we are done
					this.logger.debug("Finish the file");
					this.saveFile();
				} else {
					// packet was recieved and should write to the file
					result = 0;
					this.writeFile(data);
				}
			} else {
				// just drop the packet
				result = 0;
				this.logger.debug("Packet was invalid sequence");
			}
		} catch(SocketTimeoutException e) {
			this.logger.debug("Timeout exception");
			result = this.receivePacket();
		}
		return result;
	}

	/**
	* this will continually receive packet until final packet is signaled
	* @throws IOException: occurs in various submethods
	*/
	public void receiveFile() throws IOException {
		while (this.receivePacket() >= 0) {
			this.logger.debug("receiving Packet");
		}
	}

	/**
	* write the data file using the appropriate output object
	* It will acknowledge the packet was processed
	* @param data: the byte data to output
	* @throws IOException: this occurs when unable to write to the file
	*/
	private void writeFile(byte[] data) throws IOException {
		if (this.binaryFile) {
			this.fs.write(data, 2, data[1]);
		} else {
			this.fw.write(new String(data,"UTF-8"), 2, data[1]);
		}
		this.acknowledge();
	}

	/**
	* save the file and closes it
	* It will acknowledge the file was closed
	* @throws IOException: this occurs when unable to close the file
	*/
	private void saveFile() throws IOException {
		if (this.binaryFile) {
			this.fs.close();
		} else {
			this.fw.close();
		}
		this.acknowledge();
		this.logger.debug("Finished closing file");
	}

	/**
	* acknowledges the packet sequence was processed
	* @throws IOException: occurs when unable to send ack
	*/
	private void acknowledge() throws IOException {
		this.logger.debug("Acknowledging the packet");
		byte[] data = new byte[1];
		data[0] = (byte) this.sequence;
		this.out_packet.setData(data);
		this.socket.send(this.out_packet);
		this.sequence = (this.sequence + 1) % 128; // update the sequence number
		this.logger.debug("Packet should be ack");
	}

	/**
	* There are 5 mandatory parameters  and one optional parameter
	* @param 0: a string of the host address
	* @param 1: the sender port number
	* @param 2: the receiver port number
	* @param 3: the reliability number of the receiver
	* @param 4: the file name
	* @param 5: the logging level
	*/
	public static void main(String[] args) {
		try {
			if (args.length < 5) {
				throw new Exception("Missing an argument: hostAddress senderPort receiverPort reliabilityNumber fileName");
			}
			String hostAddress = args[0];
			int senderPort = new Integer(args[1]).intValue();
			int receiverPort = new Integer(args[2]).intValue();
			int reliabilityNumber = new Integer(args[3]).intValue();
			String fileName = args[4];
			Logger logger = new Logger(0);
			GoBackNReceiver gb = new GoBackNReceiver(hostAddress,
														senderPort,
														receiverPort,
														reliabilityNumber,
														fileName,
														logger);
			logger.debug("Created");
			gb.receiveFile();
		} catch(Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
	}
}
