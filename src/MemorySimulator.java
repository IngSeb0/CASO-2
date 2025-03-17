


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class MemorySimulator {
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Seleccione una opción:\n1. Generar referencias\n2. Ejecutar simulación y calcular hits/misses \n3. Ejecutar Sobel");
        int option = scanner.nextInt();
        
        if (option == 1) {
            System.out.print("Ingrese tamaño de página: ");
            int pageSize = scanner.nextInt();
            System.out.print("Ingrese nombre de imagen (sin extensión): ");
            String imageName = scanner.next();
            ReferenceGenerator.generateReferences(imageName, pageSize);
        } else if (option == 2) {
            System.out.print("Ingrese número de marcos de página: ");
            int numFrames = scanner.nextInt();
            System.out.print("Ingrese nombre del archivo de referencias: ");
            String refFile = scanner.next();
            
            
            List<Integer> references = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader("src/" + refFile))) {
                reader.readLine(); 
                reader.readLine(); 
                reader.readLine();
                reader.readLine();
                reader.readLine(); 
                
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    int pageRef = Integer.parseInt(parts[1]);
                    if (pageRef < 100) { 
                        references.add(pageRef);
                    } else {
                        System.out.println("Advertencia: Página fuera de rango ignorada - " + pageRef);
                    }
                }
            }
            
            NRUAlgorithm nru = new NRUAlgorithm(numFrames);
            PageTable pageTable = new PageTable(100);
            
            MemorySimulatorThread simThread = new MemorySimulatorThread(references, nru, pageTable);
            simThread.start();
            
            try {
                simThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else if (option == 3) {
            System.out.print("Ingrese nombre del archivo de imagen BMP (sin extensión): ");
            String inputImage = scanner.next();
            System.out.print("Ingrese nombre de la imagen procesada BMP (sin extensión): ");
            String outputImage = scanner.next();
            SobelFilter.applySobel(inputImage, outputImage);
        }
        
        scanner.close();
    }
}
