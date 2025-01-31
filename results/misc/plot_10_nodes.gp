set terminal pdf
set output "./results_plot.pdf"
#use the first and second columns from the file .dat

set ylabel "Throughput"
set xlabel "Iteration"
set key outside  #so that the legend does not overlap with the graphs
plot "./data_10_nodes.dat" using 1:2 title "throughput"
