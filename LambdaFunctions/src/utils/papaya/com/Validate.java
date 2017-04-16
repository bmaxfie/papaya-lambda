package utils.papaya.com;

import static utils.papaya.com.ResponseGenerator.*;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TimeZone;

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
					generate400(fieldname + " field, when looking for a field in the path is not in AWS transformed JSON.", fieldname));
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
		} else if (username.length() > 45)
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
						&& authentication_key.length() < Authentication.FACEBOOK_KEY_MIN_LEN
				|| service_type == AuthServiceType.GOOGLE
						&& authentication_key.length() < Authentication.GOOGLE_KEY_MIN_LEN) {
			
			throw new Exception400("ERROR: 400 Bad Request - Returned to client. authentication_key was not of valid length, instead it was '" + authentication_key.length() + "'.", 
					generate400("authentication_key was not of valid length, instead it was '" + authentication_key.length() + "'.", "authentication_key"));
		}
		
		authentication_key = authentication_key.replaceAll("%2F", "/");
		authentication_key = authentication_key.replaceAll("%2B", "+");
		
		return authentication_key;
	}
	
	static public String service_user_id(Map<String, Object> json, AuthServiceType service_type) throws Exception400 {
		String service_user_id;
		if ((!json.containsKey("service_user_id") 
						|| !(json.get("service_user_id") instanceof String)
						|| !((service_user_id = (String) json.get("service_user_id")) != null))) {
			
			throw new Exception400("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.",
					generate400("service_user_id does not exist.", "service_user_id"));
		}
		
		if (service_type == AuthServiceType.FACEBOOK 
						&& service_user_id.length() < Authentication.FACEBOOK_KEY_MIN_LEN
				|| service_type == AuthServiceType.GOOGLE
						&& service_user_id.length() < Authentication.GOOGLE_KEY_MIN_LEN) {
			
			throw new Exception400("ERROR: 400 Bad Request - Returned to client. service_user_id was not of valid length, instead it was '" + service_user_id.length() + "'.", 
					generate400("service_user_id was not of valid length, instead it was '" + service_user_id.length() + "'.", "service_user_id"));
		}

		service_user_id = service_user_id.replaceAll("%2F", "/");
		service_user_id = service_user_id.replaceAll("%2B", "+");
		
		return service_user_id;
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
		} else if (class_id.length() > 45)
			class_id = class_id.substring(0, 45);
		class_id = class_id.replaceAll("%2F", "/");
		class_id = class_id.replaceAll("%2B", "+");
		
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

		user_id = user_id.replaceAll("%2F", "/");
		user_id = user_id.replaceAll("%2B", "+");
		
		return user_id;
	}
	
	//used in validating the user_id of receiving friend
	public static String user_id2(Map<String, Object> json) throws Exception400 {
		String user_id2;
		
		if (!json.containsKey("user_id2")
				|| !(json.get("user_id2") instanceof String)
				|| !((user_id2 = (String) json.get("user_id2")) != null)) {
		
		throw new Exception400("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.",
				generate400("user_id2 does not exist.", "user_id2"));
		} else if (user_id2.length() > 45) {
			user_id2 = user_id2.substring(0, 45);
		}
		
		return user_id2;
	}
	
	public static String session_id(Map<String, Object> json) throws Exception400 {
		String session_id;
		
		if (!json.containsKey("session-id")
				|| !(json.get("session-id") instanceof String)
				|| !((session_id = (String) json.get("session-id")) != null)) {
		
		throw new Exception400("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.",
				generate400("session-id does not exist.", "session-id"));
		} else if (session_id.length() > 45) {
			session_id = session_id.substring(0, 45);
		}
		session_id = session_id.replaceAll("%2F", "/");
		session_id = session_id.replaceAll("%2B", "+");
		
		return session_id;
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
				|| !(json.get(fieldname) instanceof String)
				|| !((description = (String) json.get(fieldname)) != null)) {
			
			throw new Exception400("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.",
					generate400(fieldname+" does not exist.", fieldname));
		}
		else if (description.length() > 255)
			throw new Exception400("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.",
					generate400(fieldname+" is too long (> 255 characters).", fieldname));
		
		return description;
	}
	
	public static String message(Map<String, Object> json, String fieldname) throws Exception400 {
		String message;
		
		if (!json.containsKey(fieldname)
				|| !(json.get(fieldname) instanceof String)
				|| !((message = (String) json.get(fieldname)) != null)) {
			
			throw new Exception400("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.",
					generate400(fieldname+" does not exist.", fieldname));
		}
		else if (message.length() > 255)
			throw new Exception400("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.",
					generate400(fieldname+" is too long (> 255 characters).", fieldname));
		
		return message;
	}
	
	public static int visibility(Map<String, Object> json) throws Exception400 {
		String visible;
		
		if (!json.containsKey("visibility")
				|| !(json.get("visibility") instanceof String)
				|| !((visible = (String) json.get("visibility")) != null)) {
			
			throw new Exception400("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.",
					generate400("visibility does not exist.", "visibility"));
		}
		int visibility = -1;
		try {
			visibility = Integer.parseInt(visible);
		} catch(Exception e) {
			throw new Exception400("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.",
					generate400("visibility is not a valid integer.", "visibility"));
		}

		return visibility;
	}
	
	public static Boolean sponsored(Map<String, Object> json) throws Exception400 {
		Boolean sponsored;
		
		if (!json.containsKey("sponsored")
				|| !(json.get("sponsored") instanceof Boolean)
				|| !((sponsored = (Boolean) json.get("sponsored")) != null)) {
		
		throw new Exception400("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.",
				generate400("sponsor does not exist.", "sponsor"));
		} //else if (sponsor.length() > 45) {
		//	sponsor = sponsor.substring(0, 45);
		//}
		
		return sponsored;
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
	
	public static String classname(Map<String, Object> json) throws Exception400 {
		String classname;
		
		if ((!json.containsKey("classname") 
				|| !(json.get("classname") instanceof String)
				|| !((classname = (String) json.get("classname")) != null))) {
			
			throw new Exception400("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.",
					generate400("classname does not exist.", "classname"));
		} else if (classname.length() > 45)
			classname = classname.substring(0, 45);
		return classname;
	}
	
	public static String start_time(Map<String, Object> json) throws Exception400 {
		String start_time;
		
		if ((!json.containsKey("start_time") 
				|| !(json.get("start_time") instanceof String)
				|| !((start_time = (String) json.get("start_time")) != null))) {
			
			throw new Exception400("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.",
					generate400("start_time does not exist.", "start_time"));
		}
			
		return start_time;
	}
	
	/*public static TimeZone GMT(Map<String, Object> json) throws Exception400 {
		String GMT;
		TimeZone zone = null;
		
		if ((!json.containsKey("GMT")
				|| !(json.get("GMT") instanceof String)
				|| !((GMT = (String) json.get("GMT")) != null))) {
			
			throw new Exception400("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.",
					generate400("GMT does not exist.", "GMT"));
		} 
		
		zone = TimeZone.getTimeZone(GMT);
		if (zone == null)
			throw new Exception400("ERROR: 400 Bad Request - Returned to client. Required keys did not exist or are empty.",
					generate400("GMT is not in the right format.", "GMT"));
		
		return zone;
	}*/
	
}
