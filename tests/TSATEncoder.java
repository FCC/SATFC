import java.io.FileNotFoundException;

import org.apache.commons.math3.util.Pair;
import org.junit.Test;

import ca.ubc.cs.beta.stationpacking.base.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.ChannelSpecificConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.constraints.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.DomainStationManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.stations.IStationManager;
import ca.ubc.cs.beta.stationpacking.solvers.sat.base.CNF;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.ISATDecoder;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.ISATEncoder;
import ca.ubc.cs.beta.stationpacking.solvers.sat.cnfencoder.SATEncoder;


public class TSATEncoder {

	@Test
	public void testDuplicateClauses() throws FileNotFoundException {
		
		System.out.println("Creating station manager ...");
		String domainsFilename = "/Users/MightyByte/Documents/workspace/SATFC/relaxed_08232013/domains.csv";
		IStationManager stationManager = new DomainStationManager(domainsFilename);
		
		System.out.println("Creating constraint manager ...");
		String interferencesFilename = "/Users/MightyByte/Documents/workspace/SATFC/relaxed_08232013/interferences.csv";
		IConstraintManager constraintmanager = new ChannelSpecificConstraintManager(stationManager, interferencesFilename);
		
		System.out.println("Creating SAT encoder ...");
		ISATEncoder encoder = new SATEncoder(stationManager, constraintmanager);
		
		System.out.println("Creating station packing instance ...");
		String instanceString = "14-15-16-17-18-19-20-21-22-23-24-25-26-27-28-29-30-31-32-33-34-35-36-37-38-39-40-41-42-43-44-45-46-47-48-49-50-51_648-1822-8-2120-736-1942-1489-2000-1110-1467-487-181-1855-2153-1081-351-1129-1895-1145-41-759-904-161-577-1423-842-865-1813-647-673-878-1283-1388-1069-1835-1062-1431-1002-1412-2087-1073-1491-1021-1334-1663-1668-470-945-2014-832-1681-1187-854-388-209-1136-2039-2173-1305-1845-1850-465-236-118-1311-773-1805-1325-1402-307-51-503-1510-1711-1541-1682-1575-1328-1060-72-374-387-1034-1184-691-720-1827-1273-233-1120-197-1141-1633-1158-89-920-169-322-1936-1109-256-1353-1101-379-1291-62-477-172-2095-1161-428-1472-975-1339-807-1342-572-3-7-1880-97-1010-1976-784-243-1112-108-283-1329-1195-901-1834-1516-1892-1686-2148-1961-1377-1116-1247-1258-627-5-1658-679-135-1960-1349-1084-770-852-604-1635-1437-300-1152-1570-2156-1000-1286-919-2029-926-1278-1694-439-282-1404-1483-1422-1512-1418-1330-1986-1504-751-1222-1721-177-1715-1944-1873-1780-512-1864-1945-613-1814-502-2111-767-1028-1591-309-325-915-2096-204-835-419-1583-1006-1886-167-936-729-473-1716-1640-1344-2126-1376-148-36-2123-965-1272-882-2069-893-401-845-1965-944-1930-1729-1107-29-573-1044-1926-1755-519-69-670-1176-2144-449-1477-1277-780-1771-2047-1395-2008-2-1494-641-1508-467-1324-2171-1830-801-2033-1505-1725-327-2057-2110-1295-1-334-1165-580-1691-869-971-1590-295-75-548-1362-923-1797-294-1051-225-1354-645-1667-1197-1139-59-1656-1125-1779-2107-1531-1639-198-1783-1567-790-1774-749-438-546-1493-1790-143-2102-2066-558-918-744-2166-722-1137-2103-471-239-931-1469-1819-480-1410-1671-1607-188-1683-1490-1357-1296-890-2022-1207-2177-1043-77-814-1327-1394-1578-531-595-1859-32-130-1643-53-2041-1572-1870-1577-628-2059-1359-1432-432-1302-1252-969-849-1348-365-1929-1358-296-1553-706-1593-1594-1767-1613-427-1630-412-552-224-973-1206-1219-905-2054-605-623-496-521-1495-1049-1679-838-128-1462-1089-557-310-1314-730-660-70-881-267-1938-1773-156-1539-662-589-917-686-1768-459-1992-789-160-1312-1628-22-1024-440-2168-545-226-61-131-680-863-912-1029-2105-523-1672-1844-1383-1877-1770-1310-898-833-461-1064-1159-2012-1507-1317-378-1475-1589-2091-142-1458-274-2118-1871-1657-621-1379-1270-1364-1990-1983-1209-983-659-725-1259-1735-1917-1901-1604-406-1825-2021-1647-1479-1519-304-2147-1601-543-1143-690-1520-551-106-2160-747-1192-1939-390-1964-1356-205-2089-827-1602-2077-956-600-1511-2031-28-1665-682-102-1012-1214-255";
		StationPackingInstance instance = StationPackingInstance.valueOf(instanceString, stationManager);
		
		System.out.println("Encoding instance into SAT ...");
		Pair<CNF,ISATDecoder> encoding = encoder.encode(instance);
		
		CNF cnf = encoding.getFirst();
		
		System.out.println("Number of clauses in CNF:");
		System.out.println(cnf.size());
	 
		
		
		
	}

}
