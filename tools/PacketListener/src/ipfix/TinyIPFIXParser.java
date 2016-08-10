package ipfix;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;


/**
 * Extension to IPFIXParser to enable it for the TinyIPFIX protocol.
 * TinyIPFIX compresses the header by omitting several unnecessary fields. Also omits nodeID since it may be generated out of the IPv6 IP-address of the source.
 * 
 * @author André Freitag, adapted by Michael Meister
 *
 */
public class TinyIPFIXParser extends IPFIXParser {


	/**
	 * Parses an IPFIX or tinyIPFIX packet. Generates nodeID out of source for tinyIPFIX packets (since it isn't transmitted in tinyIPFIX).
	 * 
	 * @param packet
	 * @param source
	 * @throws Exception
	 */
	public Map<String,List<IPFIXField>> parse(byte[] packet, InetAddress source) throws ParseException {

		try {
			// try to convert packet into IPFIX format
			packet = translatePacket(packet, source);

			// parse (possibly converted..) packet
			return super.parse(packet);
		} catch (IPFIXParser.ParseException e) {
			throw new ParseException("TinyIPFIXParser: couldn't parse packet.", e);
		}
	}
	/**
	 * Parses an IPFIX or tinyIPFIX packet. Uses localhost as source (will always generate the same nodeID for tinyIPFIX packets -> use parse(byte[], InetAddress).
	 * 
	 * @param packet
	 * @throws UnknownHostException
	 * @throws Exception
	 */
	@Override
	public Map<String,List<IPFIXField>> parse(byte[] packet) throws ParseException {
		try {
			return parse(packet, InetAddress.getLocalHost());
		} catch (UnknownHostException e) {
			throw new ParseException("TinyIPFIXParser: Couldn't get InetAddress of localhost => use TinyIPFIXParser.parse(packet,InetAddress).");
		}
	}

	/**
	 * Takes an IPFIX or TinyIPFIX packet and returns it in IPFIX notation.
	 * 
	 * @param packet
	 * @param source
	 * @return
	 * @throws ParseException
	 */
	public byte[] translatePacket(byte[] packet, InetAddress source) throws ParseException {
		try {
			// convert packet to IPFIX if it is TinyIPFIX
			if (isTinyIPFIX(packet)) {
				packet = translateHeader(packet, source);
			}

			// parse (possibly converted..) packet
			return packet;
		} catch (IPFIXParser.ParseException e) {
			throw new ParseException("TinyIPFIXParser: couldn't parse packet.", e);
		}
	}


	/**
	 * This TinyIPFIX implementation only supports IPFIX version 0xA
	 */
	@Override
	protected int getSupportedVersion() {
		return 0xA;
	}

	/**
	 * determine whether packet may be TinyIPFIX packet.
	 * 
	 * @param packet
	 */
	protected boolean isTinyIPFIX(byte[] packet) {
		try {

			// get IPFIX version
			int version = new DataInputStream(new ByteArrayInputStream(packet)).readUnsignedShort();

			// TinyIPFIX compresses the normal IPFIX header.
			// if the IPFIX version field doesn't match 0xA, packet is probably TinyIPFIX.
			// if packet.length==10 the version field only accidently matches the right version.
			if (version != getSupportedVersion() || packet.length == 10) {
				return true;
			}

		} catch (IOException e) {}

		return false;
	}

	/**
	 * Convert TinyIPFIX to IPFIX packet. Reconstructs original header.
	 * Uses `source` for generating a nodeID.
	 * 
	 * @author Thomas Kothmayr, adapted by André Freitag
	 * @param packet
	 */
	protected byte[] translateHeader(byte[] packet, InetAddress source) throws ParseException {

		try {
			byte[] ipfixPacket = null;

			// wrap DataInputStream around packet
			DataInputStream stream = new DataInputStream(new ByteArrayInputStream(packet));


			// unsigned integers are needed for the bit-shifting that follows.
			int byte1 = stream.readUnsignedByte();
			int byte2 = stream.readUnsignedByte();



			// construct the length
			int len = (byte1 & 0x03) + byte2;
			// construct the flags
			int flags = byte1 >> 6;
			// construct the set lookup field
			int set_lookup = (byte1 & 0x3F) >> 2;

			if (packet.length != len) {
				throw new ParseException("TinyIPFIXParser: invalid header. invalid packet length.");
			}

			// calculate payload length from flags
			if (flags == 0) {
				len -= 3;
			} else if (flags == 3) {
				len -= 5;
			} else {
				len -= 4;
			}
			// array that will hold the package with a full IPFIX Header
			ipfixPacket = new byte[20 + len];

			// write the version number
			ipfixPacket[1] = 0x0a;

			// write the length
			ipfixPacket[2] = (byte) (ipfixPacket.length >> 8);
			ipfixPacket[3] = (byte) (ipfixPacket.length & 0xff);

			// write system time, for lack of better values use the current time
			long now = System.currentTimeMillis();
			ipfixPacket[4] = (byte) ((now & 0xff000000l) >> 24);
			ipfixPacket[5] = (byte) ((now & 0x00ff0000l) >> 16);
			ipfixPacket[6] = (byte) ((now & 0x0000ff00l) >> 8);
			ipfixPacket[7] = (byte) ((now & 0x000000ffl) >> 0);

			// write sequence id, handle extended sequence field
			if ((flags == 1) || (flags == 3)) {
				ipfixPacket[8] = (byte) 0xff;
				ipfixPacket[9] = (byte) 0xff;
				ipfixPacket[10] = packet[2];
				ipfixPacket[11] = packet[3];
			} else {
				ipfixPacket[8] = (byte) 0xff;
				ipfixPacket[9] = (byte) 0xff;
				ipfixPacket[10] = (byte) 0xff;
				ipfixPacket[11] = packet[2];
			}

			// get the node ID from the IPv6 address that was given and write it to
			// the observation domain id field
			String obsDomain = "0x"	+ source.getHostAddress()
					.replaceAll("^([a-fA-F0-9]*:){3}", "")
					.replaceAll("(:[a-fA-F0-9]*){4}$", "");
			int obsdomain = Integer.decode(obsDomain);
			ipfixPacket[14] = (byte) ((obsdomain & 0xff00) >> 8);
			ipfixPacket[15] = (byte) ((obsdomain & 0x00ff) >> 0);

			// write the set id
			if ((flags == 2) || (flags == 3)) {
				// extended field is present, check if it is used
				if (set_lookup == 0x0F) {
					if (flags == 2) {
						ipfixPacket[17] = packet[3];
					} else {
						ipfixPacket[17] = packet[4];
					}
				} else if (set_lookup == 0) {
					if (flags == 2) {
						ipfixPacket[16] = packet[3];
					} else {
						ipfixPacket[16] = packet[4];
					}
				} else if (set_lookup == 1) {
					ipfixPacket[17] = 2;
				} else if (set_lookup == 2) {
					ipfixPacket[16] = 1;
				}
			} else {
				if (set_lookup == 1) {
					ipfixPacket[17] = 2;
				} else if (set_lookup == 2) {
					ipfixPacket[16] = 1;
				}
			}

			// write the set length
			ipfixPacket[18] = (byte) (((ipfixPacket.length - 16) & 0xff00) >> 8);
			ipfixPacket[19] = (byte) ((ipfixPacket.length - 16) & 0xff);

			// copy payload
			System.arraycopy(packet, packet.length - len, ipfixPacket, 20, len);

			return ipfixPacket;
		} catch (Exception e) {
			throw new ParseException("TinyIPFIXParser: couldn't translate packet header.", e);
		}
	}



	/* helper classes */
	/**
	 * Exception that gets thrown if parsing of a packet went wrong.
	 * 
	 * @author André Freitag
	 */
	public class ParseException extends IPFIXParser.ParseException {
		/**
		 * constructor
		 * 
		 * @see Exception#Exception(String)
		 * @param msg
		 */
		public ParseException(String msg) {
			super(msg);
		}
		/**
		 * constructor
		 * 
		 * @see Exception#Exception(Throwable)
		 * @param cause
		 */
		public ParseException(Throwable cause) {
			super(cause);
		}
		/**
		 * constructor
		 * 
		 * @see Exception#Exception(String, Throwable)
		 * @param msg
		 * @param cause
		 */
		public ParseException(String msg, Throwable cause) {
			super(msg,cause);
		}


		/* private member */
		private static final long serialVersionUID = 1L;
	}
}
