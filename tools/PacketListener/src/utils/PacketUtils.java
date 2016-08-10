package utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class PacketUtils {

	// IP header = 40, UDP header = 8 bytes
	public static final int IP_UDP_HEADER_SIZE = 48;
	public static final int IP_ADDRESS_SIZE = 16;
	
	public static byte[] extractPayload(byte[] packet) throws IllegalArgumentException {
		
		if(packet.length <= IP_UDP_HEADER_SIZE) {
			throw new IllegalArgumentException();
		}
		
		byte[] payload = new byte[packet.length - IP_UDP_HEADER_SIZE];
		System.arraycopy(packet, IP_UDP_HEADER_SIZE, payload, 0, payload.length);
		
		return payload;
	}
	
	public static InetAddress extractSourceAddress(byte[] packet)
			throws UnknownHostException, IllegalArgumentException {
		
		if(packet.length < IP_UDP_HEADER_SIZE) {
			throw new IllegalArgumentException();
		}
		
		byte[] sourceAddress = new byte[16];
		System.arraycopy(packet, IP_ADDRESS_SIZE, sourceAddress, 0, IP_ADDRESS_SIZE);
		
		return InetAddress.getByAddress(sourceAddress);
	}
	
	public static InetAddress extractDestinationAddress(byte[] packet)
			throws UnknownHostException, IllegalArgumentException {
		
		if(packet.length < IP_UDP_HEADER_SIZE) {
			throw new IllegalArgumentException();
		}
		
		byte[] destinationAddress = new byte[16];
		System.arraycopy(packet, 0, destinationAddress, 0, IP_ADDRESS_SIZE);
		
		return InetAddress.getByAddress(destinationAddress);
	}
}
