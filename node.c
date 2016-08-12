#include "contiki.h"
#include <stdint.h>

#include "dev/leds.h"

#include "TinyIPFIX/tinyipfix.h"
#include "networking/networking.h"

PROCESS(main_proc, "Main Process");
AUTOSTART_PROCESSES(&main_proc);

void debug(unsigned char config);

PROCESS_THREAD(main_proc, ev, data)
{

  // Process data declaration
  static struct etimer data_timer, template_timer;
  uint8_t *buffer;

  PROCESS_BEGIN();

  if(conn_set_up() == -1) {

	  debug(LEDS_GREEN | LEDS_RED | LEDS_BLUE);
	  PROCESS_EXIT();
  }

  initialize();

  // Set event timers for template and data packet creation
  etimer_set(&template_timer, CLOCK_SECOND*16);
  etimer_set(&data_timer, CLOCK_SECOND*7);

  while (1) {


	  PROCESS_WAIT_EVENT();
      if (etimer_expired (&template_timer)) {

    	  buffer = get_template();

    	  if( (buffer = get_template()) == NULL) {

    		  debug(LEDS_RED | LEDS_BLUE);
    		  break;
    	  }

    	  leds_on (LEDS_BLUE);
    	  clock_delay (500);
    	  leds_off (LEDS_BLUE);

    	  send_msg(buffer, buffer[1]);

    	  etimer_reset(&template_timer);
      } else if(etimer_expired(&data_timer)) {

    	  if( (buffer = get_data()) == NULL) {

    		  debug(LEDS_RED | LEDS_GREEN);
    		  break;
    	  }

    	  leds_on (LEDS_GREEN);
    	  clock_delay (500);
    	  leds_off (LEDS_GREEN);

    	  send_msg(buffer, buffer[1]);

    	  etimer_reset(&data_timer);
      }
  }

  PROCESS_END();
}

void debug(unsigned char config) {

	  leds_off(LEDS_ALL);
	  leds_on(config);
	  clock_wait(CLOCK_SECOND);
	  leds_off(config);
}


