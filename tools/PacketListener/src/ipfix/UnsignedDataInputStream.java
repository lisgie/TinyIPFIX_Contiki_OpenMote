package ipfix;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * UnsignedDataInputStream for IPFIX Data-Stream.
 * @author Thomas Kothmayr
 *
 */
public class UnsignedDataInputStream extends DataInputStream {
	/**
	 * constructor
	 * 
	 * @param in
	 */
	public UnsignedDataInputStream(InputStream in) {
		super(in);
	}
	
	/**
	 * gets unsigned int from stream
	 * 
	 * @return long since an unsigned int may not fit into a signed int
	 * @throws IOException
	 */
	public long readUnsignedInt() throws IOException{
		long returner = super.readInt();
		if(returner < 0) {
			//correct the offset caused by two's complement
			returner = 0xFFFFFFFFL + returner + 1;
		}
		return returner;
	}

}
