package fr.umontpellier.iut;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.zip.*;

public class Server {

  private int port;

  public Server(int port) {
    this.port = port;
  }

  public void start() {
    try (ServerSocket serverSocket = new ServerSocket(port)) {
      System.out.println("Serveur en écoute sur le port " + port);

      while (true) {
        try (Socket clientSocket = serverSocket.accept();
            InputStream is = clientSocket.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is)) {

          File receivedFile = new File("received.zip");
          try (FileOutputStream fos = new FileOutputStream(receivedFile)) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = bis.read(buffer)) > 0) {
              fos.write(buffer, 0, count);
            }
          }

          // Décompression du fichier ZIP
          unzip(receivedFile, new File("unzipped_folder"));

          // Suppression de l'archive après décompression
          receivedFile.delete();

          System.out.println("Dossier reçu et décompressé.");
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void unzip(File zipFile, File outputFolder) throws IOException {
    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
      ZipEntry ze = zis.getNextEntry();
      while (ze != null) {
        File newFile = newFile(outputFolder, ze);
        if (ze.isDirectory()) {
          newFile.mkdirs();
        } else {
          new File(newFile.getParent()).mkdirs();
          try (FileOutputStream fos = new FileOutputStream(newFile)) {
            int len;
            byte[] buffer = new byte[1024];
            while ((len = zis.read(buffer)) > 0) {
              fos.write(buffer, 0, len);
            }
          }
        }
        ze = zis.getNextEntry();
      }
      zis.closeEntry();
    }
  }

  // Pour éviter la vulnérabilité Zip Slip
  private File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
    File destFile = new File(destinationDir, zipEntry.getName());
    String destDirPath = destinationDir.getCanonicalPath();
    String destFilePath = destFile.getCanonicalPath();

    if (!destFilePath.startsWith(destDirPath + File.separator)) {
      throw new IOException("L'entrée ZIP est en dehors du dossier cible");
    }
    return destFile;
  }

  public static void main(String[] args) {
    Server server = new Server(12345); // Même port que le client
    server.start();
  }
}

