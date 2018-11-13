package org.auscope.nvcl.server.service;

import com.spotify.sparkey.*;
import java.io.File;
import java.io.IOException;

public class SparkeyServiceSingleton {
	private SparkeyReader reader;
	private SparkeyWriter writer;
	private SparkeyLogIterator logIterator;
	
	private static SparkeyServiceSingleton instance = null;
    
    //private constructor to avoid client applications to use constructor
    private SparkeyServiceSingleton(){}

    public static SparkeyServiceSingleton getInstance(){
    	if (instance == null) {
    		instance = new SparkeyServiceSingleton();
    		try {
				instance.init("nvcl2.spi");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
        return instance;
    }
    
    
	private void init (String sparkeyFileName) throws IOException {
		File indexFile = new File(sparkeyFileName);
		this.writer = Sparkey.appendOrCreate(indexFile, CompressionType.SNAPPY, 512);	
		this.writer.setFsync(true);
	    this.reader = Sparkey.open(indexFile);
		this.logIterator = new SparkeyLogIterator(Sparkey.getLogFile(indexFile));
	}

	public String get(String key) throws IOException {
		String value = this.reader.getAsString(key);
		if (value == null) {
			value = Boolean.toString(false);
		}
		return value;
	}

	public void put(String key, String value) throws IOException{
		this.writer.put(key, value);
		this.writer.flush();
		this.writer.writeHash();
		//this.writer.close();
	}

	public void iteration() throws IOException {
		for (SparkeyReader.Entry entry : reader) {
			String key = entry.getKeyAsString();
			String value = entry.getValueAsString();
			System.out.println(key + " " + value);
		}

	}

	public void logIteration() throws IOException {
		for (SparkeyReader.Entry entry : this.logIterator) {
			if (entry.getType() == SparkeyReader.Type.PUT) {
				String key = entry.getKeyAsString();
				String value = entry.getValueAsString();
				System.out.println(key + " " + value);
			}
		}
	}
}