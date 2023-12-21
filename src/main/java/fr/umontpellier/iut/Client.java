package fr.umontpellier.iut;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.zip.*;

public class Client {

  private String serverIP;
  private int port;

  public Client(String serverAddress, int port) {
    this.serverIP = serverAddress;
    this.port = port;
  }

  public void sendFolder(String folderPath) throws IOException {
    File folder = new File(folderPath);
    if (!folder.isDirectory()) {
      throw new IllegalArgumentException("Le chemin fourni n'est pas un dossier valide");
    }

    // Création d'une archive du dossier
    File zipFile = createZipFile(folder);

    // Envoi de l'archive au serveur
    sendFileToServer(zipFile);

    // Suppression de l'archive temporaire après l'envoi
    zipFile.delete();
  }

  private File createZipFile(File folder) throws IOException {
    File zipFile = new File(folder.getName() + ".zip");
    try (FileOutputStream fos = new FileOutputStream(zipFile);
        ZipOutputStream zos = new ZipOutputStream(fos)) {
      zipFolder(folder, folder.getName(), zos);
    }
    return zipFile;
  }

  private void zipFolder(File folderToZip, String parentFolder, ZipOutputStream zos) throws IOException {
    for (File file : folderToZip.listFiles()) {
      if (file.isDirectory()) {
        zipFolder(file, parentFolder + "/" + file.getName(), zos);
        continue;
      }
      zos.putNextEntry(new ZipEntry(parentFolder + "/" + file.getName()));
      Files.copy(file.toPath(), zos);
      zos.closeEntry();
    }
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
    Client client = new Client("127.0.0.1", 12345);
    try {
      client.sendFolder("C:\\Users\\32496\\Desktop\\fichier2");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


}

