"""
Módulo electrónico del juego: dados (botón + 2 displays) y lector RFID.

Protocolo con el servidor (un JSON por línea, terminado en "\n"):
  Servidor -> Pico:  {"accion": "ACTIVAR_DADOS"}
                     {"accion": "LEER_TARJETA"}
  Pico -> Servidor:  {"accion": "RESULTADO_DADOS", "valor": 7}
                     {"accion": "IDENTIFICAR_JUGADOR", "uid": "a1b2c3d4"}
"""
import network
import time
import socket
import select
import json
import random
import sys
from machine import Pin, reset
from mfrc522 import MFRC522
from config import SSID, PASSWORD, SERVER_IP, SERVER_PORT

# ============================================================
# CONFIGURACIÓN
# ============================================================
# True: funciona sin servidor (dados y tarjeta en bucle, resultados por consola).
# False: modo juego (WiFi + servidor, espera órdenes).
MODO_PRUEBA = True

PIN_BOTON = 15                # GP15 (pin físico 20); el otro lado del botón va a GND (el mismo de los displays)
REINTENTO_SERVIDOR_MS = 5000  # cada cuánto reintenta conectar con el servidor
MAX_PENDIENTES = 10           # mensajes que guarda si el servidor no está disponible

# ============================================================
# DISPLAYS
# ============================================================
# Display 2: GP0..GP6 = segmentos A..G. Display 1: GP7 = segmentos B y C (el "1").
SEGMENTOS = [Pin(i, Pin.OUT, value=0) for i in range(7)]
UNO = Pin(7, Pin.OUT, value=0)

# Cada dígito es un byte: bit 0 = A, bit 1 = B, ... bit 6 = G
DIGITOS = [0x3F, 0x06, 0x5B, 0x4F, 0x66, 0x6D, 0x7D, 0x07, 0x7F, 0x6F]


def mostrar_numero(n):
    """Muestra un número de 0 a 19 (el display 1 solo puede formar el 1)."""
    if n < 0 or n > 19:
        raise ValueError("El display solo puede mostrar de 0 a 19")
    decena, unidad = divmod(n, 10)
    UNO.value(decena)
    patron = DIGITOS[unidad]
    for i, pin in enumerate(SEGMENTOS):
        pin.value((patron >> i) & 1)


def apagar_displays():
    UNO.value(0)
    for pin in SEGMENTOS:
        pin.value(0)


# ============================================================
# DADOS
# ============================================================
boton = Pin(PIN_BOTON, Pin.IN, Pin.PULL_UP)  # presionado = 0


def esperar_boton():
    """Bloquea hasta que se presione el botón (con antirrebote)."""
    while boton.value() == 0:  # si estaba presionado, espera a que lo suelten
        time.sleep_ms(10)
    while True:
        if boton.value() == 0:
            time.sleep_ms(30)
            if boton.value() == 0:
                return
        time.sleep_ms(10)


def tirar_dados():
    """Espera el botón, muestra la suma de dos dados (2 a 12) y la devuelve."""
    apagar_displays()
    print("Esperando el botón para tirar los dados...")
    esperar_boton()
    resultado = random.randint(1, 6) + random.randint(1, 6)
    mostrar_numero(resultado)
    print("Resultado de los dados:", resultado)
    return resultado


# ============================================================
# RFID
# ============================================================
reader = MFRC522(spi_id=0, sck=18, miso=16, mosi=19, cs=17, rst=20, baudrate=100000)
_ultimo_uid = None


def leer_tarjeta():
    """Activa el lector y espera hasta leer una tarjeta. Devuelve el UID en hexadecimal.

    Si la tarjeta de la lectura anterior sigue apoyada, no la cuenta de nuevo:
    hay que retirarla y volver a acercarla.
    """
    global _ultimo_uid
    print("Acerca una tarjeta al lector...")
    while True:
        reader.init()
        (stat, tag_type) = reader.request(reader.REQIDL)
        if stat == reader.OK:
            (stat, uid) = reader.SelectTagSN()
            if stat == reader.OK:
                uid_str = reader.tohexstring(uid)
                if uid_str != _ultimo_uid:
                    _ultimo_uid = uid_str
                    print("Tarjeta detectada! UID:", uid_str)
                    return uid_str
        else:
            _ultimo_uid = None
        time.sleep_ms(50)


# ============================================================
# WIFI Y SERVIDOR
# ============================================================
wlan = network.WLAN(network.STA_IF)


def conectar_wifi(timeout_s=10):
    """Intenta conectar al WiFi. Devuelve True si lo logra dentro del timeout."""
    wlan.active(True)
    if wlan.isconnected():
        return True
    wlan.connect(SSID, PASSWORD)
    inicio = time.time()
    while not wlan.isconnected():
        if time.time() - inicio > timeout_s:
            return False
        time.sleep(1)
    return True


class Servidor:
    """Conexión TCP persistente con el servidor (envía y recibe JSON por líneas)."""

    def __init__(self, ip, puerto):
        self.ip = ip
        self.puerto = puerto
        self.sock = None
        self.poll = None
        self.buffer = b""
        self.pendientes = []
        self.ultimo_intento = None

    def cerrar(self):
        if self.sock is not None:
            try:
                self.sock.close()
            except Exception:
                pass
        self.sock = None
        self.poll = None
        self.buffer = b""

    def conectar(self):
        self.cerrar()
        self.ultimo_intento = time.ticks_ms()
        s = None
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            s.settimeout(3)
            s.connect((self.ip, self.puerto))
            self.poll = select.poll()
            self.poll.register(s, select.POLLIN)
            self.sock = s
            print("Conectado al servidor")
            return True
        except Exception as e:
            print("No se pudo conectar al servidor:", e)
            if s is not None:
                s.close()
            return False

    def mantener(self):
        """Reconecta si hace falta y envía los mensajes que quedaron pendientes."""
        if self.sock is None:
            ahora = time.ticks_ms()
            if self.ultimo_intento is None or time.ticks_diff(ahora, self.ultimo_intento) >= REINTENTO_SERVIDOR_MS:
                self.conectar()
        if self.sock is not None and self.pendientes:
            self._vaciar_pendientes()

    def enviar(self, mensaje):
        """Envía un dict como JSON. Si no hay conexión, lo guarda para enviarlo al reconectar."""
        self.pendientes.append(mensaje)
        if len(self.pendientes) > MAX_PENDIENTES:
            self.pendientes.pop(0)
        self._vaciar_pendientes()

    def _vaciar_pendientes(self):
        while self.pendientes and self.sock is not None:
            try:
                self.sock.write((json.dumps(self.pendientes[0]) + "\n").encode())
                print("Enviado al servidor:", self.pendientes[0])
                self.pendientes.pop(0)
            except Exception as e:
                print("Error al enviar:", e)
                self.cerrar()

    def recibir(self):
        """Devuelve un mensaje (dict) si hay uno completo, o None. No bloquea."""
        if self.sock is None:
            return None
        try:
            if self.poll.poll(0):
                datos = self.sock.recv(256)
                if not datos:
                    raise OSError("el servidor cerró la conexión")
                self.buffer += datos
        except Exception as e:
            print("Error al recibir:", e)
            self.cerrar()
            return None
        if b"\n" in self.buffer:
            linea, self.buffer = self.buffer.split(b"\n", 1)
            try:
                mensaje = json.loads(linea.decode())
            except Exception:
                print("Mensaje inválido del servidor:", linea)
                return None
            if isinstance(mensaje, dict):
                return mensaje
        return None


servidor = Servidor(SERVER_IP, SERVER_PORT)


def atender(mensaje):
    """Ejecuta la orden recibida del servidor y responde con el resultado."""
    accion = mensaje.get("accion")
    if accion == "ACTIVAR_DADOS":
        valor = tirar_dados()
        servidor.enviar({"accion": "RESULTADO_DADOS", "valor": valor})
    elif accion == "LEER_TARJETA":
        uid = leer_tarjeta()
        servidor.enviar({"accion": "IDENTIFICAR_JUGADOR", "uid": uid})
    else:
        print("Acción desconocida:", accion)


# ============================================================
# PROGRAMA PRINCIPAL
# ============================================================
def modo_prueba():
    print("MODO PRUEBA (sin servidor)")
    while True:
        tirar_dados()
        leer_tarjeta()


def modo_juego():
    print("Conectando a WiFi...")
    while not conectar_wifi():
        print("Reintentando conexión WiFi...")
    print("WiFi conectado. IP de la Pico:", wlan.ifconfig()[0])
    print("Módulo listo, esperando órdenes del servidor...")

    while True:
        if not wlan.isconnected():
            print("WiFi desconectado, reconectando...")
            servidor.cerrar()
            conectar_wifi()
            continue
        servidor.mantener()
        mensaje = servidor.recibir()
        if mensaje:
            atender(mensaje)
        time.sleep_ms(50)


try:
    if MODO_PRUEBA:
        modo_prueba()
    else:
        modo_juego()
except Exception as e:
    # Con batería no hay consola: se reporta el error y la Pico se reinicia sola.
    sys.print_exception(e)
    apagar_displays()
    time.sleep(5)
    reset()