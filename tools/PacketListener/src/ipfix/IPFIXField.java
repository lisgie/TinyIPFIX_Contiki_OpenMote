package ipfix;



/**
 * IPFIXField. Representing an IPFIX Field.
 * @author André Freitag
 *
 */
public class IPFIXField {

	/** Field value */
	public byte[] value;
	/** nodeID of the origin of this field */
	public long nodeID;

	/** corresponding IPFIXTemplate.Field */
	public IPFIXTemplate.Field templateField;



	/**
	 * constructor.
	 * 
	 * @param value
	 * @param nodeID
	 * @param templateField
	 */
	public IPFIXField(byte[] value, long nodeID, IPFIXTemplate.Field templateField) {
		this.value = value;
		this.nodeID = nodeID;
		this.templateField = templateField;
	}
}
