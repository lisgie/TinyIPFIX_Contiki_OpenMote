#include <stdint.h>

#ifndef BORDER_ROUTER_IP
#define BORDER_ROUTER_IP /*TO-DO*/
#endif

#ifndef BORDER_ROUTER_PORT
#define BORDER_ROUTER_PORT 40001
#endif

#ifndef NODE_PORT
#define NODE_PORT 40001
#endif

int conn_set_up();

void send_msg(uint8_t *data, uint16_t len);
