package test.papaya.com;

public class FunctionInputs 
{
	
	class InsertClassInput {
		
		BodyJson body_json;
		
		class BodyJson {
			String user_id;
			String service;
			String authentication_key;
			String service_user_id;
			String classname;
			String description;
		}
	}
	
	class GetSessionUsersInput {
		Params params;
		
		class Params {
			Path path;
			QueryString querystring;
			
			class Path {
				String class_id;
				String session_id;
			}
			class QueryString {
				String user_id;
				String service;
				String authentication_key;
				String service_user_id;
			}
		}
	}
	
	class InsertSessionInput {
		BodyJson body_json;
		Params params;
		
		class BodyJson {
			String user_id;
			String service;
			String authentication_key;
			String service_user_id;
			Integer duration;
			Double location_lat;
			Double location_long;
			String location_desc;
			String description;
			Boolean sponsored;
		}
		
		class Params {
			Path path;
			
			class Path {
				String class_id;
			}
		}
	}
	
}
