package utils.papaya.com;

public enum AuthServiceType {
	FACEBOOK, 
	GOOGLE, 
	NONE;
	
	
	public String toString() {
		switch (this) {
		case FACEBOOK:
			return "FACEBOOK";
		case GOOGLE:
			return "GOOGLE";
		default:
			return "NONE";
		}
	}
}
