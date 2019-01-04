package cn.com.fmsh.util;

public class CRCUtil {

	private static int[] Crc16Calc(byte[] data_arr, int data_len) {
		int crc16 = 0;
		int i;
		for (i = 0; i < (data_len); i++) {
			crc16 = ( char ) ((crc16 >> 8) | (crc16 << 8));
			crc16 ^= data_arr[i] & 0xFF;
			crc16 ^= ( char ) ((crc16 & 0xFF) >> 4);
			crc16 ^= ( char ) ((crc16 << 8) << 4);
			crc16 ^= ( char ) (((crc16 & 0xFF) << 4) << 1);
		}
		int[] result = new int[2];
		result[0] = (crc16 / 256);
		result[1] = (crc16 % 256);
		return result;
	}


	private static byte[] toBytes(String str) {
		if (str == null || str.trim().equals("")) {
			return new byte[0];
		}

		byte[] bytes = new byte[str.length() / 2];
		for (int i = 0; i < str.length() / 2; i++) {
			String subStr = str.substring(i * 2, i * 2 + 2);
			bytes[i] = ( byte ) Integer.parseInt(subStr, 16);
		}
		return bytes;
	}

	public static String getCrc16(String Key) {
		byte[] test = toBytes(Key);
		int[] result = Crc16Calc(test, test.length);
		String s = Integer.toHexString(result[0])
				+ Integer.toHexString(result[1]);
		return s;
	}

}
