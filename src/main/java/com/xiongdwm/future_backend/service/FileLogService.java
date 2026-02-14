package com.xiongdwm.future_backend.service;

import java.util.List;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.multipart.FilePart;

import com.xiongdwm.future_backend.entity.FileLog;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FileLogService {
    public Mono<FileLog> upload(FilePart filePart);
    public Flux<DataBuffer> download(String fileId);
    public String getDownloadFilename(String fileId);
    public String getFileSubfix(String fileId);
    public Mono<Boolean> delete(String fileId);
    public List<FileLog> listFiles();
}
