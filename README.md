# SendPushNotification #

Sends a HTTP/2 message with JSON to Apple's Push Notification service (APNs).  Built off the [Pushy](https://github.com/relayrides/pushy) Java library.

## Installation ##

 1. [Download complied NAR](https://github.com/danshev/SendPushNotification/blob/master/nifi-sendpushnotification-nar-1.0-SNAPSHOT.nar) into NiFi's `/lib/` directory.
 2. Change permissions on the file (>`chmod 755 nifi-sendpushnotification-nar-1.0-SNAPSHOT.nar`)
 3. Restart NiFi (`/bin/nifi.sh restart`)


## Configuration ##

After NiFi restarts and you've added the processor to your canvas ...

 - Apple Identifier (ex: com.example.app-id)
 - Filepath to your p12 certificate
 - Certificate file password
 - Apple APNs endpoint (Development / Production)
 - Device (or 'push') token to which to send
 - How you want to build the JSON to be pushed

 	> If "Use JSON from FlowFile Content" is set to `False`, then the JSON payload will be populated using the processor's properties.  If 'Use JSON from FlowFile Content' is set to `True`, then the FlowFile content will be assumed to be correctly-formatted and sent as-is.


## Usage ##

Due to the inherent delay between sending a *request* (to APNs) to push data to a remote device and *the result* of that request, the SendPushNotification processor has three relationships:

 1. **Error**: something went wrong and your request was **not made**
 2. **Sent to APNs**: nothing broke and your request was made (continue on with your Flow)
 3. **APNs Response**: the result of your request (whether it was successful ... or not)

 	> The APNs Response is also where you will receive information as to whether the remote device has unregistered (aka: the Device Token is no longer valid) and should be removed from / deactivated in your database.


### Note on behavior ###
Due to the mechanics of this processor, the `APNs response` will not be released / emitted until a new FlowFile hits the processor.  For example, if you were to use a GenerateFlowFile processor to route a single FlowFile to  SendPushNotification, you would only receive a **Sent to APNs** output.  You will not see the **APNs Response** output until you send a second FlowFile to SendPushNotification.