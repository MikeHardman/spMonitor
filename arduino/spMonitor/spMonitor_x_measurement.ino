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
 * Puts values over the bridge for easy access from external
 *
 * @param index
 *          Index for sensor to read
 *          0 = solar CT
 *          1 = mains CT
 */
void getCTValues (int index) {
  /* Get the measured current from the solar panel */
  emon[index].calcVI(20, 2000);

  /** Measured power in W */
  double power = emon[index].realPower;
  /** String for prefix */
  String prefix = "c";
  if (index == 0) {
    prefix = "s";
    /** Sensor 1 is measuring the solar panel, if it is less than 20W then mostlikely that is the standby current drawn by the inverters */
    if ( emon[index].Irms < 0.5 ) {
      power = 0.0;
    }
  }
  collPower[index] = collPower[index] + power;
  collCount[index] += 1;

  Bridge.put ( prefix, String ( emon[index].Irms ) );
  Bridge.put ( prefix + "r", String ( power )  );
  Bridge.put ( prefix + "v", String ( emon[index].Vrms ) );
  Bridge.put ( prefix + "a", String ( emon[index].apparentPower ) );
  Bridge.put ( prefix + "p", String ( emon[index].powerFactor ) );
}

/**
 * Called every minute by "eventTimer"
 * Reads values from analog input 0 (current produced by solar panel)
 * and analog input 3 (measured luminosity)
 */
void getMeasures () {
  /* Activity LED on */
  digitalWrite ( activityLED, HIGH );

  wdt_reset();
  /* Get the light measurement if a sensor is attached */
  readLux();

  /* Get the measured current from the solar panel */
  getCTValues(0);

  /** Get the measured current from mains */
  getCTValues(1);

  /* Activity LED off */
  digitalWrite ( activityLED, LOW );
}


