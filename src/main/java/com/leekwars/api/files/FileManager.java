package com.leekwars.api.files;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FileManager {
    private String currentPath;
    private final String rootPath;
    
    /**
     * Initialize FileManager with the current working directory as root
     */
    public FileManager() {
        this.rootPath = System.getProperty("user.dir");
        this.currentPath = this.rootPath;

        // Ensure that .merged_ai directory exists
        try {
            createDirectory(".merged_ais");
        } catch (IOException e) {
            System.err.println("Failed to create .merged_ai directory: " + e.getMessage());
        }
    }
    
    /**
     * Initialize FileManager with a custom root path
     */
    public FileManager(String rootPath) {
        this.rootPath = new File(rootPath).getAbsolutePath();
        this.currentPath = this.rootPath;
    }
    
    /**
     * Get the current path
     */
    public String getCurrentPath() {
        return currentPath;
    }
    
    /**
     * Get the root path
     */
    public String getRootPath() {
        return rootPath;
    }
    
    /**
     * List all files in the current directory
     */
    public List<FileInfo> listFiles() {
        return listFiles(currentPath);
    }
    
    /**
     * List all files in a specific directory
     */
    public List<FileInfo> listFiles(String path) {
        File directory = new File(path);
        if (!directory.exists() || !directory.isDirectory()) {
            return new ArrayList<>();
        }
        
        File[] files = directory.listFiles();
        if (files == null) {
            return new ArrayList<>();
        }
        
        return Arrays.stream(files)
                .filter(File::isFile)
                .map(this::createFileInfo)
                .collect(Collectors.toList());
    }
    
    /**
     * List all directories in the current directory
     */
    public List<FileInfo> listDirectories() {
        return listDirectories(currentPath);
    }
    
    /**
     * List all directories in a specific directory
     */
    public List<FileInfo> listDirectories(String path) {
        File directory = new File(path);
        if (!directory.exists() || !directory.isDirectory()) {
            return new ArrayList<>();
        }
        
        File[] files = directory.listFiles();
        if (files == null) {
            return new ArrayList<>();
        }
        
        return Arrays.stream(files)
                .filter(File::isDirectory)
                .map(this::createFileInfo)
                .collect(Collectors.toList());
    }
    
    /**
     * List all files and directories in the current directory
     */
    public List<FileInfo> listAll() {
        return listAll(currentPath);
    }
    
    /**
     * List all files and directories in a specific directory
     */
    public List<FileInfo> listAll(String path) {
        File directory = new File(path);
        if (!directory.exists() || !directory.isDirectory()) {
            return new ArrayList<>();
        }
        
        File[] files = directory.listFiles();
        if (files == null) {
            return new ArrayList<>();
        }
        
        return Arrays.stream(files)
                .map(this::createFileInfo)
                .sorted((a, b) -> {
                    // Directories first, then files
                    if (a.isDirectory() != b.isDirectory()) {
                        return a.isDirectory() ? -1 : 1;
                    }
                    return a.getName().compareToIgnoreCase(b.getName());
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Change to a specific directory (relative or absolute path)
     * Returns true if successful, false if path is invalid or outside root
     */
    public boolean changeDirectory(String path) {
        File newDir = new File(currentPath, path);
        
        // If path is absolute, use it directly
        if (new File(path).isAbsolute()) {
            newDir = new File(path);
        }
        
        try {
            String canonicalPath = newDir.getCanonicalPath();
            String canonicalRoot = new File(rootPath).getCanonicalPath();
            
            // Check if new path is within root directory
            if (!canonicalPath.startsWith(canonicalRoot)) {
                return false;
            }
            
            if (newDir.exists() && newDir.isDirectory()) {
                currentPath = canonicalPath;
                return true;
            }
        } catch (IOException e) {
            return false;
        }
        
        return false;
    }
    
    /**
     * Navigate to parent directory
     * Returns true if successful, false if already at root
     */
    public boolean goUp() {
        File current = new File(currentPath);
        File parent = current.getParentFile();
        
        if (parent == null) {
            return false;
        }
        
        try {
            String parentCanonical = parent.getCanonicalPath();
            String rootCanonical = new File(rootPath).getCanonicalPath();
            
            // Don't go above root
            if (parentCanonical.length() < rootCanonical.length()) {
                return false;
            }
            
            currentPath = parentCanonical;
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Reset current path to root directory
     */
    public void resetToRoot() {
        currentPath = rootPath;
    }
    
    /**
     * Get relative path from root to current directory
     */
    public String getRelativePathFromRoot() {
        try {
            Path root = Paths.get(rootPath);
            Path current = Paths.get(currentPath);
            Path relative = root.relativize(current);
            return relative.toString();
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * Get file content as string (for text files)
     */
    public String readFile(String relativePath) throws IOException {
        File file = new File(currentPath, relativePath);
        
        // Security check: ensure file is within root
        String canonical = file.getCanonicalPath();
        String rootCanonical = new File(rootPath).getCanonicalPath();
        
        if (!canonical.startsWith(rootCanonical)) {
            throw new SecurityException("Access denied: file is outside root directory");
        }
        
        if (!file.exists() || !file.isFile()) {
            throw new IOException("File not found or is not a file");
        }
        
        return new String(Files.readAllBytes(file.toPath()));
    }
    
    /**
     * Get file content as bytes
     */
    public byte[] readFileBytes(String relativePath) throws IOException {
        File file = new File(currentPath, relativePath);
        
        // Security check: ensure file is within root
        String canonical = file.getCanonicalPath();
        String rootCanonical = new File(rootPath).getCanonicalPath();
        
        if (!canonical.startsWith(rootCanonical)) {
            throw new SecurityException("Access denied: file is outside root directory");
        }
        
        if (!file.exists() || !file.isFile()) {
            throw new IOException("File not found or is not a file");
        }
        
        return Files.readAllBytes(file.toPath());
    }
    
    /**
     * Write text content to a file
     * Creates parent directories if they don't exist
     */
    public void writeFile(String relativePath, String content) throws IOException {
        File file = new File(currentPath, relativePath);
        
        // Security check: ensure file is within root
        String canonical = file.getCanonicalPath();
        String rootCanonical = new File(rootPath).getCanonicalPath();
        
        if (!canonical.startsWith(rootCanonical)) {
            throw new SecurityException("Access denied: file is outside root directory");
        }
        
        // Create parent directories if they don't exist
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IOException("Failed to create parent directories");
            }
        }
        
        // Write content to file
        Files.write(file.toPath(), content.getBytes());
    }
    
    /**
     * Create a directory if it does not exist
     * Creates parent directories as needed
     * Returns true if directory was created or already exists
     */
    public boolean createDirectory(String relativePath) throws IOException {
        File directory = new File(currentPath, relativePath);
        
        // Security check: ensure directory is within root
        String canonical = directory.getCanonicalPath();
        String rootCanonical = new File(rootPath).getCanonicalPath();
        
        if (!canonical.startsWith(rootCanonical)) {
            throw new SecurityException("Access denied: directory is outside root directory");
        }
        
        // If already exists and is a directory, return true
        if (directory.exists()) {
            return directory.isDirectory();
        }
        
        // Create directory and all necessary parent directories
        return directory.mkdirs();
    }
    
    /**
     * Check if a path exists
     */
    public boolean exists(String relativePath) {
        File file = new File(currentPath, relativePath);
        return file.exists();
    }
    
    /**
     * Check if a path is a directory
     */
    public boolean isDirectory(String relativePath) {
        File file = new File(currentPath, relativePath);
        return file.isDirectory();
    }
    
    /**
     * Check if a path is a file
     */
    public boolean isFile(String relativePath) {
        File file = new File(currentPath, relativePath);
        return file.isFile();
    }
    
    /**
     * Get file info for a specific file/directory
     */
    public FileInfo getFileInfo(String relativePath) {
        File file = new File(currentPath, relativePath);
        if (!file.exists()) {
            return null;
        }
        return createFileInfo(file);
    }
    
    /**
     * Check if current path is at root
     */
    public boolean isAtRoot() {
        try {
            return new File(currentPath).getCanonicalPath()
                    .equals(new File(rootPath).getCanonicalPath());
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Create a FileInfo object from a File
     */
    private FileInfo createFileInfo(File file) {
        FileInfo info = new FileInfo();
        info.setName(file.getName());
        info.setPath(file.getAbsolutePath());
        info.setDirectory(file.isDirectory());
        info.setSize(file.length());
        info.setLastModified(file.lastModified());
        info.setReadable(file.canRead());
        info.setWritable(file.canWrite());
        
        // Get additional attributes
        try {
            Path path = file.toPath();
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            info.setCreationTime(attrs.creationTime().toMillis());
            info.setHidden(Files.isHidden(path));
        } catch (IOException e) {
            // Use defaults
            info.setCreationTime(0);
            info.setHidden(false);
        }
        
        // Get file extension
        if (!file.isDirectory()) {
            String name = file.getName();
            int lastDot = name.lastIndexOf('.');
            if (lastDot > 0 && lastDot < name.length() - 1) {
                info.setExtension(name.substring(lastDot + 1));
            } else {
                info.setExtension("");
            }
        }
        
        return info;
    }
    
    /**
     * FileInfo class to hold file/directory information
     */
    public static class FileInfo {
        private String name;
        private String path;
        private boolean isDirectory;
        private long size;
        private long lastModified;
        private long creationTime;
        private boolean readable;
        private boolean writable;
        private boolean hidden;
        private String extension;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public boolean isDirectory() { return isDirectory; }
        public void setDirectory(boolean directory) { isDirectory = directory; }
        
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        
        public long getLastModified() { return lastModified; }
        public void setLastModified(long lastModified) { this.lastModified = lastModified; }
        
        public long getCreationTime() { return creationTime; }
        public void setCreationTime(long creationTime) { this.creationTime = creationTime; }
        
        public boolean isReadable() { return readable; }
        public void setReadable(boolean readable) { this.readable = readable; }
        
        public boolean isWritable() { return writable; }
        public void setWritable(boolean writable) { this.writable = writable; }
        
        public boolean isHidden() { return hidden; }
        public void setHidden(boolean hidden) { this.hidden = hidden; }
        
        public String getExtension() { return extension; }
        public void setExtension(String extension) { this.extension = extension; }
        
        /**
         * Get human-readable file size
         */
        public String getFormattedSize() {
            if (isDirectory) return "-";
            
            long bytes = size;
            if (bytes < 1024) return bytes + " B";
            
            int exp = (int) (Math.log(bytes) / Math.log(1024));
            String pre = "KMGTPE".charAt(exp - 1) + "B";
            return String.format("%.1f %s", bytes / Math.pow(1024, exp), pre);
        }
    }

    /**
     * Sanitize an absolute file path to prevent directory traversal
     */
    public static String sanitizeAbsoluteFilePath(String absolutePath) {
        try {
            Path path = Paths.get(absolutePath).toRealPath();
            return path.toString();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Get a file directory relative path from a file absolute path
     * @param absolutePath
     * @return directory relative path
     */
    public static String GetSanitizedRelativeAiFilePath(String absolutePath) {

        // get directory path only
        Path path = Paths.get(absolutePath);
        absolutePath = path.getParent() != null ? path.getParent().toString() : "";

        // convert to sanitized relative path
        absolutePath = FileManager.absolutePathToRelative(absolutePath);
        return absolutePath;
    }

    /**
     * Convert an absolute path to a relative path from the relative root
     */
    public static String absolutePathToRelative(String absolutePath) {
        try {
            Path path = Paths.get(absolutePath).toRealPath();
            Path root = Paths.get(System.getProperty("user.dir")).toRealPath();
            String relativePath = root.relativize(path).toString();
            return relativePath.replace("\\", "/");
        } catch (IOException e) {
            return null;
        }
    }
}
