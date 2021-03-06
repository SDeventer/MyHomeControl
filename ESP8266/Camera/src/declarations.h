#include "Setup.h"

/** Build time */
const char compileDate[] = __DATE__ " " __TIME__;

/**********************************************
When doing breadboard test, enable this define
***********************************************/
//#define BREADBOARD

#ifdef BREADBOARD
	IPAddress ipAddr = ipSpare4;
#else
	IPAddress ipAddr = ipCam1;
#endif

/** WiFiServer class to create TCP socket server on port 6000 */
WiFiServer tcpServer(6000);
// /** FTPServer class to create simple ftp server */
FtpServer ftpSrv;
/** FTP client */
WiFiClient ftpClient;
/** External FTP server for data transfer*/
WiFiClient ftpDataClient;
/** External FTP server IP */
IPAddress ftpDataServer( 93, 104, 213, 79 );
/** External FTP server port */
uint16_t ftpDataPort = 21;

/** Buffer for received/sent data */
char ftpBuf[128];
/** Counter for sent/received data */
char ftpCount;

/** Flag for boot status */
boolean inSetup = true;

/** Flag for OTA update running */
boolean otaRunning = false;
/** Flag for TCP debugging */
bool debugOn = false;
/** Last time NTP sync was performed */
time_t lastSyncTime;

/** On ESP: camera TX connected to GPIO14, camera RX to GPIO12: */
SoftwareSerial cameraconnection = SoftwareSerial(14, 12);
/** Camera connection */
Adafruit_VC0706 cam = Adafruit_VC0706(&cameraconnection);
/** Flag if camera was found */
boolean hasCamera = true;

/** Flashlight LED output */
int flashLED = 5;
/** Blinking LED output */
int blinkLED = 4;
