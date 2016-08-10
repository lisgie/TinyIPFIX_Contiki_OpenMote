package ipfix;


/**
 * IPFIXEnrichedField. Representing a IPFIX field enriched with metadata.
 * @author André Freitag
 *
 */
public class IPFIXEnrichedField extends IPFIXField {

	/** Field name */
	public String name;
	/** Field type */
	public String type;
	/** Field unit */
	public String unit;
	/** datatype */
	public String dataType;
	/** Field value */
	public Object value;


	/**
	 * constructor
	 * 
	 * @param field
	 */
	public IPFIXEnrichedField(IPFIXField field) {
		super(field.value, field.nodeID, field.templateField);
		this.value = super.value; // since member(variables) can't be overwritten, explicitly set this.value=super.value
	}
}