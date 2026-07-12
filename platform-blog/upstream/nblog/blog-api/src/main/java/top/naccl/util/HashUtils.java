package top.naccl.util;

import cn.hutool.core.lang.hash.MurmurHash;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * @Description: Hash工具类
 * @Author: Naccl
 * @Date: 2020-11-17
 */
public class HashUtils {
	private static final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

	public static long getMurmurHash32(String str) {
		int i = MurmurHash.hash32(str);
		long num = i < 0 ? Integer.MAX_VALUE - (long) i : i;
		return num;
	}

	public static String getBC(CharSequence rawPassword) {
		return bCryptPasswordEncoder.encode(rawPassword);
	}

	public static boolean matchBC(CharSequence rawPassword, String encodedPassword) {
		return bCryptPasswordEncoder.matches(rawPassword, encodedPassword);
	}
}
