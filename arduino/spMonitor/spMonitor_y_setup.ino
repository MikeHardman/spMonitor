/**
 * Solar Panel Monitor
 *
 * Uses current sensor to measure output of solar panels.
 * Optional additional measurement of luminosity.
 * Optional additional measurement of in/output to electricity grid
 *
 * @author Bernd Giesecke
 * @version 0.1 beta August 13, 2015.
 */

/**
 * Solar Panel Monitor
 * setup
 * required by Arduino IDE
 *
 * Initialize timers for LED and analog readings
 * Initialize sensor interfaces
 * Initialize communication
 */
void setup() {
  /* set pin to output */
  pinMode ( activityLED, OUTPUT );

  /* Initialize bridge connection */
  Bridge.begin();
  /* Listen for incoming connection only from localhost */
  /* (no one from the external network could connect) */
  server.listenOnLocalhost();
  server.begin();

  /* Initialize access to SDcard */
  FileSystem.begin();

  /* Configure the Adafruit TSL2561 light sensor */
  /* Initialise the sensor */
  if ( tsl.begin() ) {
    /* Setup the sensor gain and integration time */
    configureSensor();
  }

  /* Initialize counters and accumulators */
  collPower[0] = collPower[1] = 0.0;
  collCount[0] = collCount[1] = collCount[2] = 0;
  collEnergy[0] = collEnergy[1] = 0.0;

  /* Configure the YHDC SCT013-000 current sensors */
  /* Initialise the current sensor 1 */
  emon[0].voltage( 2, vCal, 1.3 ); // AD2, Vcal = 255, phase shift = 1.3
  emon[0].current ( 0, iCal1 ); // AD0, Ical = 5.7
  /* Initialise the current sensor 2 */
  emon[1].voltage( 2, vCal, 2 ); // AD2, Vcal = 255, phase shift = 2
  emon[1].current ( 1, iCal2 ); // AD1, Ical = 11.5

  /* Get initial reading to setup the low pass filter */
  unsigned int i = 0;
  while (i<50) {
    /* LED on */
    digitalWrite ( activityLED, HIGH );
    emon[0].calcVI ( 20, 2000 );
    emon[1].calcVI ( 20, 2000 );
    /* LED off */
    digitalWrite ( activityLED, LOW );
    i++;
  }

  /* For debug only */
  //writeDebug( getTimeStamp() );
  /* End of For debug only */

  /* Initiate call of getMeasures and saveData every 5 seconds / 60 seconds */
  lastMeasure = lastSave = millis();

  /* Activate the watchdog */
  wdt_enable(WDTO_8S);
}

