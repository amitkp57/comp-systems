// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/04/Mult.asm

// Multiplies R0 and R1 and stores the result in R2.
// (R0, R1, R2 refer to RAM[0], RAM[1], and RAM[2], respectively.)

// Put your code here.
@R2     // R2 to store product
M = 0   // Initialize R2 to 0

@R1 	 
D = M   // D set to R1 memory
@END  
D;JEQ 	// If D is 0, jump to end

@R3 	// R3 to store number of additions done
M = D 	// Set memory to D and D = 0 previously

(LOOP)
@R0		
D = M   // Set memory to D

@R2	
M = D + M // Add R0 memory to product R2 memory

@R3     
M = M - 1 // Reduce R3 memory by 1
D = M   // Set memory to D

@LOOP
D;JGT // Jump to LOOP when count D > 0

(END)
@END
0;JMP