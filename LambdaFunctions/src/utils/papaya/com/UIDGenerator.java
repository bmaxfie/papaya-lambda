package utils.papaya.com;

import java.math.BigInteger;

import com.amazonaws.util.Md5Utils;

public class UIDGenerator {
	
	/**
	 * generateUID - generates a 'unique' MD5 hash from the argument String and the current time in nanoseconds of the machine.
	 * @param arg - input string to salt the hash. Could be the username, etc.
	 * @return - A ~20 character base64 UID hash.
	 */
	public static String generateUID(String arg) {
    	BigInteger nameint = new BigInteger(arg.getBytes());
        BigInteger timeint = new BigInteger(Long.toString(System.nanoTime()));
        String hash = Md5Utils.md5AsBase64(nameint.multiply(timeint).toByteArray());
        if (hash.length() > 45)
    		return hash.substring(hash.length() - 45);
        return hash;
    }
}
