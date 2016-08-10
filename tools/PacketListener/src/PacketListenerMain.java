import ipfix.TinyIPFIXListener;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Listener handling data received from an (tiny)IPFIX based WSN operating under
 * Contiki OS. Listens to the USB device using the tunslip program, parses the
 * input and enriches the data with the given metadata. It handles IPFIX
 * messages as well as tinyIPFIX ones.
 * 
 * @author Michael Meister
 */
public class PacketListenerMain {

	/**
	 * Main method of the packet listener for a Contiki WSN.
	 * 
	 * @param args
	 *            args[0] specifies the path to the USB device to listen to
	 *            args[1] specifies the output path where the file containing
	 *            the packet data is written to
	 */
	public static void main(final String[] args) {
		if (args.length < 1) {
			System.err.println("Usage: java PacketListenerMain usbdevice [outputpath]");
			return;
		}

		final String usbDevice = args[0];
		final String outputDir = args[1];
		
		File outputFile = null;
		
		// create output file
		if(outputDir != null) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
			Date date = new Date();
			
			String filename = "packet_dump_" + dateFormat.format(date) + ".txt";
			outputFile = new File(outputDir + File.separator + filename);
		}

		try {
			new TinyIPFIXListener("tinyIPFIX-conf.xml", usbDevice, outputFile);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
