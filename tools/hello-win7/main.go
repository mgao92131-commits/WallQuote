package main

import (
	"bufio"
	"fmt"
	"os"
)

func main() {
	fmt.Println("Hello World")
	fmt.Println("Press Enter to exit...")
	_, _ = bufio.NewReader(os.Stdin).ReadString('\n')
}
