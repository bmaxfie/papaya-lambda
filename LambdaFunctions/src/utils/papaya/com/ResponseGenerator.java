package utils.papaya.com;

import java.util.HashMap;
import java.util.Map;

public class ResponseGenerator {
    
    public static Map<String, Object> generate400(String message, String fields) {
    	Map<String, Object> response = new HashMap<String, Object>();
		response.put("code", 400);
		response.put("code_description", "Bad Request");
		response.put("error_description", message);
		response.put("fields", fields);
		return response;
    }
    
    public static Map<String, Object> generate404(String message) {
    	Map<String, Object> response = new HashMap<String, Object>();
    	response.put("code", 404);
    	response.put("code_description", "Not Found");
    	response.put("error_description", message);
    	return response;
    }
    
    public static Map<String, Object> generate500(String message) {
    	Map<String, Object> response = new HashMap<String, Object>();
    	response.put("code", 500);
    	response.put("code_description", "Internal Server Error");
    	response.put("error_description", message);
    	return response;
    }
}
