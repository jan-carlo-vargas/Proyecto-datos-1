import network
import time

SSID = "Kenia 2.4"
PASSWORD = "K-30-09-95"

wlan = network.WLAN(network.STA_IF)
wlan.active(True)
wlan.connect(SSID, PASSWORD)

print("Conectando...")
intentos = 0
while not wlan.isconnected() and intentos < 20:
    print(".", end="")
    time.sleep(1)
    intentos += 1

if wlan.isconnected():
    print("\n¡Conectado!")
    print("IP de la Pico:", wlan.ifconfig()[0])
else:
    print("\nNo se pudo conectar. Revisá el nombre de red y contraseña.")