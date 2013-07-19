package ca.ubc.cs.beta.stationpacking.execution;

import java.util.List;
import java.util.Set;

import ca.ubc.cs.beta.stationpacking.datamanagers.IConstraintManager;
import ca.ubc.cs.beta.stationpacking.datamanagers.IStationManager;
import ca.ubc.cs.beta.stationpacking.datastructures.Station;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.IncrementalSolverParameters;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.TAESolverParameters;
import ca.ubc.cs.beta.stationpacking.solver.cnfencoder.CNFEncoder;
import ca.ubc.cs.beta.stationpacking.solver.cnfencoder.ICNFEncoder;
import ca.ubc.cs.beta.stationpacking.solver.incrementalsolver.IncrementalSolver;
import ca.ubc.cs.beta.stationpacking.solver.incrementalsolver.SATLibraries.GlueMiniSatLibrary;
import ca.ubc.cs.beta.stationpacking.solver.incrementalsolver.SATLibraries.IIncrementalSATLibrary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aclib.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aclib.misc.options.UsageSection;
import ca.ubc.cs.beta.aclib.options.ConfigToLaTeX;
import ca.ubc.cs.beta.stationpacking.datastructures.StationPackingInstance;
import ca.ubc.cs.beta.stationpacking.datastructures.SolverResult;
import ca.ubc.cs.beta.stationpacking.execution.parameters.solver.ExecutableSolverParameters;
import ca.ubc.cs.beta.stationpacking.solver.ISolver;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

/**
 * @author Arpit Goel
 * SingleInstanceExecutor for Incremental SAT Solver
 */
public class IncrementalSolverExecutor {

    private static Logger log = LoggerFactory.getLogger(SingleInstanceStatelessSolverExecutor.class);

     public static String project_dir = "/Users/arpit/Documents/webDev/FCC/fcc-station-packing/FCCStationPacking/";
    /**
     * @param args
     */
    public static void main(String[] args) {


        String[] aPaxosTargetArgs = {
//				"-STATIONS_FILE",
//				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Data/stations2.csv",
                "-DOMAINS_FILE",
//				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Data/NewDACData/Domain-041813.csv",
                project_dir+"fccdata/Domain-041813.csv",
                "-CONSTRAINTS_FILE",
//				"/ubc/cs/home/a/afrechet/arrow-space/workspace/FCCStationPackingExperimentDir/Data/NewDACData/Interferences-041813.csv",
                project_dir+"fccdata/Interferences-041813.csv",
                "-CNF_DIR",
                project_dir+"CNFs",
                "-SOLVER",
                "tunedclasp",
                "--execDir",
                project_dir+"SATsolvers",
                "-LIBRARY",
                project_dir+"SATsolvers/glueminisat/glueminisat-incremental/core/libglueminisat.so",
                "--algoExec",
                "python solverwrapper.py",
                "--cutoffTime",
                "1800",
//				"--cores",
//				"6",
                "-CUTOFF",
                "1800",
                "--logAllProcessOutput",
                "true",
                "--logAllCallStrings",
                "true",
                "-PACKING_CHANNELS",
                "14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30",
                "-PACKING_STATIONS",
                //"25684,32334,39664",
//				"144,146,147,148,363,413,414,416,593,713,714,717,999,1136,1151,1328,2245,2424,2708,2710,2728,2739,2770,3255,3369,3978,4152,4190,4297,4301,4328,4585,4624,4693,5360,5468,5800,5801,5981,5982,6096,6359,6554,6601,6823,6838,7143,7675,7780,7890,7893,7908,7933,8564,8617,8620,8688,9015,9088,9635,9719,9762,9766,9781,9881,9908,9987,10019,10203,10221,10267,10318,10758,10802,11125,11204,11265,11289,11371,11559,11908,11913,11951,12171,12279,12520,12522,12855,13058,13060,13200,13206,13456,13602,13607,13924,13950,13988,13991,13992,14040,16530,16788,16940,16950,17012,17433,17683,18252,18793,19183,19199,19200,19776,19777,20426,20624,20871,21536,21649,21729,21737,22161,22204,22211,22570,22644,22685,22819,23074,23342,23394,23428,23918,23937,23948,23960,24316,24436,24485,24508,24582,24783,25067,25456,25544,25683,25932,26304,26655,26950,27772,27969,28010,28119,28324,28462,28468,28476,29000,29102,29108,29114,29547,29712,30244,30577,31870,32142,32334,33543,33742,33770,34195,34200,34204,34329,34439,34457,34529,34874,34894,35037,35042,35092,35095,35336,35380,35385,35396,35417,35419,35460,35486,35576,35587,35630,35703,35705,35841,35843,35846,35852,35855,35867,35910,35920,35994,36504,36838,36846,36916,36917,36918,37005,37099,37102,37106,37179,37503,37511,38214,38562,39736,39746,40875,40993,41095,41223,41225,41230,41237,41315,41397,41398,42359,42636,42663,43169,43952,44052,46979,47535,47707,47903,47904,48477,48481,48525,48589,48608,48667,48975,49153,49235,49264,49330,49439,49632,50147,50198,50205,50590,50782,51102,51163,51349,51488,51499,51502,51517,51518,51569,51597,51969,52073,52527,52579,52887,52953,53113,53114,53116,53517,53586,53819,53859,53921,55083,55516,55528,55644,56523,56528,57221,57832,57840,57884,57945,58340,58552,58725,58795,58835,59438,59442,59443,59444,59988,60018,60165,60354,60539,60552,60560,60654,60683,60793,60820,60827,60830,60850,61003,61009,61010,61064,61251,61504,61573,62182,62207,62354,62388,62469,63154,63768,63840,63867,64017,64444,64547,64548,64550,64588,64592,64969,64971,64983,65395,65526,65666,65681,65686,65690,66172,66185,66221,66258,66358,66398,66469,66589,66781,66790,66804,66996,67000,67002,67022,67048,67485,67602,67781,67787,67802,67866,67868,67869,67893,67910,67950,68007,68540,68581,68834,68886,68889,69114,69124,69237,69273,69360,69531,69532,69571,69692,69733,69735,69880,69994,70021,70034,70041,70251,70309,70416,70419,70537,70815,70852,70900,71069,71070,71074,71078,71121,71127,71217,71278,71293,71425,71427,71428,71657,71725,72054,72076,72098,72106,72120,72123,72278,72618,72971,73042,73101,73130,73150,73152,73187,73188,73195,73207,73230,73238,73263,73354,73371,73692,73879,73901,73910,73940,73982,74094,74100,74173,74174,74192,74197,74215,74424,76324,77451,77480,78908,78915,81458,81508,81593,83180",
//				"126,282,1328,2566,4624,5982,6048,8322,8653,9015,10203,10205,10213,10238,10242,12913,13206,13924,14322,15320,16788,18783,19190,19707,19783,20287,20818,22206,23128,23342,23671,24518,24981,25735,26025,28324,28954,29234,29706,30576,31352,33336,33691,33742,33778,34874,35101,35280,35500,35652,35675,35883,36989,48836,49326,49803,50780,51349,51499,51517,51567,51597,52408,53921,55106,55350,56537,57219,58608,58827,58912,59255,59440,59442,60534,60556,60559,61011,64592,65074,65247,65667,6584,65690,67335,67869,67910,67999,68444,68540,69677,70415,70493,71236,71586,72064,72096,72123,73123,73238,73333,73363,73764,73881,73988,74091,74112,74174,74211,74422,81458",
//				"147,363,414,593,1136,1151,1328,2424,2708,2710,2728,2739,3978,4152,4190,4297,4328,4585,4693,5800,5801,5981,6096,6554,6838,7143,7780,7890,9635,9719,9766,9881,9987,10019,10221,10267,10758,10802,11204,11265,11289,11371,12279,13058,13206,13456,13602,13607,13924,13991,16530,16788,19776,20871,21737,22161,22211,22570,22644,22685,23074,23342,23394,23937,24436,24485,24783,25456,26655,27772,29114,30577,32142,32334,33742,34894,35037,35092,35095,35396,35417,35419,35576,35587,35703,35841,35846,35855,35920,35994,36504,36916,37102,37106,37179,37511,39736,39746,41223,41225,41230,41397,42359,42636,42663,43952,46979,47535,47904,48477,48481,48525,48608,48975,49153,49330,49439,51163,51488,51499,51517,51518,51569,52073,52953,53113,53114,53116,53517,55083,55516,55528,57832,57840,57945,58340,58795,59438,59442,59443,60539,60560,60850,61009,61064,62182,62354,62469,63154,63768,63840,64550,64592,64971,65526,65666,65681,66469,66589,66781,66790,66996,67048,67602,67787,67869,67893,67950,68007,69237,69273,69733,70041,70852,70900,71069,71070,71074,71127,71425,71427,71428,72098,72106,72120,72123,72278,73150,73152,73188,73195,73207,73230,73238,73371,73879,73910,73940,73982,74174,74192,74197,74215,76324,77480,81458"
                //Instance gen found that one SAT, but it is truly UNSAT
//				"125,126,131,267,414,416,589,590,591,701,711,749,1002,1136,1328,2174,2424,2455,2708,2728,2767,2784,2942,3001,3978,4110,4144,4149,4150,4190,4301,4326,4354,4366,4624,4939,4991,5471,5801,5981,6104,6359,6463,6669,6744,6823,6838,6866,6870,6900,7143,7692,7700,7908,8532,8617,8661,8688,9064,9375,9610,9617,9629,9630,9632,9739,9754,9781,9939,9971,10019,10061,10073,10177,10188,10192,10203,10221,10238,10242,10267,10645,10758,10802,10897,10981,11027,11033,11117,11118,11259,11289,11290,11291,11559,11683,11893,11906,11910,11911,12498,12508,12521,12522,12930,13058,13995,14040,14050,14322,15320,16517,16530,16820,16930,16940,16950,17203,17433,17611,17625,17742,18287,18410,18732,18740,18783,18793,18819,19117,19190,19199,19593,19654,20295,21156,21158,21161,21252,21422,21649,21729,21801,21808,22201,22204,22206,22207,22211,22826,23079,23302,23394,23422,23428,23917,23918,23930,23935,23947,23948,23960,24215,24257,24316,24436,24485,24514,24518,24582,24784,25048,25358,25396,25456,25685,25722,25932,26025,26231,26602,26676,26681,26993,27140,27245,27290,27504,27969,28010,28119,28155,28199,28324,28468,29015,29102,29108,29547,29560,30244,30576,30580,30833,31368,31369,31870,33078,33081,33336,33337,33440,33471,33543,33749,33819,33894,34167,34174,34181,34196,34211,34412,34470,34529,34847,34867,34874,34894,35037,35042,35090,35092,35101,35134,35189,35190,35277,35280,35313,35336,35380,35385,35396,35417,35434,35460,35464,35486,35582,35594,35630,35652,35663,35666,35675,35685,35693,35705,35841,35846,35867,35883,35903,35991,36003,36533,36607,36838,36851,36912,36916,37101,37106,37511,37732,37809,38214,38586,38588,38589,38591,39270,39561,39648,39736,39746,39884,39887,40758,40759,40861,40875,40877,40878,40902,40993,41095,41110,41221,41223,41225,41230,41232,41315,41397,41436,41458,41671,41969,42061,42359,42636,42665,43095,43169,43197,43203,44052,46979,47535,47707,47902,47904,48465,48525,48575,48608,48662,48663,48666,48772,48813,49264,49326,49397,49713,50182,50589,50590,50633,50781,51102,51163,51488,51491,51493,51517,51568,51570,51806,51969,51991,52430,52527,52579,52888,52953,53113,53734,53819,53820,53843,53847,53859,53863,54011,54176,54280,54414,54420,55049,55108,55370,55454,55644,55686,56028,56523,56524,56528,56852,57219,57431,57826,57832,57838,57884,58408,58552,58795,58835,58912,59013,59137,59438,59440,59441,59443,59444,60018,60165,60534,60537,60553,60556,60559,60793,60820,60827,61009,61010,61011,61012,61026,61084,61111,61173,61251,61573,62182,62388,62469,63166,63768,63840,63865,64048,64444,64547,64865,64969,64974,64984,64987,65046,65128,65130,65387,65522,65680,65684,65686,65690,65696,65919,65944,66170,66172,66174,66190,66222,66258,66358,66398,66536,66781,66790,66978,66996,67014,67022,67048,67089,67335,67462,67602,67802,67866,67868,67884,67950,68007,68547,68569,68581,68594,68597,68666,68886,68889,69080,69114,69124,69269,69273,69292,69332,69338,69396,69447,69532,69571,69582,69735,69880,69993,70034,70119,70138,70161,70162,70416,70417,70419,70482,70649,70651,70689,70852,70900,71024,71070,71074,71079,71080,71127,71280,71293,71297,71338,71353,71586,71657,71725,71905,72054,72060,72062,72076,72099,72207,72278,72342,72618,72871,72958,72971,73120,73123,73130,73136,73150,73187,73188,73189,73195,73205,73226,73230,73311,73642,73692,73701,73764,73879,73901,73910,73982,74091,74094,74098,74112,74151,74192,74197,74416,74417,74424,76268,77451,77512,77677,78908,81451,81503,81507,81508,81692,81946,82476,83180"
//				"144,146,147,148,308,363,413,414,416,418,591,593,713,714,717,721,804,999,1136,1151,1328,2174,2245,2424,2708,2710,2728,2739,2770,3246,3255,3369,3978,4077,4152,4190,4297,4301,4328,4329,4585,4624,4693,5360,5468,5800,5801,5875,5981,5982,5985,6096,6124,6359,6554,6601,6823,6838,6870,7143,7651,7675,7780,7890,7893,7894,7908,7933,8322,8564,8617,8620,8688,9015,9088,9635,9719,9762,9766,9781,9881,9908,9987,9989,10019,10177,10192,10203,10221,10242,10267,10318,10758,10802,11113,11125,11204,11264,11265,11289,11291,11371,11559,11906,11908,11913,11951,12171,12279,12520,12522,12525,12855,13058,13060,13200,13206,13456,13602,13607,13924,13950,13960,13988,13991,13992,13994,14040,14315,16530,16788,16930,16940,16950,17012,17433,17683,18252,18793,18798,19183,19199,19200,19776,19777,20295,20426,20624,20871,21161,21250,21536,21649,21656,21729,21737,21801,22161,22204,22211,22215,22570,22644,22685,22819,23074,23079,23128,23342,23394,23428,23918,23937,23948,23960,24303,24316,24436,24485,24508,24582,24783,25067,25382,25456,25544,25683,25932,26304,26655,26950,27772,27969,28010,28119,28230,28324,28462,28468,28476,28496,29000,29102,29108,29114,29547,29712,30244,30577,31351,31870,32142,32176,32334,33337,33543,33742,33770,33819,34195,34200,34204,34329,34406,34439,34457,34529,34874,34894,35037,35042,35084,35092,35095,35096,35097,35336,35380,35385,35396,35417,35419,35460,35486,35576,35584,35587,35630,35703,35705,35841,35843,35846,35852,35855,35867,35883,35907,35910,35918,35920,35994,36318,36395,36504,36838,36846,36916,36917,36918,37005,37099,37102,37106,37176,37179,37503,37511,37732,38214,38375,38562,39736,39746,40618,40875,40993,41095,41223,41225,41230,41237,41315,41397,41398,42359,42636,42663,43169,43952,44052,46979,47535,47707,47903,47904,47987,48477,48481,48525,48575,48589,48608,48666,48667,48813,48975,49153,49235,49264,49330,49439,49632,49832,50044,50147,50198,50205,50589,50590,50782,51102,51163,51349,51488,51499,51502,51517,51518,51569,51570,51597,51969,52073,52527,52579,52887,52953,53113,53114,53116,53517,53586,53819,53859,53921,54940,55083,55108,55516,55528,55644,56528,56642,57221,57832,57840,57884,57905,57945,58340,58552,58616,58684,58725,58795,58835,59438,59439,59442,59443,59444,59988,60018,60165,60354,60539,60552,60560,60654,60683,60793,60820,60827,60830,60850,60931,61003,61005,61009,61010,61064,61173,61214,61251,61504,61573,62182,62207,62354,62388,62469,63154,63158,63557,63768,63840,63865,63867,64017,64444,64547,64548,64550,64588,64592,64969,64971,64974,64983,65387,65395,65526,65666,65681,65686,65690,66172,66185,66221,66222,66258,66358,66398,66469,66589,66781,66790,66804,66996,67000,67002,67022,67048,67077,67335,67462,67485,67494,67602,67781,67787,67802,67866,67868,67869,67893,67910,67950,68007,68518,68540,68542,68581,68597,68695,68834,68886,68889,69114,69124,69237,69273,69360,69396,69416,69440,69531,69532,69571,69692,69733,69735,69880,69946,69994,70021,70034,70041,70251,70309,70416,70419,70482,70537,70815,70852,70900,71069,71070,71074,71078,71085,71121,71127,71217,71238,71278,71293,71425,71427,71428,71657,71680,71725,71871,72054,72076,72098,72106,72119,72120,72123,72207,72278,72618,72971,73042,73101,73113,73130,73150,73152,73187,73188,73195,73207,73230,73238,73263,73354,73371,73692,73706,73879,73901,73910,73940,73982,74094,74100,74173,74174,74192,74197,74215,74424,76324,77451,77480,78908,78915,81458,81508,81593,81750,83180"
//              "5,10,16,39,42,58,82,92,147,165,158"
                "144,147,148"
        };

        args = aPaxosTargetArgs;
        ISolver aSolver = null;
        try
        {
            //Parse the command line arguments in a parameter object.
            IncrementalSolverParameters aIncrementalSolverParameter= new IncrementalSolverParameters();
            ExecutableSolverParameters aExecParams = aIncrementalSolverParameter.ExecutableSolverParameters;
            TAESolverParameters ataeParams = aExecParams.SolverParameters;
            JCommander aParameterParser = JCommanderHelper.getJCommander(aIncrementalSolverParameter,
                    ataeParams.AvailableTAEOptions);
            try
            {
                aParameterParser.parse(args);
            }
            catch (ParameterException aParameterException)
            {
                List<UsageSection> sections = ConfigToLaTeX.getParameters(aIncrementalSolverParameter,
                        ataeParams.AvailableTAEOptions);

                boolean showHiddenParameters = false;

                //A much nicer usage screen than JCommander's
                ConfigToLaTeX.usage(sections, showHiddenParameters);

                log.error(aParameterException.getMessage());
                return;
            }


            try
            {

                IStationManager aStationManager = ataeParams
                        .RepackingDataParameters.getDACStationManager();

                Set<Station> aStations = aStationManager.getStations();

                IConstraintManager iCM = ataeParams
                        .RepackingDataParameters.getDACConstraintManager(aStations);

                log.info("Creating CNF encoder...");
                ICNFEncoder aCNFEncoder = new CNFEncoder(aStations);

                log.info("Creating solver...");

                String aLibraryPath = aIncrementalSolverParameter.getIncrementalLibraryLocation();
			    IIncrementalSATLibrary aSATLibrary = new GlueMiniSatLibrary(aLibraryPath);
			    aSolver = new IncrementalSolver(iCM, aCNFEncoder, aSATLibrary);


                StationPackingInstance aInstance = aExecParams.getInstance();

                SolverResult aResult = aSolver.solve(aInstance,
                        aExecParams.ProblemInstanceParameters.Cutoff,
                        aExecParams.ProblemInstanceParameters.Seed);

                System.out.println("Result for feasibility checker: "+aResult.toParsableString());

            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        finally{
            aSolver.notifyShutdown();
        }

    }

}