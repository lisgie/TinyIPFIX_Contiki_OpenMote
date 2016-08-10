#include "contiki.h"
#include <stdint.h>
//#include "net/ip/tcpip.h"
//#include "dev/leds.h"
//#include "networking/networking.h"

//#include "TinyIPFIX/tinyipfix.h"

PROCESS(main_proc, "Main Process");
AUTOSTART_PROCESSES(&main_proc);

PROCESS_THREAD(main_proc, ev, data)
{

  // Process data declaration
  static struct etimer data_timer, template_timer;
  uint8_t *buffer;

  PROCESS_BEGIN();

  /*if(conn_set_up() == -1) {
	  PROCESS_EXIT();
  }

  initialize();

  // Set event timers for template and data packet creation
  etimer_set(&template_timer, CLOCK_SECOND*15);
  etimer_set(&data_timer, CLOCK_SECOND*5);

  while (1) {

	  PROCESS_WAIT_EVENT();
      if (etimer_expired (&template_timer)) {

    	  //buffer = get_template();

    	  buffer = get_template();

    	  leds_on (LEDS_RED);
    	  clock_delay (500);
    	  leds_off (LEDS_RED);

    	  send_msg(buffer, buffer[1]);

    	  etimer_reset(&template_timer);
      } else if(etimer_expired(&data_timer)) {

    	  buffer = get_data();

    	  leds_on (LEDS_GREEN);
    	  clock_delay (500);
    	  leds_off (LEDS_GREEN);

    	  send_msg(buffer, buffer[1]);

    	  etimer_reset(&data_timer);
      }
  }*/

  PROCESS_END();
}


