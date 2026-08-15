package main

import (
	"bufio"
	"encoding/binary"
	"flag"
	"fmt"
	"io"
	"log"
	"net"
	"os"
	"os/signal"
	"strings"
	"sync"
	"time"
)

const defaultControlAddr = "127.0.0.1:1081"

func main() {
	hasConsole, cleanupLogging := setupLogging()
	defer cleanupLogging()

	listenAddr := flag.String("listen", "0.0.0.0:1080", "SOCKS5 listen address")
	controlAddr := flag.String("control", defaultControlAddr, "local control address")
	showStatus := flag.Bool("status", false, "show running status and exit")
	stopServer := flag.Bool("stop", false, "stop the running server and exit")
	flag.Parse()

	if *showStatus {
		result, err := controlCommand(*controlAddr, "status")
		if err != nil {
			printCommandResult(hasConsole, "not running")
			return
		}
		printCommandResult(hasConsole, result)
		return
	}

	if *stopServer {
		result, err := controlCommand(*controlAddr, "stop")
		if err != nil {
			printCommandResult(hasConsole, "not running")
			return
		}
		printCommandResult(hasConsole, result)
		return
	}

	startedAt := time.Now()

	// Binding the loopback-only control port also acts as a single-instance guard.
	controlListener, err := net.Listen("tcp", *controlAddr)
	if err != nil {
		if status, statusErr := controlCommand(*controlAddr, "status"); statusErr == nil && strings.HasPrefix(status, "running ") {
			log.Printf("another instance is already running: %s", status)
			return
		}
		log.Fatalf("control listener %s failed: %v", *controlAddr, err)
	}
	defer controlListener.Close()

	listener, err := net.Listen("tcp", *listenAddr)
	if err != nil {
		log.Fatalf("SOCKS5 listener %s failed: %v", *listenAddr, err)
	}
	defer listener.Close()

	stopCh := make(chan struct{})
	var stopOnce sync.Once
	requestStop := func(reason string) {
		stopOnce.Do(func() {
			log.Printf("stopping: %s", reason)
			close(stopCh)
		})
	}

	go serveControl(controlListener, *listenAddr, startedAt, requestStop)
	go func() {
		<-stopCh
		_ = listener.Close()
		_ = controlListener.Close()
	}()

	interrupts := make(chan os.Signal, 1)
	signal.Notify(interrupts, os.Interrupt)
	defer signal.Stop(interrupts)
	go func() {
		select {
		case <-interrupts:
			requestStop("interrupt")
		case <-stopCh:
		}
	}()

	log.Printf("LAN SOCKS5 started")
	log.Printf("SOCKS5 listen: %s", *listenAddr)
	log.Printf("control: %s", *controlAddr)
	log.Printf("authentication: none")
	log.Printf("TCP CONNECT only")
	if hasConsole {
		log.Printf("console attached; Ctrl+C or -stop can stop the server")
	}

	for {
		conn, acceptErr := listener.Accept()
		if acceptErr != nil {
			select {
			case <-stopCh:
				log.Printf("LAN SOCKS5 stopped")
				return
			default:
				log.Printf("accept failed: %v", acceptErr)
				continue
			}
		}
		go handleClient(conn)
	}
}

func printCommandResult(hasConsole bool, result string) {
	if hasConsole && os.Stdout != nil {
		_, _ = fmt.Fprintln(os.Stdout, result)
		return
	}
	log.Print(result)
}

func serveControl(listener net.Listener, socksListen string, startedAt time.Time, requestStop func(string)) {
	for {
		conn, err := listener.Accept()
		if err != nil {
			return
		}
		go handleControlConnection(conn, socksListen, startedAt, requestStop)
	}
}

func handleControlConnection(conn net.Conn, socksListen string, startedAt time.Time, requestStop func(string)) {
	defer conn.Close()
	_ = conn.SetDeadline(time.Now().Add(3 * time.Second))

	line, err := bufio.NewReader(io.LimitReader(conn, 128)).ReadString('\n')
	if err != nil {
		return
	}

	switch strings.TrimSpace(strings.ToLower(line)) {
	case "status":
		uptime := time.Since(startedAt).Round(time.Second)
		_, _ = fmt.Fprintf(conn, "running pid=%d listen=%s uptime=%s\n", os.Getpid(), socksListen, uptime)
	case "stop":
		_, _ = io.WriteString(conn, "stopping\n")
		requestStop("local control command")
	default:
		_, _ = io.WriteString(conn, "error unknown-command\n")
	}
}

func controlCommand(controlAddr, command string) (string, error) {
	conn, err := net.DialTimeout("tcp", controlAddr, 1500*time.Millisecond)
	if err != nil {
		return "", err
	}
	defer conn.Close()

	_ = conn.SetDeadline(time.Now().Add(2 * time.Second))
	if _, err = io.WriteString(conn, command+"\n"); err != nil {
		return "", err
	}

	line, err := bufio.NewReader(io.LimitReader(conn, 512)).ReadString('\n')
	if err != nil {
		return "", err
	}
	return strings.TrimSpace(line), nil
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
	if header[2] != 0x00 {
		return "", fmt.Errorf("invalid reserved byte: %d", header[2])
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
