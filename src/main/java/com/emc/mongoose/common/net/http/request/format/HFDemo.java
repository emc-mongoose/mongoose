package com.emc.mongoose.common.net.http.request.format;

public class HFDemo {

	private final static String pattern = "Dan %D[2014/01/01-2016/12/31] %f[1.0-50.0] %d[1-4] %f blabla";

	public static void main(String[] args) {
		HeaderFormatter formatter = new HeaderFormatter(pattern);
		formatter.format();
//		System.out.println(pattern);
//		System.out.println(formatter);
	}

}
