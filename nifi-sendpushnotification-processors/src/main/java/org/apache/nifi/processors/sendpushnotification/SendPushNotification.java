/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.processors.sendpushnotification;

import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.annotation.lifecycle.OnUnscheduled;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.util.StandardValidators;

import com.turo.pushy.apns.ApnsClient;
import com.turo.pushy.apns.ApnsClientBuilder;
import com.turo.pushy.apns.PushNotificationResponse;
import com.turo.pushy.apns.util.ApnsPayloadBuilder;
import com.turo.pushy.apns.util.SimpleApnsPushNotification;
import com.turo.pushy.apns.util.TokenUtil;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.StringUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

@Tags({"apns apn"})
@CapabilityDescription("Pushes JSON Content data to APNs")
@SeeAlso({})
@ReadsAttributes({@ReadsAttribute(attribute="", description="")})
@WritesAttributes({@WritesAttribute(attribute="", description="")})
public class SendPushNotification extends AbstractProcessor {

	

    public static final PropertyDescriptor APNS_SERVER = new PropertyDescriptor
            .Builder().name("APNS_SERVER")
            .displayName("APNS Server Mode")
            .defaultValue("Development")
            .expressionLanguageSupported(true)
            .allowableValues("Production", "Development")
            .required(true)
            .build();
	

    public static final PropertyDescriptor APNS_NAME = new PropertyDescriptor
            .Builder().name("APNS_NAME")
            .displayName("APNS App Package Name")
            .expressionLanguageSupported(true)
            .description("Specify APNS Package Name")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .required(true)
            .build();

    public static final PropertyDescriptor CERT_FILE = new PropertyDescriptor
            .Builder().name("CERT_FILE")
            .displayName("Certificate File")
            .description("Certificate File")
            .expressionLanguageSupported(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .required(true)
            .build();
    

    public static final PropertyDescriptor CERT_PASSWORD = new PropertyDescriptor
            .Builder().name("CERT_PASSWORD")
            .displayName("Certificate File Password")
            .description("Certificate File Password")
            .expressionLanguageSupported(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .sensitive(true)
            .required(true)
            .build();
    
    public static final PropertyDescriptor DEVICE_TOKEN = new PropertyDescriptor
            .Builder().name("DEVICE_TOKEN")
            .displayName("Device Identifier Token")
            .expressionLanguageSupported(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .required(false)
            .build();
    
    public static final PropertyDescriptor PAYLOAD_BADGE = new PropertyDescriptor
            .Builder().name("PAYLOAD_BADGE")
            .displayName("Payload: Badge")
            .expressionLanguageSupported(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .required(false)
            .build();
    
    public static final PropertyDescriptor PAYLOAD_ALERT = new PropertyDescriptor
            .Builder().name("PAYLOAD_ALERT")
            .displayName("Payload: Alert Title")
            .expressionLanguageSupported(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .required(false)
            .build();
    
    public static final PropertyDescriptor PAYLOAD_SOUND = new PropertyDescriptor
            .Builder().name("PAYLOAD_SOUND")
            .displayName("Payload: Sound")
            .expressionLanguageSupported(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .required(false)
            .build();
    
    public static final PropertyDescriptor PAYLOAD_CONTENT_AVAILABLE = new PropertyDescriptor
            .Builder().name("PAYLOAD_CONTENT_AVAILABLE")
            .displayName("Payload: Content-Available")
            .defaultValue("true")
            .allowableValues("true", "false")
            .expressionLanguageSupported(true)
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .required(false)
            .build();

    public static final PropertyDescriptor PAYLOAD_CATEGORY = new PropertyDescriptor
            .Builder().name("PAYLOAD_CATEGORY")
            .displayName("Payload: Category")
            .expressionLanguageSupported(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .required(false)
            .build();
    
    public static final PropertyDescriptor PAYLOAD_THREAD_ID = new PropertyDescriptor
            .Builder().name("PAYLOAD_THREAD_ID")
            .displayName("Payload: Thread ID")
            .expressionLanguageSupported(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .required(false)
            .build();


    public static final Relationship PUBLISHED = new Relationship.Builder()
            .name("Published")
            .description("Published to APNS")
            .build();

    public static final Relationship ERROR = new Relationship.Builder()
            .name("Error")
            .description("Error")
            .build();

    public static final Relationship STATUS = new Relationship.Builder()
            .name("Publish Status")
            .description("Published to APNS Successfuly")
            .build();
    
    private List<PropertyDescriptor> descriptors;
    private Set<Relationship> relationships;

    private static Queue<PushEntry> workLoad = new LinkedList<PushEntry>();
    private static Queue<PushEntry> response = new LinkedList<PushEntry>();
    
    private ApnsClient apnsClient = null;
    
    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add(APNS_NAME);
        descriptors.add(CERT_FILE);
        descriptors.add(CERT_PASSWORD);
        descriptors.add(PAYLOAD_BADGE);
        descriptors.add(PAYLOAD_ALERT);
        descriptors.add(PAYLOAD_SOUND);
        descriptors.add(PAYLOAD_CONTENT_AVAILABLE);
        descriptors.add(PAYLOAD_CATEGORY);
        descriptors.add(PAYLOAD_THREAD_ID);
        descriptors.add(DEVICE_TOKEN);
        descriptors.add(APNS_SERVER);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<Relationship>();
        relationships.add(PUBLISHED);
        relationships.add(ERROR);
        relationships.add(STATUS);
        this.relationships = Collections.unmodifiableSet(relationships);
        
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }
    
    @OnUnscheduled
    public void onUnscheduled() {
    	if (apnsClient != null) {
    		apnsClient.close();
    	}
    }


    @OnScheduled
    public void onScheduled(final ProcessContext context) {
		final String apns_server = context.getProperty(APNS_SERVER).getValue();
		final String apns_name = context.getProperty(APNS_NAME).getValue();
		final String cert_file = context.getProperty(CERT_FILE).getValue();
		final String cert_password = context.getProperty(CERT_PASSWORD).getValue();
		final Integer payload_badge = context.getProperty(PAYLOAD_BADGE).asInteger();
		final String payload_alert = context.getProperty(PAYLOAD_ALERT).getValue();
		final String payload_sound = context.getProperty(PAYLOAD_SOUND).getValue();
		final boolean payload_content_available = context.getProperty(PAYLOAD_CONTENT_AVAILABLE).asBoolean();
		final String payload_category = context.getProperty(PAYLOAD_CATEGORY).getValue();
		final String payload_threadID = context.getProperty(PAYLOAD_THREAD_ID).getValue();

    	String hostname = "";
    	int port = 443;
    	
    	if (apns_server.equals("Production")) {
    		hostname = "api.push.apple.com";
    	}
    	else {
    		hostname = "api.development.push.apple.com";
    	}
    	
    	while (true) {
    		PushEntry entry = workLoad.poll();
    		if (entry == null) {
    			break;
    		}

        	
    		final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
            payloadBuilder.setAlertBody(entry.getContent());
            
            if (payload_badge != null) {
            	payloadBuilder.setBadgeNumber(payload_badge);
            }
            
            if (!StringUtil.isNullOrEmpty(payload_sound)) {
            	payloadBuilder.setSoundFileName(payload_sound);
            }
            
            if (!StringUtil.isNullOrEmpty(payload_category)) {
            	payloadBuilder.setCategoryName(payload_category);
            }

            if (!StringUtil.isNullOrEmpty(payload_threadID)) {
                payloadBuilder.setThreadId(payload_threadID);
            }

            if (!StringUtil.isNullOrEmpty(payload_alert)) {
                payloadBuilder.setAlertTitle(payload_alert);
            }
            
            payloadBuilder.setContentAvailable(payload_content_available);
            	
            final String payload = payloadBuilder.buildWithDefaultMaximumLength();
            final String token = TokenUtil.sanitizeTokenString(entry.getDeviceIdentifier());

            SimpleApnsPushNotification pushNotification = new SimpleApnsPushNotification(token, apns_name, payload);

        	try {
        		apnsClient = new ApnsClientBuilder()
        				.setClientCredentials(new File(cert_file), cert_password)
        				.setApnsServer(hostname, port)
        				.build();

                final Future<PushNotificationResponse<SimpleApnsPushNotification>> sendNotificationFuture =
                        apnsClient.sendNotification(pushNotification);
                
                sendNotificationFuture.addListener(new GenericFutureListener<Future<PushNotificationResponse>>() {            	  
                	@Override
                	  public void operationComplete(final Future<PushNotificationResponse> future) throws Exception {
                	    if (future.isSuccess()) {        	
                	      final PushNotificationResponse<SimpleApnsPushNotification> pushNotificationResponse =
                	          sendNotificationFuture.get();
                	      if (pushNotificationResponse.isAccepted()) {
                  	    	  getLogger().info("SendPushNotification: Accepted");
                	    	  PushEntry responseEntry = new PushEntry();
                	    	  responseEntry.setContent(entry.getContent());
                	    	  responseEntry.setDeviceIdentifier(entry.getDeviceIdentifier());
                	    	  responseEntry.setStatus("Push Accepted");
                	    	  response.add(responseEntry);
                	      }
                	      else {
                  	    	  getLogger().error("SendPushNotification: Denied: " + pushNotificationResponse.getRejectionReason());
                	    	  PushEntry responseEntry = new PushEntry();
                	    	  responseEntry.setContent(entry.getContent());
                	    	  responseEntry.setDeviceIdentifier(entry.getDeviceIdentifier());
                	    	  responseEntry.setStatus("Push Denied: " + pushNotificationResponse.getRejectionReason());
                	    	  response.add(responseEntry);
                	      }
                	    }
                	    else {
                	      getLogger().error("SendPushNotification: Failure");
              	    	  PushEntry responseEntry = new PushEntry();
              	    	  responseEntry.setContent(entry.getContent());
              	    	  responseEntry.setDeviceIdentifier(entry.getDeviceIdentifier());
              	    	  responseEntry.setStatus("Push Failure");
              	    	  response.add(responseEntry);
                	    }
                	  }
                });
        	}
        	catch (Exception e) {
            	getLogger().error("SendPushNotification: Client Connection Error: " + e);
            	e.printStackTrace();

        	}
        }
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
		onScheduled(context);
    	while (true) {
    		PushEntry responseEntry = response.poll();
    		if (responseEntry == null) {
    			break;
    		}
    		else {
    			FlowFile f = session.create();
    			f = session.putAttribute(f, "deviceIdentifier", responseEntry.getDeviceIdentifier());
    			f = session.putAttribute(f, "content", responseEntry.getContent());
    			f = session.putAttribute(f, "status", responseEntry.getStatus());
    			session.transfer(f, STATUS);
    		}
    	}
    	
        FlowFile flowFile = session.get();
        if ( flowFile == null ) {
            return;
        }
        
        context.getProperty(PAYLOAD_ALERT).getValue();
        
		String deviceIdentifier = context.getProperty(DEVICE_TOKEN).getValue();
		if (StringUtil.isNullOrEmpty(deviceIdentifier)) {
			deviceIdentifier = "";
		}
        // A device token is an identifier for the Apple Push Notification System for iOS devices. Apple assigns a Device Token on a per-app basis (iOS 7 and later) which is used as a unique identifier for sending push notifications. Each device has two device tokens per app: one for development, and one for production (ad hoc or app store builds). The tokens are 64 hexadecimal characters.
        
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        session.exportTo(flowFile, bytes);
        final String contents = bytes.toString();

        PushEntry entry = new PushEntry();
        entry.setContent(contents);
        entry.setDeviceIdentifier(deviceIdentifier);
        
        workLoad.add(entry);
        session.transfer(flowFile, PUBLISHED);
        
    }




}
