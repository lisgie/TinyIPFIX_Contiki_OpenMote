package ipfix;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * IPFIX packet parser.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc5101">IETF - RFC5101</a>
 * 
 * @author André Freitag, adapted by Michael Meister
 */
public class IPFIXParser {

	/** @var only keep the last MAX_LOG_SIZE characters of the log */
	public static int MAX_LOG_SIZE=4096;

	/**
	 * parses packet.
	 * stores parsed templates. returns parsed data fields.
	 * 
	 * @param packet
	 * @return Map<nodeID, List<Fields>>
	 */
	public Map<String,List<IPFIXField>> parse(byte[] packet) throws ParseException {

		try {

			// reset log
			_log = new StringBuffer();

			// wrap DataInputStream around packet
			DataInputStream stream = new DataInputStream(new ByteArrayInputStream(packet));


			/* parse packet */
			int version = stream.readUnsignedShort();
			int length = stream.readUnsignedShort();
			stream.skip(12);
			int setID = stream.readUnsignedShort();


			// check for matching version
			if (version != getSupportedVersion()) {
				throw new ParseException("IPFIXParser: packet has unsupported version, expected "+getSupportedVersion()+", got: "+version+".");
			}

			// check for right packet length
			if (packet.length < length) {
				throw new ParseException("IPFIXParser: packet too short.");
			}

			// extract real packet
			byte[] buff = new byte[length];
			System.arraycopy(packet, 0, buff, 0, length);

			// parse template / data
			if (setID == _getTemplateSetID()) {
				_parseTemplate(buff);
				return null;
			} else {
				return _parseData(buff);
			}

		} catch (Exception e) {
			Writer stackTrace = new StringWriter();
			e.printStackTrace(new PrintWriter(stackTrace));
			_log("EXCEPTION: %s\n%s\n", e.getMessage(), stackTrace.toString());

			throw new ParseException ("IPFIXParser: couldn't parse packet.", e);
		}
	}

	/**
	 * gets logged messages (for last parse() call)
	 * 
	 * @return log
	 */
	public String getLog() {
		return _log.toString();
	}

	/* IPFIX constants */
	/** get supported IPFIX version	 */
	protected int getSupportedVersion() {
		return 0xA;
	}
	/** get setID for templates  */
	protected int _getTemplateSetID() {
		return 2;
	}

	/* protected helper methods */
	/**
	 * parse template packet
	 * 
	 * @author Thomas Kothmayr, adapted by André Freitag
	 * @param packet
	 * @throws Exception
	 */
	protected void _parseTemplate(byte[] packet) throws ParseException {

		// wrap UnsignedDataInputStream around packet
		UnsignedDataInputStream stream = new UnsignedDataInputStream(new ByteArrayInputStream(packet));
		try {

			// abort if packet is too short
			if ((stream.available() < 24)) {
				throw new ParseException("IPFIXParser: template packet too short.");
			}


			// parse header
			stream.readUnsignedShort(); // version
			stream.readUnsignedShort(); // length
			stream.readUnsignedInt(); // export time
			long sequenceNumber = stream.readUnsignedInt(); // sequence number
			long nodeID = stream.readUnsignedInt(); // observation domain ID => nodeID


			// parse payload
			while (stream.available() > 0) {

				int setID = stream.readUnsignedShort(); // set ID
				if (setID != 2) {
					throw new ParseException(
							"IPFIXParser: template has wrong setID, expected 2, got " + setID + ".");
				}

				int length = stream.readUnsignedShort();

				int bytesread = 4;
				while (bytesread < length) {
					// parse template
					int templateID = stream.readUnsignedShort();
					bytesread += 2;
					int fieldcount = stream.readUnsignedShort();
					bytesread += 2;

					IPFIXTemplate template = new IPFIXTemplate(nodeID, templateID, sequenceNumber);

					_log("\n+--[%d] Template: %d, received %s \n|",
							nodeID, templateID, Calendar.getInstance().getTime().toString());

					// parse template fields
					for (int i = 0; i < fieldcount; i++) {
						int fieldID = stream.readUnsignedShort();
						bytesread += 2;
						int fieldLength = stream.readUnsignedShort();
						bytesread += 2;
						Long enterpriseNumber = null;
						if (fieldID > 0x8000) { // enterpriseNumber is only available if enterprise bit is set
							enterpriseNumber = stream.readUnsignedInt();
							bytesread += 4;
						}

						_log("|----- Field " + fieldID + ", enterpriseNumber: " + enterpriseNumber + ", length: " + fieldLength);
						template.addField(template, fieldID, fieldLength, enterpriseNumber);
					}

					_templateHeap.add(template);
				}
			}

			stream.close();
		} catch (Exception e) {
			throw new ParseException("IPFIXParser: couldn't parse template.", e);
		} finally {
			try {
				stream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	/**
	 * parse data packet
	 * 
	 * @author Thomas Kothmayr, adapted by André Freitag
	 * @param packet
	 * @return parsed fields, null if none could be parsed
	 * @throws Exception
	 */
	protected Map<String,List<IPFIXField>> _parseData(byte[] packet) throws ParseException {

		// wrap UnsignedDataInputStream around packet
		UnsignedDataInputStream stream = new UnsignedDataInputStream(new ByteArrayInputStream(packet));

		try {
			Map<String,List<IPFIXField>> ret = new HashMap<String,List<IPFIXField>>();


			// abort if packet is too short
			if ((stream.available() < 17)) {
				throw new ParseException("IPFIXParser: data packet too short.");
			}


			// parse header
			stream.readUnsignedShort(); // version, already checked by parse()
			stream.readUnsignedShort(); // length, already checked by parse()
			stream.readUnsignedInt(); // export time
			long sequenceNumber = stream.readUnsignedInt(); // sequence number
			long nodeID = stream.readUnsignedInt(); // observation domain ID => nodeID

			// parse payload
			while (stream.available() > 0) {
				int setID = stream.readUnsignedShort();
				int setLength = stream.readUnsignedShort();
				int fieldsLength = setLength - 4; // fieldsLength= setLength - number of bytes already read


				// data sets must have setID>255 -> abort if not the case
				if (setID < 256) {
					throw new Exception("IPFIXParser: data packet from node#"+nodeID+" has wrong setID, expected setID > 255, got: " + setID + ".");
				}

				// get corresponding template
				IPFIXTemplate template = _templateHeap.get(IPFIXTemplate.getQualifier(nodeID, setID));

				// no template available for this packet -> abort
				if (template == null) {
					String rawData="";
					stream.mark(stream.available());
					while (stream.available() > 0) {
						rawData += String.format("%02X ", stream.readUnsignedByte());
					}
					stream.reset();


					_log("\n|+--[%d] No template, raw data:\n|---- %s", nodeID, rawData);
					break;
				}

				// packet must have right size
				if (fieldsLength != template.getDataPacketLength()) {
					throw new Exception("IPFIXParser: data packet from node#"+nodeID+" does not fit its template, setLength doesn't match expected length.");
				} else if (fieldsLength > stream.available()) {
					throw new Exception("IPFIXParser: data packet from node#"+nodeID+" too short.");
				}

				// packet must have a sequence number greater than the one of the last processed packet
				// ATTENTION: possible replay attack: if lastSequenceNumber==0xffffffff any packet regardless of its sequence number is accepted (and since template.lastSequenceNumber is being set the attacker could lock out the actual node and make the system only accept its packets)
				if (sequenceNumber <= template.lastSequenceNumber && template.lastSequenceNumber<0xffffffff) {
					throw new Exception("IPFIXParser: received obsolete packet from node#"+nodeID+" => discarded it ("+sequenceNumber+"<="+template.lastSequenceNumber+".");
				}

				_log("\n|+--[%d] Data received %s \n|", nodeID, Calendar.getInstance().getTime().toString());

				// parse fields
				for (IPFIXTemplate.Field currentTemplateField: template.fields) {

					// read data
					int fieldlen = currentTemplateField.length;
					byte[] data = new byte[fieldlen];
					stream.read(data, 0, fieldlen);

					IPFIXField field = new IPFIXField(data, nodeID, currentTemplateField);

					if (!ret.containsKey(Long.toString(field.nodeID))) {
						ret.put(Long.toString(field.nodeID), new ArrayList<IPFIXField>());
					}
					ret.get(Long.toString(field.nodeID)).add(field);
				}
				//_log(System.getProperty("line.separator"));

				// if everything went ok -> update lastSequenceNumber of the template
				template.lastSequenceNumber = sequenceNumber;
			}

			return ret;
		} catch (Exception e) {
			throw new ParseException("IPFIXParser: couldn't parse template.", e);
		} finally {
			try {
				stream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	/**
	 * logs sprintf style.
	 * @param format
	 * @param args
	 * @return
	 */
	protected IPFIXParser _log(String format, Object... args) {
		_log.append(String.format(format, args));
		_log.append(System.getProperty( "line.separator" ));

		if (_log.length() > MAX_LOG_SIZE) {
			_log.delete(0, _log.length()-MAX_LOG_SIZE);
		}

		return this;
	}


	/* protected member */
	protected IPFIXTemplateHeap _templateHeap=new IPFIXTemplateHeap();
	protected static StringBuffer _log=new StringBuffer();


	/* helper classes */
	/**
	 * Exception that gets thrown if parsing of a packet went wrong.
	 * 
	 * @author André Freitag
	 */
	public static class ParseException extends Exception {
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
