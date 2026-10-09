from machine import Pin
import time

# Display 2: GP0..GP6 = segmentos A, B, C, D, E, F, G
SEGMENTOS = [Pin(i, Pin.OUT, value=0) for i in range(7)]
NOMBRES = "ABCDEFG"

# Display 1: GP7 alimenta los segmentos B y C (forma el "1")
UNO = Pin(7, Pin.OUT, value=0)

# Cada dígito es un byte: bit 0 = A, bit 1 = B, ... bit 6 = G
DIGITOS = [
    0x3F,  # 0
    0x06,  # 1
    0x5B,  # 2
    0x4F,  # 3
    0x66,  # 4
    0x6D,  # 5
    0x7D,  # 6
    0x07,  # 7
    0x7F,  # 8
    0x6F,  # 9
]


def mostrar(valor):
    """Enciende en el display 2 los segmentos indicados por los bits de 'valor'."""
    for i, pin in enumerate(SEGMENTOS):
        pin.value((valor >> i) & 1)


def apagar_todo():
    mostrar(0)
    UNO.value(0)


# --- Prueba 1: segmento por segmento del display 2 ---
print("Prueba 1: segmentos del display 2 (uno por uno)")
for i, nombre in enumerate(NOMBRES):
    print("  Debe encenderse el segmento", nombre)
    mostrar(1 << i)
    time.sleep(1)
apagar_todo()
time.sleep(1)

# --- Prueba 2: display 1 ---
print("Prueba 2: display 1 (debe mostrar un 1)")
UNO.value(1)
time.sleep(2)
UNO.value(0)
time.sleep(1)

# --- Prueba 3: dígitos del 0 al 9 en el display 2 ---
print("Prueba 3: dígitos 0 a 9 en el display 2")
for n in range(10):
    print("  Mostrando", n)
    mostrar(DIGITOS[n])
    time.sleep(0.8)
apagar_todo()
time.sleep(1)

# --- Prueba 4: ambos displays juntos (11) ---
print("Prueba 4: ambos displays juntos (debe verse 11)")
UNO.value(1)
mostrar(DIGITOS[1])
time.sleep(3)

apagar_todo()
print("Prueba terminada")