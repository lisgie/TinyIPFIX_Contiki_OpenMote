package ipfix;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ipfix.IPFIXParser.ParseException;

public class IPFIXEnricher {
	/** @var only keep the last MAX_LOG_SIZE characters of the log */
	public static int MAX_LOG_SIZE=4096;


	/* constructors */
	/**
	 * constructor.
	 * 
	 * @param pathToMetdata path to XML-file containing metadata to parse fields.
	 * @throws Exception
	 */
	public IPFIXEnricher(String pathToMetadata) throws Exception {
		// parse metadata file
		_parseMetadataFile(pathToMetadata);
	}



	/* public methods */
	public IPFIXEnrichedField enrich(IPFIXField field) throws ParseException {
		// reset log
		_log = new StringBuffer();

		// enrich field
		IPFIXFieldEnricher enricher = _enricherHeap.get(field.templateField.fieldID, field.templateField.enterpriseNumber);
		IPFIXEnrichedField enrichedField;
		if (enricher != null) { // enrich field if an enricher is available ..
			try {
				enrichedField = enricher.enrich(field);
			} catch (Exception e) {
				throw new IPFIXParser.ParseException("IPFIXEnricher: couldn't enrich field.", e);
			}
		} else { // otherwise just convert it..
			enrichedField = new IPFIXEnrichedField(field);
		}

		_log("|----- %s[%d] (%d - %d): %s %s", enrichedField.name!=null?enrichedField.name:enrichedField.type, field.templateField.length, enrichedField.templateField.enterpriseNumber, enrichedField.templateField.fieldID, enrichedField.value.toString(), enrichedField.unit!=null?enrichedField.unit:"");

		return enrichedField;
	}
	/**
	 * returns log of the last enrich() call
	 */
	public String getLog() {
		return _log.toString();
	}



	/* protected methods */
	/**
	 * parses metadata xml file
	 * @param pathToMetadata
	 * @throws Exception
	 */
	protected void _parseMetadataFile(String pathToMetadata) throws Exception {

		try {
			// set up xml parse helper..
			DocumentBuilderFactory factory  = DocumentBuilderFactory.newInstance();
			DocumentBuilder        builder  = factory.newDocumentBuilder();
			Document               document = builder.parse(new File(pathToMetadata) );

			// iterate through <field>s
			NodeList fields = document.getElementsByTagName("field");
			for (int i=0; i<fields.getLength(); i++) {

				/* parse <field> entry and create enricher */
				Node n = fields.item(i);
				NodeList children = n.getChildNodes();

				IPFIXFieldEnricher enricher = new IPFIXFieldEnricher(); // create enricher

				for (int j=0; j<children.getLength(); j++) { // parse entry

					Node child = children.item(j);
					String key = child.getNodeName().toLowerCase();
					String val = child.getTextContent();

					// add enricher data
					if (key.equals("name")) {
						enricher.name = val;
					} else if (key.equals("type")) {
						enricher.type = val;
					} else if (key.equals("unit")) {
						enricher.unit = val;
					} else if (key.equals("fieldid")) {
						try {
							if (val.startsWith("0x")) { // treat as hex string
								enricher.fieldID = Integer.parseInt(val.substring(2), 16);
							} else { // treat as integer
								enricher.fieldID = Integer.parseInt(val);
							}
						} catch (Exception e) {

						}
					} else if (key.equals("enterprisenumber")) {
						try {
							if (val.startsWith("0x")) { // treat as hex string
								enricher.enterpriseNumber = Long.parseLong(val.substring(2), 16);
							} else { // treat as integer
								enricher.enterpriseNumber = Long.parseLong(val, 10);
							}
						} catch (Exception e) {

						}
					} else if (key.equals("expression")) {
						if (!val.trim().equals("")) { // only add non zero expressions
							enricher.expression = val;
						}
					} else if (key.equals("reference")) {

						Node type = child.getAttributes().getNamedItem("type");
						if (type != null) {
							enricher.addReference(val, type.getTextContent());
						}
					}
				}

				/* add enricher to heap */
				_enricherHeap.add(enricher);
			}
		} catch (Exception e) {
			throw new Exception("IPFIXParser: couldn't parse metadata file.", e);
		}
	}
	/**
	 * logs sprintf style.
	 * @param format
	 * @param args
	 * @return
	 */
	protected IPFIXEnricher _log(String format, Object... args) {
		_log.append(String.format(format, args));
		_log.append(System.getProperty( "line.separator" ));

		if (_log.length() > MAX_LOG_SIZE) {
			_log.delete(0, _log.length()-MAX_LOG_SIZE);
		}

		return this;
	}



	/* protected member */
	protected IPFIXFieldEnricherHeap _enricherHeap=new IPFIXFieldEnricherHeap();
	protected static StringBuffer _log=new StringBuffer();
}
