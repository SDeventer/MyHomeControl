byte H_FAN[4] = {B00001000,B11100111,B01101000,B10010111};
byte M_FAN[4] = {B00001000,B11100111,B01110000,B10001111};
byte L_FAN[4] = {B00001000,B11100111,B01010000,B10101111};

byte COOL[4] =	{B00001000,B11100111,B00101000,B11010111};
byte DRY[4] =	{B00001000,B11100111,B00110000,B11001111};
byte FAN[4] =	{B00001000,B11100111,B00010000,B11101111};
byte POWER[4] = {B00001000,B11100111,B00000000,B11111111};

byte PLUS[4] =	{B00001000,B11100111,B10101000,B01010111};
byte MINUS[4] =	{B00001000,B11100111,B10010000,B01101111};

byte TIMER[4] = {B00001000,B11100111,B10000000,B01111111};

unsigned int sendBuffer[67] = {9000,4500,450,700,450,700,450,700,450,700,450,1800,450,700,450,700,450,700,450,1800,450,1800,450,1800,450,700,450,700,450,1800,450,1800,450,1800,450,0,450,0,450,0,450,0,450,0,450,700,450,700,450,700,450,0,450,0,450,0,450,0,450,0,450,1800,450,1800,450,1800,450};
