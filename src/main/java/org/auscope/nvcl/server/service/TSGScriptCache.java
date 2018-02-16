package org.auscope.nvcl.server.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class TSGScriptCache {
	
    private static final Logger logger = LogManager.getLogger(TSGScriptCache.class);
    
	private HashMap<String, String> scripts;

	public TSGScriptCache() {

		this.scripts = new HashMap<String, String>();

		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(getClass().getClassLoader().getResourceAsStream("tsgscripts/scripts.txt")))) {
			String firstLine;

			while ((firstLine = br.readLine()) != null) {
				Pattern pattern = Pattern.compile("^name = ([A-Za-z0-9 _-]*),[ 0-9]*$");
				Matcher matcher = pattern.matcher(firstLine);
				if (matcher.matches()) {
					String name = matcher.group(1);
					String script = new String(firstLine + '\n');
					String scriptline = "";
					while ((scriptline = br.readLine()) != null && !scriptline.equals("")) {
						script += scriptline + '\n';
					}
					this.scripts.put(name, script);
				}
			}
		} catch (IOException e) {
			logger.error("Failed to load TSG Algorithm scripts from resource file tsgscripts/scripts.txt");
		} 
		logger.info("Successfully loaded " + this.scripts.size()+ " TSG Algorithm scripts.");
	}

	public HashMap<String, String> getScripts() {
		return scripts;
	}

}
