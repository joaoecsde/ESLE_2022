#!/usr/local/bin/python
import pandas as pd
import sys
import matplotlib.pyplot as plt

plot_dir = "../plots/"

def main():
    csv_file = sys.argv[1]
    nr_of_nodes = csv_file.split("_")[1].split('.')[0]
    plot_file = "{}snapshot_{}.pdf".format(plot_dir,nr_of_nodes)

    data = pd.read_csv(csv_file,sep=",",index_col=None).values
    x,y = data.T    
    fig, ax = plt.subplots()


    ax.scatter(x, y, c="r", s=[10])
    
    plt.figure(1)
    plt.title("Throughput for {} nodes".format(nr_of_nodes))
    plt.xlabel('Time in seconds')
    plt.ylabel('Throughput')
    plt.savefig(plot_file)


if __name__ == '__main__':
	main()
