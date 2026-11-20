"""
Script Python para recibir telemetría de sensores desde la Android App vía Bluetooth SPP.

Requisitos:
    pip install pyserial

Uso en Windows:
    1. Empareja el celular Android con la PC por Bluetooth.
    2. En Configuración de Windows -> Bluetooth -> Más opciones de Bluetooth -> Puertos COM,
       identifica el puerto COM entrante (por ejemplo COM3 o COM4).
    3. Ejecuta este script indicando el puerto:
       python recibir_telemetria_pc.py --puerto COM3
"""

import sys
import argparse
import time

try:
    import serial
except ImportError:
    print("Error: Se requiere la librería 'pyserial'.")
    print("Instálala ejecutando: pip install pyserial")
    sys.exit(1)


def escuchar_telemetria(puerto, baudrate=9600):
    print(f"==================================================")
    print(f" ESPERANDO TELEMETRÍA DESDE ANDROID APP EN {puerto}")
    print(f"==================================================")

    try:
        ser = serial.Serial(puerto, baudrate, timeout=1)
        print(f"Conectado exitosamente al puerto {puerto}.\nEscuchando datos inerciales...\n")

        while True:
            linea = ser.readline().decode('utf-8', errors='ignore').strip()
            if linea:
                print(f"[{time.strftime('%H:%M:%S')}] TELEMETRÍA RECIBIDA: {linea}")
    except serial.SerialException as e:
        print(f"\nError al abrir el puerto {puerto}: {e}")
        print("\nConsejos de solución:")
        print("1. Verifica que el celular esté emparejado por Bluetooth con la PC.")
        print("2. Revisa la lista de puertos COM en Administrador de dispositivos -> Puertos (COM y LPT).")
    except KeyboardInterrupt:
        print("\nRecepción finalizada por el usuario.")
    finally:
        if 'ser' in locals() and ser.is_open:
            ser.close()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Receptor de Telemetría Bluetooth SPP")
    parser.add_argument("--puerto", type=str, default="COM3", help="Puerto COM (ej: COM3, COM4 o /dev/rfcomm0)")
    args = parser.parse_args()

    escuchar_telemetria(args.puerto)
