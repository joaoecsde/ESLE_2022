set terminal pdf
set output ARG2
#use the first and second columns from the file .dat
print ARG3
print ARG4
print ARG5 


Lambda= ARG3 +0
Delta= ARG4 +0
Kappa= ARG5 +0


set ylabel "Throughput"
set xlabel "Nodes"

#f(x) = (Lambda*x)/(1+Delta*(x-1)+Kappa*x(x-1))
#fit f(x) '../results/example.dat' via Lambda,Delta,Kappa

f(x) = Lambda*x/(1+Delta*(x-1)+Kappa*x*(x-1))

plot f(x), ARG1 using 1:2 title "throughput"
