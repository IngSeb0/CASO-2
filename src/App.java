import java.io.*;
import java.util.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.concurrent.locks.*;

// Clase que representa una página en la memoria virtual
class Page {
    int pageNumber;
    boolean referenced;
    boolean modified;
    boolean inMemory;
    
    public Page(int pageNumber) {
        this.pageNumber = pageNumber;
        this.referenced = false;
        this.modified = false;
        this.inMemory = false;
    }
}

// Clase que maneja la tabla de páginas
class PageTable {
    private Map<Integer, Page> pages;
    
    public PageTable(int numPages) {
        pages = new HashMap<>();
        for (int i = 0; i < numPages; i++) {
            pages.put(i, new Page(i));
        }
    }
    
    public Page getPage(int pageNumber) {
        return pages.getOrDefault(pageNumber, null);
    }
}

// Algoritmo de reemplazo de páginas: "No Usadas Recientemente" (NRU)
class NRUAlgorithm {
    private List<Page> memoryFrames;
    private Lock lock;
    
    public NRUAlgorithm(int numFrames) {
        this.memoryFrames = new LinkedList<>();
        this.lock = new ReentrantLock();
    }
    
    public void addPage(Page page) {
        lock.lock();
        try {
            if (memoryFrames.size() >= 4) { // Simulamos que hay 4 marcos de memoria
                replacePage(page);
            } else {
                memoryFrames.add(page);
                page.inMemory = true;
            }
        } finally {
            lock.unlock();
        }
    }
    
    private void replacePage(Page newPage) {
        Page removedPage = memoryFrames.remove(0);
        removedPage.inMemory = false;
        memoryFrames.add(newPage);
        newPage.inMemory = true;
        System.out.println("Página reemplazada: " + removedPage.pageNumber + " -> " + newPage.pageNumber);
    }
}

// Clase para simular la ejecución del sistema de paginación
class MemorySimulatorThread extends Thread {
    private List<Integer> references;
    private NRUAlgorithm nru;
    private PageTable pageTable;
    private int hits = 0;
    private int misses = 0;
    private int timeHits = 0;
    private int timeMisses = 0;
    private final int timeRAM = 50; // 50ns acceso RAM
    private final int timeSWAP = 10000000; // 10ms fallo de página
    
    public MemorySimulatorThread(List<Integer> references, NRUAlgorithm nru, PageTable pageTable) {
        this.references = references;
        this.nru = nru;
        this.pageTable = pageTable;
    }
    
    public void run() {
        for (int ref : references) {
            Page page = pageTable.getPage(ref);
            if (page != null) {
                if (page.inMemory) {
                    hits++;
                    timeHits += timeRAM;
                } else {
                    misses++;
                    timeMisses += timeSWAP;
                    nru.addPage(page);
                }
            }
            try {
                Thread.sleep(1); // Simulación de ejecución cada ms
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("\nResumen de la simulación:");
        System.out.println("Total de referencias procesadas: " + references.size());
        System.out.println("Total Hits: " + hits + " (" + ((double) hits / references.size() * 100) + "%)");
        System.out.println("Total Misses: " + misses + " (" + ((double) misses / references.size() * 100) + "%)");
        System.out.println("Tiempo total Hits: " + timeHits + " ns");
        System.out.println("Tiempo total Misses: " + timeMisses + " ns");;
    }
}

class ReferenceGenerator {
    public static void generateReferences(String imageName, int pageSize) throws IOException {
        File file = new File("src/" + imageName + ".bmp");
        BufferedImage image = ImageIO.read(file);
        int NF = image.getHeight();
        int NC = image.getWidth();
        int NR = NF * NC * 3; // Cada píxel tiene 3 referencias (R, G, B)
        int NP = (NF * NC * 3) / pageSize + 1; // Número de páginas virtuales necesarias
        
        BufferedWriter writer = new BufferedWriter(new FileWriter("src/referencias.txt"));
        writer.write("TP=" + pageSize + "\n");
        writer.write("NF=" + NF + "\n");
        writer.write("NC=" + NC + "\n");
        writer.write("NR=" + NR + "\n");
        writer.write("NP=" + NP + "\n");
        
        // Generar referencias
        for (int i = 0; i < NF; i++) {
            for (int j = 0; j < NC; j++) {
                int page = (i * NC + j) / pageSize;
                writer.write("Imagen[" + i + "][" + j + "].r," + page + ",0,R\n");
                writer.write("Imagen[" + i + "][" + j + "].g," + page + ",1,R\n");
                writer.write("Imagen[" + i + "][" + j + "].b," + page + ",2,R\n");
            }
        }
        writer.close();
        System.out.println("Archivo de referencias generado en src/referencias.txt");
    }
}

class SobelFilter {
    public static void applySobel(String inputImage, String outputImage) throws IOException {
        File file = new File("src/" + inputImage + ".bmp");
        BufferedImage image = ImageIO.read(file);
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage resultImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        int[][] sobelX = {
                {-1, 0, 1},
                {-2, 0, 2},
                {-1, 0, 1}
        };
        int[][] sobelY = {
                {-1, -2, -1},
                {0, 0, 0},
                {1, 2, 1}
        };
        
        for (int x = 1; x < width - 1; x++) {
            for (int y = 1; y < height - 1; y++) {
                int gx = 0;
                int gy = 0;
                for (int i = -1; i <= 1; i++) {
                    for (int j = -1; j <= 1; j++) {
                        int rgb = image.getRGB(x + i, y + j) & 0xFF;
                        gx += sobelX[i + 1][j + 1] * rgb;
                        gy += sobelY[i + 1][j + 1] * rgb;
                    }
                }
                int magnitude = (int) Math.sqrt(gx * gx + gy * gy);
                magnitude = Math.min(255, Math.max(0, magnitude));
                int edgeColor = (magnitude << 16) | (magnitude << 8) | magnitude;
                resultImage.setRGB(x, y, edgeColor);
            }
        }
        ImageIO.write(resultImage, "bmp", new File("src/" + outputImage + ".bmp"));
        System.out.println("Filtro Sobel aplicado. Imagen guardada en: src/" + outputImage + ".bmp");
    }
}
