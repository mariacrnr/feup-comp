package pt.up.fe.comp;

import pt.up.fe.comp.analysis.JmmAnalyser;
import pt.up.fe.comp.backend.JmmBackend;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.optimization.JmmOptimizer;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Launcher {

    public static void main(String[] args) {
        SpecsSystem.programStandardInit();

        SpecsLogs.info("Executing with args: " + Arrays.toString(args));

        // read the input code
        if (args.length != 1) {
            throw new RuntimeException("Expected a single argument, a path to an existing input file.");
        }
        File inputFile = new File(args[0]);
        if (!inputFile.isFile()) {
            throw new RuntimeException("Expected a path to an existing input file, got '" + args[0] + "'.");
        }
        String input = SpecsIo.read(inputFile);

        // Create config
        Map<String, String> config = new HashMap<>();
        config.put("inputFile", args[0]);
        config.put("optimize", "false");
        config.put("registerAllocation", "-1");
        config.put("debug", "false");

        // Instantiate JmmParser
        SimpleParser parser = new SimpleParser();
        // Parse stage
        JmmParserResult parserResult = parser.parse(input, config);
        // Check if there are parsing errors
        TestUtils.noErrors(parserResult.getReports());


        // Instantiate JmmAnalysis
        JmmAnalyser analyser = new JmmAnalyser();
        // Analysis stage
        JmmSemanticsResult analysisResult = analyser.semanticAnalysis(parserResult);
        // Check if there are analysis errors
        TestUtils.noErrors(analysisResult.getReports());


        // Instantiate JmmOptimizer
        JmmOptimizer optimizer = new JmmOptimizer();
        // Optimization stage
        OllirResult optimizerResult = optimizer.toOllir(analysisResult);
        // Check if there are optimization errors
        TestUtils.noErrors(optimizerResult.getReports());

        try {
            File ollirFile = new File(System.getProperty("user.dir") + "/" + optimizerResult.getOllirClass().getClassName() + ".ollir");
            if (!ollirFile.createNewFile()) {
                ollirFile.delete();

                if(!ollirFile.createNewFile()){
                    System.out.println("Error writing OLLIR file");
                }
            }

            try(FileWriter fileWriter = new FileWriter(ollirFile.getAbsolutePath())){
                fileWriter.write(optimizerResult.getOllirCode());
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }


        JmmBackend backend = new JmmBackend();
        JasminResult backendResult = backend.toJasmin(optimizerResult);
        TestUtils.noErrors(backendResult.getReports());

        try {
            File jasminFile = new File(System.getProperty("user.dir")+ "/" + backendResult.getClassName() + ".j");
            if (!jasminFile.createNewFile()) {
                jasminFile.delete();

                if(!jasminFile.createNewFile()){
                    System.out.println("Error writing Jasmin file");
                }

            }

            try(FileWriter fileWriter = new FileWriter(jasminFile.getAbsolutePath())){
                fileWriter.write(backendResult.getJasminCode());
            }

            File classDir = new File(System.getProperty("user.dir")+ "/.");
            File classFile = new File(System.getProperty("user.dir")+ "/" + backendResult.getClassName() + ".class");

            if (classFile.exists()) {
                if(!classFile.delete()){
                    System.out.println("Error writing class file");
                }
            }

            backendResult.compile(classDir);
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}
