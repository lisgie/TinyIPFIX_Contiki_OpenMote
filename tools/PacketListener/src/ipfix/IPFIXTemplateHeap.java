package ipfix;

import java.util.HashMap;

/**
 * IPFIXTemplateHeap. Maintains a set of IPFIXTemplates.
 * @author André Freitag
 *
 */
public class IPFIXTemplateHeap {

	
	/**
	 * adds template
	 * 
	 * @param template
	 */
	public void add(IPFIXTemplate template) {
		String qualifier = template.getQualifier();
		_templates.put(qualifier, template);
	}
	
	/**
	 * gets template for specific nodeID/templateID combination
	 * 
	 * @param templateID
	 * @return
	 */
	public IPFIXTemplate get(String qualifier) {
		if (_templates.containsKey(qualifier)) {
			return _templates.get(qualifier);
		} else {
			return null;
		}
	}
	
	
	
	/* private member */
	private HashMap<String,IPFIXTemplate> _templates=new HashMap<String,IPFIXTemplate>();
}
