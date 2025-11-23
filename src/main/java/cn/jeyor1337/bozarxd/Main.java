package cn.jeyor1337.bozarxd;

import cn.jeyor1337.bozarxd.obfuscator.Bozar;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarConfig;
import cn.jeyor1337.bozarxd.ui.ConfigManager;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;

/**
 * Main entry point for BozarXD obfuscator
 * Handles both GUI and CLI modes with proper JavaFX environment detection
 */
public class Main {

    public static void main(String[] args) {
        // Parse command line arguments first
        CommandLineParser parser = new DefaultParser();
        try {
            Options options = getOptions();
            CommandLine cmd = parser.parse(options, args);

            // Handle init command - generate template config
            if(cmd.hasOption("init")) {
                System.out.println("Generating template configuration file...");
                try {
                    ConfigManager.generateTemplateConfig();
                    System.out.println("Template configuration file 'bozarConfig.json' has been created successfully.");
                    System.out.println("\nUsage:");
                    System.out.println("1. Edit the configuration file according to your needs");
                    System.out.println("2. Run with: java -jar bozar.jar -config bozarConfig.json -console");
                    System.out.println("\nAlternatively, specify parameters directly:");
                    System.out.println("   java -jar bozar.jar -input input.jar -output output.jar -console");
                } catch (IOException e) {
                    System.err.println("Failed to generate template configuration: " + e.getMessage());
                    e.printStackTrace();
                    System.exit(1);
                }
                return;
            }

            // Handle console mode without GUI
            if(cmd.hasOption("console")) {
                System.out.println("[BozarXD] Running in console mode...");

                // Load or generate config
                BozarConfig config;
                try {
                    if(cmd.hasOption("config")) {
                        File configFile = new File(cmd.getOptionValue("config"));
                        if(!configFile.exists()) {
                            System.err.println("Config file not found: " + configFile.getAbsolutePath());
                            System.exit(1);
                        }
                        config = ConfigManager.loadConfigStatic(configFile);
                    } else if(cmd.hasOption("input") && cmd.hasOption("output")) {
                        // Create minimal config from command line
                        String input = cmd.getOptionValue("input");
                        String output = cmd.getOptionValue("output");
                        config = ConfigManager.createDefaultConfig(input, output);
                        System.out.println("[BozarXD] Using default obfuscation settings");
                    } else {
                        System.err.println("Error: Console mode requires either -config option or both -input and -output options");
                        printUsage(options);
                        System.exit(1);
                        return;
                    }
                } catch (IOException e) {
                    System.err.println("Failed to load configuration: " + e.getMessage());
                    e.printStackTrace();
                    System.exit(1);
                    return;
                }

                // Validate input file exists
                if(!config.getInput().exists()) {
                    System.err.println("Input file not found: " + config.getInput().getAbsolutePath());
                    System.exit(1);
                }

                // Run obfuscation
                System.out.println("[BozarXD] Initializing Bozar...");
                Bozar bozar = new Bozar(config);
                System.out.println("[BozarXD] Executing Bozar...");
                bozar.run();
                return;
            }

            // Try to launch JavaFX application for GUI mode
            try {
                // First check if JavaFX is available
                Class.forName("javafx.application.Application");

                // JavaFX is available, load and launch the App
                Class<?> appClass = Class.forName("cn.jeyor1337.bozarxd.ui.App");
                var method = appClass.getMethod("main", String[].class);
                method.invoke(null, (Object) args);
            } catch (ClassNotFoundException e) {
                // JavaFX not available
                showCliHelp();
            } catch (Exception e) {
                // JavaFX environment error
                System.err.println("JavaFX environment not available: " + e.getMessage());
                showCliHelp();
                System.exit(1);
            }
        } catch (ParseException e) {
            System.err.println("Cannot parse command line arguments: " + e.getMessage());
            printUsage(getOptions());
            System.exit(1);
        }
    }

    private static void showCliHelp() {
        System.err.println("\n=== JavaFX Not Available - Command Line Mode Only ===");
        System.err.println("BozarXD GUI requires JavaFX environment.");
        System.err.println("\nTo use BozarXD in command line mode:");
        System.err.println("1. Generate template config:");
        System.err.println("   java -jar bozar.jar -init");
        System.err.println("\n2. Edit the generated 'bozarConfig.json' file");
        System.err.println("\n3. Run obfuscation:");
        System.err.println("   java -jar bozar.jar -config bozarConfig.json -console");
        System.err.println("\nAlternatively, specify parameters directly:");
        System.err.println("   java -jar bozar.jar -input input.jar -output output.jar -console");
    }

    private static void printUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar bozar.jar", options);
    }

    private static Options getOptions() {
        final Options options = new Options();
        options.addOption(new Option("input", true, "Input JAR file to obfuscate"));
        options.addOption(new Option("output", true, "Output path for obfuscated JAR"));
        options.addOption(new Option("cfg", "config", true, "JSON configuration file path"));
        options.addOption(new Option("noupdate", false, "Disable update version warnings"));
        options.addOption(new Option("c", "console", false, "Run without GUI, start obfuscation immediately"));
        options.addOption(new Option("init", false, "Generate template configuration file in current directory"));
        return options;
    }
}
