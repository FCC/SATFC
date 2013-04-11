station_id_pop_interference = read.table(file.choose(),sep=",")
station_data = as.matrix(read.xls(file.choose())[-c(1,2),])
#Why do we have pop info in station_pop.csv that is not entered in the .xls file?
#Why are there 532 more stations in the .xls file?
station_id_type_pop = cbind(station_data[,4],station_data[,7],station_data[,10]); #take columns we want
full_power_ind = which(station_id_type_pop[,2]=="DT") 
station_id_type_pop[full_power_ind,2]=1
station_id_type_pop[-full_power_ind,2]=0
station_id_type_pop = apply(station_id_type_pop,2,as.numeric) #convert to numeric
have_constraint_info_ind = which(station_id_type_pop[,1] %in% station_id_pop_interference[,1]) #keep only stations in stations_pop.csv
reduced_station_id_type_pop = station_id_type_pop[have_constraint_info_ind,]
reduced_station_id_type_pop = reduced_station_id_type_pop[order(reduced_station_id_type_pop[,1]),] #sort by stationID
station_id_pop_interference = station_id_pop_interference[order(station_id_pop_interference[,1]),] #sort by stationID
reduced_station_id_type_pop = cbind(reduced_station_id_type_pop[,1:2], station_id_pop_interference[,2:3]) #combine data
full_power_ind = which(reduced_station_id_type_pop[,2]==1)
f_p = reduced_station_id_type_pop[full_power_ind,-2]
l_p = reduced_station_id_type_pop[-full_power_ind,-2]

#Generating xxx_power_pop_vs_interference
fpp = f_p[which(f_p[,2]>99),]
lpp = l_p[which(l_p[,2]>99),]
plot(log(fpp[,2],base=10),fpp[,3],pch='.',cex=2,xlab='log(Population)',ylab='Interference Count',main='Full Power Stations',xlim=c(3.5,7.5),ylim=c(0,180))
plot(log(lpp[,2],base=10),lpp[,3],pch='.',cex=2,xlab='log(Population)',ylab='Interference Count',main='Low Power Stations',xlim=c(3.5,7.5),ylim=c(0,180))
plot(log(c(fpp[,2],lpp[,2]),base=10),c(fpp[,3],lpp[,3]),pch='.',cex=2,xlab='log(Population)',ylab='Interference Count',main='All Stations',xlim=c(3.5,7.5),ylim=c(0,180))

#Generating interference_per_pop graph
l_intperpop = lpp[,3]/lpp[,2]
f_intperpop =fpp[,3]/fpp[,2]
l_F = ecdf(l_intperpop)
f_F = ecdf(f_intperpop)
plot(1000*f_intperpop[1:1317],f_F(f_intperpop[1:1317]),cex=.5,col="BLUE",xlab='Interference Count/1000 Pop',ylab='Percentile')
points(1000*l_intperpop[1:400],l_F(l_intperpop[1:400]),pch='x',cex=.5,col="RED")






############################

station_id_pop_interference = read.table(file.choose(),sep=",")
constraints = read.table(file.choose(),sep=",")
station_data = as.matrix(read.xls(file.choose())[-c(1,2),])
station_locations = apply(station_data[,11:16],2,as.numeric)
#Converting degrees, minutes, seconds into just degrees
station_lat = station_locations[,1]+station_locations[,2]/60+station_locations[,3]/3600
station_long_neg = -(station_locations[,4]+station_locations[,5]/60+station_locations[,6]/3600)
#the relevant columns
id_long_lat_state = cbind(as.numeric(station_data[,4]),station_long_neg,station_lat,station_data[,2])
id_long_lat_state = filter_data(id_long_lat_state,station_id_pop_interference[,1],"id",TRUE)


excluded_territories = c("HI","AK","PR","VI")
included_territories = c("MN","WI","IA","ND","SD")
included_territories = c("CA","WA","OR")
#id_long_lat_state_r = filter_data(id_long_lat_state,included_territories,"state",TRUE)
id_long_lat_state_r = filter_data(id_long_lat_state,excluded_territories,"state",FALSE)
plot(id_long_lat_state_r[,2],id_long_lat_state_r[,3],pch=id_long_lat_state_r[,4],cex=0.5,xlab="",ylab="", axes = FALSE, main = "Title Here")
print_constraints(apply(id_long_lat_state_r[,1:3],2,as.numeric),constraints,type=c("ADJ"))


filter_data = function(id_long_lat_state,list,filter_type,included=TRUE){
	#Insert some checks about appropriate length
	if(filter_type=="state") ind = which(id_long_lat_state[,4] %in% list) #some state fields have two states listed
	else if(filter_type == "id") ind = which(id_long_lat_state[,1] %in% list)
	else return(NULL)
	if(included) return(id_long_lat_state[ind,])
	else{
		if(length(ind)==0) return(id_long_lat_state)
		return(id_long_lat_state[-ind,])
	}
}

print_constraints = function(id_long_lat='numeric',constraints,type="CO"){
	numPrintedConstraints = 0
	constraints = constraints[which(constraints[,1] %in% type),2:3]
	ids = id_long_lat[,1]
	numConstraints = length(constraints[,1])
	for(i in 1:numConstraints){
		j = which(ids == constraints[i,1])
		k = which(ids == constraints[i,2])
		if(length(j)==1 && length(k)==1){
			segments(id_long_lat[j,2],id_long_lat[j,3],id_long_lat[k,2],id_long_lat[k,3])
			numPrintedConstraints = numPrintedConstraints+1
		}
	}
	return(numPrintedConstraints)
}
