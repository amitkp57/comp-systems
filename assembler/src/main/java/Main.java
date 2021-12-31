import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.RemoteException;

public class Main {
    public static void main(String[] args) throws IOException {
        String file1 = "/home/amit/Downloads/pradhanAmitProject4 (1)/pradhanAmitProject4/Fill.hack";
        String file2 = "/home/amit/Downloads/assembler/Fill.hack";
        BufferedReader br1 = new BufferedReader(new FileReader(new File(file1)));
        BufferedReader br2 = new BufferedReader(new FileReader(new File(file2)));
        while (true) {
            String line1 = br1.readLine();
            String line2 = br2.readLine();
            if (line1 == null && line2 == null) {
                System.out.println("Done");
                break;
            }

            if (line1 == null || line2 == null || !line1.equals(line2)) {
                throw new RemoteException("Diff");
            }
        }
    }
}
