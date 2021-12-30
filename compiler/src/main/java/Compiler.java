import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Main class
 */
public class Compiler {
    public static void main(String[] args) {
        String inputPath = args[0];//"/home/amit/Software/nand2tetris/projects/11/Square";
        File inputFIle = new File(inputPath);

        List<File> jackFiles = new ArrayList<>();

        // If only one input file /some/path/file.jack was passed, write the output to /some/path/output/
        // If only one input directory /some/path/folder/ was passed, write the output to /some/path/folder/output

        if (!inputFIle.isDirectory()) {
            jackFiles.add(inputFIle);
        } else {
            Arrays.asList(inputFIle.listFiles()).stream().filter(file -> file.toString().endsWith(".jack"))
                    .forEach(jackFiles::add);
        }

        for (File jackFile : jackFiles) {
            File tokenFile = new File(String.format("%s%soutput%s%sT.xml", jackFile.getParent(), File.separator,
                    File.separator, jackFile.getName().split("\\.")[0]));
            tokenFile.getParentFile().mkdir();
            File xmlFile = new File(String.format("%s%soutput%s%s.xml", jackFile.getParent(), File.separator,
                    File.separator, jackFile.getName().split("\\.")[0]));
            xmlFile.getParentFile().mkdir();
            File vmFile = new File(String.format("%s%soutput%s%s.vm", jackFile.getParent(), File.separator,
                    File.separator, jackFile.getName().split("\\.")[0]));
            Tokenizer tokenizer = new Tokenizer(jackFile);
            tokenizer.write(tokenFile);
            VMWriter vmWriter = new VMWriter(vmFile);
            CompilationEngine parser = new CompilationEngine(new Tokenizer(jackFile), vmWriter);
            parser.writeXml(xmlFile);
//            parser.writeVm();
            System.out.println(String.format("Completed writing to '%s'", tokenFile));
            System.out.println(String.format("Completed writing to '%s'", xmlFile));
            System.out.println(String.format("Completed writing to '%s'", vmFile));
        }
        System.out.println("Compilation completed successfully!");
    }
}

