/*
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.verizon.onosapp;
//import org.apache.felix.scr.annotations.*;
import org.apache.felix.scr.annotations.*;
import org.onosproject.core.CoreService;
//import org.onosproject.net.Device;
//import org.onosproject.
import org.onosproject.net.*;
import org.onosproject.net.device.*;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;        
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import org.clapper.util.misc.FileHashMap;

import java.net.MalformedURLException;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;



/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true)
public class AppComponent {
	FileHashMap<String,String> fhm = null;
	
    private final Logger log = LoggerFactory.getLogger(getClass());
    private DeviceListener deviceListener;
    private  HostListener hostListener;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

     @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceAdminService adminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceProviderRegistry providerRegistry;


    private HashMap<String,String> appConfig = new HashMap<String, String>();
    @Activate
    protected void activate() {
    	 
        deviceListener = new InnerDeviceListener();
        deviceService.addListener(deviceListener);
        
        hostListener= new InnerHostListener();
        hostService.addListener(hostListener);
        log.info("Device Event: Started");
        
            //JSch jsch=new JSch();
    }
    
    protected boolean resetFileMapper(String deviceId) {
    	 boolean status=true;
    	try {
    	    log.debug("Creating File HashMap");
            fhm = new FileHashMap<String,String>("/home/ubuntu/Applications/data/vcpe.dat",0);
            fhm.clear();
            fhm.save();
            fhm.close();
    	    return status;
    	}
    	 catch (Exception ex)
        {
    		status=false; 	
            log.error("Exception during opening FileHashMap",ex);
            return status;
        }
    }
    protected String validate(String deviceId) {
    	try {
    		String devicefound="";
    		log.debug("Checking File HashMap");
            fhm = new FileHashMap<String,String>("/home/ubuntu/Applications/data/vcpe.dat",0);
            for (Iterator keys = fhm.keySet().iterator(); keys.hasNext(); )
            {
                Object key   = keys.next();
                Object value = fhm.get(key);
                log.info("Device Active key = {}  value = {} ",key , value); 
                //System.out.println("key={} " + key + ", value=" + value);
                if ( key.toString().equalsIgnoreCase(deviceId)) {
                	log.info("Device already present = {}",deviceId);
                	devicefound="Present";
                	}
              }
          if (devicefound.isEmpty()) {
        	  String userName="user:"+deviceId;
        	  fhm.put(deviceId,userName);
        	  log.info("Adding Device to FileMap={}",deviceId);
          }
             //String userName="user:"+deviceId;
            fhm.save();
            fhm.close();
            return devicefound;
        }
        catch (Exception ex)
        {
            log.error("Exception during opening FileHashMap",ex);
        
		return "Error_FileHashMap";
        }
    }

    protected void instanceUpdate(String deviceId , String insType) {
    	String res;
      
           log.info("Creating Instance for Device = {}", deviceId);
           res=invokeShell(deviceId,insType);
           log.info("Instance Response= {}", res);
    }
  
    
    public String  invokeShell(String deviceId , String insType) {
		String host = "10.76.110.90";
		String user = "sdnos";
		String password = "sdnos";
		java.util.Properties config = new java.util.Properties();
		config.put("StrictHostKeyChecking", "no");
		JSch jsch = new JSch();
		Channel channel = null;
		 
		try {
			Session session = jsch.getSession(user, host, 22);
			session.setPassword(password);
			session.setConfig(config);
			session.connect();
			log.info("Connecting Ssh ... Session is = {} ",session.isConnected());
			log.info("Instant Type:::",insType);
			channel = session.openChannel("exec");
			InputStream outStream = channel.getInputStream();
			
			String instance="VCPE_FW:".concat(deviceId);
			//String cmd = "sh  VCPE_FWScript_final.sh " + instance + "  > /home/sdnos/run.txt";
			//String cmd = "sh  VCPE_test.sh " + instance ;
			String cmd;
			if (insType.startsWith("Del"))  {
				cmd="sh  VCPE_FWScript_final.sh " + instance + " 0  >  /home/sdnos/run.txt";
			  if ( resetFileMapper(deviceId)) {
				log.info("Sucessfully cleared the previous Instance",deviceId);  
			  }
			  else {
				  log.info("Did not  clear the previous Instance",deviceId);
			  		}
			}			
			else {
				cmd = "sh  VCPE_FWScript_final.sh " + instance + " 1 > /home/sdnos/run.txt" ;
			}
				log.info("Command to be executed = {} ",cmd);
			
				((ChannelExec) channel).setCommand(cmd);
			channel.connect();
			/*BufferedReader buff = new BufferedReader(new InputStreamReader(outStream));
			String line = buff.readLine();*/
			//log.info("Ssh Bufferred line" + line);
			channel.disconnect();
			session.disconnect();
			return "Success";
			

		} catch (Exception e) {
			e.printStackTrace();
			log.info("Exceptions = {}" ,e.getMessage().toString());
			return e.getMessage().substring(1, 300).toString();
			
		}
	

	}

    
    
    
    @Deactivate
    protected void deactivate() {
        deviceService.removeListener(deviceListener);
           log.info("Stopped");
    }

    // Triggers driver setup when a device is (re)detected.
    private class InnerDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
        	DeviceId devId;
        	String deviceFound="";
            log.info("Event Detected eventInfo=:{}" , event.toString());
            log.info("Event.subject.id --->deviceid :{}" , event.subject().id());
            //event.type().
             String deviceId1;
            devId=event.subject().id();
            deviceId1=devId.toString();
            String insType="create";
        	log.info("Device event:Top Level={}" , deviceId1.toString() );
        	log.info("Device event Type Top Level={}  devId = {} " , event.type().toString() , deviceId1);
        	
            switch (event.type()) {
                case DEVICE_ADDED:
                	//String deviceId=event.subject().id().toString();
                	log.info("device_added" , event.subject().id().toString());
                	log.info("device_added" , event.subject().id());
                	deviceFound = validate(deviceId1);
                	log.info("Returned from ValidateFunction = {}" , deviceFound);
                	if (deviceFound.isEmpty() ) {   			
                		instanceUpdate(deviceId1,insType); 
                	}
                	
                    break;
                case DEVICE_AVAILABILITY_CHANGED:
                	log.info("device_availability_changed" ,  event.subject().id().toString());
                	 devId=event.subject().id();
                     deviceId1=devId.toString();
                    	deviceFound = validate(deviceId1);
                    	log.info("Checking of Device Present..Returned from ValidateFunction = {}" , deviceFound);
                 	if (deviceFound.contentEquals("Present")) { 
                 		insType="Del";
                 		instanceUpdate(deviceId1,insType); 
                 	}
                 	else
                 		{
                 			insType="Cre";
                     		instanceUpdate(deviceId1,insType); 
               		
                 		}
                	
                	break;
               	
                	
                case PORT_STATS_UPDATED:
                	log.info("device_stats_updated ={}" , event.type().toString());
                    break;
                // TODO other cases
                case DEVICE_UPDATED:
                   log.info("device_updated = {}" ,event.type().toString() );
                   devId=event.subject().id();
                   deviceId1=devId.toString();
                   log.info("device_updated:DeviceId={}" ,deviceId1);
                   log.info("Returned from ValidateFunction = {}" , deviceFound);
               		deviceFound = validate(deviceId1);
                	if (deviceFound.isEmpty() ) {   			
                		instanceUpdate(deviceId1,insType); 
                	}
                	break;
                    
                case DEVICE_REMOVED:
                	log.info("device_removed = {} " ,event.type().toString() );
                    break;
                case DEVICE_SUSPENDED:
                	
                    break;
                case PORT_ADDED:
                    break;
                case PORT_UPDATED:
                    break;
                case PORT_REMOVED:
                    break;
                default:
                	log.info("In Default Break..........",event.type().toString() );
                	
                
                    break;
            }
        }
    }

    private class InnerHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {
            log.debug("Host Event: time = {} type = {} event = {}",
                    event.time(), event.type(), event);
            
            log.debug("Host Event: time = {} type = {} event = {} deviceidFromHost={}  = ",
                    event.time(), event.type(), event , event.subject().id());
            
            
            
        }
    }
        
   protected boolean WriteToFileExample(String content) {
	   boolean status=true;
    		try {
    			//String content = "This is the content to write into file";
    			File file = new File("/home/ubuntu/Applications/data/Events.txt");
    			// if file doesn't exists, then create it
    			if (!file.exists()) {
    				file.createNewFile();
    			}
    			FileWriter fw = new FileWriter(file.getAbsoluteFile());
    			BufferedWriter bw = new BufferedWriter(fw);
    			bw.write(content);
    			bw.close();
    			System.out.println("Done");
    			return status;

    		} catch (IOException e) {
    			e.printStackTrace();
    			return status;
    		}
    		
    	}
       
  
    

    
    

    
    
    
    
} //end of class 
