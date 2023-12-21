package fr.umontpellier.iut;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.*;

public class Client {

  private String serverIP;
  private int port;

  public Client(String serverAddress, int port) {
    this.serverIP = serverAddress;
    this.port = port;
  }

  public void sendFolder(String folderPath) throws IOException {
    Set<String> allowedExtensions = loadAllowedExtensions();

    File folder = new File(folderPath);
    if (!folder.isDirectory()) {
      throw new IllegalArgumentException("Le chemin fourni n'est pas un dossier valide");
    }

    // Création d'une archive du dossier avec filtrage des extensions
    File zipFile = createZipFile(folder, allowedExtensions);

    // Envoi de l'archive au serveur
    sendFileToServer(zipFile);

    // Suppression de l'archive temporaire après l'envoi
    zipFile.delete();
  }

  private Set<String> loadAllowedExtensions() throws IOException {
    Set<String> extensions = new HashSet<>();
    try (BufferedReader br = new BufferedReader(new FileReader("src\\main\\resources\\extensions.txt"))) {
      String line;
      while ((line = br.readLine()) != null) {
        extensions.add(line.trim().toLowerCase());
      }
    }
    return extensions;
  }

  private File createZipFile(File folder, Set<String> allowedExtensions) throws IOException {
    File zipFile = new File(folder.getName() + ".zip");
    try (FileOutputStream fos = new FileOutputStream(zipFile);
        ZipOutputStream zos = new ZipOutputStream(fos)) {
      zipFolder(folder, folder.getName(), zos, allowedExtensions);
    }
    return zipFile;
  }

  private void zipFolder(File folderToZip, String parentFolder, ZipOutputStream zos, Set<String> allowedExtensions) throws IOException {
    for (File file : folderToZip.listFiles()) {
      if (file.isDirectory()) {
        zipFolder(file, parentFolder + "/" + file.getName(), zos, allowedExtensions);
        continue;
      }
      String extension = getFileExtension(file);
      if (allowedExtensions.contains(extension)) {
        zos.putNextEntry(new ZipEntry(parentFolder + "/" + file.getName()));
        Files.copy(file.toPath(), zos);
        zos.closeEntry();
      }
    }
  }

  private String getFileExtension(File file) {
    String name = file.getName();
    int lastIndexOf = name.lastIndexOf(".");
    if (lastIndexOf == -1) {
      return ""; // Fichier sans extension
    }
    return name.substring(lastIndexOf + 1).toLowerCase();
  }

  private void sendFileToServer(File file) throws IOException {
    try (Socket socket = new Socket(serverIP, port);
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis);
        OutputStream os = socket.getOutputStream()) {

      byte[] buffer = new byte[4096];
      int count;
      while ((count = bis.read(buffer)) > 0) {
        os.write(buffer, 0, count);
      }
      os.flush();
    }
  }

  public static void main(String[] args) {
    // Exemple d'utilisation
    Client client = new Client("7.tcp.eu.ngrok.io", 16956);
    try {
      client.sendFolder("C:\\Users\\32496\\Desktop\\fichiers");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
