#!/bin/bash
x="file.s"
i=0
n=1
s=0
exec_file="a.out"
if [ "$#" -lt "1" ];then
	echo "Usage: ./grace [-i] [-n] [-o exec_file] [-s] grace_file"
	exit 1
fi

if [ "$#" \> "6" ];then
	echo "Usage: ./grace [-i] [-n] [-o exec_file] [-s] grace_file"
	exit 1
fi

while [ "$1" != "" ]                       #Until read all the arguments                          
	do
    if [ "$1" = "-i" ]; then                #-i flag for intermediate code
	i=1
    	shift                               #go to next argument 
    elif [ "$1" = "-n" ]; then              #-n flag for no optimization
	n=0
    	shift                               #go to next argument 
    elif [ "$1" = "-s" ]; then              #-n flag for no optimization
	s=1
    	shift                               #go to next argument 
    elif [ "$1" = "-o" ]; then              #-n flag for no optimization
    	shift                               #go to next argument
	if [ "$1" == "" ];then
		echo "Usage: ./grace [-i] [-n] [-o exec_file] [-s] grace_file"
		exit 1 
	fi
	exec_file=$1
	shift
    else
    	file=$1
	shift
    fi
done

if ! [ -e "target/classes/compiler/Main.class" ];then		#Check if compiler is not already made
	echo "`mvn clean package`"				#Make compiler
fi

cd target/classes						#Jump to that directory 
if [ -e "${file}" ];then
	y=${file}							#Exists file with that name (full path)
else	
	y="../../${file}"						#Exists file with that name in (current directory)
	if ! [ -e "$y" ];then
		echo "No such file with name ${file}"
		exit 2
	fi
fi
if [ "$n" = "0" ]; then
	java compiler.Main $y $n				#Compile your file if file exists (full path) without optimization
else
	java compiler.Main $y					#Compile your file if file exists (full path) with optimization
fi

if [ "$?" != "0" ];then						#Something goes wrong
	cd ../..
	if [ -e Quad ];then					#Remove Quad file and assembly code file
		rm Quad
		rm file.s
	fi
	exit 1
fi

cd ../..
gcc -c file.s							#Create object file
if [ "$?" != "0" ];then						#If fails
	exit 2							#Exit
fi
gcc -c library.s						#else create library object file
gcc -m32 -o ${exec_file} file.o library.o			#Make the executable
rm *.o								#Delete object files
if [ "$i" = "0" ]; then
	rm Quad
fi
if [ "$s" = "0" ]; then
	rm file.s
fi
exit 0
