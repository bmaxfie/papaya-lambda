package utils.papaya.com;

//1 = STUDENT
//2 = TA
//3 = PROFESSOR
public enum UserRole {
	STUDENT, TA, PROFESSOR;
	
	public static int value(UserRole role) {
		switch(role) {
			case STUDENT: return 1;
			case TA: return 2;
			case PROFESSOR: return 3;
			default: return 0;
			
		}
	}
}
