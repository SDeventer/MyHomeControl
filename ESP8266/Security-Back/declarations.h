/* Includes from libraties */
#include <ArduinoOTA.h>
#include <ESP8266WiFi.h>
#include <WiFiUdp.h>
#include <WiFiClient.h>
#include <Ticker.h>
#include <Wire.h>
#include <Adafruit_Sensor_ESP.h>
#include <Adafruit_TSL2561_U_ESP.h>
#include <pgmspace.h>
#include <ArduinoJson.h>
#include <FS.h>
#include <TimeLib.h> 
#include <ESP8266FtpServer.h>
#include <DHT.h>

/** Build time */
const char compileDate[] = __DATE__ " " __TIME__;

/* wifiAPinfo.h contains wifi SSID and password */
#include "wifiAPinfo.h"

/** Red LED on GPIO0 for visual signal if alarm is on or off */
#define alarmLED 0
/** Blue LED on GPIO2 for communication activities */
#define comLED 2
/** Input from PIR sensor */
#define pirPort 4
/** Output to activate Relay */
#define relayPort 5
/** Data pin for I2C communication with light sensor */
#define sdaPin 13
/** Clock pin for I2C communication with light sensor */
#define sclPin 12
/** Output to loudspeaker or piezo */
#define speakerPin 15
/** Definition of data pin for DHT sensor */
#define DHTPIN  14

/**********************************************
When doing breadboard test, enable this define
***********************************************/
//#define BREADBOARD

#ifdef BREADBOARD
	#define DEVICE_ID "sbb" // ID for security in front yard
	#define OTA_HOST "secbb" // Host name for OTA updates
#else
	#define DEVICE_ID "sb1" // ID for security in front yard
	#define OTA_HOST "secb" // Host name for OTA updates
#endif

/** WiFiUDP class for creating UDP communication */
WiFiUDP udpClientServer;

/** WiFiServer class to create simple web server */
WiFiServer server(80);

/** WiFiServer class to create TCP socket server on port 6000 */
WiFiServer tcpServer(6000);

/** FTPServer class to create simple ftp server */
FtpServer  ftpSrv;

/** IP address of this module */
#ifdef BREADBOARD
	IPAddress ipAddr IPSPARE4;
#else
	IPAddress ipAddr IPSECB;
#endif
/** MAC address of this module = unique id on the LAN */
String localMac = "";

/** Timer for flashing red detection LED */
Ticker ledFlasher;
/** Timer for flashing blue communication LED */
Ticker comFlasher;
/** Timer to switch off the relay */
Ticker relayOffTimer;
/** Timer to collect light information from TSL2561 sensor */
Ticker getWeatherTimer;
/** Timer for alarm siren */
Ticker alarmTimer;
/** Timer for heart beat */
Ticker heartBeatTimer;
/** Timer to collect temperature & humidity information */
Ticker getDHTTimer;

/** Flag for alarm activity */
boolean alarmOn = true;

/** Flag if heart beat was triggered */
boolean heartBeatTriggered = false;
/** Flag if WiFi connection is ongoing */
boolean wifiConnecting = false;
/** Flag if panic button was pressed */
boolean panicOn = false;

/** Melody as delay time */
//long melody[] = {1700, 1700, 1136, 1136, 1432, 1915, 1915, 1700 ,1700 ,1136 ,1136 ,1700 ,1700 ,1915 ,1915 ,1432 ,1432 ,1700 ,1700 ,1136 ,1136 ,1915 ,1915 ,1700 ,1700 ,1136 ,1136 ,1432 ,1915 ,1915 ,1700 ,1700 ,1136 ,1136 ,1136 ,1136 ,1275 ,1275 ,1275 ,1275};
/** Number of melody[] notes */
//int melodyLenght = 40;
// Bido Bido sound
//long melody[] = {1915, 1915, 1915, 1915, 1275, 1275, 1275, 1275, 1915, 1915, 1915, 1915, 1275, 1275, 1275, 1275, 1915, 1915, 1915, 1915, 1275, 1275, 1275, 1275, 1915, 1915, 1915, 1915, 1275, 1275, 1275, 1275, 1915, 1915, 1915, 1915, 1275, 1275, 1275, 1275};
/** Number of melody[] notes */
//int melodyLenght = 40;
// Martinshorn
long melody[] = {1136, 1136, 850, 850, 850, 850, 1136, 1136 ,850 ,850 ,850 ,850 ,1136 ,1136 ,850 ,850 ,850 ,850 ,1136 ,1136 ,850 ,850 ,850 ,850 ,1136 ,1136 ,850 ,850 ,850 ,850 ,1136 ,1136 ,850 ,850 ,850 ,850 ,1136 ,1136 ,850 ,850 ,850 ,850};
/** Number of melody[] notes */
int melodyLenght = 42;

/** Relation between values and notes */
//	1915	1700	1519	1432	1275	1136	1014	956
//	c		d		e		f		g		a		b		c

/** Melody position pointer */
int melodyPoint = 0;
/** Time to play a single tune in milliseconds */
int melodyTuneTime = 175;

/** Relay on delay time in seconds */
int onTime = 120;
/** Counter for relay switch off timing */
long offDelay = 0;
/** Flag if lights should be switched on after movement detection */
boolean switchLights = false;
/** Flag for PIR status change */
boolean pirTriggered = false;
/** Flag for request to read out light sensor */
boolean weatherUpdateTriggered = false;
/** Flag for detection status */
boolean hasDetection = false;
/** Flag for light switched off */
boolean lightOffTriggered = false;
/** Flag for request to read out temperature & humidity information */
boolean dhtUpdated = false;

/** Flag for boot status */
boolean inSetup = true;
/** String with reboot reason */
String rebootReason = "unknown";
/** String with last known reboot reason */
String lastRebootReason = "unknown";

/** Instance of the Adafruit TSL2561 sensor */
//Adafruit_TSL2561_Unified tsl = Adafruit_TSL2561_Unified ( TSL2561_ADDR_FLOAT, 1 );
/** Integration time for light sensor, 0 = 13.7ms, 1 = 101ms, 2 = 402ms */
//int lightInteg = 2;
/** Result of last light measurement */
long lightValue = 0;

/** Definition of DHT sensor type */
#define DHTTYPE DHT11
/** Instance of the DHT sensor */
DHT dht(DHTPIN, DHTTYPE, 11); // 11 works fine for ESP8266
/** Result of last temperature measurement */
float tempValue = 0.0;
/** Result of last humidity measurement */
float humidValue = 0.0;
/** Result of last heat index calculation */
float heatIndex = 0.0;

/** Counter for "I am alive" led blink */
int liveCnt = 0;

/** Flag for OTA update */
bool otaRunning = false;

// NTP Servers stuff
/** URL of NTP server */
const char* timeServerURL = "time.nist.gov";

/** Definition of timezone */
const int timeZone = 8;     // Philippine time (GMT + 8h)

/** Port for connection to NTP server */
unsigned int localPort = 8888;  // local port to listen for UDP packets

/** Size of NTP time server packet */
const int NTP_PACKET_SIZE = 48; // NTP time is in the first 48 bytes of message
/** Buffer for data from NTP server */
byte packetBuffer[NTP_PACKET_SIZE]; //buffer to hold incoming & outgoing packets

/** Flag for automatic activation/deactivation of alarm */
boolean hasAutoActivation = false;
/** Hour for automatic activation of alarm (24h format) */
int autoActivOn = 22;
/** Hour for automatic deactivation of alarm (24h format) */
int autoActivOff = 8;
