/**
 * 
 */
package com.burritopos.server.rest.utilities;

import org.apache.log4j.Logger;

import com.burritopos.server.rest.webresource.ServerService;

import javax.ws.rs.core.MultivaluedMap;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 *
 */
public class BurritoPOSUtils {
	private static Logger dLog = Logger.getLogger(BurritoPOSUtils.class);
	private static Map<String, String> properties = new HashMap<String, String>();
	
	static {

	}
	
	/**
	 * Gets a given property
	 * @param propName
	 * @return
	 * @throws IOException 
	 */
	public static String getProperty(String propName) throws IOException {
		String propValue = "";
		
		//logger.trace("prop count: " + properties.size());
		if(properties.containsKey(propName)) {
			propValue = properties.get(propName);
		}
		else {
	    	// get burrito pos service properties
	        Properties propList = new Properties();
	        dLog.trace("Loading burritoposserver.properties");

	        propList.load(ServerService.class.getResourceAsStream("burritoposserver.properties"));
	        propValue = propList.getProperty(propName);
	        properties.put(propName, propValue);
	            
	        dLog.trace("Got " + propName + " value: " + propValue);
		}
		
		return propValue;
	}
    
    /**
     * Parses multi-valued map into a map object
     * @param queryParameters
     * @return Map<>
     */
    public static Map<String, String> parseMultivaluedMap(MultivaluedMap<String, String> queryParameters) {
        Map<String, String> map = new HashMap<String, String>();
        for (Map.Entry<String, List<String>> param : queryParameters.entrySet()) {
            String key = param.getKey();
            String value = param.getValue().iterator().next();
            map.put(key, value);
        }
        return map;
    }
    
	/**
	* Writes temporary BPMN XML file out so it can be loaded as a process definition.
	*
	* @param bpmnXML
	* @param fileName
	* @throws IOException
	*/	
	public static void writeFile(String bpmnXML, String dir, String fileName) throws IOException {
        BufferedWriter out = null;
        try {
        	dLog.trace("Dir: " + dir + " | fileName: " + fileName);
            // ensure directory exists
            File directoryExists = new File(dir);

            if (!directoryExists.exists()) {
                directoryExists.mkdirs();
            }

            out = new BufferedWriter(new FileWriter(dir + fileName));
            out.write(bpmnXML);
        } catch (IOException e) {
        	dLog.error("Writing file " + fileName + " failed", e);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
}
