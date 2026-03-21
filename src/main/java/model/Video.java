package model;

import java.time.LocalDate;
import java.time.LocalTime;

public class Video {
    private int id;
    private String title;
    private String author;
    private LocalDate creationDate;
    private LocalTime duration;
    private int views;
    private String description;
    private String format;
    private String filePath;
    private String originalFilename;
    private String fileSource;

    // Used when registering a new video (id is not known yet)
    public Video(String title, String author, LocalDate creationDate,
                 LocalTime duration, int views, String description,
                 String format, String filePath, String originalFilename,
                 String fileSource) {
        this.title            = title;
        this.author           = author;
        this.creationDate     = creationDate;
        this.duration         = duration;
        this.views            = views;
        this.description      = description;
        this.format           = format;
        this.filePath         = filePath;
        this.originalFilename = originalFilename;
        this.fileSource       = fileSource;
    }

    // Used when reading from DB (id is known)
    public Video(int id, String title, String author, LocalDate creationDate,
                 LocalTime duration, int views, String description,
                 String format, String filePath, String originalFilename,
                 String fileSource) {
        this(title, author, creationDate, duration, views, description,
             format, filePath, originalFilename, fileSource);
        this.id = id;
    }

    // Getters
    public int       getId()               { return id; }
    public String    getTitle()            { return title; }
    public String    getAuthor()           { return author; }
    public LocalDate getCreationDate()     { return creationDate; }
    public LocalTime getDuration()         { return duration; }
    public int       getViews()            { return views; }
    public String    getDescription()      { return description; }
    public String    getFormat()           { return format; }
    public String    getFilePath()         { return filePath; }
    public String    getOriginalFilename() { return originalFilename; }
    public String    getFileSource()       { return fileSource; }

    // Setters
    public void setId(int id)                        { this.id = id; }
    public void setTitle(String title)               { this.title = title; }
    public void setAuthor(String author)             { this.author = author; }
    public void setCreationDate(LocalDate d)         { this.creationDate = d; }
    public void setDuration(LocalTime duration)      { this.duration = duration; }
    public void setViews(int views)                  { this.views = views; }
    public void setDescription(String description)   { this.description = description; }
    public void setFormat(String format)             { this.format = format; }
    public void setFilePath(String filePath)         { this.filePath = filePath; }
    public void setOriginalFilename(String name)     { this.originalFilename = name; }
    public void setFileSource(String fileSource)     { this.fileSource = fileSource; }
}