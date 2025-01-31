#!/usr/local/bin/python

import numpy as np
import os
import re
import pandas as pd

############    Files      ########################
jar_name = "./misc/esle-usl-1.0-SNAPSHOT.jar"
data_file= "./misc/example.dat" #only used temporarily (is overwritten for each result csv)
gnu_file = "./misc/throughput.gp"
###################################################

def main():
	#retrieve average throughput per nr_of_nodes from result_csv
	
	for file in os.listdir("./phase_1/"):
		if file.endswith(".csv"):
			s = os.path.join("./", file)
			print(s)
			createPlot(s)

	
	
	

def createPlot(result_file_path):

	file_name = result_file_path.split(".")[1]
	plot_file = "./plots/{}.pdf".format(file_name)
	print(plot_file)

	data = pd.read_csv(result_file_path,sep=",",index_col=None)
	data_grouped = data.groupby('nodes').mean()['throughput']
	
	f = open(data_file, "w")
	f.write("#nodes throughput")
	nodes = 1
	for group in data_grouped:
		s = "\n{}, {}".format(nodes,group)
		f.write(s)
		nodes+= 1

	f.close()


	#run jar for lin. regression
	cmd = "java -jar {} {}".format(jar_name, data_file)
	results = os.popen(cmd).read()

	print(results)
	# parse results
	results = re.split('\d\s', results) 

	#results = ['Total useful lines read: 3', 'Lambda: 995,648780114', 'Delta: 0,026715944', 'Kappa: 0,000769093', '']


	lam = results[1].split(": ")[1]
	delt = results[2].split(": ")[1]
	kap = results[3].split(": ")[1]

	# forward to gnu-function
	cmd= "gnuplot -c {} {} {} {} {} {}".format(gnu_file, data_file, plot_file, lam, delt, kap).replace(",",".")
	print(cmd)
	os.system(cmd)



if __name__ == '__main__':
	main()



