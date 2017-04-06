package test.papaya.com;

import java.util.List;

public class FunctionOutputs {
	
	class InsertClassOutput {
		Integer code;
		String code_description;
		String error_description;
		String fields;
		String student_access_key;
		String ta_access_key;
		String professor_access_key;
	}
	
	class GetSessionUsersOutput {
		Integer code;
		String code_description;
		String error_description;
		String fields;
		// TODO: How to represent an array of Users in POJO?
		List<User> users;
		String description;
		String location_desc;
		String authentication_key;
		
		class User {
			String user_id;
			String username;
		}
	}
	
	class InsertSessionOutput {
		Integer code;
		String code_description;
		String error_description;
		String fields;
		String session_id;
		String class_id;
	}

}
