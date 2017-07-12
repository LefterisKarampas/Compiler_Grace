.intel_syntax noprefix # Use Intel syntax instead of AT&T
	.text
	.global puts1
puts1:
	push ebp
        mov ebp, esp
	push esi
	mov esi, [ebp+16]
	push esi
	mov esi, OFFSET FLAT:.puts
	push esi
	call printf
	pop esi
	mov esp, ebp
        pop ebp
	ret

.global puti1
puti1:
	push ebp
        mov ebp, esp
	push esi
	mov eax, [ebp+16]
	push eax
	mov esi, OFFSET FLAT:.puti
	push esi
	call printf
	pop esi
	mov esp, ebp
        pop ebp
	ret

.global putc1
putc1:	push ebp
        mov ebp, esp
	mov al, byte ptr [ebp+16]
	push eax
	mov esi, OFFSET FLAT:.putc
	push esi
	call printf
	pop esi
	mov esp, ebp
        pop ebp
	ret

.global geti1
geti1:	
	push ebp
        mov ebp, esp
	push esi
	lea esi, [ebp-4]
	push esi
	mov esi, OFFSET FLAT:.geti
	push esi
	call scanf
	mov eax, dword ptr [ebp-4]
	mov esi,dword ptr [ebp+12]
	mov dword ptr [esi],eax
	lea esi,[ebp-4]
	push esi
	mov esi, OFFSET FLAT:.newline
	push esi
	call scanf
	pop esi
	mov esp, ebp
        pop ebp
	ret

.global getc1
getc1:	
	push ebp
        mov ebp, esp
	sub esp,4
	push esi
	lea esi, [ebp-4]
	push esi
	mov esi, OFFSET FLAT:.getc
	push esi
	call scanf
	mov al, byte ptr [ebp-4]
	mov esi,dword ptr [ebp+12]
	mov byte ptr [esi],al
	lea esi,[ebp-4]
	push esi
	mov esi, OFFSET FLAT:.newline
	push esi
	call scanf
	pop esi
	mov esp, ebp
        pop ebp
	ret

.global gets1
gets1:	
    push ebp
    mov ebp, esp
    mov eax, DWORD PTR stdin
    push eax
    mov eax, DWORD PTR [ebp + 16]
    push eax
    mov eax, DWORD PTR [ebp + 20]
    push eax
    call fgets
    mov eax, 10 # Carriage return
    push eax
    mov eax, DWORD PTR [ebp + 20]
    push eax
    call strchr
    add esp, 8
    cmp eax, 0
    je grace_gets_no_newline
    mov BYTE PTR [eax], 0
grace_gets_no_newline:
    mov esp, ebp
    pop ebp
    ret
	
.global strlen1
strlen1:
   push ebp
   mov ebp, esp
   push esi
   lea esi,[ebp+16]
   mov esi,dword ptr [esi]
   push esi
   call strlen
   mov esi,dword ptr [ebp+12]
   mov dword ptr [esi], eax
   pop esi
   mov esp,ebp
   pop ebp
   ret

.global strcmp1
strcmp1:
	push ebp
        mov ebp, esp
	push esi
	mov esi, [ebp+20]
	push esi
	mov esi, [ebp+16]
	push esi
	call strcmp
	mov esi,dword ptr[ebp+12]
	mov dword ptr [esi],eax
	pop esi
	mov esp, ebp
        pop ebp
	ret

.global strcpy1
strcpy1:
	push ebp
        mov ebp, esp
	push esi
	lea esi, [ebp+20]
	mov esi, dword ptr [esi]
	push esi
	lea esi, [ebp+16]
	mov esi, dword ptr [esi]
	push esi
	call strcpy
	pop esi
	mov esp, ebp
        pop ebp
	ret


.global strcat1
strcat1:
	push ebp
        mov ebp, esp
	push esi
	lea esi, [ebp+20]
	mov esi,dword ptr [esi]
	push esi
	lea esi, [ebp+16]
	mov esi,dword ptr [esi]
	push esi
	call strcat
	pop esi
	mov esp, ebp
        pop ebp
	ret

.global abs1
abs1:
	push ebp
	mov ebp, esp
	push esi
	mov eax, dword ptr[ebp + 16]
	push eax
	call abs # absolute value will be in eax
	mov esi, dword ptr [ebp+12]
	mov dword ptr [esi],eax
	pop esi
	mov esp, ebp
	pop ebp
	ret

.global ord1
ord1:
	push ebp
	mov ebp,esp
	sub esp,4
	push esi
	mov al,BYTE PTR[ebp+16]
	and eax,0x000000FF
	mov esi, DWORD PTR[ebp+12]
	mov DWORD PTR [esi],eax
	pop esi
	mov esp,ebp
	pop ebp
	ret
	

.global chr1
chr1:
	push ebp
	mov ebp,esp
	push esi
	mov eax,DWORD PTR[ebp+16]
	mov esi,DWORD PTR[ebp+12]
	mov BYTE PTR[esi],al
	pop esi
	mov esp,ebp
	pop ebp
	ret


	
.data
	.puti: 		.asciz "%d"
	.putc:		.asciz "%c"
	.puts:		.asciz "%s"
	.geti:		.asciz "%d"
	.getc:		.asciz "%c"
	.newline:	.asciz "\n"
