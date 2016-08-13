#include "networking.h"

//This include changed with Contiki 3.0 from /net/uip.h to /net/ip/uip.h
#include "net/ip/uip.h"
#include "net/ip/uip-udp-packet.h"

static struct uip_udp_conn* conn_handle;
static uip_ipaddr_t border_router;

int conn_set_up() {

	//This might get changed in the future
	uip_ip6addr(&border_router, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0xFFFF);

	//Setting up a UDP connection with a predefined port, see header
	if((conn_handle = udp_new (&border_router, UIP_HTONS (BORDER_ROUTER_PORT), NULL)) == NULL)
		return -1;

	//Bind the connection to the local port
	udp_bind(conn_handle, UIP_HTONS (NODE_PORT));

	return 0;
}

void send_msg(uint8_t *data, uint16_t len) {

	//Pass data down the stack and send it
	uip_udp_packet_send(conn_handle, data, len);
}
