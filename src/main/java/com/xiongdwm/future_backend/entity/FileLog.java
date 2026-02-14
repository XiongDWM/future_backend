package com.xiongdwm.future_backend.entity;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="file_log")
public class FileLog {
    @Id
    private String id; // uuid
    private Date uploadAt;
    private String filename;
    private String url;
    private String subfix;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Date getUploadAt() {
		return uploadAt;
	}
	public void setUploadAt(Date uploadAt) {
		this.uploadAt = uploadAt;
	}
	public String getFilename() {
		return filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getSubfix() {
		return subfix;
	}
	public void setSubfix(String subfix) {
		this.subfix = subfix;
	}

    
    
}
