package nl.bioinf.cawarmerdam.compound_evolver.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipHelper {
    //Don't allow instantiation of the class
    private ZipHelper() {
    }

    public static byte[] zipDirs(List<Path> dirs) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(outputStream);
        byte[] temp;
        for (Path path : dirs) {
            temp = zipDir(path);
            zos.putNextEntry(new ZipEntry(path.getFileName().toString() + ".zip"));
            zos.write(temp);
            zos.closeEntry();
        }
        zos.flush();
        zos.close();
        outputStream.flush();
        outputStream.close();
        return outputStream.toByteArray();
    }

    /**
     * Zips the contents of a directory
     *
     * @param directory the directory to zip
     * @return a bytearray representing the zipped directory
     * @throws IOException opening the directory failed
     */
    public static byte[] zipDir(Path directory) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(outputStream);
        addDirToZipArchive(zos, directory.toFile(), null);
        zos.flush();
        outputStream.flush();
        zos.close();
        outputStream.close();

        return outputStream.toByteArray();
    }

    /**
     * Adds a directory to a zipped archive
     *
     * @param zipOutputStream     A zip output stream that holds the archive.
     * @param fileToZip           The file object which should be added to the archive.
     * @param parentDirectoryName The parent path of the file which should be added.
     */
    private static void addDirToZipArchive(ZipOutputStream zipOutputStream, File fileToZip, String parentDirectoryName) {
        if (fileToZip == null || !fileToZip.exists()) {
            return;
        }

        String zipEntryName = fileToZip.getName();
        if (parentDirectoryName != null && !parentDirectoryName.isEmpty()) {
            zipEntryName = parentDirectoryName + "/" + fileToZip.getName();
        }

        if (fileToZip.isDirectory()) {
            for (File file : Objects.requireNonNull(fileToZip.listFiles())) {
                addDirToZipArchive(zipOutputStream, file, zipEntryName);
            }

        } else {
            try {
                byte[] buffer = new byte[1024];
                FileInputStream fileInputStream;
                fileInputStream = new FileInputStream(fileToZip);

                zipOutputStream.putNextEntry(new ZipEntry(zipEntryName));
                int length;
                while ((length = fileInputStream.read(buffer)) > 0) {
                    zipOutputStream.write(buffer, 0, length);
                }
                zipOutputStream.closeEntry();
                fileInputStream.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
