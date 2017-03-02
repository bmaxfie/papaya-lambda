package utils.papaya.com;

import java.util.Map;

public class Exception400 extends Exception 
{

	private Map<String, Object> response;
	
	public Exception400(String logMessage, Map<String, Object> response) {
		super(logMessage);
		this.response = response;
	}
	
	public Map<String, Object> getResponse() {	return this.response;	}
	
}
