// SPIFFS
#include "SPIFFS.h"
#define SPIFFS_FILENAME_RECORDED_SIGNAL "/recordedSignal.txt"
#define SPIFFS_FILENAME_INCOMING_COMMAND "/incomingCommand.txt"
bool spiffsMounted = false;

// BLUETOOTH
#include "Arduino.h"
#include "BluetoothSerial.h"
BluetoothSerial SerialBT;

//INCOMING BLUETOOTH COMMAND
#define CHAR_COMMAND_EOL '\n'
#define INCOMING_BLUETOOTH_COMMAND_HEADER_LENGTH 8
int incomingCommandLength = 0;
bool receivingBluetoothCommand = false;
int incomingCommandStartTime = 0;
int incomingCommandEndTime = 0;
#define INCOMING_REPEATIONS_STRING_LENGTH 4
#define INCOMING_REPEATIONS_DELAY_STRING_LENGTH 6

// CONNECTION STATES
#define CONNECTION_STATE_DISCONNECTED "0000" 
#define CONNECTION_STATE_CONNECTING "0001" 
#define CONNECTION_STATE_CONNECTED "0002" 

// COMMANDS
#define COMMAND_REPLAY_SIGNAL_FROM_BLUETOOTH_COMMAND "0001" 
#define COMMAND_SET_OPERATION_MODE "0002" 
#define COMMAND_SEND_SIGNAL "0003"
#define COMMAND_SET_ADAPTER_CONFIGURATION "0004"
#define COMMAND_GET_ADAPTER_CONFIGURATION "0005"
#define COMMAND_DETECTED_FREQUENCY "0006"
#define COMMAND_UPDATE_CONNECTION_STATUS "0007" 

String incomingCommand = "";
String incomingCommandId = "";
String incomingCommandDataString = "";

// COMMAND IDS
#define COMMAND_ID_DUMMY "0000"

// SIGNAL FROM BLUETOOTH COMMAND
#define INCOMING_BLUETOOTH_SIGNAL_BUFFER_SIZE 4096
int incomingBluetoothSignal[INCOMING_BLUETOOTH_SIGNAL_BUFFER_SIZE];

// OPERATION MODES
#define OPERATIONMODE_IDLE "0000" 
#define OPERATIONMODE_HANDLE_INCOMING_COMMAND "0001" 
#define OPERATIONMODE_PERISCOPE "0002"
#define OPERATIONMODE_RECORD_SIGNAL "0003"
#define OPERATIONMODE_DETECT_SIGNAL "0004"
String _lastExecutedOperationMode = "0000"; 
String _operationMode = "0000";

// RECORDING SINGAL PARAMERS
#define MAX_EMPTY_RECORDING_CYCLES 32 // 32 RESET CYCLES
#define MINIMUM_RECORDED_TRANSITIONS 32
#define MINIMUM_RECORDTIME_MICROSECONDS 16000
#define MAX_LENGHT_RECORDED_SIGNAL 4096
#define MAX_TRANSITION_TIME_MICROSECONDS 32000
int recordedSignal[MAX_LENGHT_RECORDED_SIGNAL];
int recordedSamples = 0;
long lastRecordDuration = 0;

// SINGAL DETECTION
#define SIGNAL_DETECTION_FREQUENCIES_LENGTH 16
float signalDetectionFrequencies[SIGNAL_DETECTION_FREQUENCIES_LENGTH] = {300.00, 303.87, 304.25, 310.00, 315.00, 318.00, 390.00, 418.00, 433.07, 433.92, 434.42, 434.77, 438.90, 868.35, 915.00, 925.00};
int detectedRssi = -100;
float detectedFrequency = 0.0;
int signalDetectionMinRssi = -65;

// CC1101
#include <ELECHOUSE_CC1101_SRC_DRV.h>
#define PIN_GDO0 12
#define PIN_GDO2 4

// CC1101 MODULE SETTINGS
#define CC1101_ADAPTER_CONGIRURATION_LENGTH 30
 /*
      Adapter Configuration Structure:
      Bytes:
      0-5 => MHZ
      6 => TX
      7 => MODULATION
      8-10 => DRATE
      11-16 => RX_BW
      17 => PKT_FORMAT
      18-23 => AVG_LQI
      24-29 => AVG_RSSI
*/
float CC1101_MHZ = 433.92;
bool CC1101_TX = false;
int CC1101_MODULATION = 2;
int CC1101_DRATE = 512;
float CC1101_RX_BW = 256;
int CC1101_PKT_FORMAT = 3;
float CC1101_LAST_AVG_LQI = 0;
float CC1101_LAST_AVG_RSSI = 0;

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
  // INIT SPIFFS
  initSpiffs();
  // PIN MODES
  pinMode(PIN_LED_ONBOARD, OUTPUT);
}

void initBluetooth(){
  SerialBT.begin("Sub Marine"); //Bluetooth device name
  SerialBT.register_callback(btCallback);
}

void initSpiffs(){
  if(SPIFFS.begin(true)){
    Serial.println("SPIFFS mounted successfully");
    spiffsMounted = true;
  } else {
    Serial.println("An Error has occurred while mounting SPIFFS");  
    spiffsMounted = false;  
  }
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
    //Serial.println("CC1101 Connection OK");
  }
}


void btCallback(esp_spp_cb_event_t event, esp_spp_cb_param_t *param){
  if(event == ESP_SPP_SRV_OPEN_EVT){
    Serial.println("Bluetooth Client Connected!");
    // INFORM ABOUT CONNECTION SUCCESS
    blinkLed(1);
    //delay(100);
    //sendCommand(COMMAND_UPDATE_CONNECTION_STATUS,COMMAND_ID_DUMMY,CONNECTION_STATE_CONNECTED);
    //delay(1000);
  } else if(event == ESP_SPP_DATA_IND_EVT){
    // RECEIVE INCOMING BLUETOOTH COMMAND
    if(receivingBluetoothCommand == false){
      // CLEAR ANY PREVIOUS RESULT
      //memset(incomingBluetoothCommand,0,INCOMING_BLUETOOTH_COMMAND_BUFFER_SIZE*sizeof(int));
      incomingCommandStartTime = millis();      
      //incomingBluetoothCommandByteIndex = 0;
      incomingCommandLength = 0;
      // CLEAR PREVIOUS COMMAND FILE
      SPIFFS.remove(SPIFFS_FILENAME_INCOMING_COMMAND);
      File file = SPIFFS.open(SPIFFS_FILENAME_INCOMING_COMMAND, FILE_WRITE);
      file.close();
    }    
    receivingBluetoothCommand = true;
    File file = SPIFFS.open(SPIFFS_FILENAME_INCOMING_COMMAND, FILE_APPEND);

    while(SerialBT.available()){ 
      int incoming = SerialBT.read();
      char incomingChar = incoming; 

      if(incomingChar == '\n'){
        // TRANSMISSION COMPLETED
        receivingBluetoothCommand = false;
        incomingCommandEndTime = millis();
        file.print(incomingChar);
      } else {
        // ADD BYTE TO COMMAND BUFFER
        incomingCommandLength++;
        file.print(incomingChar);
      }
    }

    //CLOSE THE FILE
    file.close();  

    // LOG INFORMATION, CALL CALLBACK     
    if(receivingBluetoothCommand == false){
        Serial.print("Stopped after receiving ");
        Serial.print(incomingCommandLength);
        Serial.print(" Bytes ");
        Serial.print("in ");
        Serial.print(incomingCommandEndTime - incomingCommandStartTime);
        Serial.println(" Milliseconds");

        // CALLBACK
        commandReceivedCallback();      
    }
  }
}

void commandReceivedCallback(){
   /*
      Command Structure:
      Bytes:
      0-3 => Command
      4-7 => CommandId
      Remaining Bytes => Data      
  */

  String parsedCommand = "";
  String parsedCommandId = "";
  String parsedDataString = "";

  int fileIndex = 0;  
  File file = SPIFFS.open(SPIFFS_FILENAME_INCOMING_COMMAND, FILE_READ);
  while (file.available()) {
    char c = file.read();

    if(fileIndex <= 3){
      parsedCommand += c;
    }

    if(fileIndex > 3 && fileIndex <= 7){
      parsedCommandId += c;
    }

    if(fileIndex > 7 && c != '\n'){
      parsedDataString += c;
    }
    
    fileIndex++;
  }
  file.close();

  Serial.print("Parsed Command: " + parsedCommand);
  Serial.print(", Command ID: " + parsedCommandId);
  Serial.println(", Parsed Data: " + parsedDataString);

  // SET THE CURRENT COMMAND HEADER INFORMATION FOR MAIN THREAD
  incomingCommand = parsedCommand;
  incomingCommandId = parsedCommand;
  incomingCommandDataString = parsedDataString;

  setOperationMode(OPERATIONMODE_HANDLE_INCOMING_COMMAND); 
}

void setOperationMode(String opMode){
  _operationMode = opMode;
  Serial.println("Setting Operation Mode: " + opMode);
}

void goIdle(){
  _operationMode = OPERATIONMODE_IDLE;
}

String getOperationMode(){
  return _operationMode;
}

String getLastExecutedOperationMode(){
  return _lastExecutedOperationMode;
}

String getCC1101Configuration(){
   /*
      Adapter Configuration Structure:
      Bytes:
      0-5 => MHZ
      6 => TX
      7 => MODULATION
      8-10 => DRATE
      11-16 => RX_BW
      17 => PKT_FORMAT
      18-23 => AVG_LQI
      24-29 => AVG_RSSI
  */
  String configurationString = "";
  // MHZ
  String mhz = String(CC1101_MHZ, 2);
  while(mhz.length() < 6){
    mhz = "0" + mhz;
  }
  configurationString += mhz;
  // TX
  if(CC1101_TX){
    configurationString += "1";
  } else {
    configurationString += "0";
  }
  // MODULATION
  configurationString += String(CC1101_MODULATION).substring(0,1);
  //DRATE
  String dRate = String(CC1101_DRATE);
  while(dRate.length() < 3){
    dRate = "0" + dRate;
  }
  configurationString += String(CC1101_DRATE);
  //RX_BW
  String rxBw = String(CC1101_RX_BW, 2);
  while(rxBw.length() < 6){
    rxBw = "0" + rxBw;
  }
  configurationString += rxBw;
  // PKT_FORMAT
  configurationString += String(CC1101_PKT_FORMAT).substring(0,1);

  String lqi = String(CC1101_LAST_AVG_LQI, 2);
  while(lqi.length() < 6){
    lqi = "0" + lqi;
  }
  configurationString += lqi;

  String rssi = String(CC1101_LAST_AVG_RSSI, 2);
  while(rssi.length() < 6){
    rssi = "0" + rssi;
  }
  configurationString += rssi;

  return configurationString;
}

void setCC1101Configuration(String configurationString){
   /*
      Adapter Configuration Structure:
      Bytes:
      0-5 => MHZ
      6 => TX
      7 => MODULATION
      8-10 => DRATE
      11-16 => RX_BW
      17 => PKT_FORMAT
      18-23 => AVG_LQI
      24-29 => AVG_RSSI
  */

  int position = 0;

  String mhz = "";
  bool tx;
  int modulation;
  String dRate = "";
  String rxBw = "";
  int pktFormat;
  String lqi = "";
  String rssi = "";

  for(auto x : configurationString)
  {
    if(position <= 5){
      mhz += x;      
    }

    if(position == 6){
      tx = String(x).toInt() > 0 ? true : false;
    }

    if(position == 7){
      modulation = String(x).toInt();
    }

    if(position > 7 && position <= 10){
      dRate += x;
    }

    if(position > 10 && position <= 16){
      rxBw += x;
    }

    if(position == 17){
      pktFormat = String(x).toInt();
    }

    if(position > 17 && position <= 23){
      lqi += x;
    }

    if(position > 23 && position <= 29){
      rssi += x;
    }

    position++;
  }

  /*
  Serial.println("DETECTED FROM STRING: ");
  Serial.print("MHZ: ");
  Serial.println(mhz.toFloat());
  Serial.print("TX: ");
  Serial.println(tx);
  Serial.print("MODULATION: ");
  Serial.println(modulation);
  Serial.print("DRATE: ");
  Serial.println(dRate.toInt());
  Serial.print("RX BW: ");
  Serial.println(rxBw.toFloat());
  Serial.print("PKT FORMAT: ");
  Serial.println(pktFormat);
  Serial.print("LQI: ");
  Serial.println(lqi.toFloat());
  Serial.print("RSSI: ");
  Serial.println(rssi);*/
  
  // SET VALUES:
  CC1101_MHZ = mhz.toFloat();
  CC1101_TX = tx;
  CC1101_MODULATION = modulation;
  CC1101_DRATE = dRate.toInt();
  CC1101_RX_BW = rxBw.toFloat();
  CC1101_PKT_FORMAT = pktFormat;
  CC1101_LAST_AVG_LQI = lqi.toFloat();
  CC1101_LAST_AVG_RSSI = rssi.toFloat();

  // RELOAD ADAPTER
  initCC1101();
}

void replaySignalFromIncomingCommand(){
  Serial.println("Replaying Signal from incoming Command");

  int fileIndex = 0;
  int parsedSamples = 0;
  String currentSample = "";

  String CC1101_ConfigurationString = "";

  String repeatitionsString = ""; // LENGTH 4
  String repeatitionDelayString = ""; // LENGTH 6

  //#define INCOMING_REPEATIONS_STRING_LENGTH 4
  //#define INCOMING_REPEATIONS_DELAY_STRING_LENGTH 6

  File file = SPIFFS.open(SPIFFS_FILENAME_INCOMING_COMMAND, FILE_READ);
  while (file.available()) {
    char c = file.read();

    if(fileIndex >= INCOMING_BLUETOOTH_COMMAND_HEADER_LENGTH && fileIndex < INCOMING_BLUETOOTH_COMMAND_HEADER_LENGTH + CC1101_ADAPTER_CONGIRURATION_LENGTH){
      CC1101_ConfigurationString += c;      
    }

    int repeatitionsStartIndex = INCOMING_BLUETOOTH_COMMAND_HEADER_LENGTH + CC1101_ADAPTER_CONGIRURATION_LENGTH;
    int repeatitionsEndIndex = repeatitionsStartIndex + INCOMING_REPEATIONS_STRING_LENGTH;
    if(fileIndex >= repeatitionsStartIndex && fileIndex < repeatitionsEndIndex){
      repeatitionsString += c;      
    }

    int repeatitionDelayStartIndex = repeatitionsEndIndex;
    int repeatitionDelayEndIndex = repeatitionDelayStartIndex + INCOMING_REPEATIONS_DELAY_STRING_LENGTH;
    if(fileIndex >= repeatitionDelayStartIndex && fileIndex < repeatitionDelayEndIndex){
      repeatitionDelayString += c;      
    }
    
    if(fileIndex >= repeatitionDelayEndIndex){
      // START PARSING THE SIGNAL FROM THE DATA PORTION OF THE COMMAND
      if(c == '\n' || c == ','){
        int sample = currentSample.toInt();
        //Serial.println("Adding Sample from File: " + String(sample));
        if(parsedSamples < INCOMING_BLUETOOTH_SIGNAL_BUFFER_SIZE){
          incomingBluetoothSignal[parsedSamples] = sample;
          currentSample = "";
          parsedSamples++;
        } else {
          Serial.println("Too many Samples");
        }
        
        if(c == '\n'){
          break;
        }
      } else {
        currentSample += c;
      }
    }

    fileIndex++;
  }
  file.close();

  int repeatitions = repeatitionsString.toInt();
  int repeatitionDelay = repeatitionDelayString.toInt();

  Serial.print("Repeatitions: ");
  Serial.print(repeatitions);
  Serial.print(" Repeatition Delay: ");
  Serial.println(repeatitionDelay);

  if(parsedSamples > 0){
    // SET CC1101 CONFIG
    setCC1101Configuration(CC1101_ConfigurationString);
    // REPLAY THE SIGNAL
    for (int i = 0; i < repeatitions; i++) {  
      sendSamples(incomingBluetoothSignal, parsedSamples); 
      delay(repeatitionDelay);     
    }
  }
}

void setAdapterConfigurationFromIncomingCommand(){
  Serial.println("Setting Adapter Configuration from incoming Bluetooth Command");
  
  int fileIndex = 0;
  String parsedConfigurationString = "";

  File file = SPIFFS.open(SPIFFS_FILENAME_INCOMING_COMMAND, FILE_READ);
  while (file.available()) {
    char c = file.read();
    
    if(fileIndex >= INCOMING_BLUETOOTH_COMMAND_HEADER_LENGTH){
      // START PARSING THE SIGNAL FROM THE DATA PORTION OF THE COMMAND
      if(c == '\n'){
        setCC1101Configuration(parsedConfigurationString);
        Serial.println("Parsed Configuration String from Command: " + parsedConfigurationString);
        break;
      } else {
        parsedConfigurationString += c;
      }
    }

    fileIndex++;
  }
  file.close();

  blinkLed(2);
}

void blinkLed(int repeatitions){
  for (int i = 0; i < repeatitions; i++) {  
    digitalWrite(PIN_LED_ONBOARD, HIGH);
    delay(250);
    digitalWrite(PIN_LED_ONBOARD, LOW);
    delay(250);    
  }
}

void sendCommand(String command, String commandId, String dataString){
    String commandString = command + commandId + dataString;
    Serial.println("Sending Command: " + command + " with ID: " + commandId + ", Data: " + dataString);

    // SEND IT WITH BT SERIAL
    if(SerialBT.hasClient()){
        int before = millis();  
        for(int i = 0; i < commandString.length(); i++){
          SerialBT.write(commandString[i]);      
        }
        SerialBT.write('\n');
        SerialBT.flush();
        int after = millis();

        Serial.print("Command sent after: ");
        Serial.print(after - before);
        Serial.println(" Milliseconds");
    }

}

void sendSignalFromFile(String fileName){
    // GET SIGNAL DATA
    String dataString = "";
    File file = SPIFFS.open(fileName, FILE_READ);   
    if(file){
        while(file.available()){
          char c = file.read();
          dataString += c;     
        }  
    }

    sendCommand(COMMAND_SEND_SIGNAL,COMMAND_ID_DUMMY, dataString);
}

void loop() {

  // HANDLE OPERATION MODE
  if(getOperationMode() == OPERATIONMODE_IDLE){
    delay(100);
  } else if(getOperationMode() == OPERATIONMODE_PERISCOPE){
    periscope();
  } else if(getOperationMode() == OPERATIONMODE_RECORD_SIGNAL){
    operationModeRecordSignal();
  } else if(getOperationMode() == OPERATIONMODE_DETECT_SIGNAL){
    operationModeDetectSignal();
  } else if(getOperationMode() == OPERATIONMODE_HANDLE_INCOMING_COMMAND){
    // HANDLE INCOMING COMMAND
    handleIncomingCommand();
  }

  // KEEP TRACK
  _lastExecutedOperationMode = _operationMode;  
}

void handleIncomingCommand(){

  if(incomingCommand == COMMAND_REPLAY_SIGNAL_FROM_BLUETOOTH_COMMAND){
    replaySignalFromIncomingCommand();
    goIdle();
  }

  if(incomingCommand == COMMAND_SET_OPERATION_MODE){
    if(incomingCommandDataString.length() >= 4){
      setOperationMode(incomingCommandDataString.substring(0,4));
    }
  }

  if(incomingCommand == COMMAND_SET_ADAPTER_CONFIGURATION){
    setAdapterConfigurationFromIncomingCommand();
    goIdle();
  }

  if(incomingCommand == COMMAND_GET_ADAPTER_CONFIGURATION){
    sendCommand(COMMAND_SET_ADAPTER_CONFIGURATION, "0000", getCC1101Configuration());    
    goIdle();
  }
}

void recordSignal(){
  int i, transitions = 0;
  lastRecordDuration = 0;
 
  // RECORD TO BUFFER AND THEN WRITE TO SPIFFS
  while(transitions < MINIMUM_RECORDED_TRANSITIONS && lastRecordDuration < MINIMUM_RECORDTIME_MICROSECONDS && CC1101_TX == false){ /*&& (getOperationMode() == OPERATIONMODE_PERISCOPE  || getOperationMode() == OPERATIONMODE_RECORD_SIGNAL))*/
    transitions = tryRecordSignalToBuffer();          
  }

  bool isSuccess = false;
  if(transitions >= MINIMUM_RECORDED_TRANSITIONS){
    if(lastRecordDuration >= MINIMUM_RECORDTIME_MICROSECONDS){
      isSuccess = true;
    } else {
      Serial.println("Copytime too small: " + String(lastRecordDuration));
    }
  } else {
    Serial.println("Not enough Transitions: " + String(transitions));
  }

  if(isSuccess){
    Serial.println("Signal Recorded successfully");
    Serial.println(String(transitions) + " Transitions Recorded");

    Serial.println("AVG RSSI: " + String(CC1101_LAST_AVG_RSSI));
    Serial.println("AVG LQI: " + String(CC1101_LAST_AVG_LQI));

    // WRITE RECORDED SIGNAL BUFFER TO SPIFFS FILE
    // REMOVE PREVIUOS FILE
    SPIFFS.remove(SPIFFS_FILENAME_RECORDED_SIGNAL);
    // OPEN FILE
    File file = SPIFFS.open(SPIFFS_FILENAME_RECORDED_SIGNAL, FILE_WRITE);
    // ADD CONFIGURATION TO FILE
    String cc1101Config = getCC1101Configuration();
    for(auto c : cc1101Config){file.write(c);}    

    writeSignalBufferToFile(file,recordedSignal, transitions);   

    file.close();
  
    dumpSpiffsFileToSerial(SPIFFS_FILENAME_RECORDED_SIGNAL);
    sendSignalFromFile(SPIFFS_FILENAME_RECORDED_SIGNAL);    
  } else {
    
    if(getOperationMode() == OPERATIONMODE_PERISCOPE  || getOperationMode() == OPERATIONMODE_RECORD_SIGNAL){
      Serial.println("Recording again...");
      recordSignal();
    } else {
       Serial.println("Not Recording again." + getOperationMode());
    }
    
  }
}

bool detectSignal(int minRssi){
  bool signalDetected = false;
  detectedRssi = -100;
  detectedFrequency = 0.0;
  while(signalDetected == false && _operationMode == OPERATIONMODE_DETECT_SIGNAL){
      // ITERATE FREQUENCIES      
      for(float fMhz : signalDetectionFrequencies)
      {
          CC1101_MHZ = fMhz;
          initCC1101();

          int rssi = ELECHOUSE_cc1101.getRssi();

          if(rssi >= detectedRssi){
            detectedRssi = rssi;
            detectedFrequency = fMhz;
          }            
      }

      if(detectedRssi >= minRssi){

        signalDetected = true;

        // DEBUG
        Serial.print("MHZ: ");  
        Serial.print(detectedFrequency);   
        Serial.print(" RSSI: ");
        Serial.println(detectedRssi);
        delay(300);

        // SEND BACK
        String frequencyDetected = String(detectedFrequency); // MAX 6 DIGITS E.G. 123.00
        while(frequencyDetected.length() < 6){
          frequencyDetected = "0" + frequencyDetected;
        }

        String rssiDetected = String(detectedRssi * -1); // MAX 4 DIGITS E.G. -100
        while(rssiDetected.length() < 3){
          rssiDetected = "0" + rssiDetected;
        }
        rssiDetected = "-" + rssiDetected;

        String dataString = frequencyDetected + rssiDetected;
        sendCommand(COMMAND_DETECTED_FREQUENCY,COMMAND_ID_DUMMY,dataString);
        
      } 
  }
  return signalDetected;
}

void writeSignalBufferToFile(File file, int signalBuffer[], int signalLength){
    String prepend = "";
    for(int i = 0; i <= signalLength; i++){
        String valueToAdd = prepend + String(signalBuffer[i]);
        writeStringToFile(file, valueToAdd); 
        prepend = ",";
    }
}

void periscope(){
  Serial.println("Looking around...");
  if(getLastExecutedOperationMode() != OPERATIONMODE_PERISCOPE || CC1101_TX == true){
    CC1101_TX = false;
    initCC1101();
  }
  digitalWrite(PIN_LED_ONBOARD, HIGH);  
  while(getOperationMode() == OPERATIONMODE_PERISCOPE && CC1101_TX == false){
    recordSignal();
    Serial.println("Looking for more Signals");
  }
  
  Serial.println("Periscope closed.");
  digitalWrite(PIN_LED_ONBOARD, LOW);  
}

void operationModeRecordSignal(){
  Serial.println("Recording a Signal");
  if(getLastExecutedOperationMode() != OPERATIONMODE_RECORD_SIGNAL || CC1101_TX == true){
    CC1101_TX = false;
    initCC1101();
  }
  digitalWrite(PIN_LED_ONBOARD, HIGH);  

  recordSignal();    

  Serial.println("Recording done.");
  digitalWrite(PIN_LED_ONBOARD, LOW);  
  goIdle();
}

void operationModeDetectSignal(){
  // PARSE MIN RSSI FROM DATASTRING
  if(incomingCommandDataString != "" && incomingCommandDataString.length() == 8){
      int parsedMinRssi = incomingCommandDataString.substring(4).toInt();
      if(parsedMinRssi < 0){
        signalDetectionMinRssi = parsedMinRssi;          
      }
  }

  Serial.println("Detecting a Signal with min. RSSI: " + String(signalDetectionMinRssi));
  if(getLastExecutedOperationMode() != OPERATIONMODE_RECORD_SIGNAL || CC1101_TX == true){
    CC1101_TX = false;
    initCC1101();
  }
  digitalWrite(PIN_LED_ONBOARD, HIGH);  

  bool result = detectSignal(signalDetectionMinRssi);   
   
  Serial.println("Detection done.");
  digitalWrite(PIN_LED_ONBOARD, LOW);  
  if(result){
    setOperationMode(OPERATIONMODE_IDLE);
  }
  
}

void replaySignal(){
  sendSamples(recordedSignal, MAX_LENGHT_RECORDED_SIGNAL);
}

void sendSamples(int samples[], int samplesLenght) {
  if(CC1101_TX == false){
    CC1101_TX = true;
    initCC1101();
  }
  
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

void dumpSpiffsFileToSerial(String fileName){
  Serial.println("Content of File: " + fileName);
  
  File file = SPIFFS.open(fileName, FILE_READ);
  while (file.available()) {
    char c = file.read();
    Serial.print(c);
  }

  Serial.println("<-- End of Content");
  file.close(); 
}

int tryRecordSignalToBuffer(){
  // RESET
  memset(recordedSignal,0,MAX_LENGHT_RECORDED_SIGNAL*sizeof(int));
  byte currentInput = 0;
  int sign = -1;
  int64_t attempts = 0;
  int maxAttempts = 32;
  int recordedTransitions = 0;
  int64_t recordingStarted = esp_timer_get_time();

  for (recordedTransitions = 0; recordedTransitions < MAX_LENGHT_RECORDED_SIGNAL; recordedTransitions++) {
    // RECORD TRANSITION TIMES 
    int64_t transitionTime = 0;
    int64_t readingStarted = esp_timer_get_time();

    while (transitionTime < MAX_TRANSITION_TIME_MICROSECONDS) {
      transitionTime = esp_timer_get_time() - readingStarted;
      if(digitalRead(PIN_GDO0) != currentInput){
        //BREAK THE LOOP IF THE PIN STATE CHANGES
        break;
      }
    }

    int transitionValue;
    if (transitionTime >= MAX_TRANSITION_TIME_MICROSECONDS) {
      transitionValue = MAX_TRANSITION_TIME_MICROSECONDS * sign;
      recordedSignal[recordedTransitions] = transitionValue;
      // RESET ITERATOR WHEN -3200 WAS RECORDED FOR THE FIRST TIME
      if (recordedTransitions == 0) {
        recordedTransitions = -1;
        attempts++;
        if (attempts > maxAttempts) {
          //Serial.println("No signal detected!");
          return -1;
        }
      } else {
        // -32000 WAS RECORDED AFTER SOME POSITIVES VALUES --> END OF SIGNAL
        attempts++;
        if (attempts > MAX_EMPTY_RECORDING_CYCLES) {
          //Serial.println("End of signal detected!");
          break;
        }
      }

    } else {
      transitionValue = transitionTime * sign;
      recordedSignal[recordedTransitions] = transitionValue;
      currentInput = !currentInput;
      if(currentInput) {
        sign = 1;
      } else {
        sign = -1;
      }
    }
  }

  int64_t recordingEnded = esp_timer_get_time();
  lastRecordDuration = (long)(recordingEnded - recordingStarted);

  CC1101_LAST_AVG_LQI = 0.0;
  CC1101_LAST_AVG_RSSI = 0.0;
  
  return recordedTransitions;
}

void writeStringToFile(File file, String data){
  for(char c : data){
    file.write(c);    
  }
}



/*
int lqiCounter = 1;
int lqiRecorded = 1;
int rssiCounter = 1;
int rssiRecorded = 1;
*/
/*
int tryRecordSignalToBuffer(){
  int i;
  lastRecordDuration = 0;
  //Serial.println("Copying...");
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
    while (dif < MAX_TRANSITION_TIME_MICROSECONDS) {
      dif = esp_timer_get_time() - startread;
      if (CCAvgRead() != n) {
        break;
      }
    }
    if (dif >= MAX_TRANSITION_TIME_MICROSECONDS) {
      recordedSignal[i] = MAX_TRANSITION_TIME_MICROSECONDS * sign;
      //if not started wait...
      if (i == 0) {
        i = -1;
        ttime++;
        if (ttime > MAX_EMPTY_RECORDING_CYCLES) {
          //Serial.println("No signal detected!");
          return -1;
        }
      }
      else {
        ttime++;
        if (ttime > MAX_EMPTY_RECORDING_CYCLES) {
          //Serial.println("End of signal detected!");
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

  recordedSamples = i;
  int64_t stopus = esp_timer_get_time();
  lastRecordDuration = (long)(stopus - startus);
  CC1101_LAST_AVG_LQI = lqiRecorded / lqiCounter;
  CC1101_LAST_AVG_RSSI = rssiRecorded / rssiCounter;
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
*/