package main

import (
	"encoding/binary"
	"flag"
	"fmt"
	"io"
	"log"
	"net"
	"os"
	"os/signal"
	"syscall"
	"time"
)

func main() {
	listenAddr := flag.String("listen", "0.0.0.0:1080", "SOCKS5 listen address")
	flag.Parse()

	log.SetFlags(log.Ldate | log.Ltime | log.Lmicroseconds)

	ln, err := net.Listen("tcp", *listenAddr)
	if err != nil {
		log.Fatalf("listen failed: %v", err)
	}
	defer ln.Close()

	log.Printf("SOCKS5 server listening on %s", *listenAddr)
	log.Printf("authentication: none")
	log.Printf("TCP CONNECT only; press Ctrl+C to stop")

	sig := make(chan os.Signal, 1)
	signal.Notify(sig, os.Interrupt, syscall.SIGTERM)
	go func() {
		<-sig
		log.Printf("shutting down")
		_ = ln.Close()
	}()

	for {
		conn, err := ln.Accept()
		if err != nil {
			return
		}
		go handleClient(conn)
	}
}

func handleClient(client net.Conn) {
	defer client.Close()
	clientAddr := client.RemoteAddr().String()
	_ = client.SetDeadline(time.Now().Add(15 * time.Second))

	if err := handshake(client); err != nil {
		log.Printf("[%s] handshake failed: %v", clientAddr, err)
		return
	}

	target, err := readRequest(client)
	if err != nil {
		log.Printf("[%s] request failed: %v", clientAddr, err)
		return
	}

	log.Printf("[%s] -> %s", clientAddr, target)
	remote, err := net.DialTimeout("tcp", target, 15*time.Second)
	if err != nil {
		sendReply(client, 0x05, nil)
		log.Printf("[%s] connect %s failed: %v", clientAddr, target, err)
		return
	}
	defer remote.Close()

	_ = client.SetDeadline(time.Time{})
	sendReply(client, 0x00, remote.LocalAddr())

	done := make(chan struct{}, 2)
	go func() {
		_, _ = io.Copy(remote, client)
		if tcp, ok := remote.(*net.TCPConn); ok {
			_ = tcp.CloseWrite()
		}
		done <- struct{}{}
	}()
	go func() {
		_, _ = io.Copy(client, remote)
		if tcp, ok := client.(*net.TCPConn); ok {
			_ = tcp.CloseWrite()
		}
		done <- struct{}{}
	}()

	<-done
	log.Printf("[%s] <- %s closed", clientAddr, target)
}

func handshake(conn net.Conn) error {
	header := make([]byte, 2)
	if _, err := io.ReadFull(conn, header); err != nil {
		return err
	}
	if header[0] != 0x05 {
		return fmt.Errorf("unsupported SOCKS version: %d", header[0])
	}

	nMethods := int(header[1])
	if nMethods == 0 {
		return fmt.Errorf("client offered no authentication methods")
	}
	methods := make([]byte, nMethods)
	if _, err := io.ReadFull(conn, methods); err != nil {
		return err
	}

	for _, method := range methods {
		if method == 0x00 {
			_, err := conn.Write([]byte{0x05, 0x00})
			return err
		}
	}

	_, _ = conn.Write([]byte{0x05, 0xff})
	return fmt.Errorf("client does not support no-authentication mode")
}

func readRequest(conn net.Conn) (string, error) {
	header := make([]byte, 4)
	if _, err := io.ReadFull(conn, header); err != nil {
		return "", err
	}
	if header[0] != 0x05 {
		return "", fmt.Errorf("invalid SOCKS version")
	}
	if header[1] != 0x01 {
		sendReply(conn, 0x07, nil)
		return "", fmt.Errorf("unsupported command %d; TCP CONNECT only", header[1])
	}

	var host string
	switch header[3] {
	case 0x01:
		ip := make([]byte, 4)
		if _, err := io.ReadFull(conn, ip); err != nil {
			return "", err
		}
		host = net.IP(ip).String()
	case 0x03:
		length := make([]byte, 1)
		if _, err := io.ReadFull(conn, length); err != nil {
			return "", err
		}
		if length[0] == 0 {
			return "", fmt.Errorf("empty domain")
		}
		domain := make([]byte, int(length[0]))
		if _, err := io.ReadFull(conn, domain); err != nil {
			return "", err
		}
		host = string(domain)
	case 0x04:
		ip := make([]byte, 16)
		if _, err := io.ReadFull(conn, ip); err != nil {
			return "", err
		}
		host = net.IP(ip).String()
	default:
		sendReply(conn, 0x08, nil)
		return "", fmt.Errorf("unsupported address type %d", header[3])
	}

	portBytes := make([]byte, 2)
	if _, err := io.ReadFull(conn, portBytes); err != nil {
		return "", err
	}
	port := binary.BigEndian.Uint16(portBytes)
	return net.JoinHostPort(host, fmt.Sprintf("%d", port)), nil
}

func sendReply(conn net.Conn, rep byte, addr net.Addr) {
	ip := net.IPv4zero
	port := uint16(0)
	if tcpAddr, ok := addr.(*net.TCPAddr); ok {
		if v4 := tcpAddr.IP.To4(); v4 != nil {
			ip = v4
		}
		if tcpAddr.Port >= 0 && tcpAddr.Port <= 65535 {
			port = uint16(tcpAddr.Port)
		}
	}

	resp := make([]byte, 10)
	resp[0] = 0x05
	resp[1] = rep
	resp[2] = 0x00
	resp[3] = 0x01
	copy(resp[4:8], ip.To4())
	binary.BigEndian.PutUint16(resp[8:10], port)
	_, _ = conn.Write(resp)
}
