package utils.papaya.com;

import static utils.papaya.com.ResponseGenerator.*;

import java.util.Map;

/**
 * Class holds generic functions to validate fields in input JSON
 * @author bmaxfie
 *
 */

public class Validate 
{
	
	static public Map<String, Object> field(Map<String, Object> json, String fieldname) throws Exception400
	{
		Map<String, Object> path;
		if(!json.containsKey(fieldname)
				|| !(json.get(fieldname) instanceof Map)
				|| !((path = (Map<String, Object>) json.get(fieldname)) != null)) {
			throw new Exception400("Could not access params field of AWS transformed JSON.", 
					generate400("params field, when looking for the class_id is not in AWS transformed JSON.", "class_id"));
		}
		else 
			return path;
	}

	static public String username(Map<String, Object> json) throws Exception400
	{
		String username;
		if ((!json.containsKey("username") 
				|| !(json.get("username") instanceof String)
				|| !((username = (String) json.get("username")) != null))) {
			
			throw new Exception400("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.",
					generate400("username does not exist.", "username"));
		}
		
		if (username.length() > 45)
			username = username.substring(0, 45);
		return username;
	}
	
	static public AuthServiceType service(Map<String, Object> json) throws Exception400 {
		String service;
		AuthServiceType service_type;
		
		if (!json.containsKey("service") 
				|| !(json.get("service") instanceof String)
				|| !((service = (String) json.get("service")) != null)) {
			
			throw new Exception400("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.",
					generate400("service does not exist.", "service"));
		}
		
		if (service.contentEquals(Authentication.SERVICE_FACEBOOK)) {
			service_type = AuthServiceType.FACEBOOK;
		} else if (service.contentEquals(Authentication.SERVICE_GOOGLE)) {
			service_type = AuthServiceType.GOOGLE;
		} else {
			
			throw new Exception400("ERROR: 400 Bad Request - Returned to client. Service was of an unrecognizable type '" + service + "'.",
					generate400("service was of an unrecognizable type '" + service + "'.", service));
		}
		
		return service_type;
	}
	
	static public String authentication_key(Map<String, Object> json, AuthServiceType service_type) throws Exception400 {
		String authentication_key;
		if ((!json.containsKey("authentication_key") 
						|| !(json.get("authentication_key") instanceof String)
						|| !((authentication_key = (String) json.get("authentication_key")) != null))) {
			
			throw new Exception400("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.",
					generate400("authentication_key does not exist.", "authentication_key"));
		}
		
		if (service_type == AuthServiceType.FACEBOOK 
						&& (authentication_key.length() > Authentication.FACEBOOK_KEY_MAX_LEN
								|| authentication_key.length() < Authentication.FACEBOOK_KEY_MIN_LEN)
				|| service_type == AuthServiceType.GOOGLE
						&& (authentication_key.length() > Authentication.GOOGLE_KEY_MAX_LEN
								|| authentication_key.length() < Authentication.GOOGLE_KEY_MIN_LEN)) {
			
			throw new Exception400("ERROR: 400 Bad Request - Returned to client. authentication_key was not of valid length, instead it was '" + authentication_key.length() + "'.", 
					generate400("authentication_key was not of valid length, instead it was '" + authentication_key.length() + "'.", "authentication_key"));
		}
		
		return authentication_key;
	}
	
	static public long phone(Map<String, Object> json) throws Exception400 {
		long phone = 0;
		if (json.containsKey("phone")) {
			if (json.get("phone") instanceof Long)
				phone = ((Long) json.get("phone")).longValue();
			else if (json.get("phone") instanceof Integer)
				phone = ((Integer) json.get("phone")).longValue();
			else
				throw new Exception400("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.",
						generate400("phone does not exist.", "phone"));
			
			if (// phone is not 7 digits
					!(phone > 999999l
						&& phone < 10000000l)
				&&
				// phone is not 10 digits
					!(phone > 999999999l
						&& phone < 10000000000l)) {
				
				throw new Exception400("ERROR: 400 Bad Request - Returned to client. phone was not formatted right (i.e. neither 7 or 10 digits long).",
						generate400("phone was not formatted right (i.e. neither 7 or 10 digits long): " + phone + ".", "phone"));
			}
		} else {
			
			throw new Exception400("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.",
					generate400("phone does not exist.", "phone"));
				
		}
		
		return phone;
	}
	
	public static String email(Map<String, Object> json) throws Exception400{
		String email;

		if (!json.containsKey("email")
				|| !(json.get("email") instanceof String)
				|| !((email = (String) json.get("email")) != null)) {
		
			throw new Exception400("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.",
					generate400("email does not exist.", "email"));
		}
		
		if (email.length() > 45
				// Regex is supposed to loosely match general email form.
				|| !email.matches(".{3,}@.{3,}\\..{2,}")) {
			
			throw new Exception400("ERROR: 400 Bad Request - Returned to client. email was not formatted right (i.e. length or no @ or no domain) '" + email + "'.", 
					generate400("email was not formatted right (i.e. length or no @ or no domain) '" + email + "'.", "email"));
		}
		
		return email;
	}
	
	public static String class_id(Map<String, Object> json) throws Exception400 {
		String class_id;
		
		if (!json.containsKey("class-id")
				|| !(json.get("class-id") instanceof String)
				|| !((class_id = (String) json.get("class-id")) != null)) {
			
			throw new Exception400("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.",
					generate400("class-id does not exist.", "class-id"));
		}
		
		if (class_id.length() > 45)
			class_id = class_id.substring(0, 45);
		
		return class_id;
	}
	
	public static String user_id(Map<String, Object> json) throws Exception400 {
		String user_id;
		
		if (!json.containsKey("user_id")
				|| !(json.get("user_id") instanceof String)
				|| !((user_id = (String) json.get("user_id")) != null)) {
		
		throw new Exception400("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.",
				generate400("user_id does not exist.", "user_id"));
		} else if (user_id.length() > 45) {
			user_id = user_id.substring(0, 45);
		}
		
		return user_id;
	}
	
	public static int duration(Map<String, Object> json) throws Exception400 {
		Integer duration;
		
		if (!json.containsKey("duration")
				|| !(json.get("duration") instanceof Integer)
				|| !((duration = (Integer) json.get("duration")) != null)) {
			
			throw new Exception400("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.",
				generate400("duration does not exist.", "duration"));
		} else if (duration > 1440)
			throw new Exception400("ERROR: 400 Bad Request - Returned to client. duration is too long (longer than 24 hours).",
					generate400("duration is too long (> 1440 mins).", "duration"));
		
		return duration.intValue();
	}
	
	public static double location(Map<String, Object> json, String fieldname) throws Exception400 {
		Double location;
		
		if (!json.containsKey(fieldname)
				|| !(json.get(fieldname) instanceof Double)
				|| !((location = (Double) json.get(fieldname)) != null)) {
			
			throw new Exception400("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.",
					generate400(fieldname+" does not exist.", fieldname));
		}
		
		return location.doubleValue();
	}
	
	public static String description(Map<String, Object> json, String fieldname) throws Exception400 {
		String description;
		
		if (!json.containsKey(fieldname)
				|| !(json.get(fieldname) instanceof Double)
				|| !((description = (String) json.get(fieldname)) != null)) {
			
			throw new Exception400("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.",
					generate400(fieldname+" does not exist.", fieldname));
		}
		else if (description.length() > 255)
			throw new Exception400("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.",
					generate400(fieldname+" is too long (> 255 characters).", fieldname));
		
		return description;
	}
	
	public static String sponsor(Map<String, Object> json) throws Exception400 {
		String sponsor;
		
		if (!json.containsKey("sponsor")
				|| !(json.get("sponsor") instanceof String)
				|| !((sponsor = (String) json.get("sponsor")) != null)) {
		
		throw new Exception400("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.",
				generate400("sponsor does not exist.", "sponsor"));
		} else if (sponsor.length() > 45) {
			sponsor = sponsor.substring(0, 45);
		}
		
		return sponsor;
	}
	
	public static String access_key(Map<String, Object> json) throws Exception400 {
		String access_key;
		
		if (!json.containsKey("access_key")
				|| !(json.get("access_key") instanceof String)
				|| !((access_key = (String) json.get("access_key")) != null)) {
		
			throw new Exception400("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.",
					generate400("access_key does not exist.", "access_key"));
		} else if (access_key.length() > 45) {
			access_key = access_key.substring(0, 45);
		}
		
		return access_key;
	}
	
	public static Integer auth_option(Map<String, Object> json) throws Exception400 {
		Integer auth_option;
		
		if (!json.containsKey("auth_option")
				|| !(json.get("auth_option") instanceof Integer)
				|| !((auth_option = (Integer) json.get("auth_option")) != null)) {
			
			throw new Exception400("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.",
					generate400("auth_option does not exist.", "auth_option"));
		}
		
		return auth_option.intValue();
	}
	
}
