package ipfix;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import utils.Converter;
import utils.PacketUtils;

/**
 * Listens to the USB device using the tunslip program, parses the input and
 * enriches the data with the given metadata. It handles IPFIX messages as well
 * as tinyIPFIX ones.
 * 
 * @author Michael Meister
 */
public class TinyIPFIXListener implements Runnable {

	private TinyIPFIXParser parser;
	private IPFIXEnricher enricher;

	private BufferedReader stdInput;
	private Process tunslip6Process;
	private static final String TUNSLIP6 = "./tunslip6";
	private String tunslip6Args;
	private String usbDevice;
	private FileWriter fileWriter;

	/**
	 * Listens to the USB device using the tunslip program, parses the input and
	 * enriches the data with the given metadata.
	 * 
	 * @param pathToMetadata
	 *            specifies path to the metadata file used to enrich incoming
	 *            packets
	 * @param usbDevice
	 *            specifies the path to the USB device to listen to
	 * @param outputFile
	 *            specifies the file where the parsed and enriched packet data
	 *            is stored. If NULL no packets are stored.
	 * @throws Exception
	 */
	public TinyIPFIXListener(String pathToMetadata, String usbDevice, File outputFile)
			throws Exception {
		this.usbDevice = usbDevice;
		if (outputFile != null) {
			this.fileWriter = new FileWriter(outputFile, true);
		}
		this.tunslip6Args = "-s" + " " + usbDevice + " " + "aaaa::1/64 -v5";

		File usbDeviceFile = new File(usbDevice);
		if (!usbDeviceFile.exists()) {
			throw new IOException("Invalid packet USB port. Device file '" + usbDevice
					+ "' does not exist.");
		}

		// create parser an enricher instance
		parser = new TinyIPFIXParser();
		enricher = new IPFIXEnricher(pathToMetadata);

		new Thread(this).start();
	}

	/**
	 * Listens to the default USB device using the tunslip program, parses the
	 * input and enriches the data with the given metadata.
	 * 
	 * @param pathToMetadata
	 *            specifies path to the metadata file used to enrich incoming
	 *            packets
	 * @param usbDevice
	 *            specifies the path to the USB device to listen to
	 * @throws Exception
	 */
	public TinyIPFIXListener(String pathToMetadata, String usbDevice) throws Exception {
		this(pathToMetadata, usbDevice, null);
	}

	/**
	 * Listens to the default USB device using the tunslip program, parses the
	 * input and enriches the data with the given metadata.
	 * 
	 * @param pathToMetadata
	 *            specifies path to the metadata file used to enrich incoming
	 *            packets
	 * @throws Exception
	 */
	public TinyIPFIXListener(String pathToMetadata) throws Exception {
		this(pathToMetadata, "/dev/ttyUSB0");
	}

	/**
	 * Get log messages of the parser.
	 */
	public String getLog() {
		return parser.getLog();
	}

	/**
	 * Worker thread
	 */
	@Override
	public void run() {

		System.out.println("Listening on USB device: " + usbDevice);

		//String[] fullCommand = { "/bin/bash", "-c",
			//	"echo \"contiki\" | sudo -S" + " " + TUNSLIP6 + " " + tunslip6Args };
		
		String fullCommand = "sudo /home/livio/workspace/contiki-2.7/tools/tunslip6 -s /dev/ttyUSB0 aaaa:1/64 -v5";

		try {
			tunslip6Process = Runtime.getRuntime().exec(fullCommand);
			stdInput = new BufferedReader(new InputStreamReader(tunslip6Process.getInputStream()));
		} catch (Exception e) {
			System.err.println("Exception when executing '" + fullCommand + "'.");
			System.err.println("Exiting packet listener...");
			e.printStackTrace();
			System.exit(1);
		}

		try {
			while (!Thread.interrupted()) {

				// read packet from input stream
				DataPacket p;
				try {
					p = readPacket();
				} catch (Exception e) {
					return;
				}

				// parse and enrich incoming packets
				if (p != null && p.data.length != 0) {
					try {
						// parse packet
						Map<String, List<IPFIXField>> fields = parser.parse(p.data, p.address);

						// print parsed packet to console and write it
						// to output file
						System.out.print(parser.getLog());
						if (fileWriter != null) {
							fileWriter.write(parser.getLog());
							fileWriter.flush();
						}

						// enrich received fields
						if (fields != null && fields.size() > 0) {
							List<List<IPFIXField>> fieldList = new ArrayList<List<IPFIXField>>(
									fields.values());
							for (IPFIXField field : fieldList.get(0)) {
								enricher.enrich(field);

								// print enriched field to console and write it
								// to output file
								System.out.print(enricher.getLog());
								if (fileWriter != null) {
									fileWriter.write(enricher.getLog());
									fileWriter.flush();
								}
							}
						}
					} catch (TinyIPFIXParser.ParseException e) {
						System.err.println("COULDN'T PARSE IPFIX PACKET:");
						e.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
					}

				} else {
					// no payload received
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// clean up
			try {
				stdInput.close();
				tunslip6Process.destroy();
				fileWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Read packets from input stream of tunslip.
	 * 
	 * @return datapacket packet containing the payload and source address of
	 *         the original packet.
	 * @throws IOException
	 */
	private DataPacket readPacket() throws IOException {
		String line;
		byte[] packet;

		while (true) {
			line = stdInput.readLine();

			if (line != null && line.endsWith("write TUN")) {
				line = stdInput.readLine();
				line = line.substring(5);
				packet = Converter.hexStringToByteArray(line.replaceAll(" ", ""));
				try {
					return new DataPacket(PacketUtils.extractPayload(packet),
							PacketUtils.extractSourceAddress(packet));
				} catch (Exception e) {
					throw new IOException(e);
				}
			}
		}
	}

	/**
	 * Class representing the result of readPacket() method.
	 */
	protected class DataPacket {
		InetAddress address;
		byte[] data;

		public DataPacket(byte[] data, InetAddress address) {
			this.data = data;
			this.address = address;
		}
	}
}
