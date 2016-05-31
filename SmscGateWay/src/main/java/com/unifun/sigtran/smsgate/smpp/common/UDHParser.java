package com.unifun.sigtran.smsgate.smpp.common;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class UDHParser {
	
	public static  Map<Integer, byte[]> parse(byte[] udh) {
		Map<Integer, byte[]> parsedUDH = new HashMap();
		if(udh != null) {
			while (udh.length != 0) {
				parsedUDH.put((int) udh[0], Arrays.copyOfRange(udh, 2, udh[1] + 2));
				udh = Arrays.copyOfRange(udh, udh[1] + 2, udh.length);
			}			
		}
		return parsedUDH;
	}
}
