//Some edit

//Some other edit

#include "contiki.h"
#include <stdint.h>

//The LED code is platform independent
#include "dev/leds.h"

#include "TinyIPFIX/tinyipfix.h"
#include "networking/networking.h"

#define TEMPLATE_INTERVAL 16
#define DATA_INTERVAL 7

PROCESS(main_proc, "Main Process");
AUTOSTART_PROCESSES(&main_proc);

//Used to signal undesired behavior, configuration of lights are coded
void debug(unsigned char config);

PROCESS_THREAD(main_proc, ev, data)
{
	static struct etimer data_timer, template_timer;
	uint8_t *buffer;

	PROCESS_BEGIN();

	//Build everything that is known at compile time (e.g., template, data header)
	if(conn_set_up() == -1 || initialize_tinyipfix()) {

		debug(LEDS_GREEN | LEDS_RED | LEDS_BLUE);
		PROCESS_EXIT();
	}

	//Set event timers for template and data packet creation
	etimer_set(&template_timer, CLOCK_SECOND*TEMPLATE_INTERVAL);
	etimer_set(&data_timer, CLOCK_SECOND*DATA_INTERVAL);

	//Main Loop
	while (1) {

		PROCESS_WAIT_EVENT();

		if (etimer_expired (&template_timer)) {

			buffer = get_template();

			if( (buffer = get_template()) == NULL) {

				debug(LEDS_RED | LEDS_BLUE);
				continue;
			}

			leds_on (LEDS_BLUE);
			clock_delay (500);
			leds_off (LEDS_BLUE);

			send_msg(buffer, buffer[1]);

			etimer_reset(&template_timer);
		} else if(etimer_expired(&data_timer)) {

			if( (buffer = get_data()) == NULL) {

				debug(LEDS_RED | LEDS_GREEN);
				continue;
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

//Used for special debugging codes
void debug(unsigned char config) {

	  leds_off(LEDS_ALL);
	  leds_on(config);
	  clock_wait(CLOCK_SECOND);
	  leds_off(config);
}


