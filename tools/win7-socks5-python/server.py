import argparse
import logging
import os
import select
import socket
import struct
import sys
import threading


def setup_logging():
    handlers = []
    log_path = os.path.join(os.path.dirname(os.path.abspath(sys.executable if getattr(sys, 'frozen', False) else __file__)), 'lan-socks5-python.log')
    try:
        handlers.append(logging.FileHandler(log_path, encoding='utf-8'))
    except OSError:
        pass
    if sys.stdout is not None:
        handlers.append(logging.StreamHandler(sys.stdout))
    if not handlers:
        handlers.append(logging.NullHandler())
    logging.basicConfig(level=logging.INFO, format='%(asctime)s %(levelname)s %(message)s', handlers=handlers)


def recv_exact(sock, count):
    data = bytearray()
    while len(data) < count:
        chunk = sock.recv(count - len(data))
        if not chunk:
            raise ConnectionError('connection closed')
        data.extend(chunk)
    return bytes(data)


def send_reply(client, rep, bind_addr=None):
    host = '0.0.0.0'
    port = 0
    if bind_addr:
        host, port = bind_addr[0], bind_addr[1]
    try:
        packed = socket.inet_aton(host)
    except OSError:
        packed = b'\x00\x00\x00\x00'
    client.sendall(b'\x05' + bytes([rep]) + b'\x00\x01' + packed + struct.pack('!H', port))


def relay(a, b):
    sockets = [a, b]
    while True:
        readable, _, _ = select.select(sockets, [], [], 60)
        if not readable:
            continue
        for src in readable:
            dst = b if src is a else a
            data = src.recv(65536)
            if not data:
                return
            dst.sendall(data)


def handle_client(client, addr):
    remote = None
    try:
        client.settimeout(15)
        ver, nmethods = recv_exact(client, 2)
        if ver != 5 or nmethods == 0:
            return
        methods = recv_exact(client, nmethods)
        if 0 not in methods:
            client.sendall(b'\x05\xff')
            return
        client.sendall(b'\x05\x00')

        ver, cmd, rsv, atyp = recv_exact(client, 4)
        if ver != 5 or rsv != 0:
            return
        if cmd != 1:
            send_reply(client, 7)
            return

        if atyp == 1:
            host = socket.inet_ntoa(recv_exact(client, 4))
        elif atyp == 3:
            length = recv_exact(client, 1)[0]
            if length == 0:
                return
            host = recv_exact(client, length).decode('idna')
        elif atyp == 4:
            host = socket.inet_ntop(socket.AF_INET6, recv_exact(client, 16))
        else:
            send_reply(client, 8)
            return

        port = struct.unpack('!H', recv_exact(client, 2))[0]
        target = '{}:{}'.format(host, port)
        logging.info('[%s:%s] -> %s', addr[0], addr[1], target)

        remote = socket.create_connection((host, port), timeout=15)
        client.settimeout(None)
        remote.settimeout(None)
        send_reply(client, 0, remote.getsockname())
        relay(client, remote)
        logging.info('[%s:%s] <- %s closed', addr[0], addr[1], target)
    except Exception as exc:
        logging.warning('[%s:%s] error: %s', addr[0], addr[1], exc)
        try:
            send_reply(client, 5)
        except Exception:
            pass
    finally:
        if remote is not None:
            try:
                remote.close()
            except Exception:
                pass
        try:
            client.close()
        except Exception:
            pass


def main():
    parser = argparse.ArgumentParser(description='Minimal SOCKS5 TCP CONNECT server for Windows 7 test')
    parser.add_argument('--listen', default='0.0.0.0')
    parser.add_argument('--port', type=int, default=1080)
    args = parser.parse_args()

    setup_logging()
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind((args.listen, args.port))
    server.listen(128)
    logging.info('Python SOCKS5 listening on %s:%d', args.listen, args.port)
    logging.info('authentication: none; TCP CONNECT only')

    try:
        while True:
            client, addr = server.accept()
            threading.Thread(target=handle_client, args=(client, addr), daemon=True).start()
    except KeyboardInterrupt:
        logging.info('stopping')
    finally:
        server.close()


if __name__ == '__main__':
    main()
