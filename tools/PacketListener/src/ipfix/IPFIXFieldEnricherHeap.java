package ipfix;

import java.util.HashMap;

/**
 * IPFIXFieldEnricherHeap. Manages a bunch of IPFIEXFieldEnricher.
 * @author André Freitag
 *
 */
public class IPFIXFieldEnricherHeap {

	/**
	 * adds enricher
	 * 
	 * @param enricher
	 */
	public void add(IPFIXFieldEnricher enricher) {
		String qualifier = enricher.getQualifier();
		_enricher.put(qualifier, enricher);
	}

	/**
	 * gets enricher for specific fieldID/enterpriseNumber combination
	 * 
	 * @param fieldID
	 * @param enterpriseNumber
	 * @return
	 */
	public IPFIXFieldEnricher get(int fieldID, long enterpriseNumber) {
		IPFIXFieldEnricher enricher = null;

		enricher = _enricher.get( // try to find enricher exactly fitting the given fieldID
				IPFIXFieldEnricher.getQualifier(fieldID, enterpriseNumber)
				);
		if (enricher == null) {
			enricher = _enricher.get( // try to find enricher fitting given fieldID with blacked out reserved protocol bytes (first byte is used for IPFIX itself as well as aggregation).
					IPFIXFieldEnricher.getQualifier(fieldID&0x00FF, enterpriseNumber)
					);
		}



		return enricher;
	}


	/* private member */
	private HashMap<String,IPFIXFieldEnricher> _enricher=new HashMap<String,IPFIXFieldEnricher>();
}
