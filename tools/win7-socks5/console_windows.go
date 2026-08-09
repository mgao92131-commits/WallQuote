//go:build windows

package main

import (
	"io"
	"log"
	"os"
	"path/filepath"
	"syscall"
)

const attachParentProcess = uintptr(0xFFFFFFFF)

var (
	kernel32      = syscall.NewLazyDLL("kernel32.dll")
	attachConsole = kernel32.NewProc("AttachConsole")
)

func setupLogging() (bool, func()) {
	log.SetFlags(log.Ldate | log.Ltime | log.Lmicroseconds)

	var closers []*os.File
	var writers []io.Writer

	logPath := "lan-socks5.log"
	if exe, err := os.Executable(); err == nil {
		logPath = filepath.Join(filepath.Dir(exe), "lan-socks5.log")
	}

	if file, err := os.OpenFile(logPath, os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0644); err == nil {
		closers = append(closers, file)
		writers = append(writers, file)
	}

	hasConsole := false
	if result, _, _ := attachConsole.Call(attachParentProcess); result != 0 {
		if out, err := os.OpenFile("CONOUT$", os.O_WRONLY, 0); err == nil {
			os.Stdout = out
			os.Stderr = out
			closers = append(closers, out)
			writers = append(writers, out)
			hasConsole = true
		}
	}

	if len(writers) == 0 {
		log.SetOutput(io.Discard)
	} else if len(writers) == 1 {
		log.SetOutput(writers[0])
	} else {
		log.SetOutput(io.MultiWriter(writers...))
	}

	cleanup := func() {
		for i := len(closers) - 1; i >= 0; i-- {
			_ = closers[i].Close()
		}
	}

	return hasConsole, cleanup
}
