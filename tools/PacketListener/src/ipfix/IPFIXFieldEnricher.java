package ipfix;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

/**
 * IPFIXFieldEnricher. Enriches a IPFIXField with metadata.
 * @author André Freitag
 *
 */
public class IPFIXFieldEnricher {

	/** fieldID this enricher is for*/
	public int fieldID;
	/** enterpriseNumber this enricher is for */
	public long enterpriseNumber;

	/** field name */
	public String name;
	/** field type */
	public String type;
	/** unit for field's value */
	public String unit;

	/** expression to convert field's value */
	public String expression = null;
	/** references that are used in the expression */
	public HashMap<String,String> expressionReferences = new HashMap<String,String>();



	/**
	 * constructor
	 */
	public IPFIXFieldEnricher() {
	}

	/**
	 * add expression reference
	 * 
	 * @param alias
	 * @param type
	 */
	public void addReference(String alias, String type) {
		this.expressionReferences.put(alias, type);
	}


	/**
	 * enriches IPFIXField with metadata
	 * 
	 * @param field
	 * @return enriched IPFIXEnrichedField
	 * @throws Exception
	 */
	public IPFIXEnrichedField enrich(IPFIXField field) throws Exception {
		IPFIXEnrichedField enrichedField = new IPFIXEnrichedField(field);

		// add metadata
		enrichedField.name = this.name;
		enrichedField.type = this.type;
		enrichedField.unit = this.unit;

		// calculate value
		if (this.expression != null) {
			// do expression
			UnsignedDataInputStream stream = new UnsignedDataInputStream(new ByteArrayInputStream(field.value));

			try {
				// convert value to unsigned int
				Long value=null;
				if (stream.available() == 4) {
					value = stream.readUnsignedInt();
				} else if (stream.available() == 2) {
					value = (long) stream.readUnsignedShort();
				} else if (stream.available() == 1) {
					value = (long) stream.readUnsignedByte();
				}


				// prepare ECMAScript engine
				ScriptEngineManager sem = new ScriptEngineManager();
				ScriptEngine e = sem.getEngineByName("ECMAScript");
				ScriptEngineFactory f = e.getFactory();

				// construct statements
				ArrayList<String> statements = new ArrayList<String>();

				for (Map.Entry<String,String> ref : this.expressionReferences.entrySet()) {
					statements.add("var "+ref.getKey()+" = 1;"); // TODO: replace 1 with last occurence of ref.getValue()
				}
				statements.add("var x="+value+";");
				statements.add(this.expression);

				// execute script
				String[] temp = new String[statements.size()];
				statements.toArray(temp);
				String program = f.getProgram(temp);
				enrichedField.value = e.eval(program);//Double.parseDouble(e.eval(program).toString());
				if (enrichedField.value!=null && enrichedField.value instanceof Double) {
					Double v = (Double)enrichedField.value;
					if ((new Integer(v.intValue())).doubleValue() == v.doubleValue()) {
						enrichedField.value = v.intValue();
					}
				}
			} catch (Exception e) {
				throw new Exception("IPFIXFieldEnricher: couldn't evaluate expression.", e);
			} finally {
				stream.close();
			}
		}

		return enrichedField;
	}


	/**
	 * get qualifier
	 * 
	 * @return distinct enricher qualifier
	 */
	public String getQualifier() {
		return IPFIXFieldEnricher.getQualifier(this.fieldID, this.enterpriseNumber);
	}

	/**
	 * get enricher qualifier for specific fieldID/enterpriseNumber
	 * 
	 * @param fieldID
	 * @param enterpriseNumber
	 * @return distinct enricher qualifier
	 */
	public static String getQualifier(int fieldID, long enterpriseNumber) {
		return IPFIXTemplate.Field.getQualifier(fieldID, enterpriseNumber);
	}
}
