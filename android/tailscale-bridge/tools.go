//go:build tools

// Hält golang.org/x/mobile in go.mod fest: gomobile bind braucht golang.org/x/mobile/bind im Modul.
package tsbridge

import _ "golang.org/x/mobile/bind"
