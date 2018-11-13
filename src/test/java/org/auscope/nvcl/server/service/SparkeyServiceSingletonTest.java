package org.auscope.nvcl.server.service;

import org.junit.Test;

import java.io.IOException;

public class SparkeyServiceSingletonTest {

	@Test
	public void testReadAndWrite() {
		System.out.println("SparkeyServiceSingletonTest:enter");

		// Just to make the junit test runner work
		SparkeyServiceSingleton ss;
		try {
			ss = SparkeyServiceSingleton.getInstance();
			ss.put("a", "197");
			ss.put("b", "098");
			ss.put("c", "099");
			ss.put("d", "100");
			ss.put("e", "101");
			ss.put("f", "102");
			ss.put("g", "103");
			ss.put("h", "114");

			System.out.println(ss.get("a"));
			System.out.println(ss.get("b"));
			System.out.println(ss.get("c"));
			System.out.println(ss.get("d"));
			System.out.println(ss.get("e"));
			System.out.println(ss.get("f"));
			System.out.println(ss.get("g"));
			System.out.println(ss.get("h"));

			ss.iteration();

			System.out.println("SparkeyServiceSingletonTest:exit");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}