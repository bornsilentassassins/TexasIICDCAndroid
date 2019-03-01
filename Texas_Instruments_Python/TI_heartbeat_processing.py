from numpy import genfromtxt
import numpy as np
from sklearn.model_selection import train_test_split

input = genfromtxt("TI data.csv", delimiter=",", dtype="|U16")

data = input[1:,:]

sum_rest=0
count_rest=0
sum_general=0
count_general=0
sum_extra=0
extra=0
for i in range (len(data)):
    if(data[i][3]=="Resting"):
        sum_rest=sum_rest+int(data[i][2])
        count_rest=count_rest+1
    elif (data[i][3] == "General"):
        sum_general=sum_general+int(data[i][2])
        count_general=count_general+1
    else:
        sum_extra=sum_extra+int(data[i][2])
        extra=extra+1

#print(sum_extra/extra)
print("Resting Mean " , sum_general/count_general)
print("General Mean " , sum_rest/count_rest)
#print(extra)
print(count_general)
print(count_rest)

bpm = data[:,2]
state = data[:,3]

X_train, X_test, y_train, y_test = train_test_split(bpm, state, test_size=0.1, random_state=42)

#print (X_train)