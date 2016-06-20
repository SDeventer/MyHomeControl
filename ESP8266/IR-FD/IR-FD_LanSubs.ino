/**
	connectWiFi
	Connect to WiFi AP
	if no WiFi is found for 60 seconds
	module is restarted
*/
void connectWiFi() {
	digitalWrite(COM_LED, LOW);
	WiFi.disconnect();
	WiFi.mode(WIFI_STA);
	WiFi.config(ipAddr, ipGateWay, ipSubNet);
	WiFi.begin(ssid, password);
	Serial.print("Waiting for WiFi connection ");
	sendDebug("Waiting for WiFi connection ");
	int connectTimeout = 0;
	while (WiFi.status() != WL_CONNECTED) {
		delay(500);
		Serial.print(".");
		connectTimeout++;
		digitalWrite(COM_LED,!digitalRead(COM_LED));
		if (connectTimeout > 60) { //Wait for 30 seconds (60 x 500 milliseconds) to reconnect
			// pinMode(16, OUTPUT); // Connected to RST pin
			// digitalWrite(16,LOW); // Initiate reset
			// ESP.reset(); // In case it didn't work
			delay(60); // Wait for a minute before retry
			connectTimeout = 0;
			WiFi.disconnect();
			WiFi.mode(WIFI_STA);
			WiFi.config(ipAddr, ipGateWay, ipSubNet);
			WiFi.begin(ssid, password);
			sendDebug("Reset WiFi connection ");
		}
	}
	digitalWrite(COM_LED, HIGH); // Turn off LED
}

/**
	WiFiEvent
	called if there is a change in the WiFi connection
*/
void WiFiEvent(WiFiEvent_t event) {

	switch (event) {
		#ifdef DEBUG_OUT
		case WIFI_EVENT_STAMODE_CONNECTED:
			Serial.println("WiFi connected");
			break;
		#endif
		case WIFI_EVENT_STAMODE_DISCONNECTED:
			#ifdef DEBUG_OUT
			Serial.println("WiFi lost connection");
			#endif
			connectWiFi();
			break;
		#ifdef DEBUG_OUT
		case WIFI_EVENT_STAMODE_AUTHMODE_CHANGE:
			Serial.println("WiFi authentication mode changed");
			break;
		case WIFI_EVENT_STAMODE_GOT_IP:
			Serial.println("WiFi got IP");
			Serial.println("IP address: ");
			Serial.println(WiFi.localIP());
			break;
		case WIFI_EVENT_STAMODE_DHCP_TIMEOUT:
			Serial.println("WiFi DHCP timeout");
			break;
		case WIFI_EVENT_MAX:
			Serial.println("WiFi MAX event");
			break;
		#endif
	}
}

/**
	 sendBroadCast
	 send updated status over LAN
	 - to my gcm server for broadcast to
	 		registered Android devices
	 - by UTP broadcast over local lan
*/
void sendBroadCast() {
	digitalWrite(COM_LED, LOW);
	DynamicJsonBuffer jsonBuffer;
	// Prepare json object for the response
	JsonObject& root = jsonBuffer.createObject();
	root["result"] = "fail";
	root["device"] = DEVICE_ID;

	root["result"] = "success";
	// Display status of aircon
	if ((acMode & AC_ON) == AC_ON) {
		root["power"] = 1;
	} else {
		root["power"] = 0;
	}
	byte testMode = acMode & MODE_MASK;
	if (testMode == MODE_FAN) {
		root["mode"] = 0;
	} else if (testMode == MODE_DRY) {
		root["mode"] = 1;
	} else if (testMode == MODE_COOL) {
		root["mode"] = 2;
	} else if (testMode == MODE_AUTO) {
		root["mode"] = 3;
	}
	testMode = acMode & FAN_MASK;
	if (testMode == FAN_LOW) {
		root["speed"] = 0;
	} else if (testMode == FAN_MED) {
		root["speed"] = 1;
	} else if (testMode == FAN_HIGH) {
		root["speed"] = 2;
	}
	testMode = acTemp & TEMP_MASK;
	root["temp"] = testMode;

	// Display power consumption and production values
	/** Calculate average power consumption of the last 10 minutes */
	consPower = 0;
	for (int i = 0; i < 10; i++) {
		consPower += avgConsPower[i];
	}
	consPower = consPower / 10;

	root["cons"] = consPower;

	// Display power cycle status
	root["status"] = powerStatus;

	// Display status of auto control by power consumption
	if ((acMode & AUTO_ON) == AUTO_ON) {
		root["auto"] = 1;
	} else {
		root["auto"] = 0;
	}
	
	// Display timer status of aircon
	if ((acMode & TIM_ON) == TIM_ON) {
		root["timer"] = 1;
	} else {
		root["timer"] = 0;
	}

	// Display last timer on time
	root["onTime"] = onTime;
	
	// Display device id
	root["device"] = DEVICE_ID;
	
	// Set flag for restart
	if (inSetup) {
		root["boot"] = 1;
	} else {
		root["boot"] = 0;
	}

	// Broadcast per UTP to LAN
	String broadCast;
	root.printTo(broadCast);
	udpClientServer.beginPacketMulticast(multiIP, 5000, ipAddr);
	udpClientServer.print(broadCast);
	udpClientServer.endPacket();
	udpClientServer.stop();
	udpClientServer.beginPacket(monitorIP,5000);
	udpClientServer.print(broadCast);
	udpClientServer.endPacket();
	udpClientServer.stop();

	// Send over Google Cloud Messaging
	// gcmSendMsg(root);

	digitalWrite(COM_LED, HIGH);
}

/**
	replyClient
	answer request on http server
	returns command to client
*/
void replyClient(WiFiClient httpClient) {
	digitalWrite(COM_LED, LOW);
	/** Flag for valid command */
	boolean isValidCmd = false;
	/** String for response to client */
	String s = "HTTP/1.1 200 OK\r\nAccess-Control-Allow-Origin: *\r\nContent-Type: application/json\r\n\r\n";
	/** Wait out time for client request */
	int waitTimeOut = 0;
	/** String to hold the response */
	String jsonString;

	/** Buffer for Json object */
	DynamicJsonBuffer jsonBuffer;

	/** json object for the response */
	JsonObject& root = jsonBuffer.createObject();
	root["result"] = "fail";
	root["device"] = DEVICE_ID;

	// Wait until the client sends some data
	while (!httpClient.available()) {
		delay(1);
		waitTimeOut++;
		if (waitTimeOut > 3000) { // If no response for 3 seconds return
			root["result"] = "timeout";
			root.printTo(jsonString);
			s += jsonString;
			httpClient.print(s);
			httpClient.flush();
			httpClient.stop();
			digitalWrite(COM_LED, HIGH);
			return;
		}
	}

	/** First line of the request */
	String req = httpClient.readStringUntil('\r');
	// Strip leading (GET, PUSH) and trailing (HTTP/1) characters
	req = req.substring(req.indexOf("/"),req.length()-9);
	/** Response to client */
	String statResponse = "fail " + req;
	root["result"] = statResponse;

	if (req.substring(0, 4) == "/?c=") { // command received
		if (isDigit(req.charAt(4))) {
			if (isDigit(req.charAt(5))) {
				statResponse = req.substring(4, 6);
				irCmd = statResponse.toInt();
				parseCmd(root);
			} else {
				irCmd = 9999;
				root["reason"] = "Invalid command";
			}
		} else {
			irCmd = 9999;
			root["reason"] = "Invalid command";
		}
		// Timer duration received
	} else if (req.substring(0, 4) == "/?t=") {
		if (isDigit(req.charAt(4))) {
			statResponse = req.substring(4, 5);
			onTime = statResponse.toInt();
			#ifdef DEBUG_OUT 
			Serial.print("Changed timer to ");
			Serial.print(onTime);
			Serial.println(" hour");
			#endif
			root["result"] = "success";
			root["onTime"] = onTime;
			writeStatus();
		} else {
			root["result"] = "fail";
			root["reason"] = "Invalid time";
		}
		 // status request received
	} else if (req.substring(0, 3) == "/?s") {
		root["result"] = "success";
		// Display status of aircon
		if ((acMode & AC_ON) == AC_ON) {
			root["power"] = 1;
		} else {
			root["power"] = 0;
		}
		byte testMode = acMode & MODE_MASK;
		if (testMode == MODE_FAN) {
			root["mode"] = 0;
		} else if (testMode == MODE_DRY) {
			root["mode"] = 1;
		} else if (testMode == MODE_COOL) {
			root["mode"] = 2;
		} else if (testMode == MODE_AUTO) {
			root["mode"] = 3;
		}
		testMode = acMode & FAN_MASK;
		if (testMode == FAN_LOW) {
			root["speed"] = 0;
		} else if (testMode == FAN_MED) {
			root["speed"] = 1;
		} else if (testMode == FAN_HIGH) {
			root["speed"] = 2;
		}
		testMode = acTemp & TEMP_MASK;
		root["temp"] = testMode;

		// Display timer status of aircon
		if ((acMode & TIM_ON) == TIM_ON) {
			root["timer"] = 1;
		} else {
			root["timer"] = 0;
		}
		root["onTime"] = onTime;
		
		// Display power consumption and production values
		/** Calculate average power consumption of the last 10 minutes */
		consPower = 0;
		for (int i = 0; i < 10; i++) {
			consPower += avgConsPower[i];
		}
		consPower = consPower / 10;

		root["cons"] = consPower;

		// Display power cycle status
		root["status"] = powerStatus;

		// Display status of auto control by power consumption
		if ((acMode & AUTO_ON) == AUTO_ON) {
			root["auto"] = 1;
		} else {
			root["auto"] = 0;
		}
		root["build"] = compileDate;
		root["daytime"] = dayTime;
		String nowTime = String(hour()) + ":";
		if (minute() < 10) {
			nowTime += "0";
		}
		nowTime += String(minute());
		root["time"] = nowTime;
		// // Registration of new device
	// } else if (req.substring(0, 8) == "/?regid=") {
		// /** String to hold the received registration ID */
		// String regID = req.substring(8,req.length());
		// #ifdef DEBUG_OUT
		// Serial.println("RegID: "+regID);
		// Serial.println("Length: "+String(regID.length()));
		// #endif
		// // Check if length of ID is correct
		// if (regID.length() != 140) {
			// #ifdef DEBUG_OUT 
			// Serial.println("Length of ID is wrong");
			// #endif
			// root["result"] = "invalid";
			// root["reason"] = "Length of ID is wrong";
		// } else {
			// // Try to save ID 
			// if (!addRegisteredDevice(regID)) {
				// #ifdef DEBUG_OUT 
				// Serial.println("Failed to save ID");
				// #endif
				// root["result"] = "failed";
				// root["reason"] = failReason;
			// } else {
				// #ifdef DEBUG_OUT 
				// Serial.println("Successful saved ID");
				// #endif
				// root["result"] = "success";
				// getRegisteredDevices();
				// for (int i=0; i<regDevNum; i++) {
					// root[String(i)] = regAndroidIds[i];
				// }
				// root["num"] = regDevNum;
			// }
		// }
		// // Delete one or all registered device
	// } else if (req.substring(0, 3) == "/?d"){
		// /** String for the sub command */
		// String delReq = req.substring(3,4);
		// if (delReq == "a") { // Delete all registered devices
			// if (delRegisteredDevice()) {
				// root["result"] = "success";
			// } else {
				// root["result"] = "failed";
				// root["reason"] = failReason;
			// }
		// } else if (delReq == "i") {
			// /** String to hold the ID that should be deleted */
			// String delRegId = req.substring(5,146);
			// delRegId.trim();
			// if (delRegisteredDevice(delRegId)) {
				// root["result"] = "success";
			// } else {
				// root["result"] = "failed";
				// root["reason"] = failReason;
			// }
		// } else if (delReq == "x") {
			// /** Index of the registration ID that should be deleted */
			// int delRegIndex = req.substring(5,req.length()).toInt();
			// if ((delRegIndex < 0) || (delRegIndex > MAX_DEVICE_NUM-1)) {
				// root["result"] = "invalid";
				// root["reason"] = "Index out of range";
			// } else {
				// if (delRegisteredDevice(delRegIndex)) {
					// root["result"] = "success";
				// } else {
					// root["result"] = "failed";
					// root["reason"] = failReason;
				// }
			// }
		// }
		// // Send list of registered devices
		// if (getRegisteredDevices()) {
			// if (regDevNum != 0) { // Any devices already registered?
				// for (int i=0; i<regDevNum; i++) {
					// root[String(i)] = regAndroidIds[i];
				// }
			// }
			// root["num"] = regDevNum;
		// }
		// // Send list of registered devices
	// } else if (req.substring(0, 3) == "/?l"){
		// if (getRegisteredDevices()) {
			// if (regDevNum != 0) { // Any devices already registered?
				// for (int i=0; i<regDevNum; i++) {
					// root[String(i)] = regAndroidIds[i];
				// }
			// }
			// root["num"] = regDevNum;
			// root["result"] = "success";
		// } else {
			// root["result"] = "failed";
			// root["reason"] = failReason;
		// }
		// // toggle debugging
	} else if (req.substring(0, 3) == "/?b"){
		debugOn = !debugOn;
		root["result"] = "success";
		root["status"] = debugOn;
		// initialization request received
	} else if (req.substring(0, 3) == "/?r") {
		irCmd = CMD_INIT_AC;
		root["result"] = "success";
	}
	// Send the response to the client
	root.printTo(jsonString);
	s += jsonString;
	httpClient.print(s);
	httpClient.flush();
	httpClient.stop();

	digitalWrite(COM_LED, HIGH);
}

// For debug over TCP
void sendDebug(String debugMsg) {
	if (debugOn) {
		digitalWrite(COM_LED, LOW);
		const int httpPort = 9999;
		if (!tcpClient.connect(debugIP, httpPort)) {
			Serial.println("connection to Debug PC " + String(debugIP[0]) + "." + String(debugIP[1]) + "." + String(debugIP[2]) + "." + String(debugIP[3]) + " failed");
			tcpClient.stop();
			digitalWrite(COM_LED, HIGH);
			return;
		}

		String sendMsg = DEVICE_ID;
		debugMsg = sendMsg + " " + debugMsg;
		tcpClient.print(debugMsg);

		tcpClient.stop();
		digitalWrite(COM_LED, HIGH);
	}
}


