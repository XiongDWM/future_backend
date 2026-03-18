package com.xiongdwm.future_backend.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Service;

import com.xiongdwm.future_backend.entity.FileLog;
import com.xiongdwm.future_backend.repository.FileLogRepository;
import com.xiongdwm.future_backend.service.FileLogService;
import com.xiongdwm.future_backend.utils.exception.ServiceException;

import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class FileLogServiceImpl implements FileLogService {
    private final FileLogRepository fileLogRepository;
    private final String uploadDir;

    public FileLogServiceImpl(FileLogRepository fileLogRepository,
                              @Value("${file.upload.dir}") String uploadDir) {
        this.fileLogRepository = fileLogRepository;
        this.uploadDir = uploadDir;
    }

    @Override
    public Mono<FileLog> upload(FilePart filePart) {
        String originalFilename = filePart.filename();
        String subfix = originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        String uuid = UUID.randomUUID().toString();
        String storedFilename = uuid + subfix;
        Path dirPath = Paths.get(uploadDir);
        Path filePath = dirPath.resolve(storedFilename);

        if (!Files.exists(dirPath)) {
            try {
                Files.createDirectories(dirPath);
            } catch (IOException e) {
                return Mono.error(new ServiceException("创建上传目录失败: " + e.getMessage()));
            }
        }

        return DataBufferUtils.write(filePart.content(), filePath,
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE)
                .then(Mono.fromCallable(() -> {
                    FileLog fileLog = new FileLog();
                    fileLog.setId(uuid);
                    fileLog.setFilename(originalFilename);
                    fileLog.setSubfix(subfix);
                    fileLog.setUrl(storedFilename);
                    fileLog.setUploadAt(new Date());
                    return fileLogRepository.saveAndFlush(fileLog);
                }));
    }

    @Override
    public Flux<DataBuffer> download(String fileId) {
        var fileLog = fileLogRepository.findById(fileId).orElse(null);
        if (fileLog == null) throw new ServiceException("文件不存在");
        Path filePath = Paths.get(uploadDir).resolve(fileLog.getUrl());
        if (!Files.exists(filePath)) throw new ServiceException("文件不存在于磁盘");
        return DataBufferUtils.read(filePath, new DefaultDataBufferFactory(), 4096);
    }

    @Override
    public String getDownloadFilename(String fileId) {
        var fileLog = fileLogRepository.findById(fileId).orElse(null);
        if (fileLog == null) throw new ServiceException("文件不存在");
        return fileLog.getFilename();
    }

    @Override
    public String getFileSubfix(String fileId) {
        var fileLog = fileLogRepository.findById(fileId).orElse(null);
        if (fileLog == null) throw new ServiceException("文件不存在");
        return fileLog.getSubfix();
    }

    @Override
    public Mono<Boolean> delete(String fileId) {
        return Mono.fromCallable(() -> {
            var fileLog = fileLogRepository.findById(fileId).orElse(null);
            if (fileLog == null) throw new ServiceException("文件不存在");
            Path filePath = Paths.get(uploadDir).resolve(fileLog.getUrl());
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                throw new ServiceException("删除文件失败");
            }
            fileLogRepository.delete(fileLog);
            return true;
        });
    }

    @Override
    public List<FileLog> listFiles() {
        return fileLogRepository.findAll();
    }
}
