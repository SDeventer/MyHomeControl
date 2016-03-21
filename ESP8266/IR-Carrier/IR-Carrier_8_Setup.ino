void setup() {
	pinMode(IR_LED_OUT, OUTPUT); // IR LED red
	pinMode(COM_LED, OUTPUT); // Communication LED blue
	pinMode(ACT_LED, OUTPUT); // Communication LED red
	digitalWrite(IR_LED_OUT, LOW); // Turn off IR LED
	digitalWrite(COM_LED, HIGH); // Turn off blue LED
	digitalWrite(ACT_LED, HIGH); // Turn off red LED

	Serial.begin(115200);

	Serial.setDebugOutput(false);
	Serial.println("");
	Serial.println("Hello from ESP8266");

	// Setup WiFi event handler
	WiFi.onEvent(WiFiEvent);
	
	// Try to connect to WiFi
	connectWiFi();

	Serial.println("");
	Serial.print("Connected to ");
	Serial.println(ssid);
	Serial.print("IP address: ");
	Serial.println(WiFi.localIP());

	// Start the web server to serve incoming requests
	server.begin();

	My_Sender.begin();

	Serial.println(F("00 - On/Off"));
	Serial.println(F("11 - Cool"));
	Serial.println(F("12 - Dry"));
	Serial.println(F("13 - Fan"));
	Serial.println(F("23 - Fan Speed"));
	Serial.println(F("30 - Plus"));
	Serial.println(F("31 - Minus"));
	Serial.println(F("40 - Timer"));
	Serial.println(F("41 - Sweep"));
	Serial.println(F("42 - Turbo"));
	Serial.println(F("43 - Ion"));
	Serial.println(F("98 - Auto function on"));
	Serial.println(F("99 - Auto function off"));


	// Initialize file system.
	boolean foundStatus = SPIFFS.begin();
	if (foundStatus) { // File system found
		// Try to get last status from status.txt
		if (!readStatus()) {
			foundStatus = false;
		}
	}
	if (!foundStatus) // Could not get last status
	{
		/* Asume aircon off, timer off, power control enabled, */
		/* aircon mode fan, fan speed low, temperature set to 25 deg Celsius */
		acMode = acMode | AUTO_ON | TIM_OFF | AC_OFF | MODE_FAN | FAN_LOW | TUR_OFF | SWP_OFF | ION_OFF;
		acTemp = acTemp & TEMP_CLR; // set temperature bits to 0
		acTemp = acTemp + 25; // set temperature bits to 25
	}

	if ((acMode & AUTO_OFF) == AUTO_OFF) {
		powerStatus = 0;
		writeStatus();
	}
	
	// Get first values from spMonitor
	//Only used in main control ESP on 192.168.0.142 address
	//getPowerVal(false);

	// Start update of consumption value every 60 seconds if enabled
	//	getPowerTimer.attach(60, triggerGetPower);

	// Start sending status update every 1 minutes (1x60=60 seconds)
	// TODO testing to send only if status changed or power value changed by more than 10 Watt
	sendUpdateTimer.attach(60, triggerSendUpdate);
	Serial.println("");
	Serial.print("Current time: ");
	Serial.println(getTime());
	// Send aircon restart message
	sendBroadCast();
	inSetup = false;
	
	// Setup NTP time updates
	Udp.begin(localPort);
	setSyncProvider(getNtpTime);

	// Start FTP server
	ftpSrv.begin(DEVICE_ID,DEVICE_ID);    //username, password for ftp.  set ports in ESP8266FtpServer.h  (default 21, 50009 for PASV)
		
	ArduinoOTA.onStart([]() {
		Serial.println("OTA start");
		ledFlasher.attach(0.1, redLedFlash); // Flash very fast if we started update
		resetFanModeTimer.detach();
		sendUpdateTimer.detach();
		WiFiUDP::stopAll();
		WiFiClient::stopAll();
		server.close();
	});

	// Start OTA server.
	ArduinoOTA.setHostname(DEVICE_ID);
	ArduinoOTA.begin();
}
