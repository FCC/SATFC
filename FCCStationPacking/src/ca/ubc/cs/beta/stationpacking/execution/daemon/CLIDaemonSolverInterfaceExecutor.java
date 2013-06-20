package ca.ubc.cs.beta.stationpacking.execution.daemon;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.stationpacking.execution.daemon.client.ClientCommunicationMechanism;
import ca.ubc.cs.beta.stationpacking.execution.daemon.message.CommandMessage;
import ca.ubc.cs.beta.stationpacking.execution.daemon.message.IMessage;
import ca.ubc.cs.beta.stationpacking.execution.daemon.message.SolveMessage;

/**
 * CLI interface that reads user commands and sends them to Daemon Solver.
 * @author afrechet
 *
 */
public class CLIDaemonSolverInterfaceExecutor {

	private static Logger log = LoggerFactory.getLogger(CLIDaemonSolverInterfaceExecutor.class);

	public static void main(String[] args) throws Exception{
		
		ClientCommunicationMechanism aCComMec = new ClientCommunicationMechanism(8080);
		IMessage aSolverAnswerMessage = aCComMec.communicate(new SolveMessage(Arrays.asList(144,146,147,148,308,363,413,414,416,418,591,593,713,714,717,804,999,1136,1151,1328,2174,2245,2424,2708,2710,2728,2739,2770,3246,3255,3369,3978,4077,4152,4190,4297,4301,4328,4329,4585,4624,4693,5360,5468,5800,5801,5875,5981,5982,6096,6124,6359,6554,6601,6823,6838,6870,7143,7651,7675,7780,7890,7893,7894,7908,7933,8322,8564,8617,8620,8688,9015,9088,9635,9719,9762,9766,9781,9881,9908,9987,9989,10019,10177,10192,10203,10221,10242,10267,10318,10758,10802,11125,11204,11264,11265,11289,11291,11371,11559,11906,11908,11913,11951,12171,12279,12520,12522,12525,12855,13058,13060,13200,13206,13456,13602,13607,13924,13950,13960,13988,13991,13992,13994,14040,14315,16530,16788,16930,16940,16950,17012,17433,17683,18252,18793,18798,19183,19199,19200,19776,19777,20295,20426,20624,20871,21161,21250,21536,21649,21656,21729,21737,21801,22161,22204,22211,22215,22570,22644,22685,22819,23074,23128,23342,23394,23428,23918,23937,23948,23960,24303,24316,24436,24485,24508,24582,24783,25067,25382,25456,25544,25683,25932,26304,26655,26950,27772,27969,28010,28119,28230,28324,28462,28468,28476,28496,29000,29102,29108,29114,29547,29712,30244,30577,31351,31870,32142,32176,32334,33337,33543,33742,33770,33819,34195,34200,34204,34329,34406,34439,34457,34529,34874,34894,35037,35042,35084,35092,35095,35097,35336,35380,35385,35396,35417,35419,35460,35486,35576,35584,35587,35630,35703,35705,35841,35843,35846,35852,35855,35867,35883,35907,35910,35920,35994,36318,36395,36504,36838,36846,36916,36917,36918,37005,37099,37102,37106,37176,37179,37503,37511,37732,38214,38375,38562,39736,39746,40618,40875,40993,41095,41223,41225,41230,41237,41315,41397,41398,42359,42636,42663,43169,43952,44052,46979,47535,47707,47903,47904,48477,48481,48525,48575,48589,48608,48667,48813,48975,49153,49235,49264,49330,49439,49632,49832,50044,50147,50198,50205,50589,50590,50782,51102,51163,51349,51488,51499,51502,51517,51518,51569,51597,51969,52073,52527,52579,52887,52953,53113,53114,53116,53517,53586,53819,53859,53921,54940,55083,55516,55528,55644,56528,57221,57832,57840,57884,57905,57945,58340,58552,58684,58725,58795,58835,59438,59439,59442,59443,59444,59988,60018,60165,60354,60539,60552,60560,60654,60683,60793,60820,60827,60830,60850,60931,61003,61005,61009,61010,61064,61173,61214,61251,61504,61573,62182,62207,62354,62388,62469,63154,63158,63557,63768,63840,63865,63867,64017,64444,64547,64548,64550,64588,64592,64969,64971,64974,64983,65387,65395,65526,65666,65681,65686,65690,66172,66185,66221,66222,66258,66358,66398,66469,66589,66781,66790,66804,66996,67000,67002,67022,67048,67077,67462,67485,67494,67602,67781,67787,67802,67866,67868,67869,67893,67910,67950,68007,68518,68540,68542,68581,68597,68695,68834,68886,68889,69114,69124,69237,69273,69360,69396,69416,69440,69531,69532,69571,69692,69733,69735,69880,69946,69994,70021,70034,70041,70251,70309,70416,70419,70482,70537,70815,70852,70900,71069,71070,71074,71078,71085,71121,71127,71217,71278,71293,71425,71427,71428,71657,71725,71871,72054,72076,72098,72106,72119,72120,72123,72207,72278,72618,72971,73042,73101,73113,73130,73150,73152,73187,73188,73195,73207,73230,73238,73263,73354,73371,73692,73706,73879,73901,73910,73940,73982,74094,74100,74173,74174,74192,74197,74215,74424,76324,77451,77480,78908,78915,81458,81508,81593,81750,83180), Arrays.asList(14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30),1800.0));
		//IMessage aSolverAnswerMessage = aCComMec.communicate(new SolveMessage(Arrays.asList(144,146,147,148,363,413,414,416,1136,1151), Arrays.asList(14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30),1800.0));
		//aCComMec.communicate(new StatusMessage(StatusMessage.Status.TERMINATED));
		log.info(aSolverAnswerMessage.toString());
		
		//aCComMec = new ClientCommunicationMechanism(8080);
		//aSolverAnswerMessage = aCComMec.communicate(new CommandMessage(CommandMessage.Command.TERMINATE));
		//log.info(aSolverAnswerMessage.toString());

	}

	
	


}
