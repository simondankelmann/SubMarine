// BLUETOOTH
#include "Arduino.h"
#include "BluetoothSerial.h"
BluetoothSerial SerialBT;

// INCOMING BLUETOOTH DATA
#define INCOMING_BLUETOOTH_SIGNAL_BUFFER 4096
int incomingBluetoothSignal[INCOMING_BLUETOOTH_SIGNAL_BUFFER];
bool receivingBluetoothData = false;
int receivingPosition = 0;
int incomingTransmissionStart = 0;
int incomingTransmissionEnd = 0;
String incomingSample = "";
int incomingBluetoothSignalPosition = 0;

// RECORDED SAMPLES BUFFER
#define MAX_LENGHT_RECORDED_SIGNAL 4096
int recordedSignal[MAX_LENGHT_RECORDED_SIGNAL];
int recordedSamples = 0;

// CC1101
#include <ELECHOUSE_CC1101_SRC_DRV.h>
#define PIN_GDO0 12
#define PIN_GDO2 4

// CC1101 MODULE SETTINGS
float CC1101_MHZ = 433.92;
bool CC1101_TX = false;
int CC1101_MODULATION = 2;
int CC1101_DRATE = 512;
float CC1101_RX_BW = 256;
int CC1101_PKT_FORMAT = 3;

// LED
#define PIN_LED_ONBOARD 2

// BUTTONS
#define THRESHOLD_BTN_CLICK 4000
#define PIN_BTN_1 32
#define PIN_BTN_2 25
#define PIN_BTN_3 27
#define PIN_BTN_4 14

void setup() {
  Serial.begin(1000000);
  //BLUETOOTH SETUP
  initBluetooth();
  //CC1101 SETUP
  initCC1101();
  // PIN MODES
  pinMode(PIN_LED_ONBOARD, OUTPUT);
}

void initBluetooth(){
  SerialBT.begin("Sub Marine"); //Bluetooth device name
  SerialBT.register_callback(btCallback);
}

void initCC1101(){
  ELECHOUSE_cc1101.Init();
  ELECHOUSE_cc1101.setGDO(PIN_GDO0, PIN_GDO2);
  ELECHOUSE_cc1101.setMHZ(CC1101_MHZ);                    // Here you can set your basic frequency. The lib calculates the frequency automatically (default = 433.92).The cc1101 can: 300-348 MHZ, 387-464MHZ and 779-928MHZ. Read More info from datasheet.
  if(CC1101_TX){
    ELECHOUSE_cc1101.SetTx();                             // set Transmit on
    pinMode(PIN_GDO0,OUTPUT);  
  } else {
    ELECHOUSE_cc1101.SetRx();                             // set Receive on
    pinMode(PIN_GDO0,INPUT);  
  }
  
  ELECHOUSE_cc1101.setModulation(CC1101_MODULATION);      // set modulation mode. 0 = 2-FSK, 1 = GFSK, 2 = ASK/OOK, 3 = 4-FSK, 4 = MSK.
  ELECHOUSE_cc1101.setDRate(CC1101_DRATE);                // Set the Data Rate in kBaud. Value from 0.02 to 1621.83. Default is 99.97 kBaud!
  ELECHOUSE_cc1101.setRxBW(CC1101_RX_BW);                 // Set the Receive Bandwidth in kHz. Value from 58.03 to 812.50. Default is 812.50 kHz.
  ELECHOUSE_cc1101.setPktFormat(CC1101_PKT_FORMAT);       // Format of RX and TX data. 0 = Normal mode, use FIFOs for RX and TX. 
                                                          // 1 = Synchronous serial mode, Data in on GDO0 and data out on either of the GDOx pins. 
                                                          // 2 = Random TX mode; sends random data using PN9 generator. Used for test. Works as normal mode, setting 0 (00), in RX. 
                                                          // 3 = Asynchronous serial mode, Data in on GDO0 and data out on either of the GDOx pins.
  // Check the CC1101 Spi connection.
  if(!ELECHOUSE_cc1101.getCC1101()){       
    Serial.println("CC1101 Connection Error");
  } else {
    Serial.println("CC1101 Connection OK");
  }
}

void btCallback(esp_spp_cb_event_t event, esp_spp_cb_param_t *param){
  if(event == ESP_SPP_SRV_OPEN_EVT){
    Serial.println("Bluetooth Client Connected!");
    delay(100);
  } else if(event == ESP_SPP_DATA_IND_EVT){
    if(receivingBluetoothData == false){
      // CLEAR ANY PREVIOUS RESULT
      memset(incomingBluetoothSignal,0,INCOMING_BLUETOOTH_SIGNAL_BUFFER*sizeof(int));
      incomingSample = "";
      incomingBluetoothSignalPosition = 0;
      incomingTransmissionStart = millis();      
    }

    receivingBluetoothData = true;
    int readBytes = 0;
   
    while(SerialBT.available()){ 
      int incoming = SerialBT.read();
      char incomingChar = incoming; 
      readBytes++;
      receivingPosition++;

      if(incomingChar == '\n'){
        receivingBluetoothData = false;
        incomingTransmissionEnd = millis();
        addSampleToBluetoothSignal(incomingSample);
      } else if(incomingChar == ',') {
        addSampleToBluetoothSignal(incomingSample);
      } else {
        incomingSample += incomingChar;
      }
    }

    if(receivingBluetoothData == false){
        Serial.print("Stopped after receiving ");
        Serial.print(receivingPosition);
        Serial.println(" Bytes");
        Serial.print("in ");
        Serial.print(incomingTransmissionEnd - incomingTransmissionStart);
        Serial.println(" Milliseconds");

        // RESET
        receivingPosition = 0;
        receivingBluetoothData = false;
        onReceivedCallback();      
    }
  }
}

void addSampleToBluetoothSignal(String sample){
  int iSample = sample.toInt();
  incomingBluetoothSignal[incomingBluetoothSignalPosition] = iSample;
  //Serial.println("Adding: " + String(iSample) + " on Position: " + String(currentBluetoothSignalPosition));
  incomingBluetoothSignalPosition ++;
  incomingSample = "";
}


void onReceivedCallback(){
  Serial.println("Receive Success Callback");
  sendSamples(incomingBluetoothSignal, incomingBluetoothSignalPosition, 433.92);
}

void sendSignalToBluetooth(int samples[], int samplesLength){
  if(SerialBT.hasClient()){
      Serial.println("STARTING TO SEND DATA VIA BLUETOOTH");
      int before = millis();  
      for (int i = 0; i < samplesLength; i++) {
        String currentValue = String(samples[i]);
        for (int c=0;c<strlen(currentValue.c_str());c++) {
          SerialBT.write(currentValue[c]);
        }
        if(i == (samplesLength - 1)){
          SerialBT.write('\n');
          break;
        } else {
          SerialBT.write(',');
        }
      }
      SerialBT.flush();

      int after = millis();

      Serial.print("Transmission ended after: ");
      Serial.print(after - before);
      Serial.println(" Milliseconds");
  }
}

void loop() {

  // READ BTN STATES
  int state_btn_1 = analogRead(PIN_BTN_1);
  int state_btn_2 = analogRead(PIN_BTN_2);

  // BUTTON 1  
  if(state_btn_1 >= THRESHOLD_BTN_CLICK){
    while(analogRead(PIN_BTN_1) >= THRESHOLD_BTN_CLICK) {
      delay(10);
    }
    //BTN 1 CLICKED
    CC1101_TX = false;
    initCC1101();
    recordSignal();
  }

  // BUTTON 2
  if(state_btn_2 >= THRESHOLD_BTN_CLICK){
    while(analogRead(PIN_BTN_2) >= THRESHOLD_BTN_CLICK) {
      delay(10);
    }
    //BTN 2 CLICKED
    replaySignal();
  }
}

void recordSignal(){
  digitalWrite(PIN_LED_ONBOARD, HIGH);  
  copy();
  dump();
  //SEND TO APP
  sendSignalToBluetooth(recordedSignal, recordedSamples);
  digitalWrite(PIN_LED_ONBOARD, LOW);  
}

void replaySignal(){
  sendSamples(recordedSignal, MAX_LENGHT_RECORDED_SIGNAL, 433.92);
  dump();
}

void sendSamples(int samples[], int samplesLenght, float mhz) {
  CC1101_TX = true;
  initCC1101();
  Serial.println("Transmitting " + String(samplesLenght) + " Samples");

  int delay = 0;
  unsigned long time;
  byte n = 0;

  for (int i=0; i < samplesLenght; i++) {
    // TRANSMIT
    n = 1;
    delay = samples[i];
    if(delay < 0){
      // DONT TRANSMIT
      delay = delay * -1;
      n = 0;
    }

    digitalWrite(PIN_GDO0,n);
    delayMicroseconds(delay);
  }

  // STOP TRANSMITTING
  digitalWrite(PIN_GDO0,0);

  Serial.println("Transmission completed.");
}

// ------------------------- COPY REPLAY STUFF --> REFACTOR THIS !!!
#define LOOPDELAY 20
#define HIBERNATEMS 30*1000
//#define BUFSIZE MAX_LENGHT_RECORDED_SIGNAL
#define REPLAYDELAY 0
// THESE VALUES WERE FOUND PRAGMATICALLY
#define RESET443 32000 //32ms
#define WAITFORSIGNAL 32 // 32 RESET CYCLES
#define MINIMUM_TRANSITIONS 32
#define MINIMUM_COPYTIME_US 16000
#define DUMP_RAW_MBPS 0.1 // as percentage of 1Mbps, us precision. (100kbps) This is mainly to dump and analyse in, ex, PulseView
#define BOUND_SAMPLES true
int delayus = REPLAYDELAY;
long lastCopyTime = 0;

void copy() {
  int i, transitions = 0;
  lastCopyTime = 0;
  //FILTER OUT NOISE SIGNALS (too few transistions or too fast)
  while (transitions < MINIMUM_TRANSITIONS && lastCopyTime < MINIMUM_COPYTIME_US) {
    transitions = trycopy();
  }
  //CLEAN LAST ELEMENTS
  for (i=transitions-1;i>0;i--) {
    if (recordedSignal[i] == RESET443) recordedSignal[i] = 0;
    else break;
  }
  if (BOUND_SAMPLES) {
    recordedSignal[0] = 200;
    if (i < MAX_LENGHT_RECORDED_SIGNAL) recordedSignal[i+1] = 200;
  }
}

int trycopy() {
  int i;
  Serial.println("Copying...");

  // CLEAR ANY PREVIOUSLY RECORDED SIGNAL
  memset(recordedSignal,0,MAX_LENGHT_RECORDED_SIGNAL*sizeof(int));
  byte n = 0;
  int sign = -1;

  int64_t startus = esp_timer_get_time();
  int64_t startread;
  int64_t dif = 0;
  int64_t ttime = 0;
  for (i = 0; i < MAX_LENGHT_RECORDED_SIGNAL; i++) {
    startread = esp_timer_get_time();
    dif = 0;
    //WAIT FOR INIT
    while (dif < RESET443) {
      dif = esp_timer_get_time() - startread;
      if (CCAvgRead() != n) {
        break;
      }
    }
    if (dif >= RESET443) {
      recordedSignal[i] = RESET443 * sign;
      //if not started wait...
      if (i == 0) {
        i = -1;
        ttime++;
        if (ttime > WAITFORSIGNAL) {
          Serial.println("No signal detected!");
          return -1;
        }
      }
      else {
        ttime++;
        if (ttime > WAITFORSIGNAL) {
          Serial.println("End of signal detected!");
          break;
        }
      }
    }
    else {
      recordedSignal[i] = dif * sign;
      n = !n;
      if(n) {
        sign = 1;
      } else {
        sign = -1;
      }
    }
  }
  
  int64_t stopus = esp_timer_get_time();
  Serial.print("Copy took (us): ");
  lastCopyTime = (long)(stopus - startus);
  Serial.println(lastCopyTime , DEC);
  Serial.print("Transitions: ");
  Serial.println(i);
  //memcpy(signal433_current,newsignal433,BUFSIZE*sizeof(uint16_t));
  recordedSamples = i;
  return i;
}

#define BAVGSIZE 11
byte bavg[BAVGSIZE];
byte pb = 0;
byte cres = 0;

byte CCAvgRead() {
  cres -= bavg[pb];
  bavg[pb] = digitalRead(PIN_GDO0);
  cres += bavg[pb];
  pb++;
  if (pb >= BAVGSIZE) pb = 0;
  if (cres > BAVGSIZE/2) return 1;
  return 0;
}

void dump(){
  int i;
  int n = 0;
  Serial.println("Dump transition times: ");
  for (i = 0; i < recordedSamples; i++) {
    if (i > 0) Serial.print(",");
    Serial.print(recordedSignal[i]);
  }  
  Serial.println("");
}
