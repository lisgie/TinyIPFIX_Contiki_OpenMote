package utils;

public final class Converter {

	final private static char[] hexArray = "0123456789ABCDEF".toCharArray();
	
	public static String byteArrayToHexString(byte[] byteArray) {
		char[] hexChars = new char[byteArray.length * 2];
		for (int j = 0; j < byteArray.length; j++) {
			int v = byteArray[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}
	
	public static byte[] hexStringToByteArray(String hexString) {
		int len = hexString.length();
	    byte[] byteArray = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        byteArray[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
	                             + Character.digit(hexString.charAt(i+1), 16));
	    }
	    return byteArray;
	}
}
