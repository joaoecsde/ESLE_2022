#!/usr/local/bin/python

import numpy as np
import os
import re
import pandas as pd

############    Files      ########################
result_file = "./results.csv"
output_file = "summed_up.csv"
###################################################


#read csv -> group by exp nr -> get averages
def main():

	data = pd.read_csv(result_file,sep=",",index_col=None)
	del data['clientId']

	#new_frame= pd.Dataframe(columns=['experiment','nodes','cachesize',
	#	'isPartionAware','cacheIsPartioned','isPerstistent','cpuCores',
	#	'throughputs','avg_throughput']);

	x = data.groupby('experiment').mean()
	x.to_csv(output_file)
	

if __name__ == '__main__':
	main()



