package com.emc.mongoose.common.net.http.request.format;

public class HFDemo {

	private final static String pattern = "Dan %D[1969/01/01-2016/12/31] %f[+1-10.0] %d[1-a] %f blabla";

	public static void main(String[] args) {
		HeaderFormatter formatter = new HeaderFormatter(pattern);
		while(true) {
			System.out.println(formatter.format());
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
