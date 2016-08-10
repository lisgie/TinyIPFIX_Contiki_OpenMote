package ipfix;

import java.util.ArrayList;
import java.util.List;

/**
 * IPFIXTemplate. Describes structure of a data packet.
 * @author André Freitag
 *
 */
public class IPFIXTemplate {

	/** nodeID of the node this template belongs to */
	public long nodeID;
	/** template ID */
	public int templateID;
	/** sequence number of the latest packet that belonged to this template */
	public long lastSequenceNumber;
	/** template fields **/
	public List<Field> fields;


	/**
	 * constructor
	 * 
	 * @param nodeID of the node this template belongs to
	 * @param templateID of the template
	 * @param sequenceNumber of the packet that declared this template (used to initialize lastSequenceNumber)
	 */
	public IPFIXTemplate(long nodeID, int templateID, long sequenceNumber) {
		this.nodeID = nodeID;
		this.templateID = templateID;
		this.fields = new ArrayList<Field>();
		this.lastSequenceNumber = sequenceNumber;
	}

	/**
	 * add Field to template
	 * 
	 * @param field
	 */
	public void addField(IPFIXTemplate template, int fieldID, int length, Long enterpriseNumber) {
		this.fields.add(new Field(template, fieldID, length, enterpriseNumber));
	}
	/**
	 * add Field to template, enterpriseNumber=null (none given)
	 * 
	 * @param field
	 */
	public void addField(IPFIXTemplate template, int fieldID, int length) {
		addField(template, fieldID, length, null);
	}

	/**
	 * gets the size of a valid data packet fitting this template
	 * 
	 * @return
	 */
	public int getDataPacketLength() {
		int len=0;
		for (Field field: this.fields) {
			len += field.length;
		}

		return len;
	}

	/**
	 * get qualifier
	 * 
	 * @return distinct enricher qualifier
	 */
	public String getQualifier() {
		return IPFIXTemplate.getQualifier(this.nodeID, this.templateID);
	}
	/**
	 * get template qualifier for specific nodeID/templateID
	 * 
	 * @param nodeID
	 * @param templateID
	 * @return distinct enricher qualifier
	 */
	public static String getQualifier(long nodeID, int templateID) {
		return nodeID + "|" + templateID;
	}



	/**
	 * IPFIXTemplate.Field. Describes the structure of a template field.
	 * @author André Freitag
	 *
	 */
	public static class Field {

		/** fieldID */
		public int fieldID;
		/** field length */
		public int length;
		/** enterpriseNumber, null if none given */
		public Long enterpriseNumber=null;

		/** corresponding template */
		public IPFIXTemplate template;



		/**
		 * constructor
		 * 
		 * @param template
		 * @param fieldID
		 * @param length
		 * @param enterpriseNumber
		 */
		public Field(IPFIXTemplate template, int fieldID, int length, Long enterpriseNumber) {
			this.template = template;
			this.fieldID = fieldID;
			this.length = length;
			this.enterpriseNumber = enterpriseNumber;
		}

		/**
		 * get qualifier
		 * 
		 * @return distinct enricher qualifier
		 */
		public String getQualifier() {
			return Field.getQualifier(this.fieldID, this.enterpriseNumber);
		}

		/**
		 * get enricher qualifier for specific fieldID/enterpriseNumber
		 * 
		 * @param fieldID
		 * @param enterpriseNumber
		 * @return distinct enricher qualifier
		 */
		public static String getQualifier(int fieldID, long enterpriseNumber) {
			return enterpriseNumber + "|" + fieldID;
		}
	}
}
