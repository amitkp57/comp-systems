// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/04/Fill.asm

// Runs an infinite loop that listens to the keyboard input.
// When a key is pressed (any key), the program blackens the screen,
// i.e. writes "black" in every pixel;
// the screen should remain fully black as long as the key is pressed. 
// When no key is pressed, the program clears the screen, i.e. writes
// "white" in every pixel;
// the screen should remain fully clear as long as no key is pressed.

// Put your code here.
// It whitens or blackens one pixel on the screen at a time and comes back to LOOP to decide on the next step
@SCREEN // Start of screen 
D = A   
@pos    // Keeps track of position of current screen bit 
M = D   // Set pos memory to screen start value 


(LOOP) 
@KBD    // Keyboard value
D = M   

@BLACK  // If any key is pressed, blacken the screen
D;JGT

@WHITE  // If no key is pressed, whiten the screen
D;JMP

 
(WHITE)
@pos    // Whiten the screen at pos
D = M
A = D
M = 0

@SCREEN
D = A	// Set D to screen start

@pos
D = M - D  // pos - screen start 

@LOOP
D;JEQ // If pos is at minimum screen value, don't decrease pos value

@pos
M = M - 1   // Decrease pos by 1

@LOOP
0;JMP   // Jump to LOOP


(BLACK)
@pos      // Blacken the screen at pos 
D = M
A = D
M = -1

@KBD
D = A
D = D - 1 // Max screen value

@pos
D = M - D // pos - max screen value

@LOOP
D;JEQ	// If pos is at maximum screen value, don't increase pos value

@pos
M = M + 1  // Increase pos by 1

@LOOP
0;JMP   // Jump to LOOP